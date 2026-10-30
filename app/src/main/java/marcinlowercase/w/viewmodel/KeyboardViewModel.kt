package marcinlowercase.w.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import marcinlowercase.w.model.DefaultLayout
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.ShiftState

class KeyboardViewModel : ViewModel() {
    val layoutConfig = mutableStateOf(DefaultLayout.create())
    val shiftState = mutableStateOf(ShiftState.OFF)
    val isShiftPhysicallyHeld = mutableStateOf(false)
    private var lastShiftPressTime = 0L

    var onKeyPressHandler: ((KeyDefinition) -> Boolean)? = null
    var onTextCommitHandler: ((String) -> Unit)? = null
    var onCursorGlideHandler: ((Int, Boolean) -> Boolean)? = null
    var onGlideEndHandler: (() -> Unit)? = null

    fun onShiftPressed() {
        val now = System.currentTimeMillis()
        val current = shiftState.value
        shiftState.value = when (current) {
            ShiftState.OFF -> ShiftState.SHIFTED
            ShiftState.SHIFTED -> {
                if (now - lastShiftPressTime < 300L) {
                    ShiftState.CAPS_LOCK
                } else {
                    ShiftState.OFF
                }
            }
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        lastShiftPressTime = now
    }

    fun resolveDisplayedChar(key: KeyDefinition): String {
        val currentShift = shiftState.value
        return if (key.type == KeyType.CHARACTER) {
            when {
                key.primaryChar.length == 1 && key.primaryChar[0].isLetter() && currentShift != ShiftState.OFF -> {
                    key.primaryChar.uppercase()
                }
                currentShift == ShiftState.SHIFTED -> {
                    when (key.primaryChar) {
                        "1" -> "!"
                        "2" -> "@"
                        "3" -> "#"
                        "4" -> "$"
                        "5" -> "%"
                        "6" -> "^"
                        "7" -> "&"
                        "8" -> "*"
                        "9" -> "("
                        "0" -> ")"
                        else -> key.primaryChar
                    }
                }
                else -> key.primaryChar
            }
        } else if (key.type == KeyType.MODIFIER) {
            if (currentShift != ShiftState.OFF) key.primaryChar.uppercase() else key.primaryChar.lowercase()
        } else {
            key.primaryChar
        }
    }

    fun pressKey(key: KeyDefinition): Boolean {
        return if (key.type == KeyType.SHIFT) {
            onShiftPressed()
            true
        } else {
            val resolved = key.copy(primaryChar = resolveDisplayedChar(key))
            val handled = onKeyPressHandler?.invoke(resolved) ?: false
            if (key.type == KeyType.CHARACTER && shiftState.value == ShiftState.SHIFTED) {
                shiftState.value = ShiftState.OFF
            }
            handled
        }
    }

    fun selectPopup(symbol: String) {
        onTextCommitHandler?.invoke(symbol)
        if (shiftState.value == ShiftState.SHIFTED) {
            shiftState.value = ShiftState.OFF
        }
    }

    fun glideCursor(delta: Int): Boolean {
        return onCursorGlideHandler?.invoke(delta, isShiftPhysicallyHeld.value) ?: false
    }

    fun onGlideEnd() {
        onGlideEndHandler?.invoke()
    }
}