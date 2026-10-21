package com.airtouch.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airtouch.app.input.ConnectionSummary
import com.airtouch.app.input.HidKey
import com.airtouch.app.input.HidModifier
import com.airtouch.app.input.SystemAction
import com.airtouch.app.model.AppSettings
import com.airtouch.app.model.HostOs
import com.airtouch.app.ui.components.ActionChip
import com.airtouch.app.ui.components.AirTopBar
import com.airtouch.app.ui.theme.airPalette

@Composable
fun KeyboardScreen(
    settings: AppSettings,
    connection: ConnectionSummary,
    inputReady: Boolean,
    onMenu: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenSettings: () -> Unit,
    onTapKey: (modifiers: Int, key: Int) -> Unit,
    onAction: (SystemAction) -> Unit
) {
    val palette = airPalette
    val haptic = LocalHapticFeedback.current
    val compact = LocalConfiguration.current.screenHeightDp < 600
    var shift by rememberSaveable { mutableStateOf(false) }
    var control by rememberSaveable { mutableStateOf(false) }
    var option by rememberSaveable { mutableStateOf(false) }
    var command by rememberSaveable { mutableStateOf(false) }
    var symbols by rememberSaveable { mutableStateOf(false) }

    fun hapticTick(strong: Boolean = false) {
        if (settings.haptics) {
            haptic.performHapticFeedback(
                if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }

    fun activeModifiers(): Int {
        var modifiers = 0
        if (control) modifiers = modifiers or HidModifier.LEFT_CTRL
        if (shift) modifiers = modifiers or HidModifier.LEFT_SHIFT
        if (option) modifiers = modifiers or HidModifier.LEFT_ALT
        if (command) modifiers = modifiers or HidModifier.LEFT_GUI
        return modifiers
    }

    fun clearOneShotModifiers() {
        shift = false
        control = false
        option = false
        command = false
    }

    fun sendKey(usage: Int) {
        onTapKey(activeModifiers(), usage)
        clearOneShotModifiers()
        hapticTick()
    }

    fun sendSystemAction(action: SystemAction) {
        onAction(action)
        hapticTick(strong = true)
    }

    val modifierLabel = if (settings.hostOs == HostOs.MACOS) "⌘" else "Win"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        AirTopBar(
            title = "键盘",
            subtitle = "AirTouch · Keyboard",
            connection = connection,
            onMenu = onMenu,
            onOpenConnection = onOpenConnection,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .padding(top = 2.dp, bottom = 14.dp)
        ) {
            if (!compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(palette.surfaceRaised)
                            .border(1.dp, palette.border, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (inputReady) "键入到 ${connection.detail}" else "尚未连接电脑",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "修饰键粘滞：先点亮 ⌃ ⌥ ⌘ ⇧，再按目标键",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textTertiary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip(label = "esc") { sendKey(HidKey.ESCAPE) }
                ActionChip(label = "tab") { sendKey(HidKey.TAB) }
                ActionChip(label = "caps") { sendKey(HidKey.CAPS_LOCK) }
                ActionChip(label = "调度中心") {
                    sendSystemAction(SystemAction.MISSION_CONTROL)
                }
                ActionChip(label = "显示桌面") {
                    sendSystemAction(SystemAction.SHOW_DESKTOP)
                }
                ActionChip(label = "截图") { sendSystemAction(SystemAction.SCREENSHOT) }
                ActionChip(label = "复制") { sendSystemAction(SystemAction.COPY) }
                ActionChip(label = "粘贴") { sendSystemAction(SystemAction.PASTE) }
                ActionChip(label = "撤销") { sendSystemAction(SystemAction.UNDO) }
                ActionChip(label = "全选") { sendSystemAction(SystemAction.SELECT_ALL) }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModifierIndicator("⌃", control)
                ModifierIndicator("⌥", option)
                ModifierIndicator(modifierLabel, command)
                ModifierIndicator("⇧", shift)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = if (activeModifiers() == 0) {
                        "未启用修饰键"
                    } else {
                        "已锁定 ${describeModifiers(control, option, command, shift, settings.hostOs)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary
                )
            }

            if (!compact) {
                KeyboardStage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 14.dp)
                        .heightIn(min = 104.dp),
                    modifierLabel = modifierLabel,
                    control = control,
                    option = option,
                    command = command,
                    shift = shift,
                    connection = connection,
                    inputReady = inputReady
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val keyHeight: Dp = when {
                    compact -> 34.dp
                    maxWidth >= 560.dp -> 56.dp
                    else -> 48.dp
                }
                val gap = if (compact) 3.dp else 5.dp

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    if (!symbols) {
                        LetterKeyboard(
                            shift = shift,
                            control = control,
                            option = option,
                            command = command,
                            modifierLabel = modifierLabel,
                            keyHeight = keyHeight,
                            gap = gap,
                            onShift = { shift = !shift; hapticTick() },
                            onControl = { control = !control; hapticTick() },
                            onOption = { option = !option; hapticTick() },
                            onCommand = { command = !command; hapticTick() },
                            onPage = { symbols = true; hapticTick() },
                            onKey = ::sendKey
                        )
                    } else {
                        SymbolKeyboard(
                            shift = shift,
                            control = control,
                            option = option,
                            command = command,
                            modifierLabel = modifierLabel,
                            keyHeight = keyHeight,
                            gap = gap,
                            onShift = { shift = !shift; hapticTick() },
                            onControl = { control = !control; hapticTick() },
                            onOption = { option = !option; hapticTick() },
                            onCommand = { command = !command; hapticTick() },
                            onPage = { symbols = false; hapticTick() },
                            onKey = ::sendKey
                        )
                    }
                }
            }

            if (!compact) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (settings.hostOs == HostOs.MACOS) {
                        "快捷键按 macOS 映射；键盘布局使用 US / ABC 键位"
                    } else {
                        "快捷键按 Windows 映射；Win 键对应 ⌘ 修饰键"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LetterKeyboard(
    shift: Boolean,
    control: Boolean,
    option: Boolean,
    command: Boolean,
    modifierLabel: String,
    keyHeight: Dp,
    gap: Dp,
    onShift: () -> Unit,
    onControl: () -> Unit,
    onOption: () -> Unit,
    onCommand: () -> Unit,
    onPage: () -> Unit,
    onKey: (Int) -> Unit
) {
    val rows = listOf(
        listOf(
            KeyDef("q", HidKey.Q), KeyDef("w", HidKey.W), KeyDef("e", HidKey.E),
            KeyDef("r", HidKey.R), KeyDef("t", HidKey.T), KeyDef("y", HidKey.Y),
            KeyDef("u", HidKey.U), KeyDef("i", HidKey.I), KeyDef("o", HidKey.O),
            KeyDef("p", HidKey.P)
        ),
        listOf(
            KeyDef("a", HidKey.A), KeyDef("s", HidKey.S), KeyDef("d", HidKey.D),
            KeyDef("f", HidKey.F), KeyDef("g", HidKey.G), KeyDef("h", HidKey.H),
            KeyDef("j", HidKey.J), KeyDef("k", HidKey.K), KeyDef("l", HidKey.L),
            KeyDef("'", HidKey.APOSTROPHE)
        ),
        listOf(
            KeyDef("⇧", weight = 1.45f, special = true),
            KeyDef("z", HidKey.Z), KeyDef("x", HidKey.X), KeyDef("c", HidKey.C),
            KeyDef("v", HidKey.V), KeyDef("b", HidKey.B), KeyDef("n", HidKey.N),
            KeyDef("m", HidKey.M), KeyDef(",", HidKey.COMMA), KeyDef(".", HidKey.PERIOD),
            KeyDef("⌫", weight = 1.45f, special = true)
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                row.forEach { key ->
                    KeyboardKey(
                        label = key.label,
                        weight = key.weight,
                        active = if (rowIndex == 2 && key.label == "⇧") shift else false,
                        special = key.special,
                        height = keyHeight,
                        modifier = Modifier.weight(key.weight),
                        onClick = {
                            when {
                                rowIndex == 2 && key.label == "⇧" -> onShift()
                                rowIndex == 2 && key.label == "⌫" -> onKey(HidKey.BACKSPACE)
                                key.usage != null -> onKey(key.usage)
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            KeyboardKey(
                label = "123",
                weight = 1.3f,
                special = true,
                height = keyHeight,
                modifier = Modifier.weight(1.3f),
                onClick = onPage
            )
            KeyboardKey("⌃", weight = 1f, active = control, special = true,
                height = keyHeight, modifier = Modifier.weight(1f), onClick = onControl)
            KeyboardKey("⌥", weight = 1f, active = option, special = true,
                height = keyHeight, modifier = Modifier.weight(1f), onClick = onOption)
            KeyboardKey(modifierLabel, weight = 1.25f, active = command, special = true,
                height = keyHeight, modifier = Modifier.weight(1.25f), onClick = onCommand)
            KeyboardKey(
                label = "space",
                weight = 3.1f,
                special = false,
                height = keyHeight,
                modifier = Modifier.weight(3.1f),
                onClick = { onKey(HidKey.SPACE) }
            )
            KeyboardKey(
                label = "return",
                weight = 1.45f,
                special = true,
                height = keyHeight,
                modifier = Modifier.weight(1.45f),
                onClick = { onKey(HidKey.ENTER) }
            )
            ArrowKey("←", HidKey.LEFT, keyHeight, Modifier.weight(0.85f), onKey)
            ArrowKey("↑", HidKey.UP, keyHeight, Modifier.weight(0.85f), onKey)
            ArrowKey("↓", HidKey.DOWN, keyHeight, Modifier.weight(0.85f), onKey)
            ArrowKey("→", HidKey.RIGHT, keyHeight, Modifier.weight(0.85f), onKey)
        }
    }
}

@Composable
private fun SymbolKeyboard(
    shift: Boolean,
    control: Boolean,
    option: Boolean,
    command: Boolean,
    modifierLabel: String,
    keyHeight: Dp,
    gap: Dp,
    onShift: () -> Unit,
    onControl: () -> Unit,
    onOption: () -> Unit,
    onCommand: () -> Unit,
    onPage: () -> Unit,
    onKey: (Int) -> Unit
) {
    val numberRow = listOf(
        KeyDef("1", HidKey.DIGIT_1, hint = "!"), KeyDef("2", HidKey.DIGIT_2, hint = "@"),
        KeyDef("3", HidKey.DIGIT_3, hint = "#"), KeyDef("4", HidKey.DIGIT_4, hint = "$"),
        KeyDef("5", HidKey.DIGIT_5, hint = "%"), KeyDef("6", HidKey.DIGIT_6, hint = "^"),
        KeyDef("7", HidKey.DIGIT_7, hint = "&"), KeyDef("8", HidKey.DIGIT_8, hint = "*"),
        KeyDef("9", HidKey.DIGIT_9, hint = "("), KeyDef("0", HidKey.DIGIT_0, hint = ")")
    )
    val symbolRow = listOf(
        KeyDef("-", HidKey.MINUS, hint = "_"), KeyDef("=", HidKey.EQUALS, hint = "+"),
        KeyDef("[", HidKey.LEFT_BRACKET, hint = "{"), KeyDef("]", HidKey.RIGHT_BRACKET, hint = "}"),
        KeyDef("\\", HidKey.BACKSLASH, hint = "|"), KeyDef(";", HidKey.SEMICOLON, hint = ":"),
        KeyDef("'", HidKey.APOSTROPHE, hint = "\""), KeyDef("`", HidKey.GRAVE, hint = "~"),
        KeyDef("/", HidKey.SLASH, hint = "?"), KeyDef(".", HidKey.PERIOD, hint = ">")
    )

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            numberRow.forEach { key ->
                KeyboardKey(
                    label = key.label,
                    hint = key.hint,
                    weight = 1f,
                    height = keyHeight,
                    modifier = Modifier.weight(1f),
                    onClick = { key.usage?.let(onKey) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            symbolRow.forEach { key ->
                KeyboardKey(
                    label = key.label,
                    hint = key.hint,
                    weight = 1f,
                    height = keyHeight,
                    modifier = Modifier.weight(1f),
                    onClick = { key.usage?.let(onKey) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            KeyboardKey(
                label = "⇧", weight = 1.5f, active = shift, special = true,
                height = keyHeight, modifier = Modifier.weight(1.5f), onClick = onShift
            )
            listOf(
                KeyDef(",", HidKey.COMMA, hint = "<"),
                KeyDef(".", HidKey.PERIOD, hint = ">"),
                KeyDef("-", HidKey.MINUS, hint = "_"),
                KeyDef("=", HidKey.EQUALS, hint = "+"),
                KeyDef(";", HidKey.SEMICOLON, hint = ":"),
                KeyDef("'", HidKey.APOSTROPHE, hint = "\""),
                KeyDef("/", HidKey.SLASH, hint = "?")
            ).forEach { key ->
                KeyboardKey(
                    label = key.label,
                    hint = key.hint,
                    weight = 0.86f,
                    height = keyHeight,
                    modifier = Modifier.weight(0.86f),
                    onClick = { key.usage?.let(onKey) }
                )
            }
            KeyboardKey(
                label = "⌫", weight = 1.45f, special = true, height = keyHeight,
                modifier = Modifier.weight(1.45f),
                onClick = { onKey(HidKey.BACKSPACE) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            KeyboardKey(
                label = "ABC", weight = 1.3f, special = true, height = keyHeight,
                modifier = Modifier.weight(1.3f), onClick = onPage
            )
            KeyboardKey("⌃", weight = 1f, active = control, special = true,
                height = keyHeight, modifier = Modifier.weight(1f), onClick = onControl)
            KeyboardKey("⌥", weight = 1f, active = option, special = true,
                height = keyHeight, modifier = Modifier.weight(1f), onClick = onOption)
            KeyboardKey(modifierLabel, weight = 1.25f, active = command, special = true,
                height = keyHeight, modifier = Modifier.weight(1.25f), onClick = onCommand)
            KeyboardKey(
                label = "space", weight = 3.1f, height = keyHeight,
                modifier = Modifier.weight(3.1f),
                onClick = { onKey(HidKey.SPACE) }
            )
            KeyboardKey(
                label = "return", weight = 1.45f, special = true, height = keyHeight,
                modifier = Modifier.weight(1.45f),
                onClick = { onKey(HidKey.ENTER) }
            )
            ArrowKey("←", HidKey.LEFT, keyHeight, Modifier.weight(0.85f), onKey)
            ArrowKey("↑", HidKey.UP, keyHeight, Modifier.weight(0.85f), onKey)
            ArrowKey("↓", HidKey.DOWN, keyHeight, Modifier.weight(0.85f), onKey)
            ArrowKey("→", HidKey.RIGHT, keyHeight, Modifier.weight(0.85f), onKey)
        }
    }
}

private data class KeyDef(
    val label: String,
    val usage: Int? = null,
    val weight: Float = 1f,
    val special: Boolean = false,
    val hint: String? = null
)

@Composable
private fun ArrowKey(
    label: String,
    usage: Int,
    height: Dp,
    modifier: Modifier,
    onKey: (Int) -> Unit
) {
    KeyboardKey(
        label = label,
        weight = 0.85f,
        special = true,
        height = height,
        modifier = modifier,
        onClick = { onKey(usage) }
    )
}

@Composable
private fun KeyboardKey(
    label: String,
    weight: Float,
    height: Dp,
    modifier: Modifier = Modifier,
    hint: String? = null,
    active: Boolean = false,
    special: Boolean = false,
    onClick: () -> Unit
) {
    val palette = airPalette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val defaultColor = if (special) palette.surfaceRaised else palette.surface.copy(alpha = 0.72f)
    val background by animateColorAsState(
        targetValue = when {
            active -> palette.accent
            pressed -> palette.accentSoft
            else -> defaultColor
        },
        label = "keyBackground"
    )
    val contentColor = when {
        active -> palette.background
        special -> palette.textSecondary
        else -> palette.textPrimary
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (active) palette.accent else palette.border,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = if (label.length > 2) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (special) FontWeight.Medium else FontWeight.Normal
                    )
                },
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = if (label.length > 4) 10.sp else 16.sp
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = if (active) palette.background.copy(alpha = 0.7f) else palette.textTertiary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ModifierIndicator(label: String, active: Boolean) {
    val palette = airPalette
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (active) palette.accent else palette.surfaceRaised)
            .border(
                1.dp,
                if (active) palette.accent else palette.border,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) palette.background else palette.textTertiary
        )
    }
}

@Composable
private fun KeyboardStage(
    modifier: Modifier,
    modifierLabel: String,
    control: Boolean,
    option: Boolean,
    command: Boolean,
    shift: Boolean,
    connection: ConnectionSummary,
    inputReady: Boolean
) {
    val palette = airPalette
    val chord = buildList {
        if (control) add("⌃")
        if (option) add("⌥")
        if (command) add(modifierLabel)
        if (shift) add("⇧")
    }.joinToString("  ")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.surfaceRaised.copy(alpha = 0.82f),
                        palette.surface.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, palette.border, RoundedCornerShape(26.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val gap = 28.dp.toPx()
            var x = gap / 2f
            while (x < size.width) {
                var y = gap / 2f
                while (y < size.height) {
                    drawCircle(
                        color = palette.textTertiary.copy(alpha = 0.12f),
                        radius = 0.8.dp.toPx(),
                        center = Offset(x, y)
                    )
                    y += gap
                }
                x += gap
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.accent.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width * 0.76f, size.height * 0.30f),
                    radius = size.width * 0.55f
                ),
                center = Offset(size.width * 0.76f, size.height * 0.30f),
                radius = size.width * 0.55f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "INPUT STAGE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.7.sp),
                    color = palette.textTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (inputReady) palette.success else palette.textTertiary)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (inputReady) "已连接" else "未连接",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = chord.ifBlank { "Aa" },
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp),
                color = if (chord.isBlank()) palette.textPrimary else palette.accentStrong,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (chord.isBlank()) "准备输入" else "修饰键已锁定",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (inputReady) "输入将发送到 ${connection.detail}" else "连接电脑后即可输入",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
                maxLines = 1
            )

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "US · ABC",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Shift 自动复位",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun describeModifiers(
    control: Boolean,
    option: Boolean,
    command: Boolean,
    shift: Boolean,
    hostOs: HostOs
): String {
    val parts = mutableListOf<String>()
    if (control) parts += "Control"
    if (option) parts += "Option"
    if (command) parts += if (hostOs == HostOs.MACOS) "Command" else "Win"
    if (shift) parts += "Shift"
    return parts.joinToString(" + ")
}
