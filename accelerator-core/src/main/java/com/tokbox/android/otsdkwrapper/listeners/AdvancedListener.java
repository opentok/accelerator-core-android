package com.tokbox.android.otsdkwrapper.listeners;

import com.opentok.android.OpentokError;

/**
 * Monitors advanced state changes in the OpenTok communication.
 *
 * @see com.tokbox.android.otsdkwrapper.wrapper.OTWrapper#addAdvancedListener(AdvancedListener) method
 */
public interface AdvancedListener<Wrapper> extends BaseOTListener {

    /**
     * Invoked when the camera of the device changed
     * @param wrapper
     * @throws ListenerException
     */
    void onCameraChanged(Wrapper wrapper) throws ListenerException;

    /**
     * Invoked when the local client has lost its connection to an OpenTok communication and is trying
     * to reconnect.
     * @param wrapper
     * @throws ListenerException
     */
    void onReconnecting(Wrapper wrapper) throws ListenerException;

    /**
     * Invoked when the local client has reconnected to the OpenTok communication
     * to reconnect.
     * @param wrapper
     * @throws ListenerException
     */
    void onReconnected(Wrapper wrapper) throws ListenerException;

    /**
     * Invoked when stream quality has degraded and the video will be disabled if the quality degrades further.
     * @param wrapper
     * @param remoteId
     * @throws ListenerException
     */
    void onVideoQualityWarning(Wrapper wrapper, String remoteId) throws ListenerException;

    /**
     * Invoked when the stream quality has improved to the point at which the video being disabled is not an
     * immediate risk.
     * @param wrapper
     * @param remoteId
     * @throws ListenerException
     */
    void onVideoQualityWarningLifted(Wrapper wrapper, String remoteId) throws ListenerException;

    /**
     * Invoked when an error in the communication happened
     * @param wrapper
     * @param error
     * @throws ListenerException
     */
    void onError(Wrapper wrapper, OpentokError error) throws ListenerException;
}
