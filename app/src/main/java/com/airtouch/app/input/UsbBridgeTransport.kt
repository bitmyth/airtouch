package com.airtouch.app.input

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

enum class UsbLinkStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    WAITING,
    ERROR
}

data class UsbUiState(
    val status: UsbLinkStatus = UsbLinkStatus.IDLE,
    val host: String = "127.0.0.1",
    val port: Int = 17575,
    val detail: String? = null,
    val error: String? = null
) {
    val connected: Boolean get() = status == UsbLinkStatus.CONNECTED
}

/**
 * 通过 USB 调试通道（adb reverse）连接电脑上的桌面桥接程序。
 *
 * 手机端是 TCP 客户端，只连接 127.0.0.1；adb reverse 会把该端口转发到电脑。
 * 若使用 USB 网络共享，也可以把 host 改成电脑在共享网络中的 IP。
 */
class UsbBridgeTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(UsbUiState())
    val state: StateFlow<UsbUiState> = _state.asStateFlow()

    private var connectionJob: Job? = null
    private var shouldRun = false
    private val outbound = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private val writeLock = Any()

    @Volatile
    private var socket: Socket? = null

    @Volatile
    private var output: OutputStream? = null

    @Volatile
    private var input: InputStream? = null

    init {
        // Socket 写入不能在主线程执行；UI 事件只负责入队。
        scope.launch(Dispatchers.IO) {
            for (frame in outbound) {
                val stream = output ?: continue
                try {
                    synchronized(writeLock) {
                        stream.write(frame)
                        stream.flush()
                    }
                } catch (t: Throwable) {
                    Log.d(TAG, "socket write failed: ${t.javaClass.simpleName}", t)
                    closeSocket()
                    _state.value = _state.value.copy(
                        status = UsbLinkStatus.WAITING,
                        detail = "连接中断，正在重连"
                    )
                }
            }
        }
    }

    fun connect(host: String, port: Int) {
        val cleanHost = host.trim().ifBlank { "127.0.0.1" }
        if (port !in 1..65535) {
            _state.value = _state.value.copy(
                status = UsbLinkStatus.ERROR,
                error = "端口必须在 1–65535 之间"
            )
            return
        }
        if (connectionJob?.isActive == true) {
            shouldRun = false
            connectionJob?.cancel()
        }
        closeSocket()
        drainOutbound()
        shouldRun = true
        _state.value = UsbUiState(
            status = UsbLinkStatus.CONNECTING,
            host = cleanHost,
            port = port,
            detail = "正在连接 $cleanHost:$port"
        )
        Log.d(TAG, "connect requested: $cleanHost:$port")
        connectionJob = scope.launch { connectionLoop(cleanHost, port) }
    }

    fun disconnect() {
        Log.d(TAG, "manual disconnect")
        shouldRun = false
        connectionJob?.cancel()
        connectionJob = null
        closeSocket()
        _state.value = _state.value.copy(
            status = UsbLinkStatus.IDLE,
            detail = "已断开",
            error = null
        )
    }

    fun shutdown() {
        shouldRun = false
        connectionJob?.cancel()
        scope.cancel()
        closeSocket()
    }

    private suspend fun connectionLoop(host: String, port: Int) {
        var attempt = 0
        while (shouldRun) {
            attempt++
            if (attempt > 1) {
                _state.value = _state.value.copy(
                    status = UsbLinkStatus.WAITING,
                    detail = "等待桌面桥接程序（第 $attempt 次尝试）"
                )
            }
            try {
                val newSocket = Socket()
                newSocket.tcpNoDelay = true
                newSocket.connect(InetSocketAddress(host, port), 2500)
                socket = newSocket
                input = newSocket.getInputStream()
                output = newSocket.getOutputStream()
                drainOutbound()
                Log.d(TAG, "socket connected to $host:$port")
                _state.value = _state.value.copy(
                    status = UsbLinkStatus.CONNECTED,
                    detail = "已连接 $host:$port",
                    error = null
                )
                monitorConnection(newSocket)
            } catch (t: Throwable) {
                if (!shouldRun) break
                _state.value = _state.value.copy(
                    status = UsbLinkStatus.WAITING,
                    detail = "未连接到桌面端",
                    error = humanReadableError(t)
                )
                closeSocket()
                delay(2000)
            }
        }
        if (!shouldRun) {
            _state.value = _state.value.copy(
                status = UsbLinkStatus.IDLE,
                detail = "已断开",
                error = null
            )
        }
    }

    private suspend fun monitorConnection(activeSocket: Socket) {
        val readJob = scope.launch {
            val buffer = ByteArray(64)
            runCatching {
                while (shouldRun && activeSocket.isConnected) {
                    val read = input?.read(buffer) ?: -1
                    if (read < 0) break
                }
            }
            if (shouldRun && !activeSocket.isClosed) {
                Log.d(TAG, "read loop ended, closing socket")
                runCatching { activeSocket.close() }
            }
        }

        try {
            var heartbeatTick = 0
            while (shouldRun && !activeSocket.isClosed) {
                delay(1000)
                heartbeatTick++
                if (heartbeatTick % 2 == 0) {
                    sendFrame(byteArrayOf(TYPE_HEARTBEAT))
                }
            }
        } finally {
            readJob.cancel()
            closeSocket()
        }

        if (shouldRun) {
            _state.value = _state.value.copy(
                status = UsbLinkStatus.WAITING,
                detail = "桌面端连接已断开，正在重连"
            )
        }
    }

    fun sendMove(dx: Int, dy: Int) {
        sendFrame(
            byteArrayOf(
                TYPE_MOVE,
                dx.coerceIn(-127, 127).toByte(),
                dy.coerceIn(-127, 127).toByte()
            )
        )
    }

    fun sendScroll(vertical: Int, horizontal: Int) {
        sendFrame(
            byteArrayOf(
                TYPE_SCROLL,
                vertical.coerceIn(-127, 127).toByte(),
                horizontal.coerceIn(-127, 127).toByte()
            )
        )
    }

    fun sendButtons(mask: Int) {
        sendFrame(byteArrayOf(TYPE_BUTTONS, (mask and 0x07).toByte()))
    }

    fun sendKeyboard(modifiers: Int, keys: IntArray) {
        val frame = ByteArray(8)
        frame[0] = TYPE_KEYBOARD
        frame[1] = modifiers.toByte()
        for (i in 0 until 6) {
            frame[i + 2] = (keys.getOrNull(i) ?: 0).toByte()
        }
        sendFrame(frame)
    }

    private fun sendFrame(frame: ByteArray) {
        if (!_state.value.connected) return
        outbound.trySend(frame)
    }

    private fun drainOutbound() {
        while (outbound.tryReceive().isSuccess) {
            // 丢弃断线前残留的输入帧，避免重连后误发。
        }
    }

    private fun closeSocket() {
        synchronized(writeLock) {
            runCatching { output?.close() }
            runCatching { input?.close() }
            runCatching { socket?.close() }
            output = null
            input = null
            socket = null
        }
    }

    private fun humanReadableError(t: Throwable): String {
        return when (t) {
            is java.net.ConnectException -> "连接被拒绝：请先运行桌面桥接程序并执行 adb reverse"
            is java.net.SocketTimeoutException -> "连接超时：请检查 adb reverse 和端口"
            is java.net.UnknownHostException -> "无法解析主机地址：${t.message.orEmpty()}"
            else -> t.message ?: t.javaClass.simpleName
        }
    }

    companion object {
        private const val TAG = "AirTouchUsb"
        const val DEFAULT_PORT = 17575

        // 与 desktop/airtouch_bridge.py 的协议保持一致
        const val TYPE_MOVE: Byte = 0x4D       // 'M'
        const val TYPE_SCROLL: Byte = 0x57     // 'W'
        const val TYPE_BUTTONS: Byte = 0x42     // 'B'
        const val TYPE_KEYBOARD: Byte = 0x4B    // 'K'
        const val TYPE_HEARTBEAT: Byte = 0x48   // 'H'
    }
}
