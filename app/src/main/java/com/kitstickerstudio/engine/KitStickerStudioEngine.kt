package com.kitstickerstudio.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Standard export formats for stickers.
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG(".png", "image/png"),
    WEBP_STICKER(".webp", "image/webp"),
    GIF_ANIMATED(".gif", "image/gif")
}

/**
 * KitStickerStudioEngine: Coordinates presets, modifiers, animated frames, and pack generation.
 */
class KitStickerStudioEngine {

    companion object {
        const val STANDARD_STICKER_SIZE = 512
        const val THUMBNAIL_SIZE = 128

        val PACK_EXPRESSIONS = listOf(
            "Happy 😊",
            "Cat Wink 😸",
            "Love Heart 😻",
            "Sparkle Ready ✨",
            "Silly 🤪",
            "Party 🎉"
        )
    }

    /**
     * Converts a raw Unicode emoji character (e.g. "🐱", "😊", "😈") to a high-resolution Bitmap.
     */
    fun createEmojiBaseBitmap(emoji: String, size: Int = STANDARD_STICKER_SIZE): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.72f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }

        val xPos = size / 2f
        val yPos = (size / 2f - (paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(emoji, xPos, yPos, paint)

        return bitmap
    }

    /**
     * Processes a single emoji/sticker with both base preset and modifier layers applied.
     */
    fun processSingleEmoji(
        source: Bitmap,
        preset: BasePreset,
        modifiers: List<ModifierLayer>,
        targetSquareSize: Int = STANDARD_STICKER_SIZE
    ): Bitmap {
        val presetTransformed = BasePresetEngine.applyPreset(source, preset)
        return ModifierOverlayRenderer.renderToSquare(presetTransformed, targetSquareSize, modifiers)
    }

    /**
     * Generates an entire expression pack from a single base or emoji string.
     */
    fun generateExpressionPack(
        baseEmojis: List<String>,
        preset: BasePreset,
        modifiers: List<ModifierLayer>,
        targetSize: Int = STANDARD_STICKER_SIZE
    ): List<Bitmap> {
        return baseEmojis.map { emoji ->
            val raw = createEmojiBaseBitmap(emoji, targetSize)
            processSingleEmoji(raw, preset, modifiers, targetSize)
        }
    }

    /**
     * Generates a frame sequence for Emoji Animator loops.
     */
    fun generateAnimatedFrames(
        source: Bitmap,
        preset: BasePreset,
        modifiers: List<ModifierLayer>,
        frameCount: Int = 8,
        targetSize: Int = STANDARD_STICKER_SIZE
    ): List<Bitmap> {
        val baseTransformed = BasePresetEngine.applyPreset(source, preset)
        val frames = mutableListOf<Bitmap>()

        for (frameIndex in 0 until frameCount) {
            val progress = frameIndex.toFloat() / frameCount.toFloat()
            val bobOffset = kotlin.math.sin(progress * 2.0 * Math.PI).toFloat() * 0.04f
            val dynamicModifiers = modifiers.map { mod ->
                mod.copy(offsetYRatio = mod.offsetYRatio + bobOffset)
            }
            val frame = ModifierOverlayRenderer.renderToSquare(baseTransformed, targetSize, dynamicModifiers)
            frames.add(frame)
        }

        return frames
    }

    /**
     * Encodes a bitmap to PNG / WebP byte array.
     */
    fun exportBitmap(bitmap: Bitmap, format: ExportFormat, quality: Int = 95): ByteArray {
        val stream = ByteArrayOutputStream()
        when (format) {
            ExportFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            ExportFormat.WEBP_STICKER -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, stream)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, stream)
                }
            }
            ExportFormat.GIF_ANIMATED -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        return stream.toByteArray()
    }

    fun saveToFile(bitmap: Bitmap, destination: File, format: ExportFormat): Boolean {
        return try {
            val bytes = exportBitmap(bitmap, format)
            FileOutputStream(destination).use { fos ->
                fos.write(bytes)
                fos.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
