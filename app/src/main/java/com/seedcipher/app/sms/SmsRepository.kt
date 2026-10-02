package com.seedcipher.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.seedcipher.app.scrambler.SeedScramblerEngine

import com.seedcipher.app.stego.DecoyConversationData
import com.seedcipher.app.stego.EmojiStegoEngine

object SmsRepository {

    fun hasSmsPermissions(context: Context): Boolean {
        val readSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val sendSms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val receiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        return readSms && sendSms && receiveSms
    }

    fun getContactName(context: Context, phoneNumber: String): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return phoneNumber
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) cursor.getString(nameIdx) else phoneNumber
                } else phoneNumber
            } ?: phoneNumber
        } catch (e: Exception) {
            phoneNumber
        }
    }

    fun processAutoDecryption(rawBody: String, seedKey: String): Pair<Boolean, String?> {
        if (rawBody.isEmpty()) return Pair(false, null)

        // Check for invisible emoji/text steganography
        val (hasEmojiStego, emojiExtracted) = EmojiStegoEngine.extractFromCoverText(rawBody)
        val (hasStego, stegoExtracted) = if (hasEmojiStego) Pair(true, emojiExtracted) else SeedScramblerEngine.extractFromStego(rawBody)
        val textToDecrypt = if (hasStego && stegoExtracted != null) stegoExtracted else rawBody

        val hasPrefix = textToDecrypt.startsWith("SP1:") ||
                textToDecrypt.startsWith("EM1:") ||
                textToDecrypt.startsWith("CV1:")

        if (!hasPrefix && !hasStego && seedKey.isEmpty()) {
            return Pair(false, null)
        }

        val decrypted = when {
            textToDecrypt.startsWith("SP1:") -> {
                SeedScramblerEngine.unscrambleSeedPermute(textToDecrypt, seedKey)
            }
            textToDecrypt.startsWith("EM1:") -> {
                SeedScramblerEngine.unscrambleEmoji(textToDecrypt, seedKey)
            }
            textToDecrypt.startsWith("CV1:") -> {
                SeedScramblerEngine.unscrambleCryptoVault(textToDecrypt, seedKey)
            }
            hasStego -> textToDecrypt
            seedKey.isNotEmpty() -> {
                SeedScramblerEngine.unscrambleSeedPermute(textToDecrypt, seedKey)
            }
            else -> null
        }

        val isSuccess = decrypted != null &&
                !decrypted.startsWith("❌") &&
                !decrypted.startsWith("⚠️") &&
                decrypted.isNotBlank()

        return Pair(hasPrefix || hasStego || isSuccess, if (isSuccess) decrypted else null)
    }

    fun fetchSmsThreads(context: Context, seedKey: String): List<SmsThread> {
        val resultThreads = mutableListOf<SmsThread>()

        // Always provide the Decoy Plausible Deniability Conversation with photo & emoji stego
        try {
            resultThreads.add(DecoyConversationData.getDecoyThread(context, seedKey))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!hasSmsPermissions(context)) return resultThreads

        val threadsMap = LinkedHashMap<String, MutableList<SmsMessageItem>>()
        val uri = Uri.parse("content://sms")
        val projection = arrayOf("_id", "address", "body", "date", "type")

        try {
            context.contentResolver.query(
                uri, projection, null, null, "date DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")
                val typeIdx = cursor.getColumnIndex("type")

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val rawAddress = if (addressIdx >= 0) cursor.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else 1

                    val isOutgoing = (type == 2)
                    val (isEncrypted, decrypted) = processAutoDecryption(body, seedKey)
                    val (hasEmojiStego, _) = EmojiStegoEngine.extractFromCoverText(body)

                    val item = SmsMessageItem(
                        id = id,
                        address = rawAddress,
                        body = body,
                        timestamp = timestamp,
                        isOutgoing = isOutgoing,
                        decryptedBody = decrypted,
                        isEncryptedPayload = isEncrypted,
                        hasEmojiStego = hasEmojiStego,
                        emojiSecret = if (hasEmojiStego) decrypted else null,
                        stegoCoverText = if (hasEmojiStego) EmojiStegoEngine.cleanVisibleText(body) else null
                    )

                    val normalizedAddr = rawAddress.replace(" ", "").replace("-", "")
                    threadsMap.getOrPut(normalizedAddr) { mutableListOf() }.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val parsedThreads = threadsMap.map { (addr, msgList) ->
            val contactName = getContactName(context, addr)
            SmsThread(
                threadId = msgList.firstOrNull()?.id ?: 0L,
                address = addr,
                contactName = contactName,
                lastMessage = msgList.first(),
                messages = msgList.sortedBy { it.timestamp }
            )
        }

        resultThreads.addAll(parsedThreads)
        return resultThreads
    }

    fun sendSmsMessage(context: Context, address: String, messageText: String): Boolean {
        if (address.isBlank() || messageText.isBlank()) return false
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(address, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(address, null, messageText, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
