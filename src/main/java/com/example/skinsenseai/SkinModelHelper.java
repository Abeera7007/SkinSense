package com.example.skinsenseai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * On-device TensorFlow Lite image classifier.
 *
 * Preprocessing mirrors the Colab training pipeline:
 * RGB image -> resize while preserving aspect ratio -> grey padding ->
 * float32 RGB values normalized to [-1, 1].
 *
 * The two output classes are user-reported dataset groupings, not diagnoses.
 */
public class SkinModelHelper implements AutoCloseable {
    public static final String MODEL_FILE = "skin_health_model.tflite";
    public static final String LABELS_FILE = "labels.txt";

    public static class Prediction {
        public final String label;
        /** Raw model score. This is not a medically calibrated confidence. */
        public final float confidence;

        Prediction(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }

    private final Interpreter interpreter;
    private final List<String> labels;
    private final int inputWidth;
    private final int inputHeight;
    private final int numClasses;

    public SkinModelHelper(Context context) throws IOException {
        MappedByteBuffer modelBuffer;
        try (AssetFileDescriptor afd = context.getAssets().openFd(MODEL_FILE);
             FileInputStream input = new FileInputStream(afd.getFileDescriptor())) {
            FileChannel channel = input.getChannel();
            modelBuffer = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.getStartOffset(),
                    afd.getDeclaredLength());
        }

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2);
        interpreter = new Interpreter(modelBuffer, options);

        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        if (inputShape.length != 4 || inputShape[0] != 1 || inputShape[3] != 3
                || inputTensor.dataType() != DataType.FLOAT32) {
            interpreter.close();
            throw new IOException("Unsupported model input. Expected float32 [1,H,W,3].");
        }
        inputHeight = inputShape[1];
        inputWidth = inputShape[2];

        Tensor outputTensor = interpreter.getOutputTensor(0);
        int[] outputShape = outputTensor.shape();
        if (outputShape.length != 2 || outputShape[0] != 1
                || outputTensor.dataType() != DataType.FLOAT32) {
            interpreter.close();
            throw new IOException("Unsupported model output. Expected float32 [1,classes].");
        }
        numClasses = outputShape[1];

        labels = loadLabels(context);
        if (labels.size() != numClasses) {
            interpreter.close();
            throw new IOException("Model has " + numClasses + " outputs but labels.txt has "
                    + labels.size() + " labels.");
        }
    }

    private List<String> loadLabels(Context context) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(LABELS_FILE), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) result.add(line);
            }
        }
        return result;
    }

    /** Call from a background thread. */
    public List<Prediction> classify(Bitmap source, int topK) {
        ByteBuffer input = preprocess(source);
        float[][] output = new float[1][numClasses];
        interpreter.run(input, output);

        float[] scores = normalizeScores(output[0]);
        List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < numClasses; i++) {
            predictions.add(new Prediction(labels.get(i), scores[i]));
        }
        Collections.sort(predictions, (a, b) ->
                Float.compare(b.confidence, a.confidence));
        return predictions.subList(0, Math.min(topK, predictions.size()));
    }

    /**
     * Replicates training preprocessing: preserve the full image, resize to fit,
     * fill the remaining canvas with neutral grey, then normalize RGB to [-1,1].
     */
    private ByteBuffer preprocess(Bitmap source) {
        Bitmap fitted = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(fitted);
        canvas.drawColor(Color.rgb(128, 128, 128));

        float scale = Math.min(
                (float) inputWidth / source.getWidth(),
                (float) inputHeight / source.getHeight());
        float scaledWidth = source.getWidth() * scale;
        float scaledHeight = source.getHeight() * scale;
        float left = (inputWidth - scaledWidth) / 2f;
        float top = (inputHeight - scaledHeight) / 2f;

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(source, null,
                new RectF(left, top, left + scaledWidth, top + scaledHeight), paint);

        ByteBuffer buffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * 4);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputWidth * inputHeight];
        fitted.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 127.5f - 1.0f;
            float g = ((pixel >> 8) & 0xFF) / 127.5f - 1.0f;
            float b = (pixel & 0xFF) / 127.5f - 1.0f;
            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
        }
        buffer.rewind();
        fitted.recycle();
        return buffer;
    }

    /** The exported model has a softmax output; keep it when it sums to one. */
    private float[] normalizeScores(float[] raw) {
        float sum = 0f;
        boolean valid = true;
        for (float value : raw) {
            if (Float.isNaN(value) || Float.isInfinite(value) || value < 0f) valid = false;
            sum += value;
        }
        if (valid && Math.abs(sum - 1f) < 0.05f) return raw.clone();

        float max = Float.NEGATIVE_INFINITY;
        for (float value : raw) max = Math.max(max, value);
        float expSum = 0f;
        float[] result = new float[raw.length];
        for (int i = 0; i < raw.length; i++) {
            result[i] = (float) Math.exp(raw[i] - max);
            expSum += result[i];
        }
        for (int i = 0; i < result.length; i++) result[i] /= expSum;
        return result;
    }

    public int[] getInputShape() {
        return interpreter.getInputTensor(0).shape();
    }

    public int[] getOutputShape() {
        return interpreter.getOutputTensor(0).shape();
    }

    @Override
    public void close() {
        interpreter.close();
    }
}
