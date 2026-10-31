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
    val ctrlArmed = mutableStateOf(false)
    val altArmed = mutableStateOf(false)
    val ctrlShiftArmed = mutableStateOf(false)
    val altShiftArmed = mutableStateOf(false)
    private var lastShiftPressTime = 0L

    var onKeyPressHandler: ((KeyDefinition, Int) -> Boolean)? = null
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
            when (key.id) {
                "ctrl" -> if (ctrlShiftArmed.value || currentShift != ShiftState.OFF) "CTRL" else "ctrl"
                "alt" -> if (altShiftArmed.value || currentShift != ShiftState.OFF) "ALT" else "alt"
                else -> if (currentShift != ShiftState.OFF) key.primaryChar.uppercase() else key.primaryChar.lowercase()
            }
        } else {
            key.primaryChar
        }
    }

    fun onCtrlPressed() {
        if (ctrlArmed.value) {
            ctrlArmed.value = false
            ctrlShiftArmed.value = false
        } else {
            ctrlArmed.value = true
            if (shiftState.value == ShiftState.SHIFTED) {
                ctrlShiftArmed.value = true
                shiftState.value = ShiftState.OFF
            }
        }
    }

    fun onAltPressed() {
        if (altArmed.value) {
            altArmed.value = false
            altShiftArmed.value = false
        } else {
            altArmed.value = true
            if (shiftState.value == ShiftState.SHIFTED) {
                altShiftArmed.value = true
                shiftState.value = ShiftState.OFF
            }
        }
    }

    fun pressKey(key: KeyDefinition): Boolean {
        if (key.type == KeyType.SHIFT) {
            onShiftPressed()
            return true
        }

        if (key.id == "ctrl") {
            onCtrlPressed()
            return true
        }

        if (key.id == "alt") {
            onAltPressed()
            return true
        }

        var metaState = 0
        if (ctrlArmed.value) {
            metaState = metaState or android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
        }
        if (altArmed.value) {
            metaState = metaState or android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
        }
        if (ctrlShiftArmed.value || altShiftArmed.value || shiftState.value != ShiftState.OFF) {
            metaState = metaState or android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
        }

        val resolved = key.copy(primaryChar = resolveDisplayedChar(key))
        val handled = onKeyPressHandler?.invoke(resolved, metaState) ?: false

        if (ctrlArmed.value) {
            ctrlArmed.value = false
            ctrlShiftArmed.value = false
        }
        if (altArmed.value) {
            altArmed.value = false
            altShiftArmed.value = false
        }
        if (key.type == KeyType.CHARACTER && shiftState.value == ShiftState.SHIFTED) {
            shiftState.value = ShiftState.OFF
        }

        return handled
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