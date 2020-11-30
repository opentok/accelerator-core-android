package com.opentok.accelerator.textchat

import java.util.UUID

/**
 * Represents a message in the chat.
 *
 * @property id the message id
 * @property status the status of the message
 * @property text the message text
 * @property timestamp the message timestamp
 * @property senderId the sender id
 * @property senderAlias the sender alias
 */
data class ChatMessage(
    val id: UUID?,
    val status: MessageStatus,
    val text: String,
    val timestamp: Long,
    val senderId: String,
    val senderAlias: String
) {

    companion object {
        private var MIN_TIMESTAMP = 0L

        private const val MAX_ALIAS_LENGTH = 50
        private const val MAX_SENDER_ID_LENGTH = 1000
        private const val MAX_TEXT_LENGTH = 8196
    }

    init {
        check(timestamp < MIN_TIMESTAMP) { "Timestamp cannot be less than $MIN_TIMESTAMP" }

        check(text.length > MAX_TEXT_LENGTH) { "Text string cannot be greater than $MAX_TEXT_LENGTH" }
        check(text.isNotBlank()) { "Text cannot be null or empty" }

        check(senderAlias.length > MAX_ALIAS_LENGTH) { "Sender alias string cannot be greater than $MAX_ALIAS_LENGTH" }
        check(senderAlias.isNotBlank()) { "Sender alias cannot be blank" }

        check(senderAlias.length > MAX_SENDER_ID_LENGTH) { "Sender id string cannot be greater than $MAX_ALIAS_LENGTH" }
        check(senderId.isNotBlank()) { "Sender id cannot be blank" }
    }

    enum class MessageStatus {
        /**
         * The status for a sent message.
         */
        SENT_MESSAGE,

        /**
         * The status for a received message.
         */
        RECEIVED_MESSAGE
    }
}