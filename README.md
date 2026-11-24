<div align="center">

<img src="banner.svg" alt="SkinSense AI banner" width="100%"/>

<br/>

<img src="https://img.shields.io/badge/Platform-Android-E8A9B6?style=for-the-badge&labelColor=4A3428&logo=android&logoColor=white" alt="Android"/>
<img src="https://img.shields.io/badge/Language-Java-F4C2CC?style=for-the-badge&labelColor=4A3428&logo=openjdk&logoColor=white" alt="Java"/>
<img src="https://img.shields.io/badge/UI-XML%20%2B%20Material-E9D5BE?style=for-the-badge&labelColor=4A3428" alt="XML and Material"/>
<img src="https://img.shields.io/badge/ML-TensorFlow%20Lite-D98FA0?style=for-the-badge&labelColor=4A3428&logo=tensorflow&logoColor=white" alt="TensorFlow Lite"/>

<br/><br/>

*A cosmetology education and skincare companion with private, on-device image analysis.*

[Features](#-features) · [Design](#-interface-design) · [Getting Started](#-getting-started) · [AI Analysis](#-ai-skin-analysis) · [Disclaimer](#-disclaimer)

</div>

<br/>

---

##  About

**SkinSense AI** is a Java + XML Android application that combines skincare education with an experimental on-device skin photo analysis tool. It is built as a calm, journal-style companion: learn what's in your products, build a simple routine, understand sun protection, and explore cosmetology basics, all without sending your photos anywhere.

> [!NOTE]
> SkinSense AI is an **educational project**. It does not diagnose skin conditions and is not a substitute for a qualified dermatologist.

---

##  Features

| | Feature | What it does |
|---|---|---|
|  | **Branded splash screen** | Soft, minimal launch experience in the app's palette |
|  | **Editorial home dashboard** | Rounded category tiles and serif headings in a skincare-journal layout |
|  | **Facial photo picker & preview** | Choose a photo from your device and preview it before analysis |
|  | **Skin type self-assessment** | Guided questions to understand your skin type |
|  | **Ingredient encyclopedia** | Plain-language reference for common cosmetic ingredients |
|  | **Morning & evening routines** | Basic step-by-step skincare routine guide |
|  | **Sun protection education** | Learn why and how to protect your skin |
|  | **Cosmetology learning hub** | Introductory cosmetology learning content |
|  | **On-device AI analysis** | TensorFlow Lite inference with the top 3 results and inference time |

---

##  Interface Design

The interface uses a **warm ivory, muted blush, sand and espresso** palette with editorial serif headings, rounded category tiles and a minimal skincare-journal layout. Everything is built with native XML views and drawable shapes, so **no remote image assets** are required for the dashboard.

<div align="center">

| Ivory | Blush | Rose | Sand | Espresso |
|:---:|:---:|:---:|:---:|:---:|
| ![#FFF8F1](https://img.shields.io/badge/-%20%20%20%20%20%20-FFF8F1?style=flat-square) | ![#F4C2CC](https://img.shields.io/badge/-%20%20%20%20%20%20-F4C2CC?style=flat-square) | ![#D98FA0](https://img.shields.io/badge/-%20%20%20%20%20%20-D98FA0?style=flat-square) | ![#E9D5BE](https://img.shields.io/badge/-%20%20%20%20%20%20-E9D5BE?style=flat-square) | ![#4A3428](https://img.shields.io/badge/-%20%20%20%20%20%20-4A3428?style=flat-square) |
| `#FFF8F1` | `#F4C2CC` | `#D98FA0` | `#E9D5BE` | `#4A3428` |

</div>

<!-- Add screenshots here once available:
<p align="center">
  <img src="docs/screenshots/splash.png" width="22%"/>
  <img src="docs/screenshots/home.png" width="22%"/>
  <img src="docs/screenshots/analysis.png" width="22%"/>
  <img src="docs/screenshots/routine.png" width="22%"/>
</p>
-->

---

##  Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| UI | XML layouts, Material Components |
| Libraries | AndroidX AppCompat |
| Build | Gradle (Groovy DSL), Android Gradle Plugin 8.7.3 |
| ML | TensorFlow Lite Interpreter |
| JDK / SDK | JDK 17 · Android SDK Platform 35 |

---

##  Getting Started

1. **Extract** `SkinSenseAI_Complete_Project.zip` (or clone this repository).
2. In Android Studio choose **File → Open**.
3. Select the extracted `SkinSenseAI` folder, the one containing `settings.gradle`.
4. Use **JDK 17** with **Android Gradle Plugin 8.7.3**.
5. Install **Android SDK Platform 35** from SDK Manager if prompted.
6. Let Gradle sync finish, then run on an emulator or Android phone.

---

##  AI Skin Analysis

The app classifies a chosen facial photo **on-device** with a TensorFlow Lite model. Photos are not uploaded by this app flow.

### Pipeline

`SkinModelHelper.java` runs the following steps:

```text
 Photo ──▶ Centre-crop & resize ──▶ RGB → ByteBuffer ──▶ Interpreter.run() ──▶ Top-3 labelled scores
          (to model input size)      (float32 pixels)       (TensorFlow Lite)     (+ inference time)
```

1. Load `skin_model.tflite` from assets
2. Centre-crop and resize the photo to the model's input size
3. Write RGB pixels into a `ByteBuffer`
4. Run `Interpreter.run()`
5. Decode the output vector into labelled probabilities and show the top 3 with inference time

### Adding a custom model

> [!IMPORTANT]
> The generic model files are **not bundled**. Add both to `app/src/main/assets/`:

| File | Purpose |
|---|---|
| `skin_model.tflite` | The TensorFlow Lite model |
| `labels.txt` | One class per line, in the same order as the model output |

You can produce them in two ways:

- **Train your own:** run `tools/train_and_convert.py` (MobileNetV2 transfer learning on a folder-per-class dataset, exported to TFLite).
- **Teachable Machine:** export an image model as TensorFlow Lite, then set `INPUT_MEAN = 127.5f` and `INPUT_STD = 127.5f` in `SkinModelHelper.java`.

Without the model files the app still runs and reports that the model is unavailable.

> Predictions are educational estimates, not medical diagnoses. If you train your own model, report its source, dataset, licence and validation accuracy.

###  Integrated experimental SCIN model

| Property | Value |
|---|---|
| Model asset | `app/src/main/assets/skin_health_model.tflite` |
| Output labels (in order) | `Healthy-looking`, `Reported skin concern` |
| Input | float32 RGB `[1, 224, 224, 3]` |
| Preprocessing | Preserve aspect ratio, neutral-grey padding, normalize RGB to `[-1, 1]` |
| Inference | On-device with TensorFlow Lite; photo is not uploaded by this app flow |

> [!WARNING]
> **Important limitation:** this experimental model showed unstable validation performance during training and was trained on imbalanced, user-reported SCIN groupings. Its displayed percentages are **model scores, not calibrated probabilities**. It is **not suitable for diagnosis** or for determining whether skin is healthy.

---

##  Reference

The assignment reference repository is [Uzair390-Del/NumberGuessingAppTflite](https://github.com/Uzair390-Del/NumberGuessingAppTflite). This project is an **independent implementation**; the reference repository has not been copied into this project.

---

##  Disclaimer

This educational application does not diagnose skin diseases or replace advice from a qualified dermatologist. If you have a skin concern, please consult a medical professional.

<div align="center">

<br/>

<sub>Made with 🌸 and a lot of Gradle syncs</sub>

</div>
