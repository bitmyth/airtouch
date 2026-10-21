#!/usr/bin/env python3
"""
AirTouch desktop bridge
=======================

通过 USB 调试通道（adb reverse）接收 Android 端 AirTouch 发来的二进制输入帧，
再注入到 macOS / Windows / Linux 的系统输入栈。

协议（TCP，默认 127.0.0.1:17575）
---------------------------------
  0x4D 'M'  [dx:int8][dy:int8]               相对鼠标移动
  0x57 'W'  [vertical:int8][horizontal:int8] 滚轮（正数向上/向右）
  0x42 'B'  [buttons:uint8]                  全量按键状态：bit0 左键, bit1 右键, bit2 中键
  0x4B 'K'  [modifiers:uint8][key1..key6]    与 USB HID 键盘报告一致
  0x48 'H'  []                               心跳

不依赖 pip 包：
  - macOS   使用 ctypes 调用 ApplicationServices / Quartz
  - Windows 使用 ctypes 调用 user32.keybd_event / mouse_event
  - Linux   调用 xdotool（需要系统安装）
"""

from __future__ import annotations

import argparse
import ctypes
import os
import platform
import shutil
import socket
import socketserver
import struct
import subprocess
import sys
import threading
import time
import traceback
from typing import Callable, Iterable

VERSION = "1.0.0"

TYPE_MOVE = 0x4D
TYPE_SCROLL = 0x57
TYPE_BUTTONS = 0x42
TYPE_KEYBOARD = 0x4B
TYPE_HEARTBEAT = 0x48

FRAME_LENGTHS = {
    TYPE_MOVE: 2,
    TYPE_SCROLL: 2,
    TYPE_BUTTONS: 1,
    TYPE_KEYBOARD: 7,
    TYPE_HEARTBEAT: 0,
}

MOUSE_LEFT = 0x01
MOUSE_RIGHT = 0x02
MOUSE_MIDDLE = 0x04

HID_MOD_CTRL = 0x01
HID_MOD_SHIFT = 0x02
HID_MOD_ALT = 0x04
HID_MOD_GUI = 0x08

# HID Keyboard/Keypad usage -> macOS virtual key code
HID_TO_MAC_VK = {
    0x04: 0x00, 0x05: 0x0B, 0x06: 0x08, 0x07: 0x02, 0x08: 0x0E,
    0x09: 0x03, 0x0A: 0x05, 0x0B: 0x04, 0x0C: 0x22, 0x0D: 0x26,
    0x0E: 0x28, 0x0F: 0x25, 0x10: 0x2E, 0x11: 0x2D, 0x12: 0x1F,
    0x13: 0x23, 0x14: 0x0C, 0x15: 0x0F, 0x16: 0x01, 0x17: 0x11,
    0x18: 0x20, 0x19: 0x09, 0x1A: 0x0D, 0x1B: 0x07, 0x1C: 0x10,
    0x1D: 0x06,
    0x1E: 0x12, 0x1F: 0x13, 0x20: 0x14, 0x21: 0x15, 0x22: 0x17,
    0x23: 0x16, 0x24: 0x1A, 0x25: 0x1C, 0x26: 0x19, 0x27: 0x1D,
    0x28: 0x24, 0x29: 0x35, 0x2A: 0x33, 0x2B: 0x30, 0x2C: 0x31,
    0x2D: 0x1B, 0x2E: 0x18, 0x2F: 0x21, 0x30: 0x1E, 0x31: 0x2A,
    0x33: 0x29, 0x34: 0x27, 0x35: 0x32, 0x36: 0x2B, 0x37: 0x2F,
    0x38: 0x2C, 0x39: 0x39,
    0x3A: 0x7A, 0x3B: 0x78, 0x3C: 0x63, 0x3D: 0x76, 0x3E: 0x60,
    0x3F: 0x61, 0x40: 0x62, 0x41: 0x64, 0x42: 0x65, 0x43: 0x6D,
    0x44: 0x67, 0x45: 0x6F,
    0x4A: 0x73, 0x4B: 0x74, 0x4C: 0x75, 0x4D: 0x77, 0x4E: 0x79,
    0x4F: 0x7C, 0x50: 0x7B, 0x51: 0x7D, 0x52: 0x7E,
}

