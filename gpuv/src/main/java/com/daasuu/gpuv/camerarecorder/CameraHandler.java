package com.daasuu.gpuv.camerarecorder;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by sudamasayuki on 2018/03/13.
 */

public class CameraHandler extends Handler {
    private static final boolean DEBUG = false;
    private static final String TAG = "CameraHandler";

    private static final int MSG_PREVIEW_START = 1;
    private static final int MSG_PREVIEW_STOP = 2;
    private static final int MSG_MANUAL_FOCUS = 3;
    private static final int MSG_SWITCH_FLASH = 4;
    private static final int MSG_AUTO_FOCUS = 5;


    private int viewWidth = 0;
    private int viewHeight = 0;
    private float eventX = 0;
    private float eventY = 0;

    private CameraThread thread;

    CameraHandler(final CameraThread thread) {
        this.thread = thread;
    }

    void startPreview(final int width, final int height) {
        sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
    }

    /**
     * request to stop camera preview
     *
     * @param needWait need to wait for stopping camera preview
     */
    void stopPreview(final boolean needWait) {
        synchronized (this) {
            sendEmptyMessage(MSG_PREVIEW_STOP);
            if (thread == null) return;
            if (needWait && thread.isRunning) {
                try {
                    if (DEBUG) Log.d(TAG, "wait for terminating of camera thread");
                    wait();
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    void changeManualFocusPoint(float eventX, float eventY, int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
        this.eventX = eventX;
        this.eventY = eventY;
        sendMessage(obtainMessage(MSG_MANUAL_FOCUS));
    }

    void changeAutoFocus() {
        sendMessage(obtainMessage(MSG_AUTO_FOCUS));
    }

    void switchFlashMode() {
        sendMessage(obtainMessage(MSG_SWITCH_FLASH));
    }

    /**
     * message handler for camera thread
     */
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case MSG_PREVIEW_START:
                if (thread != null) {
                    thread.startPreview(msg.arg1, msg.arg2);
                }
                break;
            case MSG_PREVIEW_STOP:
                if (thread != null) {
                    thread.stopPreview();
                }
                synchronized (this) {
                    notifyAll();
                }
                try {
                    Looper.myLooper().quit();
                    removeCallbacks(thread);
                    removeMessages(MSG_PREVIEW_START);
                    removeMessages(MSG_PREVIEW_STOP);
                    removeMessages(MSG_MANUAL_FOCUS);
                    removeMessages(MSG_SWITCH_FLASH);
                    removeMessages(MSG_AUTO_FOCUS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                thread = null;
                break;
            case MSG_MANUAL_FOCUS:
                if (thread != null) {
                    thread.changeManualFocusPoint(eventX, eventY, viewWidth, viewHeight);
                }
                break;
            case MSG_SWITCH_FLASH:
                if (thread != null) {
                    thread.switchFlashMode();
                }
                break;
            case MSG_AUTO_FOCUS:
                if (thread != null) {
                    thread.changeAutoFocus();
                }
                break;

            default:
                throw new RuntimeException("unknown message:what=" + msg.what);
        }
    }
}

