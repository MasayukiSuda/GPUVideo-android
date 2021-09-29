package com.daasuu.gpuv.egl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.daasuu.gpuv.egl.filter.GlFilter;

import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;



public abstract class GlFrameBufferObjectRenderer implements GLSurfaceView.Renderer {

    private GlFramebufferObject framebufferObject;
    private GlFilter normalShader;

    private boolean isNewShader;

    private final Queue<Runnable> runOnDraw;


    protected GlFrameBufferObjectRenderer() {

        runOnDraw = new LinkedList<Runnable>();
        isNewShader = false;
    }


    @Override
    public final void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
        framebufferObject = new GlFramebufferObject();
        if (normalShader == null) {
            normalShader = new GlFilter();
        }
        normalShader.setup();
        onSurfaceCreated(config);
    }

    @Override
    public final void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        framebufferObject.setup(width, height);
        normalShader.setFrameSize(width, height);
        onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, framebufferObject.getWidth(), framebufferObject.getHeight());
    }

    @Override
    public final void onDrawFrame(final GL10 gl) {
        synchronized (runOnDraw) {
            while (!runOnDraw.isEmpty()) {
                runOnDraw.poll().run();
            }
        }

        if (isNewShader) {
            isNewShader = false;
            normalShader.setup();
            normalShader.setFrameSize(framebufferObject.getWidth(), framebufferObject.getHeight());
        }
        framebufferObject.enable();

        onDrawFrame(framebufferObject);

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        normalShader.draw(framebufferObject.getTexName(), null);

    }

    public void setNormalShader(GlFilter filter) {
        synchronized (runOnDraw) {
            while (!runOnDraw.isEmpty()) {
                runOnDraw.poll().run();
            }
        }
        normalShader = filter;
        isNewShader = true;
    }

    @Override
    protected void finalize() throws Throwable {

    }

    public abstract void onSurfaceCreated(EGLConfig config);

    public abstract void onSurfaceChanged(int width, int height);

    public abstract void onDrawFrame(GlFramebufferObject fbo);

}
