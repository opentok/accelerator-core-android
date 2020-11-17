package com.opentok.otsdkwrapper.wrapper;

import android.content.Context;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.otsdkwrapper.GlobalLogLevel;
import com.opentok.otsdkwrapper.signal.SignalInfo;
import com.opentok.otsdkwrapper.signal.SignalProcessorThread;
import com.opentok.otsdkwrapper.signal.SignalProtocol;
import com.opentok.otsdkwrapper.utils.Callback;
import com.opentok.otsdkwrapper.utils.LogWrapper;
import com.opentok.otsdkwrapper.utils.ThreadPool;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an OpenTok Session
 */
public class OTAcceleratorSession extends Session {
    private static final short LOCAL_LOG_LEVEL = 0xFF;
    private static final LogWrapper LOG =
            new LogWrapper((short) (GlobalLogLevel.sMaxLogLevel & LOCAL_LOG_LEVEL));
    private final String LOG_TAG = this.getClass().getSimpleName();
    ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();
    private HashSet<SessionListener> mSessionListeners = new HashSet<>();
    private HashSet<ConnectionListener> mConnectionsListeners = new HashSet<>();
    private HashSet<ArchiveListener> mArchiveListeners = new HashSet<>();
    private HashSet<StreamPropertiesListener> mStreamPropertiesListeners = new HashSet<>();
    private HashSet<ReconnectionListener> mReconnectionListeners = new HashSet<>();
    private ConcurrentHashMap<String, ArrayList<com.opentok.otsdkwrapper.listeners.SignalListener>>
            mSignalListeners =
            new ConcurrentHashMap<>();
    //signal protocol
    private SignalProtocol mInputSignalProtocol;
    private SignalProtocol mOutputSignalProtocol;
    private SignalProcessorThread mInputSignalProcessor;
    private SignalProcessorThread mOutputSignalProcessor;
    private ThreadPool mSignalThreadPool;
    private Callback<SignalInfo> mInternalSendSignal = new Callback<SignalInfo>() {
        @Override
        public void run(SignalInfo signalInfo) {
            Connection connection = null;
            if (signalInfo.mDstConnId != null) {
                connection = connections.get(signalInfo.mDstConnId);
            }
            internalSendSignal(signalInfo, connection);
        }
    };
    private Callback<SignalInfo> mDispatchSignal = new Callback<SignalInfo>() {
        @Override
        public void run(SignalInfo signalInfo) {
            dispatchSignal(signalInfo);
        }
    };
    private Session.SignalListener mSignalListener = new Session.SignalListener() {
        @Override
        public void onSignalReceived(Session session, String signalName, String data,
                                     Connection connection) {
            String connId = connection != null ? connection.getConnectionId() : null;
            SignalInfo inputSignal = new SignalInfo(connId, OTAcceleratorSession.this.getConnection().getConnectionId(), signalName, data);
            if (mInputSignalProtocol != null) {
                mInputSignalProtocol.write(inputSignal);
            } else {
                dispatchSignal(inputSignal);
            }
        }
    };

    /**
     * Creates an OTAcceleratorSession instance
     *
     * @param context
     * @param apiKey
     * @param sessionId
     */
    public OTAcceleratorSession(Context context, String apiKey, String sessionId) {
        super(context, apiKey, sessionId);
    }

    public static void setLogLevel(short logLevel) {
        LOG.setLogLevel(logLevel);
    }

    /**
     * Registers a signal listener for a given signal.
     *
     * @param signalName Name of the signal this listener will listen to. Pass "*" if the listener
     *                   is to be invoked for all signals.
     * @param listener   Listener that will be invoked when a signal is received.
     */
    public void addSignalListener(String signalName,
                                  com.opentok.otsdkwrapper.listeners.SignalListener listener) {
        LOG.d(LOG_TAG, "Adding Signal Listener for: ", signalName);
        if (mSignalThreadPool == null) {
            mSignalThreadPool = new ThreadPool();
        }
        ArrayList<com.opentok.otsdkwrapper.listeners.SignalListener> perNameListeners =
                mSignalListeners.get(signalName);
        if (perNameListeners == null) {
            perNameListeners = new ArrayList<>();
            mSignalListeners.put(signalName, perNameListeners);
        }
        if (perNameListeners.indexOf(listener) == -1) {
            LOG.d(LOG_TAG, "Signal listener for: ", signalName, " is new!");
            perNameListeners.add(listener);
        }
    }

