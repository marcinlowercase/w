package marcinlowercase.w

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.ExtractedTextRequest
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.ShiftState
import marcinlowercase.w.ui.KeyboardDimens
import marcinlowercase.w.ui.KeyboardView
import marcinlowercase.w.viewmodel.KeyboardViewModel

class KeyboardImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModel = KeyboardViewModel()

    private var currentSelStart = 0
    private var currentSelEnd = 0

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentSelStart = attribute?.initialSelStart ?: 0
        currentSelEnd = attribute?.initialSelEnd ?: 0
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        currentSelStart = newSelStart
        currentSelEnd = newSelEnd
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = appViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        applyNavigationBarTheme(win)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window?.window?.let { win ->
            applyNavigationBarTheme(win)
        }
    }

    private fun applyNavigationBarTheme(win: Window) {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        win.navigationBarColor = if (isDarkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        WindowCompat.getInsetsController(win, win.decorView).isAppearanceLightNavigationBars = !isDarkMode
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val density = resources.displayMetrics.density
        val headroomPx = (KeyboardDimens.popupHeadroom.value * density).toInt()
        outInsets.contentTopInsets = headroomPx
        outInsets.visibleTopInsets = headroomPx
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this)

        window?.window?.let { win ->
            val decor = win.decorView
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
            applyNavigationBarTheme(win)
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        viewModel.onKeyPressHandler = { key, metaState ->
            resetCursorTracking()
            handleKeyPress(key, metaState)
        }
        viewModel.onTextCommitHandler = {
            resetCursorTracking()
            commitText(it)
        }
        viewModel.onCursorGlideHandler = { delta, isSelecting -> handleCursorGlide(delta, isSelecting) }
        viewModel.onGlideEndHandler = {
            if (viewModel.shiftState.value != ShiftState.SHIFTED) {
                resetGlideSelection()
            }
        }

        composeView.setContent {
            KeyboardView(viewModel = viewModel)
        }

        return composeView
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        appViewModelStore.clear()
    }

    private fun handleKeyPress(key: KeyDefinition, metaState: Int = 0): Boolean {
        val ic = currentInputConnection ?: return false

        if (key.type == KeyType.BACKSPACE) {
            val isShiftOn = (metaState and KeyEvent.META_SHIFT_ON) != 0
            val isCtrlOn = (metaState and KeyEvent.META_CTRL_ON) != 0

            if (isCtrlOn) {
                val now = android.os.SystemClock.uptimeMillis()
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, metaState))
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, metaState))
                return true
            }

            if (isShiftOn) {
                val textAfter = ic.getTextAfterCursor(1, 0)
                return if (!textAfter.isNullOrEmpty()) {
                    ic.deleteSurroundingText(0, 1)
                    true
                } else {
                    false
                }
            }

            val hasSelection = currentSelStart != currentSelEnd
            if (hasSelection) {
                val now = android.os.SystemClock.uptimeMillis()
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, 0))
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, 0))
                return true
            }

            if (currentSelStart > 0 || !ic.getTextBeforeCursor(1, 0).isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
                return true
            }

            return false
        }

        val isCtrl = (metaState and KeyEvent.META_CTRL_ON) != 0
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val maxSafeChars = 1000000
        val ipcChunkSize = 40000

        if (isCtrl && key.type == KeyType.CHARACTER) {
            when (key.primaryChar.lowercase()) {
                "c" -> {
                    val copied = ic.performContextMenuAction(android.R.id.copy)
                    if (!copied) {
                        val selected = ic.getSelectedText(0)?.toString()?.take(maxSafeChars)
                        if (!selected.isNullOrEmpty()) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("w_clip", selected))
                        } else {
                            val now = android.os.SystemClock.uptimeMillis()
                            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, metaState))
                            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C, 0, metaState))
                        }
                    }
                    return true
                }
                "x" -> {
                    val cut = ic.performContextMenuAction(android.R.id.cut)
                    if (!cut) {
                        val selected = ic.getSelectedText(0)?.toString()?.take(maxSafeChars)
                        if (!selected.isNullOrEmpty()) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("w_clip", selected))
                            ic.commitText("", 1)
                        } else {
                            val now = android.os.SystemClock.uptimeMillis()
                            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0, metaState))
                            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_X, 0, metaState))
                        }
                    }
                    return true
                }
                "v" -> {
                    val pasted = ic.performContextMenuAction(android.R.id.paste)
                    if (!pasted) {
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val rawText = clip.getItemAt(0).coerceToText(this).toString()
                            val text = rawText.take(maxSafeChars)
                            if (text.isNotEmpty()) {
                                ic.beginBatchEdit()
                                try {
                                    if (text.length > ipcChunkSize) {
                                        text.chunked(ipcChunkSize).forEach { chunk ->
                                            ic.commitText(chunk, 1)
                                        }
                                    } else {
                                        ic.commitText(text, 1)
                                    }
                                } finally {
                                    ic.endBatchEdit()
                                }
                                return true
                            }
                        }
                        val now = android.os.SystemClock.uptimeMillis()
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, 0, metaState))
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_V, 0, metaState))
                    }
                    return true
                }
                "a" -> {
                    rememberedAnchor = null
                    rememberedActiveHead = null
                    val selectedAll = ic.performContextMenuAction(android.R.id.selectAll)
                    if (!selectedAll) {
                        val now = android.os.SystemClock.uptimeMillis()
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 0, metaState))
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A, 0, metaState))
                    }
                    return true
                }
            }
        }

        if (metaState != 0) {
            val keyCode = resolveKeyCode(key)
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                val now = android.os.SystemClock.uptimeMillis()
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState))
                return true
            }
        }

        return when (key.type) {
            KeyType.CHARACTER -> {
                commitText(key.primaryChar)
                true
            }
            KeyType.SPACE -> {
                commitText(" ")
                true
            }
            KeyType.BACKSPACE -> false
            KeyType.ENTER -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                true
            }
            KeyType.MODIFIER -> {
                handleModifier(key.id)
                true
            }
            KeyType.ARROW -> {
                handleArrow(key.id)
                true
            }
            KeyType.SHIFT,
            KeyType.SPACER -> false
        }
    }

    private fun resolveKeyCode(key: KeyDefinition): Int {
        if (key.type == KeyType.ENTER) return KeyEvent.KEYCODE_ENTER
        if (key.type == KeyType.SPACE) return KeyEvent.KEYCODE_SPACE
        if (key.type == KeyType.BACKSPACE) return KeyEvent.KEYCODE_DEL
        if (key.id == "tab") return KeyEvent.KEYCODE_TAB
        if (key.id == "esc") return KeyEvent.KEYCODE_ESCAPE

        val ch = key.primaryChar.firstOrNull() ?: return KeyEvent.KEYCODE_UNKNOWN
        return when {
            ch in 'a'..'z' -> KeyEvent.KEYCODE_A + (ch - 'a')
            ch in 'A'..'Z' -> KeyEvent.KEYCODE_A + (ch - 'A')
            ch in '0'..'9' -> KeyEvent.KEYCODE_0 + (ch - '0')
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun handleModifier(id: String) {
        val ic = currentInputConnection ?: return
        when (id) {
            "tab" -> ic.commitText("\t", 1)
            "esc" -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
            "ctrl" -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT))
            "alt" -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT))
        }
    }

    private fun handleArrow(id: String) {
        val ic = currentInputConnection ?: return
        val keyCode = when (id) {
            "left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "up" -> KeyEvent.KEYCODE_DPAD_UP
            "down" -> KeyEvent.KEYCODE_DPAD_DOWN
            else -> return
        }
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }

    private var selectionAnchor: Int? = null
    private var activeCursorPos: Int? = null
    private var rememberedAnchor: Int? = null
    private var rememberedActiveHead: Int? = null

    private fun handleCursorGlide(deltaSteps: Int, isSelecting: Boolean): Boolean {
        val ic = currentInputConnection ?: return false

        if (!isSelecting) {
            val hasSelection = currentSelStart != currentSelEnd
            val selMin = kotlin.math.min(currentSelStart, currentSelEnd)
            val selMax = kotlin.math.max(currentSelStart, currentSelEnd)

            val startFrom = when {
                rememberedActiveHead != null -> rememberedActiveHead!!
                hasSelection && deltaSteps > 0 -> selMin
                hasSelection && deltaSteps < 0 -> selMax
                else -> currentSelEnd
            }

            resetGlideSelection()
            rememberedAnchor = null
            rememberedActiveHead = null

            if (deltaSteps < 0 && startFrom <= 0) return false
            if (deltaSteps > 0 && !hasSelection && ic.getTextAfterCursor(1, 0).isNullOrEmpty()) return false
            if (deltaSteps > 0 && hasSelection && startFrom >= selMax && ic.getTextAfterCursor(1, 0).isNullOrEmpty()) return false

            val target = (startFrom + deltaSteps).coerceAtLeast(0)
            if (target != currentSelEnd || currentSelStart != currentSelEnd) {
                ic.setSelection(target, target)
                currentSelStart = target
                currentSelEnd = target
                return true
            }
            return false
        } else {
            val hasExistingSelection = currentSelStart != currentSelEnd

            if (selectionAnchor == null) {
                if (rememberedAnchor != null && rememberedActiveHead != null) {
                    selectionAnchor = rememberedAnchor
                    activeCursorPos = rememberedActiveHead
                } else if (hasExistingSelection) {
                    val selMin = kotlin.math.min(currentSelStart, currentSelEnd)
                    val selMax = kotlin.math.max(currentSelStart, currentSelEnd)
                    if (deltaSteps < 0) {
                        selectionAnchor = selMin
                        activeCursorPos = selMax
                    } else {
                        selectionAnchor = selMax
                        activeCursorPos = selMin
                    }
                } else {
                    selectionAnchor = currentSelEnd
                    activeCursorPos = currentSelEnd
                }
            }

            val anchor = selectionAnchor ?: currentSelEnd
            val currentPos = activeCursorPos ?: currentSelEnd

            if (deltaSteps < 0 && currentPos <= 0) return false

            val target = (currentPos + deltaSteps).coerceAtLeast(0)
            val maxBound = kotlin.math.max(anchor, currentPos)

            if (deltaSteps > 0 && target > maxBound && ic.getTextAfterCursor(1, 0).isNullOrEmpty()) {
                return false
            }

            if (target != currentPos) {
                activeCursorPos = target
                rememberedAnchor = anchor
                rememberedActiveHead = target
                ic.setSelection(anchor, target)
                return true
            }
            return false
        }
    }

    private fun resetGlideSelection() {
        selectionAnchor = null
        activeCursorPos = null
    }

    private fun resetCursorTracking() {
        resetGlideSelection()
        rememberedAnchor = null
        rememberedActiveHead = null
    }
}