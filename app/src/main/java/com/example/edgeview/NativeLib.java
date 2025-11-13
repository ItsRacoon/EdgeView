package com.example.edgeview;

/**
 * Native library wrapper for processing video frames.
 * The native methods expect an RGBA byte array with a length of 4 * width * height.
 */
public final class NativeLib {
    static {
        // Load dependencies first, in order
        System.loadLibrary("c++_shared");
        System.loadLibrary("opencv_java4");
        // Load our library last
        System.loadLibrary("native-lib");
    }

    public static native byte[] processFrame(byte[] input, int width, int height);

    public static byte[] processFrameSafe(byte[] input, int width, int height) {
        try {
            if (input == null) return null;
            return processFrame(input, width, height);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