# HID Keyboard/Keypad usage -> Windows virtual key
HID_TO_WIN_VK = {
    0x04: 0x41, 0x05: 0x42, 0x06: 0x43, 0x07: 0x44, 0x08: 0x45,
    0x09: 0x46, 0x0A: 0x47, 0x0B: 0x48, 0x0C: 0x49, 0x0D: 0x4A,
    0x0E: 0x4B, 0x0F: 0x4C, 0x10: 0x4D, 0x11: 0x4E, 0x12: 0x4F,
    0x13: 0x50, 0x14: 0x51, 0x15: 0x52, 0x16: 0x53, 0x17: 0x54,
    0x18: 0x55, 0x19: 0x56, 0x1A: 0x57, 0x1B: 0x58, 0x1C: 0x59,
    0x1D: 0x5A,
    0x1E: 0x31, 0x1F: 0x32, 0x20: 0x33, 0x21: 0x34, 0x22: 0x35,
    0x23: 0x36, 0x24: 0x37, 0x25: 0x38, 0x26: 0x39, 0x27: 0x30,
    0x28: 0x0D, 0x29: 0x1B, 0x2A: 0x08, 0x2B: 0x09, 0x2C: 0x20,
    0x2D: 0xBD, 0x2E: 0xBB, 0x2F: 0xDB, 0x30: 0xDD, 0x31: 0xDC,
    0x33: 0xBA, 0x34: 0xDE, 0x35: 0xC0, 0x36: 0xBC, 0x37: 0xBE,
    0x38: 0xBF, 0x39: 0x14,
    0x3A: 0x70, 0x3B: 0x71, 0x3C: 0x72, 0x3D: 0x73, 0x3E: 0x74,
    0x3F: 0x75, 0x40: 0x76, 0x41: 0x77, 0x42: 0x78, 0x43: 0x79,
    0x44: 0x7A, 0x45: 0x7B,
    0x49: 0x2D, 0x4A: 0x24, 0x4B: 0x21, 0x4C: 0x2E, 0x4D: 0x23,
    0x4E: 0x22, 0x4F: 0x27, 0x50: 0x25, 0x51: 0x28, 0x52: 0x26,
}

HID_TO_XDOTOOL = {
    0x04: "a", 0x05: "b", 0x06: "c", 0x07: "d", 0x08: "e", 0x09: "f",
    0x0A: "g", 0x0B: "h", 0x0C: "i", 0x0D: "j", 0x0E: "k", 0x0F: "l",
    0x10: "m", 0x11: "n", 0x12: "o", 0x13: "p", 0x14: "q", 0x15: "r",
    0x16: "s", 0x17: "t", 0x18: "u", 0x19: "v", 0x1A: "w", 0x1B: "x",
    0x1C: "y", 0x1D: "z",
    0x1E: "1", 0x1F: "2", 0x20: "3", 0x21: "4", 0x22: "5", 0x23: "6",
    0x24: "7", 0x25: "8", 0x26: "9", 0x27: "0",
    0x28: "Return", 0x29: "Escape", 0x2A: "BackSpace", 0x2B: "Tab",
    0x2C: "space", 0x2D: "minus", 0x2E: "equal", 0x2F: "bracketleft",
    0x30: "bracketright", 0x31: "backslash", 0x33: "semicolon",
    0x34: "apostrophe", 0x35: "grave", 0x36: "comma", 0x37: "period",
    0x38: "slash", 0x39: "Caps_Lock",
    0x3A: "F1", 0x3B: "F2", 0x3C: "F3", 0x3D: "F4", 0x3E: "F5",
    0x3F: "F6", 0x40: "F7", 0x41: "F8", 0x42: "F9", 0x43: "F10",
    0x44: "F11", 0x45: "F12",
    0x49: "Insert", 0x4A: "Home", 0x4B: "Prior", 0x4C: "Delete",
    0x4D: "End", 0x4E: "Next", 0x4F: "Right", 0x50: "Left",
    0x51: "Down", 0x52: "Up",
}


