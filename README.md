# Experimental CV Lab (ECVL)

<p align="center">
  <img src="https://socialify.git.ci/mirror-2922/ExperimentalCVLab/image?description=1&descriptionEditable=A%20high-performance%20Android%20Computer%20Vision%20laboratory%20powered%20by%20OpenCV%2C%20YOLOv12%2C%20and%20ONNX%20Runtime.&font=Inter&forks=1&issues=1&language=1&name=1&owner=1&pattern=Circuit%20Board&pulls=1&stargazers=1&theme=Dark" alt="ExperimentalCVLab" />
</p>

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/mirror-2922/ExperimentalCVLab/build.yml?branch=master&style=for-the-badge" alt="Build Status" />
  <img src="https://img.shields.io/github/v/release/mirror-2922/ExperimentalCVLab?label=dev&style=for-the-badge&color=orange" alt="Release" />
  <img src="https://img.shields.io/badge/Android-API%2026%2B-green?style=for-the-badge&logo=android" alt="Android Support" />
  <img src="https://img.shields.io/badge/License-GPL%20v2-blue?style=for-the-badge" alt="License" />
</p>

---

> "Pushing the boundaries of real-time mobile computer vision. A playground for modern AI inference and high-performance image processing."

**Experimental CV Lab (ECVL)** is a high-performance Android application designed for real-time computer vision tasks. It integrates state-of-the-art object detection (YOLOv12), face recognition (Google ML Kit), and advanced image processing filters via a modular C++ JNI architecture.

## 🚀 Key Features

### 🧠 Advanced AI Inference
- **Multi-Engine Support**: Seamlessly switch between **OpenCV DNN** and **ONNX Runtime** backends.
- **Hardware Acceleration**: Full support for **CPU**, **GPU (OpenCL)**, and **NPU (NNAPI)**.
- **YOLO Ecosystem**: Real-time object detection supporting YOLOv12, v11, and v8 models.
- **Face Detection**: Integration with Google ML Kit for robust, low-latency face tracking.

### ⚡ Performance-First Architecture
- **Dual-Path Pipeline**: Decoupled preview and inference streams. Achieve crystal-clear **4K preview** while running AI inference at an optimized lower resolution (e.g., 640p).
- **Pre-Rotation Scaling**: Specialized optimization for high-resolution sensors. By scaling before rotation, the compute-intensive rotation overhead is reduced by up to **97%** on 4K streams.
- **Aspect-Aware Overlay**: Precision coordinate mapping that automatically compensates for camera sensor aspect ratios and UI letterboxing.

### 🛠️ Lab Tools & UI
- **Model Management**: Online model center to download, delete, or add custom ONNX models via URL.
- **Dynamic Resolution**: Real-time hardware capability detection with support for dozens of capture resolutions.
- **Real-Time HUD**: Professional performance monitor showing FPS, dual-resolution stats, latency, and live **CPU/GPU/NPU usage**.
- **Material You Design**: Modern, dynamic-color themed UI built entirely with Jetpack Compose.

## 🛠️ Build Information

The project uses a cutting-edge Android toolchain:
- **Gradle**: 9.2.1
- **AGP**: 8.13.0
- **Kotlin**: 2.1.0 (K2 Compiler)
- **NDK**: 28.2.13676358
- **OpenCV**: 4.10.0 (Official Maven distribution)
- **ONNX Runtime**: 1.18.0

## 📅 Roadmap (TODO)
- [ ] **Socket Communication**: Transfer real-time detection data (JSON/Text) to PC via network.
- [ ] **Custom Filter Shader**: Add support for user-defined GLSL shaders.
- [ ] **NPU Fine-tuning**: Targeted optimization for Snapdragon 8 Elite NPU.
- [ ] **Recording Support**: Capture AI-processed video streams with overlays.

## 📜 Credits
- **OpenCV**: [opencv.org](https://opencv.org/)
- **ONNX Runtime**: [onnxruntime.ai](https://onnxruntime.ai/)
- **ML Kit**: [developers.google.com/ml-kit](https://developers.google.com/ml-kit)
- **YOLO**: [Ultralytics](https://ultralytics.com/) & community ONNX exports.

## ⚖️ License
Distributed under the **GNU GPL v2** License. See `LICENSE` for more information.

---
Developed with ❤️ by [mirror-2922](https://github.com/mirror-2922)
