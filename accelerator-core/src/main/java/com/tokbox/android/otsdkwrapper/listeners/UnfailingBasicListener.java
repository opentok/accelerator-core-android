package com.tokbox.android.otsdkwrapper.listeners;

import android.util.Log;
import android.view.View;

import com.opentok.android.OpentokError;

public class UnfailingBasicListener<Wrapper> implements RetriableBasicListener<Wrapper> {

    private final static String LOG_TAG = UnfailingBasicListener.class.getSimpleName();

    private BasicListener mInternalListener = null;


    public UnfailingBasicListener(BasicListener internaListener) {
        mInternalListener = internaListener;
    }

    @Override
    public BasicListener getInternalListener() {
        return mInternalListener;
    }

    @Override
    public void resume() {}


    /**
     * Called when a new connection (including our own) is detected. The first parameter will be the
     * number of current participants on the session, including ourselves.
     *
     * @param wrapper Object that emmited this event
     * @param participantsNumber Number of current participants on the conference/session.
     * @param connId             Identifier of the new connection
     * @param data               Connection data as included by the server
     */
    @Override
    public void onConnected(Wrapper wrapper, int participantsNumber, String connId, String data) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onConnected(wrapper, participantsNumber, connId, data);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onConnected Exception: ", e);
        }
    }

    /**
     * Called when a participant disconnects from the session/conference. The first parameter will
     * be the number of current participants on the session, including ourselves.
     *
     * @param wrapper Object that emits this event
     * @param participantsNumber Number of current participants on the conference/session.
     * @param connId             Identifier of the new connection
     * @param data               Connection data as included by the server
     */
    @Override
    public void onDisconnected(Wrapper wrapper, int participantsNumber, String connId, String data) {
        try {
            if(mInternalListener != null) {
                mInternalListener.onDisconnected(wrapper, participantsNumber, connId, data);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onDisconnected Exception: ", e);
        }
    }

    @Override
    public void onStartedPublishingMedia(Wrapper wrapper, boolean screensharing) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onStartedPublishingMedia(wrapper, screensharing);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onStartedSharingMedia Exception: ", e);
        }
    }

    @Override
    public void onStoppedPublishingMedia(Wrapper wrapper, boolean screensharing) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onStoppedPublishingMedia(wrapper, screensharing);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onStoppedSharingMedia Exception: ", e);
        }
    }

    @Override
    public void onPreviewViewReady(Wrapper wrapper, View localView) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onPreviewViewReady(wrapper, localView);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onPreviewViewReady Exception: ", e);
        }
    }

    @Override
    public void onPreviewViewDestroyed(Wrapper wrapper, View localView) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onPreviewViewDestroyed(wrapper, localView);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onPreviewViewDestroyed Exception: ", e);
        }
    }

    @Override
    public void onRemoteViewReady(Wrapper wrapper, View remoteView, String remoteId, String data) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onRemoteViewReady(wrapper, remoteView, remoteId, data);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onRemoteViewReady Exception: ", e);
        }
    }

    @Override
    public void onRemoteViewDestroyed(Wrapper wrapper, View remoteView, String remoteId) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onRemoteViewDestroyed(wrapper, remoteView, remoteId);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onRemoteViewDestroyed Exception: ", e);
        }
    }

    @Override
    public void onRemoteJoined(Wrapper wrapper, String remoteId)  {
        try {
            if (mInternalListener != null) {
                mInternalListener.onRemoteJoined(wrapper, remoteId);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onRemoteJoined Exception: ", e);
        }
    }

    @Override
    public void onRemoteLeft(Wrapper wrapper, String remoteId) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onRemoteLeft(wrapper, remoteId);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onRemoteLeft Exception: ", e);
        }
    }

    @Override
    public void onRemoteVideoChanged(Wrapper wrapper, String remoteId, String reason, boolean disabled,
                                    boolean subscribed) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onRemoteVideoChanged(wrapper, remoteId, reason, disabled, subscribed);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onRemoteVideoChange Exception: ", e);
        }
    }

    @Override
    public void onError(Wrapper wrapper, OpentokError error) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onError(wrapper, error);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onError Exception: ", e);
        }
    }

}