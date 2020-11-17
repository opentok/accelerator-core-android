package com.opentok.otsdkwrapper.listeners;

import android.view.View;
import com.opentok.android.OpentokError;

public interface RetriableBasicListener<Wrapper>
  extends BasicListener<Wrapper>, RetriableOTListener {

  @Override
  void onConnected(Wrapper wrapper, int participantsNumber, String connId, String data);

  @Override
  void onDisconnected(Wrapper wrapper, int participantsNumber, String connId, String data);

  @Override
  void onPreviewViewReady(Wrapper wrapper, View localView);

  @Override
  void onPreviewViewDestroyed(Wrapper wrapper, View localView);

  @Override
  void onStartedPublishingMedia(Wrapper wrapper, boolean screensharing);

  @Override
  void onStoppedPublishingMedia(Wrapper wrapper, boolean screensharing);

  @Override
  void onRemoteViewReady(Wrapper wrapper, View remoteView, String remoteId, String data);

  @Override
  void onRemoteViewDestroyed(Wrapper wrapper, View remoteView, String remoteId);

  @Override
  void onRemoteJoined(Wrapper wrapper, String remoteId);

  @Override
  void onRemoteLeft(Wrapper wrapper, String remoteId);

  @Override
  void onRemoteVideoChanged(Wrapper wrapper, String remoteId, String reason, boolean videoActive,
                            boolean subscribed);

  @Override
  void onError(Wrapper wrapper, OpentokError error);
}