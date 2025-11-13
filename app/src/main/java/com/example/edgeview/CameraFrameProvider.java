package com.example.edgeview;

import android.content.Context;
import android.graphics.ImageFormat;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

// Note: Requires CAMERA permission in AndroidManifest.xml
// <uses-permission android:name="android.permission.CAMERA" />
public class CameraFrameProvider {

    public interface FrameCallback {
        void onFrame(byte[] nv21bytes, int width, int height);
    }

    private final Context context;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;

    public CameraFrameProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start(@NonNull LifecycleOwner lifecycleOwner, @NonNull FrameCallback callback) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider == null) {
                    return;
                }

                Preview preview = new Preview.Builder().build();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, image -> {
                    if (image == null || image.getFormat() != ImageFormat.YUV_420_888) {
                        if (image != null) {
                            image.close();
                        }
                        return;
                    }

                    byte[] nv21 = yuv420888ToNv21(image);
                    callback.onFrame(nv21, image.getWidth(), image.getHeight());
                    image.close();
                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void stop() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (!analysisExecutor.isShutdown()) {
            analysisExecutor.shutdown();
        }
    }

    private byte[] yuv420888ToNv21(@NonNull ImageProxy image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final ImageProxy.PlaneProxy[] planes = image.getPlanes();
        final int yuvSize = width * height * 3 / 2;
        final byte[] nv21 = new byte[yuvSize];
        final ByteBuffer yBuffer = planes[0].getBuffer();
        final ByteBuffer uBuffer = planes[1].getBuffer();
        final ByteBuffer vBuffer = planes[2].getBuffer();
        final int yRowStride = planes[0].getRowStride();
        final int uRowStride = planes[1].getRowStride();
        final int vRowStride = planes[2].getRowStride();
        final int vPixelStride = planes[2].getPixelStride();

        // Copy Y plane
        int yPos = 0;
        for (int row = 0; row < height; ++row) {
            yBuffer.position(row * yRowStride);
            yBuffer.get(nv21, yPos, width);
            yPos += width;
        }

        // Copy VU plane
        final int chromaWidth = width / 2;
        final int chromaHeight = height / 2;
        int vuPos = width * height;
        final byte[] vRow = new byte[vRowStride];

        for (int row = 0; row < chromaHeight; ++row) {
            vBuffer.position(row * vRowStride);
            vBuffer.get(vRow, 0, vRowStride);
             uBuffer.position(row * uRowStride);

            for (int col = 0; col < chromaWidth; ++col) {
                final int vPos = col * vPixelStride;
                nv21[vuPos++] = vRow[vPos];
                nv21[vuPos++] = uBuffer.get();
            }
        }
        return nv21;
    }
}
