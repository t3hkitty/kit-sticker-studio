package com.kitstickerstudio.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Types of lightweight overlay modifiers that can be attached to emoji faces or stickers.
 */
enum class ModifierType(val displayName: String, val chipLabel: String) {
    NONE("None", "None ∅"),
    CAT_EARS("Cat Ears", "Cat Ears 🐱"),
    DEVIL_HORNS("Devil Horns", "Devil Horns 😈"),
    KAWAII_BLUSH("Blush", "Blush 🌸"),
    SPARKLES("Sparkles", "Sparkles ✨")
}

/**
 * Anchor position relative to the base emoji bounds (normalized 0.0 .. 1.0).
 */
enum class ModifierAnchor {
    TOP_HEAD,       // Ears, horns
    CHEEKS,         // Blush
    FOREHEAD,       // Halo / accessory
    CORNER_SPARKLE  // Ambient sparkles
}

/**
 * ModifierLayer definition with proportional scaling, offsets, and mirroring/flip support.
 */
data class ModifierLayer(
    val type: ModifierType,
    val scale: Float = 1.0f,
    val offsetXRatio: Float = 0.0f,   // Proportional horizontal offset (-0.5 .. 0.5)
    val offsetYRatio: Float = 0.0f,   // Proportional vertical offset (-0.5 .. 0.5)
    val isFlippedHorizontal: Boolean = false,
    val anchor: ModifierAnchor = when (type) {
        ModifierType.CAT_EARS, ModifierType.DEVIL_HORNS -> ModifierAnchor.TOP_HEAD
        ModifierType.KAWAII_BLUSH -> ModifierAnchor.CHEEKS
        ModifierType.SPARKLES -> ModifierAnchor.CORNER_SPARKLE
        ModifierType.NONE -> ModifierAnchor.TOP_HEAD
    }
) {
    /**
     * Calculates the bounding rect for this modifier relative to target dimensions [width] x [height].
     */
    fun calculateBounds(width: Int, height: Int): ModifierBounds {
        val w = width.toFloat()
        val h = height.toFloat()

        return when (type) {
            ModifierType.CAT_EARS -> {
                val earWidth = w * 0.90f * scale
                val earHeight = h * 0.35f * scale
                val left = (w - earWidth) / 2f + (offsetXRatio * w)
                val top = -h * 0.10f + (offsetYRatio * h)
                ModifierBounds(left, top, left + earWidth, top + earHeight)
            }
            ModifierType.DEVIL_HORNS -> {
                val hornWidth = w * 0.75f * scale
                val hornHeight = h * 0.32f * scale
                val left = (w - hornWidth) / 2f + (offsetXRatio * w)
                val top = -h * 0.12f + (offsetYRatio * h)
                ModifierBounds(left, top, left + hornWidth, top + hornHeight)
            }
            ModifierType.KAWAII_BLUSH -> {
                val blushWidth = w * 0.85f * scale
                val blushHeight = h * 0.22f * scale
                val left = (w - blushWidth) / 2f + (offsetXRatio * w)
                val top = h * 0.52f + (offsetYRatio * h)
                ModifierBounds(left, top, left + blushWidth, top + blushHeight)
            }
            ModifierType.SPARKLES -> {
                val sparkleSize = w * 0.95f * scale
                val left = (w - sparkleSize) / 2f + (offsetXRatio * w)
                val top = (h - sparkleSize) / 2f + (offsetYRatio * h)
                ModifierBounds(left, top, left + sparkleSize, top + sparkleSize)
            }
            ModifierType.NONE -> ModifierBounds(0f, 0f, 0f, 0f)
        }
    }
}

/**
 * ModifierOverlayRenderer: Procedurally renders crisp Kawaiian vector overlays
 * (Cat Ears, Devil Horns, Blush, Sparkles) directly onto emoji/sticker canvases.
 */
object ModifierOverlayRenderer {

