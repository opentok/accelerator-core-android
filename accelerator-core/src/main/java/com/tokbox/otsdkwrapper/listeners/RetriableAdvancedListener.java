package com.tokbox.otsdkwrapper.listeners;


import com.opentok.android.OpentokError;

public interface RetriableAdvancedListener<Wrapper>
  extends AdvancedListener<Wrapper>, RetriableOTListener {

  public AdvancedListener setInternalListener(AdvancedListener listener);

  @Override
  void onCameraChanged(Wrapper wrapper);

  @Override
  void onReconnecting(Wrapper wrapper);

  @Override
  void onReconnected(Wrapper wrapper);

  @Override
  void onReconnected(Wrapper wrapper, String remoteId);

  @Override
  void onDisconnected(Wrapper wrapper, String remoteId);

  @Override
  void onAudioEnabled(Wrapper wrapper, String remoteId);

  @Override
  void onAudioDisabled(Wrapper wrapper, String remoteId);

  @Override
  void onVideoQualityWarning(Wrapper wrapper, String remoteId);

  @Override
  void onVideoQualityWarningLifted(Wrapper wrapper, String remoteId);

  @Override
  void onAudioLevelUpdated(float audioLevel);

  @Override
  void onError(Wrapper wrapper, OpentokError error);
}