package com.daasuu.gpuv.camerarecorder.capture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Process;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioMeter extends Thread {
    /////////////////////////////////////////////////////////////////
    // PUBLIC CONSTANTS

    // Convenience constants
    public static final int AMP_SILENCE = 0;
    public static final int AMP_NORMAL_BREATHING = 10;
    public static final int AMP_MOSQUITO = 20;
    public static final int AMP_WHISPER = 30;
    public static final int AMP_STREAM = 40;
    public static final int AMP_QUIET_OFFICE = 50;
    public static final int AMP_NORMAL_CONVERSATION = 60;
    public static final int AMP_HAIR_DRYER = 70;
    public static final int AMP_GARBAGE_DISPOSAL = 80;

    /////////////////////////////////////////////////////////////////
    // PRIVATE CONSTANTS

    private static final float MAX_REPORTABLE_AMP = 32767f;
    public static final float MAX_REPORTABLE_DB = 90.3087f;

    /////////////////////////////////////////////////////////////////
    // PRIVATE MEMBERS

    private AudioRecord mAudioRecord;
    private int mSampleRate;
    private short mAudioFormat;
    private short mChannelConfig;

    private short[] mBuffer;
    private int mBufferSize = AudioRecord.ERROR_BAD_VALUE;

    private boolean isRecording = false;

    /////////////////////////////////////////////////////////////////
    // CONSTRUCTOR

    private AudioMeter() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        createAudioRecord();
    }

    /////////////////////////////////////////////////////////////////
    // PUBLIC METHODS

    public static AudioMeter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public float getAmplitude() {
        return (float) (MAX_REPORTABLE_DB + (20 * Math.log10(getRawAmplitude() / MAX_REPORTABLE_AMP)));
    }

    public void setBuffer(ByteBuffer buffer) {
        mBuffer = shortArrayFromByteBuffer(buffer);
    }



    public synchronized void startRecording() {
        if (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            createAudioRecord();
        }

        if (mAudioRecord != null) {
            mAudioRecord.startRecording();
            isRecording = true;
        }
    }

    public synchronized void stopRecording() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;

        }
        mSampleRate = 0;
        mAudioFormat = 0;
        mChannelConfig = 0;
        isRecording = false;
    }

    /////////////////////////////////////////////////////////////////
    // PRIVATE METHODS

    private void createAudioRecord() {
        if (mSampleRate > 0 && mAudioFormat > 0 && mChannelConfig > 0) {
            mAudioRecord = new AudioRecord(AudioSource.MIC, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize);

            return;
        }

        // Find best/compatible AudioRecord
        for (int sampleRate : new int[] { 8000, 11025, 16000, 22050, 32000, 44100, 47250, 48000 }) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.CHANNEL_CONFIGURATION_STEREO }) {

                    // Try to initialize
                    try {
                        mBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

                        if (mBufferSize < 0) {
                            continue;
                        }

                        mBuffer = new short[mBufferSize];
                        mAudioRecord = new AudioRecord(AudioSource.MIC, sampleRate, channelConfig, audioFormat,
                                mBufferSize);

                        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                            mSampleRate = sampleRate;
                            mAudioFormat = audioFormat;
                            mChannelConfig = channelConfig;

                            return;
                        }

                        mAudioRecord.release();
                        mAudioRecord = null;
                    }
                    catch (Exception e) {
                        // Do nothing
                    }
                }
            }
        }
    }

    private int getRawAmplitude() {
        if (isRecording && mAudioRecord != null) {
            mAudioRecord.read(mBuffer, 0, mBufferSize);
        }
        return getRawAmplitude(mBuffer);
    }

    private int getRawAmplitude(ByteBuffer buff) {

        short[] buffer = shortArrayFromByteBuffer(buff);
        return getRawAmplitude(buffer);
    }

    private int getRawAmplitude(short[] buffer) {
        if (buffer.length < 0) {
            return 0;
        }

        int sum = 0;
        for (int i = 0; i < buffer.length; i++) {
            sum += Math.abs(buffer[i]);
        }

        if (buffer.length > 0) {
            return sum / buffer.length;
        }

        return 0;
    }

    public short[] shortArrayFromByteBuffer(ByteBuffer buffer) {
        int size = buffer.capacity();
        short[] shortArray = new short[size/2];
        buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
        return shortArray;
    }

    /////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES

    private static class InstanceHolder {
        private static final AudioMeter INSTANCE = new AudioMeter();
    }
}