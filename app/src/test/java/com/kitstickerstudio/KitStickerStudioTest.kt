package com.kitstickerstudio

import com.kitstickerstudio.engine.BasePreset
import com.kitstickerstudio.engine.BasePresetType
import com.kitstickerstudio.engine.ExportFormat
import com.kitstickerstudio.engine.KitStickerStudioEngine
import com.kitstickerstudio.engine.ModifierAnchor
import com.kitstickerstudio.engine.ModifierLayer
import com.kitstickerstudio.engine.ModifierType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Standalone unit tests for KitStickerStudio:
 * 1. BasePreset transformations and intensity clamping
 * 2. ModifierLayer bounds calculation, proportional anchoring, and offsets
 * 3. ModifierLayer horizontal flip state logic
 * 4. Export format extensions and MIME types
 * 5. Expression pack definitions
 */
class KitStickerStudioTest {

    @Test
    fun testBasePresetValidationAndDefaults() {
        val originalPreset = BasePreset(BasePresetType.ORIGINAL, 1.0f)
        assertEquals(BasePresetType.ORIGINAL, originalPreset.type)
        assertEquals(1.0f, originalPreset.intensity, 0.001f)

        val greenPreset = BasePreset(BasePresetType.GREEN_FACE, 0.75f)
        assertEquals(BasePresetType.GREEN_FACE, greenPreset.type)
        assertEquals(0.75f, greenPreset.intensity, 0.001f)

        val purplePreset = BasePreset(BasePresetType.PURPLE_FACE, 0.5f)
        assertEquals(BasePresetType.PURPLE_FACE, purplePreset.type)
        assertEquals("Purple Face", purplePreset.type.displayName)

        val kawaiiPreset = BasePreset(BasePresetType.KAWAII_TINT, 0.9f)
        assertEquals("Kawaii Tint", kawaiiPreset.type.displayName)

        val hcPreset = BasePreset(BasePresetType.HIGH_CONTRAST, 1.0f)
        assertEquals("High Contrast", hcPreset.type.displayName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBasePresetIntensityUnderflowThrows() {
        BasePreset(BasePresetType.GREEN_FACE, -0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBasePresetIntensityOverflowThrows() {
        BasePreset(BasePresetType.GREEN_FACE, 1.1f)
    }

    @Test
    fun testCatEarsModifierBoundsCalculation() {
        val catEars = ModifierLayer(
            type = ModifierType.CAT_EARS,
            scale = 1.0f,
            offsetXRatio = 0.0f,
            offsetYRatio = 0.0f
        )
        val bounds = catEars.calculateBounds(512, 512)

        assertFalse("Bounds should not be empty", bounds.isEmpty)
        assertEquals(ModifierAnchor.TOP_HEAD, catEars.anchor)
        assertEquals(512f * 0.90f, bounds.width, 0.01f)
        assertTrue("Cat ears top should sit above head (< 0)", bounds.top < 0f)
    }

    @Test
    fun testDevilHornsModifierBoundsCalculation() {
        val devilHorns = ModifierLayer(
            type = ModifierType.DEVIL_HORNS,
            scale = 1.0f,
            offsetXRatio = 0.0f,
            offsetYRatio = 0.0f
        )
        val hornBounds = devilHorns.calculateBounds(512, 512)

        assertFalse("Horn bounds should not be empty", hornBounds.isEmpty)
        assertEquals(ModifierAnchor.TOP_HEAD, devilHorns.anchor)
        assertEquals(512f * 0.75f, hornBounds.width, 0.01f)

        val scaledHorns = ModifierLayer(
            type = ModifierType.DEVIL_HORNS,
            scale = 1.25f
        )
        val scaledBounds = scaledHorns.calculateBounds(512, 512)
        assertTrue("1.25x scaled horns should have larger width", scaledBounds.width > hornBounds.width)
    }

    @Test
    fun testKawaiiBlushAndSparklesAnchoring() {
        val blush = ModifierLayer(type = ModifierType.KAWAII_BLUSH)
        assertEquals(ModifierAnchor.CHEEKS, blush.anchor)
        val blushBounds = blush.calculateBounds(512, 512)
        assertEquals(512f * 0.52f, blushBounds.top, 0.01f)
        assertTrue("Blush should sit in cheeks/middle region (y >= 250)", blushBounds.top >= 250f)

        val sparkles = ModifierLayer(type = ModifierType.SPARKLES)
        assertEquals(ModifierAnchor.CORNER_SPARKLE, sparkles.anchor)
        val sparkleBounds = sparkles.calculateBounds(512, 512)
        assertFalse("Sparkle bounds should not be empty", sparkleBounds.isEmpty)

        val noneMod = ModifierLayer(type = ModifierType.NONE)
        val noneBounds = noneMod.calculateBounds(512, 512)
        assertTrue("NONE modifier bounds should be empty", noneBounds.isEmpty)
    }

    @Test
    fun testModifierHorizontalFlipAndOffsets() {
        val layerNormal = ModifierLayer(
            type = ModifierType.CAT_EARS,
            offsetXRatio = 0.1f,
            offsetYRatio = -0.05f,
            isFlippedHorizontal = false
        )
        val boundsNormal = layerNormal.calculateBounds(512, 512)
        assertFalse(layerNormal.isFlippedHorizontal)

        val layerShifted = ModifierLayer(
            type = ModifierType.CAT_EARS,
            offsetXRatio = -0.1f,
            offsetYRatio = 0.05f,
            isFlippedHorizontal = true
        )
        val boundsShifted = layerShifted.calculateBounds(512, 512)
        assertTrue(layerShifted.isFlippedHorizontal)

        assertTrue("Positive offset should shift left boundary rightward", boundsNormal.left > boundsShifted.left)
        assertTrue("Negative vertical offset should shift top boundary upward", boundsNormal.top < boundsShifted.top)
    }

    @Test
    fun testExportFormatsContract() {
        assertEquals(".png", ExportFormat.PNG.extension)
        assertEquals("image/png", ExportFormat.PNG.mimeType)

        assertEquals(".webp", ExportFormat.WEBP_STICKER.extension)
        assertEquals("image/webp", ExportFormat.WEBP_STICKER.mimeType)

        assertEquals(".gif", ExportFormat.GIF_ANIMATED.extension)
        assertEquals("image/gif", ExportFormat.GIF_ANIMATED.mimeType)
    }

    @Test
    fun testPackExpressionsList() {
        val expressions = KitStickerStudioEngine.PACK_EXPRESSIONS
        assertTrue("Pack should contain at least 5 standard expressions", expressions.size >= 5)
        assertTrue("Should include Cat Wink", expressions.any { it.contains("Cat Wink") })
        assertTrue("Should include Sparkle Ready", expressions.any { it.contains("Sparkle Ready") })
        assertEquals(512, KitStickerStudioEngine.STANDARD_STICKER_SIZE)
    }
}