def log(message: str) -> None:
    stamp = time.strftime("%H:%M:%S")
    print(f"[{stamp}] {message}", flush=True)


def iter_frames(buffer: bytearray) -> Iterable[tuple[int, bytes]]:
    """从 TCP 缓冲区中切出完整帧；不完整时保留在 buffer 中。"""
    while buffer:
        frame_type = buffer[0]
        payload_length = FRAME_LENGTHS.get(frame_type)
        if payload_length is None:
            raise ValueError(f"未知帧类型 0x{frame_type:02X}")
        if len(buffer) < 1 + payload_length:
            return
        payload = bytes(buffer[1:1 + payload_length])
        del buffer[:1 + payload_length]
        yield frame_type, payload


class InputBackend:
    """所有平台的公共键盘状态机。"""

    def __init__(self) -> None:
        self._keys_down: set[int] = set()
        self._mods_down: set[int] = set()

    def move(self, dx: int, dy: int) -> None:
        raise NotImplementedError

    def scroll(self, vertical: int, horizontal: int) -> None:
        raise NotImplementedError

    def set_buttons(self, mask: int) -> None:
        raise NotImplementedError

    def set_keyboard(self, modifiers: int, keys: list[int]) -> None:
        new_keys = {key for key in keys if key}
        new_mods = {
            bit for bit, _ in (
                (HID_MOD_CTRL, None),
                (HID_MOD_SHIFT, None),
                (HID_MOD_ALT, None),
                (HID_MOD_GUI, None),
            ) if modifiers & bit
        }
        # 释放普通键，再释放修饰键。
        for key in sorted(self._keys_down - new_keys):
            self.key_event(key, False)
        for mod in sorted(self._mods_down - new_mods):
            self.modifier_event(mod, False)
        # 按下修饰键，再按普通键；这是快捷键能生效的关键顺序。
        for mod in sorted(new_mods - self._mods_down):
            self.modifier_event(mod, True)
        for key in sorted(new_keys - self._keys_down):
            self.key_event(key, True)
        self._keys_down = new_keys
        self._mods_down = new_mods

    def key_event(self, usage: int, down: bool) -> None:
        raise NotImplementedError

    def modifier_event(self, modifier_bit: int, down: bool) -> None:
        raise NotImplementedError

    def close(self) -> None:
        for key in sorted(self._keys_down):
            try:
                self.key_event(key, False)
            except Exception:
                pass
        for mod in sorted(self._mods_down):
            try:
                self.modifier_event(mod, False)
            except Exception:
                pass
        self._keys_down.clear()
        self._mods_down.clear()


class DryRunBackend(InputBackend):
    """--dry-run 使用：只打印输入，不操作真实系统。"""

    def __init__(self) -> None:
        super().__init__()
        self._move_count = 0
        self._move_accum = [0, 0]
        self._lock = threading.Lock()

    def _flush_moves(self) -> None:
        if self._move_count:
            log(f"mouse move: {self._move_accum[0]:+d}, {self._move_accum[1]:+d}")
            self._move_count = 0
            self._move_accum = [0, 0]

    def move(self, dx: int, dy: int) -> None:
        with self._lock:
            self._move_count += 1
            self._move_accum[0] += dx
            self._move_accum[1] += dy
            if self._move_count >= 20:
                self._flush_moves()

    def scroll(self, vertical: int, horizontal: int) -> None:
        with self._lock:
            self._flush_moves()
            log(f"scroll: v={vertical:+d} h={horizontal:+d}")

    def set_buttons(self, mask: int) -> None:
        with self._lock:
            self._flush_moves()
            log(f"buttons: 0b{mask:03b}")

    def set_keyboard(self, modifiers: int, keys: list[int]) -> None:
        super().set_keyboard(modifiers, keys)
        with self._lock:
            self._flush_moves()
            log(
                "keyboard report: mods=0x%02X keys=%s"
                % (modifiers, [f"0x{k:02X}" for k in keys if k])
            )

    def key_event(self, usage: int, down: bool) -> None:
        log(f"  key {'DOWN' if down else 'UP'} usage=0x{usage:02X}")

    def modifier_event(self, modifier_bit: int, down: bool) -> None:
        names = {
            HID_MOD_CTRL: "Ctrl",
            HID_MOD_SHIFT: "Shift",
            HID_MOD_ALT: "Alt",
            HID_MOD_GUI: "GUI",
        }
        log(f"  mod {'DOWN' if down else 'UP'} {names.get(modifier_bit, modifier_bit)}")

    def close(self) -> None:
        with self._lock:
            self._flush_moves()
        super().close()


