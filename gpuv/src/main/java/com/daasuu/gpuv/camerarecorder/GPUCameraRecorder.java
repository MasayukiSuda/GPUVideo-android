package com.daasuu.gpuv.camerarecorder;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import com.daasuu.gpuv.camerarecorder.capture.MediaAudioEncoder;
import com.daasuu.gpuv.camerarecorder.capture.MediaEncoder;
import com.daasuu.gpuv.camerarecorder.capture.MediaMuxerCaptureWrapper;
import com.daasuu.gpuv.camerarecorder.capture.MediaVideoEncoder;
import com.daasuu.gpuv.egl.GlPreviewRenderer;
import com.daasuu.gpuv.egl.filter.GlFilter;


public class GPUCameraRecorder {
    private GlPreviewRenderer glPreviewRenderer;

    private final CameraRecordListener cameraRecordListener;
    private static final String TAG = "GPUCameraRecorder";

    private boolean started = false;
    private CameraHandler cameraHandler = null;
    private GLSurfaceView glSurfaceView;

    private boolean flashSupport = false;

    private MediaMuxerCaptureWrapper muxer;
    private final int fileWidth;
    private final int fileHeight;

    private final int cameraWidth;
    private final int cameraHeight;
    private final LensFacing lensFacing;
    private final boolean flipHorizontal;
    private final boolean flipVertical;
    private final boolean mute;
    private final CameraManager cameraManager;
    private final boolean isLandscapeDevice;
    private final int degrees;
    private final boolean recordNoFilter;

    GPUCameraRecorder(
            CameraRecordListener cameraRecordListener,
            final GLSurfaceView glSurfaceView,
            final int fileWidth,
            final int fileHeight,
            final int cameraWidth,
            final int cameraHeight,
            final LensFacing lensFacing,
            final boolean flipHorizontal,
            final boolean flipVertical,
            final boolean mute,
            final CameraManager cameraManager,
            final boolean isLandscapeDevice,
            final int degrees,
            final boolean recordNoFilter
    ) {


        this.cameraRecordListener = cameraRecordListener;

        glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        this.glSurfaceView = glSurfaceView;

        this.fileWidth = fileWidth;
        this.fileHeight = fileHeight;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.lensFacing = lensFacing;
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        this.mute = mute;
        this.cameraManager = cameraManager;
        this.isLandscapeDevice = isLandscapeDevice;
        this.degrees = degrees;
        this.recordNoFilter = recordNoFilter;

        // create preview Renderer
        if (null == glPreviewRenderer) {
            glPreviewRenderer = new GlPreviewRenderer(glSurfaceView);
        }

        glPreviewRenderer.setSurfaceCreateListener(new GlPreviewRenderer.SurfaceCreateListener() {
            @Override
            public void onCreated(SurfaceTexture surfaceTexture) {
                startPreview(surfaceTexture);
            }
        });
    }


