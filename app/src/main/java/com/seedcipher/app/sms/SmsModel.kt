package com.seedcipher.app.sms

data class SmsMessageItem(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val decryptedBody: String? = null,
    val isEncryptedPayload: Boolean = false,
    val stegoCoverText: String? = null,
    val imageUri: String? = null,
    val hasPhotoStego: Boolean = false,
    val photoSecret: String? = null,
    val hasEmojiStego: Boolean = false,
    val emojiSecret: String? = null
)

data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String,
    val lastMessage: SmsMessageItem,
    val messages: List<SmsMessageItem> = emptyList()
)
