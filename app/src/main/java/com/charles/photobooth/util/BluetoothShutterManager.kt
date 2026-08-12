package com.charles.photobooth.util

import android.view.KeyEvent

object BluetoothShutterManager {
    var onShutterTriggered: (() -> Unit)? = null

    fun isShutterKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y -> true
            else -> false
        }
    }

    fun handleKeyEvent(keyCode: Int): Boolean {
        if (isShutterKey(keyCode)) {
            val callback = onShutterTriggered
            if (callback != null) {
                callback.invoke()
                return true
            }
        }
        return false
    }
}
