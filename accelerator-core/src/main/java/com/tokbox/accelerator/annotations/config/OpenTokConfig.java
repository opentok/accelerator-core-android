package com.tokbox.accelerator.annotations.config;

import com.tokbox.otsdkwrapper.BuildConfig;

public class OpenTokConfig {

    public static final String LOG_CLIENT_VERSION = BuildConfig.acceleratorCoreVersion;
    public static final String LOG_COMPONENT_ID = "annotationsAccPack";
    public static final String LOG_ACTION_INITIALIZE = "Init";
    public static final String LOG_ACTION_DESTROY = "Destroy";
    public static final String LOG_ACTION_FREEHAND = "FreeHand";
    public static final String LOG_ACTION_START_DRAWING = "StartDrawing";
    public static final String LOG_ACTION_END_DRAWING = "EndDrawing";
    public static final String LOG_ACTION_PICKER_COLOR = "PickerColor";
    public static final String LOG_ACTION_TEXT = "Text";
    public static final String LOG_ACTION_SCREEN_CAPTURE = "ScreenCapture";
    public static final String LOG_ACTION_ERASE = "Erase";
    public static final String LOG_ACTION_DONE = "DONE";
    public static final String LOG_ACTION_USE_TOOLBAR = "UseToolbar";

    public static final String LOG_VARIATION_ATTEMPT = "Attempt";
    public static final String LOG_VARIATION_ERROR = "Failure";
    public static final String LOG_VARIATION_SUCCESS = "Success";
}
