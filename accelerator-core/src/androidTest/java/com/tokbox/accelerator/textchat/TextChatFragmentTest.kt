package com.tokbox.accelerator.textchat

import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tokbox.otsdkwrapper.wrapper.OTAcceleratorSession
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldThrow
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextChatFragmentTest {

    private var instrumentation = InstrumentationRegistry.getInstrumentation()
    private var context = instrumentation.context
    private var session: OTAcceleratorSession? = null

    @Test
    fun `throw_exception_when_session_is_null`() {
        // when
        val func = { TextChatFragment.newInstance(null, "apiKey") }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw_exception_when_apiKey_is_null`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, null, "sessionId")
        }

        // when
        val func = { TextChatFragment.newInstance(session, null) }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw_exception_when_apiKey_is_empty`() {
        // given
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, null, "sessionId")
        }

        // when
        val func = { TextChatFragment.newInstance(session, "") }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `set_max_length_when_maxTextLength_is_greater_than_opentok_max`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.maxTextLength = 8197 } // Max open tok length 8195

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw_exception_when_maxTextLength_is_zero`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.maxTextLength = 0 }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw_exception_when_maxTextLength_is_lower_than_zero`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.maxTextLength = -1 }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw_exception_when_senderAlias_is_null`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.senderAlias = null }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `throw_exception_when_senderAlias_is_empty`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.senderAlias = "" }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `set_actionBar`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        textChatFragment.actionBar = LinearLayout(context)

        // then
        textChatFragment.actionBar shouldNotBe null
    }

    @Test
    fun `throw_exception_when_set_actionBar_is_null`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.actionBar = null }

        // then
        func shouldThrow java.lang.IllegalArgumentException::class
    }

    @Test
    fun `set_sendMessageView`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        textChatFragment.sendMessageView = LinearLayout(context)

        // then
        textChatFragment.sendMessageView shouldNotBe null
    }

    @Test
    fun `throw_exception_when_set_sendMessageView_is_null`() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.sendMessageView = null }

        // then
        func shouldThrow java.lang.IllegalArgumentException::class
    }

    private fun getTextChatFragment(): TextChatFragment {
        instrumentation.runOnMainSync {
            session = OTAcceleratorSession(context, "apiKey", "sessionId")
        }

        return TextChatFragment.newInstance(session, "apiKey")
    }
}