class MacBackend(InputBackend):
    """macOS Quartz 事件注入，不需要 pyobjc。"""

    kCGEventLeftMouseDown = 1
    kCGEventLeftMouseUp = 2
    kCGEventRightMouseDown = 3
    kCGEventRightMouseUp = 4
    kCGEventMouseMoved = 5
    kCGEventLeftMouseDragged = 6
    kCGEventRightMouseDragged = 7
    kCGEventOtherMouseDown = 25
    kCGEventOtherMouseUp = 26
    kCGEventOtherMouseDragged = 27
    kCGHIDEventTap = 0
    kCGScrollEventUnitLine = 1

    kCGEventFlagMaskShift = 1 << 17
    kCGEventFlagMaskControl = 1 << 18
    kCGEventFlagMaskAlternate = 1 << 19
    kCGEventFlagMaskCommand = 1 << 20

    _lib = None
    _CGPoint = None

    @classmethod
    def _load(cls) -> None:
        if cls._lib is not None:
            return

        class CGPoint(ctypes.Structure):
            _fields_ = [("x", ctypes.c_double), ("y", ctypes.c_double)]

        cls._CGPoint = CGPoint
        path = "/System/Library/Frameworks/ApplicationServices.framework/ApplicationServices"
        lib = ctypes.CDLL(path)

        lib.CGEventCreate.argtypes = [ctypes.c_void_p]
        lib.CGEventCreate.restype = ctypes.c_void_p
        lib.CGEventGetLocation.argtypes = [ctypes.c_void_p]
        lib.CGEventGetLocation.restype = CGPoint
        lib.CGEventCreateMouseEvent.argtypes = [
            ctypes.c_void_p, ctypes.c_uint32, CGPoint, ctypes.c_uint32
        ]
        lib.CGEventCreateMouseEvent.restype = ctypes.c_void_p
        lib.CGEventCreateKeyboardEvent.argtypes = [
            ctypes.c_void_p, ctypes.c_uint16, ctypes.c_bool
        ]
        lib.CGEventCreateKeyboardEvent.restype = ctypes.c_void_p
        lib.CGEventSetFlags.argtypes = [ctypes.c_void_p, ctypes.c_uint64]
        lib.CGEventSetFlags.restype = None
        lib.CGEventPost.argtypes = [ctypes.c_uint32, ctypes.c_void_p]
        lib.CGEventPost.restype = None
        lib.CGEventCreateScrollWheelEvent2.argtypes = [
            ctypes.c_void_p, ctypes.c_uint32, ctypes.c_uint32,
            ctypes.c_int32, ctypes.c_int32, ctypes.c_int32
        ]
        lib.CGEventCreateScrollWheelEvent2.restype = ctypes.c_void_p
        lib.CFRelease.argtypes = [ctypes.c_void_p]
        lib.CFRelease.restype = None

        try:
            lib.AXIsProcessTrusted.argtypes = []
            lib.AXIsProcessTrusted.restype = ctypes.c_bool
        except AttributeError:
            pass

        cls._lib = lib

    @classmethod
    def accessibility_trusted(cls) -> bool:
        cls._load()
        try:
            return bool(cls._lib.AXIsProcessTrusted())
        except Exception:
            return False

    def __init__(self) -> None:
        super().__init__()
        self._load()
        self._buttons = 0
        self._lock = threading.Lock()
        trusted = self.accessibility_trusted()
        if trusted:
            log("macOS 辅助功能权限：已授权")
        else:
            log("警告：终端/Python 尚未获得“辅助功能”权限，注入事件可能被系统忽略。")
            log("      系统设置 → 隐私与安全性 → 辅助功能 → 勾选运行脚本的终端。")

    def _location(self):
        lib = self._lib
        event = lib.CGEventCreate(None)
        if not event:
            return self._CGPoint(0.0, 0.0)
        try:
            return lib.CGEventGetLocation(event)
        finally:
            lib.CFRelease(event)

    def _post_mouse(self, event_type: int, button: int) -> None:
        if not event_type:
            return
        lib = self._lib
        point = self._location()
        event = lib.CGEventCreateMouseEvent(None, event_type, point, button)
        if event:
            lib.CGEventPost(self.kCGHIDEventTap, event)
            lib.CFRelease(event)

    def _move_to(self, x: float, y: float) -> None:
        lib = self._lib
        if self._buttons & MOUSE_LEFT:
            event_type = self.kCGEventLeftMouseDragged
            button = 0
        elif self._buttons & MOUSE_RIGHT:
            event_type = self.kCGEventRightMouseDragged
            button = 1
        elif self._buttons & MOUSE_MIDDLE:
            event_type = self.kCGEventOtherMouseDragged
            button = 2
        else:
            event_type = self.kCGEventMouseMoved
            button = 0
        event = lib.CGEventCreateMouseEvent(
            None, event_type, self._CGPoint(x, y), button
        )
        if event:
            lib.CGEventPost(self.kCGHIDEventTap, event)
            lib.CFRelease(event)

    def move(self, dx: int, dy: int) -> None:
        if dx == 0 and dy == 0:
            return
        with self._lock:
            point = self._location()
            self._move_to(point.x + dx, point.y + dy)

    def scroll(self, vertical: int, horizontal: int) -> None:
        if vertical == 0 and horizontal == 0:
            return
        lib = self._lib
        event = lib.CGEventCreateScrollWheelEvent2(
            None,
            self.kCGScrollEventUnitLine,
            2,
            int(vertical),
            int(horizontal),
            0,
        )
        if event:
            lib.CGEventPost(self.kCGHIDEventTap, event)
            lib.CFRelease(event)

    def set_buttons(self, mask: int) -> None:
        mask &= 0x07
        with self._lock:
            changed = mask ^ self._buttons
            transitions = [
                (MOUSE_LEFT, self.kCGEventLeftMouseDown, self.kCGEventLeftMouseUp, 0),
                (MOUSE_RIGHT, self.kCGEventRightMouseDown, self.kCGEventRightMouseUp, 1),
                (MOUSE_MIDDLE, self.kCGEventOtherMouseDown, self.kCGEventOtherMouseUp, 2),
            ]
            for button_mask, down_type, up_type, button_index in transitions:
                if changed & button_mask:
                    self._post_mouse(
                        down_type if mask & button_mask else up_type,
                        button_index,
                    )
            self._buttons = mask

    def _flags(self, modifiers: int) -> int:
        flags = 0
        if modifiers & (HID_MOD_SHIFT):
            flags |= self.kCGEventFlagMaskShift
        if modifiers & (HID_MOD_CTRL):
            flags |= self.kCGEventFlagMaskControl
        if modifiers & (HID_MOD_ALT):
            flags |= self.kCGEventFlagMaskAlternate
        if modifiers & (HID_MOD_GUI):
            flags |= self.kCGEventFlagMaskCommand
        return flags

    def _post_key(self, virtual_key: int, down: bool, flags: int) -> None:
        lib = self._lib
        event = lib.CGEventCreateKeyboardEvent(None, virtual_key, down)
        if not event:
            return
        try:
            if flags:
                lib.CGEventSetFlags(event, flags)
            lib.CGEventPost(self.kCGHIDEventTap, event)
        finally:
            lib.CFRelease(event)

    def set_keyboard(self, modifiers: int, keys: list[int]) -> None:
        new_keys = {key for key in keys if key}
        flags = self._flags(modifiers)
        for usage in sorted(self._keys_down - new_keys):
            vk = HID_TO_MAC_VK.get(usage)
            if vk is not None:
                self._post_key(vk, False, flags)
        for usage in sorted(new_keys - self._keys_down):
            vk = HID_TO_MAC_VK.get(usage)
            if vk is not None:
                self._post_key(vk, True, flags)
            else:
                log(f"未映射的 HID usage: 0x{usage:02X}")
        self._keys_down = new_keys

    def key_event(self, usage: int, down: bool) -> None:
        vk = HID_TO_MAC_VK.get(usage)
        if vk is not None:
            self._post_key(vk, down, 0)

    def modifier_event(self, modifier_bit: int, down: bool) -> None:
        # macOS 通过 CGEventSetFlags 给按键打修饰标记，无需单独按修饰键。
        pass

    def close(self) -> None:
        self.set_buttons(0)
        super().close()


