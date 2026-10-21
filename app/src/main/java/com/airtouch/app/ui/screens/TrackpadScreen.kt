package com.airtouch.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.input.SystemAction
import com.airtouch.app.model.AppSettings
import com.airtouch.app.ui.components.AirTopBar
import com.airtouch.app.ui.components.TrackpadSurface
import com.airtouch.app.ui.theme.airPalette

@Composable
fun TrackpadScreen(
    settings: AppSettings,
    connection: ConnectionSummary,
    inputReady: Boolean,
    onMenu: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMouseDown: () -> Unit,
    onMouseUp: () -> Unit,
    onAction: (SystemAction) -> Unit
) {
    val palette = airPalette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        AirTopBar(
            title = "触控板",
            subtitle = "AirTouch · Trackpad",
            connection = connection,
            onMenu = onMenu,
            onOpenConnection = onOpenConnection,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "触控区域",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(palette.textTertiary.copy(alpha = 0.6f))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (inputReady) {
                        connection.detail
                    } else {
                        "未连接 · 点击右上角状态"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "灵敏度 ${"%.2f".format(settings.sensitivity)}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary
                )
            }

            Spacer(Modifier.height(10.dp))

            TrackpadSurface(
                settings = settings,
                connection = connection,
                ready = inputReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onMove = onMove,
                onScroll = onScroll,
                onLeftClick = onLeftClick,
                onRightClick = onRightClick,
                onMouseDown = onMouseDown,
                onMouseUp = onMouseUp,
                onAction = onAction
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GestureHint("单指移动")
                GestureHint("轻点左键")
                GestureHint("双指滚动")
                GestureHint("双指轻点右键")
                GestureHint("捏合缩放")
                GestureHint("三指上下 / 左右")
                GestureHint("双击拖动")
            }
        }
    }
}

@Composable
private fun GestureHint(text: String) {
    val palette = airPalette
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(palette.surface.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.8f))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textTertiary,
            maxLines = 1
        )
    }
}
