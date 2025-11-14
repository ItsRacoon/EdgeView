package com.example.edgeview;

import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the camera lifecycle and provides frames for processing.
 */
public class CameraFrameProvider {
    private final Context context;
    private ProcessCameraProvider cameraProvider;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    public CameraFrameProvider(Context context) {
        this.context = context;
    }

    public void start(LifecycleOwner lifecycleOwner, FrameCallback callback) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider != null) {
                    bindCamera(lifecycleOwner, callback);
                } else {
                    Log.e("CameraFrameProvider", "Camera provider is null");
                }
            } catch (Exception e) {
                Log.e("CameraFrameProvider", "Failed to get camera provider", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCamera(LifecycleOwner lifecycleOwner, FrameCallback callback) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720)) // Example resolution
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // As per instructions, set the robust analyzer with try-finally block.
        imageAnalysis.setAnalyzer(analysisExecutor, imageProxy -> {
            try {
                byte[] nv21 = yuv420888ToNv21(imageProxy);
                callback.onFrame(nv21, imageProxy.getWidth(), imageProxy.getHeight());
            } catch (Throwable t) {
                Log.e("EdgeView", "Analyzer conversion error", t);
            } finally {
                imageProxy.close();
            }
        });

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
            // Note: We don't set a SurfaceProvider for Preview, as we are using GLSurfaceView for rendering.
        } catch (Exception e) {
            Log.e("CameraFrameProvider", "Use case binding failed", e);
        }
    }

    /**
     * As per instructions, this is the robust YUV_420_888 to NV21 conversion method.
     */
    public static byte[] yuv420888ToNv21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 2;
        byte[] nv21 = new byte[ySize + uvSize];

        // Y plane
        ByteBuffer yBuffer = planes[0].getBuffer();
        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        byte[] row = new byte[yRowStride];
        int pos = 0;
        for (int rowIndex = 0; rowIndex < height; rowIndex++) {
            yBuffer.position(rowIndex * yRowStride);
            int length = Math.min(yRowStride, yBuffer.remaining());
            yBuffer.get(row, 0, length);
            if (yPixelStride == 1) {
                System.arraycopy(row, 0, nv21, pos, width);
                pos += width;
            } else {
                int p = 0;
                for (int col = 0; col < width; col++) {
                    nv21[pos++] = row[p];
                    p += yPixelStride;
                }
            }
        }

        // UV planes (chroma)
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        int uRowStride = planes[1].getRowStride();
        int vRowStride = planes[2].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int chromaHeight = height / 2;
        int chromaWidth = width / 2;
        byte[] uRow = new byte[uRowStride];
        byte[] vRow = new byte[vRowStride];
        int uvPos = ySize;

        for (int rowIndex = 0; rowIndex < chromaHeight; rowIndex++) {
            uBuffer.position(rowIndex * uRowStride);
            vBuffer.position(rowIndex * vRowStride);
            int uLength = Math.min(uRowStride, uBuffer.remaining());
            int vLength = Math.min(vRowStride, vBuffer.remaining());
            uBuffer.get(uRow, 0, uLength);
            vBuffer.get(vRow, 0, vLength);

            int uIndex = 0;
            int vIndex = 0;
            for (int col = 0; col < chromaWidth; col++) {
                byte u = uRow[uIndex];
                byte v = vRow[vIndex];
                // NV21 expects V then U
                nv21[uvPos++] = v;
                nv21[uvPos++] = u;
                uIndex += uPixelStride;
                vIndex += uPixelStride;
            }
        }

        return nv21;
    }

    public void stop() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        analysisExecutor.shutdown();
    }

    public interface FrameCallback {
        void onFrame(byte[] nv21, int width, int height);
    }
}