    /**
     * Renders a list of modifier layers onto the base [source] bitmap.
     * Preserves alpha channel and cleans canvas states.
     */
    fun renderModifiers(source: Bitmap, modifiers: List<ModifierLayer>): Bitmap {
        val activeModifiers = modifiers.filter { it.type != ModifierType.NONE }
        if (activeModifiers.isEmpty()) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original base first
        canvas.drawBitmap(source, 0f, 0f, null)

        for (modifier in activeModifiers) {
            renderSingleModifier(canvas, source.width, source.height, modifier)
        }

        return result
    }

    /**
     * Renders into a standardized square dimension (512x512 for sticker WebP/icons).
     */
    fun renderToSquare(source: Bitmap, targetSize: Int = 512, modifiers: List<ModifierLayer>): Bitmap {
        val squareBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(squareBitmap)

        // Scale and center base bitmap within 80% safe zone to accommodate ears/horns
        val safeMargin = targetSize * 0.12f
        val innerSize = targetSize - (safeMargin * 2f)

        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()
        val scale = minOf(innerSize / srcW, innerSize / srcH)
        val destW = srcW * scale
        val destH = srcH * scale
        val destLeft = (targetSize - destW) / 2f
        val destTop = safeMargin + (innerSize - destH) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(destLeft, destTop)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        // Now render modifiers aligned with the centered inner content
        canvas.save()
        canvas.translate(destLeft, destTop)
        for (modifier in modifiers.filter { it.type != ModifierType.NONE }) {
            renderSingleModifier(canvas, destW.toInt(), destH.toInt(), modifier)
        }
        canvas.restore()

        return squareBitmap
    }

    private fun renderSingleModifier(canvas: Canvas, w: Int, h: Int, layer: ModifierLayer) {
        val bounds = layer.calculateBounds(w, h)
        if (bounds.isEmpty) return

        canvas.save()

        // Handle horizontal flipping if enabled
        if (layer.isFlippedHorizontal) {
            canvas.scale(-1.0f, 1.0f, bounds.centerX, bounds.centerY)
        }

        when (layer.type) {
            ModifierType.CAT_EARS -> drawCatEars(canvas, bounds)
            ModifierType.DEVIL_HORNS -> drawDevilHorns(canvas, bounds)
            ModifierType.KAWAII_BLUSH -> drawKawaiiBlush(canvas, bounds)
            ModifierType.SPARKLES -> drawSparkles(canvas, bounds)
            ModifierType.NONE -> { /* No-op */ }
        }

        canvas.restore()
    }

    /**
     * Draws Kawaiian Cat Ears with plush lavender/pink inner shading and bold outline.
     */
    private fun drawCatEars(canvas: Canvas, bounds: ModifierBounds) {
        val earWidth = bounds.width * 0.38f
        val earHeight = bounds.height

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1E2E") // Dark Slate outline
            style = Paint.Style.STROKE
            strokeWidth = maxOf(3f, bounds.width * 0.035f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E1A47") // Deep Indigo Kawaiian Ear
            style = Paint.Style.FILL
        }

        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB7C5") // Soft Kawaii Pink inside
            style = Paint.Style.FILL
        }

        // Left Cat Ear
        val leftEar = Path().apply {
            moveTo(bounds.left, bounds.bottom)
            lineTo(bounds.left + earWidth * 0.55f, bounds.top)
            lineTo(bounds.left + earWidth, bounds.bottom * 0.85f)
            close()
        }
        canvas.drawPath(leftEar, outerPaint)
        canvas.drawPath(leftEar, strokePaint)

        // Left Inner Ear
        val leftInner = Path().apply {
            moveTo(bounds.left + earWidth * 0.15f, bounds.bottom * 0.95f)
            lineTo(bounds.left + earWidth * 0.55f, bounds.top + earHeight * 0.25f)
            lineTo(bounds.left + earWidth * 0.85f, bounds.bottom * 0.85f)
            close()
        }
        canvas.drawPath(leftInner, innerPaint)

        // Right Cat Ear
        val rightLeft = bounds.right - earWidth
        val rightEar = Path().apply {
            moveTo(rightLeft, bounds.bottom * 0.85f)
            lineTo(rightLeft + earWidth * 0.45f, bounds.top)
            lineTo(bounds.right, bounds.bottom)
            close()
        }
        canvas.drawPath(rightEar, outerPaint)
        canvas.drawPath(rightEar, strokePaint)

