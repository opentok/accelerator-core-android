package com.tokbox.android.otsdkwrapper.listeners;

import com.tokbox.android.otsdkwrapper.signal.SignalInfo;

public interface SignalListener<SignalDataType> {

    void onSignalReceived(SignalInfo<SignalDataType> signalInfo, boolean isSelfSignal);
}