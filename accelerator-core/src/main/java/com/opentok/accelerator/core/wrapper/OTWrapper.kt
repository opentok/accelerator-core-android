package com.opentok.accelerator.core.wrapper

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.opentok.accelerator.core.GlobalLogLevel
import com.opentok.accelerator.core.listeners.AdvancedListener
import com.opentok.accelerator.core.listeners.BaseOTListener
import com.opentok.accelerator.core.listeners.BasicListener
import com.opentok.accelerator.core.listeners.RetriableAdvancedListener
import com.opentok.accelerator.core.listeners.RetriableBasicListener
import com.opentok.accelerator.core.listeners.RetriableOTListener
import com.opentok.accelerator.core.listeners.SignalListener
import com.opentok.accelerator.core.listeners.UnfailingAdvancedListener
import com.opentok.accelerator.core.listeners.UnfailingBasicListener
import com.opentok.accelerator.core.screensharing.ScreenSharingFragment
import com.opentok.accelerator.core.screensharing.ScreenSharingFragment.ScreenSharingListener
import com.opentok.accelerator.core.signal.SignalInfo
import com.opentok.accelerator.core.signal.SignalProtocol
import com.opentok.accelerator.core.utils.ClientLog
import com.opentok.accelerator.core.utils.LogWrapper
import com.opentok.accelerator.core.utils.MediaType
import com.opentok.accelerator.core.utils.OTConfig
import com.opentok.accelerator.core.utils.PreviewConfig
import com.opentok.accelerator.core.utils.PreviewConfig.PreviewConfigBuilder
import com.opentok.accelerator.core.utils.StreamStatus
import com.opentok.accelerator.core.utils.VideoScale
import com.opentok.android.BaseVideoRenderer
import com.opentok.android.Connection
import com.opentok.android.OpentokError
import com.opentok.android.OpentokError.ErrorCode
import com.opentok.android.Publisher
import com.opentok.android.Publisher.CameraCaptureFrameRate
import com.opentok.android.Publisher.CameraListener
import com.opentok.android.PublisherKit
import com.opentok.android.PublisherKit.PublisherListener
import com.opentok.android.Session
import com.opentok.android.Session.ConnectionListener
import com.opentok.android.Session.ReconnectionListener
import com.opentok.android.Session.SessionListener
import com.opentok.android.Stream
import com.opentok.android.Stream.StreamVideoType
import com.opentok.android.Subscriber
import com.opentok.android.SubscriberKit
import com.opentok.android.SubscriberKit.StreamListener
import com.opentok.android.SubscriberKit.SubscriberListener
import com.opentok.android.SubscriberKit.VideoListener
import com.tokbox.android.logging.OTKAnalytics
import com.tokbox.android.logging.OTKAnalyticsData
import java.util.HashMap
import java.util.HashSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.and

/**
 * Creates an OTWrapper instance enable communication.
 *
 * @param context  Activity context. Needed by the Opentok APIs
 * @param otConfig OTConfig: Information about the OpenTok session. This includes all the needed
 * data to connect.
 */
class OTWrapper(private val context: Context, private val otConfig: OTConfig) {

    init {
        initAnalytics()
    }

    private val SELF = this

    /**
     * Get the OTAcceleratorSession
     *
     * @return The session
     */
    var otAcceleratorSession: OTAcceleratorSession? = null
        private set
    private var mSessionConnection: Connection? = null
    private var mPublisher: Publisher? = null
    private var mScreenPublisher: Publisher? = null

    //indexed by streamId, *not* per subscriber Id
    private var mSubscribers = HashMap<String, Subscriber>()
    private var mStreams = ConcurrentHashMap<String, Stream>()

    //listeners
    private val mBasicListeners = HashSet<RetriableBasicListener<OTWrapper>>()
    private val mAdvancedListeners = HashSet<RetriableAdvancedListener<OTWrapper>>()
    private val mRetriableBasicListeners = HashMap<BasicListener<*>, RetriableBasicListener<*>>()
    private val mRetriableAdvancedListeners = HashMap<AdvancedListener<*>, RetriableAdvancedListener<*>>()

    /**
     * Returns the number of active connections for the current session
     *
     * @return the number of active connections.
     */
    val connectionCount get() = otAcceleratorSession?.connectionCount ?: 0

    private var mOlderThanMe = 0

    /**
     * Whether the local is previewing (`true`) or not (
     * `false`).
     */
    var isPreviewing = false
        private set

    /**
     * Whether the local is sharing media (`true`) or not (
     * `false`).
     */
    var isPublishing = false
        private set
    private var startPublishing = false
    private var startSharingScreen = false
    private var isSharingScreen = false

    /**
     * Returns the OpenTok Configuration
     *
     * @return current OpenTok Configuration
     */
    private var mPreviewConfig: PreviewConfig? = null

    //Screen Sharing by default
    private var mScreenSharingFragment: ScreenSharingFragment? = null
    private var isScreenSharingByDefault = false
    private var mScreenPublisherBuilder: Publisher.Builder? = null

    //Custom renderer
    private var mVideoRemoteRenderer: BaseVideoRenderer? = null
    private var mScreenRemoteRenderer: BaseVideoRenderer? = null

    //Signal protocol
    private var mInputSignalProtocol: SignalProtocol<*, *>? = null
    private var mOutputSignalProtocol: SignalProtocol<*, *>? = null

    //Analytics for internal use
    private var mAnalyticsData: OTKAnalyticsData? = null
    private var mAnalytics: OTKAnalytics? = null

    private val mConnectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnectionCreated(session: Session, connection: Connection) {
            LOG.d(LOG_TAG, "onConnectionCreated: ", connection.data)

            // ToDo: This should added/removed inside otAcceleratorSession
            otAcceleratorSession?.addConnection(connection.connectionId, connection)

            if (connection.creationTime <= mSessionConnection?.creationTime) {
                mOlderThanMe++
            }

            for (listener in mBasicListeners) {
                listener.onConnected(
                    SELF, connectionCount,
                    connection.connectionId,
                    connection.data
                )
            }
        }

