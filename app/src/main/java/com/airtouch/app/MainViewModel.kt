package com.airtouch.app

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.airtouch.app.data.SettingsRepository
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.input.InputRouter
import com.airtouch.app.model.HostOs
import com.airtouch.app.model.ThemeSkin
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    val router = InputRouter(application, settingsRepository)

    val settings = settingsRepository.settings
    val bluetoothState = router.bluetooth.state
    val usbState = router.usb.state
    val activeTransport = router.activeTransport
    val connectionSummary: StateFlow<ConnectionSummary> = router.connectionSummary
    val inputReady = router.inputReady

    init {
        // Android 12 以下蓝牙是安装时权限，可以直接初始化。
        // Android 12+ 等 Activity 拿到运行时权限后再初始化。
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            router.initializeBluetooth()
        }
    }

    fun onBluetoothPermissionGranted() {
        router.initializeBluetooth()
    }

    fun onResume() {
        router.refreshBluetooth()
        val bluetooth = bluetoothState.value
        if (bluetooth.permissionGranted && !bluetooth.hidRegistered) {
            router.initializeBluetooth()
        }
    }

    fun connectBluetooth(address: String) = router.connectBluetooth(address)

    fun disconnectBluetooth() = router.disconnectBluetooth()

    fun connectUsb(host: String, port: Int) {
        router.connectUsb(host, port)
    }

    fun disconnectUsb() = router.disconnectUsb()

    fun useBluetooth() = router.useBluetooth()

    fun useUsb() = router.useUsb()

    fun setSkin(skin: ThemeSkin) = settingsRepository.setSkin(skin)

    fun setSensitivity(value: Float) = settingsRepository.update { it.copy(sensitivity = value) }

    fun setNaturalScroll(enabled: Boolean) =
        settingsRepository.update { it.copy(naturalScroll = enabled) }

    fun setTapToClick(enabled: Boolean) =
        settingsRepository.update { it.copy(tapToClick = enabled) }

    fun setHaptics(enabled: Boolean) =
        settingsRepository.update { it.copy(haptics = enabled) }

    fun setShowTouchPoints(enabled: Boolean) =
        settingsRepository.update { it.copy(showTouchPoints = enabled) }

    fun setHostOs(hostOs: HostOs) = settingsRepository.setHostOs(hostOs)

    fun setUsbEndpoint(host: String, port: Int) = settingsRepository.update {
        it.copy(usbHost = host, usbPort = port)
    }

    fun move(dx: Float, dy: Float) = router.move(dx, dy)

    fun scroll(dx: Float, dy: Float) = router.scroll(dx, dy)

    fun clickLeft() = router.click()

    fun clickRight() = router.click(2)

    fun mouseDown() = router.mouseDown()

    fun mouseUp() = router.mouseUp()

    fun tapKey(modifiers: Int, key: Int) = router.tapKey(modifiers, key)

    fun performAction(action: com.airtouch.app.input.SystemAction) =
        router.performAction(action)

    override fun onCleared() {
        router.shutdown()
        super.onCleared()
    }
}
