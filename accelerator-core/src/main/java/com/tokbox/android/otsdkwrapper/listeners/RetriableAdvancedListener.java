package com.tokbox.android.otsdkwrapper.listeners;


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
  void onVideoQualityWarning(Wrapper wrapper, String remoteId);

  @Override
  void onVideoQualityWarningLifted(Wrapper wrapper, String remoteId);

  @Override
  void onError(Wrapper wrapper, OpentokError error);
}