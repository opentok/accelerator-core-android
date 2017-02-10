package com.tokbox.android.otsdkwrapper.utils;


import android.view.View;
import android.view.ViewGroup;

import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Publisher;
import com.tokbox.android.otsdkwrapper.GlobalLogLevel;

/**
 * Defines the configuration of the local preview
 */
public class PreviewConfig {
    private static final String LOG_TAG = PreviewConfig.class.getSimpleName();
    private static final short LOCAL_LOG_LEVEL = 0xFF;
    private static final LogWrapper LOG =
      new LogWrapper((short)(GlobalLogLevel.sMaxLogLevel & LOCAL_LOG_LEVEL));

    public static void setLogLevel(short logLevel) {
        LOG.setLogLevel(logLevel);
    }

    String name=""; //optinal
    boolean audioTrack = true; //optional
    boolean videoTrack = true; //optional
    Publisher.CameraCaptureResolution resolution = Publisher.CameraCaptureResolution.MEDIUM; //optional
    Publisher.CameraCaptureFrameRate frameRate = Publisher.CameraCaptureFrameRate.FPS_15; //optional
    BaseVideoCapturer capturer; //optional
    BaseVideoRenderer renderer; //optional

    /**
     * Creates a new PreviewConfig instance using a builder pattern
     * @param builder
     */
    public PreviewConfig(PreviewConfigBuilder builder) {
        this.name = builder.name;
        this.audioTrack = builder.audioTrack;
        this.videoTrack = builder.videoTrack;
        this.resolution = builder.resolution;
        this.frameRate = builder.frameRate;
        this.capturer = builder.capturer;
        this.renderer = builder.renderer;
    }

    /**
     * Returns the name of the local preview
     * @return the name of the local preview
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the audio track status
     * @return Whether to have audio track (<code>true</code>) or not (
     *                     <code>false</code>).
     */
    public boolean isAudioTrack() {
        return audioTrack;
    }

    /**
     * Returns the video track status
     * @return Whether to have video track (<code>true</code>) or not (
     *                     <code>false</code>).
     */
    public boolean isVideoTrack() {
        return videoTrack;
    }

    /**
     * Returns the video resolution
     * @return the video resolution
     */
    public Publisher.CameraCaptureResolution getResolution() {
        return resolution;
    }

    /**
     * Returns the video framerate
     * @return the video framerate
     */
    public Publisher.CameraCaptureFrameRate getFrameRate() {
        return frameRate;
    }

    /**
     * Returns the video capturer
     * @return the video capturer
     */
    public BaseVideoCapturer getCapturer() {
        return capturer;
    }

    /**
     * Returns the video renderer
     * @return the video renderer
     */
    public BaseVideoRenderer getRenderer() {
        return renderer;
    }

    /**
     * Sets the video framerate
     * @param newFrameRate
     */
    public void setFrameRate(Publisher.CameraCaptureFrameRate newFrameRate) {
        frameRate = newFrameRate;
    }

    /**
     * Defines the PreviewConfig builder
     */
    public static class PreviewConfigBuilder {
        String name=""; //optinal
        boolean audioTrack = true; //optional
        boolean videoTrack = true; //optional
        Publisher.CameraCaptureResolution resolution = Publisher.CameraCaptureResolution.MEDIUM; //optional
        Publisher.CameraCaptureFrameRate frameRate = Publisher.CameraCaptureFrameRate.FPS_15; //optional
        VideoScale videoScale; //optional
        BaseVideoCapturer capturer; //optional
        BaseVideoRenderer renderer; //optional

        public PreviewConfigBuilder() { }

        public PreviewConfigBuilder name(String name) {
            if ( name == null ){
                throw new RuntimeException("Name cannot be null");
            }
            this.name = name;
            return this;
        }

        public PreviewConfigBuilder audioTrack(boolean audioTrack) {
            this.audioTrack = audioTrack;
            return this;
        }

        public PreviewConfigBuilder videoTrack(boolean videoTrack) {
            this.videoTrack = videoTrack;
            return this;
        }

        public PreviewConfigBuilder resolution(Publisher.CameraCaptureResolution cameraResolution) {
            this.resolution = cameraResolution;
            return this;
        }

        public PreviewConfigBuilder framerate(Publisher.CameraCaptureFrameRate cameraFramerate) {
            this.frameRate = cameraFramerate;
            return this;
        }

        public PreviewConfigBuilder capturer(BaseVideoCapturer capturer){
            this.capturer = capturer;
            return this;
        }

        public PreviewConfigBuilder renderer(BaseVideoRenderer renderer){
            this.renderer = renderer;
            return this;
        }

        public PreviewConfig build() {
            PreviewConfig info = new PreviewConfig(this);
            return info;
        }
    }
}
