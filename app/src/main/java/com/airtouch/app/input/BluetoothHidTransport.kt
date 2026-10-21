package com.airtouch.app.input

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BluetoothDeviceItem(
    val name: String,
    val address: String
)

data class BluetoothUiState(
    val supported: Boolean = true,
    val permissionGranted: Boolean = false,
    val adapterEnabled: Boolean = false,
    val registering: Boolean = false,
    val hidRegistered: Boolean = false,
    val connectedName: String? = null,
    val connectedAddress: String? = null,
    val connectingName: String? = null,
    val bondedDevices: List<BluetoothDeviceItem> = emptyList(),
    val error: String? = null,
    val lastEvent: String? = null
) {
    val connected: Boolean get() = connectedAddress != null
}

/**
 * 通过 Android 的 Bluetooth HID Device Profile 把手机伪装成
 * 蓝牙触控板 + 键盘。电脑端不需要安装任何软件。
 */
class BluetoothHidTransport(private val context: Context) {

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(BluetoothManager::class.java)

    private val _state = MutableStateFlow(
        BluetoothUiState(
            supported = bluetoothManager?.adapter != null,
            permissionGranted = hasConnectPermission()
        )
    )
    val state: StateFlow<BluetoothUiState> = _state.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    private var currentDevice: BluetoothDevice? = null
    private var proxyRequested = false
    private var started = false
    private var lastSendWarning = 0L

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val device = proxy as? BluetoothHidDevice ?: return
            hidDevice = device
            registerHidApp(device)
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            _state.value = _state.value.copy(
                hidRegistered = false,
                registering = false,
                connectedName = null,
                connectedAddress = null
            )
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _state.value = _state.value.copy(
                registering = false,
                hidRegistered = registered,
                error = if (registered) null else "HID 注册失败，可能已有其他 HID 应用在运行",
                lastEvent = if (registered) "HID 服务已就绪" else null
            )
            if (registered && pluggedDevice != null) {
                currentDevice = pluggedDevice
                _state.value = _state.value.copy(
                    connectedName = safeName(pluggedDevice),
                    connectedAddress = pluggedDevice.address,
                    connectingName = null
                )
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    currentDevice = device
                    _state.value = _state.value.copy(
                        connectedName = safeName(device),
                        connectedAddress = device.address,
                        connectingName = null,
                        error = null,
                        lastEvent = "已连接 ${safeName(device)}"
                    )
                }

                BluetoothProfile.STATE_DISCONNECTED,
                BluetoothProfile.STATE_DISCONNECTING -> {
                    if (currentDevice?.address == device.address) {
                        currentDevice = null
                    }
                    _state.value = _state.value.copy(
                        connectedName = null,
                        connectedAddress = null,
                        connectingName = null,
                        lastEvent = "连接已断开"
                    )
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            runCatching {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            runCatching {
                hidDevice?.replyReport(device, type, id, data)
            }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            runCatching { hidDevice?.disconnect(device) }
            if (currentDevice?.address == device.address) currentDevice = null
            _state.value = _state.value.copy(
                connectedName = null,
                connectedAddress = null,
                lastEvent = "电脑端断开了连接"
            )
        }
    }

