package com.opentok.accelerator.textchat

import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.opentok.accelerator.core.wrapper.OTAcceleratorSession
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
    fun creating_new_instance_with_null_session_throws_exception() {
        // when
        val func = { TextChatFragment.newInstance(null, "apiKey") }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun creating_new_instance_with_null_apiKey_throws_exception() {
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
    fun creating_new_instance_with_empty_apiKey_throws_exception() {
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
    fun setting_maxTextLength_greater_than_opentok_max_will_set_opentok_max() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.maxTextLength = 8197 } // Max open tok length 8195

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun setting_maxTextLength_to_zero_throws_exception() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.maxTextLength = 0 }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun setting_maxTextLength_lower_than_zero_throws_exception() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.maxTextLength = -1 }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun setting_null_senderAlias_throws_exception() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.senderAlias = null }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun setting_empty_senderAlias_throws_exception() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.senderAlias = "" }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun setting_actionBar_sets_action_bar() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        textChatFragment.actionBar = LinearLayout(context)

        // then
        textChatFragment.actionBar shouldNotBe null
    }

    @Test
    fun setting_null_actionBar_throw_exception() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        val func = { textChatFragment.actionBar = null }

        // then
        func shouldThrow java.lang.IllegalArgumentException::class
    }

    @Test
    fun setting_sendMessageView_sets_sendMessageView() {
        // given
        val textChatFragment = getTextChatFragment()

        // when
        textChatFragment.sendMessageView = LinearLayout(context)

        // then
        textChatFragment.sendMessageView shouldNotBe null
    }

    @Test
    fun setting_null_sendMessageView_throw_exception() {
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