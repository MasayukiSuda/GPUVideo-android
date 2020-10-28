package com.daasuu.gpuv.camerarecorder.capture;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;



public abstract class MediaEncoder implements Runnable {
    private final String TAG = getClass().getSimpleName();

    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);

        void onStopped(MediaEncoder encoder);

        void onExit(MediaEncoder encoder);
    }

    protected final Object sync = new Object();
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean isCapturing;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    protected int requestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean requestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean isEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean muxerStarted;
    /**
     * Track Number
     */
    protected int trackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mediaCodec;
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected final WeakReference<MediaMuxerCaptureWrapper> weakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo bufferInfo;

    protected final MediaEncoderListener listener;

    MediaEncoder(final MediaMuxerCaptureWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
        if (muxer == null) throw new NullPointerException("MediaMuxerCaptureWrapper is null");
        weakMuxer = new WeakReference<MediaMuxerCaptureWrapper>(muxer);
        muxer.addEncoder(this);
        this.listener = listener;
        synchronized (sync) {
            // create BufferInfo here for effectiveness(to reduce GC)
            bufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            new Thread(this, getClass().getSimpleName()).start();
            try {
                sync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }

    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
        synchronized (sync) {
            if (!isCapturing || requestStop) {
                return false;
            }
            requestDrain++;
            sync.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
//		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        synchronized (sync) {
            requestStop = false;
            requestDrain = 0;
            sync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (sync) {
                localRequestStop = requestStop;
                localRequestDrain = (requestDrain > 0);
                if (localRequestDrain)
                    requestDrain--;
            }
            if (localRequestStop) {
                drain();
                // request stop recording
                signalEndOfInputStream();
                // process output data again for EOS signale
                drain();
                // release all related objects
                release();
                break;
            }
            if (localRequestDrain) {
                drain();
            } else {
                synchronized (sync) {
                    try {
                        sync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        Log.d(TAG, "Encoder thread exiting");
        synchronized (sync) {
            requestStop = true;
            isCapturing = false;
        }
        listener.onExit(this);
    }

    /*
     * prepareing method for each sub class
     * this method should be implemented in sub class, so set this as abstract method
     * @throws IOException
     */
    abstract void prepare() throws IOException;

    void startRecording() {
        Log.v(TAG, "startRecording");
        synchronized (sync) {
            isCapturing = true;
            requestStop = false;
            sync.notifyAll();
        }
    }

    /**
     * the method to request stop encoding
     */
    void stopRecording() {
        Log.v(TAG, "stopRecording");
        synchronized (sync) {
            if (!isCapturing || requestStop) {
                return;
            }
            requestStop = true;    // for rejecting newer frame
            sync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

//********************************************************************************
//********************************************************************************

    /**
     * Release all releated objects
     */
    protected void release() {
        Log.d(TAG, "release:");
        try {
            listener.onStopped(this);
        } catch (final Exception e) {
            Log.e(TAG, "failed onStopped", e);
        }
        isCapturing = false;
        if (mediaCodec != null) {
            try {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            } catch (final Exception e) {
                Log.e(TAG, "failed releasing MediaCodec", e);
            }
        }
        if (muxerStarted) {
            final MediaMuxerCaptureWrapper muxer = weakMuxer != null ? weakMuxer.get() : null;
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (final Exception e) {
                    Log.e(TAG, "failed stopping muxer", e);
                }
            }
        }
        bufferInfo = null;
    }

    protected void signalEndOfInputStream() {
        Log.d(TAG, "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!isCapturing) return;
        while (isCapturing) {
            final int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }

                if (length <= 0) {
                    // send EOS
                    isEOS = true;
                    Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    private void drain() {
        if (mediaCodec == null) return;
        int encoderStatus, count = 0;
        final MediaMuxerCaptureWrapper muxer = weakMuxer.get();
        if (muxer == null) {
            Log.w(TAG, "muxer is unexpectedly null");
            return;
        }
        LOOP:
        while (isCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!isEOS) {
                    if (++count > 5)
                        break LOOP;        // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (muxerStarted) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                final MediaFormat format = mediaCodec.getOutputFormat(); // API >= 16
                trackIndex = muxer.addTrack(format);
                muxerStarted = true;
                if (!muxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (muxer) {
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status
                Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = mediaCodec.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!muxerStarted) {
                        // muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    bufferInfo.presentationTimeUs = getPTSUs();
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    isCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }
}

