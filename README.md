#Accelerator Core Android

##Quickstart
The library `accelerator core android` gives you an easy way to integrate [OpenTok SDK](https://tokbox.com) to any Android applications. With `accelerator core android` you can 

##Installation
There are 2 options for installing the library:

  - [Using the repository](#using-the-repository)
  - [Using Maven](#using-maven)


### Using the repository

1. Clone the [accelerator core android repository](https://github.com/opentok/accelerator-core-android).
2. Start Android Studio and create a project.
3. Navigate to the directory in which you cloned **OpenTok Accelerator Core Android**, select **accelerator-core**, and click **Finish**.
4. Open the **build.gradle** file for the app and ensure the following lines have been added to the `dependencies` section:

```
compile project(':accelerator-core-android')

```
### Using Maven

1. In the `build.gradle` file for your solution, add the following code to the section labeled `repositories`:

  ```
  maven { url  "http://tokbox.bintray.com/maven" }
  ```

2. Add the following code snippet to the section labeled 'dependencies’:

  ```
  compile 'com.opentok.android:opentok-accelerator-core:2.0.0’
  ```

##Exploring the Code

For detail about the APIs used to develop this library, see the [OpenTok Android SDK Reference](https://tokbox.com/developer/sdks/android/reference/) and [Android API Reference](http://developer.android.com/reference/packages.html).


### Main Class Design

| Class        | Description  |
| ------------- | ------------- |
| `AccPackSession` | Manages the session, allows multiple listeners and implements a signaling protocol to facilitate the signal communication by implemting filtering by type. | 
 | `OTWrapper` | Represents an OpenTok object to enable a video communication. |
| `OTConfig`   | Defines the OpenTok Configuration to be used in the communication. It includes SessionId, Token and APIKey, and features like to subscribe automatically or subscribe to self. |
| `PreviewConfig` | Defines the configuration of the local preview. |
| `BasicListener` | Monitors basic state changes in the OpenTok communication. |
| `AdvancedListener` | Monitors advanced state changes in the OpenTok communication.|
| `SignalListener` | Monitors a new signal received in the OpenTok communication. |
| `MediaType` | Defines the Audio and Video media type. |
| `StreamStatus` | Defines the current status of the Stream properties. |
| `VideoScale` | Defines the FIT and FILL modes setting for the renderer. |
| `ScreenSharingCapturer` | Publishes a screen sharing capturer. In the absence of a selected or default capturer, `Core Android` will generate one using the full screen as the capturer. |
| `ScreenSharingFragment` | Headless fragment used to implement the screensharing feature by default. |

##Using Accelerator Core Android

You can start using one of the example applications already develop for testing:
 
 - [One to one sample application](https://github.com/opentok/one-to-one-sample-apps).
 
###Initialization

The first step in using the OTWrapper is to initialize it by calling the constructor with the OpenTokConfig parameter `OTConfig`.

```javascript
  public OTWrapper(Context context, OTConfig config) {  
    this.mContext = context;  
    this.mOTConfig = config;  
    mSubscribers = new HashMap<String, Subscriber>();  
    mConnections = new Hashtable<String, Connection>();  
    mStreams = new Hashtable<String, Stream>();  
    mBasicListeners = new   ArrayList<RetriableBasicListener<OTWrapper>>();  
    mAdvancedListeners = new   ArrayList<RetriableAdvancedListener<OTWrapper>>();  
    initAnalytics();  
  }
```
`OTConfig` **requires**  the following credentials to connect to OpenTok. See [Obtaining OpenTok Credentials](#obtaining-openok-credentials).

  - sessionId
  - token
  - apiKey
  
Once with this credentials you can use `OTConfigBuilder` to create `OTConfig` 

```javascript
    public OTConfig(OTConfigBuilder builder) {
        this.sessionId = builder.sessionId;
        this.token = builder.token;
        this.apiKey = builder.apiKey;
        this.subscribeAutomatically = builder.subscribeAutomatically;
        this.subscribeToSelf = builder.subscribeToSelf;
    }
``` 


####Obtaining OpenTok Credentials

To use OpenTok's framework you need a Session ID, Token, and API Key you can get these values at the [OpenTok Developer Dashboard](https://dashboard.tokbox.com/) . For production deployment, you must generate the Session ID and Token values using one of the [OpenTok Server SDKs](https://tokbox.com/developer/sdks/server/).
 
### Using OTWrapper

#### Pause/ Resume the Video

Call this method when the app's activity pauses.. This pauses the video for the local preview and remotes.

```java
  public void pause() {
    addLogEvent(ClientLog.LOG_ACTION_PAUSE, ClientLog.LOG_VARIATION_ATTEMPT);
    if (mSession != null) {
      mSession.onPause();
    }
    addLogEvent(ClientLog.LOG_ACTION_PAUSE, ClientLog.LOG_VARIATION_SUCCESS);
  }
```
 
 To resume the app's activity call this method. This resumes the video for the local preview and remotes.
 
```java
   public void resume(boolean resumeEvents) {
    addLogEvent(ClientLog.LOG_ACTION_RESUME, ClientLog.LOG_VARIATION_ATTEMPT);
    if (mSession != null) {
      mSession.onResume();
    }
    if (resumeEvents && mBasicListeners != null && !mBasicListeners.isEmpty()) {
      for (BasicListener listener : mBasicListeners
              ) {
        ((RetriableBasicListener) listener).resume();
      }

    }
    addLogEvent(ClientLog.LOG_ACTION_RESUME, ClientLog.LOG_VARIATION_SUCCESS);
  }
```
.

 
#### Connect and disconnect from an OpenTok session

Call to connect or disconnect from an OpenTok session. When the OTWrapper is connected, the BasicListener.onConnected(...) event is called.
If the OTWrapper failed to connect, the BasicListener.onError(...) event is called.

```java
	
	mWrapper.connect();
    
    //.....

    mWrapper.disconnect(); 

```

Each time a new participant connects to the same session, the BasicListener.onConnected(...) event is called.
This event offers the information about the new connection id of the participant who connected, the total connections count in the session and the data of the connection.
To check if the new connection is our own connection or not, use OTWrapper.getOwnConnId().

```java
	
	private boolean isConnected = false;
	private String mRemoteConnId;

	private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

                @Override
                public void onConnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Connected to the session. Number of participants: " + participantsCount);
                    
                    if (mWrapper.getOwnConnId() == connId) {
                        isConnected = true;
                    }
                    else {
                        mRemoteConnId = connId;
                    }
                }
              //....
    });
```

#### Start and stop preview

Call to start and stop displaying the camera's video in the Preview's view before it starts streaming video. Therefore, the other participants are not going to receive this video stream.

```java
	 
	mWrapper.startPreview(new PreviewConfig.PreviewConfigBuilder().
                        name("Tokboxer").build());

    //.....
    
    mWrapper.stopPreview();

              
```

When the OTWrapper started the preview, the BasicListener.onPreviewViewReady(...) event is called. And when the OTWrapper stopped the preview, the BasicListener.onPreviewViewDestroyed(...) event is called.

```java
	private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

 			@Override
                public void onPreviewViewReady(OTWrapper otWrapper, View localView) throws ListenerException {
                    Log.i(LOG_TAG, "Local preview view is ready");
                    //....
                }

                @Override
                public void onPreviewViewDestroyed(OTWrapper otWrapper, View localView) throws ListenerException {
                    Log.i(LOG_TAG, "Local preview view is destroyed");
                   //....
                }
    });
```

#### Start and stop publishing media

Call to start and stop the local streaming video. The source of the stream can be the camera or the screen. To indicate the screen source, it is necessary to set the screensharing parameter to TRUE.

```java
	
	PreviewConfig config;

	//camera streaming
	config = new PreviewConfig.PreviewConfigBuilder().
                        name("Tokboxer").build();
	mWrapper.startPublishingMedia(config, false);

    //or screen streaming, using a custom screen capturer
    config = new PreviewConfig.PreviewConfigBuilder().
                    name("screenPublisher").capturer(screenCapturer).build();
    mWrapper.startSharingMedia(config, true);

```

When the OTWrapper started the publishing media, the BasicListener.onStartedPublishingMedia(...) event is called. And when the OTWrapper stopped the publishing media, the BasicListener.onStoppedPublishingMedia(...) event is called.

```java
	private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

 			@Override
                public void onStartedSharingMedia(OTWrapper otWrapper, boolean screensharing) throws ListenerException {
                    Log.i(LOG_TAG, "Local started streaming video.");
                    //....
                }

                @Override
                public void onStoppedSharingMedia(OTWrapper otWrapper, boolean isScreensharing) throws ListenerException {
                    Log.i(LOG_TAG, "Local stopped streaming video.");
                }
    });
```

### Remote participants management
To subscribe automatically to a new participant connected to the session, the `subscribeAutomatically` property in the OTConfig has to be TRUE.
Then, when a new remote participant connected to the session, the BasicListener.onRemoteJoined(...) event is called. And the BasicListener.onRemoteLeft(...) event is called. These callbacks contain the identifier for the remote participant, which is equals to the stream id of them.
```java
	private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

		@Override
		public void onRemoteJoined(OTWrapper otWrapper, String remoteId) throws ListenerException {
        	Log.i(LOGTAG, "A new remote joined.");
        	//...
        }

        @Override
        public void onRemoteLeft(OTWrapper otWrapper, String remoteId) throws ListenerException {
        	Log.i(LOGTAG, "A new remote left.");
        	//...    
        }
    });
```

When the remote participant view is ready, the BasicListener.onRemoteViewReady(...) event is called. And when the remote participant view is destroyed, the BasicListener.onRemoteViewDestroyed(....) event is called.
```java
	private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

		@Override
        public void onRemoteViewReady(OTWrapper otWrapper, View remoteView, String remoteId, String data) throws ListenerException {
            Log.i(LOGTAG, "Remove view is ready");
            //...
  		}

        @Override
        public void onRemoteViewDestroyed(OTWrapper otWrapper, View remoteView, String remoteId) throws ListenerException {
        	Log.i(LOGTAG, "Remote view is destroyed");
        	//...
        }
    });
```

#### Connections management

The SDK Wrapper offers a set of methods to manage the connections of the session.

```java
	//get our own connection Id
	String myConnectionId = mWrapper.getOwnConnId();

	//get the total connections in the session
	String totalParticipants = mWrapper.getConnectionsCount();

	//check f the own connection is the oldest in the current session
	Boolen isTheOldest = mWrapper.isTheOldestConnection();

	//compare the connections creation times between the local connection and the argument passing
	int older = mWrapper.compareConnectionsTimes(remoteConnId);

```

#### Enable and disable the publishing and receiving media
To enable or disable the publishing audio or video.

```java
	//check the current status of the publishing video
	boolean videoEnabled = mWrapper.isPublishingMediaEnabled(MediaType.Video);

	//check the current status of the publishing audio
	boolean audioEnabled = mWrapper.isPublishingMediaEnabled(MediaType.Audio);

	//enable the video
	mWrapper.enablePublishingMedia(MediaType.Video, true);
	
	//disable the audio
	mWrapper.enablePublishingMedia(MediaType.Audio, false);
	
```

#### Pause and resume communication

Call these methods when the app's activity pauses or resumes. These pause or resume the video for the local preview and remotes. 
The SDK Wrapper offers the posibility to resume the events too setting the `resumeEvents` parameter to TRUE in the `resume` method.

```java
	mWrapper.pause();

	//.....

	mWrapper.resume(true);	
```

#### Get stream status
The status of a stream includes the media status, the stream type, the status of the media containers and the stream dimensions.

```java
	//to get the publishing stream status
	StreamStatus localStreamStatus = mWrapper.getPublishingStreamStatus();

	//to get the remote stream status
	StreamStatus remoteStreamStatus = mWrapper.getRemoteStremStatus(remoteId);

```

#### Signals Management
The SDK Wrapper includes a complete Signaling protocol to register a signal listener for a given signal.

```java
	
	mWrapper.addSignalListener(SIGNAL_TYPE, this);

	//send a signal to all the participants
	mWrapper.sendSignal(new SignalInfo(mWrapper.getOwnConnId(), null, SIGNAL_TYPE, "hello"));

	//send a signal to a specific participant, using the participant connection id.
	mWrapper.sendSignal(new SignalInfo(mWrapper.getOwnConnId(), participantConnId, SIGNAL_TYPE, "hello"));

	//manage the received signals. All the received signals will be of the registered type: SIGNAL_TYPE
	public void onSignalReceived(SignalInfo signalInfo, boolean isSelfSignal) {
   		//....
    }

```

#### Customize capturer and renderers
A custom video capturer or renderer can be used in the OpenTok communication for the publishing media.

```java

   CustomRenderer myCustomRenderer = new CustomRenderer(...);
   CustomCapturer myCustomCapturer = new CustomCapturer(...);

   PreviewConfig config = new PreviewConfig.PreviewConfigBuilder().
                    name("screenPublisher").capturer(myCustomCapturer).renderer(myCustomRenderer).build();

   mWrapper.startPublishingMedia(config, false);

```

A custom video renderer can be used in the OpenTok communication for the received media. Please note, this should be set before to start the communication.

```java
	
   CustomRenderer myCustomRenderer = new CustomRenderer(...);
   //set a custom renderer dor the received video stream
   mWrapper.setRemoteVideoRenderer(myCustomRenderer);

   //or set a custom renderer for the received screen stream 
   mWrapper.setRemoteScreenRenderer(myCustomRenderer);

```

#### Set Video Renderer styles
The video scale mode can be modified to FILL or FIT value for the publishing video or for the received video from the remotes.

```java
	mWrapper.setPublishingStyle(VideoScalse.FIT);
	mWrapper.setRemoteStyle(remoteId, VideoScale.FILL);
```

#### Cycle the camera
Cycle between cameras, if there are multiple cameras on the device. Then, the AdvancedListener.onCameraChanged(...) event is called. 

```java
	mWrapper.cycleCamera();
```

#### Advanced events
The SDK Wrapper include an AdvancedListener to define some events like when the video changed by quality reasons, or when the communication tries to reconnect,...etc.

```java
	private AdvancedListener mAdvancedListener =
            new PausableAdvancedListener(new AdvancedListener<OTWrapper>() {

                @Override
                public void onReconnecting(OTWrapper otWrapper) throws ListenerException {
                    Log.i(LOG_TAG, "The session is reconnecting.");
                }

                @Override
                public void onReconnected(OTWrapper otWrapper) throws ListenerException {
                    Log.i(LOG_TAG, "The session reconnected.");
                }

                @Override
                public void onVideoQualityWarning(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "The quality has degraded");
                }

                @Override
                public void onVideoQualityWarningLifted(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "The quality has improved");
                }

               	//...
            });
```

###Using OTAcceleratorSession

`OTAcceleratorSession` lets you manage signalling in an OpenTok Session. Start by creating an OTAcceleratorSession instance, an apiKey and sessionID are requires. For more visit [Obtaining OpenTok Credentials](obtaining-pentok-redentials) 

```java
    /**
     * @param context
     * @param apiKey
     * @param sessionId
     */
    public OTAcceleratorSession(Context context, String apiKey, String sessionId) {
        super(context, apiKey, sessionId);
    }
```
 
####Signals Management

#####Register a signal listener for a given signal.

Pass to this method the name of the signal this listener will listen to. Pass "*" if the listener is to be invoked for all signal it will return a listener that will be invoked when a signal is received.
 
```java
     public void addSignalListener(String signalName, com.tokbox.android.otsdkwrapper.listeners.SignalListener listener) {
        Log.d(LOG_TAG, "Adding Signal Listener for: " + signalName);
        ArrayList<com.tokbox.android.otsdkwrapper.listeners.SignalListener> perNameListeners = mSignalListeners.get(signalName);
        if (perNameListeners == null) {
            perNameListeners = new ArrayList<com.tokbox.android.otsdkwrapper.listeners.SignalListener>();
            mSignalListeners.put(signalName, perNameListeners);
        }
        if (perNameListeners.indexOf(listener) == -1) {
            Log.d(LOG_TAG, "Signal listener for: " + signalName + " is new!");
            perNameListeners.add(listener);
        }
    }
```

#####Remove an object as signal listener everywhere it's used. 

This method is added to support the common cases where an activity (or some object that depends on an activity) is used as a listener but the activity can be destroyed at some points (which would cause the app to crash if the signal was delivered). Pass to this method the listener to be removed.

```java
    public void removeSignalListener(com.tokbox.android.otsdkwrapper.listeners.SignalListener listener) {
        Enumeration<String> signalNames = mSignalListeners.keys();
        while (signalNames.hasMoreElements()) {
            String signalName = signalNames.nextElement();
            Log.d(LOG_TAG, "removeSignal(" + listener.toString() + ") for " + signalName);
            removeSignalListener(signalName, listener);
        }
    }
```

#####Send a new signal
 
Requires a `SignalInfo` of the signal to be sent. Add the destiantion connection. If null, the signal will be sent to all.

```java
public void sendSignal(SignalInfo signalInfo, Connection connection) {
        if (mOutputSignalProtocol != null) {
            mOutputSignalProtocol.write(signalInfo);
        } else {
            if ( connection != null )
                internalSendSignal(signalInfo, connection);
            else
                internalSendSignal(signalInfo, null);
        }
    }
```

###Examples

#### Screensharing

According to [start and stop publishing media](#start-and-stop-publishing-media), you can start screensharing using OTWrapper 

```java
mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                       name("Tokboxer").build(), true);
```

In the other hand, screesharing with a customer capturer can be achieved using: 

```java
mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                       name("Tokboxer”).capturer(myCapturer).build(), true);
```
 
