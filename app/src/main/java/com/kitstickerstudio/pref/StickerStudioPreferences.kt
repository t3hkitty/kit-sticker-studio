package com.kitstickerstudio.pref

import android.content.Context
import android.content.SharedPreferences

/**
 * Sticky SharedPreferences manager for KitStickerStudio.
 */
class StickerStudioPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREF_FILE_NAME = "kit_sticker_studio_prefs"

        const val KEY_LAST_BASE_PRESET = "pref_last_base_preset"
        const val KEY_LAST_PRESET_INTENSITY = "pref_last_preset_intensity"
        const val KEY_LAST_MODIFIER = "pref_last_modifier"
        const val KEY_LAST_MODIFIER_SCALE = "pref_last_modifier_scale"
        const val KEY_LAST_MODIFIER_FLIPPED = "pref_last_modifier_flipped"
        const val KEY_LAST_EMOJI = "pref_last_emoji"
    }

    var lastBasePreset: String
        get() = prefs.getString(KEY_LAST_BASE_PRESET, "ORIGINAL") ?: "ORIGINAL"
        set(value) = prefs.edit().putString(KEY_LAST_BASE_PRESET, value).apply()

    var lastPresetIntensity: Float
        get() = prefs.getFloat(KEY_LAST_PRESET_INTENSITY, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_LAST_PRESET_INTENSITY, value).apply()

    var lastModifier: String
        get() = prefs.getString(KEY_LAST_MODIFIER, "NONE") ?: "NONE"
        set(value) = prefs.edit().putString(KEY_LAST_MODIFIER, value).apply()

    var lastModifierScale: Float
        get() = prefs.getFloat(KEY_LAST_MODIFIER_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_LAST_MODIFIER_SCALE, value).apply()

    var isLastModifierFlipped: Boolean
        get() = prefs.getBoolean(KEY_LAST_MODIFIER_FLIPPED, false)
        set(value) = prefs.edit().putBoolean(KEY_LAST_MODIFIER_FLIPPED, value).apply()

    var lastEmoji: String
        get() = prefs.getString(KEY_LAST_EMOJI, "😊") ?: "😊"
        set(value) = prefs.edit().putString(KEY_LAST_EMOJI, value).apply()
}
