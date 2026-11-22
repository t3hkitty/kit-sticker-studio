package com.kitstickerstudio.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kitstickerstudio.engine.BasePreset
import com.kitstickerstudio.engine.BasePresetType
import com.kitstickerstudio.engine.ExportFormat
import com.kitstickerstudio.engine.KitStickerStudioEngine
import com.kitstickerstudio.engine.ModifierLayer
import com.kitstickerstudio.engine.ModifierType
import com.kitstickerstudio.pref.StickerStudioPreferences
import java.io.File

/**
 * MainActivity: Interactive UI for KitStickerStudio.
 * Provides controls for Preset Bases, Proportional Emoji Modifiers,
 * Emoji Animator, and Single-Image Emoji Pack Generator.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: StickerStudioPreferences
    private val engine = KitStickerStudioEngine()

    private var currentEmoji = "😊"
    private var currentPresetType = BasePresetType.ORIGINAL
    private var currentIntensity = 1.0f

    private var currentModifierType = ModifierType.NONE
    private var currentModifierScale = 1.0f
    private var isModifierFlipped = false

    private lateinit var imgPreview: ImageView
    private lateinit var layoutPresetChips: LinearLayout
    private lateinit var layoutModifierChips: LinearLayout
    private lateinit var layoutEmojiPicker: LinearLayout

    private lateinit var seekPresetIntensity: SeekBar
    private lateinit var lblPresetIntensity: TextView

    private lateinit var seekModifierScale: SeekBar
    private lateinit var lblModifierScale: TextView
    private lateinit var btnFlipModifier: Button

    private var currentRenderedBitmap: Bitmap? = null
    private var animatorFrames: List<Bitmap> = emptyList()
    private var currentFrameIndex = 0
    private var isAnimating = false
    private val handler = Handler(Looper.getMainLooper())

    private val samplePickerEmojis = listOf("😊", "😸", "🐱", "😻", "🥺", "🔥", "🤪", "💜", "✨", "🐾")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = StickerStudioPreferences(this)

        initViews()
        loadPersistedState()
        populatePresetChips()
        populateModifierChips()
        populateEmojiPicker()
        setupListeners()

        updateLiveSticker()
    }

    private fun initViews() {
        imgPreview = findViewById(R.id.img_sticker_preview)
        layoutPresetChips = findViewById(R.id.layout_preset_chips)
        layoutModifierChips = findViewById(R.id.layout_modifier_chips)
        layoutEmojiPicker = findViewById(R.id.layout_emoji_picker)

        seekPresetIntensity = findViewById(R.id.seek_preset_intensity)
        lblPresetIntensity = findViewById(R.id.lbl_preset_intensity)

        seekModifierScale = findViewById(R.id.seek_modifier_scale)
        lblModifierScale = findViewById(R.id.lbl_modifier_scale)
        btnFlipModifier = findViewById(R.id.btn_flip_modifier)
    }

    private fun loadPersistedState() {
        currentEmoji = prefs.lastEmoji
        currentPresetType = try {
            BasePresetType.valueOf(prefs.lastBasePreset)
        } catch (_: Exception) {
            BasePresetType.ORIGINAL
        }
        currentIntensity = prefs.lastPresetIntensity

        currentModifierType = try {
            ModifierType.valueOf(prefs.lastModifier)
        } catch (_: Exception) {
            ModifierType.NONE
        }
        currentModifierScale = prefs.lastModifierScale
        isModifierFlipped = prefs.isLastModifierFlipped

        seekPresetIntensity.progress = (currentIntensity * 100).toInt()
        lblPresetIntensity.text = "Intensity (${(currentIntensity * 100).toInt()}%):"

        seekModifierScale.progress = (currentModifierScale * 100).toInt()
        lblModifierScale.text = String.format("Scale (%.1fx):", currentModifierScale)

        btnFlipModifier.text = if (isModifierFlipped) "Flipped ⇄" else "Flip ⇄"
    }

    private fun populatePresetChips() {
        layoutPresetChips.removeAllViews()
        for (preset in BasePresetType.values()) {
            val chip = Button(this).apply {
                text = preset.chipLabel
                textSize = 11f
                val isSelected = (preset == currentPresetType)
                styleChip(this, isSelected)
                setOnClickListener {
                    currentPresetType = preset
                    prefs.lastBasePreset = preset.name
                    refreshPresetChips()
                    updateLiveSticker()
                }
            }
            layoutPresetChips.addView(chip)
        }
    }

    private fun populateModifierChips() {
        layoutModifierChips.removeAllViews()
        for (mod in ModifierType.values()) {
            val chip = Button(this).apply {
                text = mod.chipLabel
                textSize = 11f
                val isSelected = (mod == currentModifierType)
                styleChip(this, isSelected)
                setOnClickListener {
                    currentModifierType = mod
                    prefs.lastModifier = mod.name
                    refreshModifierChips()
                    updateLiveSticker()
                }
            }
            layoutModifierChips.addView(chip)
        }
    }

    private fun populateEmojiPicker() {
        layoutEmojiPicker.removeAllViews()
        for (emoji in samplePickerEmojis) {
            val btn = Button(this).apply {
                text = emoji
                textSize = 18f
                styleChip(this, emoji == currentEmoji)
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                setOnClickListener {
                    currentEmoji = emoji
                    prefs.lastEmoji = emoji
                    refreshEmojiChips()
                    updateLiveSticker()
                }
            }
            layoutEmojiPicker.addView(btn)
        }
    }

    private fun setupListeners() {
        seekPresetIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentIntensity = (progress / 100f).coerceIn(0f, 1f)
                lblPresetIntensity.text = "Intensity ($progress%):"
                prefs.lastPresetIntensity = currentIntensity
                if (fromUser) updateLiveSticker()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekModifierScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentModifierScale = (progress / 100f).coerceIn(0.5f, 1.5f)
                lblModifierScale.text = String.format("Scale (%.1fx):", currentModifierScale)
                prefs.lastModifierScale = currentModifierScale
                if (fromUser) updateLiveSticker()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnFlipModifier.setOnClickListener {
            isModifierFlipped = !isModifierFlipped
            prefs.isLastModifierFlipped = isModifierFlipped
            btnFlipModifier.text = if (isModifierFlipped) "Flipped ⇄" else "Flip ⇄"
            updateLiveSticker()
        }

        findViewById<Button>(R.id.btn_export_png).setOnClickListener {
            exportCurrentSticker(ExportFormat.PNG)
        }

        findViewById<Button>(R.id.btn_export_webp).setOnClickListener {
            exportCurrentSticker(ExportFormat.WEBP_STICKER)
        }

        findViewById<Button>(R.id.btn_generate_pack).setOnClickListener {
            generateExpressionPackModal()
        }

        findViewById<Button>(R.id.btn_animate_loop).setOnClickListener {
            toggleAnimationLoop()
        }
    }

    private fun updateLiveSticker() {
        stopAnimation()
        val rawBase = engine.createEmojiBaseBitmap(currentEmoji, KitStickerStudioEngine.STANDARD_STICKER_SIZE)
        val preset = BasePreset(currentPresetType, currentIntensity)
        val modifier = ModifierLayer(
            type = currentModifierType,
            scale = currentModifierScale,
            isFlippedHorizontal = isModifierFlipped
        )

        val rendered = engine.processSingleEmoji(
            rawBase,
            preset,
            listOf(modifier),
            KitStickerStudioEngine.STANDARD_STICKER_SIZE
        )
        currentRenderedBitmap = rendered
        imgPreview.setImageBitmap(rendered)
    }

    private fun toggleAnimationLoop() {
        if (isAnimating) {
            stopAnimation()
            updateLiveSticker()
        } else {
            val rawBase = engine.createEmojiBaseBitmap(currentEmoji, KitStickerStudioEngine.STANDARD_STICKER_SIZE)
            val preset = BasePreset(currentPresetType, currentIntensity)
            val modifier = ModifierLayer(
                type = currentModifierType,
                scale = currentModifierScale,
                isFlippedHorizontal = isModifierFlipped
            )
            animatorFrames = engine.generateAnimatedFrames(rawBase, preset, listOf(modifier), 8)
            isAnimating = true
            currentFrameIndex = 0
            startAnimationLoop()
        }
    }

    private fun startAnimationLoop() {
        if (!isAnimating || animatorFrames.isEmpty()) return
        imgPreview.setImageBitmap(animatorFrames[currentFrameIndex])
        currentFrameIndex = (currentFrameIndex + 1) % animatorFrames.size
        handler.postDelayed({ startAnimationLoop() }, 120)
    }

    private fun stopAnimation() {
        isAnimating = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun exportCurrentSticker(format: ExportFormat) {
        val bitmap = currentRenderedBitmap ?: return
        val fileName = "sticker_${System.currentTimeMillis()}${format.extension}"
        val destFile = File(getExternalFilesDir(null), fileName)

        val success = engine.saveToFile(bitmap, destFile, format)
        if (success) {
            Toast.makeText(this, "Exported 512x512 ${format.name}: ${destFile.name} 🐾", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateExpressionPackModal() {
        val sampleEmojis = listOf("😊", "😸", "😻", "🤪", "🥺", "🔥")
        val preset = BasePreset(currentPresetType, currentIntensity)
        val modifier = ModifierLayer(
            type = currentModifierType,
            scale = currentModifierScale,
            isFlippedHorizontal = isModifierFlipped
        )

        val packBitmaps = engine.generateExpressionPack(sampleEmojis, preset, listOf(modifier))

        val packView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#1E1E2E"))
        }

        val title = TextView(this).apply {
            text = "🎁 Expression Pack (${packBitmaps.size} Stickers)"
            setTextColor(Color.parseColor("#FFFBEB"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        packView.addView(title)

        val scroll = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        for ((idx, bmp) in packBitmaps.withIndex()) {
            val img = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(76), dp(76)).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageBitmap(bmp)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#25163A"))
                    cornerRadius = dp(14).toFloat()
                    setStroke(dp(1), Color.parseColor("#FFB7C5"))
                }
            }
            row.addView(img)
        }
        scroll.addView(row)
        packView.addView(scroll)

        val subtext = TextView(this).apply {
            text = "Preset & modifiers applied uniformly across expression pack ✨"
            setTextColor(Color.parseColor("#E6E6FA"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        packView.addView(subtext)

        AlertDialog.Builder(this)
            .setView(packView)
            .setPositiveButton("Close 🐾", null)
            .show()
    }

    private fun styleChip(btn: Button, isSelected: Boolean) {
        btn.apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                if (isSelected) {
                    setColor(Color.parseColor("#FFB7C5")) // Kawaii Pink
                } else {
                    setColor(Color.parseColor("#2E1A47")) // Deep Indigo
                    setStroke(dp(1), Color.parseColor("#E6E6FA"))
                }
            }
            setTextColor(if (isSelected) Color.parseColor("#1E1E2E") else Color.parseColor("#E6E6FA"))
            val padH = dp(10)
            val padV = dp(4)
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(34)
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        }
    }

    private fun refreshPresetChips() {
        for (i in 0 until layoutPresetChips.childCount) {
            val child = layoutPresetChips.getChildAt(i) as? Button ?: continue
            val preset = BasePresetType.values().getOrNull(i) ?: continue
            styleChip(child, preset == currentPresetType)
        }
    }

    private fun refreshModifierChips() {
        for (i in 0 until layoutModifierChips.childCount) {
            val child = layoutModifierChips.getChildAt(i) as? Button ?: continue
            val mod = ModifierType.values().getOrNull(i) ?: continue
            styleChip(child, mod == currentModifierType)
        }
    }

    private fun refreshEmojiChips() {
        for (i in 0 until layoutEmojiPicker.childCount) {
            val child = layoutEmojiPicker.getChildAt(i) as? Button ?: continue
            val emoji = samplePickerEmojis.getOrNull(i) ?: continue
            styleChip(child, emoji == currentEmoji)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnimation()
    }
}
