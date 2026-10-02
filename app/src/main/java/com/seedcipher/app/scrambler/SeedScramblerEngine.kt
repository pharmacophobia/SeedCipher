package com.seedcipher.app.scrambler

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Native Kotlin Cryptographic Engine for SeedCipher
 * Supports 4 Modes:
 * 1. SeedPermute (Reversible deterministic text & character shuffling)
 * 2. EmojiCipher (Seed-mapped visual glyph cipher)
 * 3. CryptoVault (AES-256-GCM military encryption with PBKDF2)
 * 4. StegoHide (Invisible zero-width steganographic embedding)
 */

class Mulberry32PRNG(seed: Long) {
    private var state: Long = seed and 0xFFFFFFFFL
    init {
        if (state == 0L) state = 0x12345678L
    }

    fun nextFloat(): Float {
        state = (state + 0x6D2B79F5L) and 0xFFFFFFFFL
        var z = state
        z = ((z xor (z ushr 15)) * (z or 1L)) and 0xFFFFFFFFL
        z = z xor (z + ((z xor (z ushr 7)) * (z or 61L))) and 0xFFFFFFFFL
        val res = (z xor (z ushr 14)) and 0xFFFFFFFFL
        return res.toFloat() / 4294967296f
    }

    fun nextInt(min: Int, max: Int): Int {
        val range = max - min + 1
        return min + (nextFloat() * range).toInt().coerceIn(0, range - 1)
    }
}

object SeedScramblerEngine {

    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:',.<>?/~` "

    private val EMOJI_PALETTE = arrayOf(
        "🔒","🔑","⚡","🔥","🌌","💎","🔮","🌀","🎯","🎲",
        "🛡️","⚔️","👑","🤖","👾","🛸","🚀","🌙","⭐","💥",
        "🧩","🗝️","🧿","🐺","🦁","🐉","🦅","🐍","🎭","🎪",
        "🎰","🎨","🏆","🥇","☣️","☢️","🌐","🧬","⚡","💥"
    )

    private const val ZW_ZERO = "\u200B"
    private const val ZW_ONE = "\u200C"
    private const val ZW_SEP = "\u200D"

    fun seedToUint32(seedStr: String): Long {
        if (seedStr.isEmpty()) return 1337L
        var hash1 = 5381L
        var hash2 = 0L
        for (ch in seedStr) {
            val code = ch.code.toLong()
            hash1 = ((hash1 * 33) xor code) and 0xFFFFFFFFL
            hash2 = (code + (hash2 shl 6) + (hash2 shl 16) - hash2) and 0xFFFFFFFFL
        }
        return (hash1 xor hash2) and 0xFFFFFFFFL
    }

    // ------------------------------------------------------------------------
    // MODE 1: SEED PERMUTE
    // ------------------------------------------------------------------------
    fun scrambleSeedPermute(text: String, seed: String): String {
        if (text.isEmpty()) return ""
        val numericSeed = seedToUint32(seed)
        val prng = Mulberry32PRNG(numericSeed)

        val chars = text.toCharArray()
        val len = chars.size

        // Step 1: Character substitution
        for (i in 0 until len) {
            val ch = chars[i]
            val shift = prng.nextInt(1, 95)

            if (LOWER.contains(ch)) {
                val idx = LOWER.indexOf(ch)
                chars[i] = LOWER[(idx + shift) % LOWER.length]
            } else if (UPPER.contains(ch)) {
                val idx = UPPER.indexOf(ch)
                chars[i] = UPPER[(idx + shift) % UPPER.length]
            } else if (DIGITS.contains(ch)) {
                val idx = DIGITS.indexOf(ch)
                chars[i] = DIGITS[(idx + shift) % DIGITS.length]
            } else if (SYMBOLS.contains(ch)) {
                val idx = SYMBOLS.indexOf(ch)
                chars[i] = SYMBOLS[(idx + shift) % SYMBOLS.length]
            }
        }

        // Step 2: Fisher-Yates position permutation
        val permPRNG = Mulberry32PRNG(numericSeed xor 0xA5A5A5A5L)
        val perm = IntArray(len) { it }
        for (i in len - 1 downTo 1) {
            val j = permPRNG.nextInt(0, i)
            val tmp = perm[i]
            perm[i] = perm[j]
            perm[j] = tmp
        }

        val result = CharArray(len)
        for (i in 0 until len) {
            result[perm[i]] = chars[i]
        }

        return "SP1:" + String(result)
    }