    private synchronized void startPreview(SurfaceTexture surfaceTexture) {
        if (cameraHandler == null) {
            final CameraThread thread = new CameraThread(cameraRecordListener, new CameraThread.OnStartPreviewListener() {
                @Override
                public void onStart(Size previewSize, boolean flash) {

                    Log.d(TAG, "previewSize : width " + previewSize.getWidth() + " height = " + previewSize.getHeight());
                    if (glPreviewRenderer != null) {
                        glPreviewRenderer.setCameraResolution(new Size(previewSize.getWidth(), previewSize.getHeight()));
                    }

                    flashSupport = flash;
                    if (cameraRecordListener != null) {
                        cameraRecordListener.onGetFlashSupport(flashSupport);
                    }

                    final float previewWidth = previewSize.getWidth();
                    final float previewHeight = previewSize.getHeight();

                    glSurfaceView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (glPreviewRenderer != null) {
                                glPreviewRenderer.setAngle(degrees);
                                glPreviewRenderer.onStartPreview(previewWidth, previewHeight, isLandscapeDevice);
                            }
                        }
                    });

                    if (glPreviewRenderer != null) {
                        final SurfaceTexture st = glPreviewRenderer.getPreviewTexture().getSurfaceTexture();
                        st.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                    }
                }
            }, surfaceTexture, cameraManager, lensFacing);
            thread.start();
            cameraHandler = thread.getHandler();
        }
        cameraHandler.startPreview(cameraWidth, cameraHeight);
    }


    public void setFilter(final GlFilter filter) {
        if (filter == null) return;
        glPreviewRenderer.setGlFilter(filter);
    }

    /**
     * change focus
     */
    public void changeManualFocusPoint(float eventX, float eventY, int viewWidth, int viewHeight) {
        if (cameraHandler != null) {
            cameraHandler.changeManualFocusPoint(eventX, eventY, viewWidth, viewHeight);
        }
    }

    public void changeAutoFocus() {
        if (cameraHandler != null) {
            cameraHandler.changeAutoFocus();
        }
    }


    public void switchFlashMode() {
        if (!flashSupport) return;
        if (cameraHandler != null) {
            cameraHandler.switchFlashMode();
        }
    }

    public void setGestureScale(float scale) {
        if (glPreviewRenderer != null) {
            glPreviewRenderer.setGestureScale(scale);
        }
    }

    public boolean isFlashSupport() {
        return flashSupport;
    }


    private void destroyPreview() {
        if (glPreviewRenderer != null) {
            glPreviewRenderer.release();
            glPreviewRenderer = null;
        }
        if (cameraHandler != null) {
            // just request stop prviewing
            cameraHandler.stopPreview(false);
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        private boolean videoStopped;
        private boolean audioStopped;
        private boolean videoExitReady;
        private boolean audioExitReady;

        @Override
        public void onPrepared(final MediaEncoder encoder) {
            Log.v("TAG", "onPrepared:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder) {
                videoStopped = false;
                if (glPreviewRenderer != null) {
                    glPreviewRenderer.setVideoEncoder((MediaVideoEncoder) encoder);
                }
            }

            if (encoder instanceof MediaAudioEncoder) {
                audioStopped = false;
            }

        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            Log.v("TAG", "onStopped:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder) {
                videoStopped = true;
                if (glPreviewRenderer != null) {
                    glPreviewRenderer.setVideoEncoder(null);
                }
            }
            if (encoder instanceof MediaAudioEncoder) {
                audioStopped = true;
            }
        }

        @Override
        public void onExit(final MediaEncoder encoder) {
            if (encoder instanceof MediaVideoEncoder && videoStopped) {
                videoExitReady = true;
            }
            if (encoder instanceof MediaAudioEncoder && audioStopped) {
                audioExitReady = true;
            }
            if (videoExitReady && (audioExitReady || mute)) {
                cameraRecordListener.onVideoFileReady();
            }
        }
    };

    /**
     * Start data processing
     */
    public void start(final String filePath) {
        if (started) return;


        new Handler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    muxer = new MediaMuxerCaptureWrapper(filePath);

                    // for video capturing
                    // ここにcamera width , heightもわたす。
                    // 差分をいろいろと吸収する。
                    new MediaVideoEncoder(
                            muxer,
                            mediaEncoderListener,
                            fileWidth,
                            fileHeight,
                            flipHorizontal,
                            flipVertical,
                            glSurfaceView.getMeasuredWidth(),
                            glSurfaceView.getMeasuredHeight(),
                            recordNoFilter,
                            glPreviewRenderer.getFilter()
                    );
                    if (!mute) {
                        // for audio capturing
                        new MediaAudioEncoder(muxer, mediaEncoderListener);
                    }
                    muxer.prepare();
                    muxer.startRecording();

                    if (cameraRecordListener != null) {
                        cameraRecordListener.onRecordStart();
                    }
                } catch (Exception e) {
                    notifyOnError(e);
                }

            }
        });

        started = true;
    }

    /**
     * Stops capturing.
     */
    public void stop() {
        if (!started) return;
        try {

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    // stop recording and release camera
                    try {
                        // stop the recording
                        if (muxer != null) {
                            muxer.stopRecording();
                            muxer = null;
                            //  you should not wait here
                        }
                    } catch (Exception e) {
                        // RuntimeException is thrown when stop() is called immediately after start().
                        // In this case the output file is not properly constructed ans should be deleted.
                        Log.d("TAG", "RuntimeException: stop() is called immediately after start()");
                        //noinspection ResultOfMethodCallIgnored
                        notifyOnError(e);
                    }

                    notifyOnDone();
                }
            });

        } catch (Exception e) {
            notifyOnError(e);
            e.printStackTrace();
        }


        started = false;
    }

    public void release() {
        // destroy everithing
        try {
            // stop the recording
            if (muxer != null) {
                muxer.stopRecording();
                muxer = null;
            }
        } catch (Exception e) {
            // RuntimeException is thrown when stop() is called immediately after start().
            // In this case the output file is not properly constructed ans should be deleted.
            Log.d("TAG", "RuntimeException: stop() is called immediately after start()");
        }

        destroyPreview();
    }


    public boolean isStarted() {
        return started;
    }

    private void notifyOnDone() {
        if (cameraRecordListener == null) return;
        cameraRecordListener.onRecordComplete();
    }

    private void notifyOnError(Exception e) {
        if (cameraRecordListener == null) return;
        cameraRecordListener.onError(e);
    }


}

