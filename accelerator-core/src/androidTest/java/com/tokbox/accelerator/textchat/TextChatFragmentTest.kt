package com.tokbox.accelerator.textchat

import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tokbox.otsdkwrapper.wrapper.OTAcceleratorSession
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldThrow
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextChatFragmentTest : BaseTest() {

    @Test
    fun `throw exception when session is null`() {
        // when
        val func = { TextChatFragment.newInstance(null, apiKey) }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw exception when apiKey is null`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, null, sessionId)
        }

        // when
        val func = { TextChatFragment.newInstance(session, null) }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw exception when apiKey is empty`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, null, sessionId)
        }

        // when
        val func = { TextChatFragment.newInstance(session, "") }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `set max length when maxTextLength is greater than opentok max`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment?.maxTextLength = 8197 } // Max open tok length 8195

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw exception when maxTextLength is zero`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment?.maxTextLength = 0 }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw exception when maxTextLength is lower than zero`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment?.maxTextLength = -1 }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw exception when senderAlias is null`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment?.senderAlias = null }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw exception when senderAlias is empty`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment?.senderAlias = "" }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `set actionBar`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        textChatFragment.actionBar = LinearLayout(context)

        // then
        textChatFragment.actionBar shouldNotBe null
    }

    @Test
    fun `throw exception when set actionBar is null`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment.actionBar = null }

        // then
        func shouldThrow java.lang.IllegalArgumentException::class
    }

    @Test
    fun `set sendMessageView`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        textChatFragment.sendMessageView = LinearLayout(context)

        // then
        textChatFragment.sendMessageView shouldNotBe null
    }

    @Test
    fun `throw exception when set sendMessageView is null`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, apiKey, sessionId)
        }

        val textChatFragment = TextChatFragment.newInstance(session, apiKey)

        // when
        val func = { textChatFragment.sendMessageView = null }

        // then
        func shouldThrow java.lang.IllegalArgumentException::class
    }
}