    /** App 启动或蓝牙权限授予后调用。 */
    @SuppressLint("MissingPermission")
    fun initialize() {
        started = true
        val adapter = adapter
        if (adapter == null) {
            _state.value = _state.value.copy(supported = false, adapterEnabled = false)
            return
        }
        val granted = hasConnectPermission()
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
        _state.value = _state.value.copy(
            supported = true,
            permissionGranted = granted,
            adapterEnabled = enabled,
            error = when {
                !granted -> "需要授予“附近的设备”权限才能使用蓝牙 HID"
                !enabled -> "请先打开蓝牙"
                else -> null
            }
        )
        refreshBondedDevices()
        if (granted && enabled && !proxyRequested) {
            proxyRequested = true
            val requested = runCatching {
                adapter.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE)
            }.getOrDefault(false)
            if (!requested) {
                proxyRequested = false
                _state.value = _state.value.copy(error = "无法打开蓝牙 HID 服务")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        val adapter = adapter ?: return
        if (!hasConnectPermission()) {
            _state.value = _state.value.copy(permissionGranted = false, bondedDevices = emptyList())
            return
        }
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
        val devices = if (enabled) {
            runCatching {
                adapter.bondedDevices.orEmpty().map {
                    BluetoothDeviceItem(safeName(it), it.address)
                }.sortedBy { it.name.lowercase() }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        _state.value = _state.value.copy(
            permissionGranted = true,
            adapterEnabled = enabled,
            bondedDevices = devices,
            supported = true
        )
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val device = hidDevice ?: run {
            _state.value = _state.value.copy(error = "HID 服务尚未就绪，请稍后重试")
            return
        }
        if (!hasConnectPermission()) {
            _state.value = _state.value.copy(error = "缺少蓝牙权限")
            return
        }
        if (!_state.value.hidRegistered) {
            _state.value = _state.value.copy(error = "HID 尚未注册完成")
            return
        }
        val target = runCatching {
            adapter?.bondedDevices?.firstOrNull { it.address == address }
        }.getOrNull()
        if (target == null) {
            _state.value = _state.value.copy(error = "找不到该设备，请先在系统蓝牙中配对")
            return
        }
        _state.value = _state.value.copy(
            connectingName = safeName(target),
            error = null
        )
        val ok = runCatching { device.connect(target) }.getOrDefault(false)
        if (!ok) {
            _state.value = _state.value.copy(
                connectingName = null,
                error = "发起连接失败，请在电脑端点击 AirTouch 设备"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val device = currentDevice ?: return
        runCatching { hidDevice?.disconnect(device) }
        currentDevice = null
        _state.value = _state.value.copy(
            connectedName = null,
            connectedAddress = null,
            lastEvent = "已断开"
        )
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp(device: BluetoothHidDevice) {
        if (!hasConnectPermission()) {
            _state.value = _state.value.copy(
                permissionGranted = false,
                error = "缺少蓝牙连接权限"
            )
            return
        }
        _state.value = _state.value.copy(registering = true, error = null)
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "AirTouch",
            "AirTouch wireless trackpad and keyboard",
            "AirTouch",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReportDescriptor.bytes
        )
        val requested = runCatching {
            device.registerApp(
                sdp,
                null,
                null,
                ContextCompat.getMainExecutor(appContext),
                hidCallback
            )
        }.getOrDefault(false)
        if (!requested) {
            _state.value = _state.value.copy(
                registering = false,
                hidRegistered = false,
                error = "HID 注册请求失败，请关闭其他 HID 应用后重试"
            )
        }
    }

    /** 发送鼠标报告。返回是否成功写入蓝牙队列。 */
    @SuppressLint("MissingPermission")
    fun sendMouse(buttons: Int, dx: Int, dy: Int, wheel: Int, pan: Int): Boolean {
        val device = currentDevice ?: return false
        val hid = hidDevice ?: return false
        if (!hasConnectPermission()) return false
        return runCatching {
            hid.sendReport(
                device,
                HidReportDescriptor.MOUSE_REPORT_ID,
                HidReportDescriptor.mouseReport(buttons, dx, dy, wheel, pan)
            )
        }.getOrElse { warnSendFailure(it); false }
    }

    /** 发送键盘报告。keys 最多 6 个 usage。 */
    @SuppressLint("MissingPermission")
    fun sendKeyboard(modifiers: Int, keys: IntArray): Boolean {
        val device = currentDevice ?: return false
        val hid = hidDevice ?: return false
        if (!hasConnectPermission()) return false
        return runCatching {
            hid.sendReport(
                device,
                HidReportDescriptor.KEYBOARD_REPORT_ID,
                HidReportDescriptor.keyboardReport(modifiers, keys)
            )
        }.getOrElse { warnSendFailure(it); false }
    }

    fun shutdown() {
        if (!started) return
        started = false
        runCatching { if (hasConnectPermission()) hidDevice?.unregisterApp() }
        runCatching {
            if (hasConnectPermission()) {
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        }
        hidDevice = null
        currentDevice = null
        proxyRequested = false
        _state.value = _state.value.copy(
            hidRegistered = false,
            registering = false,
            connectedName = null,
            connectedAddress = null
        )
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun adapterName(): String? = runCatching {
        if (hasConnectPermission()) adapter?.name else null
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice): String {
        return runCatching {
            device.name?.takeIf { it.isNotBlank() } ?: device.address
        }.getOrDefault(device.address)
    }

    private fun warnSendFailure(error: Throwable) {
        val now = System.currentTimeMillis()
        if (now - lastSendWarning < 2500) return
        lastSendWarning = now
        _state.value = _state.value.copy(
            error = "发送 HID 报告失败：${error.message ?: error.javaClass.simpleName}"
        )
    }
}
