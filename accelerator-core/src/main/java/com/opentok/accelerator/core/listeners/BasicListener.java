package com.opentok.accelerator.core.listeners;


import android.view.View;
import com.opentok.accelerator.core.wrapper.OTWrapper;
import com.opentok.android.OpentokError;

/**
 * Monitors basic state changes in the OpenTok communication.
 *
 * @see OTWrapper#addBasicListener(BasicListener) method
 */
public interface BasicListener<Wrapper> extends BaseOTListener {

    /**
     * Invoked when there is a new connection (participant) in the communication, included our own connection
     *
     * @param wrapper
     * @param participantsCount
     * @param connectionId
     * @param data
     * @throws ListenerException
     */
    void onConnected(Wrapper wrapper, int participantsCount, String connectionId, String data)
            throws ListenerException;

    /**
     * Invoked when a connection left the communication, included our own connection
     *
     * @param wrapper
     * @param participantsCount
     * @param connectionId
     * @param data
     * @throws ListenerException
     */
    void onDisconnected(Wrapper wrapper, int participantsCount, String connectionId, String data)
            throws ListenerException;

    /**
     * Invoked when the local preview view is ready to be attached
     * @param wrapper
     * @param localView
     * @throws ListenerException
     */
    void onPreviewViewReady(Wrapper wrapper, View localView) throws ListenerException;

    /**
     * Invoked when the local preview view has been destroyed
     *
     * @param wrapper
     * @throws ListenerException
     */
    void onPreviewViewDestroyed(Wrapper wrapper) throws ListenerException;

    /**
     * Invoke when the remote view is ready to be attached
     * @param wrapper
     * @param remoteView
     * @param remoteId
     * @param data
     * @throws ListenerException
     */
    void onRemoteViewReady(Wrapper wrapper, View remoteView, String remoteId, String data) throws ListenerException;

    /**
     * Invoked when the remote view has been destroyed
     *
     * @param wrapper
     * @param remoteId
     * @throws ListenerException
     */
    void onRemoteViewDestroyed(Wrapper wrapper, String remoteId) throws ListenerException;

    /**
     * Invoked when the local camera or screensharing streaming started
     * @param wrapper
     * @param screensharing
     * @throws ListenerException
     */
    void onStartedPublishingMedia(Wrapper wrapper, boolean screensharing) throws ListenerException;

    /**
     * Invoked when the local camera or screensharing streaming stopped
     * @param wrapper
     * @param screensharing
     * @throws ListenerException
     */
    void onStoppedPublishingMedia(Wrapper wrapper, boolean screensharing) throws ListenerException;

    /**
     * Invoked when a new remote participant joined and started to share video/screen.
     * @param wrapper
     * @param remoteId
     * @throws ListenerException
     */
    void onRemoteJoined(Wrapper wrapper, String remoteId) throws ListenerException;

    /**
     * Invoked when a remote participant stopped to share video/screen
     * @param wrapper
     * @param remoteId
     * @throws ListenerException
     */
    void onRemoteLeft(Wrapper wrapper, String remoteId) throws ListenerException;

    /**
     * Invoked when a remote participant video changed by different reasons. Eg: network quality,...
     * @param wrapper
     * @param remoteId
     * @param reason
     * @param videoActive
     * @param subscribed
     * @throws ListenerException
     */
    void onRemoteVideoChanged(Wrapper wrapper, String remoteId, String reason, boolean videoActive,
                              boolean subscribed)
            throws ListenerException;

    /**
     * Invoked when an error happened
     * @param wrapper
     * @param error
     * @throws ListenerException
     */
    void onError(Wrapper wrapper, OpentokError error) throws ListenerException;
}
