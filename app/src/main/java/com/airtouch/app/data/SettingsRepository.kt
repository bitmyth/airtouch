package com.airtouch.app.data

import android.content.Context
import com.airtouch.app.model.AppSettings
import com.airtouch.app.model.HostOs
import com.airtouch.app.model.ThemeSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 使用 SharedPreferences 持久化设置。数据量很小，不必为此引入 DataStore。
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        persist(next)
    }

    fun setSkin(skin: ThemeSkin) = update { it.copy(skin = skin) }
    fun setHostOs(hostOs: HostOs) = update { it.copy(hostOs = hostOs) }

    private fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            skin = prefs.getString(KEY_SKIN, null)
                ?.let { runCatching { ThemeSkin.valueOf(it) }.getOrNull() }
                ?: defaults.skin,
            sensitivity = prefs.getFloat(KEY_SENSITIVITY, defaults.sensitivity)
                .coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY),
            naturalScroll = prefs.getBoolean(KEY_NATURAL_SCROLL, defaults.naturalScroll),
            tapToClick = prefs.getBoolean(KEY_TAP_TO_CLICK, defaults.tapToClick),
            haptics = prefs.getBoolean(KEY_HAPTICS, defaults.haptics),
            showTouchPoints = prefs.getBoolean(KEY_TOUCH_POINTS, defaults.showTouchPoints),
            hostOs = prefs.getString(KEY_HOST_OS, null)
                ?.let { runCatching { HostOs.valueOf(it) }.getOrNull() }
                ?: defaults.hostOs,
            usbHost = prefs.getString(KEY_USB_HOST, null)
                ?.takeIf { it.isNotBlank() }
                ?: defaults.usbHost,
            usbPort = prefs.getInt(KEY_USB_PORT, defaults.usbPort)
                .takeIf { it in 1..65535 }
                ?: defaults.usbPort
        )
    }

    private fun persist(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_SKIN, settings.skin.name)
            .putFloat(KEY_SENSITIVITY, settings.sensitivity)
            .putBoolean(KEY_NATURAL_SCROLL, settings.naturalScroll)
            .putBoolean(KEY_TAP_TO_CLICK, settings.tapToClick)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putBoolean(KEY_TOUCH_POINTS, settings.showTouchPoints)
            .putString(KEY_HOST_OS, settings.hostOs.name)
            .putString(KEY_USB_HOST, settings.usbHost)
            .putInt(KEY_USB_PORT, settings.usbPort)
            .apply()
    }

    companion object {
        const val MIN_SENSITIVITY = 0.45f
        const val MAX_SENSITIVITY = 2.20f

        private const val PREFS_NAME = "airtouch_settings_v1"
        private const val KEY_SKIN = "skin"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_NATURAL_SCROLL = "natural_scroll"
        private const val KEY_TAP_TO_CLICK = "tap_to_click"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_TOUCH_POINTS = "touch_points"
        private const val KEY_HOST_OS = "host_os"
        private const val KEY_USB_HOST = "usb_host"
        private const val KEY_USB_PORT = "usb_port"
    }
}
