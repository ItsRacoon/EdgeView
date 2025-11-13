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
