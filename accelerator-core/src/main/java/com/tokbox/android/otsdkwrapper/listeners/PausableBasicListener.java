package com.tokbox.android.otsdkwrapper.listeners;

import android.os.Handler;
import android.view.View;

import com.opentok.android.OpentokError;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PausableBasicListener<Wrapper> implements RetriableBasicListener<Wrapper> {

    private BasicListener mUnderlyingListener = null;
    private ConcurrentLinkedQueue<Runnable> mPendingOperations;

    public PausableBasicListener(BasicListener listener) {
        mUnderlyingListener = listener;
        mPendingOperations = new ConcurrentLinkedQueue<Runnable>();
    }

    @Override
    public BasicListener getInternalListener() {
        return mUnderlyingListener;
    }

    @Override
    public void resume() {
        while(!mPendingOperations.isEmpty()) {
            // Stage the pending events letting the screen refresh between each event...
            new Handler().postDelayed(mPendingOperations.poll(), 18);
        }
    }

    /**
     * Sometimes I hate Java with a passion...
     */
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
    public void onConnected(final Wrapper wrapper, final int participantsNumber,
                            final String connId, final String data) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onConnected(wrapper, participantsNumber, connId, data);
            }
        });
    }

    @Override
    public void onDisconnected(final Wrapper wrapper, final int participantsNumber, final String connId, final String data) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onDisconnected(wrapper, participantsNumber, connId, data);
            }
        });
    }

    @Override
    public void onPreviewViewReady(final Wrapper wrapper, final View localView) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onPreviewViewReady(wrapper, localView);
            }
        });
    }

    @Override
    public void onPreviewViewDestroyed(final Wrapper wrapper, final View localView) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onPreviewViewDestroyed(wrapper, localView);
            }
        });
    }

    @Override
    public void onRemoteViewReady(final Wrapper wrapper, final View remoteView, final String remoteId,
                                  final String data)  {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onRemoteViewReady(wrapper, remoteView, remoteId, data);
            }
        });
    }

    @Override
    public void onRemoteViewDestroyed(final Wrapper wrapper, final View remoteView,
                                      final String remoteId) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onRemoteViewDestroyed(wrapper, remoteView, remoteId);
            }
        });
    }

    @Override
    public void onRemoteJoined(final Wrapper wrapper, final String remoteId)  {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onRemoteJoined(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onRemoteLeft(final Wrapper wrapper, final String remoteId)  {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onRemoteLeft(wrapper, remoteId);
            }
        });
    }

    @Override
    public void onRemoteVideoChanged(final Wrapper wrapper, final String remoteId, final String reason,
                                    final boolean disabled, final boolean subscribed) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onRemoteVideoChanged(wrapper, remoteId, reason, disabled, subscribed);
            }
        });
    }

    @Override
    public void onStartedPublishingMedia(final Wrapper wrapper, final boolean screensharing) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onStartedPublishingMedia(wrapper, screensharing);
            }
        });
    }

    @Override
    public void onStoppedPublishingMedia(final Wrapper wrapper, final boolean screensharing) {
        runUIListenerTask(new ListenerTask() {
            @Override
            public void run() throws ListenerException {
                mUnderlyingListener.onStoppedPublishingMedia(wrapper, screensharing);
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
}