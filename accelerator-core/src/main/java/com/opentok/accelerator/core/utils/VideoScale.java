package com.opentok.accelerator.core.utils;

import com.opentok.accelerator.core.wrapper.OTWrapper;
import com.opentok.android.BaseVideoRenderer;

/**
 * Defines values for the {@link OTWrapper#setLocalStyle(VideoScale)} and
 * {@link OTWrapper#setRemoteVideoRenderer(BaseVideoRenderer)} methods.
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
