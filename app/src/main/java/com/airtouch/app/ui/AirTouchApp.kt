package com.airtouch.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airtouch.app.BuildConfig
import com.airtouch.app.MainViewModel
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.model.ThemeSkin
import com.airtouch.app.ui.components.BrandMark
import com.airtouch.app.ui.components.Dot
import com.airtouch.app.ui.screens.ConnectionScreen
import com.airtouch.app.ui.screens.KeyboardScreen
import com.airtouch.app.ui.screens.SettingsScreen
import com.airtouch.app.ui.screens.TrackpadScreen
import com.airtouch.app.ui.theme.AirTouchTheme
import com.airtouch.app.ui.theme.airPalette
import kotlinx.coroutines.launch

private enum class Destination(
    val label: String,
    val caption: String,
    val icon: ImageVector
) {
    TRACKPAD("触控板", "默认 · 手势与光标", Icons.Rounded.TouchApp),
    KEYBOARD("键盘", "全键盘与快捷操作", Icons.Rounded.Keyboard),
    CONNECTION("连接电脑", "蓝牙 HID / USB 桥接", Icons.Rounded.Bluetooth),
    SETTINGS("外观与手感", "主题皮肤与触控设置", Icons.Rounded.Palette)
}

@Composable
fun AirTouchApp(
    viewModel: MainViewModel,
    onRequestBluetoothPermissions: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val bluetooth by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val usb by viewModel.usbState.collectAsStateWithLifecycle()
    val connection by viewModel.connectionSummary.collectAsStateWithLifecycle()
    val activeTransport by viewModel.activeTransport.collectAsStateWithLifecycle()
    val inputReady by viewModel.inputReady.collectAsStateWithLifecycle()

    AirTouchTheme(skin = settings.skin) {
        val palette = airPalette
        ApplySystemBars(palette.isDark)

        var destinationName by rememberSaveable { mutableStateOf(Destination.TRACKPAD.name) }
        val destination = runCatching { Destination.valueOf(destinationName) }
            .getOrDefault(Destination.TRACKPAD)
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        fun navigate(next: Destination) {
            destinationName = next.name
            scope.launch { drawerState.close() }
        }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            scrimColor = Color.Black.copy(alpha = 0.46f),
            drawerContent = {
                AirDrawer(
                    destination = destination,
                    connection = connection,
                    skin = settings.skin,
                    onNavigate = ::navigate
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.backgroundTop,
                                palette.background,
                                palette.background
                            )
                        )
                    )
            ) {
                when (destination) {
                    Destination.TRACKPAD -> TrackpadScreen(
                        settings = settings,
                        connection = connection,
                        inputReady = inputReady,
                        onMenu = { scope.launch { drawerState.open() } },
                        onOpenConnection = { navigate(Destination.CONNECTION) },
                        onOpenSettings = { navigate(Destination.SETTINGS) },
                        onMove = viewModel::move,
                        onScroll = viewModel::scroll,
                        onLeftClick = viewModel::clickLeft,
                        onRightClick = viewModel::clickRight,
                        onMouseDown = viewModel::mouseDown,
                        onMouseUp = viewModel::mouseUp,
                        onAction = viewModel::performAction
                    )

                    Destination.KEYBOARD -> KeyboardScreen(
                        settings = settings,
                        connection = connection,
                        inputReady = inputReady,
                        onMenu = { scope.launch { drawerState.open() } },
                        onOpenConnection = { navigate(Destination.CONNECTION) },
                        onOpenSettings = { navigate(Destination.SETTINGS) },
                        onTapKey = viewModel::tapKey,
                        onAction = viewModel::performAction
                    )

                    Destination.CONNECTION -> ConnectionScreen(
                        settings = settings,
                        connection = connection,
                        activeTransport = activeTransport,
                        bluetooth = bluetooth,
                        usb = usb,
                        onMenu = { scope.launch { drawerState.open() } },
                        onOpenConnection = { navigate(Destination.CONNECTION) },
                        onOpenSettings = { navigate(Destination.SETTINGS) },
                        onRequestBluetoothPermissions = onRequestBluetoothPermissions,
                        onConnectBluetooth = viewModel::connectBluetooth,
                        onDisconnectBluetooth = viewModel::disconnectBluetooth,
                        onRefreshBluetooth = viewModel::onResume,
                        onConnectUsb = { host, port ->
                            viewModel.setUsbEndpoint(host, port)
                            viewModel.connectUsb(host, port)
                        },
                        onDisconnectUsb = viewModel::disconnectUsb,
                        onSetUsbEndpoint = viewModel::setUsbEndpoint,
                        onUseBluetooth = viewModel::useBluetooth,
                        onUseUsb = viewModel::useUsb
                    )

                    Destination.SETTINGS -> SettingsScreen(
                        settings = settings,
                        connection = connection,
                        onMenu = { scope.launch { drawerState.open() } },
                        onOpenConnection = { navigate(Destination.CONNECTION) },
                        onOpenSettings = { navigate(Destination.SETTINGS) },
                        onSkinChanged = viewModel::setSkin,
                        onHostOsChanged = viewModel::setHostOs,
                        onSensitivityChanged = viewModel::setSensitivity,
                        onNaturalScrollChanged = viewModel::setNaturalScroll,
                        onTapToClickChanged = viewModel::setTapToClick,
                        onHapticsChanged = viewModel::setHaptics,
                        onShowTouchPointsChanged = viewModel::setShowTouchPoints
                    )
                }
            }
        }
    }
}

