# SceneSense

![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![C++](https://img.shields.io/badge/C++-00599C?logo=cplusplus&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)
![llama.cpp](https://img.shields.io/badge/llama.cpp-FFD700?logoColor=black)
![OpenCL](https://img.shields.io/badge/OpenCL-ED1C24?logo=khronos&logoColor=white)
![ML Kit](https://img.shields.io/badge/ML_Kit-4285F4?logo=google&logoColor=white)
![HuggingFace](https://img.shields.io/badge/HuggingFace-FFD21E?logo=huggingface&logoColor=black)

Android app that uses on-device AI to describe scenes from your camera and answer follow-up questions about what it sees. No cloud, no servers, everything runs locally on your phone.

## What it does

- **Photo mode** — Capture a photo and get an AI description with streaming token-by-token output
- **Video mode** — Record a short clip and get a description of what's happening
- **Continuous mode** — Auto-capture and describe frames in a loop
- **Q&A Chat** — Ask up to 3 follow-up questions about any captured image or video
- **Streaming inference** — Text appears word by word as the model generates it, instead of waiting for the full response
- **Hands-free voice commands** — Control the entire app by voice: take photos, record video, switch modes, ask questions, and hear results read aloud (English & Spanish)
- **Early TTS** — In voice command mode, the app starts reading complete sentences aloud before the model finishes generating the full response
- **English / Spanish** — Choose your language on first launch; Spanish mode auto-translates all responses and accepts voice/text input in Spanish

## How it works

SceneSense runs [SmolVLM2-500M-Video-Instruct](https://huggingface.co/HuggingFaceTB/SmolVLM2-500M-Video-Instruct) entirely on-device via [llama.cpp](https://github.com/ggerganov/llama.cpp). The model (~500 MB) is downloaded once on first launch. After that, the app works fully offline.

Inference uses a **streaming JNI callback** architecture: the C++ token generation loop calls back into Kotlin on every token via JNI, emitting tokens through a `callbackFlow`. The UI updates reactively as each token arrives, so text appears progressively instead of all at once.

**Voice command mode** uses Android's `SpeechRecognizer` in a continuous listen loop. A bilingual parser recognizes commands in English and Spanish. TTS output is coordinated with the recognizer to avoid echo feedback. In English mode, complete sentences are fed to TTS as they stream in (early TTS), so the user starts hearing the response before generation finishes.

Language support is powered by [Google ML Kit](https://developers.google.com/ml-kit/language/translation) on-device translation (bidirectional EN-ES). In Spanish mode, model responses are auto-translated and user input is translated back to English for the model. In English mode, no translation overhead is added.

## Requirements

- Android 10+ (API 29)
- ARM64 device (arm64-v8a)
- ~1 GB free storage (model files)
- Camera and microphone permissions

## Building

```bash
# Clone with submodules
git clone --recursive https://github.com/YOUR_USERNAME/SceneSense.git
cd SceneSense

# Debug build
./gradlew assembleDebug

# Release build (requires signing config in gradle.properties)
./gradlew bundleRelease
```

### NDK requirement

The project uses NDK `28.0.13004108` for building the llama.cpp native library with OpenCL (Adreno GPU) support.

## GPU acceleration (OpenCL / Adreno)

SceneSense offloads all transformer layers and the vision encoder to the GPU via OpenCL, targeting Qualcomm Adreno GPUs. This is what makes real-time inference possible on a 500M-parameter model.

### How it works

Android doesn't ship OpenCL headers or linkable libraries in the NDK, so the project solves this with a stub-based approach:

1. **Build time** — A custom `FindOpenCL.cmake` intercepts llama.cpp's `find_package(OpenCL)` call. Instead of searching for a system library, it compiles a minimal stub (`libOpenCL_stub.c`) that satisfies the linker. Khronos OpenCL 3.0 headers are bundled in `app/src/main/cpp/opencl/CL/`.

2. **Kernel embedding** — llama.cpp's ~90 OpenCL kernels (GEMM, quantized matmul, flash attention, softmax, RoPE, etc.) are embedded at build time as `.cl.h` headers via `GGML_OPENCL_EMBED_KERNELS=ON`. Adreno-optimized kernel variants are selected via `GGML_OPENCL_USE_ADRENO_KERNELS=ON`.

3. **Runtime** — On the device, `ggml_backend_load_all()` dynamically loads the real `libOpenCL.so` from `/system/vendor/lib64/` (provided by Qualcomm). The stub is never actually called.

### Key files

```
app/src/main/cpp/
  cmake/FindOpenCL.cmake      # Custom find module (stub + headers)
  opencl/
    CL/*.h                    # Khronos OpenCL 3.0 headers
    libOpenCL_stub.c          # Minimal stub for linking
  host-toolchain.cmake        # MSVC toolchain for host-side kernel embedding tools
  visionai_jni.cpp            # n_gpu_layers=99, use_gpu=true, flash_attn=enabled
```

### Gradle CMake flags

```
-DGGML_OPENCL=ON
-DGGML_OPENCL_USE_ADRENO_KERNELS=ON
```

These are set in `app/build.gradle.kts` and propagate to llama.cpp's ggml-opencl backend.

### Compatibility

This setup targets **Qualcomm Adreno GPUs** (most Snapdragon devices). On devices without OpenCL support, llama.cpp falls back to the CPU backend (ARM64 NEON) automatically.

## Architecture

```
app/src/main/java/com/example/visionai/
  MainViewModel.kt          # State management, inference orchestration, Q&A, voice commands
  inference/
    LlamaModel.kt           # JNI bridge to llama.cpp (image + video, sync + streaming)
  voice/
    VoiceCommandParser.kt   # Bilingual voice command recognition (EN/ES)
  ui/
    screen/MainScreen.kt    # Main composable, camera + controls + sheet
    sheet/
      ResponseBottomSheet.kt  # Animated bottom sheet container
      ResponseContent.kt      # Single response view (photo/continuous mode)
      ChatContent.kt          # Q&A chat view with bubbles and input
    overlay/                 # Header, viewfinder, bottom controls
    components/              # Reusable cyberpunk UI components + VoiceCommandButton
app/src/main/cpp/
  visionai_jni.cpp          # JNI implementation calling llama.cpp (sync + streaming)
  CMakeLists.txt            # Native build config (links llama.cpp + OpenCL)
  cmake/FindOpenCL.cmake    # Custom OpenCL finder for Android cross-compilation
  opencl/                   # Khronos headers + linker stub for Adreno GPU
  host-toolchain.cmake      # MSVC toolchain for host-side build tools
llama.cpp/                   # Git submodule
```

## Third-party licenses

| Component | License | Usage |
|-----------|---------|-------|
| [SmolVLM2](https://huggingface.co/HuggingFaceTB/SmolVLM2-500M-Video-Instruct) | Apache 2.0 | Vision-language model (downloaded at runtime) |
| [llama.cpp](https://github.com/ggerganov/llama.cpp) | MIT | On-device inference engine (git submodule) |
| [Google ML Kit Translation](https://developers.google.com/ml-kit) | [ML Kit Terms](https://developers.google.com/ml-kit/terms) | Offline bidirectional EN-ES translation |

## Purpose

This project exists to explore the potential of small vision-language models running entirely on mobile devices. It demonstrates that meaningful image understanding and visual Q&A is possible on-device without cloud APIs, preserving user privacy.

## Privacy

SceneSense collects zero data. No images, videos, text, or analytics leave your device. See [Privacy Policy](docs/privacy-policy.html).

## License

This project is licensed under the **GNU Affero General Public License v3.0** (AGPL-3.0). See [LICENSE](LICENSE) for details.

This means you can view, fork, and modify the code, but any distributed or publicly served modifications must also be released under AGPL-3.0 with source code available.