class WindowsBackend(InputBackend):
    """Windows user32 事件注入。"""

    MOUSEEVENTF_MOVE = 0x0001
    MOUSEEVENTF_LEFTDOWN = 0x0002
    MOUSEEVENTF_LEFTUP = 0x0004
    MOUSEEVENTF_RIGHTDOWN = 0x0008
    MOUSEEVENTF_RIGHTUP = 0x0010
    MOUSEEVENTF_MIDDLEDOWN = 0x0020
    MOUSEEVENTF_MIDDLEUP = 0x0040
    MOUSEEVENTF_WHEEL = 0x0800
    MOUSEEVENTF_HWHEEL = 0x01000
    KEYEVENTF_KEYUP = 0x0002

    _MOD_VK = {
        HID_MOD_CTRL: 0xA2,
        HID_MOD_SHIFT: 0xA0,
        HID_MOD_ALT: 0xA4,
        HID_MOD_GUI: 0x5B,
    }

    def __init__(self) -> None:
        super().__init__()
        if not sys.platform.startswith("win"):
            raise RuntimeError("WindowsBackend 只能在 Windows 上运行")
        self.user32 = ctypes.windll.user32
        self._buttons = 0
        self._lock = threading.Lock()

    def move(self, dx: int, dy: int) -> None:
        with self._lock:
            self.user32.mouse_event(self.MOUSEEVENTF_MOVE, int(dx), int(dy), 0, 0)

    def scroll(self, vertical: int, horizontal: int) -> None:
        with self._lock:
            if vertical:
                self.user32.mouse_event(
                    self.MOUSEEVENTF_WHEEL, 0, 0, int(vertical) * 120, 0
                )
            if horizontal:
                self.user32.mouse_event(
                    self.MOUSEEVENTF_HWHEEL, 0, 0, int(horizontal) * 120, 0
                )

    def set_buttons(self, mask: int) -> None:
        mask &= 0x07
        transitions = [
            (MOUSE_LEFT, self.MOUSEEVENTF_LEFTDOWN, self.MOUSEEVENTF_LEFTUP),
            (MOUSE_RIGHT, self.MOUSEEVENTF_RIGHTDOWN, self.MOUSEEVENTF_RIGHTUP),
            (MOUSE_MIDDLE, self.MOUSEEVENTF_MIDDLEDOWN, self.MOUSEEVENTF_MIDDLEUP),
        ]
        with self._lock:
            changed = mask ^ self._buttons
            for button_mask, down_flag, up_flag in transitions:
                if changed & button_mask:
                    flag = down_flag if mask & button_mask else up_flag
                    self.user32.mouse_event(flag, 0, 0, 0, 0)
            self._buttons = mask

    def key_event(self, usage: int, down: bool) -> None:
        vk = HID_TO_WIN_VK.get(usage)
        if vk is None:
            return
        flags = 0 if down else self.KEYEVENTF_KEYUP
        self.user32.keybd_event(vk, 0, flags, 0)

    def modifier_event(self, modifier_bit: int, down: bool) -> None:
        vk = self._MOD_VK.get(modifier_bit)
        if vk is None:
            return
        flags = 0 if down else self.KEYEVENTF_KEYUP
        self.user32.keybd_event(vk, 0, flags, 0)

    def close(self) -> None:
        self.set_buttons(0)
        super().close()


