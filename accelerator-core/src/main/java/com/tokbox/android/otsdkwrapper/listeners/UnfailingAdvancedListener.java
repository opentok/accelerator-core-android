package com.tokbox.android.otsdkwrapper.listeners;

import android.util.Log;

import com.opentok.android.OpentokError;
import com.opentok.android.PublisherKit;

public class UnfailingAdvancedListener<Wrapper> implements RetriableAdvancedListener<Wrapper> {

    private final static String LOG_TAG = UnfailingAdvancedListener.class.getSimpleName();

    private AdvancedListener mInternalListener = null;


    public UnfailingAdvancedListener(AdvancedListener internaListener) {
        mInternalListener = internaListener;
    }

    @Override
    public AdvancedListener setInternalListener(AdvancedListener listener) {
        AdvancedListener aux = mInternalListener;
        mInternalListener = listener;
        return aux;
    }


    @Override
    public AdvancedListener getInternalListener() {
        return mInternalListener;
    }

    @Override
    public void resume() {
    }

    @Override
    public void onReconnecting(Wrapper wrapper) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onReconnecting(wrapper);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onReconnecting Exception: ", e);
        }
    }

    @Override
    public void onReconnected(Wrapper wrapper) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onReconnected(wrapper);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onReconnected Exception: ", e);
        }
    }


    @Override
    public void onVideoQualityWarning(Wrapper wrapper, String remoteId) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onVideoQualityWarning(wrapper, remoteId);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onVideoQualityWarning Exception: ", e);
        }
    }

    @Override
    public void onVideoQualityWarningLifted(Wrapper wrapper, String remoteId) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onVideoQualityWarningLifted(wrapper, remoteId);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onVideoQualityWarningLifted Exception: ", e);
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

    @Override
    public void onPreviewAudioLevelUpdated(Wrapper wrapper, float level) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onPreviewAudioLevelUpdated(wrapper, level);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onPreviewAudioLevelUpdated Exception: ", e);
        }
    }

    @Override
    public void onRemoteAudioLevelUpdated(Wrapper wrapper, String remoteId, float level) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onRemoteAudioLevelUpdated(wrapper, remoteId, level);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onRemoteAudioLevelUpdated Exception: ", e);
        }
    }

    @Override
    public void onCameraChanged(Wrapper wrapper) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onCameraChanged(wrapper);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onCameraChanged Exception: ", e);
        }
    }
}