        override fun onConnectionDestroyed(session: Session, connection: Connection) {
            LOG.d(LOG_TAG, "onConnectionDestroyed: ", connection.data)
            otAcceleratorSession?.removeConnection(connection.connectionId)

            if (connection.creationTime <= mSessionConnection?.creationTime) {
                mOlderThanMe--
            }

            mBasicListeners.forEach {
                it.onDisconnected(SELF, connectionCount, connection.connectionId, connection.data)
            }
        }
    }
    private val mSubscriberListener: SubscriberListener = object : SubscriberListener {
        override fun onConnected(sub: SubscriberKit) {
            LOG.i(LOG_TAG, "Subscriber is connected")
            addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_SUCCESS)

            for (listener in mBasicListeners) {
                val stream = sub.stream
                listener.onRemoteViewReady(
                    SELF, sub.view, stream.streamId,
                    stream.connection.data
                )
            }
        }

        override fun onDisconnected(sub: SubscriberKit) {
            addLogEvent(ClientLog.LOG_ACTION_REMOVE_REMOTE, ClientLog.LOG_VARIATION_SUCCESS)

            mBasicListeners.forEach {
                it.onRemoteViewDestroyed(SELF, sub.stream.streamId)
            }
        }

        override fun onError(subscriberKit: SubscriberKit, opentokError: OpentokError) {
            LOG.e(LOG_TAG, "Subscriber: onError ", opentokError.errorCode, ", ", opentokError.message)

            val id = subscriberKit.stream.streamId

            when (opentokError.errorCode) {
                ErrorCode.SubscriberInternalError -> {
                    //TODO: Add client logs for the different subscribers errors
                    LOG.e(LOG_TAG, "Subscriber error: SubscriberInternalError")
                    mSubscribers.remove(id)
                }
                ErrorCode.ConnectionTimedOut -> {
                    addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_ERROR)
                    // Just try again
                    if (otAcceleratorSession != null) {
                        addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_ATTEMPT)
                        otAcceleratorSession?.subscribe(subscriberKit)
                    }
                }
                ErrorCode.SubscriberWebRTCError -> {
                    LOG.e(LOG_TAG, "Subscriber error: SubscriberWebRTCError")
                    mSubscribers.remove(id)
                }
                ErrorCode.SubscriberServerCannotFindStream -> {
                    LOG.e(LOG_TAG, "Subscriber error: SubscriberServerCannotFindStream")
                    mSubscribers.remove(id)
                }
                else -> {
                    LOG.e(LOG_TAG, "Subscriber error: default ")
                    mSubscribers.remove(id)

                    if (!mStreams.containsKey(id)) {
                        mStreams[id] = subscriberKit.stream
                    }

                    mBasicListeners.forEach {
                        it.onError(SELF, opentokError)
                    }
                }
            }
        }
    }
    private val mAudioLevelListener = PublisherKit.AudioLevelListener { publisherKit, audioLevel ->
        mAdvancedListeners.forEach {
            it.onAudioLevelUpdated(audioLevel)
        }
    }

    //Implements Advanced listeners
    private val mReconnectionListener: ReconnectionListener = object : ReconnectionListener {
        override fun onReconnecting(session: Session) {
            mAdvancedListeners.forEach {
                it.onReconnecting(SELF)
            }
        }

        override fun onReconnected(session: Session) {
            mAdvancedListeners.forEach {
                it.onReconnected(SELF)
            }
        }
    }

    private val mVideoListener: VideoListener = object : VideoListener {
        override fun onVideoDataReceived(subscriberKit: SubscriberKit) {
            //todo: review: a new listener to indicate the first frame received
        }

        override fun onVideoDisabled(subscriber: SubscriberKit, reason: String) {
            mBasicListeners.forEach {
                it.onRemoteVideoChanged(
                    SELF, subscriber.stream.streamId, reason, false,
                    subscriber.subscribeToVideo
                )
            }
        }

        override fun onVideoEnabled(subscriber: SubscriberKit, reason: String) {
            mBasicListeners.forEach {
                it.onRemoteVideoChanged(
                    SELF, subscriber.stream.streamId, reason, true,
                    subscriber.subscribeToVideo
                )
            }
        }

        override fun onVideoDisableWarning(subscriber: SubscriberKit) {
            mAdvancedListeners.forEach {
                it.onVideoQualityWarning(SELF, subscriber.stream.streamId)
            }
        }

        override fun onVideoDisableWarningLifted(subscriber: SubscriberKit) {
            mAdvancedListeners.forEach {
                it.onVideoQualityWarningLifted(
                    SELF,
                    subscriber.stream.streamId
                )
            }
        }
    }

    private val mCameraListener: CameraListener = object : CameraListener {
        override fun onCameraChanged(publisher: Publisher, i: Int) {
            mAdvancedListeners.forEach {
                it.onCameraChanged(SELF)
            }
        }

        override fun onCameraError(publisher: Publisher, opentokError: OpentokError) {
            LOG.d(LOG_TAG, "onCameraError: onError ", opentokError.message)

            mAdvancedListeners.forEach {
                it.onError(SELF, opentokError)
            }
        }
    }

    private val mStreamListener: StreamListener = object : StreamListener {
        override fun onReconnected(subscriber: SubscriberKit) {
            mAdvancedListeners.forEach {
                it.onReconnected(SELF, subscriber.stream.streamId)
            }
        }

        override fun onDisconnected(subscriber: SubscriberKit) {
            mAdvancedListeners.forEach {
                it.onDisconnected(SELF, subscriber.stream.streamId)
            }
        }

        override fun onAudioEnabled(subscriber: SubscriberKit) {
            mAdvancedListeners.forEach {
                it.onAudioEnabled(SELF, subscriber.stream.streamId)
            }
        }

        override fun onAudioDisabled(subscriber: SubscriberKit) {
            mAdvancedListeners.forEach {
                it.onAudioDisabled(SELF, subscriber.stream.streamId)
            }
        }
    }

    //Implements Basic listeners: Session.SessionListener, Session.ConnectionListener,
    // Session.SignalListener, Publisher.PublisherListener
    private val mSessionListener: SessionListener = object : SessionListener {

        override fun onConnected(session: Session) {
            val connection = session.connection
            mSessionConnection = connection
            otAcceleratorSession?.addConnection(connection.connectionId, connection)

            //update internal client logs with connectionId
            mAnalyticsData?.connectionId = connection.connectionId
            mAnalytics?.data = mAnalyticsData

            LOG.d(LOG_TAG, "onConnected: ", connection.data, ". listeners: ", mBasicListeners)
            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_SUCCESS)

            publishIfReady()
            mBasicListeners.forEach {
                it.onConnected(SELF, connectionCount, connection.connectionId, connection.data)
            }
        }

        override fun onDisconnected(session: Session) {
            addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_SUCCESS)
            if (otAcceleratorSession == null || mSessionConnection == null) {
                // This can happen if somehow onError was called before onDisconnected or if we somehow
                // call onDisconnected twice (so cleanup has been done already)

                LOG.w(LOG_TAG, "OnDisconnected called on a stale object")
                mSessionConnection = session.connection
            }
            if (mSessionConnection != null) {
                mBasicListeners.forEach {
                    it.onDisconnected(
                        SELF, 0,
                        mSessionConnection?.connectionId,
                        mSessionConnection?.data
                    )
                }
            }
            cleanup()
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            LOG.d(LOG_TAG, "OnStreamReceived: ", stream.connection.data)

            mStreams[stream.streamId] = stream

            if (otConfig.subscribeAutomatically) {
                addRemote(stream)
            }

            mBasicListeners.forEach {
                it.onRemoteJoined(SELF, stream.streamId)
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            LOG.d(LOG_TAG, "OnStreamDropped: ", stream.connection.data)
            val subId = stream.streamId

            if (mStreams.containsKey(subId)) {
                mStreams.remove(stream.streamId)
            }

            if (mSubscribers.containsKey(subId)) {
                mSubscribers.remove(stream.streamId)
            }

            mBasicListeners.forEach {
                it.onRemoteLeft(SELF, subId)
                it.onRemoteViewDestroyed(SELF, subId)
            }
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            LOG.e(LOG_TAG, "Session: onError ", opentokError.message)

            if (ownConnId != null) {
                addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_ERROR)
            } else {
                addLogEvent(ClientLog.LOG_ACTION_CONNECT, ClientLog.LOG_VARIATION_ERROR)
            }

            cleanup()

            mBasicListeners.forEach {
                it.onError(SELF, opentokError)
            }
        }
    }
    private val mPublisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            var screenSharing = false
            if (stream.streamVideoType == StreamVideoType.StreamVideoTypeScreen) {
                addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_SUCCESS)
                screenSharing = true
            } else {
                addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_SUCCESS)
                isPublishing = true
            }

            mBasicListeners.forEach {
                it.onStartedPublishingMedia(SELF, screenSharing)
            }

            //check subscribe to self
            if (otConfig.subscribeToSelf) {
                addRemote(stream)
            }
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            var screenSharing = false
            if (stream.streamVideoType == StreamVideoType.StreamVideoTypeScreen) {
                addLogEvent(ClientLog.LOG_ACTION_END_SCREEN_COMM, ClientLog.LOG_VARIATION_SUCCESS)
                screenSharing = true
            } else {
                addLogEvent(ClientLog.LOG_ACTION_END_COMM, ClientLog.LOG_VARIATION_SUCCESS)
                isPublishing = false
            }
            mBasicListeners.forEach {
                it.onStoppedPublishingMedia(SELF, screenSharing)
            }
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            LOG.e(
                LOG_TAG, "Publisher: onError ", opentokError.errorCode, ", ",
                opentokError.message
            )
            val errorCode = opentokError.errorCode
            if (publisherKit.stream != null && publisherKit.stream.streamVideoType == StreamVideoType.StreamVideoTypeCamera) {
                addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ERROR)
            } else {
                addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_ERROR)
            }
            when (errorCode) {
                ErrorCode.PublisherInternalError -> {
                    //TODO: Add client logs for the different publisher errors
                    LOG.e(LOG_TAG, "Publisher error: PublisherInternalError")
                    mPublisher = null
                }
                ErrorCode.PublisherTimeout ->                     //re-try publishing
                    if (otAcceleratorSession != null) {
                        if (publisherKit.stream != null && publisherKit.stream.streamVideoType == StreamVideoType.StreamVideoTypeCamera) {
                            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
                        } else {
                            addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
                        }
                        otAcceleratorSession?.publish(publisherKit)
                    }
                ErrorCode.PublisherWebRTCError -> {
                    LOG.e(LOG_TAG, "Publisher error: PublisherWebRTCError")
                    mPublisher = null
                }
                else -> {
                    LOG.e(LOG_TAG, "Publisher error: default")
                    mPublisher = null

                    mBasicListeners.forEach {
                        it.onError(SELF, opentokError)
                    }
                }
            }
        }
    }
    private var screenListener: ScreenSharingListener = object : ScreenSharingListener {
        override fun onScreenCapturerReady() {
            val capturer = mScreenSharingFragment?.screenCapturer

            if (capturer != null) {

                mScreenPublisherBuilder?.capturer(capturer)

                mScreenPublisher = mScreenPublisherBuilder?.build()?.apply {
                    setPublisherListener(mPublisherListener)
                    setAudioLevelListener(mAudioLevelListener)
                    setCameraListener(mCameraListener)
                    publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen
                    setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                }

                attachPublisherScreenView()
                otAcceleratorSession?.publish(mScreenPublisher)
            }
        }

        override fun onError(errorMsg: String) {
            LOG.i(LOG_TAG, "Error in Screen Sharing by default")

            mBasicListeners.forEach {
                val error = OpentokError(
                    OpentokError.Domain.PublisherErrorDomain,
                    ErrorCode.PublisherInternalError.errorCode,
                    errorMsg
                )

                it.onError(SELF, error)
            }
        }
    }

    /**
     * Call this method when the app's activity pauses.
     * This pauses the video for the local preview and remotes
     */
    fun pause() {
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.onPause()
        }
    }

    /**
     * Call this method when the app's activity resumes.
     * This resumes the video for the local preview and remotes.
     *
     * @param resumeEvents Set to true if the events should be resumed
     */
    fun resume(resumeEvents: Boolean) {
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.onResume()
        }

        if (resumeEvents) {
            mBasicListeners.forEach {
                it.resume()
            }
        }
    }

    /**
     * Connects to the OpenTok session.
     * When the otwrapper connects, the
     * [BasicListener.onConnected] method is called.
     * If the otwrapper fails to connect, the
     * [BasicListener.onError] method is called.
     */
    fun connect() {
        addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
        otAcceleratorSession = OTAcceleratorSession(context, otConfig.apiKey, otConfig.sessionId)
        otAcceleratorSession?.setConnectionListener(mConnectionListener)
        otAcceleratorSession?.setSessionListener(mSessionListener)
        otAcceleratorSession?.signalListener = otAcceleratorSession?.signalListener
        otAcceleratorSession?.setReconnectionListener(mReconnectionListener)
        mOlderThanMe = 0

        //check signal protocol
        if (mInputSignalProtocol != null) {
            otAcceleratorSession?.setInputSignalProtocol(mInputSignalProtocol)
        }
        if (mOutputSignalProtocol != null) {
            otAcceleratorSession?.setOutputSignalProtocol(mOutputSignalProtocol)
        }
        otAcceleratorSession?.connect(otConfig.token)
    }

    /**
     * Disconnects from the OpenTok session.
     * When the otwrapper disconnects, the
     * [BasicListener.onDisconnected]  method is called.
     * If the otwrapper fails to disconnect, the
     * [BasicListener.onError] method is called.
     */
    fun disconnect() {
        addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_ATTEMPT)
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.disconnect()
        }
    }

    /**
     * Returns the local connectionID
     *
     * @return the own connectionID
     */
    val ownConnId: String?
        get() = if (mSessionConnection != null) mSessionConnection?.connectionId else null

    /**
     * Checks if the own connection is the oldest in the current session
     *
     * @return Whether the local connection is oldest (`true`) or not (
     * `false`).
     */
    val isTheOldestConnection: Boolean
        get() = mOlderThanMe <= 0

    /**
     * Compares the connections creation times between the local connection and the argument passing
     *
     * @param connectionId The connection we want to compare with
     * @return -1 if the connection passed is newer than the current session connection, 0
     * if they have the same age, and 1 if the connection is older
     */
    fun compareConnectionsTimes(connectionId: String?): Int {

        val creationTime1 = otAcceleratorSession?.connection?.creationTime
        val creationTime2 = otAcceleratorSession?.getConnection(connectionId)?.creationTime

        if (creationTime1 == null || creationTime2 == null) {
            return 0
        }

        return creationTime1.compareTo(creationTime2)
    }

    /**
     * Returns the remote connectionID
     *
     * @param remoteId the remote Id
     * @return the remote connectionID
     */
    fun getRemoteConnId(remoteId: String): String {
        val remoteSub = mSubscribers[remoteId]

        requireNotNull(remoteSub)
        val connection = remoteSub.stream?.connection

        requireNotNull(connection)
        return connection.connectionId
    }

    /**
     * Call to display the camera's video in the Preview's view before it starts streaming
     * video.
     *
     * @param config The configuration of the preview
     */
    fun startPreview(config: PreviewConfig?) {
        mPreviewConfig = config
        if (mPublisher == null && !isPreviewing) {
            createPublisher()
            attachPublisherView()
            isPreviewing = true
        }
    }

    /**
     * Call to stop the camera's video in the Preview's view.
     */
    fun stopPreview() {
        if (mPublisher != null && isPreviewing) {
            detachPublisherView()
            mPublisher = null
            isPreviewing = false
            startPublishing = false
        }
    }

    /**
     * Starts the local streaming video
     *
     * @param config        The configuration of the preview
     * @param screenSharing Whether to indicate the camera or the screen streaming.
     */
    fun startPublishingMedia(config: PreviewConfig?, screenSharing: Boolean) {
        addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
        if (!screenSharing) {
            mPreviewConfig = config
            startPublishing = true
            if (mPublisher == null) {
                createPublisher()
            }
            publishIfReady()
        } else {
            startSharingScreen = true
            if (mScreenPublisher == null) {
                createScreenPublisher(config)
            }
            publishIfScreenReady()
        }
    }

    /**
     * Stops the local streaming video.
     *
     * @param screenSharing Whether to indicate the camera or the screen streaming
     */
    fun stopPublishingMedia(screenSharing: Boolean) {
        if (!screenSharing) {
            addLogEvent(ClientLog.LOG_ACTION_END_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
            if (mPublisher != null && startPublishing) {
                otAcceleratorSession?.unpublish(mPublisher)
            }
            isPublishing = false
            startPublishing = false
            if (!isPreviewing) {
                detachPublisherView()
                mPublisher = null
            }
        } else {
            addLogEvent(ClientLog.LOG_ACTION_END_SCREEN_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
            if (mScreenSharingFragment != null) {
                mScreenSharingFragment?.stopScreenCapture()
                isScreenSharingByDefault = false
            }
            detachPublisherScreenView()
            if (mScreenPublisher != null && startSharingScreen) {
                otAcceleratorSession?.unpublish(mScreenPublisher)
            }
            isSharingScreen = false
            startSharingScreen = false
            mScreenPublisher = null
        }
    }

    /**
     * Returns Local Media status
     *
     * @param type MediaType (Audio or Video)
     * @return Whether the local MediaType is enabled (`true`) or not (
     * `false`)
     */
    fun isLocalMediaEnabled(type: MediaType): Boolean {
        if (mPublisher == null) return false

        val publishVideo = mPublisher?.publishVideo ?: false
        val publishAudio = mPublisher?.publishAudio ?: false

        return if (type == MediaType.VIDEO) publishVideo else publishAudio
    }

    /**
     * Enables or disables the local Media.
     *
     * @param type    MediaType (Audio or Video)
     * @param enabled Whether to enable media (`true`) or not (
     * `false`).
     */
    fun enableLocalMedia(type: MediaType?, enabled: Boolean) {
        if (mPublisher != null) {
            when (type) {
                MediaType.AUDIO -> mPublisher?.publishAudio = enabled

                MediaType.VIDEO -> {
                    mPublisher?.publishVideo = enabled

                    if (enabled) {
                        mPublisher?.view?.visibility = View.VISIBLE
                    } else {
                        mPublisher?.view?.visibility = View.GONE
                    }
                }
                else -> {
                }
            }
        }
    }

    /**
     * Enables or disables the media of the remote with remoteId.
     *
     * @param type    MediaType (Audio or video)
     * @param enabled Whether to enable MediaType (`true`) or not (
     * `false`).
     */
    fun enableReceivedMedia(remoteId: String?, type: MediaType, enabled: Boolean) {
        if (remoteId != null) {
            enableRemoteMedia(mSubscribers[remoteId], type, enabled)
        } else {
            val subscribers: Collection<Subscriber> = mSubscribers.values

            subscribers.forEach {
                enableRemoteMedia(it, type, enabled)
            }
        }
    }

    /**
     * Returns the MediaType status of the remote with remoteId
     *
     * @param type MediaType: audio or video
     * @return Whether the remote MediaType is enabled (`true`) or not (
     * `false`).
     */
    fun isReceivedMediaEnabled(remoteId: String, type: MediaType): Boolean {
        val sub = mSubscribers[remoteId] ?: return false

        return if (type == MediaType.VIDEO) sub.subscribeToVideo else sub.subscribeToAudio
    }

    /**
     * Subscribe to a specific remote
     *
     * @param remoteId String to identify the remote
     */
    fun addRemote(remoteId: String) {
        LOG.i(LOG_TAG, "Add remote with ID: ", remoteId)
        addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_ATTEMPT)

        val stream = mStreams[remoteId]
        requireNotNull(stream) { "No stream found for remoteId $remoteId" }
        addRemote(stream)
    }

    /**
     * Subscribe to a specific remote
     *
     * @param stream Stream to identify the remote
     */
    private fun addRemote(stream: Stream) {
        LOG.i(LOG_TAG, "private add new remote stream != null")

        val sub = Subscriber(context, stream).apply {
            setVideoListener(mVideoListener)
            setStreamListener(mStreamListener)
            setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
        }

        val subId = stream.streamId
        mSubscribers[subId] = sub
        sub.setSubscriberListener(mSubscriberListener)

        if (stream.streamVideoType == StreamVideoType.StreamVideoTypeCamera && mVideoRemoteRenderer != null) {
            sub.renderer = mVideoRemoteRenderer
        } else {
            if (stream.streamVideoType == StreamVideoType.StreamVideoTypeScreen && mScreenRemoteRenderer != null) {
                sub.renderer = mScreenRemoteRenderer
            }
        }
        //remove the sub's stream from the streams list to avoid subscribe twice to the same stream
        mStreams.remove(sub.stream.streamId)
        otAcceleratorSession?.subscribe(sub)
    }

    /**
     * Unsusbscribe from a specific remote
     *
     * @param remoteId String to identify the remote
     */
    fun removeRemote(remoteId: String) {
        LOG.i(LOG_TAG, "Remove remote with ID: ", remoteId)
        addLogEvent(ClientLog.LOG_ACTION_REMOVE_REMOTE, ClientLog.LOG_VARIATION_ATTEMPT)
        val subscriber = mSubscribers[remoteId]

        requireNotNull(subscriber) { "No subscriber found for remoteId $remoteId" }

        mSubscribers.remove(remoteId)
        mStreams[remoteId] = subscriber.stream
        otAcceleratorSession?.unsubscribe(subscriber)
    }

    /**
     * Call to cycle between cameras, if there are multiple cameras on the device.
     */
    fun cycleCamera() {
        if (mPublisher != null) {
            mPublisher?.cycleCamera()
        }
    }

    private fun getUnfailingFromBaseListener(listener: BaseOTListener): RetriableOTListener<*> {
        return if (listener is BasicListener<*>) UnfailingBasicListener<Any?>(listener) else UnfailingAdvancedListener<Any>(
            listener as AdvancedListener<*>
        )
    }

    /**
     * Adds a [BasicListener]. If the listener was already added, nothing is done.
     *
     * @param listener
     * @return The added listener
     */
    public fun addBasicListener(listener: BasicListener<*>): BaseOTListener? {
        LOG.d(LOG_TAG, "Adding BasicListener")

        val isWrapped = listener is RetriableOTListener<*>
        var realListener = mRetriableBasicListeners[listener]

        if (realListener == null) {
            realListener = if (isWrapped) {
                listener as RetriableBasicListener<OTWrapper>
            } else {
                getUnfailingFromBaseListener(listener) as RetriableBasicListener<OTWrapper>
            }

            mRetriableBasicListeners[listener] = if (isWrapped) listener as RetriableBasicListener<*> else realListener
            mBasicListeners.add(realListener)
            refreshPeerList()
        }

        return realListener
    }

    /**
     * Removes a [BasicListener]
     *
     * @param listener
     */
    fun removeBasicListener(listener: BasicListener<*>?) {
        removeOTListener(listener, mRetriableBasicListeners, mBasicListeners)
    }

    /**
     * Adds an [AdvancedListener]
     *
     * @param listener
     * @return The removed listener
     */
    fun addAdvancedListener(listener: AdvancedListener<OTWrapper?>): AdvancedListener<*>? =
        addAdvancedOTListener(listener) as AdvancedListener<*>?

    private fun addAdvancedOTListener(
        listener: AdvancedListener<*>,
    ): BaseOTListener {

        val isWrapped = listener is RetriableOTListener<*>
        var realListener = mRetriableAdvancedListeners[listener]

        if (realListener == null) {
            realListener = if (isWrapped) {
                listener as RetriableAdvancedListener<OTWrapper>
            } else {
                getUnfailingFromBaseListener(listener) as RetriableAdvancedListener<OTWrapper>
            }

            mRetriableAdvancedListeners[listener] =
                if (isWrapped) listener as RetriableAdvancedListener<*> else realListener
            mAdvancedListeners.add(realListener)
            refreshPeerList()
        }

        return realListener
    }

    /**
     * Removes an [AdvancedListener]
     *
     * @param listener
     */
    fun removeAdvancedListener(listener: AdvancedListener<*>?) {
        removeOTListener(listener, mRetriableAdvancedListeners, mAdvancedListeners)
    }

    /**
     * Registers a signal listener for a given signal.
     *
     * @param signalName Name of the signal this listener will listen to. Pass "*" if the listener
     * is to be invoked for all signals.
     * @param listener   Listener that will be invoked when a signal is received.
     */
    fun addSignalListener(signalName: String?, listener: SignalListener<*>?) {
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.addSignalListener(signalName, listener)
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
    fun removeSignalListener(listener: SignalListener<*>?) {
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.removeSignalListener(listener)
        }
    }

    /**
     * Removes a signal listener.
     *
     * @param signalName Name of the signal this listener will listen to. Pass "*" if the listener
     * is to be invoked for all signals.
     * @param listener   Listener to be removed.
     */
    fun removeSignalListener(signalName: String?, listener: SignalListener<*>?) {
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.removeSignalListener(signalName, listener)
        }
    }

    /**
     * Sends a new signal
     *
     * @param signalInfo [SignalInfo] of the signal to be sent
     */
    fun sendSignal(signalInfo: SignalInfo<*>) {
        if (otAcceleratorSession != null) {
            if (signalInfo.mDstConnId != null) {
                val connection = otAcceleratorSession?.getConnection(signalInfo.mDstConnId)
                otAcceleratorSession?.sendSignal(signalInfo, connection)
            } else {
                otAcceleratorSession?.sendSignal(signalInfo, null)
            }
        }
    }

    /**
     * Returns the [StreamStatus] of the local.
     *
     * @return The [StreamStatus] of the local.
     */
    val localStreamStatus: StreamStatus?
        get() {
            if (mPublisher != null) {
                val stream = mPublisher?.stream

                var hasAudio = true
                var hasVideo = true

                var videoHeight = 0
                var videoWidth = 0

                var streamVideoType: StreamVideoType? = StreamVideoType.StreamVideoTypeCamera

                if (stream != null) {
                    hasAudio = stream.hasAudio()
                    hasVideo = stream.hasVideo()
                    streamVideoType = stream.streamVideoType
                    videoHeight = stream.videoHeight
                    videoWidth = stream.videoWidth
                }

                val publishAudio = mPublisher?.publishAudio ?: false
                val publishVideo = mPublisher?.publishVideo ?: false

                return StreamStatus(
                    mPublisher?.view,
                    publishAudio,
                    publishVideo,
                    hasAudio,
                    hasVideo,
                    streamVideoType,
                    videoWidth, videoHeight
                )
            }
            return null
        }

    /**
     * Returns the stream status for the requested subscriber (actually, subId is the streamId..)
     *
     * @param id Id of the subscriber/stream
     * @return The status including the view, and if it's subscribing to video and if it has local
     * video
     */
    fun getRemoteStreamStatus(id: String): StreamStatus? {
        val sub = mSubscribers[id]
        if (sub != null) {
            val subSt = sub.stream
            return StreamStatus(
                sub.view, sub.subscribeToAudio, sub.subscribeToVideo,
                subSt.hasAudio(), subSt.hasVideo(), subSt.streamVideoType,
                subSt.videoWidth, subSt.videoHeight
            )
        }
        return null
    }

    /**
     * Sets the  Video Scale style for a remote
     *
     * @param remoteId the remote subscriber ID
     * @param style    VideoScale value: FILL or FIT
     */
    fun setRemoteStyle(remoteId: String, style: VideoScale) {
        val sub = mSubscribers[remoteId]
        if (sub != null) {
            if (style == VideoScale.FILL) {
                sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            } else {
                sub.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FIT)
            }
        }
    }

    /**
     * Sets the Local Video Style
     *
     * @param style VideoScale value: FILL or FIT
     */
    fun setLocalStyle(style: VideoScale) {
        if (mPublisher != null) {
            if (style == VideoScale.FILL) {
                mPublisher?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            } else {
                mPublisher?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FIT)
            }
        }
    }

    /**
     * Sets a custom video renderer for the remote
     *
     * @param renderer     The custom video renderer
     * @param remoteScreen Whether the renderer is applied to the remote received screen or not.
     */
    fun setRemoteVideoRenderer(renderer: BaseVideoRenderer?, remoteScreen: Boolean) {
        //todo: now, it will apply to all the subscribers
        if (remoteScreen) {
            mScreenRemoteRenderer = renderer
        } else {
            mVideoRemoteRenderer = renderer
        }
    }

    /**
     * (Tries) to set the FPS of the shared video stream to the passed one. The FPS is rounded to
     * the nearest supported one.
     *
     * @param framesPerSecond
     */
    fun setPublishingFPS(framesPerSecond: Int) {
        LOG.d(LOG_TAG, "setSharingFPS: ", mPublisher)
        val frameRate = getFPS(framesPerSecond)
        if (mPublisher != null) {
            val currentCamera = mPublisher?.cameraId

            if (mPreviewConfig != null) {
                mPreviewConfig?.frameRate = frameRate
            } else {
                mPreviewConfig = PreviewConfigBuilder().framerate(frameRate).build()
            }

            val newPreview: PreviewConfig? = mPreviewConfig

            val isPublishingCurrent = isPublishing
            val isPreviewingCurrent = isPreviewing

            if (isPublishingCurrent) {
                stopPublishingMedia(false)
            }

            if (isPreviewingCurrent) {
                stopPreview()
            }
            mPublisher = null

            if (isPreviewingCurrent) {
                startPreview(newPreview)
            }

            if (isPublishingCurrent) {
                startPublishingMedia(newPreview, false)
            }

            if (mPublisher != null && currentCamera != null) {
                mPublisher?.cameraId = currentCamera
            }
        }
    }

    /**
     * Sets an input signal processor. The input processor will process all the signals coming from
     * the wire. The SignalListeners will be invoked only on processed signals. That allows you to
     * easily implement and enforce a connection wide protocol for all sent and received signals.
     *
     * @param inputProtocol The input protocol you want to enforce. Pass null if you wish to receive
     * raw signals.
     */
    @Synchronized
    fun setInputSignalProtocol(inputProtocol: SignalProtocol<*, *>?) {
        mInputSignalProtocol = inputProtocol
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.setInputSignalProtocol(mInputSignalProtocol)
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
    @Synchronized
    fun setOutputSignalProtocol(outputProtocol: SignalProtocol<*, *>?) {
        mOutputSignalProtocol = outputProtocol
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.setOutputSignalProtocol(mOutputSignalProtocol)
        }
    }

    //Private methods
    private fun cleanup() {

        if (otAcceleratorSession != null) {
            otAcceleratorSession?.cleanUpSignals()
            if (mSubscribers.size > 0) {
                for (subscriber in mSubscribers.values) {
                    otAcceleratorSession?.unsubscribe(subscriber)
                }
            }
            if (mPublisher != null) {
                otAcceleratorSession?.unpublish(mPublisher)
            }
            otAcceleratorSession?.disconnect()
        }

        mPublisher = null
        otAcceleratorSession = null
        mSubscribers = HashMap()
        mStreams = ConcurrentHashMap()
        mSessionConnection = null
        isPreviewing = false
        isPublishing = false
        isSharingScreen = false
        isScreenSharingByDefault = false
        mScreenSharingFragment = null
    }

    private fun removeOTListener(
        listener: BaseOTListener?, retriableMap: HashMap<*, *>,
        listenerSet: HashSet<*>?
    ) {
        if (listener != null) {
            val internalListener: BaseOTListener =
                if (listener is RetriableOTListener<*>) (listener as RetriableOTListener<*>).internalListener else listener

            val realListener = retriableMap[internalListener] as RetriableOTListener<*>?

            listenerSet?.remove(realListener)
            retriableMap.remove(internalListener)
        } else {
            listenerSet?.clear()
            retriableMap.clear()
        }
    }

    @Synchronized
    private fun publishIfReady() {
        LOG.d(
            LOG_TAG, "publishIfReady: ", mSessionConnection, ", ", mPublisher, ", ",
            startPublishing, ", ", isPreviewing
        )
        if (otAcceleratorSession != null && mSessionConnection != null && mPublisher != null && startPublishing) {
            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
            if (!isPreviewing) {
                attachPublisherView()
            }
            if (!isPublishing) {
                otAcceleratorSession?.publish(mPublisher)
                // Do this as soon as possible to avoid race conditions...
                isPublishing = true
            }
        }
    }

    @Synchronized
    private fun publishIfScreenReady() {
        LOG.d(
            LOG_TAG, "publishIfScreenReady: ", mSessionConnection, ", ", mScreenPublisher, ", ",
            startSharingScreen
        )
        if (otAcceleratorSession != null && mSessionConnection != null && mScreenPublisher != null && startSharingScreen && !isScreenSharingByDefault) {
            if (!isPreviewing) {
                attachPublisherScreenView()
            }
            if (!isSharingScreen) {
                otAcceleratorSession?.publish(mScreenPublisher)
                // Do this as soon as possible to avoid race conditions...
                isSharingScreen = true
            }
        }
    }

    private fun createPublisher() {
        //TODO: add more cases
        LOG.d(LOG_TAG, "createPublisher: ", mPreviewConfig)
        val builder = Publisher.Builder(context)

        if (mPreviewConfig != null) {
            builder.name(mPreviewConfig?.name)

            if (mPreviewConfig?.resolution != Publisher.CameraCaptureResolution.MEDIUM ||
                mPreviewConfig?.frameRate != CameraCaptureFrameRate.FPS_15
            ) {
                LOG.d(
                    LOG_TAG, "createPublisher: Creating publisher with: ",
                    mPreviewConfig?.resolution, ", ", mPreviewConfig?.frameRate
                )

                builder.resolution(mPreviewConfig?.resolution)
                builder.frameRate(mPreviewConfig?.frameRate)
            } else {
                LOG.d(LOG_TAG, "createPublisher: Creating Publisher with audio and video specified")

                builder
                    .audioTrack(mPreviewConfig?.isAudioTrack ?: false)
                    .videoTrack(mPreviewConfig?.isVideoTrack ?: false)
            }
            if (mPreviewConfig?.capturer != null) {
                //custom video capturer
                builder.capturer(mPreviewConfig?.capturer)
            }
            if (mPreviewConfig?.renderer != null) {
                builder.renderer(mPreviewConfig?.renderer)
            }
        } else {
            LOG.d(LOG_TAG, "createPublisher: Creating DefaultPublisher")
        }

        mPublisher = builder.build().apply {
            setPublisherListener(mPublisherListener)
            setCameraListener(mCameraListener)
            //byDefault
            setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
        }
    }

    private fun createScreenPublisher(config: PreviewConfig?) {
        LOG.d(LOG_TAG, "createScreenPublisher: ", config)
        mScreenPublisherBuilder = Publisher.Builder(context)
        if (config != null) {
            mScreenPublisherBuilder?.name(config.name)

            if (config.resolution != Publisher.CameraCaptureResolution.MEDIUM ||
                config.frameRate != CameraCaptureFrameRate.FPS_15
            ) {
                LOG.d(
                    LOG_TAG, "createPublisher: Creating publisher with: ", config.resolution,
                    ", ", config.frameRate
                )
                mScreenPublisherBuilder?.resolution(config.resolution)?.frameRate(config.frameRate)
            } else {
                LOG.d(LOG_TAG, "createPublisher: Creating Publisher with audio and video specified")
                mScreenPublisherBuilder?.audioTrack(config.isAudioTrack)?.videoTrack(config.isVideoTrack)
            }

            if (config.capturer != null) {
                //custom video capturer
                mScreenPublisherBuilder?.capturer(config.capturer)
            } else {
                //create screenSharing by default
                isScreenSharingByDefault = true
                mScreenSharingFragment = ScreenSharingFragment.newInstance().also {
                    (context as FragmentActivity?)?.supportFragmentManager?.beginTransaction()
                        ?.add(it, "screenSharingFragment")
                        ?.commit()

                    it.setListener(screenListener)
                }
            }

            if (config.renderer != null) {
                mScreenPublisherBuilder?.renderer(config.renderer)
            }
        } else {
            LOG.d(LOG_TAG, "createPublisher: Creating DefaultPublisher")
        }

        mScreenPublisher = mScreenPublisherBuilder?.build()?.apply {
            if (!isScreenSharingByDefault) {
                setPublisherListener(mPublisherListener)
                setAudioLevelListener(mAudioLevelListener)
                setCameraListener(mCameraListener)
                publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen
                //byDefault
                setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            }
        }
    }

    /**
     * Rounds to the smallest FPS. Could round to the closest one instead!
     *
     * @param framesPerSecond
     */
    private fun getFPS(framesPerSecond: Int) = when {
        framesPerSecond < 7 -> CameraCaptureFrameRate.FPS_1
        framesPerSecond < 15 -> CameraCaptureFrameRate.FPS_7
        framesPerSecond < 30 -> CameraCaptureFrameRate.FPS_15
        else -> CameraCaptureFrameRate.FPS_30
    }

    private fun attachPublisherView() {
        val view = mPublisher?.view ?: return

        mBasicListeners.forEach {
            it.onPreviewViewReady(SELF, view)
        }
    }

    private fun attachPublisherScreenView() {
        mPublisher ?: return

        mBasicListeners.forEach {
            if (isScreenSharingByDefault) {
                it.onPreviewViewReady(SELF, mScreenSharingFragment?.screen)
            } else {
                it.onPreviewViewReady(SELF, mScreenPublisher?.view)
            }
        }
    }

    private fun detachPublisherView() {
        mPublisher ?: return

        mPublisher?.onStop()

        mBasicListeners.forEach {
            it.onPreviewViewDestroyed(SELF)
        }
    }

    private fun detachPublisherScreenView() {
        mPublisher ?: return

        mScreenPublisher?.onStop()

        mBasicListeners.forEach {
            it.onPreviewViewDestroyed(SELF)
        }
    }

    private fun refreshPeerList() {
        mBasicListeners
            .filter { it.internalListener != null }
            .forEach { listener ->

                listOf(mPublisher, mScreenPublisher)
                    .mapNotNull { it?.view }
                    .first()
                    .also { listener.onPreviewViewReady(SELF, it) }

                notifyRemoteViewReady(listener)
            }
    }

    private fun notifyRemoteViewReady(listener: RetriableBasicListener<OTWrapper>) {
        mSubscribers.values.forEach {
            val stream = it.stream

            listener.onRemoteViewReady(SELF, it.view, stream.streamId, stream.connection.data)
        }
    }

    private fun enableRemoteMedia(sub: Subscriber?, type: MediaType, enabled: Boolean) {
        if (sub != null) {
            if (type == MediaType.VIDEO) {
                sub.subscribeToVideo = enabled
            } else {
                sub.subscribeToAudio = enabled
            }
        }
    }

    //Analytics
    private fun initAnalytics() {
        //Init the analytics logging
        val source = context.packageName
        val prefs = context.getSharedPreferences("opentok", Context.MODE_PRIVATE)
        var guidVSol = prefs?.getString("guidVSol", null)
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString()
            prefs?.edit()?.putString("guidVSol", guidVSol)?.apply()
        }
        mAnalyticsData =
            OTKAnalyticsData.Builder(ClientLog.LOG_CLIENT_VERSION, source, ClientLog.LOG_COMPONENTID, guidVSol).build()
        mAnalytics = OTKAnalytics(mAnalyticsData)
        mAnalytics?.enableConsoleLog(false)
        mAnalyticsData?.sessionId = otConfig.sessionId
        mAnalyticsData?.partnerId = otConfig.apiKey
        mAnalytics?.data = mAnalyticsData
    }

    private fun addLogEvent(action: String, variation: String) {
        if (mAnalytics != null) {
            mAnalytics?.logEvent(action, variation)
        }
    }

    companion object {
        private val LOG_TAG = OTWrapper::class.java.simpleName
        private const val LOCAL_LOG_LEVEL: Short = 0xFF
        private val LOG = LogWrapper((GlobalLogLevel.sMaxLogLevel and LOCAL_LOG_LEVEL))

        fun setLogLevel(logLevel: Short) {
            LOG.setLogLevel(logLevel)
        }
    }
}