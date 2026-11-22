# SkinSense AI

**An Android-based skincare education and cosmetology companion application**

SkinSense AI is a Java-based Android application that brings together basic skincare education, cosmetic ingredient information, routine guidance, sun-protection information, a simple skin-type self-assessment, and facial-photo selection/preview.

> **Current implementation status:** The project includes the TensorFlow Lite Interpreter dependency and a basic model-loading helper, but it does **not** include a trained skin-analysis model. The photo feature currently selects and previews an image only. It does not generate AI skin predictions or diagnose skin conditions.

## Features

- Branded splash screen
- Home dashboard with skincare-related sections
- Facial photo picker and image preview
- Basic skin-type self-assessment using user-selected answers
- Cosmetic ingredient encyclopedia
- Morning and evening routine guidance
- Sun-protection education
- Cosmetology learning hub

## Technology Stack

- **Language:** Java
- **User interface:** XML layouts
- **Build configuration:** Gradle Groovy DSL
- **Android namespace/application ID:** `com.example.skinsenseai`
- **Minimum SDK:** API 24
- **Compile SDK:** API 35
- **Target SDK:** API 35
- **AndroidX AppCompat:** 1.7.0
- **Material Components:** 1.12.0
- **TensorFlow Lite Interpreter:** 2.17.0

## Project Structure

```text
SkinSenseAI/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── MODEL_README.txt
│       ├── java/com/example/skinsenseai/
│       │   ├── MainActivity.java
│       │   ├── SkinModelHelper.java
│       │   └── SplashActivity.java
│       └── res/
│           ├── drawable/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   └── activity_splash.xml
│           └── values/
│               ├── colors.xml
│               └── styles.xml
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

## Setup and Run

1. Download or clone this repository.
2. Extract the project if downloaded as a ZIP.
3. Open Android Studio.
4. Select **File → Open** and choose the `SkinSenseAI` project folder (the folder containing `settings.gradle`).
5. Use **JDK 17** and allow Gradle sync to complete.
6. If prompted, install Android SDK Platform 35 through SDK Manager.
7. Run the app on an Android emulator or a connected Android device.

## How the Current App Works

1. `SplashActivity` displays the branded splash screen briefly.
2. The splash screen opens `MainActivity`.
3. `MainActivity` connects dashboard buttons to their corresponding actions.
4. The photo option opens Android's content picker and displays the selected image.
5. Other dashboard options display educational content or a basic self-assessment dialog.
6. `SkinModelHelper` provides a foundation for loading a TensorFlow Lite model and inspecting its input/output tensor shapes.

## TensorFlow Lite / AI Status

The current project does not contain a trained `.tflite` skin-analysis model. `SkinModelHelper` loads a model asset by filename and exposes input/output tensor shapes, but model-specific image preprocessing, inference, and output interpretation have not been implemented.

Before adding AI predictions:

- Select a model whose task and license are documented.
- Verify input image dimensions, color channel order, data type, and normalization.
- Verify output tensor dimensions, labels, and confidence interpretation.
- Evaluate performance across different skin tones, lighting conditions, and image qualities.
- Implement and test preprocessing, inference, and output decoding according to that model's specification.
- Avoid presenting experimental model outputs as medical diagnoses.

Do not rename an unrelated `.tflite` file and present it as a skin-analysis model.

## Limitations

- No trained skin-analysis model is bundled.
- The photo feature is limited to image selection and preview.
- The skin-type result is based on a small self-assessment, not image analysis.
- The application is an educational prototype and is not a medical diagnostic tool.

## Future Improvements

- Integrate a suitable, evaluated, and properly licensed TensorFlow Lite model.
- Implement image preprocessing and inference.
- Add model-result interpretation with clear confidence and limitation messaging.
- Improve accessibility, input validation, and device testing.
- Expand the ingredient and cosmetology learning library.
- Add automated tests and document real-device testing results.

## Disclaimer

SkinSense AI is an educational skincare and cosmetology companion. It does not diagnose, treat, or prevent skin diseases and does not replace advice from a qualified dermatologist or other healthcare professional.

## Reference

The project README identifies the following repository as an assignment reference:

[Assignment2](https://github.com/Uzair390-Del/Mobile-App-Developement/blob/main/Assignments/Theory/Assignment%20No%202.md)

SkinSense AI is an independent implementation; the reference repository is not included in this project ZIP.
