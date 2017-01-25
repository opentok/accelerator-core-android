package com.tokbox.android.otsdkwrapper.signal;

/**
 * Information for a Signal. All the attributes are public since this is only needed to be able
 * to group different values into a single return.
 */
public class SignalInfo<DataType> {
    /**
     * Id of the connection where this signal originated.
     */
    public String mSrcConnId;

    /**
     * Id of the connection where this signal was sent to
     */
    public String mDstConnId;

    /**
     * Name of the received/to be send signal
     */
    public String mSignalName;

    /**
     * Data of the signal to be send/received. By default this mData will be a String, but
     * SignalProtocols can override that behavior to allow, for example, processing the content to
     * a JSONObject o a application specific object just once even when there are several listeners
     * for the same signal.
     * Please note that signals sent and received from the wire are *always* Strings, so your incoming
     * protocols should expect a String in this field, and your outgoing protocol should generate a
     * String always.
     */
    public DataType mData;

    public SignalInfo(String srcConnId, String dstConnId, String signalName, DataType data) {
        mSrcConnId = srcConnId;
        mDstConnId = dstConnId;
        mSignalName = signalName;
        mData = data;
    }
}