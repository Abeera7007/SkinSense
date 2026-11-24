package com.example.skinsenseai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SkinSense AI home screen.
 * Educational cosmetology content is local. The user picks a facial photo, which is
 * classified on-device by a TensorFlow Lite model (see SkinModelHelper).
 */
public class MainActivity extends AppCompatActivity {

    private ImageView selectedPhoto;
    private TextView photoStatus;
    private TextView resultText;

    /** TFLite wrapper; null if the model files are missing or invalid. */
    private SkinModelHelper modelHelper;
    private String modelError;

    /** Inference runs off the UI thread so the app never freezes. */
    private final ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPhotoSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedPhoto = findViewById(R.id.selectedPhoto);
        photoStatus = findViewById(R.id.photoStatus);
        resultText = findViewById(R.id.resultText);

        // Load the TFLite model once. If it is missing, the rest of the app still works.
        try {
            modelHelper = new SkinModelHelper(this);
        } catch (IOException | RuntimeException e) {
            modelError = e.getMessage();
        }

        findViewById(R.id.btnAnalyze).setOnClickListener(v -> choosePhoto());
        findViewById(R.id.btnProfile).setOnClickListener(v -> showProfile());
        findViewById(R.id.btnIngredients).setOnClickListener(v -> showIngredients());
        findViewById(R.id.btnRoutine).setOnClickListener(v -> showRoutine());
        findViewById(R.id.btnSunCare).setOnClickListener(v -> showSunCare());
        findViewById(R.id.btnLearn).setOnClickListener(v -> showCosmetology());
    }

    private void choosePhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Your skin check")
                .setMessage("Choose a clear, evenly lit facial photo without heavy filters or makeup. " +
                        "The photo is analysed on your device. This experimental feature is for education only, not diagnosis.")
                .setPositiveButton("Choose photo", (dialog, which) -> photoPicker.launch("image/*"))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onPhotoSelected(Uri uri) {
        if (uri == null) return;
        selectedPhoto.setImageURI(uri);
        selectedPhoto.setVisibility(View.VISIBLE);

        if (modelHelper == null) {
            photoStatus.setText("Photo selected, but the AI model could not be loaded.");
            resultText.setVisibility(View.VISIBLE);
            resultText.setText("Model not available: " + modelError);
            return;
        }

        photoStatus.setText("Analysing photo on your device...");
        resultText.setVisibility(View.GONE);

        // Decode + infer on a background thread, then post the result back to the UI thread.
        inferenceExecutor.execute(() -> {
            try {
                Bitmap bitmap = decodeBitmap(uri);
                long start = System.nanoTime();
                List<SkinModelHelper.Prediction> results = modelHelper.classify(bitmap, 3);
                long ms = (System.nanoTime() - start) / 1_000_000;
                bitmap.recycle();
                runOnUiThread(() -> showResults(results, ms));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    photoStatus.setText("Could not analyse this photo. Try another one.");
                });
            }
        });
    }

    /** Shows the top predictions and an honest confidence message. */
    private void showResults(List<SkinModelHelper.Prediction> results, long millis) {
        SkinModelHelper.Prediction top = results.get(0);
        StringBuilder sb = new StringBuilder();

        if (top.confidence < 0.55f) {
            sb.append("The model result is uncertain. Try a clearer, evenly lit photo.\n\n");
        } else {
            sb.append("Model's highest-scoring category: ").append(top.label).append("\n\n");
        }
        for (SkinModelHelper.Prediction p : results) {
            sb.append(String.format(Locale.US, "%s  -  %.0f%% model score\n", p.label, p.confidence * 100f));
        }
        sb.append(String.format(Locale.US, "\nOn-device processing: %d ms", millis));
        sb.append("\n\nThis experimental model is based on a limited, imbalanced dataset. Scores are not calibrated and should not be used to assess or diagnose skin conditions.");

        resultText.setText(sb.toString().trim());
        resultText.setVisibility(View.VISIBLE);
        photoStatus.setText("Educational visual estimate only · Not a diagnosis");
    }

    /**
     * Decodes the picked image, down-sampling large photos to save memory, and
     * applies the EXIF rotation so faces are upright before they reach the model.
     */
    private Bitmap decodeBitmap(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= 1024 && bounds.outHeight / (sample * 2) >= 1024) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new IOException("Unable to decode image");

        int rotation = 0;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            int orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
        } catch (IOException ignored) { /* no EXIF data: keep as is */ }

        if (rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            bitmap.recycle();
            return rotated;
        }
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inferenceExecutor.shutdown();
        if (modelHelper != null) modelHelper.close();
    }

    private void showProfile() {
        String[] options = {
                "Mostly oily by midday",
                "Mostly dry or tight",
                "Oily T-zone, other areas normal or dry",
                "Usually comfortable and balanced"
        };

        new AlertDialog.Builder(this)
                .setTitle("How does your skin usually feel?")
                .setItems(options, (dialog, which) -> {
                    String result;
                    switch (which) {
                        case 0: result = "Your answers may be consistent with an oily-skin pattern."; break;
                        case 1: result = "Your answers may be consistent with a dry-skin pattern."; break;
                        case 2: result = "Your answers may be consistent with a combination-skin pattern."; break;
                        default: result = "Your answers may be consistent with a balanced-skin pattern.";
                    }
                    new AlertDialog.Builder(this)
                            .setTitle("Your skin profile")
                            .setMessage(result + "\n\nThis is a simple educational self-assessment, not a diagnosis.")
                            .setPositiveButton("Done", null)
                            .show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showIngredients() {
        showContent("Ingredient encyclopedia",
                "HYALURONIC ACID\nA humectant that helps attract water to the outer layers of skin.\n\n" +
                "NIACINAMIDE\nA form of vitamin B3 used in cosmetic formulations to support the skin barrier and improve the appearance of uneven tone.\n\n" +
                "SALICYLIC ACID (BHA)\nAn oil-soluble exfoliating ingredient commonly used in products for blemish-prone skin. It may irritate sensitive skin.\n\n" +
                "RETINOIDS\nVitamin-A-related ingredients used in cosmetic and dermatological products. They can cause irritation and need careful use.\n\n" +
                "VITAMIN C\nAn antioxidant ingredient often used in formulations targeting the appearance of uneven tone.");
    }

    private void showRoutine() {
        showContent("Your simple daily routine",
                "MORNING\n1. Gentle cleanser, if needed\n2. Moisturizer\n3. Broad-spectrum sunscreen, SPF 30 or higher\n\n" +
                "EVENING\n1. Gentle cleanser\n2. One suitable treatment product, if needed\n3. Moisturizer\n\n" +
                "Introduce new products one at a time. Patch test when appropriate and avoid layering multiple strong active ingredients without qualified guidance.");
    }

    private void showSunCare() {
        showContent("Sun protection guide",
                "Choose a broad-spectrum sunscreen with SPF 30 or higher. Apply enough to cover exposed skin. Reapply about every two hours outdoors and after swimming or sweating. " +
                "Shade, protective clothing and hats also help reduce UV exposure. Follow the product label.");
    }

    private void showCosmetology() {
        showContent("Cosmetology learning hub",
                "SKIN BARRIER\nThe outer protective layer helps limit water loss and protect against external irritants.\n\n" +
                "CLEANSING\nCleansers remove excess oil, sunscreen, makeup and daily impurities.\n\n" +
                "MOISTURIZATION\nMoisturizers may contain humectants, emollients and occlusives.\n\n" +
                "EXFOLIATION\nAHAs and BHAs help loosen dead surface cells. Overuse can irritate skin.\n\n" +
                "COSMETIC FORMULATION\nA product's performance depends on its complete formula, concentration, pH, stability and how it is used.");
    }

    private void showContent(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", null)
                .show();
    }
}
