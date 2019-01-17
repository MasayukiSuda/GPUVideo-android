package com.daasuu.gpuv.egl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Size;
import com.daasuu.gpuv.camerarecorder.capture.MediaVideoEncoder;
import com.daasuu.gpuv.egl.filter.GlFilter;

import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.*;



public class GlPreviewRenderer extends GlFrameBufferObjectRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private final Handler handler = new Handler();

    private GlSurfaceTexture previewTexture;

    // private final Camera camera;
    private int texName;

    private float[] MVPMatrix = new float[16];
    private float[] ProjMatrix = new float[16];
    private float[] MMatrix = new float[16];
    private float[] VMatrix = new float[16];
    private float[] STMatrix = new float[16];


    private final GLSurfaceView glView;

    private GlFramebufferObject filterFramebufferObject;
    private GlPreview previewShader;

    private GlFilter glFilter;
    private boolean isNewShader;

    private int angle = 0;
    private float aspectRatio = 1f;
    private float scaleRatio = 1f;
    private float drawScale = 1f;
    private float gestureScale = 1f;

    private Size cameraResolution;

    private int updateTexImageCounter = 0;
    private int updateTexImageCompare = 0;

    private SurfaceCreateListener surfaceCreateListener;
    private MediaVideoEncoder videoEncoder;


    public GlPreviewRenderer(GLSurfaceView glView) {
        this.glView = glView;
        this.glView.setEGLConfigChooser(new GlConfigChooser(false));
        this.glView.setEGLContextFactory(new GlContextFactory());
        this.glView.setRenderer(this);
        this.glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


        Matrix.setIdentityM(STMatrix, 0);
    }

    public void onStartPreview(float cameraPreviewWidth, float cameraPreviewHeight, boolean isLandscapeDevice) {

        Matrix.setIdentityM(MMatrix, 0);
        Matrix.rotateM(MMatrix, 0, -angle, 0.0f, 0.0f, 1.0f);

//        Log.d("GPUCameraRecorder ", "angle" + angle);
//        Log.d("GPUCameraRecorder ", "getMeasuredHeight " + glView.getMeasuredHeight());
//        Log.d("GPUCameraRecorder ", "getMeasuredWidth " + glView.getMeasuredWidth());
//        Log.d("GPUCameraRecorder ", "cameraPreviewWidth " + cameraPreviewWidth);
//        Log.d("GPUCameraRecorder ", "cameraPreviewHeight " + cameraPreviewHeight);


        if (isLandscapeDevice) {

            if (glView.getMeasuredWidth() == glView.getMeasuredHeight()) {

                float scale = Math.max(cameraPreviewWidth / cameraPreviewHeight,
                        cameraPreviewHeight / cameraPreviewWidth);
                Matrix.scaleM(MMatrix, 0, 1f * scale, 1f * scale, 1);

            } else {
                float scale = Math.max(
                        (float) glView.getMeasuredHeight() / cameraPreviewWidth,
                        (float) glView.getMeasuredWidth() / cameraPreviewHeight);
                Matrix.scaleM(MMatrix, 0, 1f * scale, 1f * scale, 1);
            }

        } else {
            // Portlate
            // View 1920 1080 Camera 1280 720 OK
            // View 1920 1080 Camera 800 600 OK
            // View 1440 1080 Camera 800 600 OK
            // View 1080 1080 Camera 1280 720 Need Scale
            // View 1080 1080 Camera 800 600 Need Scale


            float viewAspect = (float) glView.getMeasuredHeight() / glView.getMeasuredWidth();
            float cameraAspect = cameraPreviewWidth / cameraPreviewHeight;
            if (viewAspect >= cameraAspect) {
                Matrix.scaleM(MMatrix, 0, 1f, 1f, 1);
            } else {
                float adjust = cameraAspect / viewAspect;
                Matrix.scaleM(MMatrix, 0, 1f * adjust, 1f * adjust, 1);
            }
        }

    }

    public void setGlFilter(final GlFilter filter) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (glFilter != null) {
                    glFilter.release();
                }
                glFilter = filter;
                isNewShader = true;
                glView.requestRender();
            }
        });
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // increment every time a new frame is avail
        updateTexImageCounter++;
        glView.requestRender();
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        final int[] args = new int[1];

        GLES20.glGenTextures(args.length, args, 0);
        texName = args[0];

        // SurfaceTextureを生成
        previewTexture = new GlSurfaceTexture(texName);
        previewTexture.setOnFrameAvailableListener(this);

        GLES20.glBindTexture(previewTexture.getTextureTarget(), texName);
        // GL_TEXTURE_EXTERNAL_OES
        EglUtil.setupSampler(previewTexture.getTextureTarget(), GL_LINEAR, GL_NEAREST);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        filterFramebufferObject = new GlFramebufferObject();
        // GL_TEXTURE_EXTERNAL_OES
        previewShader = new GlPreview(previewTexture.getTextureTarget());
        previewShader.setup();


        Matrix.setLookAtM(VMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );


        if (glFilter != null) {
            isNewShader = true;
        }

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0);

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (surfaceCreateListener != null) {
                    surfaceCreateListener.onCreated(previewTexture.getSurfaceTexture());
                }
            }
        });
    }

    @Override
    public void onSurfaceChanged(int width, int height) {

        filterFramebufferObject.setup(width, height);
        previewShader.setFrameSize(width, height);
        if (glFilter != null) {
            glFilter.setFrameSize(width, height);
        }
        scaleRatio = (float) width / height;
        Matrix.frustumM(ProjMatrix, 0, -scaleRatio, scaleRatio, -1, 1, 5, 7);
    }

    @Override
    public void onDrawFrame(GlFramebufferObject fbo) {

        if (drawScale != gestureScale) {

            float tempScale = 1 / drawScale;
            Matrix.scaleM(MMatrix, 0, tempScale, tempScale, 1);
            drawScale = gestureScale;
            Matrix.scaleM(MMatrix, 0, drawScale, drawScale, 1);
        }

        synchronized (this) {
            if (updateTexImageCompare != updateTexImageCounter) {
                // loop and call updateTexImage() for each time the onFrameAvailable() method was called below.
                while (updateTexImageCompare != updateTexImageCounter) {

                    previewTexture.updateTexImage();
                    previewTexture.getTransformMatrix(STMatrix);
                    updateTexImageCompare++;  // increment the compare value until it's the same as _updateTexImageCounter
                }
            }

        }

        if (isNewShader) {
            if (glFilter != null) {
                glFilter.setup();
                glFilter.setFrameSize(fbo.getWidth(), fbo.getHeight());
            }
            isNewShader = false;
        }

        if (glFilter != null) {
            filterFramebufferObject.enable();
        }

        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(MVPMatrix, 0, VMatrix, 0, MMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, ProjMatrix, 0, MVPMatrix, 0);

        previewShader.draw(texName, MVPMatrix, STMatrix, aspectRatio);


        if (glFilter != null) {
            fbo.enable();
            GLES20.glClear(GL_COLOR_BUFFER_BIT);
            glFilter.draw(filterFramebufferObject.getTexName(), fbo);
        }

        synchronized (this) {
            if (videoEncoder != null) {
                // notify to capturing thread that the camera frame is available.
                videoEncoder.frameAvailableSoon(texName, STMatrix, MVPMatrix, aspectRatio);
            }
        }

    }

    public void setCameraResolution(Size cameraResolution) {
        this.cameraResolution = cameraResolution;
    }

    public void setVideoEncoder(final MediaVideoEncoder encoder) {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (GlPreviewRenderer.this) {
                    if (encoder != null) {
                        encoder.setEglContext(EGL14.eglGetCurrentContext(), texName);
                    }
                    videoEncoder = encoder;
                }
            }
        });

    }

    public GlSurfaceTexture getPreviewTexture() {
        return previewTexture;
    }

    public void setAngle(int angle) {
        this.angle = angle;
        if (angle == 90 || angle == 270) {
            aspectRatio = (float) cameraResolution.getWidth() / cameraResolution.getHeight();
        } else {
            aspectRatio = (float) cameraResolution.getHeight() / cameraResolution.getWidth();
        }
    }

    public void setGestureScale(float gestureScale) {
        this.gestureScale = gestureScale;
    }

    public GlFilter getFilter() {
        return glFilter;
    }

    public void release() {
        glView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (glFilter != null) {
                    glFilter.release();
                }
            }
        });
    }

    public interface SurfaceCreateListener {
        void onCreated(SurfaceTexture surface);
    }

    public void setSurfaceCreateListener(SurfaceCreateListener surfaceCreateListener) {
        this.surfaceCreateListener = surfaceCreateListener;
    }
}

