package com.tokbox.otsdkwrapper.listeners;

import com.tokbox.otsdkwrapper.signal.SignalInfo;

public interface SignalListener<SignalDataType> {

    void onSignalReceived(SignalInfo<SignalDataType> signalInfo, boolean isSelfSignal);
}