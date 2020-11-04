package com.tokbox.otsdkwrapper.utils;

/**
 * Defines the Media type
 */
public enum MediaType {
    /**
     * Defines the media type for video
     */
    VIDEO(0),
    /**
     * Defines the media type for audio
     */
    AUDIO(1);

    private final int value;

    MediaType(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }

}