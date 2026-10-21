package com.airtouch.app.input

import android.content.Context
import com.airtouch.app.data.SettingsRepository
import com.airtouch.app.model.TransportKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

data class ConnectionSummary(
    val active: TransportKind,
    val connected: Boolean,
    val title: String,
    val detail: String
)

/**
 * 统一输入出口：
 *  - 蓝牙：直接发送 HID Report
 *  - USB：把同样语义的输入编码后发给桌面桥接程序
 *
 * UI 只调用 move / scroll / click / tapKey / performAction，不关心底层通道。
 */
class InputRouter(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val keyMutex = Mutex()
    private val buttonLock = Any()

    val bluetooth = BluetoothHidTransport(context)
    val usb = UsbBridgeTransport()

    private val _activeTransport = MutableStateFlow(TransportKind.BLUETOOTH)
    val activeTransport: StateFlow<TransportKind> = _activeTransport

    val connectionSummary: StateFlow<ConnectionSummary> = combine(
        _activeTransport,
        bluetooth.state,
        usb.state
    ) { active, bt, usb ->
        when (active) {
            TransportKind.BLUETOOTH -> {
                if (bt.connected) {
                    ConnectionSummary(
                        active = active,
                        connected = true,
                        title = "蓝牙已连接",
                        detail = bt.connectedName ?: "HID 触控板"
                    )
                } else {
                    ConnectionSummary(
                        active = active,
                        connected = false,
                        title = "蓝牙未连接",
                        detail = if (bt.hidRegistered) "HID 已就绪，选择电脑" else "点击配置蓝牙"
                    )
                }
            }

            TransportKind.USB -> {
                if (usb.connected) {
                    ConnectionSummary(
                        active = active,
                        connected = true,
                        title = "USB 已连接",
                        detail = "${usb.host}:${usb.port}"
                    )
                } else {
                    ConnectionSummary(
                        active = active,
                        connected = false,
                        title = "USB 未连接",
                        detail = "运行桌面桥接程序"
                    )
                }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionSummary(
        TransportKind.BLUETOOTH,
        false,
        "未连接",
        "选择一种连接方式"
    ))

    val inputReady: StateFlow<Boolean> = combine(
        _activeTransport,
        bluetooth.state,
        usb.state
    ) { active, bt, usb ->
        when (active) {
            TransportKind.BLUETOOTH -> bt.connected
            TransportKind.USB -> usb.connected
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private var mouseButtons = 0
    private var accX = 0f
    private var accY = 0f
    private var scrollAccX = 0f
    private var scrollAccY = 0f

    init {
        scope.launch {
            bluetooth.state.collect { state ->
                if (state.connected) _activeTransport.value = TransportKind.BLUETOOTH
            }
        }
        scope.launch {
            usb.state.collect { state ->
                if (state.connected) _activeTransport.value = TransportKind.USB
            }
        }
    }

    fun initializeBluetooth() = bluetooth.initialize()

    fun refreshBluetooth() = bluetooth.refreshBondedDevices()

    fun connectBluetooth(address: String) {
        _activeTransport.value = TransportKind.BLUETOOTH
        bluetooth.connect(address)
    }

    fun disconnectBluetooth() {
        bluetooth.disconnect()
    }

    fun connectUsb(host: String, port: Int) {
        _activeTransport.value = TransportKind.USB
        usb.connect(host, port)
    }

    fun disconnectUsb() {
        usb.disconnect()
    }

    fun useBluetooth() {
        _activeTransport.value = TransportKind.BLUETOOTH
    }

    fun useUsb() {
        _activeTransport.value = TransportKind.USB
    }

    /** 输入是否可发送到当前通道。 */
    fun isReady(): Boolean = when (_activeTransport.value) {
        TransportKind.BLUETOOTH -> bluetooth.state.value.connected
        TransportKind.USB -> usb.state.value.connected
    }

    /**
     * 触控板相对移动。dx/dy 是手机像素，乘上灵敏度和手感系数后累积发送。
     */
    fun move(dx: Float, dy: Float) {
        if (!isReady()) return
        val sensitivity = settingsRepository.settings.value.sensitivity
        accX += dx * BASE_CURSOR_GAIN * sensitivity
        accY += dy * BASE_CURSOR_GAIN * sensitivity

        val stepX = accX.roundToInt()
        val stepY = accY.roundToInt()
        if (stepX == 0 && stepY == 0) return
        accX -= stepX
        accY -= stepY
        sendMouseChunked(stepX, stepY, 0, 0)
    }

    /**
     * 双指滚动。已经由手势层处理“自然滚动”方向，这里只做比例缩放和整数累积。
     */
    fun scroll(dx: Float, dy: Float) {
        if (!isReady()) return
        scrollAccX += dx * SCROLL_GAIN
        scrollAccY += dy * SCROLL_GAIN

        val stepX = scrollAccX.roundToInt()
        val stepY = scrollAccY.roundToInt()
        if (stepX == 0 && stepY == 0) return
        scrollAccX -= stepX
        scrollAccY -= stepY
        sendMouseChunked(0, 0, stepY, stepX)
    }

    fun click(button: Int = HidMouse.LEFT) {
        if (!isReady()) return
        synchronized(buttonLock) {
            val before = mouseButtons
            sendButtons(before or button)
            sendButtons(before)
        }
    }

    fun mouseDown(button: Int = HidMouse.LEFT) {
        if (!isReady()) return
        synchronized(buttonLock) {
            sendButtons(mouseButtons or button)
        }
    }

    fun mouseUp(button: Int = HidMouse.LEFT) {
        if (!isReady()) return
        synchronized(buttonLock) {
            sendButtons(mouseButtons and button.inv())
        }
    }

    fun tapKey(modifiers: Int, key: Int) {
        if (!isReady()) return
        scope.launch {
            keyMutex.withLock {
                sendKeyboardReport(modifiers, intArrayOf(key))
                delay(KEY_PRESS_MILLIS)
                sendKeyboardReport(0, IntArray(0))
            }
        }
    }

    fun sendKeyboardDown(modifiers: Int, key: Int) {
        if (!isReady()) return
        sendKeyboardReport(modifiers, intArrayOf(key))
    }

    fun sendKeyboardUp() {
        if (!isReady()) return
        sendKeyboardReport(0, IntArray(0))
    }

    fun performAction(action: SystemAction) {
        val chord = HidShortcuts.forAction(action, settingsRepository.settings.value.hostOs)
        tapKey(chord.modifiers, chord.key)
    }

    fun releaseAll() {
        synchronized(buttonLock) {
            if (mouseButtons != 0) sendButtons(0)
        }
        sendKeyboardReport(0, IntArray(0))
    }

    fun shutdown() {
        releaseAll()
        bluetooth.shutdown()
        usb.shutdown()
        scope.cancel()
    }

    private fun sendKeyboardReport(modifiers: Int, keys: IntArray) {
        when (_activeTransport.value) {
            TransportKind.BLUETOOTH -> bluetooth.sendKeyboard(modifiers, keys)
            TransportKind.USB -> usb.sendKeyboard(modifiers, keys)
        }
    }

    private fun sendButtons(mask: Int) {
        if (mouseButtons == mask) return
        mouseButtons = mask
        when (_activeTransport.value) {
            TransportKind.BLUETOOTH -> bluetooth.sendMouse(mask, 0, 0, 0, 0)
            TransportKind.USB -> usb.sendButtons(mask)
        }
    }

    /**
     * HID 相对位移只有 8 位，高速滑动时拆成多个 report，避免丢帧。
     */
    private fun sendMouseChunked(dx: Int, dy: Int, wheel: Int, pan: Int) {
        var remainingX = dx
        var remainingY = dy
        var remainingWheel = wheel
        var remainingPan = pan
        do {
            val chunkX = remainingX.coerceIn(-127, 127)
            val chunkY = remainingY.coerceIn(-127, 127)
            val chunkWheel = remainingWheel.coerceIn(-127, 127)
            val chunkPan = remainingPan.coerceIn(-127, 127)
            when (_activeTransport.value) {
                TransportKind.BLUETOOTH -> {
                    bluetooth.sendMouse(mouseButtons, chunkX, chunkY, chunkWheel, chunkPan)
                }

                TransportKind.USB -> {
                    if (chunkX != 0 || chunkY != 0) usb.sendMove(chunkX, chunkY)
                    if (chunkWheel != 0 || chunkPan != 0) {
                        usb.sendScroll(chunkWheel, chunkPan)
                    }
                }
            }
            remainingX -= chunkX
            remainingY -= chunkY
            remainingWheel -= chunkWheel
            remainingPan -= chunkPan
        } while (
            remainingX != 0 || remainingY != 0 ||
            remainingWheel != 0 || remainingPan != 0
        )
    }

    companion object {
        private const val BASE_CURSOR_GAIN = 1.55f
        private const val SCROLL_GAIN = 0.075f
        private const val KEY_PRESS_MILLIS = 18L
    }
}
