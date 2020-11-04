package com.tokbox.android.accpack.textchat.testbase;


import android.content.Context;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.tokbox.android.accpack.textchat.config.APIConfig;
import com.tokbox.android.otsdkwrapper.wrapper.OTAcceleratorSession;
import org.junit.After;
import org.junit.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class BaseTest {

    public static String LOGTAG = BaseTest.class.getName();

    protected final static int WAIT_TIME = 75;

    protected Context context;
    protected String sessionId = APIConfig.SESSION_ID;
    protected String apiKey = APIConfig.API_KEY;
    protected String token = APIConfig.TOKEN;

    protected OTAcceleratorSession session;

    protected AtomicBoolean sessionConnected = new AtomicBoolean();
    protected AtomicBoolean sessionError = new AtomicBoolean();

    protected CountDownLatch sessionConnectedLock = new CountDownLatch(1);
    protected CountDownLatch sessionErrorLock = new CountDownLatch(1);

    protected OpentokError sessionLastError = null;

    protected Session.SessionListener sessionListener = new Session.SessionListener() {


        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(LOGTAG,"Session - onStreamReceived");
        }

        @Override
        public void onError(Session session, OpentokError error) {
            Log.d(LOGTAG,"Session - onError");
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(LOGTAG,"Session - onStreamDropped");
        }


        @Override
        public void onDisconnected(Session session) {
            Log.d(LOGTAG,"Session - onDisconnected");
        }

        @Override
        public void onConnected(Session session) {
            Log.d(LOGTAG,"Session - onConnected");
            sessionConnected.set(true);
            sessionConnectedLock.countDown();
        }
    };

    protected Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void tearDown() throws Exception {
        sessionConnectedLock = new CountDownLatch(1);
        sessionErrorLock = new CountDownLatch(1);
        sessionConnected.set(false);
        sessionError.set(false);
    }


    protected void waitSessionConnected() throws InterruptedException {
        sessionConnectedLock.await(WAIT_TIME, TimeUnit.SECONDS);
        Assert.assertTrue("session failed to connect", sessionConnected.get());
        Assert.assertFalse(sessionError.get());
        Assert.assertNull(sessionLastError);
    }
}
