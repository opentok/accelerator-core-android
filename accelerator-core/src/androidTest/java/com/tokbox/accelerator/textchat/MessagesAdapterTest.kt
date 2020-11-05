package com.tokbox.accelerator.textchat

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tokbox.accelerator.textchat.ChatMessage.MessageStatus
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldThrow
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MessagesAdapterTest {

    @Test
    fun creating_new_instance_with_null_list_throws_exception() {
        // when
        val func = { MessagesAdapter(null) }

        // then
        func shouldThrow IllegalArgumentException::class
    }

    @Test
    fun creating_new_instance_with_empty_list_creates_instance() {
        // given
        val messagesList = ArrayList<ChatMessage>()

        // when
        val messagesAdapter = MessagesAdapter(messagesList)

        // then
        messagesAdapter.itemCount shouldBeEqualTo 0
    }

    @Test
    fun creating_new_instance_with_list_containing_one_item() {
        // given
        val chatMessage = ChatMessage
            .ChatMessageBuilder("1", UUID.randomUUID(), MessageStatus.SENT_MESSAGE)
            .build()

        val messagesList = ArrayList<ChatMessage>()
        messagesList.add(chatMessage)

        // when
        val messagesAdapter = MessagesAdapter(messagesList)

        // then
        messagesAdapter.itemCount shouldBeEqualTo 1
    }

    @Test
    fun getting_view_type_for_existing_item_returns_view_type() {
        // given
        val chatMessage = ChatMessage
            .ChatMessageBuilder("1", UUID.randomUUID(), MessageStatus.SENT_MESSAGE)
            .build()
        val messagesList = ArrayList<ChatMessage>()
        messagesList.add(chatMessage)

        // when
        val messagesAdapter = MessagesAdapter(messagesList)

        // then
        messagesAdapter.getItemViewType(0) shouldNotBe null
    }

    @Test
    fun getting_view_type_for_non_existing_item_throws_exception() {
        // given
        val messagesList = ArrayList<ChatMessage>()
        val messagesAdapter = MessagesAdapter(messagesList)

        // when
        val f = { messagesAdapter.getItemViewType(0) }

        // then
        f shouldThrow IndexOutOfBoundsException::class
    }
}