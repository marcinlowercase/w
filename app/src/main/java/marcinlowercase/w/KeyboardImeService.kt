package marcinlowercase.w

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.Window
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
import marcinlowercase.w.ui.KeyboardDimens
import marcinlowercase.w.ui.KeyboardView
import marcinlowercase.w.viewmodel.KeyboardViewModel

class KeyboardImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val appViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModel = KeyboardViewModel()

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

        viewModel.onKeyPressHandler = { handleKeyPress(it) }
        viewModel.onTextCommitHandler = { commitText(it) }
        viewModel.onCursorGlideHandler = { handleCursorGlide(it) }

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

    private fun handleKeyPress(key: KeyDefinition): Boolean {
        val ic = currentInputConnection ?: return false
        return when (key.type) {
            KeyType.CHARACTER -> {
                commitText(key.primaryChar)
                true
            }
            KeyType.SPACE -> {
                commitText(" ")
                true
            }
            KeyType.BACKSPACE -> {
                val textBefore = ic.getTextBeforeCursor(1, 0)
                val selected = ic.getSelectedText(0)
                if (!textBefore.isNullOrEmpty() || !selected.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                    true
                } else {
                    false
                }
            }
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

    private fun handleCursorGlide(deltaSteps: Int) {
        val ic = currentInputConnection ?: return
        val keyCode = if (deltaSteps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(deltaSteps)) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        }
    }
}