package com.daasuu.gpuv.composer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.util.Size;
import com.daasuu.gpuv.egl.filter.GlFilter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GPUMp4Composer {

    private final static String TAG = GPUMp4Composer.class.getSimpleName();

    private Context context;
    private final String srcPath;
    private final String destPath;
    private GlFilter filter;
    private Size outputResolution;
    private int bitrate = -1;
    private boolean mute = false;
    private Rotation rotation = Rotation.NORMAL;
    private Listener listener;
    private FillMode fillMode = FillMode.PRESERVE_ASPECT_FIT;
    private FillModeCustomItem fillModeCustomItem;
    private int timeScale = 1;
    private boolean flipVertical = false;
    private boolean flipHorizontal = false;

    private ExecutorService executorService;


    public GPUMp4Composer(final String srcPath, final String destPath) {
        this.srcPath = srcPath;
        this.destPath = destPath;
    }

    public GPUMp4Composer(final Context context, final String srcPath, final String destPath) {
        this.context = context;
        this.srcPath = srcPath;
        this.destPath = destPath;
    }

    public GPUMp4Composer filter(GlFilter filter) {
        this.filter = filter;
        return this;
    }

    public GPUMp4Composer size(int width, int height) {
        this.outputResolution = new Size(width, height);
        return this;
    }

    public GPUMp4Composer videoBitrate(int bitrate) {
        this.bitrate = bitrate;
        return this;
    }

    public GPUMp4Composer mute(boolean mute) {
        this.mute = mute;
        return this;
    }

    public GPUMp4Composer flipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
        return this;
    }

    public GPUMp4Composer flipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
        return this;
    }

    public GPUMp4Composer rotation(Rotation rotation) {
        this.rotation = rotation;
        return this;
    }

    public GPUMp4Composer fillMode(FillMode fillMode) {
        this.fillMode = fillMode;
        return this;
    }

    public GPUMp4Composer customFillMode(FillModeCustomItem fillModeCustomItem) {
        this.fillModeCustomItem = fillModeCustomItem;
        this.fillMode = FillMode.CUSTOM;
        return this;
    }


    public GPUMp4Composer listener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public GPUMp4Composer timeScale(final int timeScale) {
        this.timeScale = timeScale;
        return this;
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }


    public GPUMp4Composer start() {
        getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                GPUMp4ComposerEngine engine = new GPUMp4ComposerEngine();

                engine.setProgressCallback(new GPUMp4ComposerEngine.ProgressCallback() {
                    @Override
                    public void onProgress(final double progress) {
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                    }
                });

                final File srcFile = new File(srcPath);
                final FileInputStream fileInputStream;
                try {
                    if (srcPath.contains("content:/")) {
                        fileInputStream = (FileInputStream) context.getContentResolver().openInputStream(Uri.parse(srcPath));
                    } else {
                        fileInputStream = new FileInputStream(srcFile);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    return;
                } catch (NullPointerException e) {
                    Log.e(TAG, "Must have a context when use ScopedStorage");
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    return;
                }

                FileDescriptor fileDescriptor;
                try {
                    fileDescriptor = fileInputStream.getFD();
                    engine.setDataSource(fileDescriptor);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    return;
                }

                final int videoRotate = getVideoRotation(fileDescriptor);
                final Size srcVideoResolution = getVideoResolution(fileDescriptor, videoRotate);

                if (filter == null) {
                    filter = new GlFilter();
                }

                if (fillMode == null) {
                    fillMode = FillMode.PRESERVE_ASPECT_FIT;
                }

                if (fillModeCustomItem != null) {
                    fillMode = FillMode.CUSTOM;
                }

                if (outputResolution == null) {
                    if (fillMode == FillMode.CUSTOM) {
                        outputResolution = srcVideoResolution;
                    } else {
                        Rotation rotate = Rotation.fromInt(rotation.getRotation() + videoRotate);
                        if (rotate == Rotation.ROTATION_90 || rotate == Rotation.ROTATION_270) {
                            outputResolution = new Size(srcVideoResolution.getHeight(), srcVideoResolution.getWidth());
                        } else {
                            outputResolution = srcVideoResolution;
                        }
                    }
                }
//                if (filter instanceof IResolutionFilter) {
//                    ((IResolutionFilter) filter).setResolution(outputResolution);
//                }

                if (timeScale < 2) {
                    timeScale = 1;
                }

                Log.d(TAG, "rotation = " + (rotation.getRotation() + videoRotate));
                Log.d(TAG, "inputResolution width = " + srcVideoResolution.getWidth() + " height = " + srcVideoResolution.getHeight());
                Log.d(TAG, "outputResolution width = " + outputResolution.getWidth() + " height = " + outputResolution.getHeight());
                Log.d(TAG, "fillMode = " + fillMode);

                try {
                    if (bitrate < 0) {
                        bitrate = calcBitRate(outputResolution.getWidth(), outputResolution.getHeight());
                    }
                    engine.compose(
                            destPath,
                            outputResolution,
                            filter,
                            bitrate,
                            mute,
                            Rotation.fromInt(rotation.getRotation() + videoRotate),
                            srcVideoResolution,
                            fillMode,
                            fillModeCustomItem,
                            timeScale,
                            flipVertical,
                            flipHorizontal
                    );

                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onFailed(e);
                    }
                    executorService.shutdown();
                    return;
                }

                if (listener != null) {
                    listener.onCompleted();
                }
                executorService.shutdown();
            }
        });

        return this;
    }

    public void cancel() {
        getExecutorService().shutdownNow();
    }


    public interface Listener {
        /**
         * Called to notify progress.
         *
         * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
         */
        void onProgress(double progress);

        /**
         * Called when transcode completed.
         */
        void onCompleted();

        /**
         * Called when transcode canceled.
         */
        void onCanceled();


        void onFailed(Exception exception);
    }

    private int getVideoRotation(FileDescriptor fileDescriptor) {
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(fileDescriptor);
            String orientation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            return Integer.valueOf(orientation);
        } catch (IllegalArgumentException e) {
            Log.e("MediaMetadataRetriever", "getVideoRotation IllegalArgumentException");
            return 0;
        } catch (RuntimeException e) {
            Log.e("MediaMetadataRetriever", "getVideoRotation RuntimeException");
            return 0;
        } catch (Exception e) {
            Log.e("MediaMetadataRetriever", "getVideoRotation Exception");
            return 0;
        } finally {
            try {
                if (mediaMetadataRetriever != null) {
                    mediaMetadataRetriever.release();
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }

    private int calcBitRate(int width, int height) {
        final int bitrate = (int) (0.25 * 30 * width * height);
        Log.i(TAG, "bitrate=" + bitrate);
        return bitrate;
    }

    private Size getVideoResolution(final FileDescriptor fileDescriptor, final int rotation) {
        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor);
            int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

            return new Size(width, height);
        } finally {
            try {
                if (retriever != null) {
                    retriever.release();
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to release mediaMetadataRetriever.", e);
            }
        }
    }

}
