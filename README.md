# EdgeView

EdgeView is an Android project that integrates OpenCV for computer vision, OpenGL ES 2.0 for rendering, and a TypeScript-based web interface.

## Project Goals

*   Real-time edge detection using OpenCV in C++.
*   Render camera preview and OpenCV output using OpenGL ES 2.0.
*   Provide a web-based UI using TypeScript to control processing parameters.

## Required Tools

*   Android Studio
*   Android NDK
*   CMake
*   OpenCV Android SDK

## Folder Structure

*   `/app`: Main Android application module.
*   `/app/src/main/cpp`: Native C++ source code for OpenCV integration.
*   `/app/src/main/jniLibs`: Pre-compiled native libraries, including OpenCV.
*   `/gl`: OpenGL ES 2.0 shader and utility classes.
*   `/web`: TypeScript source for the web interface.

## Build Steps

1.  Download the OpenCV Android SDK and place it in the `jniLibs` folder.
2.  Ensure Android Studio has the NDK and CMake installed via the SDK Manager.
3.  Configure the `CMakeLists.txt` in `/app/src/main/cpp` to link OpenCV.
4.  Build the TypeScript project in the `/web` directory.
5.  Sync Gradle and build the project in Android Studio.

## Checklist

- [ ] NDK and CMake are installed.
- [ ] OpenCV Android SDK is downloaded and correctly placed.
- [ ] `CMakeLists.txt` is configured.
- [ ] TypeScript project is built.
- [ ] Project builds successfully in Android Studio.

## Build and Run Instructions

### 1. Setup OpenCV
- Download the latest **OpenCV Android SDK** from the [official releases page](https://opencv.org/releases/).
- Unzip the SDK.
- Copy the contents of `sdk/native/jni/include` into your project's `app/src/main/cpp/include` directory. You may need to create the `include` directory.
- Copy the pre-built shared libraries from `sdk/native/libs/<abi>/*.so` into your project's `app/src/main/jniLibs/<abi>/` directory (e.g., copy from `sdk/native/libs/arm64-v8a/libopencv_java4.so` to `app/src/main/jniLibs/arm64-v8a/libopencv_java4.so`). Create the directories if they don't exist.

### 2. Build the Android App
- Open a terminal in the project root.
- Run the Gradle wrapper to build the debug APK:
  ```bash
  ./gradlew assembleDebug
  ```

### 3. Install and Run on Device
- Connect your Android device or start an emulator.
- Install the newly built APK using ADB:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- Launch the "EdgeView" app on your device.

### 4. Run the Web Viewer
- Open a new terminal.
- Navigate to the `web` directory.
- Install dependencies and start the local server:
  ```bash
  cd web
  npm install
  npm run start
  ```
- Open your web browser and go to `http://localhost:8080`.

### Common Fixes
- **CMake can't find OpenCV:** Double-check that the `include` path in your `CMakeLists.txt` is correct and that the files were copied properly into `app/src/main/cpp/include`.
- **Linker errors (undefined reference):** Ensure that `libopencv_java4.so` is present in the `app/src/main/jniLibs` folder for the specific ABI you are building for (e.g., `arm64-v8a`, `x86_64`). The ABI should match your target device/emulator.
