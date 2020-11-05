package com.tokbox.accelerator.textchat

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.opentok.android.OpentokError
import com.opentok.android.Session
import com.opentok.android.Session.SessionListener
import com.opentok.android.Stream
import com.tokbox.accelerator.textchat.config.APIConfig
import com.tokbox.otsdkwrapper.wrapper.OTAcceleratorSession
import org.junit.After
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseTest {
    protected var instrumentation = InstrumentationRegistry.getInstrumentation()
    protected var context = instrumentation.context
    protected var apiKey = APIConfig.API_KEY
    protected var sessionId = APIConfig.SESSION_ID
    protected var token = APIConfig.TOKEN
    protected var session: OTAcceleratorSession? = null
    protected var sessionConnected = AtomicBoolean()
    protected var sessionError = AtomicBoolean()
    protected var sessionConnectedLock = CountDownLatch(1)
    protected var sessionErrorLock = CountDownLatch(1)
    protected var sessionLastError: OpentokError? = null
    protected var sessionListener: SessionListener = object : SessionListener {
        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d(LOGTAG, "Session - onStreamReceived")
        }

        override fun onError(session: Session, error: OpentokError) {
            Log.d(LOGTAG, "Session - onError")
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d(LOGTAG, "Session - onStreamDropped")
        }

        override fun onDisconnected(session: Session) {
            Log.d(LOGTAG, "Session - onDisconnected")
        }

        override fun onConnected(session: Session) {
            Log.d(LOGTAG, "Session - onConnected")
            sessionConnected.set(true)
            sessionConnectedLock.countDown()
        }
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        sessionConnectedLock = CountDownLatch(1)
        sessionErrorLock = CountDownLatch(1)
        sessionConnected.set(false)
        sessionError.set(false)
    }

    @Throws(InterruptedException::class)
    protected fun waitSessionConnected() {
        sessionConnectedLock.await(WAIT_TIME.toLong(), TimeUnit.SECONDS)
        Assert.assertTrue("session failed to connect", sessionConnected.get())
        Assert.assertFalse(sessionError.get())
        Assert.assertNull(sessionLastError)
    }

    companion object {
        var LOGTAG = BaseTest::class.java.name
        protected const val WAIT_TIME = 75
    }
}