@Composable
private fun AirDrawer(
    destination: Destination,
    connection: ConnectionSummary,
    skin: ThemeSkin,
    onNavigate: (Destination) -> Unit
) {
    val palette = airPalette
    ModalDrawerSheet(
        modifier = Modifier
            .width(314.dp)
            .statusBarsPadding(),
        drawerShape = RoundedCornerShape(topEnd = 30.dp, bottomEnd = 30.dp),
        drawerContainerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 52.dp)
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(
                        text = "AirTouch",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        text = "手机就是 Mac 的触控板",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surfaceRaised)
                    .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                    .clickable { onNavigate(Destination.CONNECTION) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Dot(
                    color = if (connection.connected) palette.success else palette.textTertiary,
                    size = 9.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connection.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = connection.detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "导航",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = palette.textTertiary,
                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
            )

            Destination.entries.forEach { item ->
                val selected = item == destination
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (selected) palette.accentSoft else Color.Transparent)
                        .clickable { onNavigate(item) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) palette.accent else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selected) palette.textPrimary else palette.textSecondary
                        )
                        Text(
                            text = item.caption,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textTertiary
                        )
                    }
                    if (selected) {
                        Dot(color = palette.accent, size = 5.dp)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surfaceRaised.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "当前皮肤 · ${skin.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "本地运行 · 不上传输入数据",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ApplySystemBars(dark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() as? ComponentActivity ?: return@SideEffect
            val translucent = AndroidColor.TRANSPARENT
            val darkScrim = AndroidColor.argb(0x80, 0, 0, 0)
            val statusStyle = if (dark) {
                SystemBarStyle.dark(translucent)
            } else {
                SystemBarStyle.light(translucent, darkScrim)
            }
            val navigationStyle = if (dark) {
                SystemBarStyle.dark(translucent)
            } else {
                SystemBarStyle.light(translucent, darkScrim)
            }
            activity.enableEdgeToEdge(
                statusBarStyle = statusStyle,
                navigationBarStyle = navigationStyle
            )
            val window = activity.window
            @Suppress("DEPRECATION")
            window.statusBarColor = translucent
            @Suppress("DEPRECATION")
            window.navigationBarColor = translucent
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
            window.decorView.post {
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
