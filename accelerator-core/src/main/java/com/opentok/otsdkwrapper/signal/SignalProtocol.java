package com.opentok.otsdkwrapper.signal;

/**
 * This interface defines the method a class must implement to be used as a SignalProtocol. The idea
 * behind a SignalProtocol is to enable processing of received or sent signals before they're sent
 * to their consumers/put on the wire. As such, it's possible that several input signals might be
 * translated into a single output, or the reverse.
 * Note that it's possible (and even in some cases expected) that several input SignalInfos might
 * result into a single output SignalInfo.
 * For most uses, it's better if implementors just derive from ThreadedSignalProtocol.
 */
public interface SignalProtocol<OutputDataType, InputDataType> {

    /**
     * Writes a new signal to the pipe. It doesn't block.
     * @param signalInfo Information of the received/to be send signal
     */
    public void write(SignalInfo<InputDataType> signalInfo);

    /**
     * Reads a processed signal. It will block when there is no signal to return. As such, calls to
     * read should be done on a different thread than calls to write since just doing:
     *   signalInfo = signalProcessor.read();
     *   signalProcessor.write(answer);
     * will deadlock unless there's other thread that can write on the processor.
     * @return The processed signal. It'll return null when the pipe has been closed
     */
    public SignalInfo<OutputDataType> read();

    /**
     * Closes the pipe. If the pipe is working on a different thread causes it to die, and it cancels
     * all pending reads.
     */
    public void close();

}