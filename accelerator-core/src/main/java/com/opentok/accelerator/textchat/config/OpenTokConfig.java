package com.opentok.accelerator.textchat.config;

import com.tokbox.otsdkwrapper.BuildConfig;

public class OpenTokConfig {

    public static final String LOG_CLIENT_VERSION = BuildConfig.acceleratorCoreVersion;
    public static final String LOG_COMPONENT_ID = "textChatAccPack";
    public static final String LOG_ACTION_INITIALIZE = "Init";
    public static final String LOG_ACTION_OPEN = "OpenTC";
    public static final String LOG_ACTION_CLOSE = "CloseTC";
    public static final String LOG_ACTION_SEND_MESSAGE = "SendMessage";
    public static final String LOG_ACTION_RECEIVE_MESSAGE = "ReceiveMessage";
    public static final String LOG_ACTION_SET_MAX_LENGTH = "SetMaxLength";

    public static final String LOG_VARIATION_ATTEMPT = "Attempt";
    public static final String LOG_VARIATION_ERROR = "Failure";
    public static final String LOG_VARIATION_SUCCESS = "Success";
}
