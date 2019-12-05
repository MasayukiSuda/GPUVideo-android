package com.daasuu.gpuv.egl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.daasuu.gpuv.egl.filter.GlFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    private final Queue<Runnable> runOnDraw;

    private ScreenshotConsumer screenshoter;

    protected GlFrameBufferObjectRenderer() {
        runOnDraw = new LinkedList<Runnable>();
    }


    @Override
    public final void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
        framebufferObject = new GlFramebufferObject();
        normalShader = new GlFilter();
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
        framebufferObject.enable();

        maybeTakeScreenshot();
        onDrawFrame(framebufferObject);

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        normalShader.draw(framebufferObject.getTexName(), null);

    }

    private void maybeTakeScreenshot() {
        if (screenshoter != null) {
            int width = framebufferObject.getWidth();
            int height = framebufferObject.getHeight();
            int screenshotSize = width * height;
            ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
            bb.order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
            int[] pixelsBuffer = new int[screenshotSize];
            bb.asIntBuffer().get(pixelsBuffer);
            bb = null;

            for (int i = 0; i < screenshotSize; ++i) {
                // The alpha and green channels' positions are preserved while the      red and blue are swapped
                pixelsBuffer[i] = ((pixelsBuffer[i] & 0xff00ff00)) | ((pixelsBuffer[i] & 0x000000ff) << 16) | ((pixelsBuffer[i] & 0x00ff0000) >> 16);
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixelsBuffer, screenshotSize - width, -width, 0, 0, width, height);

            screenshoter.accept(bitmap);
            screenshoter = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {

    }

    public abstract void onSurfaceCreated(EGLConfig config);

    public abstract void onSurfaceChanged(int width, int height);

    public abstract void onDrawFrame(GlFramebufferObject fbo);

    public void setScreenConsumer(ScreenshotConsumer screenshoter) {
        this.screenshoter = screenshoter;
    }
}
