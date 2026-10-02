package com.seedcipher.app.stego

import android.content.Context
import com.seedcipher.app.scrambler.SeedScramblerEngine
import com.seedcipher.app.sms.SmsMessageItem
import com.seedcipher.app.sms.SmsThread
import java.io.File

/**
 * Decoy conversation manager providing plausible deniability.
 * Generates natural everyday chat conversations hiding photo and emoji steganography.
 */
object DecoyConversationData {

    const val DECOY_THREAD_ID = 999901L
    const val DECOY_CONTACT_NAME = "Sarah Jenkins"
    const val DECOY_ADDRESS = "+1 (555) 234-8901"

    // Default sample secret seed phrase embedded in demo messages
    const val DEMO_SECRET_SEED_1 = "witch collapse practice feed shame open despair creek road again ice least"
    const val DEMO_SECRET_SEED_2 = "abandon balance cage direct eagle famous galaxy harbor injury joy keen ladder"

    /**
     * Builds or restores an authentic decoy conversation thread with natural dialog,
     * embedded emoji stego, and a photo stego attachment.
     */
    fun getDecoyThread(context: Context, seedKey: String): SmsThread {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000
        val fortyMinsAgo = now - 2400_000
        val twentyMinsAgo = now - 1200_000
        val tenMinsAgo = now - 600_000

        // 1. Generate Coffee photo with embedded secret stego payload
        val coffeeSecretCipher = SeedScramblerEngine.scrambleCryptoVault(DEMO_SECRET_SEED_2, seedKey.ifEmpty { "seed" })
        val coffeeBitmap = PhotoStegoEngine.generateDecoyBitmap(PhotoStegoEngine.DecoyScene.COFFEE_CUP)
        val stegoCoffeeBitmap = PhotoStegoEngine.embedSecretInBitmap(coffeeBitmap, coffeeSecretCipher)
        val coffeePhotoFile = PhotoStegoEngine.saveBitmapToCache(context, stegoCoffeeBitmap, "decoy_coffee")

        // 2. Generate natural message with hidden emoji stego payload
        val emojiSecretCipher = SeedScramblerEngine.scrambleCryptoVault(DEMO_SECRET_SEED_1, seedKey.ifEmpty { "seed" })
        val emojiCoverText = "Yeah definitely! Just finishing up some notes. Meet there at 12:30? 🍕😋"
        val stegoEmojiBody = EmojiStegoEngine.embedInCoverText(emojiCoverText, emojiSecretCipher)

        val messages = listOf(
            SmsMessageItem(
                id = 1001L,
                address = DECOY_ADDRESS,
                body = "Hey, are you free for lunch today? Thinking about checking out that new taco spot on 5th 🌮",
                timestamp = oneHourAgo,
                isOutgoing = false,
                decryptedBody = null,
                isEncryptedPayload = false
            ),
            SmsMessageItem(
                id = 1002L,
                address = DECOY_ADDRESS,
                body = stegoEmojiBody,
                timestamp = fortyMinsAgo,
                isOutgoing = true,
                decryptedBody = DEMO_SECRET_SEED_1,
                isEncryptedPayload = true,
                hasEmojiStego = true,
                emojiSecret = DEMO_SECRET_SEED_1,
                stegoCoverText = emojiCoverText
            ),
            SmsMessageItem(
                id = 1003L,
                address = DECOY_ADDRESS,
                body = "Sounds good! Grabbing an espresso while I wait. Look at the latte art today ☕️📸",
                timestamp = twentyMinsAgo,
                isOutgoing = false,
                decryptedBody = DEMO_SECRET_SEED_2,
                isEncryptedPayload = true,
                hasPhotoStego = true,
                imageUri = coffeePhotoFile.absolutePath,
                photoSecret = DEMO_SECRET_SEED_2,
                stegoCoverText = "Sounds good! Grabbing an espresso while I wait. Look at the latte art today ☕️📸"
            ),
            SmsMessageItem(
                id = 1004L,
                address = DECOY_ADDRESS,
                body = "Looks amazing! On my way now, should be there in 5 mins 👍🚗",
                timestamp = tenMinsAgo,
                isOutgoing = true,
                decryptedBody = null,
                isEncryptedPayload = false
            )
        )

        return SmsThread(
            threadId = DECOY_THREAD_ID,
            address = DECOY_ADDRESS,
            contactName = DECOY_CONTACT_NAME,
            lastMessage = messages.last(),
            messages = messages
        )
    }
}
