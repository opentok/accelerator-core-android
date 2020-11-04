package com.tokbox.otsdkwrapper.listeners;

import android.os.Handler;
import com.opentok.android.OpentokError;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PausableAdvancedListener<Wrapper> implements RetriableAdvancedListener<Wrapper> {

    private AdvancedListener mUnderlyingListener = null;
    private ConcurrentLinkedQueue<Runnable> mPendingOperations;

    public PausableAdvancedListener(AdvancedListener listener) {
        mUnderlyingListener = listener;
        mPendingOperations = new ConcurrentLinkedQueue<Runnable>();
    }

    @Override
    public AdvancedListener setInternalListener(AdvancedListener listener) {
        AdvancedListener previous = mUnderlyingListener;
        mUnderlyingListener = listener;
        return previous;
    }

    @Override
    public AdvancedListener getInternalListener() {
        return mUnderlyingListener;
    }

    @Override
    public void resume() {
        while(!mPendingOperations.isEmpty()) {
            // Stage the pending events letting the screen refresh between each event...
            new Handler().postDelayed(mPendingOperations.poll(), 18);
        }
    }

    private interface ListenerTask {
        public void run() throws ListenerException;
    }

    private void runUIListenerTask(final ListenerTask task) {
        new Runnable() {
            @Override
            public void run() {
                if (mUnderlyingListener == null) {
                    return;
                }
                try {
                    task.run();
                } catch (ListenerException e) {
                    mPendingOperations.add(this);
                }
            }
        }.run();
    }

    @Override
    public void onReconnecting(final Wrapper wrapper)  {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onReconnecting(wrapper);
            }
        });
    }

    @Override
    public void onReconnected(final Wrapper wrapper)  {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onReconnected(wrapper);
            }
        });
    }

    @Override
    public void onReconnected(final Wrapper wrapper, final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onReconnected(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onDisconnected(final Wrapper wrapper, final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onDisconnected(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onAudioEnabled(final Wrapper wrapper, final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onAudioEnabled(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onAudioDisabled(final Wrapper wrapper, final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onAudioDisabled(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onVideoQualityWarning(final Wrapper wrapper, final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onVideoQualityWarning(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onVideoQualityWarningLifted(final Wrapper wrapper, final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onVideoQualityWarningLifted(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onAudioLevelUpdated(final float audioLevel) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onAudioLevelUpdated(audioLevel);
            }
        });
    }

    @Override
    public void onError(final Wrapper wrapper, final OpentokError error) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onError(wrapper, error);
            }
        });
    }

    @Override
    public void onCameraChanged(final Wrapper wrapper) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onCameraChanged(wrapper);
            }
        });
    }
}