    /**
     * Removes an object as signal listener everywhere it's used. This is added to support the
     * common cases where an activity (or some object that depends on an activity) is used as a
     * listene but the activity can be destroyed at some points (which would cause the app to crash
     * if the signal was delivered).
     *
     * @param listener Listener to be removed
     */
    public void removeSignalListener(com.opentok.otsdkwrapper.listeners.SignalListener listener) {
        Enumeration<String> signalNames = mSignalListeners.keys();
        while (signalNames.hasMoreElements()) {
            String signalName = signalNames.nextElement();
            LOG.d(LOG_TAG, "removeSignal(", listener.toString(), ") for ", signalName);
            removeSignalListener(signalName, listener);
        }
    }

    /**
     * Removes a signal listener.
     *
     * @param signalName Name of the signal this listener will listen to. Pass "*" if the listener
     *                   is to be invoked for all signals.
     * @param listener   Listener to be removed.
     */
    public void removeSignalListener(String signalName,
                                     com.opentok.otsdkwrapper.listeners.SignalListener listener) {
        ArrayList<com.opentok.otsdkwrapper.listeners.SignalListener> perNameListeners =
                mSignalListeners.get(signalName);
        if (perNameListeners == null) {
            return;
        }
        perNameListeners.remove(listener);
        if (perNameListeners.size() == 0) {
            mSignalListeners.remove(signalName);
        }
    }

    /**
     * Sends a new signal
     *
     * @param signalInfo {@link SignalInfo} of the signal to be sent
     * @param connection Destiantion connection. If null, the signal will be sent to all.
     */
    public void sendSignal(SignalInfo signalInfo, Connection connection) {
        if (mOutputSignalProtocol != null) {
            mOutputSignalProtocol.write(signalInfo);
        } else {
            if (connection != null) {
                internalSendSignal(signalInfo, connection);
            } else {
                internalSendSignal(signalInfo, null);
            }
        }
    }

    /**
     * Internal method to sends a new signal. Called from {@link OTWrapper}
     *
     * @param signalInfo {@link SignalInfo} of the signal to be sent
     * @param connection Destiantion connection. If null, the signal will be sent to all.
     */
    public void internalSendSignal(SignalInfo signalInfo, Connection connection) {
        LOG.d(LOG_TAG, "internalSendSignal: ", signalInfo.mSignalName);
        if (connection == null) {
            sendSignal(signalInfo.mSignalName, (String) signalInfo.mData);
        } else {
            sendSignal(signalInfo.mSignalName, (String) signalInfo.mData,
                    connection);
        }
    }

    /**
     * Clean signals. Internal method calld from {@link OTWrapper}
     */
    public void cleanUpSignals() {
        setInputSignalProtocol(null);
        setOutputSignalProtocol(null);
    }

    /**
     * Get the signal listener
     *
     * @return the signalListener
     */
    public Session.SignalListener getSignalListener() {
        return mSignalListener;
    }

