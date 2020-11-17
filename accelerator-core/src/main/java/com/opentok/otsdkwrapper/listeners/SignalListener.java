package com.opentok.otsdkwrapper.listeners;

import com.opentok.otsdkwrapper.signal.SignalInfo;

public interface SignalListener<SignalDataType> {

    void onSignalReceived(SignalInfo<SignalDataType> signalInfo, boolean isSelfSignal);
}