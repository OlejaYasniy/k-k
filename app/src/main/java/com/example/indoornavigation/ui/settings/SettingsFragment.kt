package com.example.indoornavigation.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.indoornavigation.R
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.SettingsManager
import com.example.indoornavigation.data.local.SettingsManager.*
import com.example.indoornavigation.ui.main.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var settings: SettingsManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = SettingsManager(requireContext())

        val tvLanguage = view.findViewById<TextView>(R.id.tvLanguageValue)
        val tvTheme    = view.findViewById<TextView>(R.id.tvThemeValue)
        val tvFontSize = view.findViewById<TextView>(R.id.tvFontSizeValue)
        val tvVersion  = view.findViewById<TextView>(R.id.tvVersionValue)

        refreshValues(tvLanguage, tvTheme, tvFontSize)

        try {
            val pInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            tvVersion.text = getString(R.string.settings_version, pInfo.versionName)
        } catch (_: Exception) {
            tvVersion.text = getString(R.string.settings_version, "1.0")
        }

        // ── Language ──────────────────────────────────────────────────────────
        view.findViewById<View>(R.id.settingLanguage).setOnClickListener {
            val languages = Language.entries.toTypedArray()
            val names = languages.map {
                when (it) {
                    Language.RU -> getString(R.string.settings_language_ru)
                    Language.EN -> getString(R.string.settings_language_en)
                }
            }.toTypedArray()
            val current = languages.indexOf(settings.language)

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(names, current) { dialog, which ->
                    val newLang = languages[which]
                    if (newLang != settings.language) {
                        settings.language = newLang
                        refreshValues(tvLanguage, tvTheme, tvFontSize)

                        // Clear nav cache → server will return data in the new language on next fetch
                        val ctx = requireContext().applicationContext
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val db = AppDatabase.getInstance(ctx)
                                db.navCacheDao().clearAllBuildings()
                                db.navCacheDao().clearAllFloors()
                                db.navCacheDao().clearAllRooms()
                                db.navCacheDao().clearAllNodes()
                                db.navCacheDao().clearAllEdges()
                                db.navCacheDao().clearAllPois()
                            } catch (e: Exception) {
                                android.util.Log.w("SettingsFragment", "Cache clear error: ${e.message}")
                            }
                            withContext(Dispatchers.Main) {
                                // Signal all fragments via ViewModel SharedFlow (no deprecated broadcasts)
                                viewModel.notifyLanguageChanged()
                                val msg = if (newLang == Language.EN) "Language changed to English" else "Язык изменён на Русский"
                                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }

        // ── Theme ─────────────────────────────────────────────────────────────
        view.findViewById<View>(R.id.settingTheme).setOnClickListener {
            val themes = ThemeMode.entries.toTypedArray()
            val names = arrayOf(
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark),
                getString(R.string.settings_theme_system)
            )
            val current = themes.indexOf(settings.themeMode)

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(names, current) { dialog, which ->
                    settings.themeMode = themes[which]
                    refreshValues(tvLanguage, tvTheme, tvFontSize)
                    dialog.dismiss()
                }
                .show()
        }

        // ── Font size ──────────────────────────────────────────────────────────
        view.findViewById<View>(R.id.settingFontSize).setOnClickListener {
            val sizes = MapFontSize.entries.toTypedArray()
            val names = sizes.map {
                when (it) {
                    MapFontSize.SMALL  -> getString(R.string.settings_font_small)
                    MapFontSize.MEDIUM -> getString(R.string.settings_font_medium)
                    MapFontSize.LARGE  -> getString(R.string.settings_font_large)
                }
            }.toTypedArray()
            val current = sizes.indexOf(settings.mapFontSize)

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_map_font)
                .setSingleChoiceItems(names, current) { dialog, which ->
                    settings.mapFontSize = sizes[which]
                    refreshValues(tvLanguage, tvTheme, tvFontSize)
                    dialog.dismiss()
                }
                .show()
        }

        // ── Clear cache ────────────────────────────────────────────────────────
        view.findViewById<View>(R.id.settingClearCache).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_clear_cache)
                .setMessage(R.string.settings_clear_cache_desc)
                .setPositiveButton(getString(R.string.profile_clear)) { _, _ ->
                    val ctx = requireContext()
                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getInstance(ctx).clearAllTables()
                        }
                        Toast.makeText(ctx, R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }

        // ── About ──────────────────────────────────────────────────────────────
        view.findViewById<View>(R.id.settingAbout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.about_title)
                .setMessage(
                    getString(R.string.about_description) +
                    "\n\n" + tvVersion.text +
                    "\n" + getString(R.string.settings_developer) + ": " +
                    getString(R.string.settings_developer_name)
                )
                .setPositiveButton(R.string.about_close, null)
                .show()
        }
    }

    private fun refreshValues(tvLang: TextView, tvTheme: TextView, tvFont: TextView) {
        tvLang.text = when (settings.language) {
            Language.RU -> getString(R.string.settings_language_ru)
            Language.EN -> getString(R.string.settings_language_en)
        }
        tvTheme.text = when (settings.themeMode) {
            ThemeMode.LIGHT  -> getString(R.string.settings_theme_light)
            ThemeMode.DARK   -> getString(R.string.settings_theme_dark)
            ThemeMode.SYSTEM -> getString(R.string.settings_theme_system)
        }
        tvFont.text = when (settings.mapFontSize) {
            MapFontSize.SMALL  -> getString(R.string.settings_font_small)
            MapFontSize.MEDIUM -> getString(R.string.settings_font_medium)
            MapFontSize.LARGE  -> getString(R.string.settings_font_large)
        }
    }
}
