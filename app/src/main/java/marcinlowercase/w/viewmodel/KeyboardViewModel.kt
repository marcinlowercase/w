package marcinlowercase.w.viewmodel

import android.media.AudioManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import marcinlowercase.w.model.DefaultLayout
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.KeyboardThemeMode
import marcinlowercase.w.model.PopupDirection
import marcinlowercase.w.model.SecondaryBindings
import marcinlowercase.w.model.ShiftState

class KeyboardViewModel : ViewModel() {
    val layoutConfig = mutableStateOf(DefaultLayout.create())
    val shiftState = mutableStateOf(ShiftState.OFF)
    val bottomChinPaddingDp = mutableStateOf(32)
    val topCornerRadiusDp = mutableStateOf(0f)
    val topExtraPaddingDp = mutableStateOf(8f)
    val highlightColor = mutableStateOf(Color(0xFF001FFF))
    val fontFamily = mutableStateOf<FontFamily>(FontFamily.Monospace)
    val ctrlArmed = mutableStateOf(false)
    val altArmed = mutableStateOf(false)
    val ctrlShiftArmed = mutableStateOf(false)
    val altShiftArmed = mutableStateOf(false)
    val activePopupKeyId = mutableStateOf<String?>(null)
    val soundEnabled = mutableStateOf(true)
    val outsyncEnabled = mutableStateOf(true)
    val themeMode = mutableStateOf(KeyboardThemeMode.SYSTEM)
    var audioManager: AudioManager? = null
    private var lastShiftPressTime = 0L

    fun playKeySound(type: KeyType) {
        if (!soundEnabled.value) return
        val am = audioManager ?: return
        val effect = when (type) {
            KeyType.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            KeyType.BACKSPACE -> AudioManager.FX_KEYPRESS_DELETE
            KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        am.playSoundEffect(effect)
    }

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
        val isShiftOn = shiftState.value != ShiftState.OFF
        val isSingleShifted = shiftState.value == ShiftState.SHIFTED

        return if (key.type == KeyType.CHARACTER) {
            when {
                key.primaryChar.length == 1 && key.primaryChar[0].isLetter() && isShiftOn -> {
                    key.primaryChar.uppercase()
                }
                isSingleShifted -> {
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
                "ctrl" -> if (ctrlShiftArmed.value || isShiftOn) "CTRL" else "ctrl"
                "alt" -> if (altShiftArmed.value || isShiftOn) "ALT" else "alt"
                else -> if (isShiftOn) key.primaryChar.uppercase() else key.primaryChar.lowercase()
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

    private fun isMacroCommand(text: String): Boolean {
        val upper = text.uppercase().trim()
        return upper.contains("CTRL+") || upper.contains("ALT+") || upper.contains("SHIFT+")
    }

    private fun executeMacro(command: String) {
        val parts = command.uppercase().split("+").map { it.trim() }
        var meta = 0
        var keyChar = ""

        parts.forEach { part ->
            when (part) {
                "CTRL" -> meta = meta or android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
                "ALT" -> meta = meta or android.view.KeyEvent.META_ALT_ON or android.view.KeyEvent.META_ALT_LEFT_ON
                "SHIFT" -> meta = meta or android.view.KeyEvent.META_SHIFT_ON or android.view.KeyEvent.META_SHIFT_LEFT_ON
                else -> keyChar = part
            }
        }

        val macroKey = KeyDefinition(
            id = "macro_${keyChar.lowercase()}",
            primaryChar = keyChar,
            type = KeyType.CHARACTER
        )
        onKeyPressHandler?.invoke(macroKey, meta)
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

        if (isMacroCommand(key.primaryChar)) {
            executeMacro(key.primaryChar)
            if (shiftState.value == ShiftState.SHIFTED) {
                shiftState.value = ShiftState.OFF
            }
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
        if ((key.type == KeyType.CHARACTER || key.type == KeyType.BACKSPACE || key.type == KeyType.SPACE || key.id == "tab" || key.id == "esc") && shiftState.value == ShiftState.SHIFTED) {
            shiftState.value = ShiftState.OFF
        }

        return handled
    }

    fun selectPopup(symbol: String) {
        val payload = symbol
        if (isMacroCommand(payload)) {
            executeMacro(payload)
        } else {
            onTextCommitHandler?.invoke(payload)
        }
        ctrlArmed.value = false
        ctrlShiftArmed.value = false
        altArmed.value = false
        altShiftArmed.value = false
        if (shiftState.value == ShiftState.SHIFTED) {
            shiftState.value = ShiftState.OFF
        }
    }

    fun glideCursor(delta: Int): Boolean {
        return onCursorGlideHandler?.invoke(delta, shiftState.value != ShiftState.OFF) ?: false
    }

    fun onGlideEnd() {
        if (shiftState.value == ShiftState.SHIFTED) {
            shiftState.value = ShiftState.OFF
        }
        onGlideEndHandler?.invoke()
    }

    fun getKey(keyId: String): KeyDefinition {
        return layoutConfig.value.rows.flatMap { it.keys }.find { it.id == keyId }
            ?: layoutConfig.value.ribbon.find { it.id == keyId }
            ?: KeyDefinition(id = keyId, primaryChar = keyId)
    }

    fun commitSecondary(keyId: String, direction: PopupDirection) {
        val currentKey = getKey(keyId)
        val sec = when {
            currentKey.secondaries?.hasOptions() == true -> currentKey.secondaries
            currentKey.popups.isNotEmpty() -> when (currentKey.popups.size) {
                1 -> SecondaryBindings(center = currentKey.popups[0])
                2 -> SecondaryBindings(center = currentKey.popups[0], right = currentKey.popups[1])
                3 -> SecondaryBindings(center = currentKey.popups[0], top = currentKey.popups[1], right = currentKey.popups[2])
                4 -> SecondaryBindings(center = currentKey.popups[0], top = currentKey.popups[1], right = currentKey.popups[2], bottom = currentKey.popups[3])
                else -> SecondaryBindings(center = currentKey.popups[0], top = currentKey.popups[1], right = currentKey.popups[2], bottom = currentKey.popups[3], left = currentKey.popups[4])
            }
            else -> null
        }
        val symbol = sec?.get(direction) ?: sec?.center
        if (symbol != null) {
            selectPopup(symbol)
        }
    }
}