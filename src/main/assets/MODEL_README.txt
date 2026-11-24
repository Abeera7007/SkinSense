AI MODEL FILES (place both here, then rebuild)

  skin_model.tflite   TensorFlow Lite image classifier, input [1, H, W, 3], float32 or uint8
  labels.txt          one class name per line, in the model's output order

If either file is missing the app still runs; the photo analysis just reports that the model is unavailable.

Match INPUT_MEAN / INPUT_STD in SkinModelHelper.java to your model's training preprocessing
(0/1 for tools/train_and_convert.py models, 127.5/127.5 for Teachable Machine models).

Results are educational estimates only, not a medical diagnosis.
Document the model's source, dataset, licence and validation accuracy in your report.