class LinuxBackend(InputBackend):
    """Linux/X11 通过 xdotool 注入；Wayland 受系统安全限制。"""

    _MOD_KEYSYM = {
        HID_MOD_CTRL: "Control_L",
        HID_MOD_SHIFT: "Shift_L",
        HID_MOD_ALT: "Alt_L",
        HID_MOD_GUI: "Super_L",
    }

    def __init__(self) -> None:
        super().__init__()
        if not shutil.which("xdotool"):
            raise RuntimeError("Linux 需要安装 xdotool：sudo apt install xdotool")
        self._buttons = 0
        self._lock = threading.Lock()
        if os.environ.get("XDG_SESSION_TYPE", "").lower() == "wayland":
            log("警告：检测到 Wayland，xdotool 注入可能被限制，建议使用 X11 会话。")

    def _run(self, *args: str) -> None:
        try:
            subprocess.run(
                ["xdotool", *args],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=False,
            )
        except Exception as exc:
            log(f"xdotool 调用失败: {exc}")

    def move(self, dx: int, dy: int) -> None:
        with self._lock:
            self._run("mousemove_relative", "--", str(int(dx)), str(int(dy)))

    def scroll(self, vertical: int, horizontal: int) -> None:
        def click_repeat(button: int, count: int) -> None:
            count = min(abs(count), 50)
            if count:
                self._run("click", "--repeat", str(count), str(button))

        with self._lock:
            if vertical > 0:
                click_repeat(4, vertical)
            elif vertical < 0:
                click_repeat(5, vertical)
            if horizontal > 0:
                click_repeat(7, horizontal)
            elif horizontal < 0:
                click_repeat(6, horizontal)

    def set_buttons(self, mask: int) -> None:
        mask &= 0x07
        with self._lock:
            changed = mask ^ self._buttons
            for button_mask, native_button in (
                (MOUSE_LEFT, 1),
                (MOUSE_MIDDLE, 2),
                (MOUSE_RIGHT, 3),
            ):
                if changed & button_mask:
                    self._run(
                        "mousedown" if mask & button_mask else "mouseup",
                        str(native_button),
                    )
            self._buttons = mask

    def key_event(self, usage: int, down: bool) -> None:
        keysym = HID_TO_XDOTOOL.get(usage)
        if keysym is None:
            return
        self._run("keydown" if down else "keyup", keysym)

    def modifier_event(self, modifier_bit: int, down: bool) -> None:
        keysym = self._MOD_KEYSYM.get(modifier_bit)
        if keysym is None:
            return
        self._run("keydown" if down else "keyup", keysym)

    def close(self) -> None:
        self.set_buttons(0)
        super().close()


