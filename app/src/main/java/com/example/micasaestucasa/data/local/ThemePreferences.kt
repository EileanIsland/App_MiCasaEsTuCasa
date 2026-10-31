package com.example.micasaestucasa.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)


    companion object {
        private const val KEY_THEME = "selected_theme"
        const val LIGHT = AppCompatDelegate.MODE_NIGHT_NO
        const val DARK = AppCompatDelegate.MODE_NIGHT_YES
        const val SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }


    fun setTheme(themeMode: Int) {
        prefs.edit().putInt(KEY_THEME, themeMode).apply()
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }


    fun getTheme(): Int {
        return prefs.getInt(KEY_THEME, SYSTEM)
    }
}


