package com.example.edgeview;

public final class NativeLib {
    private NativeLib() {}

    private static boolean nativeAvailable = false;
    private static volatile boolean attemptedLoad = false;

    // call at startup to try load; safe to call multiple times
    public static synchronized void initNative() {
        if (attemptedLoad) return;
        attemptedLoad = true;
        try {
            System.loadLibrary("native-lib");
            nativeAvailable = true;
        } catch (Throwable t) {
            nativeAvailable = false;
            t.printStackTrace();
            android.util.Log.e("EdgeView", "Failed to load native-lib: " + t.getMessage());
        }
    }

    // native declaration (kept for when native is available)
    public static native byte[] processFrame(byte[] input, int width, int height);

    // Safe wrapper used by app code everywhere
    public static byte[] processFrameSafe(byte[] input, int width, int height) {
        // ensure we attempted load at least once
        if (!attemptedLoad) initNative();

        if (!nativeAvailable) {
            // native not available: return input unchanged (or convert in Java if needed)
            return input;
        }
        try {
            return processFrame(input, width, height);
        } catch (Throwable t) {
            t.printStackTrace();
            android.util.Log.e("EdgeView", "native processFrame failed: " + t.getMessage());
            return input;
        }
    }
}
