package com.airtouch.app.input

/**
 * USB HID Usage ID（Keyboard/Keypad page 0x07）。
 * 数值与 HID 报告描述符中的 usage 完全一致。
 */
object HidKey {
    const val NONE = 0x00
    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val MINUS = 0x2D
    const val EQUALS = 0x2E
    const val LEFT_BRACKET = 0x2F
    const val RIGHT_BRACKET = 0x30
    const val BACKSLASH = 0x31
    const val SEMICOLON = 0x33
    const val APOSTROPHE = 0x34
    const val GRAVE = 0x35
    const val COMMA = 0x36
    const val PERIOD = 0x37
    const val SLASH = 0x38
    const val CAPS_LOCK = 0x39

    const val A = 0x04
    const val B = 0x05
    const val C = 0x06
    const val D = 0x07
    const val E = 0x08
    const val F = 0x09
    const val G = 0x0A
    const val H = 0x0B
    const val I = 0x0C
    const val J = 0x0D
    const val K = 0x0E
    const val L = 0x0F
    const val M = 0x10
    const val N = 0x11
    const val O = 0x12
    const val P = 0x13
    const val Q = 0x14
    const val R = 0x15
    const val S = 0x16
    const val T = 0x17
    const val U = 0x18
    const val V = 0x19
    const val W = 0x1A
    const val X = 0x1B
    const val Y = 0x1C
    const val Z = 0x1D

    const val DIGIT_1 = 0x1E
    const val DIGIT_2 = 0x1F
    const val DIGIT_3 = 0x20
    const val DIGIT_4 = 0x21
    const val DIGIT_5 = 0x22
    const val DIGIT_6 = 0x23
    const val DIGIT_7 = 0x24
    const val DIGIT_8 = 0x25
    const val DIGIT_9 = 0x26
    const val DIGIT_0 = 0x27

    const val F1 = 0x3A
    const val F2 = 0x3B
    const val F3 = 0x3C
    const val F4 = 0x3D
    const val F5 = 0x3E
    const val F6 = 0x3F
    const val F7 = 0x40
    const val F8 = 0x41
    const val F9 = 0x42
    const val F10 = 0x43
    const val F11 = 0x44
    const val F12 = 0x45

    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52
}

object HidModifier {
    const val NONE = 0x00
    const val LEFT_CTRL = 0x01
    const val LEFT_SHIFT = 0x02
    const val LEFT_ALT = 0x04
    const val LEFT_GUI = 0x08
    const val RIGHT_CTRL = 0x10
    const val RIGHT_SHIFT = 0x20
    const val RIGHT_ALT = 0x40
    const val RIGHT_GUI = 0x80
}

object HidMouse {
    const val LEFT = 0x01
    const val RIGHT = 0x02
    const val MIDDLE = 0x04
}

/** 系统级动作，由 InputRouter 根据目标系统映射为具体快捷键。 */
enum class SystemAction {
    MISSION_CONTROL,
    APP_EXPOSE,
    SPACE_LEFT,
    SPACE_RIGHT,
    SHOW_DESKTOP,
    SCREENSHOT,
    COPY,
    PASTE,
    UNDO,
    SELECT_ALL,
    ZOOM_IN,
    ZOOM_OUT
}

data class KeyChord(val modifiers: Int, val key: Int)
