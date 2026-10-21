package com.airtouch.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airtouch.app.BuildConfig
import com.airtouch.app.data.SettingsRepository
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.model.AppSettings
import com.airtouch.app.model.HostOs
import com.airtouch.app.model.ThemeSkin
import com.airtouch.app.ui.components.ActionChip
import com.airtouch.app.ui.components.AirTopBar
import com.airtouch.app.ui.components.BrandMark
import com.airtouch.app.ui.components.DividerLine
import com.airtouch.app.ui.components.SectionCard
import com.airtouch.app.ui.components.SectionTitle
import com.airtouch.app.ui.components.SegmentedControl
import com.airtouch.app.ui.components.ToggleRow
import com.airtouch.app.ui.theme.airPalette
import com.airtouch.app.ui.theme.paletteFor
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    connection: ConnectionSummary,
    onMenu: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onSkinChanged: (ThemeSkin) -> Unit,
    onHostOsChanged: (HostOs) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onNaturalScrollChanged: (Boolean) -> Unit,
    onTapToClickChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onShowTouchPointsChanged: (Boolean) -> Unit
) {
    val palette = airPalette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        AirTopBar(
            title = "外观与手感",
            subtitle = "AirTouch · Settings",
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
                .padding(top = 4.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text(
                    text = "皮肤主题",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "四套精心调校的界面皮肤，立即生效并自动保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary
                )
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeSkin.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { skin ->
                                ThemeCard(
                                    skin = skin,
                                    selected = skin == settings.skin,
                                    onClick = { onSkinChanged(skin) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            SectionTitle(
                title = "目标电脑",
                caption = "决定 ⌘ / Ctrl 与手势快捷键的映射方式"
            )

            SectionCard {
                SegmentedControl(
                    options = listOf(
                        HostOs.MACOS to "macOS",
                        HostOs.WINDOWS to "Windows"
                    ),
                    selected = settings.hostOs,
                    onSelected = onHostOsChanged
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (settings.hostOs == HostOs.MACOS) {
                        "三指切换使用 Control + 方向键，复制粘贴使用 Command。"
                    } else {
                        "三指切换使用 Win + Tab / Ctrl + Win + 方向键，复制粘贴使用 Ctrl。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary
                )
            }

            SectionTitle(
                title = "触控手感",
                caption = "直接影响触控板的光标、滚动与反馈"
            )

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "光标灵敏度",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "适合大屏或高分辨率显示器",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textTertiary
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f×", settings.sensitivity),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentStrong,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionChip(label = "重置") { onSensitivityChanged(1.15f) }
                }
                Slider(
                    value = settings.sensitivity,
                    onValueChange = onSensitivityChanged,
                    valueRange = SettingsRepository.MIN_SENSITIVITY..SettingsRepository.MAX_SENSITIVITY,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.surfaceRaised
                    )
                )
                DividerLine()
                ToggleRow(
                    title = "自然滚动",
                    description = "双指向上滑动时内容向上移动",
                    checked = settings.naturalScroll,
                    onCheckedChange = onNaturalScrollChanged
                )
                DividerLine()
                ToggleRow(
                    title = "轻点即点击",
                    description = "单指轻点发送左键；双击后拖动可拖拽",
                    checked = settings.tapToClick,
                    onCheckedChange = onTapToClickChanged
                )
                DividerLine()
                ToggleRow(
                    title = "触觉反馈",
                    description = "点击、手势与按键时振动",
                    checked = settings.haptics,
                    onCheckedChange = onHapticsChanged
                )
                DividerLine()
                ToggleRow(
                    title = "显示触摸点",
                    description = "在触控板上显示手指位置",
                    checked = settings.showTouchPoints,
                    onCheckedChange = onShowTouchPointsChanged
                )
            }

            SectionTitle(
                title = "关于",
                caption = "AirTouch 完全在本地工作，不收集任何输入数据"
            )

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AirTouch",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "版本 ${BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.RELEASE}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textTertiary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                DividerLine()
                Spacer(Modifier.height(14.dp))
                InfoLine("蓝牙", "Bluetooth HID Device · 无需电脑端软件")
                InfoLine("USB", "ADB reverse + 桌面桥接 · 无需 Root")
                InfoLine("隐私", "无网络上传 · 无统计 SDK")
                InfoLine("当前主题", settings.skin.label)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val palette = airPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textTertiary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThemeCard(
    skin: ThemeSkin,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteFor(skin)
    val outerShape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .height(138.dp)
            .clip(outerShape)
            .background(
                Brush.linearGradient(
                    listOf(palette.backgroundTop, palette.background)
                )
            )
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) palette.accent else palette.border,
                shape = outerShape
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(palette.surfaceRaised, palette.surface)
                    )
                )
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.85f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(palette.textSecondary.copy(alpha = 0.55f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(palette.accentSoft)
                    .border(1.dp, palette.accent.copy(alpha = 0.5f), CircleShape)
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = skin.label,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "已选中",
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = skin.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textTertiary,
            maxLines = 1
        )
    }
}
