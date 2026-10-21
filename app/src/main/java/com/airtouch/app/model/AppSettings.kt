package com.airtouch.app.model

/** 电脑端操作系统，决定快捷键映射。 */
enum class HostOs(val label: String) {
    MACOS("macOS"),
    WINDOWS("Windows")
}

/** 四套内置皮肤。 */
enum class ThemeSkin(val label: String, val subtitle: String, val dark: Boolean) {
    OBSIDIAN("曜石黑", "深空玻璃与冷银高光", true),
    SILVER("月岩银", "浅色雾面与 Apple 蓝", false),
    MIDNIGHT("深海蓝", "午夜蓝与冰川青", true),
    ROSE("玫瑰金", "暗玫瑰与暖粉高光", true)
}

enum class TransportKind {
    BLUETOOTH,
    USB
}

data class AppSettings(
    val skin: ThemeSkin = ThemeSkin.OBSIDIAN,
    val sensitivity: Float = 1.15f,
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val haptics: Boolean = true,
    val showTouchPoints: Boolean = true,
    val hostOs: HostOs = HostOs.MACOS,
    val usbHost: String = "127.0.0.1",
    val usbPort: Int = 17575
)