        // Right Inner Ear
        val rightInner = Path().apply {
            moveTo(rightLeft + earWidth * 0.15f, bounds.bottom * 0.85f)
            lineTo(rightLeft + earWidth * 0.45f, bounds.top + earHeight * 0.25f)
            lineTo(bounds.right - earWidth * 0.15f, bounds.bottom * 0.95f)
            close()
        }
        canvas.drawPath(rightInner, innerPaint)
    }

    /**
     * Draws Devil Horns with curved demon silhouette and crimson/ruby fill.
     */
    private fun drawDevilHorns(canvas: Canvas, bounds: ModifierBounds) {
        val hornWidth = bounds.width * 0.32f
        val hornHeight = bounds.height

        val hornPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D32F2F") // Devil Crimson Red
            style = Paint.Style.FILL
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A0000") // Deep blood dark border
            style = Paint.Style.STROKE
            strokeWidth = maxOf(3f, bounds.width * 0.035f)
            strokeJoin = Paint.Join.ROUND
        }

        // Left Horn (curving outward and up)
        val leftPath = Path().apply {
            moveTo(bounds.left + hornWidth * 0.8f, bounds.bottom)
            quadTo(bounds.left + hornWidth * 0.5f, bounds.top + hornHeight * 0.4f, bounds.left, bounds.top)
            quadTo(bounds.left + hornWidth * 0.2f, bounds.bottom * 0.8f, bounds.left + hornWidth * 0.8f, bounds.bottom)
            close()
        }
        canvas.drawPath(leftPath, hornPaint)
        canvas.drawPath(leftPath, strokePaint)

        // Right Horn (curving outward and up)
        val rightPath = Path().apply {
            val rLeft = bounds.right - hornWidth
            moveTo(rLeft + hornWidth * 0.2f, bounds.bottom)
            quadTo(rLeft + hornWidth * 0.5f, bounds.top + hornHeight * 0.4f, bounds.right, bounds.top)
            quadTo(rLeft + hornWidth * 0.8f, bounds.bottom * 0.8f, rLeft + hornWidth * 0.2f, bounds.bottom)
            close()
        }
        canvas.drawPath(rightPath, hornPaint)
        canvas.drawPath(rightPath, strokePaint)
    }

    /**
     * Draws soft Kawaii Cheek Blush ovals with light transparency.
     */
    private fun drawKawaiiBlush(canvas: Canvas, bounds: ModifierBounds) {
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FF69B4") // Semi-transparent Hot Pink
            style = Paint.Style.FILL
        }

        val cheekW = bounds.width * 0.22f
        val cheekH = bounds.height * 0.65f

        // Left Cheek
        val leftRect = RectF(bounds.left, bounds.top, bounds.left + cheekW, bounds.top + cheekH)
        canvas.drawOval(leftRect, blushPaint)

        // Right Cheek
        val rightRect = RectF(bounds.right - cheekW, bounds.top, bounds.right, bounds.top + cheekH)
        canvas.drawOval(rightRect, blushPaint)
    }

    /**
     * Draws sparkly Kawaii 4-pointed stars around emoji corners.
     */
    private fun drawSparkles(canvas: Canvas, bounds: ModifierBounds) {
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFBEB") // Amber Gold sparkle
            style = Paint.Style.FILL
        }

        drawFourPointStar(canvas, bounds.right - bounds.width * 0.15f, bounds.top + bounds.height * 0.15f, bounds.width * 0.12f, starPaint)
        drawFourPointStar(canvas, bounds.left + bounds.width * 0.15f, bounds.bottom - bounds.height * 0.20f, bounds.width * 0.08f, starPaint)
    }

    private fun drawFourPointStar(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            quadTo(cx, cy, cx + size, cy)
            quadTo(cx, cy, cx, cy + size)
            quadTo(cx, cy, cx - size, cy)
            quadTo(cx, cy, cx, cy - size)
            close()
        }
        canvas.drawPath(path, paint)
    }
}