    private void dispatchSignal(ArrayList<com.opentok.otsdkwrapper.listeners.SignalListener> listeners,
                                final SignalInfo signalInfo, boolean global) {
        if (listeners != null) {
            Iterator<com.opentok.otsdkwrapper.listeners.SignalListener> listenerIterator =
                    listeners.iterator();
            while (listenerIterator.hasNext()) {
                LOG.d(LOG_TAG, "Starting thread to process: ", signalInfo.mSignalName,
                        " : ", signalInfo.mData);
                final com.opentok.otsdkwrapper.listeners.SignalListener listener =
                        listenerIterator.next();
                mSignalThreadPool.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        LOG.d(LOG_TAG, "Dispatching signal: ", signalInfo.mSignalName,
                                " : ", signalInfo.mData,
                                " on thread: ", Thread.currentThread().getId());
                        listener.
                                onSignalReceived(signalInfo,
                                        OTAcceleratorSession.this.getConnection().
                                                getConnectionId().equals(signalInfo.mSrcConnId));
                    }
                });
            }
        } else {
            LOG.d(LOG_TAG, "dispatchSignal: No ", (global ? "global " : ""),
                    "listeners registered for: ", signalInfo.mSignalName);
        }
    }

    /**
     * Sets an input signal processor.
     *
     * @param inputProtocol The input protocol you want to enforce. Pass null if you wish to receive
     *                      raw signals.
     */
    public synchronized void setInputSignalProtocol(SignalProtocol inputProtocol) {
        mInputSignalProtocol = inputProtocol;
        mInputSignalProcessor =
                refreshSignalProcessor(mInputSignalProcessor, mInputSignalProtocol, mDispatchSignal);
    }

    /**
     * Sets an output signal protocol.
     *
     * @param outputProtocol
     */
    public synchronized void setOutputSignalProtocol(SignalProtocol outputProtocol) {
        mOutputSignalProtocol = outputProtocol;
        mOutputSignalProcessor =
                refreshSignalProcessor(mOutputSignalProcessor, mOutputSignalProtocol, mInternalSendSignal);
    }

    private void dispatchSignal(final SignalInfo signalInfo) {
        LOG.d(LOG_TAG, "Dispatching signal: ", signalInfo.mSignalName, " with: ", signalInfo.mData);
        dispatchSignal(mSignalListeners.get("*"), signalInfo, true);

        if (signalInfo.mSignalName != null) {
            dispatchSignal(mSignalListeners.get(signalInfo.mSignalName), signalInfo, false);
        }
    }

    private SignalProcessorThread refreshSignalProcessor(SignalProcessorThread currentProcessor,
                                                         SignalProtocol signalProtocol,
                                                         Callback<SignalInfo> cb) {
        if (currentProcessor != null) {
            return currentProcessor.switchPipe(signalProtocol);
        } else {
            return new SignalProcessorThread(signalProtocol, cb);
        }
    }

    @Override
    public void setSessionListener(SessionListener listener) {
        mSessionListeners.add(listener);
    }

    @Override
    public void setConnectionListener(ConnectionListener listener) {
        mConnectionsListeners.add(listener);
    }

    @Override
    public void setStreamPropertiesListener(StreamPropertiesListener listener) {
        mStreamPropertiesListeners.add(listener);
    }

    @Override
    public void setArchiveListener(ArchiveListener listener) {
        mArchiveListeners.add(listener);
    }

    @Override
    public void setReconnectionListener(ReconnectionListener listener) {
        mReconnectionListeners.add(listener);
    }

    @Override
    public void onConnected() {
        for (SessionListener l : mSessionListeners) {
            l.onConnected(this);
        }
    }

    @Override
    public void onReconnecting() {
        for (ReconnectionListener l : mReconnectionListeners) {
            l.onReconnecting(this);
        }
    }

    @Override
    public void onReconnected() {
        for (ReconnectionListener l : mReconnectionListeners) {
            l.onReconnected(this);
        }
    }

    @Override
    public void onDisconnected() {
        for (SessionListener l : mSessionListeners) {
            l.onDisconnected(this);
        }
        if (mSignalThreadPool != null) {
            mSignalThreadPool.finish();
            mSignalThreadPool = null;
        }
    }

    @Override
    public void onError(OpentokError error) {
        for (SessionListener l : mSessionListeners) {
            l.onError(this, error);
        }
    }

    @Override
    public void onStreamReceived(Stream stream) {
        for (SessionListener l : mSessionListeners) {
            l.onStreamReceived(this, stream);
        }
    }

    @Override
    public void onStreamDropped(Stream stream) {
        for (SessionListener l : mSessionListeners) {
            l.onStreamDropped(this, stream);
        }
    }

    @Override
    public void onConnectionCreated(Connection connection) {
        for (ConnectionListener l : mConnectionsListeners) {
            l.onConnectionCreated(this, connection);
        }
    }

    @Override
    public void onConnectionDestroyed(Connection connection) {
        for (ConnectionListener l : mConnectionsListeners) {
            l.onConnectionDestroyed(this, connection);
        }
    }

    @Override
    public void onStreamHasAudioChanged(Stream stream, int hasAudio) {
        for (StreamPropertiesListener l : mStreamPropertiesListeners) {
            l.onStreamHasAudioChanged(this, stream, (hasAudio != 0));
        }
    }

    @Override
    public void onStreamHasVideoChanged(Stream stream, int hasVideo) {
        for (StreamPropertiesListener l : mStreamPropertiesListeners) {
            l.onStreamHasVideoChanged(this, stream, (hasVideo != 0));
        }
    }

    @Override
    public void onStreamVideoDimensionsChanged(Stream stream, int width, int height) {
        for (StreamPropertiesListener l : mStreamPropertiesListeners) {
            l.onStreamVideoDimensionsChanged(this, stream, width, height);
        }
    }

    @Override
    public void onArchiveStarted(String id, String name) {
        for (ArchiveListener l : mArchiveListeners) {
            l.onArchiveStarted(this, id, name);
        }
    }

    @Override
    public void onArchiveStopped(String id) {
        for (ArchiveListener l : mArchiveListeners) {
            l.onArchiveStopped(this, id);
        }
    }
}
