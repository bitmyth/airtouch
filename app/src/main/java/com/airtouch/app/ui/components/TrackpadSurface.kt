package com.airtouch.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.input.SystemAction
import com.airtouch.app.model.AppSettings
import com.airtouch.app.ui.theme.airPalette
import kotlin.math.abs
import kotlin.math.max

private data class TrackpadCallbacks(
    val onMove: (Float, Float) -> Unit,
    val onScroll: (Float, Float) -> Unit,
    val onLeftClick: () -> Unit,
    val onRightClick: () -> Unit,
    val onMouseDown: () -> Unit,
    val onMouseUp: () -> Unit,
    val onAction: (SystemAction) -> Unit,
    val onHaptic: () -> Unit
)

@Composable
fun TrackpadSurface(
    settings: AppSettings,
    connection: ConnectionSummary,
    ready: Boolean,
    modifier: Modifier = Modifier,
    onMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMouseDown: () -> Unit,
    onMouseUp: () -> Unit,
    onAction: (SystemAction) -> Unit
) {
    val palette = airPalette
    val haptic = LocalHapticFeedback.current
    var touchPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var pointerCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    val callbacks = rememberUpdatedState(
        TrackpadCallbacks(
            onMove = onMove,
            onScroll = onScroll,
            onLeftClick = onLeftClick,
            onRightClick = onRightClick,
            onMouseDown = onMouseDown,
            onMouseUp = onMouseUp,
            onAction = onAction,
            onHaptic = {
                if (settings.haptics) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )
    )
    val liveSettings = rememberUpdatedState(settings)

    val shape = RoundedCornerShape(34.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.surfaceRaised,
                        palette.surface,
                        palette.backgroundTop
                    )
                )
            )
            .border(1.dp, palette.borderStrong.copy(alpha = 0.7f), shape)
            .pointerInput(Unit) {
                val tapSlop = 16.dp.toPx()
                val swipeThreshold = 62.dp.toPx()
                val doubleTapWindow = 340L

                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val downTime = first.uptimeMillis
                    val startPosition = first.position
                    val gestureSettings = liveSettings.value

                    var dragMode = false
                    if (
                        gestureSettings.tapToClick &&
                        downTime - lastTapTime in 30..doubleTapWindow &&
                        (startPosition - lastTapPosition).getDistance() <= tapSlop * 2.2f
                    ) {
                        dragMode = true
                        lastTapTime = 0L
                        callbacks.value.onMouseDown()
                        callbacks.value.onHaptic()
                    }

                    var maxPointers = 1
                    var lastPointerCount = 1
                    var priorCentroid = startPosition
                    var totalMovement = 0f
                    var eventTime = downTime
                    var threeFingerStart: Offset? = null
                    var threeFingerLast: Offset? = null
                    var pinchBaseDistance = 0f
                    var pinchLastScale = 1f
                    var pinchActive = false

                    touchPoints = listOf(startPosition)
                    pointerCount = 1

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        event.changes.maxOfOrNull { it.uptimeMillis }?.let { eventTime = it }

                        if (pressed.isEmpty()) {
                            event.changes.forEach { it.consume() }
                            break
                        }

                        val count = pressed.size
                        val centroid = pressed.fold(Offset.Zero) { acc, change ->
                            acc + change.position
                        } / count.toFloat()

                        if (count != lastPointerCount) {
                            lastPointerCount = count
                            priorCentroid = centroid
                            maxPointers = max(maxPointers, count)
                            if (count == 2 && maxPointers == 2) {
                                pinchBaseDistance = distanceBetween(pressed[0].position, pressed[1].position)
                                pinchLastScale = 1f
                                pinchActive = false
                            }
                            if (count == 3) {
                                threeFingerStart = centroid
                                threeFingerLast = centroid
                            }
                            touchPoints = pressed.map { it.position }
                            pointerCount = count
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        val delta = centroid - priorCentroid

                        when {
                            count == 1 && maxPointers == 1 -> {
                                if (delta.getDistance() > 0f) {
                                    totalMovement += delta.getDistance()
                                    callbacks.value.onMove(delta.x, delta.y)
                                }
                            }

                            count == 2 && maxPointers == 2 -> {
                                totalMovement += delta.getDistance()
                                val fingerDistance = distanceBetween(
                                    pressed[0].position,
                                    pressed[1].position
                                )
                                if (pinchBaseDistance <= 0f) pinchBaseDistance = fingerDistance
                                val scale = if (pinchBaseDistance > 0f) {
                                    fingerDistance / pinchBaseDistance
                                } else {
                                    1f
                                }

                                if (!pinchActive && abs(scale - 1f) > 0.20f) {
                                    pinchActive = true
                                    pinchLastScale = scale
                                    callbacks.value.onAction(
                                        if (scale > 1f) SystemAction.ZOOM_IN else SystemAction.ZOOM_OUT
                                    )
                                    callbacks.value.onHaptic()
                                } else if (pinchActive && abs(scale - pinchLastScale) > 0.17f) {
                                    val zoomIn = scale > pinchLastScale
                                    pinchLastScale = scale
                                    callbacks.value.onAction(
                                        if (zoomIn) SystemAction.ZOOM_IN else SystemAction.ZOOM_OUT
                                    )
                                    callbacks.value.onHaptic()
                                }

                                if (!pinchActive) {
                                    val scrollX = if (gestureSettings.naturalScroll) delta.x else -delta.x
                                    val scrollY = if (gestureSettings.naturalScroll) delta.y else -delta.y
                                    callbacks.value.onScroll(scrollX, scrollY)
                                }
                            }

                            count >= 3 -> {
                                totalMovement += delta.getDistance()
                                if (count == 3) {
                                    threeFingerLast = centroid
                                }
                            }
                        }

                        priorCentroid = centroid
                        maxPointers = max(maxPointers, count)
                        touchPoints = pressed.map { it.position }
                        pointerCount = count
                        event.changes.forEach { it.consume() }
                    }

                    touchPoints = emptyList()
                    pointerCount = 0
                    val duration = eventTime - downTime

                    if (dragMode) {
                        callbacks.value.onMouseUp()
                        callbacks.value.onHaptic()
                        return@awaitEachGesture
                    }

                    if (maxPointers >= 3) {
                        val start = threeFingerStart
                        val end = threeFingerLast
                        if (start != null && end != null) {
                            val dx = end.x - start.x
                            val dy = end.y - start.y
                            if (max(abs(dx), abs(dy)) > swipeThreshold) {
                                val action = if (abs(dx) > abs(dy)) {
                                    if (dx > 0) SystemAction.SPACE_RIGHT else SystemAction.SPACE_LEFT
                                } else {
                                    if (dy < 0) SystemAction.MISSION_CONTROL else SystemAction.APP_EXPOSE
                                }
                                callbacks.value.onAction(action)
                                callbacks.value.onHaptic()
                            }
                        }
                        return@awaitEachGesture
                    }

                    if (duration <= 330L && totalMovement <= tapSlop) {
                        when (maxPointers) {
                            1 -> {
                                if (gestureSettings.tapToClick) {
                                    callbacks.value.onLeftClick()
                                    callbacks.value.onHaptic()
                                    lastTapTime = eventTime
                                    lastTapPosition = startPosition
                                }
                            }

                            2 -> {
                                callbacks.value.onRightClick()
                                callbacks.value.onHaptic()
                            }
                        }
                    }
                }
            }
    ) {
        // 背景纹理与光晕
        Canvas(Modifier.fillMaxSize()) {
            val dotGap = 30.dp.toPx()
            val dotRadius = 0.9.dp.toPx()
            var x = dotGap / 2f
            while (x < size.width) {
                var y = dotGap / 2f
                while (y < size.height) {
                    drawCircle(
                        color = palette.textTertiary.copy(alpha = 0.17f),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += dotGap
                }
                x += dotGap
            }

            val glowCenter = Offset(size.width * 0.82f, size.height * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = size.width * 0.62f
                ),
                radius = size.width * 0.62f,
                center = glowCenter
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.88f),
                    radius = size.width * 0.50f
                ),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.15f, size.height * 0.88f)
            )
        }

        // 触摸可视化
        if (settings.showTouchPoints) Canvas(Modifier.fillMaxSize()) {
            touchPoints.forEachIndexed { index, point ->
                val alpha = if (index == 0) 1f else 0.72f
                drawCircle(
                    color = palette.accent.copy(alpha = 0.10f * alpha),
                    radius = 46.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = palette.accent.copy(alpha = 0.34f * alpha),
                    radius = 20.dp.toPx(),
                    center = point,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = palette.accent.copy(alpha = 0.92f * alpha),
                    radius = 5.dp.toPx(),
                    center = point
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRACKPAD",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = palette.textTertiary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (ready) palette.success else palette.textTertiary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (ready) connection.detail else "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                color = if (ready) palette.textSecondary else palette.textTertiary,
                maxLines = 1
            )
        }

        AnimatedVisibility(
            visible = pointerCount == 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BrandMark(size = 62.dp)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = if (ready) "整块屏幕都是触控板" else "尚未连接电脑",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (ready) {
                        "单指移动光标 · 轻点左键"
                    } else {
                        "点击右上角状态，配置蓝牙 HID 或 USB"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HintDot()
            Text(
                text = "轻点  左键",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary
            )
            Spacer(Modifier.width(14.dp))
            HintDot()
            Text(
                text = "双指  滚动",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary
            )
            Spacer(Modifier.width(14.dp))
            HintDot()
            Text(
                text = "三指  切换",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary
            )
        }
    }
}

@Composable
private fun HintDot() {
    val palette = airPalette
    Box(
        Modifier
            .size(4.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.textTertiary.copy(alpha = 0.6f))
    )
    Spacer(Modifier.width(6.dp))
}

private fun distanceBetween(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
