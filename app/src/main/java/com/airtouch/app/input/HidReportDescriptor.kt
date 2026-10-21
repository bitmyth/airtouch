package com.airtouch.app.input

/**
 * 一个同时声明鼠标和键盘的 HID Report Descriptor。
 *
 * Report ID 1: 鼠标    [buttons, dx, dy, verticalWheel, horizontalPan]
 * Report ID 2: 键盘    [modifier, reserved, key1..key6]
 *
 * Android BluetoothHidDevice.sendReport() 的 reportId 与描述符中的 Report ID 对应，
 * data 中不需要重复携带 reportId。
 */
object HidReportDescriptor {

    const val MOUSE_REPORT_ID = 1
    const val KEYBOARD_REPORT_ID = 2

    val bytes: ByteArray = intArrayOf(
        // ---------- Mouse / Trackpad ----------
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x02,             // Usage (Mouse)
        0xA1, 0x01,             // Collection (Application)
        0x85, 0x01,             //   Report ID (1)
        0x09, 0x01,             //   Usage (Pointer)
        0xA1, 0x00,             //   Collection (Physical)

        0x05, 0x09,             //     Usage Page (Button)
        0x19, 0x01,             //     Usage Minimum (1)
        0x29, 0x03,             //     Usage Maximum (3)
        0x15, 0x00,             //     Logical Minimum (0)
        0x25, 0x01,             //     Logical Maximum (1)
        0x95, 0x03,             //     Report Count (3)
        0x75, 0x01,             //     Report Size (1)
        0x81, 0x02,             //     Input (Data, Variable, Absolute)
        0x95, 0x01,             //     Report Count (1)
        0x75, 0x05,             //     Report Size (5)
        0x81, 0x03,             //     Input (Constant) padding

        0x05, 0x01,             //     Usage Page (Generic Desktop)
        0x09, 0x30,             //     Usage (X)
        0x09, 0x31,             //     Usage (Y)
        0x09, 0x38,             //     Usage (Wheel)
        0x15, 0x81,             //     Logical Minimum (-127)
        0x25, 0x7F,             //     Logical Maximum (127)
        0x75, 0x08,             //     Report Size (8)
        0x95, 0x03,             //     Report Count (3)
        0x81, 0x06,             //     Input (Data, Variable, Relative)

        0x05, 0x0C,             //     Usage Page (Consumer)
        0x0A, 0x38, 0x02,       //     Usage (AC Pan)
        0x15, 0x81,             //     Logical Minimum (-127)
        0x25, 0x7F,             //     Logical Maximum (127)
        0x75, 0x08,             //     Report Size (8)
        0x95, 0x01,             //     Report Count (1)
        0x81, 0x06,             //     Input (Data, Variable, Relative)
        0xC0,                   //   End Collection (Physical)
        0xC0,                   // End Collection (Application)

        // ---------- Keyboard ----------
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1, 0x01,             // Collection (Application)
        0x85, 0x02,             //   Report ID (2)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0,             //   Usage Minimum (Left Control)
        0x29, 0xE7,             //   Usage Maximum (Right GUI)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x01,             //   Logical Maximum (1)
        0x75, 0x01,             //   Report Size (1)
        0x95, 0x08,             //   Report Count (8)
        0x81, 0x02,             //   Input (Data, Variable, Absolute) modifiers
        0x95, 0x01,             //   Report Count (1)
        0x75, 0x08,             //   Report Size (8)
        0x81, 0x03,             //   Input (Constant) reserved byte
        0x95, 0x06,             //   Report Count (6)
        0x75, 0x08,             //   Report Size (8)
        0x15, 0x00,             //   Logical Minimum (0)
        0x25, 0x65,             //   Logical Maximum (101)
        0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,             //   Usage Minimum (0)
        0x29, 0x65,             //   Usage Maximum (101)
        0x81, 0x00,             //   Input (Data, Array, Absolute)
        0xC0                    // End Collection (Application)
    ).map { it.toByte() }.toByteArray()

    /** 蓝牙 HID 鼠标报告。 */
    fun mouseReport(
        buttons: Int,
        dx: Int,
        dy: Int,
        wheel: Int,
        pan: Int
    ): ByteArray = byteArrayOf(
        buttons.toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte(),
        pan.coerceIn(-127, 127).toByte()
    )

    /** 蓝牙 HID 键盘报告（最多同时 6 个普通按键）。 */
    fun keyboardReport(modifiers: Int, keys: IntArray): ByteArray {
        val report = ByteArray(8)
        report[0] = modifiers.toByte()
        report[1] = 0
        for (index in 0 until 6) {
            report[index + 2] = (keys.getOrNull(index) ?: 0).toByte()
        }
        return report
    }
}
