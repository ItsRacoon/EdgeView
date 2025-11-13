package com.example.edgeview.gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "GLRenderer";

    private final String vertexShaderCode =
            "attribute vec4 a_Position;" +
            "attribute vec2 a_TexCoord;" +
            "varying vec2 v_TexCoord;" +
            "void main() {" +
            "  gl_Position = a_Position;" +
            "  v_TexCoord = a_TexCoord;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec2 v_TexCoord;" +
            "uniform sampler2D s_Texture;" +
            "void main() {" +
            "  gl_FragColor = texture2D(s_Texture, v_TexCoord);" +
            "}";

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texCoordBuffer;

    private int programHandle;
    private int positionHandle;
    private int texCoordHandle;
    private int textureUniformHandle;
    private int textureId;

    private volatile byte[] latestBuffer;
    private int frameWidth;
    private int frameHeight;
    private boolean textureSizeChanged = true;

    // Full-screen quad vertices
    private final float[] vertices = {
            -1.0f, -1.0f,  // Bottom Left
             1.0f, -1.0f,  // Bottom Right
            -1.0f,  1.0f,  // Top Left
             1.0f,  1.0f   // Top Right
    };

    // Texture coordinates (note the flipped V axis)
    private final float[] texCoords = {
            0.0f, 1.0f,   // Top Left
            1.0f, 1.0f,   // Top Right
            0.0f, 0.0f,   // Bottom Left
            1.0f, 0.0f    // Bottom Right
    };


    public GLRenderer() {
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(texCoords).position(0);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "Could not create shader of type " + type);
            return 0;
        }
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + type + ":");
            Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        programHandle = GLES20.glCreateProgram();
        if (programHandle == 0) {
            Log.e(TAG, "Could not create GL program.");
            return;
        }

        GLES20.glAttachShader(programHandle, vertexShader);
        GLES20.glAttachShader(programHandle, fragmentShader);
        GLES20.glLinkProgram(programHandle);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(programHandle));
            GLES20.glDeleteProgram(programHandle);
            programHandle = 0;
            return;
        }

        positionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
        texCoordHandle = GLES20.glGetAttribLocation(programHandle, "a_TexCoord");
        textureUniformHandle = GLES20.glGetUniformLocation(programHandle, "s_Texture");

        // Create and configure texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void updateFrame(byte[] rgbaBytes, int width, int height) {
        if (this.frameWidth != width || this.frameHeight != height) {
            this.frameWidth = width;
            this.frameHeight = height;
            textureSizeChanged = true;
        }
        this.latestBuffer = rgbaBytes;
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        byte[] buffer = this.latestBuffer;
        if (buffer != null && frameWidth > 0 && frameHeight > 0) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            if (textureSizeChanged) {
                 GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, frameWidth, frameHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
                 textureSizeChanged = false;
            } else {
                 GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, frameWidth, frameHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
            }
            this.latestBuffer = null;
        }

        if (programHandle != 0) {
            GLES20.glUseProgram(programHandle);

            vertexBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);

            texCoordBuffer.position(0);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
            GLES20.glEnableVertexAttribArray(texCoordHandle);

            GLES20.glUniform1i(textureUniformHandle, 0); // Use texture unit 0

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }
    }
}