    fun unscrambleSeedPermute(scrambledText: String, seed: String): String {
        if (scrambledText.isEmpty()) return ""
        val payload = if (scrambledText.startsWith("SP1:")) scrambledText.substring(4) else scrambledText
        val numericSeed = seedToUint32(seed)
        val len = payload.length

        // Step 1: Reconstruct Fisher-Yates permutation
        val permPRNG = Mulberry32PRNG(numericSeed xor 0xA5A5A5A5L)
        val perm = IntArray(len) { it }
        for (i in len - 1 downTo 1) {
            val j = permPRNG.nextInt(0, i)
            val tmp = perm[i]
            perm[i] = perm[j]
            perm[j] = tmp
        }

        val chars = CharArray(len)
        val scrambledChars = payload.toCharArray()
        for (i in 0 until len) {
            chars[i] = scrambledChars[perm[i]]
        }

        // Step 2: Reverse character substitution
        val prng = Mulberry32PRNG(numericSeed)
        for (i in 0 until len) {
            val ch = chars[i]
            val shift = prng.nextInt(1, 95)

            if (LOWER.contains(ch)) {
                val idx = LOWER.indexOf(ch)
                var origIdx = (idx - (shift % LOWER.length)) % LOWER.length
                if (origIdx < 0) origIdx += LOWER.length
                chars[i] = LOWER[origIdx]
            } else if (UPPER.contains(ch)) {
                val idx = UPPER.indexOf(ch)
                var origIdx = (idx - (shift % UPPER.length)) % UPPER.length
                if (origIdx < 0) origIdx += UPPER.length
                chars[i] = UPPER[origIdx]
            } else if (DIGITS.contains(ch)) {
                val idx = DIGITS.indexOf(ch)
                var origIdx = (idx - (shift % DIGITS.length)) % DIGITS.length
                if (origIdx < 0) origIdx += DIGITS.length
                chars[i] = DIGITS[origIdx]
            } else if (SYMBOLS.contains(ch)) {
                val idx = SYMBOLS.indexOf(ch)
                var origIdx = (idx - (shift % SYMBOLS.length)) % SYMBOLS.length
                if (origIdx < 0) origIdx += SYMBOLS.length
                chars[i] = SYMBOLS[origIdx]
            }
        }

        return String(chars)
    }

    // ------------------------------------------------------------------------
    // MODE 2: EMOJI GLYPH CIPHER
    // ------------------------------------------------------------------------
    fun scrambleEmoji(text: String, seed: String): String {
        if (text.isEmpty()) return ""
        val numericSeed = seedToUint32(seed.ifEmpty { "default" })
        val prng = Mulberry32PRNG(numericSeed)

        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val sb = StringBuilder("EM1:")

        for (b in bytes) {
            val mask = prng.nextInt(0, 255)
            val valXor = (b.toInt() and 0xFF) xor mask
            val e1 = (valXor / EMOJI_PALETTE.size) % EMOJI_PALETTE.size
            val e2 = valXor % EMOJI_PALETTE.size
            sb.append(EMOJI_PALETTE[e1]).append(EMOJI_PALETTE[e2])
        }

        return sb.toString()
    }

