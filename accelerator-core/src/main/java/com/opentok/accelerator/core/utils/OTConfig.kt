package com.opentok.accelerator.core.utils

import com.opentok.accelerator.core.GlobalLogLevel
import kotlin.experimental.and

/**
 * Defines the OpenTok Configuration to be used in the communication.
 *
 * @property sessionId the OpenTok session id
 * @property token the OpenTok token
 * @property apiKey the OpenTok apiKey
 * @property sessionName the OpenTok session name
 * @property subscribeToSelf subscribe to self
 * @property subscribeAutomatically subscribe automatically
 */
data class OTConfig(
    val apiKey: String,
    val sessionId: String,
    val token: String,
    val sessionName: String? = null,
    val subscribeToSelf: Boolean = false,
    val subscribeAutomatically: Boolean = true
) {

    init {
        check(apiKey.isNotBlank()) { "apiKey can't be blank" }
        check(sessionId.isNotBlank()) { "sessionId can't be blank" }
        check(token.isNotBlank()) { "token can't be blank" }

        if (sessionName != null) {
            check(sessionName.length <= MAX_LENGTH_NAME) { "Name length can't be greater than $MAX_LENGTH_NAME" }
            check(sessionName.isNotBlank()) { "name can't be blank" }
        }
    }

    companion object {
        private val LOG_TAG = OTConfig::class.java.simpleName
        private const val LOCAL_LOG_LEVEL: Short = 0xFF
        private val LOG = LogWrapper((GlobalLogLevel.sMaxLogLevel and LOCAL_LOG_LEVEL))

        fun setLogLevel(logLevel: Short) {
            LOG.setLogLevel(logLevel)
        }

        private const val MAX_LENGTH_NAME = 50
    }
}