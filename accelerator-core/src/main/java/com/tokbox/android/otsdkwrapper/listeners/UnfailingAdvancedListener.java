package com.tokbox.android.otsdkwrapper.listeners;

import android.util.Log;

import com.opentok.android.OpentokError;

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
    public void onReconnected(Wrapper wrapper, String remoteId) {
        if (mInternalListener != null) {
            mInternalListener.onReconnected(wrapper, remoteId);
        }
    }

    @Override
    public void onDisconnected(Wrapper wrapper, String remoteId) {
        if (mInternalListener != null) {
            mInternalListener.onDisconnected(wrapper, remoteId);
        }
    }

    @Override
    public void onAudioEnabled(Wrapper wrapper, String remoteId) {
        if (mInternalListener != null) {
            mInternalListener.onAudioEnabled(wrapper, remoteId);
        }
    }

    @Override
    public void onAudioDisabled(Wrapper wrapper, String remoteId) {
        if (mInternalListener != null) {
            mInternalListener.onAudioDisabled(wrapper, remoteId);
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
    public void onAudioLevelUpdated(float audioLevel) {
        try {
            if (mInternalListener != null) {
                mInternalListener.onAudioLevelUpdated(audioLevel);
            }
        } catch (ListenerException e) {
            Log.d(LOG_TAG, "onAudioLevelUpdated Exception: ", e);
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
