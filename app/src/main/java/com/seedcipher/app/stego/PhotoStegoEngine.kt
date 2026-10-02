package com.seedcipher.app.stego

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * High-performance Least Significant Bit (LSB) Photo Steganography Engine.
 *
 * Embeds secret cryptographic payloads (or plain text) into the RGB channels
 * of photo bitmaps with zero perceptible visual distortion.
 *
 * Capacity: Each pixel encodes 3 bits (1 bit per R, G, B channel).
 * A 400x400 photo holds up to ~60,000 bytes.
 * Format: Saved losslessly as PNG to preserve LSB bits intact.
 */
object PhotoStegoEngine {

    // 4-byte Magic Marker "STG1"
    private val MAGIC_BYTES = byteArrayOf(0x53, 0x54, 0x47, 0x31)

    /**
     * Embeds secret payload into the RGB channels of a bitmap.
     * Returns a new mutable Bitmap containing the hidden data.
     */
    fun embedSecretInBitmap(sourceBitmap: Bitmap, secretPayload: String): Bitmap {
        val payloadBytes = secretPayload.toByteArray(StandardCharsets.UTF_8)
        val totalBytes = 4 + 4 + payloadBytes.size // Magic (4) + Length (4) + Payload
        val totalBits = totalBytes * 8

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val totalPixels = width * height

        require(totalPixels * 3 >= totalBits) {
            "Image is too small for this secret payload! Need at least ${(totalBits + 2) / 3} pixels, but image has $totalPixels."
        }

        // Create ARGB_8888 copy
        val mutableBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val buffer = ByteBuffer.allocate(totalBytes)
        buffer.put(MAGIC_BYTES)
        buffer.putInt(payloadBytes.size)
        buffer.put(payloadBytes)
        val dataBytes = buffer.array()

        var bitIndex = 0

        pixelLoop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= totalBits) break@pixelLoop

                val pixel = mutableBitmap.getPixel(x, y)
                val a = (pixel ushr 24) and 0xFF
                var r = (pixel ushr 16) and 0xFF
                var g = (pixel ushr 8) and 0xFF
                var b = pixel and 0xFF

                // Channel 1: Red LSB
                if (bitIndex < totalBits) {
                    val bytePos = bitIndex / 8
                    val bitPos = 7 - (bitIndex % 8)
                    val bit = (dataBytes[bytePos].toInt() ushr bitPos) and 1
                    r = (r and 0xFE) or bit
                    bitIndex++
                }

                // Channel 2: Green LSB
                if (bitIndex < totalBits) {
                    val bytePos = bitIndex / 8
                    val bitPos = 7 - (bitIndex % 8)
                    val bit = (dataBytes[bytePos].toInt() ushr bitPos) and 1
                    g = (g and 0xFE) or bit
                    bitIndex++
                }

                // Channel 3: Blue LSB
                if (bitIndex < totalBits) {
                    val bytePos = bitIndex / 8
                    val bitPos = 7 - (bitIndex % 8)
                    val bit = (dataBytes[bytePos].toInt() ushr bitPos) and 1
                    b = (b and 0xFE) or bit
                    bitIndex++
                }

