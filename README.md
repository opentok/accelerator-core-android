![logo](tokbox-logo.png)

# Accelerator Core Android &nbsp; [![GitHub version](https://badge.fury.io/gh/opentok%2Faccelerator-core-android.svg)](https://badge.fury.io/gh/opentok%2Faccelerator-core-android) [![Build Status](https://travis-ci.org/opentok/accelerator-core-android.svg?branch=master)](https://travis-ci.org/opentok/accelerator-core-android) [![license MIT](https://img.shields.io/github/license/opentok/accelerator-core-android.svg)](./.github/LICENSE)

## Quickstart
The Accelerator Core Android library gives you an easy way to integrate [OpenTok SDK](https://tokbox.com) to any Android applications.

## Installation

  - [Using the repository](#using-the-repository)
  - [Using Maven](#using-maven)


### Using the repository

1. Clone the [accelerator core android repository](https://github.com/opentok/accelerator-core-android).
2. Start Android Studio and create a new project.
3. From the new project, right-click the app name and select New > Module > Import Gradle Project.
4. Navigate to the directory in which you cloned **OpenTok Accelerator Core Android**, select **accelerator-core**, and click **Finish**.
4. Open the **build.gradle** file for the app and ensure the following lines have been added to the `dependencies` section:

```
compile project(':accelerator-core-android')

```
### Using Maven

Download it from http://tokbox.bintray.com/maven. For example:

a) Edit the build.gradle for your project and add the following code snippet to the allprojects/repositories section:

  ```
  maven { url  "http://tokbox.bintray.com/maven" }
  ```

b) Modify build.gradle for your module and add the following code snippet to the dependencies section:

  ```
  compile 'com.opentok.android:opentok-accelerator-core:1.0.+’
  ```

## Exploring the Code

For detail about the APIs used to develop this library, see the [OpenTok Android SDK Reference](https://tokbox.com/developer/sdks/android/reference/) and [Android API Reference](http://developer.android.com/reference/packages.html).


### Main Class Design

| Class        | Description  |
| ------------- | ------------- |
| `OTAcceleratorSession` | Manages the session, allows multiple listeners and implements a signaling protocol. |
| `OTWrapper` | Represents an OpenTok object to enable a video communication. |
| `OTConfig`   | Defines the OpenTok Configuration to be used in the communication. It includes SessionId, Token and APIKey, and features like to subscribe automatically or subscribe to self. |
| `PreviewConfig` | Defines the configuration of the local preview. |
| `BasicListener` | Monitors basic state changes in the OpenTok communication. |
| `AdvancedListener` | Monitors advanced state changes in the OpenTok communication.|
| `SignalListener` | Monitors a new signal received in the OpenTok communication. |
| `MediaType` | Defines the Audio and Video media type. |
| `StreamStatus` | Defines the current status of the Stream properties. |
| `VideoScale` | Defines the FIT and FILL modes setting for the renderer. |
| `ScreenSharingCapturer` | Custom screen sharing capturer. In the absence of a custom or default camera capturer, `Core Android` will generate one using the full screen as the capturer. |
| `ScreenSharingFragment` | Headless fragment used to implement the screensharing feature by default. |

## Using Accelerator Core Android

You can start testing a basic multiparty  application using the Accelerator Core with best-practices for Android.
 - [OpenTok Accelerator Sample Application](https://github.com/opentok/accelerator-sample-apps-android).

### Initialization

The first step in using the OTWrapper is to initialize it by calling the constructor with the OpenTokConfig parameter.

```java
    OTConfig config =
                new OTConfig.OTConfigBuilder(SESSION_ID, TOKEN,
                        API_KEY).name("core-sample").subscribeAutomatically(true).subscribeToSelf(false).build();

    OTWrapper mWrapper = new OTWrapper(MainActivity.this, config);

```

#### Obtaining OpenTok Credentials

To use OpenTok's framework you need a Session ID, Token, and API Key you can get these values at the [OpenTok Developer Dashboard](https://dashboard.tokbox.com/) . For production deployment, you must generate the Session ID and Token values using one of the [OpenTok Server SDKs](https://tokbox.com/developer/sdks/server/).


### Set and define listeners

```java

    mWrapper.addBasicListener(mBasicListener);
    mWrapper.addAdvancedListener(mAdvancedListener);

    ....

    private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {
                //.......
            });

    private AdvancedListener mAdvancedListener =
            new PausableAdvancedListener(new AdvancedListener<OTWrapper>() {
            //.......
        });
```

### Connect and disconnect from an OpenTok session

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


### Start and stop preview

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

### Start and stop publishing media

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

### Pause and resume communication

Call these methods when the app's activity pauses or resumes. These pause or resume the video for the local preview and remotes.
The Accelerator Core offers the possibility to resume the events setting the `resumeEvents` parameter to TRUE in the `resume` method.

```java
  mWrapper.pause();

  //.....

  mWrapper.resume(true);  
```

### Connections management

The Accelerator Core offers a set of methods to manage the connections of the session.

```java
	//get our own connection Id
	String myConnectionId = mWrapper.getOwnConnId();

	//get the total connections in the session
	String totalParticipants = mWrapper.getConnectionsCount();

	//check if the own connection is the oldest in the current session
	Boolen isTheOldest = mWrapper.isTheOldestConnection();

	//compare the connections creation times between the local connection and the argument passing
	int older = mWrapper.compareConnectionsTimes(remoteConnId);

```

### Enable and disable the publishing and receiving media

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

### Get stream status

The status of a stream includes the media status, the stream type, the status of the media containers and the stream dimensions.

```java
	//to get the publishing stream status
	StreamStatus localStreamStatus = mWrapper.getPublishingStreamStatus();

	//to get the remote stream status
	StreamStatus remoteStreamStatus = mWrapper.getRemoteStremStatus(remoteId);

```

### Signals Management

The Accelerator Core includes a complete Signaling protocol to register a signal listener for a given type.

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

### Customize capturer and renderers

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

### Set Video Renderer styles

The video scale mode can be modified to FILL or FIT value for the publishing video or for the received video from the remotes.

```java
	mWrapper.setPublishingStyle(VideoScalse.FIT);
	mWrapper.setRemoteStyle(remoteId, VideoScale.FILL);
```

### Cycle the camera

Cycle between cameras, if there are multiple cameras on the device. Then, the AdvancedListener.onCameraChanged(...) event is called.

```java
	mWrapper.cycleCamera();
```

### Advanced events

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

### Screensharing

According to [start and stop publishing media](#start-and-stop-publishing-media), you can start screensharing using OTWrapper

```java
mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                       name("Tokboxer").build(), true);
```

Screesharing with a customer capturer can be achieved using:

```java
mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                       name("Tokboxer”).capturer(myCapturer).build(), true);
```

### Using OTAcceleratorSession

The Accelerator Core library uses the `OTAcceleratorSession` to manage the OpenTok Session. This class lets you have several Session listeners and manage the signalling in the lib.

In the case, you don't need the audio/video communication, you can start by creating an OTAcceleratorSession instance, an apiKey and sessionID are requires. For more visit [Obtaining OpenTok Credentials](obtaining-pentok-redentials)

```java

    OTAcceleratorSession mSession = new OTAcceleratorSession(context, apikey, sessionId);
    mSession.addSignalListener("CHAT", this);

    //send a new signal
    JSONObject messageObj = new JSONObject();
    messageObj.put("sender", "Tokbox");
    messageObj.put("text", "hi!");
    messageObj.put("sentOn", System.currentTimeMillis());

    mSession.sendSignal(new SignalInfo(mSession.getConnection().getConnectionId(), null, "CHAT", messageObj.toString()), null);

```

To get the OTAcceleratorSession instance used in the audio/video communication, call to:

```java
   OTAcceleratorSession mSession = mWrapper.getSession();
```
