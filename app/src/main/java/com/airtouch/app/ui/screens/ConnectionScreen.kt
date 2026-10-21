package com.airtouch.app.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.airtouch.app.input.BluetoothDeviceItem
import com.airtouch.app.input.BluetoothUiState
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.input.UsbLinkStatus
import com.airtouch.app.input.UsbUiState
import com.airtouch.app.model.AppSettings
import com.airtouch.app.model.TransportKind
import com.airtouch.app.ui.components.ActionChip
import com.airtouch.app.ui.components.AirTopBar
import com.airtouch.app.ui.components.DividerLine
import com.airtouch.app.ui.components.Dot
import com.airtouch.app.ui.components.SectionCard
import com.airtouch.app.ui.components.SectionTitle
import com.airtouch.app.ui.components.SegmentedControl
import com.airtouch.app.ui.theme.airPalette

@Composable
fun ConnectionScreen(
    settings: AppSettings,
    connection: ConnectionSummary,
    activeTransport: TransportKind,
    bluetooth: BluetoothUiState,
    usb: UsbUiState,
    onMenu: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestBluetoothPermissions: () -> Unit,
    onConnectBluetooth: (String) -> Unit,
    onDisconnectBluetooth: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    onConnectUsb: (String, Int) -> Unit,
    onDisconnectUsb: () -> Unit,
    onSetUsbEndpoint: (String, Int) -> Unit,
    onUseBluetooth: () -> Unit,
    onUseUsb: () -> Unit
) {
    val palette = airPalette
    var tab by rememberSaveable(activeTransport) {
        mutableStateOf(activeTransport)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        AirTopBar(
            title = "连接电脑",
            subtitle = "AirTouch · Connection",
            connection = connection,
            onMenu = onMenu,
            onOpenConnection = onOpenConnection,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SegmentedControl(
                options = listOf(
                    TransportKind.BLUETOOTH to "蓝牙 HID",
                    TransportKind.USB to "USB 桥接"
                ),
                selected = tab,
                onSelected = { tab = it }
            )

            when (tab) {
                TransportKind.BLUETOOTH -> BluetoothPanel(
                    settings = settings,
                    state = bluetooth,
                    active = activeTransport == TransportKind.BLUETOOTH,
                    onRequestPermissions = onRequestBluetoothPermissions,
                    onConnect = onConnectBluetooth,
                    onDisconnect = onDisconnectBluetooth,
                    onRefresh = onRefreshBluetooth,
                    onUse = onUseBluetooth
                )

                TransportKind.USB -> UsbPanel(
                    settings = settings,
                    state = usb,
                    active = activeTransport == TransportKind.USB,
                    onConnect = onConnectUsb,
                    onDisconnect = onDisconnectUsb,
                    onSetEndpoint = onSetUsbEndpoint,
                    onUse = onUseUsb
                )
            }

            SectionTitle(
                title = "工作方式",
                caption = "两种连接可以同时保留，但同一时间只向当前通道发送输入"
            )
            SectionCard {
                InfoLine(
                    title = "蓝牙",
                    body = "手机直接模拟标准 HID 触控板 + 键盘。电脑无需安装软件，适合 macOS / Windows。"
                )
                Spacer(Modifier.height(12.dp))
                DividerLine()
                Spacer(Modifier.height(12.dp))
                InfoLine(
                    title = "USB",
                    body = "通过 adb reverse 建立本地 TCP 通道，由开源桌面桥接注入系统事件。无需 Root。"
                )
            }

            Text(
                text = "所有输入只在本机与电脑之间传输。蓝牙 HID 不经过网络；USB 桥接默认只监听 127.0.0.1。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun BluetoothPanel(
    settings: AppSettings,
    state: BluetoothUiState,
    active: Boolean,
    onRequestPermissions: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onUse: () -> Unit
) {
    val palette = airPalette
    val context = LocalContext.current

    ConnectionHero(
        icon = Icons.Rounded.Bluetooth,
        title = "蓝牙 HID",
        subtitle = "无需电脑端软件 · 标准触控板与键盘",
        connected = state.connected,
        active = active,
        statusText = when {
            state.connected -> state.connectedName ?: "已连接"
            state.registering -> "正在注册 HID…"
            state.hidRegistered -> "HID 已就绪 · 等待连接"
            else -> "尚未就绪"
        }
    )

    if (!state.permissionGranted) {
        NoticeBanner(
            title = "需要蓝牙权限",
            body = "Android 12+ 需要“附近的设备”权限，才能注册 HID 并连接电脑。",
            actionLabel = "授予权限",
            onAction = onRequestPermissions
        )
    } else if (!state.adapterEnabled) {
        NoticeBanner(
            title = "蓝牙未开启",
            body = "打开蓝牙后，AirTouch 会注册成组合键鼠设备。",
            actionLabel = "打开蓝牙",
            onAction = {
                runCatching {
                    context.startActivity(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
    }

    if (state.error != null) {
        NoticeBanner(
            title = "连接提示",
            body = state.error,
            warning = true
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChip(label = "刷新设备", icon = Icons.Rounded.Refresh, onClick = onRefresh)
        ActionChip(
            label = "让电脑发现本机",
            icon = Icons.Rounded.Bluetooth,
            onClick = {
                if (!state.permissionGranted) {
                    onRequestPermissions()
                } else {
                    runCatching {
                        context.startActivity(
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        )
        ActionChip(
            label = "系统蓝牙设置",
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        )
        if (state.connected && active) {
            ActionChip(label = "当前输入通道", icon = Icons.Rounded.Check, onClick = onUse)
        } else if (state.connected) {
            ActionChip(label = "切换到蓝牙", icon = Icons.Rounded.Check, onClick = onUse)
        }
    }

    SectionTitle(
        title = "已配对设备",
        caption = "先在系统蓝牙中与电脑配对，再回到这里连接"
    )

    SectionCard(padding = 14.dp) {
        if (state.bondedDevices.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "还没有已配对设备",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "点击上方“让电脑发现本机”，然后在电脑蓝牙设置中配对。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary
                )
            }
        } else {
            state.bondedDevices.forEachIndexed { index, device ->
                DeviceRow(
                    device = device,
                    connected = state.connectedAddress == device.address,
                    connecting = state.connectingName == device.name,
                    enabled = state.hidRegistered,
                    onConnect = { onConnect(device.address) },
                    onDisconnect = onDisconnect
                )
                if (index != state.bondedDevices.lastIndex) {
                    DividerLine(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }

    SectionTitle(title = "配对步骤")
    SectionCard {
        StepRow("1", "打开电脑的蓝牙设置，保持可被发现。")
        StepRow("2", "在手机上点击“让电脑发现本机”。")
        StepRow("3", "在电脑上选择这部手机并完成配对。")
        StepRow("4", "回到 AirTouch，点击电脑名称右侧的“连接”。")
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (settings.hostOs == com.airtouch.app.model.HostOs.MACOS) {
                "macOS 提示：如果 AirTouch 没有出现在蓝牙列表，请删除电脑上的旧配对记录，关闭再打开蓝牙后重试。"
            } else {
                "Windows 提示：在“蓝牙和其他设备”中添加设备；若连接不稳定，先删除旧设备记录再重新配对。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = palette.textTertiary
        )
    }
}

@Composable
private fun UsbPanel(
    settings: AppSettings,
    state: UsbUiState,
    active: Boolean,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onSetEndpoint: (String, Int) -> Unit,
    onUse: () -> Unit
) {
    val palette = airPalette
    var host by rememberSaveable(settings.usbHost) { mutableStateOf(settings.usbHost) }
    var port by rememberSaveable(settings.usbPort) { mutableStateOf(settings.usbPort.toString()) }

    ConnectionHero(
        icon = Icons.Rounded.Usb,
        title = "USB 桥接",
        subtitle = "ADB reverse 本地转发 · 无需 Root",
        connected = state.connected,
        active = active,
        statusText = when (state.status) {
            UsbLinkStatus.CONNECTED -> state.detail ?: "已连接"
            UsbLinkStatus.CONNECTING -> "正在连接…"
            UsbLinkStatus.WAITING -> "等待桌面端…"
            UsbLinkStatus.ERROR -> "连接失败"
            UsbLinkStatus.IDLE -> "尚未连接"
        }
    )

    if (state.error != null && state.status != UsbLinkStatus.CONNECTED) {
        NoticeBanner(
            title = "USB 连接提示",
            body = state.error,
            warning = true
        )
    }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("主机") },
                singleLine = true,
                modifier = Modifier.weight(1.7f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = airTextFieldColors()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { value ->
                    if (value.length <= 5 && value.all { it.isDigit() }) port = value
                },
                label = { Text("端口") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = airTextFieldColors()
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "使用 adb reverse 时保持 127.0.0.1:17575；使用 USB 网络共享时改成电脑 IP。",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textTertiary
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = if (state.connected) "重新连接" else "保存并连接",
                enabled = host.isNotBlank() && port.toIntOrNull() != null,
                modifier = Modifier.weight(1.4f)
            ) {
                val parsedPort = port.toIntOrNull()?.coerceIn(1, 65535) ?: 17575
                onSetEndpoint(host.trim(), parsedPort)
                onConnect(host.trim(), parsedPort)
            }
            SecondaryButton(
                text = "断开",
                enabled = state.status != UsbLinkStatus.IDLE,
                modifier = Modifier.weight(1f),
                onClick = onDisconnect
            )
        }
        if (state.connected) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(color = palette.success)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = state.detail ?: "已连接",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
                Spacer(Modifier.weight(1f))
                if (active) {
                    Text(
                        text = "当前输入通道",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.success
                    )
                } else {
                    ActionChip(label = "切换到此通道", onClick = onUse)
                }
            }
        }
    }

    SectionTitle(
        title = "三步启用 USB",
        caption = "需要开启手机的开发者选项 → USB 调试"
    )
    SectionCard {
        StepRow("1", "用数据线连接手机与电脑，并在手机上允许 USB 调试。")
        StepRow("2", "在电脑终端执行下面的 adb reverse 命令。")
        StepRow("3", "运行桌面桥接脚本，然后在上方点击“保存并连接”。")
        Spacer(Modifier.height(10.dp))
        CommandRow("adb devices")
        Spacer(Modifier.height(8.dp))
        CommandRow("adb reverse tcp:17575 tcp:17575")
        Spacer(Modifier.height(8.dp))
        CommandRow("python3 desktop/airtouch_bridge.py")
        Spacer(Modifier.height(10.dp))
        Text(
            text = "命令需在工程根目录执行。桥接脚本跨平台，macOS 需要授予终端“辅助功能”权限。",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textTertiary
        )
    }
}

@Composable
private fun ConnectionHero(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    connected: Boolean,
    active: Boolean,
    statusText: String
) {
    val palette = airPalette
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(palette.accentSoft)
                    .border(1.dp, palette.accent.copy(alpha = 0.35f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    if (active) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "当前",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accentSoft)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceRaised.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dot(color = if (connected) palette.success else palette.textTertiary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = if (connected) palette.success else palette.textSecondary,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            if (active) {
                Text(
                    text = "输入通道",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDeviceItem,
    connected: Boolean,
    connecting: Boolean,
    enabled: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val palette = airPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(palette.surfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Computer,
                contentDescription = null,
                tint = if (connected) palette.accent else palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary,
                maxLines = 1
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                maxLines = 1
            )
        }
        when {
            connected -> SecondaryButton(
                text = "断开",
                enabled = true,
                onClick = onDisconnect
            )

            connecting -> Text(
                text = "连接中…",
                style = MaterialTheme.typography.labelMedium,
                color = palette.warning
            )

            else -> PrimaryButton(
                text = "连接",
                enabled = enabled,
                compact = true,
                onClick = onConnect
            )
        }
    }
}

@Composable
private fun NoticeBanner(
    title: String,
    body: String,
    warning: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val palette = airPalette
    val accent = if (warning) palette.warning else palette.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.09f))
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color = accent, size = 7.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                text = actionLabel,
                enabled = true,
                compact = true,
                onClick = onAction
            )
        }
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    val palette = airPalette
    Row(
        modifier = Modifier.padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(palette.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentStrong
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CommandRow(command: String) {
    val palette = airPalette
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.background)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = command,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = palette.accentStrong,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(command))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "复制命令",
                tint = palette.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val palette = airPalette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) palette.accent else palette.surfaceRaised)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 14.dp else 16.dp,
                vertical = if (compact) 8.dp else 12.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) palette.background else palette.textTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    onClick: () -> Unit
) {
    val palette = airPalette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 14.dp else 16.dp,
                vertical = if (compact) 8.dp else 12.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) palette.textSecondary else palette.textTertiary,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    val palette = airPalette
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textTertiary
        )
    }
}

@Composable
private fun airTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = airPalette.textPrimary,
    unfocusedTextColor = airPalette.textPrimary,
    focusedBorderColor = airPalette.accent,
    unfocusedBorderColor = airPalette.border,
    focusedLabelColor = airPalette.accent,
    unfocusedLabelColor = airPalette.textTertiary,
    cursorColor = airPalette.accent,
    focusedContainerColor = airPalette.surfaceRaised,
    unfocusedContainerColor = airPalette.surfaceRaised
)
