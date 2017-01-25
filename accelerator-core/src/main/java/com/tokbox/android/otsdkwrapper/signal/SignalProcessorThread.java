package com.tokbox.android.otsdkwrapper.signal;

import android.util.Log;

import com.tokbox.android.otsdkwrapper.utils.Callback;

/**
 * This class implements a thread that reads from a input signal pipe and invokes a Callback
 * function (on the same thread) for each processed signal.
 *
 */
public class SignalProcessorThread<OutputDataType, InputDataType> extends Thread {
    private static final String LOG_TAG = SignalProcessorThread.class.getSimpleName();

    private SignalProtocol<OutputDataType, InputDataType> mProcessedProtocol;
    private boolean mChangingPipe = false;
    private Callback<SignalInfo<OutputDataType>> mCallback;

    public SignalProcessorThread(SignalProtocol protocol, Callback<SignalInfo<OutputDataType>> cb) {
        mProcessedProtocol = protocol;
        mCallback = cb;
        if (mProcessedProtocol != null) {
            this.start();
        }
    }

    @Override
    public void run() {
        SignalInfo<OutputDataType> signal;
        final SignalInfo<OutputDataType> FAKE_SIGNAL = new SignalInfo<OutputDataType>("", "", "", null);
        do {
            signal = mProcessedProtocol.read();
            if (signal != null) {
                Log.d(LOG_TAG, "(" + mProcessedProtocol.getClass().getSimpleName() +
                        "): got a processed signal: " + signal.mSignalName);
                mCallback.run(signal);
            } else if (mChangingPipe) {
                mChangingPipe = false;
                signal = FAKE_SIGNAL;
            }
        } while ((mProcessedProtocol != null) && (signal != null));
    }

    /**
     * Note that this *closes* the current queue!
     *
     * @param newProtocol The new input queue
     * @return Usually, this, to allow chaining.
     */
    public synchronized SignalProcessorThread<OutputDataType, InputDataType> switchPipe(
            SignalProtocol<OutputDataType, InputDataType> newProtocol) {
        mChangingPipe = true;
        SignalProtocol<OutputDataType, InputDataType> aux = mProcessedProtocol;
        mProcessedProtocol = newProtocol;
        if (aux != null) {
            aux.close();
        }
        return mProcessedProtocol != null ? this : null;
    }
}