def make_backend(dry_run: bool) -> InputBackend:
    if dry_run:
        return DryRunBackend()
    system = platform.system()
    if system == "Darwin":
        return MacBackend()
    if system == "Windows":
        return WindowsBackend()
    if system == "Linux":
        return LinuxBackend()
    raise RuntimeError(f"暂不支持的平台: {system}")


class AirTouchServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, address, handler, dry_run: bool, verbose: bool = False):
        super().__init__(address, handler)
        self.dry_run = dry_run
        self.verbose = verbose


class ClientHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        peer = f"{self.client_address[0]}:{self.client_address[1]}"
        log(f"手机已连接：{peer}")
        try:
            backend = make_backend(self.server.dry_run)
        except Exception as exc:
            log(f"无法初始化输入后端：{exc}")
            return

        buffer = bytearray()
        self.request.settimeout(8.0)
        try:
            while True:
                try:
                    data = self.request.recv(4096)
                except socket.timeout:
                    log("心跳超时，断开连接")
                    break
                if not data:
                    break
                buffer.extend(data)
                for frame_type, payload in iter_frames(buffer):
                    if self.server.verbose:
                        log(
                            f"recv frame 0x{frame_type:02X} "
                            f"({len(payload)}B) {payload.hex()}"
                        )
                    self._dispatch(backend, frame_type, payload)
        except (ConnectionResetError, BrokenPipeError) as exc:
            log(f"连接被重置：{exc!r}")
        except ValueError as exc:
            log(f"协议错误：{exc}")
        except Exception:
            log("处理输入时发生异常：")
            traceback.print_exc()
        finally:
            try:
                backend.close()
            except Exception:
                pass
            log(f"手机已断开：{peer}")

    @staticmethod
    def _dispatch(backend: InputBackend, frame_type: int, payload: bytes) -> None:
        if frame_type == TYPE_HEARTBEAT:
            return
        if frame_type == TYPE_MOVE:
            dx, dy = struct.unpack("bb", payload)
            backend.move(dx, dy)
        elif frame_type == TYPE_SCROLL:
            vertical, horizontal = struct.unpack("bb", payload)
            backend.scroll(vertical, horizontal)
        elif frame_type == TYPE_BUTTONS:
            backend.set_buttons(payload[0] & 0x07)
        elif frame_type == TYPE_KEYBOARD:
            modifiers = payload[0]
            keys = list(payload[1:7])
            backend.set_keyboard(modifiers, keys)
        else:
            raise ValueError(f"未知帧 0x{frame_type:02X}")


