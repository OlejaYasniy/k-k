package com.example.indoornavigation.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    

    enum class ThemeMode(val value: Int) {
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        companion object {
            fun fromValue(v: Int) = entries.firstOrNull { it.value == v } ?: SYSTEM
        }
    }

    var themeMode: ThemeMode
        get() = ThemeMode.fromValue(prefs.getInt("theme_mode", ThemeMode.SYSTEM.value))
        set(v) {
            prefs.edit().putInt("theme_mode", v.value).apply()
            AppCompatDelegate.setDefaultNightMode(v.value)
        }

    

    enum class Language(val code: String, val displayName: String) {
        RU("ru", "Русский"),
        EN("en", "English");

        companion object {
            fun fromCode(code: String) = entries.firstOrNull { it.code == code } ?: RU
        }
    }

    var language: Language
        get() = Language.fromCode(prefs.getString("language", Language.RU.code) ?: Language.RU.code)
        set(v) {
            prefs.edit().putString("language", v.code).apply()
            applyLanguage()
        }

    
    fun applyLanguage() {
        val locales = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    

    enum class MapFontSize(val scale: Float, val displayName: String) {
        SMALL(0.65f, "Мелкий"),
        MEDIUM(1.0f, "Средний"),
        LARGE(1.5f, "Крупный");

        companion object {
            fun fromScale(s: Float) = entries.minByOrNull {
                kotlin.math.abs(it.scale - s)
            } ?: MEDIUM
        }
    }

    var mapFontSize: MapFontSize
        get() = MapFontSize.fromScale(prefs.getFloat("map_font_size", MapFontSize.MEDIUM.scale))
        set(v) = prefs.edit().putFloat("map_font_size", v.scale).apply()

    
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(themeMode.value)
    }
}
