package com.daasuu.gpuv.camerarecorder;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.camera2.CameraManager;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import com.daasuu.gpuv.egl.filter.GlFilter;


public class GPUCameraRecorderBuilder {


    private GLSurfaceView glSurfaceView;

    private LensFacing lensFacing = LensFacing.FRONT;
    private Resources resources;
    private Activity activity;
    private CameraRecordListener cameraRecordListener;
    private int fileWidth = 720;
    private int fileHeight = 1280;
    private boolean flipVertical = false;
    private boolean flipHorizontal = false;
    private boolean mute = false;
    private boolean recordNoFilter = false;
    private int cameraWidth = 1280;
    private int cameraHeight = 720;
    private GlFilter glFilter;

    public GPUCameraRecorderBuilder(Activity activity, GLSurfaceView glSurfaceView) {
        this.activity = activity;
        this.glSurfaceView = glSurfaceView;
        this.resources = activity.getResources();
    }

    public GPUCameraRecorderBuilder cameraRecordListener(CameraRecordListener cameraRecordListener) {
        this.cameraRecordListener = cameraRecordListener;
        return this;
    }

    public GPUCameraRecorderBuilder filter(GlFilter glFilter) {
        this.glFilter = glFilter;
        return this;
    }

    public GPUCameraRecorderBuilder videoSize(int fileWidth, int fileHeight) {
        this.fileWidth = fileWidth;
        this.fileHeight = fileHeight;
        return this;
    }

    public GPUCameraRecorderBuilder cameraSize(int cameraWidth, int cameraHeight) {
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        return this;
    }

    public GPUCameraRecorderBuilder lensFacing(LensFacing lensFacing) {
        this.lensFacing = lensFacing;
        return this;
    }

    public GPUCameraRecorderBuilder flipHorizontal(boolean flip) {
        this.flipHorizontal = flip;
        return this;
    }

    public GPUCameraRecorderBuilder flipVertical(boolean flip) {
        this.flipVertical = flip;
        return this;
    }

    public GPUCameraRecorderBuilder mute(boolean mute) {
        this.mute = mute;
        return this;
    }

    public GPUCameraRecorderBuilder recordNoFilter(boolean recordNoFilter) {
        this.recordNoFilter = recordNoFilter;
        return this;
    }

    public GPUCameraRecorder build() {
        if (this.glSurfaceView == null) {
            throw new IllegalArgumentException("glSurfaceView and windowManager, multiVideoEffects is NonNull !!");
        }

        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        boolean isLandscapeDevice = resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int degrees = 0;
        if (isLandscapeDevice) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Log.d("GPUCameraRecorder", "Surface.ROTATION_90 = " + Surface.ROTATION_90 + " rotation = " + rotation);
            degrees = 90 * (rotation - 2);
        }

        GPUCameraRecorder GPUCameraRecorder = new GPUCameraRecorder(
                cameraRecordListener,
                glSurfaceView,
                fileWidth,
                fileHeight,
                cameraWidth,
                cameraHeight,
                lensFacing,
                flipHorizontal,
                flipVertical,
                mute,
                cameraManager,
                isLandscapeDevice,
                degrees,
                recordNoFilter
        );

        GPUCameraRecorder.setFilter(glFilter);
        activity = null;
        resources = null;
        return GPUCameraRecorder;
    }

}
