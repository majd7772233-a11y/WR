package com.example.wallrush.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.wallrush.ui.localization.AppLanguage

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wallrush_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "key_app_language"
        private const val KEY_SOUND = "key_sound_enabled"
        private const val KEY_VIBRATION = "key_vibration_enabled"
        private const val KEY_WALL_CONFIRM = "key_wall_confirm"
        private const val KEY_LEGAL_MOVES = "key_show_legal_moves"
        private const val KEY_THEME = "key_app_theme"
    }

    fun getThemeId(): com.example.ui.theme.GameThemeId {
        val name = prefs.getString(KEY_THEME, null)
        return try {
            if (name != null) com.example.ui.theme.GameThemeId.valueOf(name) else com.example.ui.theme.GameThemeId.CYBER_NEON
        } catch (e: Exception) {
            com.example.ui.theme.GameThemeId.CYBER_NEON
        }
    }

    fun setThemeId(themeId: com.example.ui.theme.GameThemeId) {
        prefs.edit().putString(KEY_THEME, themeId.name).apply()
    }

    fun getLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, null)
        return when (code) {
            "ar" -> AppLanguage.ARABIC
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.ARABIC // Default to Arabic as requested by user
        }
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isWallConfirmRequired(): Boolean = prefs.getBoolean(KEY_WALL_CONFIRM, true)
    fun setWallConfirmRequired(required: Boolean) {
        prefs.edit().putBoolean(KEY_WALL_CONFIRM, required).apply()
    }

    fun isShowLegalMoves(): Boolean = prefs.getBoolean(KEY_LEGAL_MOVES, true)
    fun setShowLegalMoves(show: Boolean) {
        prefs.edit().putBoolean(KEY_LEGAL_MOVES, show).apply()
    }
}
