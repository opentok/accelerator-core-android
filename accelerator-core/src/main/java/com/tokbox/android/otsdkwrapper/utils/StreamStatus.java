package com.tokbox.android.otsdkwrapper.utils;


import android.view.View;

import com.opentok.android.Stream;

/**
 * Defines the properties of the Stream
 */
public class StreamStatus {

    private final View mView;
    // Stream status
    private boolean mHasAudio;
    private boolean mHasVideo;
    // Status of the container of the stream (publisher/subscriber). This is if the publisher
    // is publishing or the subscriber is subscribing.
    private boolean mContainerAudioStatus;
    private boolean mContainerVideoStatus;
    private StreamType mType;
    private int mWidth;
    private int mHeight;

    /**
     * Defines the type of the stream
     */
    public enum StreamType {
        /**
         * Defines the Camera type
         */
        CAMERA(0),
        /**
         * Defines the Screen type
         */
        SCREEN(1);

        private final int value;

        StreamType(int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Creates a new StreamStatus instance
     * @param streamView
     * @param containerAudio
     * @param containerVideo
     * @param hasAudio
     * @param hasVideo
     * @param type
     * @param width
     * @param height
     */
    public StreamStatus(View streamView, boolean containerAudio, boolean containerVideo,
                        boolean hasAudio, boolean hasVideo, Stream.StreamVideoType type, int width, int height) {
        mView = streamView;
        mHasAudio = hasAudio;
        mHasVideo = hasVideo;
        mContainerAudioStatus = containerAudio;
        mContainerVideoStatus = containerVideo;
        mWidth = width;
        mHeight = height;
        if ( type == Stream.StreamVideoType.StreamVideoTypeCamera) {
            mType = StreamType.CAMERA;
        }
        else {
            if ( type == Stream.StreamVideoType.StreamVideoTypeScreen) {
                mType = StreamType.SCREEN;
            }
        }
    }

    /**
     * Returns the audio/video status for the stream
     * @param type
     * @return Whether to have audio/video (<code>true</code>) or not (
     *                     <code>false</code>).
     */
    public boolean has(MediaType type) {
        return type == MediaType.VIDEO ? mHasVideo : mHasAudio;
    }

    /**
     * Returns the view for the stream
     * @return the stream view
     */
    public View getView() {
        return mView;
    }

    /**
     * Returns the type of the stream
     * @return the type of the stream
     */
    public StreamType getType() {
        return mType;
    }

    /**
     * Returns the width of the stream
     * @return the width of the stream
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the height of the stream
     * @return the height of the stream
     */
    public int getHeight() {
        return mHeight;
    }

    public boolean subscribedTo(MediaType type) {
        return type == MediaType.VIDEO ? mContainerVideoStatus : mContainerAudioStatus;
    }

    public void setHas(MediaType type, boolean value) {
        if (type == MediaType.VIDEO) {
            mHasVideo = value;
        } else {
            mHasAudio = value;
        }
    }

    public void setContainerStatus(MediaType type, boolean value) {
        if (type == MediaType.VIDEO) {
            mContainerVideoStatus = value;
        } else {
            mContainerAudioStatus = value;
        }
    }


    @Override
    public String toString() {
        return "hasAudio: " + mHasAudio + ", hasVideo: " + mHasVideo + ", containerAudio: " +
                mContainerAudioStatus + ", containerVideo: " + mContainerVideoStatus;
    }
}