def setup_adb_reverse(port: int) -> None:
    adb = shutil.which("adb")
    if not adb:
        log("未找到 adb；如果已经手动执行 adb reverse，可以忽略。")
        return
    try:
        result = subprocess.run(
            [adb, "reverse", f"tcp:{port}", f"tcp:{port}"],
            capture_output=True,
            text=True,
            timeout=10,
            check=False,
        )
        if result.returncode == 0:
            log(f"adb reverse 已建立：tcp:{port} → tcp:{port}")
        else:
            message = (result.stderr or result.stdout).strip()
            log(f"adb reverse 失败（可手动执行）：{message}")
    except Exception as exc:
        log(f"adb reverse 执行失败：{exc}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="AirTouch USB bridge - receive trackpad/keyboard frames from Android"
    )
    parser.add_argument(
        "--host",
        default="127.0.0.1",
        help="监听地址（默认 127.0.0.1；USB 网络共享时用 0.0.0.0）",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=17575,
        help="监听端口（默认 17575，需与 App 内一致）",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印收到的输入，不注入系统",
    )
    parser.add_argument(
        "--no-adb-reverse",
        action="store_true",
        help="启动时不自动执行 adb reverse",
    )
    parser.add_argument(
        "--no-banner",
        action="store_true",
        help="不打印启动横幅",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="打印每个输入帧，便于调试协议",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.port < 1 or args.port > 65535:
        print("端口必须在 1–65535 之间", file=sys.stderr)
        return 2

    if not args.no_banner:
        print("")
        print("  AirTouch USB Bridge  v" + VERSION)
        print("  ------------------------------------------")
        print(f"  平台      : {platform.system()} {platform.release()}")
        print(f"  监听      : {args.host}:{args.port}")
        print(f"  模式      : {'DRY RUN（不注入）' if args.dry_run else '真实输入注入'}")
        print("  ------------------------------------------")
        print("")

    if not args.no_adb_reverse and args.host in ("127.0.0.1", "localhost"):
        setup_adb_reverse(args.port)

    try:
        server = AirTouchServer(
            (args.host, args.port), ClientHandler, args.dry_run, args.verbose
        )
    except OSError as exc:
        print(f"无法监听 {args.host}:{args.port}: {exc}", file=sys.stderr)
        return 1

    log(f"等待手机连接 {args.host}:{args.port} …（Ctrl+C 退出）")
    try:
        server.serve_forever(poll_interval=0.5)
    except KeyboardInterrupt:
        log("正在退出…")
    finally:
        server.shutdown()
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