    fun unscrambleEmoji(scrambledText: String, seed: String): String {
        if (scrambledText.isEmpty()) return ""
        val payload = if (scrambledText.startsWith("EM1:")) scrambledText.substring(4) else scrambledText
        val numericSeed = seedToUint32(seed.ifEmpty { "default" })
        val prng = Mulberry32PRNG(numericSeed)

        // Split payload into emojis
        val emojiList = ArrayList<String>()
        var i = 0
        while (i < payload.length) {
            val codePoint = payload.codePointAt(i)
            val emojiStr = String(Character.toChars(codePoint))
            emojiList.add(emojiStr)
            i += Character.charCount(codePoint)
        }

        val bytes = ArrayList<Byte>()
        var idx = 0
        while (idx < emojiList.size - 1) {
            val e1Str = emojiList[idx]
            val e2Str = emojiList[idx + 1]

            val e1 = EMOJI_PALETTE.indexOf(e1Str)
            val e2 = EMOJI_PALETTE.indexOf(e2Str)

            if (e1 == -1 || e2 == -1) return "⚠️ Invalid Emoji Cipher Format or Corrupted Data"

            val valXor = (e1 * EMOJI_PALETTE.size + e2) and 0xFF
            val mask = prng.nextInt(0, 255)
            bytes.add((valXor xor mask).toByte())
            idx += 2
        }

        return try {
            String(bytes.toByteArray(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "⚠️ Failed to decode Emoji Cipher"
        }
    }

    // ------------------------------------------------------------------------
    // MODE 3: CRYPTO VAULT (AES-256-GCM)
    // ------------------------------------------------------------------------

    // Memory cache for derived keys to avoid redundant PBKDF2 iterations
    private val keyCache = HashMap<String, SecretKeySpec>()

    private fun getDerivedKey(seed: String, salt: ByteArray): SecretKeySpec {
        val saltStr = Base64.getEncoder().encodeToString(salt)
        val cacheKey = "$seed|$saltStr"
        keyCache[cacheKey]?.let { return it }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(seed.toCharArray(), salt, 50000, 256)
        val key = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        keyCache[cacheKey] = key
        return key
    }

    fun clearKeyCache() {
        keyCache.clear()
    }

    fun scrambleCryptoVault(text: String, seed: String): String {
        if (text.isEmpty()) return ""
        val seedWord = seed.ifEmpty { "default_seed" }

        val random = SecureRandom()
        val salt = ByteArray(16)
        val iv = ByteArray(12)
        random.nextBytes(salt)
        random.nextBytes(iv)

        val secretKey = getDerivedKey(seedWord, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + ciphertext.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)

        return "CV1:" + Base64.getEncoder().encodeToString(combined)
    }

    fun unscrambleCryptoVault(scrambledText: String, seed: String): String {
        if (scrambledText.isEmpty()) return ""
        val payload = if (scrambledText.startsWith("CV1:")) scrambledText.substring(4) else scrambledText

        return try {
            val bytes = Base64.getDecoder().decode(payload)
            if (bytes.size < 28) return "❌ Invalid Cryptographic Payload Length"

            val salt = bytes.copyOfRange(0, 16)
            val iv = bytes.copyOfRange(16, 28)
            val ciphertext = bytes.copyOfRange(28, bytes.size)

            val secretKey = getDerivedKey(seed, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "❌ Incorrect Seed or Corrupted Cryptographic Data"
        }
    }

    // ------------------------------------------------------------------------
    // MODE 4: STEGANOGRAPHY
    // ------------------------------------------------------------------------
    fun hideInStego(visibleCoverText: String, secretScrambledText: String): String {
        if (secretScrambledText.isEmpty()) return visibleCoverText
        val bytes = secretScrambledText.toByteArray(StandardCharsets.UTF_8)
        val sbBits = StringBuilder()

        for (b in bytes) {
            val bitStr = String.format("%8s", Integer.toBinaryString(b.toInt() and 0xFF)).replace(' ', '0')
            for (bit in bitStr) {
                sbBits.append(if (bit == '0') ZW_ZERO else ZW_ONE)
            }
        }

        val cover = visibleCoverText.ifEmpty { "Hey, hope you are having a great day!" }
        val insertIndex = 5.coerceAtMost(cover.length)
        return cover.substring(0, insertIndex) + ZW_SEP + sbBits.toString() + ZW_SEP + cover.substring(insertIndex)
    }

    fun extractFromStego(stegoText: String): Pair<Boolean, String> {
        val firstSep = stegoText.indexOf(ZW_SEP)
        val lastSep = stegoText.lastIndexOf(ZW_SEP)

        if (firstSep == -1 || lastSep == -1 || firstSep == lastSep) {
            return Pair(false, stegoText)
        }

        val zwBits = stegoText.substring(firstSep + 1, lastSep)
        val bytes = ArrayList<Byte>()
        var bitBuffer = StringBuilder()

        for (ch in zwBits) {
            if (ch.toString() == ZW_ZERO) bitBuffer.append("0")
            else if (ch.toString() == ZW_ONE) bitBuffer.append("1")

            if (bitBuffer.length == 8) {
                bytes.add(bitBuffer.toString().toInt(2).toByte())
                bitBuffer = StringBuilder()
            }
        }

        if (bytes.isEmpty()) return Pair(false, stegoText)

        return try {
            Pair(true, String(bytes.toByteArray(), StandardCharsets.UTF_8))
        } catch (e: Exception) {
            Pair(false, stegoText)
        }
    }
}