                val newPixel = (a shl 24) or (r shl 16) or (g shl 8) or b
                mutableBitmap.setPixel(x, y, newPixel)
            }
        }

        return mutableBitmap
    }

    /**
     * Extracts a hidden secret payload from a bitmap if present.
     * Returns null if no valid steganography signature is found.
     */
    fun extractSecretFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        if (totalPixels * 3 < 64) return null // Must at least fit 64-bit header

        // 1. Read first 64 bits for Header (4 bytes Magic + 4 bytes Length)
        val headerBits = IntArray(64)
        var bitIndex = 0

        headerLoop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= 64) break@headerLoop
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                headerBits[bitIndex++] = r and 1
                if (bitIndex < 64) headerBits[bitIndex++] = g and 1
                if (bitIndex < 64) headerBits[bitIndex++] = b and 1
            }
        }

        val headerBytes = ByteArray(8)
        for (i in 0 until 8) {
            var b = 0
            for (j in 0 until 8) {
                b = (b shl 1) or headerBits[i * 8 + j]
            }
            headerBytes[i] = b.toByte()
        }

        // Verify Magic Marker "STG1"
        if (!headerBytes.sliceArray(0..3).contentEquals(MAGIC_BYTES)) {
            return null
        }

        // Read payload length
        val payloadLength = ByteBuffer.wrap(headerBytes, 4, 4).int
        if (payloadLength <= 0 || payloadLength > 1_000_000) {
            return null
        }

        val totalBitsNeeded = (8 + payloadLength) * 8
        if (totalPixels * 3 < totalBitsNeeded) {
            return null
        }

        // 2. Extract payload bytes
        val payloadBytes = ByteArray(payloadLength)
        var pByteIndex = 0
        var currentByte = 0
        var currentBitInByte = 0
        var totalProcessedBits = 0

        extractLoop@ for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                val channels = intArrayOf(r and 1, g and 1, b and 1)
                for (bit in channels) {
                    if (totalProcessedBits < 64) {
                        totalProcessedBits++
                        continue
                    }
                    if (pByteIndex >= payloadLength) {
                        break@extractLoop
                    }

                    currentByte = (currentByte shl 1) or bit
                    currentBitInByte++

                    if (currentBitInByte == 8) {
                        payloadBytes[pByteIndex++] = currentByte.toByte()
                        currentByte = 0
                        currentBitInByte = 0
                    }
                }
            }
        }

        return try {
            String(payloadBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves a bitmap losslessly as a PNG to app cache storage.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, prefix: String = "stego_img"): File {
        val dir = File(context.cacheDir, "stego_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /**
     * Loads a bitmap safely from a File or Uri.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmapFromFile(file: File): Bitmap? {
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------------
    // HIGH-QUALITY PROCEDURAL REALISTIC DECOY PHOTOS
    // ------------------------------------------------------------------------
    enum class DecoyScene(val label: String, val coverCaption: String) {
        COFFEE_CUP("Café Latte Art ☕️", "Grabbing a quick coffee before my next meeting! ☕️✨"),
        MOUNTAIN_SUNSET("Mountain Trail 🏔️", "Sunset on the hiking trail yesterday was breathtaking 🏔️🌅"),
        WORK_DESK("Modern Desk 💻", "Finishing up the updated deck at my desk 💻📑"),
        PIZZA_SLICE("Artisan Pizza 🍕", "Made homemade artisan pizza tonight! 🍕😋")
    }

    /**
     * Generates a crisp, visually appealing 480x480 photo-like canvas bitmap
     * representing a natural everyday photo scene (Coffee, Hiking, Work, Food).
     */
    fun generateDecoyBitmap(scene: DecoyScene, width: Int = 480, height: Int = 480): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (scene) {
            DecoyScene.COFFEE_CUP -> drawCoffeeScene(canvas, width, height, paint)
            DecoyScene.MOUNTAIN_SUNSET -> drawMountainSunsetScene(canvas, width, height, paint)
            DecoyScene.WORK_DESK -> drawWorkDeskScene(canvas, width, height, paint)
            DecoyScene.PIZZA_SLICE -> drawPizzaScene(canvas, width, height, paint)
        }

        return bitmap
    }

    private fun drawCoffeeScene(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Wooden tabletop background
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 
            intArrayOf(Color.rgb(112, 70, 42), Color.rgb(78, 48, 28)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Wood grain planks
        paint.color = Color.argb(40, 0, 0, 0)
        paint.strokeWidth = 3f
        canvas.drawLine(0f, h * 0.33f, w.toFloat(), h * 0.33f, paint)
        canvas.drawLine(0f, h * 0.67f, w.toFloat(), h * 0.67f, paint)

        // Saucer shadow & plate
        paint.color = Color.argb(80, 0, 0, 0)
        canvas.drawOval(RectF(w * 0.12f, h * 0.22f, w * 0.88f, h * 0.88f), paint)
        paint.color = Color.rgb(235, 235, 235)
        canvas.drawOval(RectF(w * 0.15f, h * 0.20f, w * 0.85f, h * 0.84f), paint)

        // Saucer rim highlight
        paint.color = Color.rgb(215, 215, 215)
        canvas.drawOval(RectF(w * 0.22f, h * 0.26f, w * 0.78f, h * 0.78f), paint)

        // Ceramic cup
        paint.color = Color.rgb(250, 250, 250)
        canvas.drawCircle(w * 0.5f, h * 0.52f, w * 0.25f, paint)

        // Rich Espresso layer
        paint.color = Color.rgb(65, 38, 22)
        canvas.drawCircle(w * 0.5f, h * 0.52f, w * 0.22f, paint)

        // Crema ring
        paint.color = Color.rgb(180, 120, 70)
        canvas.drawCircle(w * 0.5f, h * 0.52f, w * 0.20f, paint)

        // Latte Art Heart Foam
        paint.color = Color.rgb(255, 250, 240)
        val heartPath = Path()
        val cx = w * 0.5f
        val cy = h * 0.51f
        heartPath.moveTo(cx, cy - 25f)
        heartPath.cubicTo(cx - 55f, cy - 70f, cx - 80f, cy + 10f, cx, cy + 60f)
        heartPath.cubicTo(cx + 80f, cy + 10f, cx + 55f, cy - 70f, cx, cy - 25f)
        canvas.drawPath(heartPath, paint)

        // Latte art center swirl tip
        paint.color = Color.rgb(200, 140, 90)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, cy - 40f, cx, cy + 45f, paint)
        paint.style = Paint.Style.FILL

        // Steam vapor swirls
        paint.color = Color.argb(45, 255, 255, 255)
        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE
        val steam1 = Path()
        steam1.moveTo(cx - 30f, cy - 100f)
        steam1.quadTo(cx - 50f, cy - 140f, cx - 25f, cy - 180f)
        canvas.drawPath(steam1, paint)
        val steam2 = Path()
        steam2.moveTo(cx + 25f, cy - 90f)
        steam2.quadTo(cx + 50f, cy - 130f, cx + 30f, cy - 170f)
        canvas.drawPath(steam2, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawMountainSunsetScene(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Sunset gradient sky (Deep Purple -> Sunset Orange -> Golden Yellow)
        paint.shader = LinearGradient(0f, 0f, 0f, h * 0.7f,
            intArrayOf(Color.rgb(44, 24, 76), Color.rgb(180, 52, 70), Color.rgb(245, 158, 40), Color.rgb(255, 220, 110)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Glowing Sun disc
        paint.color = Color.rgb(255, 248, 190)
        canvas.drawCircle(w * 0.65f, h * 0.42f, 45f, paint)
        paint.color = Color.argb(60, 255, 240, 150)
        canvas.drawCircle(w * 0.65f, h * 0.42f, 75f, paint)

        // Distant mountain range (purple silhouette)
        paint.color = Color.rgb(72, 45, 95)
        val mtnFar = Path()
        mtnFar.moveTo(0f, h * 0.62f)
        mtnFar.lineTo(w * 0.28f, h * 0.45f)
        mtnFar.lineTo(w * 0.58f, h * 0.55f)
        mtnFar.lineTo(w * 0.85f, h * 0.41f)
        mtnFar.lineTo(w.toFloat(), h * 0.52f)
        mtnFar.lineTo(w.toFloat(), h.toFloat())
        mtnFar.lineTo(0f, h.toFloat())
        canvas.drawPath(mtnFar, paint)

        // Middle mountain ridge (deep navy)
        paint.color = Color.rgb(40, 32, 65)
        val mtnMid = Path()
        mtnMid.moveTo(0f, h * 0.68f)
        mtnMid.lineTo(w * 0.38f, h * 0.52f)
        mtnMid.lineTo(w * 0.72f, h * 0.64f)
        mtnMid.lineTo(w.toFloat(), h * 0.58f)
        mtnMid.lineTo(w.toFloat(), h.toFloat())
        mtnMid.lineTo(0f, h.toFloat())
        canvas.drawPath(mtnMid, paint)

        // Foreground pine ridge & trail (near black / dark forest)
        paint.color = Color.rgb(20, 22, 34)
        val fgRidge = Path()
        fgRidge.moveTo(0f, h * 0.80f)
        fgRidge.lineTo(w * 0.45f, h * 0.72f)
        fgRidge.lineTo(w.toFloat(), h * 0.78f)
        fgRidge.lineTo(w.toFloat(), h.toFloat())
        fgRidge.lineTo(0f, h.toFloat())
        canvas.drawPath(fgRidge, paint)

        // Pine trees silhouette along foreground
        val treePoints = floatArrayOf(0.08f, 0.16f, 0.26f, 0.34f, 0.78f, 0.88f)
        for (tp in treePoints) {
            val tx = w * tp
            val ty = h * 0.76f
            val tree = Path()
            tree.moveTo(tx, ty - 45f)
            tree.lineTo(tx - 18f, ty)
            tree.lineTo(tx + 18f, ty)
            tree.close()
            canvas.drawPath(tree, paint)
        }
    }

    private fun drawWorkDeskScene(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Clean Scandinavian office desk surface (light birch wood)
        paint.color = Color.rgb(228, 222, 210)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Wall background at top
        paint.color = Color.rgb(240, 243, 246)
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.25f, paint)
        paint.color = Color.rgb(180, 185, 192)
        canvas.drawLine(0f, h * 0.25f, w.toFloat(), h * 0.25f, paint)

        // Laptop base
        paint.color = Color.rgb(160, 165, 172)
        val laptopRect = RectF(w * 0.22f, h * 0.42f, w * 0.78f, h * 0.82f)
        canvas.drawRoundRect(laptopRect, 16f, 16f, paint)

        // Laptop keyboard area
        paint.color = Color.rgb(40, 44, 52)
        val kbRect = RectF(w * 0.26f, h * 0.46f, w * 0.74f, h * 0.68f)
        canvas.drawRoundRect(kbRect, 8f, 8f, paint)

        // Trackpad
        paint.color = Color.rgb(140, 145, 152)
        val tpRect = RectF(w * 0.42f, h * 0.71f, w * 0.58f, h * 0.79f)
        canvas.drawRoundRect(tpRect, 4f, 4f, paint)

        // Succulent potted plant on right
        paint.color = Color.rgb(185, 105, 75) // Terracotta pot
        val potRect = RectF(w * 0.82f, h * 0.28f, w * 0.94f, h * 0.42f)
        canvas.drawRoundRect(potRect, 6f, 6f, paint)
        paint.color = Color.rgb(55, 140, 80) // Green succulent leaf
        canvas.drawCircle(w * 0.88f, h * 0.27f, 22f, paint)
        canvas.drawCircle(w * 0.85f, h * 0.25f, 16f, paint)
        canvas.drawCircle(w * 0.91f, h * 0.25f, 16f, paint)

        // Notebook with pencil on left
        paint.color = Color.rgb(60, 90, 135)
        val bookRect = RectF(w * 0.05f, h * 0.40f, w * 0.18f, h * 0.75f)
        canvas.drawRoundRect(bookRect, 8f, 8f, paint)
    }

    private fun drawPizzaScene(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        // Dark slate board background
        paint.color = Color.rgb(38, 42, 46)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Round pizza crust
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = w * 0.42f

        paint.color = Color.rgb(205, 145, 75) // Golden baked crust
        canvas.drawCircle(cx, cy, r, paint)

        // Rich red tomato sauce & bubbling golden mozzarella
        paint.color = Color.rgb(248, 208, 92)
        canvas.drawCircle(cx, cy, r - 22f, paint)

        // Pepperoni slices
        paint.color = Color.rgb(188, 44, 38)
        val peps = arrayOf(
            Pair(cx - 70f, cy - 60f),
            Pair(cx + 65f, cy - 75f),
            Pair(cx + 80f, cy + 45f),
            Pair(cx - 60f, cy + 70f),
            Pair(cx + 10f, cy + 90f),
            Pair(cx - 20f, cy - 100f),
            Pair(cx, cy)
        )
        for (p in peps) {
            canvas.drawCircle(p.first, p.second, 26f, paint)
        }

        // Fresh green basil leaves
        paint.color = Color.rgb(46, 125, 50)
        val basils = arrayOf(
            Pair(cx + 35f, cy - 30f),
            Pair(cx - 35f, cy + 20f),
            Pair(cx + 30f, cy + 45f)
        )
        for (b in basils) {
            canvas.drawOval(RectF(b.first - 14f, b.second - 8f, b.first + 14f, b.second + 8f), paint)
        }
    }
}
