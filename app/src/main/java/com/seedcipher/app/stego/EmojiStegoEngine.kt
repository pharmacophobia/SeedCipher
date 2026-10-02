package com.seedcipher.app.stego

import java.nio.charset.StandardCharsets

/**
 * Steganography Engine for Emojis and Natural Cover Conversations.
 *
 * Conceals cryptographic payloads invisibly within natural sentences and emojis
 * using zero-width Unicode carrier channels. The resulting message looks and behaves
 * 100% like ordinary, innocent conversation to any casual observer, SMS carrier, or messaging client.
 */
object EmojiStegoEngine {

    // Invisible Unicode Bit Carriers
    private const val ZW_BIT0 = '\u200B' // Zero-width space (bit 0)
    private const val ZW_BIT1 = '\u200C' // Zero-width non-joiner (bit 1)
    private const val ZW_HEADER = "\u200D\u200D\u200C" // Stego Header Sentinel
    private const val ZW_FOOTER = "\u200D\u200D\u200B" // Stego Footer Sentinel

    data class DecoyTopic(
        val category: String,
        val icon: String,
        val sampleMessages: List<String>
    )

    val DECOY_TOPICS = listOf(
        DecoyTopic(
            category = "Coffee & Casual",
            icon = "☕️",
            sampleMessages = listOf(
                "Hey, are you free for a quick coffee this afternoon? ☕️ Let me know!",
                "Just picked up iced lattes for everyone! Heading back to the office now ☕️🧊",
                "Morning! Hope you have an awesome day ahead ✨☀️",
                "Did you catch the game last night? That ending was wild! 🏀🔥"
            )
        ),
        DecoyTopic(
            category = "Food & Lunch",
            icon = "🍕",
            sampleMessages = listOf(
                "Thinking of grabbing lunch around 12:30. Want to hit that taco spot? 🌮😋",
                "Dinner reservations are confirmed for 7:30 tonight! 🍕🍷",
                "Have you tried that bakery on 5th? Their fresh croissants are incredible 🥐☕️",
                "Cooking a big batch of pasta tonight, come over if you're around! 🍝✨"
            )
        ),
        DecoyTopic(
            category = "Work & Sync",
            icon = "💼",
            sampleMessages = listOf(
                "Just submitted the updated project report. Take a look when you have a second! 📑👍",
                "Quick reminder about our sync call at 2 PM today 💻📞",
                "The client presentation went really well! Thanks for your notes 🚀🎉",
                "Almost done with the sprint review tasks for this week! 📊✅"
            )
        ),
        DecoyTopic(
            category = "Weekend & Outing",
            icon = "🏔️",
            sampleMessages = listOf(
                "Heading out for a hike while the weather is still great! 🌲🏔️",
                "Any plans for the weekend? Thinking of cycling in the park 🚴‍♂️🌤️",
                "That movie last night was so much better than I expected! 🍿🎬",
                "Packing up for the weekend trip, super excited to get away 🏖️✈️"
            )
        )
    )

    /**
     * Embeds a secret payload invisibly into cover text with emojis.
     */
    fun embedInCoverText(coverText: String, secretPayload: String): String {
        if (secretPayload.isEmpty()) return coverText

        val payloadBytes = secretPayload.toByteArray(StandardCharsets.UTF_8)
        val bitBuffer = StringBuilder()

        for (b in payloadBytes) {
            val byteVal = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                val bit = (byteVal ushr i) and 1
                bitBuffer.append(if (bit == 0) ZW_BIT0 else ZW_BIT1)
            }
        }

        val stegoBlock = ZW_HEADER + bitBuffer.toString() + ZW_FOOTER

        val baseCover = if (coverText.isBlank()) {
            "Hey! Hope you are having a wonderful day ✨☕️"
        } else {
            coverText.trim()
        }

        // Find position of the first emoji or append to end
        val emojiRegex = Regex("[\uD83C-\uDBFF\uDC00-\uDFFF\u2600-\u26FF\u2700-\u27BF]")
        val match = emojiRegex.find(baseCover)

        return if (match != null) {
            // Embed right after the first emoji
            val insertIdx = match.range.last + 1
            baseCover.substring(0, insertIdx) + stegoBlock + baseCover.substring(insertIdx)
        } else {
            // Append with an emoji
            "$baseCover ✨$stegoBlock"
        }
    }

    /**
     * Extracts a hidden secret payload from text with zero-width steganography.
     * Returns Pair(hasStegoFound, secretPayloadOrNull).
     */
    fun extractFromCoverText(text: String): Pair<Boolean, String?> {
        val startIdx = text.indexOf(ZW_HEADER)
        val endIdx = text.indexOf(ZW_FOOTER, startIndex = if (startIdx >= 0) startIdx + ZW_HEADER.length else 0)

        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return Pair(false, null)
        }

        val rawBits = text.substring(startIdx + ZW_HEADER.length, endIdx)
        val byteList = ArrayList<Byte>()
        var currentByte = 0
        var bitCount = 0

        for (ch in rawBits) {
            if (ch == ZW_BIT0) {
                currentByte = (currentByte shl 1)
                bitCount++
            } else if (ch == ZW_BIT1) {
                currentByte = (currentByte shl 1) or 1
                bitCount++
            }

            if (bitCount == 8) {
                byteList.add(currentByte.toByte())
                currentByte = 0
                bitCount = 0
            }
        }

        if (byteList.isEmpty()) return Pair(false, null)

        return try {
            val decoded = String(byteList.toByteArray(), StandardCharsets.UTF_8)
            Pair(true, decoded)
        } catch (e: Exception) {
            Pair(false, null)
        }
    }

    /**
     * Strips any zero-width steganography characters from text to return clean visible cover text.
     */
    fun cleanVisibleText(text: String): String {
        return text
            .replace(ZW_HEADER, "")
            .replace(ZW_FOOTER, "")
            .replace(ZW_BIT0.toString(), "")
            .replace(ZW_BIT1.toString(), "")
            .trim()
    }
}
