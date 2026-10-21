package com.airtouch.app.input

import com.airtouch.app.model.HostOs

object HidShortcuts {

    /**
     * macOS 使用 Command / Control 的原生快捷键；Windows 使用 Win / Ctrl。
     * 蓝牙 HID 与 USB 桥接共用这份映射。
     */
    fun forAction(action: SystemAction, hostOs: HostOs): KeyChord {
        return if (hostOs == HostOs.MACOS) macAction(action) else windowsAction(action)
    }

    private fun macAction(action: SystemAction): KeyChord = when (action) {
        SystemAction.MISSION_CONTROL -> KeyChord(HidModifier.LEFT_CTRL, HidKey.UP)
        SystemAction.APP_EXPOSE -> KeyChord(HidModifier.LEFT_CTRL, HidKey.DOWN)
        SystemAction.SPACE_LEFT -> KeyChord(HidModifier.LEFT_CTRL, HidKey.LEFT)
        SystemAction.SPACE_RIGHT -> KeyChord(HidModifier.LEFT_CTRL, HidKey.RIGHT)
        SystemAction.SHOW_DESKTOP -> KeyChord(HidModifier.NONE, HidKey.F11)
        SystemAction.SCREENSHOT -> KeyChord(
            HidModifier.LEFT_GUI or HidModifier.LEFT_SHIFT,
            HidKey.DIGIT_3
        )
        SystemAction.COPY -> KeyChord(HidModifier.LEFT_GUI, HidKey.C)
        SystemAction.PASTE -> KeyChord(HidModifier.LEFT_GUI, HidKey.V)
        SystemAction.UNDO -> KeyChord(HidModifier.LEFT_GUI, HidKey.Z)
        SystemAction.SELECT_ALL -> KeyChord(HidModifier.LEFT_GUI, HidKey.A)
        SystemAction.ZOOM_IN -> KeyChord(HidModifier.LEFT_GUI, HidKey.EQUALS)
        SystemAction.ZOOM_OUT -> KeyChord(HidModifier.LEFT_GUI, HidKey.MINUS)
    }

    private fun windowsAction(action: SystemAction): KeyChord = when (action) {
        SystemAction.MISSION_CONTROL -> KeyChord(HidModifier.LEFT_GUI, HidKey.TAB)
        SystemAction.APP_EXPOSE -> KeyChord(HidModifier.LEFT_ALT, HidKey.TAB)
        SystemAction.SPACE_LEFT -> KeyChord(
            HidModifier.LEFT_CTRL or HidModifier.LEFT_GUI,
            HidKey.LEFT
        )
        SystemAction.SPACE_RIGHT -> KeyChord(
            HidModifier.LEFT_CTRL or HidModifier.LEFT_GUI,
            HidKey.RIGHT
        )
        SystemAction.SHOW_DESKTOP -> KeyChord(HidModifier.LEFT_GUI, HidKey.D)
        SystemAction.SCREENSHOT -> KeyChord(
            HidModifier.LEFT_GUI or HidModifier.LEFT_SHIFT,
            HidKey.S
        )
        SystemAction.COPY -> KeyChord(HidModifier.LEFT_CTRL, HidKey.C)
        SystemAction.PASTE -> KeyChord(HidModifier.LEFT_CTRL, HidKey.V)
        SystemAction.UNDO -> KeyChord(HidModifier.LEFT_CTRL, HidKey.Z)
        SystemAction.SELECT_ALL -> KeyChord(HidModifier.LEFT_CTRL, HidKey.A)
        SystemAction.ZOOM_IN -> KeyChord(HidModifier.LEFT_CTRL, HidKey.EQUALS)
        SystemAction.ZOOM_OUT -> KeyChord(HidModifier.LEFT_CTRL, HidKey.MINUS)
    }
}
