package com.airtouch.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.ui.theme.airPalette
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings

@Composable
fun AirTopBar(
    title: String,
    subtitle: String,
    connection: ConnectionSummary,
    onMenu: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = airPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceRaised)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "打开菜单",
                tint = palette.textPrimary,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                maxLines = 1
            )
        }

        StatusPill(
            connection = connection,
            onClick = onOpenConnection
        )

        Spacer(Modifier.width(4.dp))

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "设置",
                tint = palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StatusPill(
    connection: ConnectionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = airPalette
    val dotColor by animateColorAsState(
        targetValue = if (connection.connected) palette.success else palette.textTertiary,
        label = "statusDot"
    )
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(palette.surfaceRaised.copy(alpha = 0.92f))
            .border(1.dp, palette.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = if (connection.connected) "已连接" else "未连接",
            style = MaterialTheme.typography.labelSmall,
            color = if (connection.connected) palette.textPrimary else palette.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val palette = airPalette
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.95f),
                        palette.accentStrong.copy(alpha = 0.65f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.58f)) {
            val stroke = Stroke(width = this.size.width * 0.075f, cap = StrokeCap.Round)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(this.size.width * 0.08f, 0f),
                size = Size(this.size.width * 0.78f, this.size.height * 0.92f),
                cornerRadius = CornerRadius(this.size.width * 0.14f),
                style = stroke
            )
            drawCircle(
                color = Color.White,
                radius = this.size.width * 0.075f,
                center = Offset(this.size.width * 0.05f, this.size.height * 0.52f)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.9f),
                startAngle = 130f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(this.size.width * 0.20f, this.size.height * 0.34f),
                size = Size(this.size.width * 0.44f, this.size.height * 0.40f),
                style = Stroke(width = this.size.width * 0.065f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val palette = airPalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(24.dp))
            .padding(padding)
    ) {
        content()
    }
}

@Composable
fun SectionTitle(
    title: String,
    caption: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = airPalette
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.textSecondary,
            letterSpacing = 0.7.sp
        )
        if (caption != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary
            )
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = airPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) palette.textPrimary else palette.textTertiary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textTertiary,
                uncheckedTrackColor = palette.surfaceRaised,
                uncheckedBorderColor = palette.borderStrong
            )
        )
    }
}

@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = airPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) palette.accentSoft else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) palette.accentStrong else palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val palette = airPalette
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    val palette = airPalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.border.copy(alpha = 0.8f))
    )
}

@Composable
fun Dot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
