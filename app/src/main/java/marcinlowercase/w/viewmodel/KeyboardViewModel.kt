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
    private var lastShiftPressTime = 0L

    var onKeyPressHandler: ((KeyDefinition) -> Unit)? = null
    var onTextCommitHandler: ((String) -> Unit)? = null
    var onCursorGlideHandler: ((Int) -> Unit)? = null

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
        val isShiftActive = shiftState.value != ShiftState.OFF
        return if (isShiftActive && key.type == KeyType.CHARACTER) {
            when {
                key.primaryChar.length == 1 && key.primaryChar[0].isLetter() -> key.primaryChar.uppercase()
                key.primaryChar == "1" -> "!"
                key.primaryChar == "2" -> "@"
                key.primaryChar == "3" -> "#"
                key.primaryChar == "4" -> "$"
                key.primaryChar == "5" -> "%"
                key.primaryChar == "6" -> "^"
                key.primaryChar == "7" -> "&"
                key.primaryChar == "8" -> "*"
                key.primaryChar == "9" -> "("
                key.primaryChar == "0" -> ")"
                else -> key.primaryChar
            }
        } else if (key.type == KeyType.SHIFT) {
            if (shiftState.value == ShiftState.CAPS_LOCK) "⇪" else "⇧"
        } else {
            key.primaryChar
        }
    }

    fun pressKey(key: KeyDefinition) {
        if (key.type == KeyType.SHIFT) {
            onShiftPressed()
        } else {
            val resolved = key.copy(primaryChar = resolveDisplayedChar(key))
            onKeyPressHandler?.invoke(resolved)
            if (key.type == KeyType.CHARACTER && shiftState.value == ShiftState.SHIFTED) {
                shiftState.value = ShiftState.OFF
            }
        }
    }

    fun selectPopup(symbol: String) {
        onTextCommitHandler?.invoke(symbol)
        if (shiftState.value == ShiftState.SHIFTED) {
            shiftState.value = ShiftState.OFF
        }
    }

    fun glideCursor(delta: Int) {
        onCursorGlideHandler?.invoke(delta)
    }
}