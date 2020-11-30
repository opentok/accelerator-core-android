package com.opentok.accelerator.textchat

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.opentok.accelerator.textchat.ChatMessage.MessageStatus
import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldThrow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatMessagesAdapterTest {

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun creating_new_instance_with_null_list_throws_exception() {
        // when
        val func = { ChatMessagesAdapter(null) }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun creating_new_instance_with_empty_list_creates_instance() {
        // given
        val messagesList = ArrayList<ChatMessage>()

        // when
        val messagesAdapter = ChatMessagesAdapter(messagesList)

        // then
        messagesAdapter.itemCount shouldBeEqualTo 0
    }

    @Test
    fun creating_new_instance_with_list_containing_one_item() {
        // given
        val chatMessage = mockk<ChatMessage>();

        val messagesList = ArrayList<ChatMessage>()
        messagesList.add(chatMessage)

        // when
        val messagesAdapter = ChatMessagesAdapter(messagesList)

        // then
        messagesAdapter.itemCount shouldBeEqualTo 1
    }

    @Test
    fun getting_view_type_for_existing_item_returns_view_type() {
        // given
        val chatMessage = ChatMessage(
            UUID.randomUUID(),
            MessageStatus.SENT_MESSAGE,
            "abc",
            1,
            "senderId",
            "senderAlias"
        )

        val messagesList = ArrayList<ChatMessage>()
        messagesList.add(chatMessage)

        // when
        val messagesAdapter = ChatMessagesAdapter(messagesList)

        // then
        messagesAdapter.getItemViewType(0) shouldNotBe null
    }

    @Test
    fun getting_view_type_for_non_existing_item_throws_exception() {
        // given
        val messagesList = ArrayList<ChatMessage>()
        val messagesAdapter = ChatMessagesAdapter(messagesList)

        // when
        val f = { messagesAdapter.getItemViewType(0) }

        // then
        f shouldThrow IndexOutOfBoundsException::class
    }
}