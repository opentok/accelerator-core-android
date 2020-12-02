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
    private var sessionConnection: Connection? = null
    private var publisher: Publisher? = null
    private var screenPublisher: Publisher? = null

    //indexed by streamId, *not* per subscriber Id
    private var subscribers = HashMap<String, Subscriber>()
    private var streams = ConcurrentHashMap<String, Stream>()

    //listeners
    private val basicListeners = HashSet<RetriableBasicListener<OTWrapper>>()
    private val advancedListeners = HashSet<RetriableAdvancedListener<OTWrapper>>()
    private val retriableBasicListeners = HashMap<BasicListener<*>, RetriableBasicListener<*>>()
    private val retriableAdvancedListeners = HashMap<AdvancedListener<*>, RetriableAdvancedListener<*>>()

    /**
     * Returns the number of active connections for the current session
     *
     * @return the number of active connections.
     */
    val connectionCount get() = otAcceleratorSession?.connectionCount ?: 0

    private var olderThanMe = 0

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
    private var previewConfig: PreviewConfig? = null

    //Screen Sharing by default
    private var screenSharingFragment: ScreenSharingFragment? = null
    private var isScreenSharingByDefault = false
    private var screenPublisherBuilder: Publisher.Builder? = null

    //Custom renderer
    private var videoRemoteRenderer: BaseVideoRenderer? = null
    private var screenRemoteRenderer: BaseVideoRenderer? = null

    //Signal protocol
    private var inputSignalProtocol: SignalProtocol<*, *>? = null
    private var outputSignalProtocol: SignalProtocol<*, *>? = null

    //Analytics for internal use
    private var analyticsData: OTKAnalyticsData? = null
    private var analytics: OTKAnalytics? = null

    private val connectionListener: ConnectionListener = object : ConnectionListener {
        override fun onConnectionCreated(session: Session, connection: Connection) {
            LOG.d(LOG_TAG, "onConnectionCreated: ", connection.data)

            // ToDo: This should added/removed inside otAcceleratorSession
            otAcceleratorSession?.addConnection(connection.connectionId, connection)

            if (connection.creationTime <= sessionConnection?.creationTime) {
                olderThanMe++
            }

            for (listener in basicListeners) {
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

            if (connection.creationTime <= sessionConnection?.creationTime) {
                olderThanMe--
            }

            basicListeners.forEach {
                it.onDisconnected(SELF, connectionCount, connection.connectionId, connection.data)
            }
        }
    }
    private val subscriberListener: SubscriberListener = object : SubscriberListener {
        override fun onConnected(sub: SubscriberKit) {
            LOG.i(LOG_TAG, "Subscriber is connected")
            addLogEvent(ClientLog.LOG_ACTION_ADD_REMOTE, ClientLog.LOG_VARIATION_SUCCESS)

            for (listener in basicListeners) {
                val stream = sub.stream
                listener.onRemoteViewReady(
                    SELF, sub.view, stream.streamId,
                    stream.connection.data
                )
            }
        }

        override fun onDisconnected(sub: SubscriberKit) {
            addLogEvent(ClientLog.LOG_ACTION_REMOVE_REMOTE, ClientLog.LOG_VARIATION_SUCCESS)

            basicListeners.forEach {
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
                    subscribers.remove(id)
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
                    subscribers.remove(id)
                }
                ErrorCode.SubscriberServerCannotFindStream -> {
                    LOG.e(LOG_TAG, "Subscriber error: SubscriberServerCannotFindStream")
                    subscribers.remove(id)
                }
                else -> {
                    LOG.e(LOG_TAG, "Subscriber error: default ")
                    subscribers.remove(id)

                    if (!streams.containsKey(id)) {
                        streams[id] = subscriberKit.stream
                    }

                    basicListeners.forEach {
                        it.onError(SELF, opentokError)
                    }
                }
            }
        }
    }
    private val audioLevelListener = PublisherKit.AudioLevelListener { publisherKit, audioLevel ->
        advancedListeners.forEach {
            it.onAudioLevelUpdated(audioLevel)
        }
    }

    //Implements Advanced listeners
    private val reconnectionListener: ReconnectionListener = object : ReconnectionListener {
        override fun onReconnecting(session: Session) {
            advancedListeners.forEach {
                it.onReconnecting(SELF)
            }
        }

        override fun onReconnected(session: Session) {
            advancedListeners.forEach {
                it.onReconnected(SELF)
            }
        }
    }

    private val videoListener: VideoListener = object : VideoListener {
        override fun onVideoDataReceived(subscriberKit: SubscriberKit) {
            //todo: review: a new listener to indicate the first frame received
        }

        override fun onVideoDisabled(subscriber: SubscriberKit, reason: String) {
            basicListeners.forEach {
                it.onRemoteVideoChanged(
                    SELF, subscriber.stream.streamId, reason, false,
                    subscriber.subscribeToVideo
                )
            }
        }

        override fun onVideoEnabled(subscriber: SubscriberKit, reason: String) {
            basicListeners.forEach {
                it.onRemoteVideoChanged(
                    SELF, subscriber.stream.streamId, reason, true,
                    subscriber.subscribeToVideo
                )
            }
        }

        override fun onVideoDisableWarning(subscriber: SubscriberKit) {
            advancedListeners.forEach {
                it.onVideoQualityWarning(SELF, subscriber.stream.streamId)
            }
        }

        override fun onVideoDisableWarningLifted(subscriber: SubscriberKit) {
            advancedListeners.forEach {
                it.onVideoQualityWarningLifted(
                    SELF,
                    subscriber.stream.streamId
                )
            }
        }
    }

    private val cameraListener: CameraListener = object : CameraListener {
        override fun onCameraChanged(publisher: Publisher, i: Int) {
            advancedListeners.forEach {
                it.onCameraChanged(SELF)
            }
        }

        override fun onCameraError(publisher: Publisher, opentokError: OpentokError) {
            LOG.d(LOG_TAG, "onCameraError: onError ", opentokError.message)

            advancedListeners.forEach {
                it.onError(SELF, opentokError)
            }
        }
    }

    private val streamListener: StreamListener = object : StreamListener {
        override fun onReconnected(subscriber: SubscriberKit) {
            advancedListeners.forEach {
                it.onReconnected(SELF, subscriber.stream.streamId)
            }
        }

        override fun onDisconnected(subscriber: SubscriberKit) {
            advancedListeners.forEach {
                it.onDisconnected(SELF, subscriber.stream.streamId)
            }
        }

        override fun onAudioEnabled(subscriber: SubscriberKit) {
            advancedListeners.forEach {
                it.onAudioEnabled(SELF, subscriber.stream.streamId)
            }
        }

        override fun onAudioDisabled(subscriber: SubscriberKit) {
            advancedListeners.forEach {
                it.onAudioDisabled(SELF, subscriber.stream.streamId)
            }
        }
    }

    //Implements Basic listeners: Session.SessionListener, Session.ConnectionListener,
    // Session.SignalListener, Publisher.PublisherListener
    private val sessionListener: SessionListener = object : SessionListener {

        override fun onConnected(session: Session) {
            val connection = session.connection
            sessionConnection = connection
            otAcceleratorSession?.addConnection(connection.connectionId, connection)

            //update internal client logs with connectionId
            analyticsData?.connectionId = connection.connectionId
            analytics?.data = analyticsData

            LOG.d(LOG_TAG, "onConnected: ", connection.data, ". listeners: ", basicListeners)
            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_SUCCESS)

            publishIfReady()
            basicListeners.forEach {
                it.onConnected(SELF, connectionCount, connection.connectionId, connection.data)
            }
        }

        override fun onDisconnected(session: Session) {
            addLogEvent(ClientLog.LOG_ACTION_DISCONNECT, ClientLog.LOG_VARIATION_SUCCESS)
            if (otAcceleratorSession == null || sessionConnection == null) {
                // This can happen if somehow onError was called before onDisconnected or if we somehow
                // call onDisconnected twice (so cleanup has been done already)

                LOG.w(LOG_TAG, "OnDisconnected called on a stale object")
                sessionConnection = session.connection
            }
            if (sessionConnection != null) {
                basicListeners.forEach {
                    it.onDisconnected(
                        SELF, 0,
                        sessionConnection?.connectionId,
                        sessionConnection?.data
                    )
                }
            }
            cleanup()
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            LOG.d(LOG_TAG, "OnStreamReceived: ", stream.connection.data)

            streams[stream.streamId] = stream

            if (otConfig.subscribeAutomatically) {
                addRemote(stream)
            }

            basicListeners.forEach {
                it.onRemoteJoined(SELF, stream.streamId)
            }
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            LOG.d(LOG_TAG, "OnStreamDropped: ", stream.connection.data)
            val subId = stream.streamId

            if (streams.containsKey(subId)) {
                streams.remove(stream.streamId)
            }

            if (subscribers.containsKey(subId)) {
                subscribers.remove(stream.streamId)
            }

            basicListeners.forEach {
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

            basicListeners.forEach {
                it.onError(SELF, opentokError)
            }
        }
    }

    private val publisherListener: PublisherListener = object : PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            var screenSharing = false
            if (stream.streamVideoType == StreamVideoType.StreamVideoTypeScreen) {
                addLogEvent(ClientLog.LOG_ACTION_START_SCREEN_COMM, ClientLog.LOG_VARIATION_SUCCESS)
                screenSharing = true
            } else {
                addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_SUCCESS)
                isPublishing = true
            }

            basicListeners.forEach {
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
            basicListeners.forEach {
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
                    publisher = null
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
                    publisher = null
                }
                else -> {
                    LOG.e(LOG_TAG, "Publisher error: default")
                    publisher = null

                    basicListeners.forEach {
                        it.onError(SELF, opentokError)
                    }
                }
            }
        }
    }
    private var screenListener: ScreenSharingListener = object : ScreenSharingListener {
        override fun onScreenCapturerReady() {
            val capturer = screenSharingFragment?.screenCapturer

            if (capturer != null) {

                screenPublisherBuilder?.capturer(capturer)

                screenPublisher = screenPublisherBuilder?.build()?.apply {
                    setPublisherListener(publisherListener)
                    setAudioLevelListener(audioLevelListener)
                    setCameraListener(cameraListener)
                    publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen
                    setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
                }

                attachPublisherScreenView()
                otAcceleratorSession?.publish(screenPublisher)
            }
        }

        override fun onError(errorMsg: String) {
            LOG.i(LOG_TAG, "Error in Screen Sharing by default")

            basicListeners.forEach {
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
            basicListeners.forEach {
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
        otAcceleratorSession?.setConnectionListener(connectionListener)
        otAcceleratorSession?.setSessionListener(sessionListener)
        otAcceleratorSession?.signalListener = otAcceleratorSession?.signalListener
        otAcceleratorSession?.setReconnectionListener(reconnectionListener)
        olderThanMe = 0

        //check signal protocol
        if (inputSignalProtocol != null) {
            otAcceleratorSession?.setInputSignalProtocol(inputSignalProtocol)
        }
        if (outputSignalProtocol != null) {
            otAcceleratorSession?.setOutputSignalProtocol(outputSignalProtocol)
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
        get() = if (sessionConnection != null) sessionConnection?.connectionId else null

    /**
     * Checks if the own connection is the oldest in the current session
     *
     * @return Whether the local connection is oldest (`true`) or not (
     * `false`).
     */
    val isTheOldestConnection: Boolean
        get() = olderThanMe <= 0

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
        val remoteSub = subscribers[remoteId]

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
        previewConfig = config
        if (publisher == null && !isPreviewing) {
            createPublisher()
            attachPublisherView()
            isPreviewing = true
        }
    }

    /**
     * Call to stop the camera's video in the Preview's view.
     */
    fun stopPreview() {
        if (publisher != null && isPreviewing) {
            detachPublisherView()
            publisher = null
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
            previewConfig = config
            startPublishing = true
            if (publisher == null) {
                createPublisher()
            }
            publishIfReady()
        } else {
            startSharingScreen = true
            if (screenPublisher == null) {
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
            if (publisher != null && startPublishing) {
                otAcceleratorSession?.unpublish(publisher)
            }
            isPublishing = false
            startPublishing = false
            if (!isPreviewing) {
                detachPublisherView()
                publisher = null
            }
        } else {
            addLogEvent(ClientLog.LOG_ACTION_END_SCREEN_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
            if (screenSharingFragment != null) {
                screenSharingFragment?.stopScreenCapture()
                isScreenSharingByDefault = false
            }
            detachPublisherScreenView()
            if (screenPublisher != null && startSharingScreen) {
                otAcceleratorSession?.unpublish(screenPublisher)
            }
            isSharingScreen = false
            startSharingScreen = false
            screenPublisher = null
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
        if (publisher == null) return false

        val publishVideo = publisher?.publishVideo ?: false
        val publishAudio = publisher?.publishAudio ?: false

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
        if (publisher != null) {
            when (type) {
                MediaType.AUDIO -> publisher?.publishAudio = enabled

                MediaType.VIDEO -> {
                    publisher?.publishVideo = enabled

                    if (enabled) {
                        publisher?.view?.visibility = View.VISIBLE
                    } else {
                        publisher?.view?.visibility = View.GONE
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
            enableRemoteMedia(subscribers[remoteId], type, enabled)
        } else {
            val subscribers: Collection<Subscriber> = subscribers.values

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
        val sub = subscribers[remoteId] ?: return false

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

        val stream = streams[remoteId]
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
            setVideoListener(videoListener)
            setStreamListener(streamListener)
            setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
        }

        val subId = stream.streamId
        subscribers[subId] = sub
        sub.setSubscriberListener(subscriberListener)

        if (stream.streamVideoType == StreamVideoType.StreamVideoTypeCamera && videoRemoteRenderer != null) {
            sub.renderer = videoRemoteRenderer
        } else {
            if (stream.streamVideoType == StreamVideoType.StreamVideoTypeScreen && screenRemoteRenderer != null) {
                sub.renderer = screenRemoteRenderer
            }
        }
        //remove the sub's stream from the streams list to avoid subscribe twice to the same stream
        streams.remove(sub.stream.streamId)
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
        val subscriber = subscribers[remoteId]

        requireNotNull(subscriber) { "No subscriber found for remoteId $remoteId" }

        subscribers.remove(remoteId)
        streams[remoteId] = subscriber.stream
        otAcceleratorSession?.unsubscribe(subscriber)
    }

    /**
     * Call to cycle between cameras, if there are multiple cameras on the device.
     */
    fun cycleCamera() {
        if (publisher != null) {
            publisher?.cycleCamera()
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
        var realListener = retriableBasicListeners[listener]

        if (realListener == null) {
            realListener = if (isWrapped) {
                listener as RetriableBasicListener<OTWrapper>
            } else {
                getUnfailingFromBaseListener(listener) as RetriableBasicListener<OTWrapper>
            }

            retriableBasicListeners[listener] = if (isWrapped) listener as RetriableBasicListener<*> else realListener
            basicListeners.add(realListener)
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
        removeOTListener(listener, retriableBasicListeners, basicListeners)
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
        var realListener = retriableAdvancedListeners[listener]

        if (realListener == null) {
            realListener = if (isWrapped) {
                listener as RetriableAdvancedListener<OTWrapper>
            } else {
                getUnfailingFromBaseListener(listener) as RetriableAdvancedListener<OTWrapper>
            }

            retriableAdvancedListeners[listener] =
                if (isWrapped) listener as RetriableAdvancedListener<*> else realListener
            advancedListeners.add(realListener)
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
        removeOTListener(listener, retriableAdvancedListeners, advancedListeners)
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
            if (publisher != null) {
                val stream = publisher?.stream

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

                val publishAudio = publisher?.publishAudio ?: false
                val publishVideo = publisher?.publishVideo ?: false

                return StreamStatus(
                    publisher?.view,
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
        val sub = subscribers[id]
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
        val sub = subscribers[remoteId]
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
        if (publisher != null) {
            if (style == VideoScale.FILL) {
                publisher?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
            } else {
                publisher?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FIT)
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
            screenRemoteRenderer = renderer
        } else {
            videoRemoteRenderer = renderer
        }
    }

    /**
     * (Tries) to set the FPS of the shared video stream to the passed one. The FPS is rounded to
     * the nearest supported one.
     *
     * @param framesPerSecond
     */
    fun setPublishingFPS(framesPerSecond: Int) {
        LOG.d(LOG_TAG, "setSharingFPS: ", publisher)
        val frameRate = getFPS(framesPerSecond)
        if (publisher != null) {
            val currentCamera = publisher?.cameraId

            if (previewConfig != null) {
                previewConfig?.frameRate = frameRate
            } else {
                previewConfig = PreviewConfigBuilder().framerate(frameRate).build()
            }

            val newPreview: PreviewConfig? = previewConfig

            val isPublishingCurrent = isPublishing
            val isPreviewingCurrent = isPreviewing

            if (isPublishingCurrent) {
                stopPublishingMedia(false)
            }

            if (isPreviewingCurrent) {
                stopPreview()
            }
            publisher = null

            if (isPreviewingCurrent) {
                startPreview(newPreview)
            }

            if (isPublishingCurrent) {
                startPublishingMedia(newPreview, false)
            }

            if (publisher != null && currentCamera != null) {
                publisher?.cameraId = currentCamera
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
        inputSignalProtocol = inputProtocol
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.setInputSignalProtocol(inputSignalProtocol)
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
        outputSignalProtocol = outputProtocol
        if (otAcceleratorSession != null) {
            otAcceleratorSession?.setOutputSignalProtocol(outputSignalProtocol)
        }
    }

    //Private methods
    private fun cleanup() {

        if (otAcceleratorSession != null) {
            otAcceleratorSession?.cleanUpSignals()
            if (subscribers.size > 0) {
                for (subscriber in subscribers.values) {
                    otAcceleratorSession?.unsubscribe(subscriber)
                }
            }
            if (publisher != null) {
                otAcceleratorSession?.unpublish(publisher)
            }
            otAcceleratorSession?.disconnect()
        }

        publisher = null
        otAcceleratorSession = null
        subscribers = HashMap()
        streams = ConcurrentHashMap()
        sessionConnection = null
        isPreviewing = false
        isPublishing = false
        isSharingScreen = false
        isScreenSharingByDefault = false
        screenSharingFragment = null
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
            LOG_TAG, "publishIfReady: ", sessionConnection, ", ", publisher, ", ",
            startPublishing, ", ", isPreviewing
        )
        if (otAcceleratorSession != null && sessionConnection != null && publisher != null && startPublishing) {
            addLogEvent(ClientLog.LOG_ACTION_START_COMM, ClientLog.LOG_VARIATION_ATTEMPT)
            if (!isPreviewing) {
                attachPublisherView()
            }
            if (!isPublishing) {
                otAcceleratorSession?.publish(publisher)
                // Do this as soon as possible to avoid race conditions...
                isPublishing = true
            }
        }
    }

    @Synchronized
    private fun publishIfScreenReady() {
        LOG.d(
            LOG_TAG, "publishIfScreenReady: ", sessionConnection, ", ", screenPublisher, ", ",
            startSharingScreen
        )
        if (otAcceleratorSession != null && sessionConnection != null && screenPublisher != null && startSharingScreen && !isScreenSharingByDefault) {
            if (!isPreviewing) {
                attachPublisherScreenView()
            }
            if (!isSharingScreen) {
                otAcceleratorSession?.publish(screenPublisher)
                // Do this as soon as possible to avoid race conditions...
                isSharingScreen = true
            }
        }
    }

    private fun createPublisher() {
        //TODO: add more cases
        LOG.d(LOG_TAG, "createPublisher: ", previewConfig)
        val builder = Publisher.Builder(context)

        if (previewConfig != null) {
            builder.name(previewConfig?.name)

            if (previewConfig?.resolution != Publisher.CameraCaptureResolution.MEDIUM ||
                previewConfig?.frameRate != CameraCaptureFrameRate.FPS_15
            ) {
                LOG.d(
                    LOG_TAG, "createPublisher: Creating publisher with: ",
                    previewConfig?.resolution, ", ", previewConfig?.frameRate
                )

                builder.resolution(previewConfig?.resolution)
                builder.frameRate(previewConfig?.frameRate)
            } else {
                LOG.d(LOG_TAG, "createPublisher: Creating Publisher with audio and video specified")

                builder
                    .audioTrack(previewConfig?.isAudioTrack ?: false)
                    .videoTrack(previewConfig?.isVideoTrack ?: false)
            }
            if (previewConfig?.capturer != null) {
                //custom video capturer
                builder.capturer(previewConfig?.capturer)
            }
            if (previewConfig?.renderer != null) {
                builder.renderer(previewConfig?.renderer)
            }
        } else {
            LOG.d(LOG_TAG, "createPublisher: Creating DefaultPublisher")
        }

        publisher = builder.build().apply {
            setPublisherListener(publisherListener)
            setCameraListener(cameraListener)
            //byDefault
            setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)
        }
    }

    private fun createScreenPublisher(config: PreviewConfig?) {
        LOG.d(LOG_TAG, "createScreenPublisher: ", config)
        screenPublisherBuilder = Publisher.Builder(context)
        if (config != null) {
            screenPublisherBuilder?.name(config.name)

            if (config.resolution != Publisher.CameraCaptureResolution.MEDIUM ||
                config.frameRate != CameraCaptureFrameRate.FPS_15
            ) {
                LOG.d(
                    LOG_TAG, "createPublisher: Creating publisher with: ", config.resolution,
                    ", ", config.frameRate
                )
                screenPublisherBuilder?.resolution(config.resolution)?.frameRate(config.frameRate)
            } else {
                LOG.d(LOG_TAG, "createPublisher: Creating Publisher with audio and video specified")
                screenPublisherBuilder?.audioTrack(config.isAudioTrack)?.videoTrack(config.isVideoTrack)
            }

            if (config.capturer != null) {
                //custom video capturer
                screenPublisherBuilder?.capturer(config.capturer)
            } else {
                //create screenSharing by default
                isScreenSharingByDefault = true
                screenSharingFragment = ScreenSharingFragment.newInstance().also {
                    (context as FragmentActivity?)?.supportFragmentManager?.beginTransaction()
                        ?.add(it, "screenSharingFragment")
                        ?.commit()

                    it.setListener(screenListener)
                }
            }

            if (config.renderer != null) {
                screenPublisherBuilder?.renderer(config.renderer)
            }
        } else {
            LOG.d(LOG_TAG, "createPublisher: Creating DefaultPublisher")
        }

        screenPublisher = screenPublisherBuilder?.build()?.apply {
            if (!isScreenSharingByDefault) {
                setPublisherListener(publisherListener)
                setAudioLevelListener(audioLevelListener)
                setCameraListener(cameraListener)
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
        val view = publisher?.view ?: return

        basicListeners.forEach {
            it.onPreviewViewReady(SELF, view)
        }
    }

    private fun attachPublisherScreenView() {
        publisher ?: return

        basicListeners.forEach {
            if (isScreenSharingByDefault) {
                it.onPreviewViewReady(SELF, screenSharingFragment?.screen)
            } else {
                it.onPreviewViewReady(SELF, screenPublisher?.view)
            }
        }
    }

    private fun detachPublisherView() {
        publisher ?: return

        publisher?.onStop()

        basicListeners.forEach {
            it.onPreviewViewDestroyed(SELF)
        }
    }

    private fun detachPublisherScreenView() {
        publisher ?: return

        screenPublisher?.onStop()

        basicListeners.forEach {
            it.onPreviewViewDestroyed(SELF)
        }
    }

    private fun refreshPeerList() {
        basicListeners
            .filter { it.internalListener != null }
            .forEach { listener ->

                listOf(publisher, screenPublisher)
                    .mapNotNull { it?.view }
                    .first()
                    .also { listener.onPreviewViewReady(SELF, it) }

                notifyRemoteViewReady(listener)
            }
    }

    private fun notifyRemoteViewReady(listener: RetriableBasicListener<OTWrapper>) {
        subscribers.values.forEach {
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

    private fun initAnalytics() {
        val source = context.packageName
        val prefs = context.getSharedPreferences("opentok", Context.MODE_PRIVATE)
        var guidVSol = prefs?.getString("guidVSol", null)
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString()
            prefs?.edit()?.putString("guidVSol", guidVSol)?.apply()
        }
        analyticsData = OTKAnalyticsData.Builder(
            ClientLog.LOG_CLIENT_VERSION,
            source,
            ClientLog.LOG_COMPONENTID,
            guidVSol
        ).build()

        analytics = OTKAnalytics(analyticsData)
        analytics?.enableConsoleLog(false)
        analyticsData?.sessionId = otConfig.sessionId
        analyticsData?.partnerId = otConfig.apiKey
        analytics?.data = analyticsData
    }

    private fun addLogEvent(action: String, variation: String) {
        if (analytics != null) {
            analytics?.logEvent(action, variation)
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