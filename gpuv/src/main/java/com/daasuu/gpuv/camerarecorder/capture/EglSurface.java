package com.daasuu.gpuv.camerarecorder.capture;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;



public class EglSurface {
    private static final boolean DEBUG = false;
    private static final String TAG = "EglWrapper";


    private final EglWrapper egl;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
    private final int width, height;

    EglSurface(final EglWrapper egl, final Object surface) {
        if (DEBUG) Log.v(TAG, "EglSurface:");
        if (!(surface instanceof SurfaceView)
                && !(surface instanceof Surface)
                && !(surface instanceof SurfaceHolder)
                && !(surface instanceof SurfaceTexture))
            throw new IllegalArgumentException("unsupported surface");
        this.egl = egl;
        eglSurface = this.egl.createWindowSurface(surface);
        width = this.egl.querySurface(eglSurface, EGL14.EGL_WIDTH);
        height = this.egl.querySurface(eglSurface, EGL14.EGL_HEIGHT);
        if (DEBUG) Log.v(TAG, String.format("EglSurface:size(%d,%d)", width, height));
    }

    public void makeCurrent() {
        egl.makeCurrent(eglSurface);
    }

    public void swap() {
        egl.swap(eglSurface);
    }

    public EGLContext getContext() {
        return egl.getContext();
    }

    public void release() {
        if (DEBUG) Log.v(TAG, "EglSurface:release:");
        egl.makeDefault();
        egl.destroyWindowSurface(eglSurface);
        eglSurface = EGL14.EGL_NO_SURFACE;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
