package com.example.edgeview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.edgeview.gl.GLRenderer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main activity that displays a camera preview, a GLSurfaceView for rendering,
 * and handles frame processing.
 * Note: Requires CAMERA permission.
 */
public class MainActivity extends AppCompatActivity {

    private CameraFrameProvider cameraFrameProvider;
    private GLSurfaceView glSurfaceView;
    private GLRenderer renderer;
    private ToggleButton modeToggle;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final ConcurrentLinkedQueue<byte[]> glQueue = new ConcurrentLinkedQueue<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initCameraAndGL();
                } else {
                    // Handle permission denial
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout mainLayout = new FrameLayout(this);

        PreviewView previewView = new PreviewView(this);
        glSurfaceView = new GLSurfaceView(this);
        modeToggle = new ToggleButton(this);

        modeToggle.setTextOn("Processed");
        modeToggle.setTextOff("Raw");
        modeToggle.setChecked(true);

        FrameLayout.LayoutParams glParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        FrameLayout.LayoutParams toggleParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        );
        
        mainLayout.addView(previewView);
        mainLayout.addView(glSurfaceView, glParams);
        mainLayout.addView(modeToggle, toggleParams);

        setContentView(mainLayout);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCameraAndGL();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void initCameraAndGL() {
        renderer = new GLRenderer();
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        cameraFrameProvider = new CameraFrameProvider(this);
        cameraFrameProvider.start(this, (nv21, width, height) -> {
            backgroundExecutor.execute(() -> {
                byte[] rgba = convertNV21ToRGBA(nv21, width, height);
                byte[] bytesToRender;

                if (modeToggle.isChecked()) {
                    bytesToRender = NativeLib.processFrameSafe(rgba, width, height);
                } else {
                    bytesToRender = rgba;
                }

                if (bytesToRender != null && glQueue.isEmpty()) {
                    glQueue.offer(bytesToRender);
                    glSurfaceView.queueEvent(() -> {
                        byte[] frameBytes = glQueue.poll();
                        if (frameBytes != null) {
                            renderer.updateFrame(frameBytes, width, height);
                            glSurfaceView.requestRender();
                        }
                    });
                }
            });
        });
    }

    // Basic NV21 to RGBA conversion
    private byte[] convertNV21ToRGBA(byte[] nv21, int width, int height) {
        byte[] rgba = new byte[width * height * 4];
        int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & nv21[yp]) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & nv21[uvp++]) - 128;
                    u = (0xff & nv21[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                int ri = (j * width + i) * 4;
                rgba[ri] = (byte) ((r >> 10) & 0xff);
                rgba[ri + 1] = (byte) ((g >> 10) & 0xff);
                rgba[ri + 2] = (byte) ((b >> 10) & 0xff);
                rgba[ri + 3] = (byte) 0xff;
            }
        }
        return rgba;
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraFrameProvider != null) {
            cameraFrameProvider.stop();
        }
        backgroundExecutor.shutdown();
    }
}
