package com.seedcipher.app

import com.seedcipher.app.scrambler.SeedScramblerEngine
import com.seedcipher.app.stego.EmojiStegoEngine
import org.junit.Assert.*
import org.junit.Test

class EmojiStegoTest {

    @Test
    fun testEmojiStegoEmbeddingAndExtraction() {
        val coverText = "Hey! Let's grab some coffee and tacos at 12:30 ☕️🌮"
        val secretPhrase = "witch collapse practice feed shame open despair creek road again ice least"

        // 1. Encrypt secret phrase with AES-256 CryptoVault
        val seedPassword = "my-secret-vault-key-2026"
        val ciphertext = SeedScramblerEngine.scrambleCryptoVault(secretPhrase, seedPassword)

        // 2. Embed into cover text with emojis
        val stegoMessage = EmojiStegoEngine.embedInCoverText(coverText, ciphertext)

        // Verify visible cover text is clean and readable
        val cleaned = EmojiStegoEngine.cleanVisibleText(stegoMessage)
        assertEquals(coverText, cleaned)

        // 3. Extract steganographic payload
        val (found, extractedCiphertext) = EmojiStegoEngine.extractFromCoverText(stegoMessage)
        assertTrue("Stego payload should be found", found)
        assertNotNull(extractedCiphertext)
        assertEquals(ciphertext, extractedCiphertext)

        // 4. Decrypt extracted payload
        val decrypted = SeedScramblerEngine.unscrambleCryptoVault(extractedCiphertext!!, seedPassword)
        assertEquals(secretPhrase, decrypted)
    }

    @Test
    fun testMultipleEmojiTopics() {
        for (topic in EmojiStegoEngine.DECOY_TOPICS) {
            for (sample in topic.sampleMessages) {
                val secret = "Secret-Data-123456"
                val stego = EmojiStegoEngine.embedInCoverText(sample, secret)
                val (found, extracted) = EmojiStegoEngine.extractFromCoverText(stego)
                assertTrue("Failed for topic ${topic.category} message: $sample", found)
                assertEquals(secret, extracted)
            }
        }
    }
}
