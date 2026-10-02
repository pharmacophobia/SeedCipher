package com.seedcipher.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_sms")
data class CachedSms(
    @PrimaryKey val messageId: Long,
    val address: String,
    val rawBody: String,
    val decryptedBody: String?,
    val seedUsed: String?,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isEncrypted: Boolean
)
