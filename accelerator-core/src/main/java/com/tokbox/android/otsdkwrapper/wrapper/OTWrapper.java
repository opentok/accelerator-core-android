package com.tokbox.android.otsdkwrapper.wrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import com.tokbox.android.otsdkwrapper.GlobalLogLevel;
import com.tokbox.android.otsdkwrapper.listeners.AdvancedListener;
import com.tokbox.android.otsdkwrapper.listeners.BaseOTListener;
import com.tokbox.android.otsdkwrapper.listeners.BasicListener;
import com.tokbox.android.otsdkwrapper.listeners.RetriableAdvancedListener;
import com.tokbox.android.otsdkwrapper.listeners.RetriableBasicListener;
import com.tokbox.android.otsdkwrapper.listeners.RetriableOTListener;
import com.tokbox.android.otsdkwrapper.listeners.UnfailingAdvancedListener;
import com.tokbox.android.otsdkwrapper.listeners.UnfailingBasicListener;
import com.tokbox.android.otsdkwrapper.screensharing.ScreenSharingCapturer;
import com.tokbox.android.otsdkwrapper.screensharing.ScreenSharingFragment;
import com.tokbox.android.otsdkwrapper.signal.SignalInfo;
import com.tokbox.android.otsdkwrapper.signal.SignalProtocol;
import com.tokbox.android.otsdkwrapper.utils.ClientLog;
import com.tokbox.android.otsdkwrapper.utils.LogWrapper;
import com.tokbox.android.otsdkwrapper.utils.MediaType;
import com.tokbox.android.otsdkwrapper.utils.OTConfig;
import com.tokbox.android.otsdkwrapper.utils.PreviewConfig;
import com.tokbox.android.otsdkwrapper.utils.StreamStatus;
import com.tokbox.android.otsdkwrapper.utils.VideoScale;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an OpenTok object to enable a video communication.
 * The first step in using the OTWrapper is to initialize it by calling the constructor with the
 * OpenTokConfig parameter.
 */
