package com.kitstickerstudio.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Supported base preset types for emoji and sticker transformations.
 */
enum class BasePresetType(val displayName: String, val chipLabel: String) {
    ORIGINAL("Original", "Original ✨"),
    GREEN_FACE("Green Face", "Green 🟢"),
    PURPLE_FACE("Purple Face", "Purple 💜"),
    KAWAII_TINT("Kawaii Tint", "Kawaii 🌸"),
    HIGH_CONTRAST("High Contrast", "Hi-Contrast ⚡")
}

/**
 * Base preset model holding type and transformation intensity (0.0f .. 1.0f).
 */
data class BasePreset(
    val type: BasePresetType = BasePresetType.ORIGINAL,
    val intensity: Float = 1.0f
) {
    init {
        require(intensity in 0.0f..1.0f) { "Preset intensity must be between 0.0 and 1.0" }
    }
}

/**
 * BasePresetEngine executes pixel-level color transformations while preserving alpha.
 * Supports Green Face, Purple Face, Kawaii Tint, and High-Contrast presets.
 */
object BasePresetEngine {

    /**
     * Applies a base preset transformation to a bitmap.
     * Returns a new transformed bitmap or the original copy if preset is ORIGINAL or intensity <= 0f.
     */
    fun applyPreset(source: Bitmap, preset: BasePreset): Bitmap {
        if (preset.type == BasePresetType.ORIGINAL || preset.intensity <= 0f) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val intensity = preset.intensity.coerceIn(0.0f, 1.0f)
        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val color = pixels[i]
            val alpha = Color.alpha(color)
            if (alpha == 0) continue // Preserve transparent background pixels cleanly

            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            when (preset.type) {
                BasePresetType.GREEN_FACE -> {
                    // Shift yellow/warm face tones towards green
                    Color.colorToHSV(color, hsv)
                    val originalHue = hsv[0]
                    if (originalHue in 20f..75f) {
                        val targetHue = 110f // Vibrant soft green
                        hsv[0] = lerp(originalHue, targetHue, intensity)
                        pixels[i] = Color.HSVToColor(alpha, hsv)
                    } else {
                        val targetG = min(255, (g * (1.0f + 0.5f * intensity)).toInt())
                        val targetR = (r * (1.0f - 0.25f * intensity)).toInt()
                        pixels[i] = Color.argb(alpha, targetR, targetG, b)
                    }
                }

                BasePresetType.PURPLE_FACE -> {
                    // Shift tones towards Kawaiian Lavender/Purple (#2E1A47 / #9C27B0)
                    Color.colorToHSV(color, hsv)
                    val originalHue = hsv[0]
                    val targetHue = 280f
                    hsv[0] = lerp(originalHue, targetHue, intensity * 0.85f)
                    hsv[1] = min(1.0f, hsv[1] * (1.0f + 0.2f * intensity))
                    pixels[i] = Color.HSVToColor(alpha, hsv)
                }

                BasePresetType.KAWAII_TINT -> {
                    // Soft pastel pink/lavender warmth with elevated brightness
                    val targetR = min(255, (r * (1.0f + 0.25f * intensity) + 30f * intensity).toInt())
                    val targetG = (g * (1.0f - 0.10f * intensity) + 10f * intensity).toInt().coerceIn(0, 255)
                    val targetB = min(255, (b * (1.0f + 0.35f * intensity) + 35f * intensity).toInt())
                    pixels[i] = Color.argb(alpha, targetR, targetG, targetB)
                }

                BasePresetType.HIGH_CONTRAST -> {
                    // High-contrast punch with sharp black/white inflection
                    val factor = (259f * (intensity * 128f + 255f)) / (255f * (259f - intensity * 128f))
                    val newR = clampColor((factor * (r - 128) + 128).toInt())
                    val newG = clampColor((factor * (g - 128) + 128).toInt())
                    val newB = clampColor((factor * (b - 128) + 128).toInt())
                    pixels[i] = Color.argb(alpha, newR, newG, newB)
                }

                BasePresetType.ORIGINAL -> {
                    // No-op
                }
            }
        }

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }

    /**
     * Applies a preset across an entire batch/pack of bitmaps.
     */
    fun applyPresetToPack(sources: List<Bitmap>, preset: BasePreset): List<Bitmap> {
        return sources.map { applyPreset(it, preset) }
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount.coerceIn(0.0f, 1.0f)
    }

    private fun clampColor(value: Int): Int {
        return max(0, min(255, value))
    }
}
