package app.openscanner.android.data

import android.content.Context
import app.openscanner.android.core.vision.ScanFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    System("Follow system"),
    Light("Light"),
    Dark("Dark")
}

/** SharedPreferences-backed settings exposed as StateFlows. */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeMode.System)
    )
    val theme = _theme.asStateFlow()

    private val _defaultFilter = MutableStateFlow(
        runCatching { ScanFilter.valueOf(prefs.getString(KEY_DEFAULT_FILTER, null) ?: "") }
            .getOrDefault(ScanFilter.MagicBw)
    )
    val defaultFilter = _defaultFilter.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setDefaultFilter(filter: ScanFilter) {
        _defaultFilter.value = filter
        prefs.edit().putString(KEY_DEFAULT_FILTER, filter.name).apply()
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_DEFAULT_FILTER = "default_filter"
    }
}
