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

        // NV21 has a single byte per pixel for Y, and a single byte for each V and U component
        // The VU plane is subsampled by 2 in each dimension.
        final int yuvSize = width * height * 3 / 2;
        final byte[] nv21 = new byte[yuvSize];

        final ByteBuffer yBuffer = planes[0].getBuffer();
        final ByteBuffer uBuffer = planes[1].getBuffer();
        final ByteBuffer vBuffer = planes[2].getBuffer();

        final int yRowStride = planes[0].getRowStride();
        final int uRowStride = planes[1].getRowStride();
        final int vRowStride = planes[2].getRowStride();

        final int uPixelStride = planes[1].getPixelStride();
        final int vPixelStride = planes[2].getPixelStride();

        // Copy Y plane
        int yPos = 0;
        if (yRowStride == width) {
            yBuffer.get(nv21, 0, width * height);
            yPos = width * height;
        } else {
             for (int row = 0; row < height; ++row) {
                yBuffer.position(row * yRowStride);
                yBuffer.get(nv21, yPos, width);
                yPos += width;
            }
        }
        
        // Copy VU data. NV21 has interleaved V, U, V, U...
        int vuPos = width * height;
        final int chromaHeight = height / 2;
        final int chromaWidth = width / 2;

        for (int row = 0; row < chromaHeight; ++row) {
            for (int col = 0; col < chromaWidth; ++col) {
                int vPos = row * vRowStride + col * vPixelStride;
                int uPos = row * uRowStride + col * uPixelStride;
                
                // V is first in NV21
                if (vuPos < yuvSize - 1) {
                    nv21[vuPos++] = vBuffer.get(vPos);
                    nv21[vuPos++] = uBuffer.get(uPos);
                }
            }
        }

        return nv21;
    }
}