public class OTWrapper {
    private static final String LOG_TAG = OTWrapper.class.getSimpleName();
    private static final short LOCAL_LOG_LEVEL = 0xFF;
    private static final LogWrapper LOG =
            new LogWrapper((short) (GlobalLogLevel.sMaxLogLevel & LOCAL_LOG_LEVEL));
    private final OTWrapper SELF = this;
    private Context mContext = null;
    private OTAcceleratorSession mSession = null;
    private Connection mSessionConnection = null;
    private Publisher mPublisher = null;
    private Publisher mScreenPublisher = null;
    //indexed by streamId, *not* per subscriber Id
    private HashMap<String, Subscriber> mSubscribers = null;
    private ConcurrentHashMap<String, Stream> mStreams = null;
    //listeners
    private HashSet<RetriableBasicListener<OTWrapper>> mBasicListeners;
    private HashSet<RetriableAdvancedListener<OTWrapper>> mAdvancedListeners;
    private HashMap<BasicListener, RetriableBasicListener> mRetriableBasicListeners = new HashMap<>();
    private HashMap<AdvancedListener, RetriableAdvancedListener> mRetriableAdvancedListeners =
            new HashMap<>();
    private int mConnectionsCount;
    private int mOlderThanMe = 0;
    private boolean isPreviewing = false;
    private boolean isPublishing = false;
    private boolean startPublishing = false;
    private boolean startSharingScreen = false;
    private boolean isSharingScreen = false;
    private OTConfig mOTConfig;
    private PreviewConfig mPreviewConfig;
    //Screensharing by default
    private ScreenSharingFragment mScreensharingFragment;
    private boolean isScreensharingByDefault = false;
    private Publisher.Builder mScreenPublisherBuilder;
    //Custom renderer
    private BaseVideoRenderer mVideoRemoteRenderer;
    private BaseVideoRenderer mScreenRemoteRenderer;
    //Signal protocol
    private SignalProtocol mInputSignalProtocol;
    private SignalProtocol mOutputSignalProtocol;
    //Analytics for internal use
    private OTKAnalyticsData mAnalyticsData;
    private OTKAnalytics mAnalytics;
    private Session.ConnectionListener mConnectionListener = new Session.ConnectionListener() {
        @Override
        public void onConnectionCreated(Session session, Connection connection) {
            LOG.d(LOG_TAG, "onConnectionCreated: ", connection.getData());
            mSession.connections.put(connection.getConnectionId(), connection);
            mConnectionsCount++;
            if (connection.getCreationTime().compareTo(mSessionConnection.getCreationTime()) <= 0) {
                mOlderThanMe++;
            }
            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onConnected(SELF, mConnectionsCount,
                            connection.getConnectionId(),
                            connection.getData());
                }
            }
        }

        @Override
        public void onConnectionDestroyed(Session session, Connection connection) {
            LOG.d(LOG_TAG, "onConnectionDestroyed: ", connection.getData());
            mSession.connections.remove(connection.getConnectionId());
            mConnectionsCount--;
            if (connection.getCreationTime().compareTo(mSessionConnection.getCreationTime()) <= 0) {
                mOlderThanMe--;
            }
            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onDisconnected(SELF, mConnectionsCount,
                            connection.getConnectionId(),
                            connection.getData());
                }
            }
        }
    };
    private SubscriberKit.SubscriberListener mSubscriberListener =
            new SubscriberKit.SubscriberListener() {
                @Override
                public void onConnected(SubscriberKit sub) {
                    LOG.i(LOG_TAG, "Subscriber is connected");
                    addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_SUCCESS);

                    if (mBasicListeners != null) {
                        for (BasicListener listener : mBasicListeners) {
                            Stream stream = sub.getStream();
                            ((RetriableBasicListener) listener).
                                    onRemoteViewReady(SELF, sub.getView(), stream.getStreamId(),
                                            stream.getConnection().getData());
                        }
                    }
                }

                @Override
                public void onDisconnected(SubscriberKit sub) {
                    addLogEvent(ClientLog.LOG_ACTION_REMOVE_REMOTE, ClientLog.LOG_VARIATION_SUCCESS);

                    if (mBasicListeners != null) {
                        for (BasicListener listener : mBasicListeners) {
                            ((RetriableBasicListener) listener).
                                    onRemoteViewDestroyed(SELF, null, sub.getStream().getStreamId());
                        }
                    }
                }

                @Override
                public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
                    LOG.e(LOG_TAG, "Subscriber: onError ", opentokError.getErrorCode(), ", ",
                            opentokError.getMessage());
                    String id = subscriberKit.getStream().getStreamId();
                    OpentokError.ErrorCode errorCode = opentokError.getErrorCode();
                    switch (errorCode) {
                        case SubscriberInternalError:
                            //TODO: Add client logs for the different subscribers errors
                            LOG.e(LOG_TAG, "Subscriber error: SubscriberInternalError");
                            mSubscribers.remove(id);
                            break;
                        case ConnectionTimedOut:
                            addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_ERROR);
                            // Just try again
                            if (mSession != null) {
                                addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_ATTEMPT);
                                mSession.subscribe(subscriberKit);
                            }
                            break;
                        case SubscriberWebRTCError:
                            LOG.e(LOG_TAG, "Subscriber error: SubscriberWebRTCError");
                            mSubscribers.remove(id);
                            break;
                        case SubscriberServerCannotFindStream:
                            LOG.e(LOG_TAG, "Subscriber error: SubscriberServerCannotFindStream");
                            mSubscribers.remove(id);
                            break;
                        default:
                            LOG.e(LOG_TAG, "Subscriber error: default ");
                            mSubscribers.remove(id);
                            if (!mStreams.containsKey(id)) {
                                mStreams.put(id, subscriberKit.getStream());
                            }

                            for (BasicListener listener : mBasicListeners) {
                                ((RetriableBasicListener) listener).onError(SELF, opentokError);
                            }
                            break;
                    }
                }
            };
    private Publisher.AudioLevelListener mAudioLevelListener = new Publisher.AudioLevelListener() {
        @Override
        public void onAudioLevelUpdated(PublisherKit publisherKit, float audioLevel) {
            for (AdvancedListener listener : mAdvancedListeners) {
                ((RetriableAdvancedListener) listener).onAudioLevelUpdated(audioLevel);
            }
        }
    };
    //Implements Advanced listeners
    private Session.ReconnectionListener mReconnectionListener = new Session.ReconnectionListener() {

        @Override
        public void onReconnecting(Session session) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    ((RetriableAdvancedListener) listener).onReconnecting(SELF);
                }
            }
        }

        @Override
        public void onReconnected(Session session) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    ((RetriableAdvancedListener) listener).onReconnected(SELF);
                }
            }
        }
    };
    private SubscriberKit.VideoListener mVideoListener = new SubscriberKit.VideoListener() {
        @Override
        public void onVideoDataReceived(SubscriberKit subscriberKit) {
            //to-review: a new listener to indicate the first frame received
        }

        @Override
        public void onVideoDisabled(SubscriberKit subscriber, String reason) {
            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).
                            onRemoteVideoChanged(SELF, subscriber.getStream().getStreamId(), reason, false,
                                    subscriber.getSubscribeToVideo());
                }
            }
        }

        @Override
        public void onVideoEnabled(SubscriberKit subscriber, String reason) {
            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).
                            onRemoteVideoChanged(SELF, subscriber.getStream().getStreamId(), reason, true,
                                    subscriber.getSubscribeToVideo());
                }
            }
        }

        @Override
        public void onVideoDisableWarning(SubscriberKit subscriber) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    ((RetriableAdvancedListener) listener).
                            onVideoQualityWarning(SELF, subscriber.getStream().getStreamId());
                }
            }
        }

        @Override
        public void onVideoDisableWarningLifted(SubscriberKit subscriber) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    ((RetriableAdvancedListener) listener).
                            onVideoQualityWarningLifted(SELF, subscriber.getStream().getStreamId());
                }
            }
        }
    };
    private Publisher.CameraListener mCameraListener = new Publisher.CameraListener() {

        @Override
        public void onCameraChanged(Publisher publisher, int i) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    ((RetriableAdvancedListener) listener).onCameraChanged(SELF);
                }
            }
        }

        @Override
        public void onCameraError(Publisher publisher, OpentokError opentokError) {
            LOG.d(LOG_TAG, "onCameraError: onError ", opentokError.getMessage());
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    ((RetriableAdvancedListener) listener).onError(SELF, opentokError);
                }
            }
        }
    };
    private SubscriberKit.StreamListener mStreamListener = new SubscriberKit.StreamListener() {

        @Override
        public void onReconnected(SubscriberKit subscriber) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    listener.
                            onReconnected(SELF, subscriber.getStream().getStreamId());
                }
            }
        }

        @Override
        public void onDisconnected(SubscriberKit subscriber) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    listener.
                            onDisconnected(SELF, subscriber.getStream().getStreamId());
                }
            }
        }

        @Override
        public void onAudioEnabled(SubscriberKit subscriber) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    listener.
                            onAudioEnabled(SELF, subscriber.getStream().getStreamId());
                }
            }
        }

        @Override
        public void onAudioDisabled(SubscriberKit subscriber) {
            if (mAdvancedListeners != null) {
                for (AdvancedListener listener : mAdvancedListeners) {
                    listener.
                            onAudioDisabled(SELF, subscriber.getStream().getStreamId());
                }
            }
        }
    };
    //Implements Basic listeners: Session.SessionListener, Session.ConnectionListener,
    // Session.SignalListener, Publisher.PublisherListener
    private Session.SessionListener mSessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            mSessionConnection = session.getConnection();
            mSession.connections.put(mSessionConnection.getConnectionId(), mSessionConnection);
            //update internal client logs with connectionId
            mAnalyticsData.setConnectionId(mSessionConnection.getConnectionId());
            mAnalytics.setData(mAnalyticsData);
            LOG.d(LOG_TAG, "onConnected: ", mSessionConnection.getData(),
                    ". listeners: ", mBasicListeners);
            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_SUCCESS);

            mConnectionsCount++;
            publishIfReady();

            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onConnected(SELF, mConnectionsCount,
                            mSessionConnection.getConnectionId(),
                            mSessionConnection.getData());
                }
            }
        }

        @Override
        public void onDisconnected(Session session) {
            addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_SUCCESS);

            if (mSession == null || mSessionConnection == null) {
                // This can happen if somehow onError was called before onDisconnected or if we somehow
                // call onDisconected twice (so cleanup has been done already)
                LOG.w(LOG_TAG, "OnDisconnected called on a stale object");
                mSessionConnection = session.getConnection();
            }
            if (mBasicListeners != null && mSessionConnection != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onDisconnected(SELF, 0,
                            mSessionConnection.getConnectionId(),
                            mSessionConnection.getData());
                }
            }
            cleanup();
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            LOG.d(LOG_TAG, "OnStreamReceived: ", stream.getConnection().getData());

            if (mStreams != null) {
                mStreams.put(stream.getStreamId(), stream);
            }
            if (mOTConfig.shouldSubscribeAutomatically()) {
                addRemote(stream.getStreamId());
            }

            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onRemoteJoined(SELF, stream.getStreamId());
                }
            }
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            LOG.d(LOG_TAG, "OnStreamDropped: ", stream.getConnection().getData());

            String subId = stream.getStreamId();
            if (mStreams.containsKey(subId)) {
                mStreams.remove(stream.getStreamId());
            }
            if (mSubscribers.containsKey(subId)) {
                mSubscribers.remove(stream.getStreamId());
            }
            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onRemoteLeft(SELF, subId);
                    ((RetriableBasicListener) listener).onRemoteViewDestroyed(SELF, null, subId);
                }
            }
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            LOG.e(LOG_TAG, "Session: onError ", opentokError.getMessage());
            if (getOwnConnId() != null) {
                addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_ERROR);
            } else {
                addLogEvent(ClientLog.LOG_ACTION_CONNECT, ClientLog.LOG_VARIATION_ERROR);
            }
            cleanup();
            if (mBasicListeners != null) {
                for (BasicListener listener : mBasicListeners) {
                    ((RetriableBasicListener) listener).onError(SELF, opentokError);
                }
            }
        }
    };
    private Publisher.PublisherListener mPublisherListener = new Publisher.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            boolean screensharing = false;

            if (stream.getStreamVideoType() == Stream.StreamVideoType.StreamVideoTypeScreen) {
                addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_SUCCESS);
                screensharing = true;
            } else {
                addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_SUCCESS);
                isPublishing = true;
            }
            for (BasicListener listener : mBasicListeners) {
                ((RetriableBasicListener) listener).onStartedPublishingMedia(SELF, screensharing);
            }

            //check subscribe to self
            if (mOTConfig.shouldSubscribeToSelf()) {
                addRemote(stream.getStreamId());
            }
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            boolean screensharing = false;
            if (stream.getStreamVideoType() == Stream.StreamVideoType.StreamVideoTypeScreen) {
                addLogEvent(ClientLog.LOG_ACTION_END_SCREEN_COMM, ClientLog.LOG_VARIATION_SUCCESS);
                screensharing = true;
            } else {
                addLogEvent(ClientLog.LOG_ACTION_END_COMM, ClientLog.LOG_VARIATION_SUCCESS);
                isPublishing = false;
            }
            for (BasicListener listener : mBasicListeners) {
                ((RetriableBasicListener) listener).onStoppedPublishingMedia(SELF, screensharing);
            }
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            LOG.e(LOG_TAG, "Publisher: onError ", opentokError.getErrorCode(), ", ",
                    opentokError.getMessage());
            OpentokError.ErrorCode errorCode = opentokError.getErrorCode();
            if (publisherKit.getStream() != null && publisherKit.getStream().getStreamVideoType() == Stream.StreamVideoType.StreamVideoTypeCamera) {
                addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ERROR);
            } else {
                addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_ERROR);
            }

            switch (errorCode) {
                case PublisherInternalError:
                    //TODO: Add client logs for the different publisher errors
                    LOG.e(LOG_TAG, "Publisher error: PublisherInternalError");
                    mPublisher = null;
                    break;
                case PublisherTimeout:
                    //re-try publishing
                    if (mSession != null) {
                        if (publisherKit.getStream() != null && publisherKit.getStream().getStreamVideoType() == Stream.StreamVideoType.StreamVideoTypeCamera) {
                            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT);
                        } else {
                            addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_ATTEMPT);
                        }
                        mSession.publish(publisherKit);
                    }
                    break;
                case PublisherWebRTCError:
                    LOG.e(LOG_TAG, "Publisher error: PublisherWebRTCError");
                    mPublisher = null;
                    break;
                default:
                    LOG.e(LOG_TAG, "Publisher error: default");
                    mPublisher = null;
                    for (BasicListener listener : mBasicListeners) {
                        ((RetriableBasicListener) listener).onError(SELF, opentokError);
                    }
                    break;
            }

        }
    };
    ScreenSharingFragment.ScreenSharingListener screenListener = new ScreenSharingFragment.ScreenSharingListener() {
        @Override
        public void onScreenCapturerReady() {
            ScreenSharingCapturer capturer = mScreensharingFragment.getScreenCapturer();

            if (capturer != null) {
                mScreenPublisherBuilder.capturer(capturer);
                mScreenPublisher = mScreenPublisherBuilder.build();
                mScreenPublisher.setPublisherListener(mPublisherListener);
                mScreenPublisher.setAudioLevelListener(mAudioLevelListener);
                mScreenPublisher.setCameraListener(mCameraListener);
                mScreenPublisher.
                        setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);

                //byDefault
                mScreenPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                        BaseVideoRenderer.STYLE_VIDEO_FILL);
                attachPublisherScreenView();
                mSession.publish(mScreenPublisher);
            }
        }

        @Override
        public void onError(String errorMsg) {
            LOG.i(LOG_TAG, "Error in Screensharing by default");
            for (BasicListener listener : mBasicListeners) {
                OpentokError error = new OpentokError(OpentokError.Domain.PublisherErrorDomain, OpentokError.ErrorCode.PublisherInternalError.getErrorCode(), errorMsg);
                ((RetriableBasicListener) listener).onError(SELF, error);
            }
        }
    };

    /**
     * Creates an OTWrapper instance.
     *
     * @param context Activity context. Needed by the Opentok APIs
     * @param config  OTConfig: Information about the OpenTok session. This includes all the needed
     *                data to connect.
     */
    public OTWrapper(Context context, OTConfig config) {
        this.mContext = context;
        this.mOTConfig = config;
        mSubscribers = new HashMap<>();
        mStreams = new ConcurrentHashMap<>();
        mBasicListeners = new HashSet<>();
        mAdvancedListeners = new HashSet<>();

        initAnalytics();
    }

    public static void setLogLevel(short logLevel) {
        LOG.setLogLevel(logLevel);
    }

    /**
     * Null-safe check if the specified collection is empty.
     * <p>
     * Null returns true.
     *
     * @param coll the collection to check, may be null
     * @return true if empty or null
     */
    public static boolean isNullOrEmpty(Collection coll) {
        return (coll == null || coll.isEmpty());
    }

    /**
     * Call this method when the app's activity pauses.
     * This pauses the video for the local preview and remotes
     */
    public void pause() {
        if (mSession != null) {
            mSession.onPause();
        }
    }

    /**
     * Call this method when the app's activity resumes.
     * This resumes the video for the local preview and remotes.
     *
     * @param resumeEvents Set to true if the events should be resumed
     */
    public void resume(boolean resumeEvents) {
        if (mSession != null) {
            mSession.onResume();
        }
        if (resumeEvents && !isNullOrEmpty(mBasicListeners)) {
            for (BasicListener listener : mBasicListeners
            ) {
                ((RetriableBasicListener) listener).resume();
            }

        }
    }

    /**
     * Connects to the OpenTok session.
     * When the otwrapper connects, the
     * {@link BasicListener#onConnected(Object, int, String, String)} method is called.
     * If the otwrapper fails to connect, the
     * {@link BasicListener#onError(Object, OpentokError)} method is called.
     */
    public void connect() {
        addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT);

        mSession = new OTAcceleratorSession(mContext, mOTConfig.getApiKey(), mOTConfig.getSessionId());
        mSession.setConnectionListener(mConnectionListener);
        mSession.setSessionListener(mSessionListener);
        mSession.setSignalListener(mSession.getSignalListener());
        mSession.setReconnectionListener(mReconnectionListener);

        mOlderThanMe = 0;

        //check signal protocol
        if (mInputSignalProtocol != null) {
            mSession.setInputSignalProtocol(mInputSignalProtocol);
        }
        if (mOutputSignalProtocol != null) {
            mSession.setOutputSignalProtocol(mOutputSignalProtocol);
        }

        mSession.connect(mOTConfig.getToken());
    }

    /**
     * Disconnects from the OpenTok session.
     * When the otwrapper disconnects, the
     * {@link BasicListener#onDisconnected(Object, int, String, String)}  method is called.
     * If the otwrapper fails to disconnect, the
     * {@link BasicListener#onError(Object, OpentokError)} method is called.
     */
    public void disconnect() {
        addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_ATTEMPT);
        if (mSession != null) {
            mSession.disconnect();
        }
    }

    /**
     * Returns the local connectionID
     *
     * @return the own connectionID
     */
    public String getOwnConnId() {
        String ownConnectionId = mSessionConnection != null ? mSessionConnection.getConnectionId() : null;
        return ownConnectionId;
    }

    /**
     * Returns the number of active connections for the current session
     *
     * @return the number of active connections.
     */
    public int getConnectionsCount() {
        return mConnectionsCount;
    }

    /**
     * Checks if the own connection is the oldest in the current session
     *
     * @return Whether the local connection is oldest (<code>true</code>) or not (
     * <code>false</code>).
     */
    public boolean isTheOldestConnection() {
        boolean theOldest = mOlderThanMe <= 0;
        return theOldest;
    }

    /**
     * Compares the connections creation times between the local connection and the argument passing
     *
     * @param connectionId The connection we want to compare with
     * @return -1 if the connection passed is newer than the current session connection, 0
     * if they have the same age, and 1 if the connection is older
     */
    public int compareConnectionsTimes(String connectionId) {
        int age = 0;
        if (mSession != null) {
            age = mSession.getConnection().
                    getCreationTime().compareTo(mSession.connections.get(connectionId).getCreationTime());
        }
        return age;
    }

    /**
     * Returns the remote connectionID
     *
     * @param remoteId the remote Id
     * @return the remote connectionID
     */
    public String getRemoteConnId(String remoteId) {
        Subscriber remoteSub = mSubscribers.get(remoteId);
        String remoteConnId = remoteSub.getStream().getConnection().getConnectionId();
        return remoteConnId;
    }

    /**
     * Call to display the camera's video in the Preview's view before it starts streaming
     * video.
     *
     * @param config The configuration of the preview
     */
    public void startPreview(PreviewConfig config) {
        mPreviewConfig = config;
        if (mPublisher == null && !isPreviewing) {
            createPublisher();
            attachPublisherView();
            isPreviewing = true;
        }
    }

    /**
     * Whether the local is sharing media (<code>true</code>) or not (
     * <code>false</code>).
     */
    public boolean isPublishing() {
        return isPublishing;
    }

    /**
     * Whether the local is previewing (<code>true</code>) or not (
     * <code>false</code>).
     */
    public boolean isPreviewing() {
        return isPreviewing;
    }

    /**
     * Call to stop the camera's video in the Preview's view.
     */
    public void stopPreview() {
        if (mPublisher != null && isPreviewing) {
            detachPublisherView();
            mPublisher = null;
            isPreviewing = false;
            startPublishing = false;
        }
    }

    /**
     * Starts the local streaming video
     *
     * @param config        The configuration of the preview
     * @param screensharing Whether to indicate the camera or the screen streaming.
     */
    public void startPublishingMedia(PreviewConfig config, boolean screensharing) {
        addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT);

        if (!screensharing) {
            mPreviewConfig = config;
            startPublishing = true;
            if (mPublisher == null) {
                createPublisher();
            }
            publishIfReady();
        } else {
            startSharingScreen = true;
            if (mScreenPublisher == null) {
                createScreenPublisher(config);
            }
            publishIfScreenReady();
        }
    }

    /**
     * Stops the local streaming video.
     *
     * @param screensharing Whether to indicate the camera or the screen streaming
     */
    public void stopPublishingMedia(boolean screensharing) {

        if (!screensharing) {
            addLogEvent(ClientLog.LOG_ACTION_END_COMM, ClientLog.LOG_VARIATION_ATTEMPT);

            if (mPublisher != null && startPublishing) {
                mSession.unpublish(mPublisher);
            }
            isPublishing = false;
            startPublishing = false;
            if (!isPreviewing) {
                detachPublisherView();
                mPublisher = null;
            }
        } else {
            addLogEvent(ClientLog.LOG_ACTION_END_SCREEN_COMM, ClientLog.LOG_VARIATION_ATTEMPT);

            if (mScreensharingFragment != null) {
                mScreensharingFragment.stopScreenCapture();
                isScreensharingByDefault = false;
            }
            detachPublisherScreenView();
            if (mScreenPublisher != null && startSharingScreen) {
                mSession.unpublish(mScreenPublisher);
            }
            isSharingScreen = false;
            startSharingScreen = false;

            mScreenPublisher = null;
        }
    }

    /**
     * Returns Local Media status
     *
     * @param type MediaType (Audio or Video)
     * @return Whether the local MediaType is enabled (<code>true</code>) or not (
     * <code>false</code>)
     */
    public boolean isLocalMediaEnabled(MediaType type) {
        boolean returnedValue = (mPublisher != null) &&
                (type == MediaType.VIDEO ? mPublisher.getPublishVideo() : mPublisher.getPublishAudio());
        return returnedValue;
    }

    /**
     * Enables or disables the local Media.
     *
     * @param type    MediaType (Audio or Video)
     * @param enabled Whether to enable media (<code>true</code>) or not (
     *                <code>false</code>).
     */
    public void enableLocalMedia(MediaType type, boolean enabled) {
        if (mPublisher != null) {
            switch (type) {
                case AUDIO:
                    mPublisher.setPublishAudio(enabled);
                    break;
                case VIDEO:
                    mPublisher.setPublishVideo(enabled);
                    if (enabled) {
                        mPublisher.getView().setVisibility(View.VISIBLE);
                    } else {
                        mPublisher.getView().setVisibility(View.GONE);
                    }
                    break;
                default:

            }
        }
    }

    /**
     * Enables or disables the media of the remote with remoteId.
     *
     * @param type    MediaType (Audio or video)
     * @param enabled Whether to enable MediaType (<code>true</code>) or not (
     *                <code>false</code>).
     */
    public void enableReceivedMedia(String remoteId, MediaType type, boolean enabled) {
        if (remoteId != null) {
            enableRemoteMedia(mSubscribers.get(remoteId), type, enabled);
        } else {
            Collection<Subscriber> subscribers = mSubscribers.values();
            for (Subscriber sub : subscribers) {
                enableRemoteMedia(sub, type, enabled);
            }
        }
    }

    /**
     * Returns the MediaType status of the remote with remoteId
     *
     * @param type MediaType: audio or video
     * @return Whether the remote MediaType is enabled (<code>true</code>) or not (
     * <code>false</code>).
     */
    public boolean isReceivedMediaEnabled(String remoteId, MediaType type) {
        Subscriber sub = mSubscribers.get(remoteId);
        boolean returnedValue = false;
        if (sub != null) {
            if (type == MediaType.VIDEO) {
                returnedValue = sub.getSubscribeToVideo();
            } else {
                returnedValue = sub.getSubscribeToAudio();
            }
        }
        return returnedValue;
    }

    /**
     * Susbscribe to a specific remote
     *
     * @param remoteId String to identify the remote
     */
    public void addRemote(String remoteId) {
        LOG.i(LOG_TAG, "Add remote with ID: ", remoteId);
        addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_ATTEMPT);
        Stream stream = mStreams.get(remoteId);
        LOG.i(LOG_TAG, "private add new remote stream != null");
        Subscriber sub = new Subscriber(mContext, stream);
        sub.setVideoListener(mVideoListener);
        sub.setStreamListener(mStreamListener);
        sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        String subId = stream.getStreamId();
        mSubscribers.put(subId, sub);
        sub.setSubscriberListener(mSubscriberListener);

        if (stream != null && stream.getStreamVideoType() == Stream.StreamVideoType.StreamVideoTypeCamera &&
                mVideoRemoteRenderer != null) {
            sub.setRenderer(mVideoRemoteRenderer);
        } else {
            if (stream != null && stream.getStreamVideoType() == Stream.StreamVideoType.StreamVideoTypeScreen &&
                    mScreenRemoteRenderer != null) {
                sub.setRenderer(mScreenRemoteRenderer);
            }
        }
        //remove the sub's stream from the streams list to avoid subscribe twice to the same stream
        mStreams.remove(sub.getStream().getStreamId());

        mSession.subscribe(sub);
    }

    /**
     * Unsusbscribe from a specific remote
     *
     * @param remoteId String to identify the remote
     */
    public void removeRemote(String remoteId) {
        LOG.i(LOG_TAG, "Remove remote with ID: ", remoteId);
        addLogEvent(ClientLog.LOG_ACTION_REMOVE_REMOTE, ClientLog.LOG_VARIATION_ATTEMPT);
        Subscriber sub = mSubscribers.get(remoteId);
        mSubscribers.remove(remoteId);

        mStreams.put(remoteId, sub.getStream());
        mSession.unsubscribe(sub);
    }

    /**
     * Call to cycle between cameras, if there are multiple cameras on the device.
     */
    public void cycleCamera() {
        if (mPublisher != null) {
            mPublisher.cycleCamera();
        }
    }

    /**
     * Returns the OpenTok Configuration
     *
     * @return current OpenTok Configuration
     */
    public OTConfig getOTConfig() {
        return this.mOTConfig;
    }

    private RetriableOTListener getUnfailingFromBaseListener(BaseOTListener listener) {
        return listener instanceof BasicListener ?
                new UnfailingBasicListener((BasicListener) listener) :
                new UnfailingAdvancedListener<>((AdvancedListener) listener);
    }

    /**
     * Adds a {@link BasicListener}. If the listener was already added, nothing is done.
     *
     * @param listener
     * @return The added listener
     */
    public BasicListener addBasicListener(BasicListener listener) {
        LOG.d(LOG_TAG, "Adding BasicListener");
        BasicListener returnedListener = (BasicListener) addOTListener(listener, mRetriableBasicListeners, mBasicListeners);
        return returnedListener;
    }

    /**
     * Removes a {@link BasicListener}
     *
     * @param listener
     */
    public void removeBasicListener(BasicListener listener) {
        removeOTListener(listener, mRetriableBasicListeners, mBasicListeners);
    }

    /**
     * Adds an {@link AdvancedListener}
     *
     * @param listener
     * @return The removed listener
     */
    public AdvancedListener addAdvancedListener(AdvancedListener<OTWrapper> listener) {
        AdvancedListener returnedListener = (AdvancedListener) addOTListener(listener, mRetriableAdvancedListeners,
                mAdvancedListeners);
        return returnedListener;
    }

    /**
     * Removes an {@link AdvancedListener}
     *
     * @param listener
     */
    public void removeAdvancedListener(AdvancedListener listener) {
        removeOTListener(listener, mRetriableAdvancedListeners, mAdvancedListeners);
    }

    /**
     * Registers a signal listener for a given signal.
     *
     * @param signalName Name of the signal this listener will listen to. Pass "*" if the listener
     *                   is to be invoked for all signals.
     * @param listener   Listener that will be invoked when a signal is received.
     */
    public void addSignalListener(String signalName, com.tokbox.android.otsdkwrapper.listeners.SignalListener listener) {
        if (mSession != null) {
            mSession.addSignalListener(signalName, listener);
        }
    }

    /**
     * Removes an object as signal listener everywhere it's used. This is added to support the common
     * cases where an activity (or some object that depends on an activity) is used as a listener
     * but the activity can be destroyed at some points (which would cause the app to crash if the
     * signal was delivered).
     *
     * @param listener Listener to be removed
     */
    public void removeSignalListener(com.tokbox.android.otsdkwrapper.listeners.SignalListener listener) {
        if (mSession != null) {
            mSession.removeSignalListener(listener);
        }
    }

    /**
     * Removes a signal listener.
     *
     * @param signalName Name of the signal this listener will listen to. Pass "*" if the listener
     *                   is to be invoked for all signals.
     * @param listener   Listener to be removed.
     */
    public void removeSignalListener(String signalName, com.tokbox.android.otsdkwrapper.listeners.SignalListener listener) {
        if (mSession != null) {
            mSession.removeSignalListener(signalName, listener);
        }
    }

    /**
     * Sends a new signal
     *
     * @param signalInfo {@link SignalInfo} of the signal to be sent
     */
    public void sendSignal(SignalInfo signalInfo) {
        if (mSession != null) {
            if (signalInfo.mDstConnId != null) {
                mSession.sendSignal(signalInfo, mSession.connections.get(signalInfo.mDstConnId));
            } else {
                mSession.sendSignal(signalInfo, null);
            }
        }
    }

    /**
     * Returns the {@link StreamStatus} of the local.
     *
     * @return The {@link StreamStatus} of the local.
     */
    public StreamStatus getLocalStreamStatus() {
        if (mPublisher != null) {
            Stream stream = mPublisher.getStream();
            boolean hasAudio = true;
            boolean hasVideo = true;
            int videoHeight = 0;
            int videoWidth = 0;
            Stream.StreamVideoType streamVideoType = Stream.StreamVideoType.StreamVideoTypeCamera;
            if (stream != null) {
                hasAudio = stream.hasAudio();
                hasVideo = stream.hasVideo();
                streamVideoType = stream.getStreamVideoType();
                videoHeight = stream.getVideoHeight();
                videoWidth = stream.getVideoWidth();
            }
            return new StreamStatus(mPublisher.getView(),
                    mPublisher.getPublishAudio(), mPublisher.getPublishVideo(),
                    hasAudio, hasVideo, streamVideoType,
                    videoWidth, videoHeight);
        }
        return null;
    }

    /**
     * Returns the stream status for the requested subscriber (actually, subId is the streamId..)
     *
     * @param id Id of the subscriber/stream
     * @return The status including the view, and if it's subscribing to video and if it has local
     * video
     */
    public StreamStatus getRemoteStreamStatus(String id) {
        Subscriber sub = mSubscribers.get(id);
        if (sub != null) {
            Stream subSt = sub.getStream();
            return new StreamStatus(sub.getView(), sub.getSubscribeToAudio(), sub.getSubscribeToVideo(),
                    subSt.hasAudio(), subSt.hasVideo(), subSt.getStreamVideoType(),
                    subSt.getVideoWidth(), subSt.getVideoHeight());
        }
        return null;
    }

    /**
     * Sets the  Video Scale style for a remote
     *
     * @param remoteId the remote subscriber ID
     * @param style    VideoScale value: FILL or FIT
     */
    public void setRemoteStyle(String remoteId, VideoScale style) {
        Subscriber sub = mSubscribers.get(remoteId);
        if (sub != null) {
            if (style == VideoScale.FILL) {
                sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            } else {
                sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FIT);
            }
        }
    }

    /**
     * Sets the Local Video Style
     *
     * @param style VideoScale value: FILL or FIT
     */
    public void setLocalStyle(VideoScale style) {
        if (mPublisher != null) {
            if (style == VideoScale.FILL) {
                mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
            } else {
                mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FIT);
            }
        }
    }

    /**
     * Sets a custom video renderer for the remote
     *
     * @param renderer     The custom video renderer
     * @param remoteScreen Whether the renderer is applied to the remote received screen or not.
     */
    public void setRemoteVideoRenderer(BaseVideoRenderer renderer, boolean remoteScreen) {
        //todo: now, it will apply to all the subscribers
        if (remoteScreen) {
            mScreenRemoteRenderer = renderer;
        } else {
            mVideoRemoteRenderer = renderer;
        }
    }

    /**
     * (Tries) to set the FPS of the shared video stream to the passed one. The FPS is rounded to
     * the nearest supported one.
     *
     * @param framesPerSecond
     */
    public void setPublishingFPS(int framesPerSecond) {
        LOG.d(LOG_TAG, "setSharingFPS: ", mPublisher);
        Publisher.CameraCaptureFrameRate frameRate = getFPS(framesPerSecond);
        if (mPublisher != null) {
            int currentCamera = mPublisher.getCameraId();
            PreviewConfig newPreview;
            if (mPreviewConfig != null) {
                mPreviewConfig.setFrameRate(frameRate);
            } else {
                mPreviewConfig = new PreviewConfig.PreviewConfigBuilder().framerate(frameRate).build();
            }
            newPreview = mPreviewConfig;
            boolean isPublishingCurrent = this.isPublishing;
            boolean isPreviewingCurrent = this.isPreviewing;

            if (isPublishingCurrent) {
                stopPublishingMedia(false);
            }
            if (isPreviewingCurrent) {
                stopPreview();
            }
            mPublisher = null;
            if (isPreviewingCurrent) {
                startPreview(newPreview);
            }
            if (isPublishingCurrent) {
                startPublishingMedia(newPreview, false);
            }
            if (mPublisher != null) {
                mPublisher.setCameraId(currentCamera);
            }
        }
    }

    /**
     * Sets an input signal processor. The input processor will process all the signals coming from
     * the wire. The SignalListeners will be invoked only on processed signals. That allows you to
     * easily implement and enforce a connection wide protocol for all sent and received signals.
     *
     * @param inputProtocol The input protocol you want to enforce. Pass null if you wish to receive
     *                      raw signals.
     */
    public synchronized void setInputSignalProtocol(SignalProtocol inputProtocol) {
        mInputSignalProtocol = inputProtocol;
        if (mSession != null) {
            mSession.setInputSignalProtocol(mInputSignalProtocol);
        }
    }

    /**
     * Sets an output signal protocol. The output protocol will process all the signals going to
     * the wire. A Signal will be sent using Opentok only after it has been processed by the protocol.
     * That allows you to easily implement and enforce a connection wide protocol for all sent and
     * received signals.
     *
     * @param outputProtocol
     */
    public synchronized void setOutputSignalProtocol(SignalProtocol outputProtocol) {
        mOutputSignalProtocol = outputProtocol;
        if (mSession != null) {
            mSession.setOutputSignalProtocol(mOutputSignalProtocol);
        }
    }

    /**
     * Get the OTAcceleratorSession
     *
     * @return The session
     */
    public OTAcceleratorSession getSession() {
        return mSession;
    }

    //Private methods
    private void cleanup() {
        if (mSession != null) {
            mSession.cleanUpSignals();
            if (mSubscribers.size() > 0) {
                for (Subscriber subscriber : mSubscribers.values()) {
                    if (subscriber != null) {
                        mSession.unsubscribe(subscriber);
                    }
                }
            }
            if (mPublisher != null) {
                mSession.unpublish(mPublisher);
            }
            mSession.disconnect();
        }
        mConnectionsCount = 0;
        mPublisher = null;
        mSession = null;
        mSubscribers = new HashMap<>();
        mStreams = new ConcurrentHashMap<>();
        mSessionConnection = null;
        isPreviewing = false;
        isPublishing = false;
        isSharingScreen = false;
        isScreensharingByDefault = false;
        mScreensharingFragment = null;
    }

    private BaseOTListener addOTListener(BaseOTListener listener,
                                         HashMap retriableMap,
                                         HashSet listenerSet) {
        boolean isWrapped = listener instanceof RetriableOTListener;
        RetriableOTListener realListener = (RetriableOTListener) retriableMap.get(listener);
        if (realListener == null) {
            realListener =
                    (RetriableOTListener) (isWrapped ? listener : getUnfailingFromBaseListener(listener));
            retriableMap.put(listener, (isWrapped ? listener : realListener));
            listenerSet.add(realListener);
            refreshPeerList();
        }
        return (BaseOTListener) realListener;

    }

    private void removeOTListener(BaseOTListener listener, HashMap retriableMap,
                                  HashSet listenerSet) {
        if (listener != null) {
            BaseOTListener internalListener = listener instanceof RetriableOTListener ?
                    ((RetriableOTListener) listener).getInternalListener() :
                    listener;
            RetriableOTListener realListener = (RetriableOTListener) retriableMap.get(internalListener);
            listenerSet.remove(realListener);
            retriableMap.remove(internalListener);
        } else {
            listenerSet.clear();
            retriableMap.clear();
        }
    }

    private synchronized void publishIfReady() {
        LOG.d(LOG_TAG, "publishIfReady: ", mSessionConnection, ", ", mPublisher, ", ",
                startPublishing, ", ", isPreviewing);
        if (mSession != null && mSessionConnection != null && mPublisher != null && startPublishing) {
            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT);
            if (!isPreviewing) {
                attachPublisherView();
            }
            if (!isPublishing) {
                mSession.publish(mPublisher);
                // Do this as soon as possible to avoid race conditions...
                isPublishing = true;
            }
        }
    }

    private synchronized void publishIfScreenReady() {
        LOG.d(LOG_TAG, "publishIfScreenReady: ", mSessionConnection, ", ", mScreenPublisher, ", ",
                startSharingScreen);
        if (mSession != null && mSessionConnection != null
                && mScreenPublisher != null && startSharingScreen && !isScreensharingByDefault) {

            if (!isPreviewing) {
                attachPublisherScreenView();
            }
            if (!isSharingScreen) {
                mSession.publish(mScreenPublisher);
                // Do this as soon as possible to avoid race conditions...
                isSharingScreen = true;
            }
        }
    }

    private void createPublisher() {
        //TODO: add more cases
        LOG.d(LOG_TAG, "createPublisher: ", mPreviewConfig);
        Publisher.Builder builder = new Publisher.Builder(mContext);

        if (mPreviewConfig != null) {
            builder.name(mPreviewConfig.getName());
            if (mPreviewConfig.getResolution() != Publisher.CameraCaptureResolution.MEDIUM ||
                    mPreviewConfig.getFrameRate() != Publisher.CameraCaptureFrameRate.FPS_15) {
                LOG.d(LOG_TAG, "createPublisher: Creating publisher with: ",
                        mPreviewConfig.getResolution(), ", ", mPreviewConfig.getFrameRate());
                builder.resolution(mPreviewConfig.getResolution());
                builder.frameRate(mPreviewConfig.getFrameRate());

            } else {
                LOG.d(LOG_TAG, "createPublisher: Creating Publisher with audio and video specified");
                builder.audioTrack(mPreviewConfig.isAudioTrack()).videoTrack(mPreviewConfig.isVideoTrack());
            }

            if (mPreviewConfig.getCapturer() != null) {
                //custom video capturer
                builder.capturer(mPreviewConfig.getCapturer());
            }
            if (mPreviewConfig.getRenderer() != null) {
                builder.renderer(mPreviewConfig.getRenderer());
            }
        } else {
            LOG.d(LOG_TAG, "createPublisher: Creating DefaultPublisher");
        }
        mPublisher = builder.build();
        mPublisher.setPublisherListener(mPublisherListener);
        mPublisher.setCameraListener(mCameraListener);
        //byDefault
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
    }

    private void createScreenPublisher(PreviewConfig config) {
        LOG.d(LOG_TAG, "createScreenPublisher: ", config);
        mScreenPublisherBuilder = new Publisher.Builder(mContext);

        if (config != null) {
            mScreenPublisherBuilder.name(config.getName());
            if (config.getResolution() != Publisher.CameraCaptureResolution.MEDIUM ||
                    config.getFrameRate() != Publisher.CameraCaptureFrameRate.FPS_15) {
                LOG.d(LOG_TAG, "createPublisher: Creating publisher with: ", config.getResolution(),
                        ", ", config.getFrameRate());
                mScreenPublisherBuilder.resolution(config.getResolution()).frameRate(config.getFrameRate());
            } else {
                LOG.d(LOG_TAG, "createPublisher: Creating Publisher with audio and video specified");
                mScreenPublisherBuilder.audioTrack(config.isAudioTrack()).videoTrack(config.isVideoTrack());
            }

            if (config.getCapturer() != null) {
                //custom video capturer
                mScreenPublisherBuilder.capturer(config.getCapturer());
            } else {
                //create screensharing by default
                isScreensharingByDefault = true;
                mScreensharingFragment = ScreenSharingFragment.newInstance();
                ((FragmentActivity) mContext).getSupportFragmentManager().beginTransaction()
                        .add(mScreensharingFragment, "screensharing-fragment").commit();
                mScreensharingFragment.setListener(screenListener);
            }

            if (config.getRenderer() != null) {
                mScreenPublisherBuilder.renderer(config.getRenderer());
            }

        } else {
            LOG.d(LOG_TAG, "createPublisher: Creating DefaultPublisher");
        }
        mScreenPublisher = mScreenPublisherBuilder.build();
        if (!isScreensharingByDefault) {
            mScreenPublisher.setPublisherListener(mPublisherListener);
            mScreenPublisher.setAudioLevelListener(mAudioLevelListener);
            mScreenPublisher.setCameraListener(mCameraListener);
            mScreenPublisher.
                    setPublisherVideoType(PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen);

            //byDefault
            mScreenPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                    BaseVideoRenderer.STYLE_VIDEO_FILL);
        }
    }

    /**
     * Rounds to the smallest FPS. Could round to the closest one instead!
     *
     * @param framesPerSecond
     */
    private Publisher.CameraCaptureFrameRate getFPS(int framesPerSecond) {
        Publisher.CameraCaptureFrameRate returnedValue;
        if (framesPerSecond < 7) {
            returnedValue = Publisher.CameraCaptureFrameRate.FPS_1;
        } else if (framesPerSecond < 15) {
            returnedValue = Publisher.CameraCaptureFrameRate.FPS_7;
        } else if (framesPerSecond < 30) {
            returnedValue = Publisher.CameraCaptureFrameRate.FPS_15;
        } else {
            returnedValue = Publisher.CameraCaptureFrameRate.FPS_30;
        }
        return returnedValue;
    }

    private void attachPublisherView() {
        if (mPublisher != null && !isNullOrEmpty(mBasicListeners)) {
            for (BasicListener listener : mBasicListeners) {
                ((RetriableBasicListener) listener).onPreviewViewReady(SELF, mPublisher.getView());
            }
        }
    }

    private void attachPublisherScreenView() {
        if (mScreenPublisher != null && !isNullOrEmpty(mBasicListeners)) {
            for (BasicListener listener : mBasicListeners) {
                if (isScreensharingByDefault) {
                    ((RetriableBasicListener) listener).onPreviewViewReady(SELF, mScreensharingFragment.getScreen());
                } else {
                    ((RetriableBasicListener) listener).onPreviewViewReady(SELF, mScreenPublisher.getView());
                }
            }
        }
    }

    private void detachPublisherView() {
        if (mPublisher != null && !isNullOrEmpty(mBasicListeners)) {
            mPublisher.onStop();
            for (BasicListener listener : mBasicListeners) {
                ((RetriableBasicListener) listener).onPreviewViewDestroyed(SELF, mPublisher.getView());
            }
        }
    }

    private void detachPublisherScreenView() {
        if (mScreenPublisher != null && !isNullOrEmpty(mBasicListeners)) {
            mScreenPublisher.onStop();
            for (BasicListener listener : mBasicListeners) {
                ((RetriableBasicListener) listener).onPreviewViewDestroyed(SELF, mScreenPublisher.getView());
            }
        }
    }

    private void refreshPeerList() {
        if (!isNullOrEmpty(mBasicListeners)) {
            for (BasicListener listener : mBasicListeners) {
                if (((RetriableBasicListener) listener).getInternalListener() != null) {
                    if (mPublisher != null) {
                        ((RetriableBasicListener) listener).onPreviewViewReady(SELF, mPublisher.getView());
                    }
                    if (mScreenPublisher != null) {
                        ((RetriableBasicListener) listener).onPreviewViewReady(SELF,
                                mScreenPublisher.getView());
                    }
                    notifyRemoteViewReady((RetriableBasicListener) listener);
                }
            }
        }
    }

    private void notifyRemoteViewReady(RetriableBasicListener listener) {
        for (Subscriber sub : mSubscribers.values()) {
            Stream stream = sub.getStream();
            listener.onRemoteViewReady(SELF, sub.getView(), stream.getStreamId(),
                    stream.getConnection().getData());
        }
    }

    private void enableRemoteMedia(Subscriber sub, MediaType type, boolean enabled) {
        if (sub != null) {
            if (type == MediaType.VIDEO) {
                sub.setSubscribeToVideo(enabled);
            } else {
                sub.setSubscribeToAudio(enabled);
            }
        }
    }

    //Analytics
    private void initAnalytics() {
        //Init the analytics logging
        String source = mContext.getPackageName();

        SharedPreferences prefs = mContext.getSharedPreferences("opentok", Context.MODE_PRIVATE);
        String guidVSol = prefs.getString("guidVSol", null);
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString();
            prefs.edit().putString("guidVSol", guidVSol).commit();
        }

        mAnalyticsData = new OTKAnalyticsData.
                Builder(ClientLog.LOG_CLIENT_VERSION, source, ClientLog.LOG_COMPONENTID, guidVSol).build();
        mAnalytics = new OTKAnalytics(mAnalyticsData);
        mAnalytics.enableConsoleLog(false);

        mAnalyticsData.setSessionId(getOTConfig().getSessionId());
        mAnalyticsData.setPartnerId(getOTConfig().getApiKey());
        mAnalytics.setData(mAnalyticsData);
    }

    private void addLogEvent(String action, String variation) {
        if (mAnalytics != null) {
            mAnalytics.logEvent(action, variation);
        }
    }
}
