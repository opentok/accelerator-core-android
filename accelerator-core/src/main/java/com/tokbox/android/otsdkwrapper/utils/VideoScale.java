package com.tokbox.android.otsdkwrapper.utils;

import com.opentok.android.BaseVideoRenderer;

/**
 * Defines values for the {@link com.tokbox.android.otsdkwrapper.wrapper.OTWrapper#setLocalStyle(VideoScale)} and
 * {@link com.tokbox.android.otsdkwrapper.wrapper.OTWrapper#setRemoteVideoRenderer(BaseVideoRenderer)} methods.
 */
public enum VideoScale {

    /**
     * Defines the FILL mode setting for the renderer
     */
    FILL(0),
    /**
     * Defines the FIT mode setting for the renderer
     */
    FIT(1);

    private final int value;

    VideoScale(int v) {
        value = v;
    }

    public int getValue() {
        return value;
    }

}
