# SkinSense AI — Complete Android Studio Project

A Java + XML Android application starter for a cosmetology education and skincare companion app.

## Interface Design\n\nThe Android interface uses a warm ivory, muted blush, sand, and espresso palette with editorial serif headings, rounded category tiles, and a minimal skincare-journal layout. The design uses native XML views and drawable shapes, so no remote image assets are required for the dashboard.\n\n## Features included
- Branded splash screen
- Editorial-style home dashboard inspired by the supplied visual reference
- Facial photo picker and preview
- Skin type self-assessment
- Cosmetic ingredient encyclopedia
- Basic morning/evening routine guide
- Sun protection education
- Cosmetology learning hub

## Technology
- Java
- XML layouts
- Gradle Groovy DSL
- AndroidX AppCompat
- Material Components
- TensorFlow Lite Interpreter dependency

## Open in Android Studio
1. Extract `SkinSenseAI_Complete_Project.zip`.
2. In Android Studio choose **File > Open**.
3. Select the extracted `SkinSenseAI` folder (the folder containing `settings.gradle`).
4. Use JDK 17 with Android Gradle Plugin 8.7.3.
5. Install Android SDK Platform 35 from SDK Manager if prompted.
6. Allow Gradle sync to finish, then run on an emulator or Android phone.

## AI skin analysis (TensorFlow Lite)
The app classifies a chosen facial photo on-device with a TensorFlow Lite model.
Pipeline (`SkinModelHelper.java`): load `skin_model.tflite` from assets -> centre-crop and resize the
photo to the model's input size -> write RGB pixels into a ByteBuffer -> `Interpreter.run()` -> decode
the output vector into labelled probabilities (top 3 shown with the inference time).

**Model files are not bundled.** Add both to `app/src/main/assets/`:
- `skin_model.tflite`
- `labels.txt` (one class per line, same order as the model output)

Produce them with `tools/train_and_convert.py` (MobileNetV2 transfer learning on a folder-per-class
dataset, exported to TFLite), or export a Teachable Machine image model as TensorFlow Lite and set
`INPUT_MEAN = 127.5f`, `INPUT_STD = 127.5f` in `SkinModelHelper.java`.

Without the model files the app still runs and reports that the model is unavailable.
Predictions are educational estimates, not medical diagnoses. Report your model's source, dataset,
licence and validation accuracy.

## Reference repository from the assignment
https://github.com/Uzair390-Del/NumberGuessingAppTflite

This project is an independent implementation. The reference repository has not been copied into this ZIP.

## Disclaimer
This educational application does not diagnose skin diseases or replace advice from a qualified dermatologist.


## Integrated experimental SCIN model
- Model asset: `app/src/main/assets/skin_health_model.tflite`
- Labels (output order): `Healthy-looking`, `Reported skin concern`
- Input: float32 RGB `[1,224,224,3]`
- Preprocessing: preserve aspect ratio, neutral-grey padding, normalize RGB to `[-1,1]`
- Inference: on-device with TensorFlow Lite; photo is not uploaded by this app flow.

**Important limitation:** this experimental model showed unstable validation performance during training and was trained on imbalanced, user-reported SCIN groupings. Its displayed percentages are model scores, not calibrated probabilities. It is not suitable for diagnosis or determining whether skin is healthy.
