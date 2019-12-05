package com.daasuu.gpuv.egl;

import android.graphics.Bitmap;

import androidx.annotation.UiThread;

public interface ScreenshotConsumer {
    @UiThread
    void accept(Bitmap bitmap);
}
