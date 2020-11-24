package com.opentok.accelerator.core.listeners;

import com.opentok.accelerator.core.signal.SignalInfo;

public interface SignalListener<SignalDataType> {

    void onSignalReceived(SignalInfo<SignalDataType> signalInfo, boolean isSelfSignal);
}