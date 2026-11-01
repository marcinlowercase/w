package marcinlowercase.w.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.KeyboardLayoutConfig
import marcinlowercase.w.model.PopupDirection
import marcinlowercase.w.model.SecondaryBindings
import marcinlowercase.w.R

object KeyboardDimens {
    val popupHeadroom = 100.dp
    val keyHeight = 53.dp
    val rowGap = 6.dp
    val popupCircleSize = keyHeight + rowGap
    val ribbonHeight = 38.dp
    val bottomChinPadding = 32.dp
    val topPadding = 8.dp
    val keyGap = 5.dp
    val keyCornerRadius = 8.dp
    val popupOffset = (-64).dp
}

val LocalKeyboardViewModel = staticCompositionLocalOf<marcinlowercase.w.viewmodel.KeyboardViewModel> {
    error("No KeyboardViewModel provided")
}

@Composable
fun KeyboardView(
    viewModel: marcinlowercase.w.viewmodel.KeyboardViewModel
) {
    val density = LocalDensity.current
    var shiftKeyWidthDp by remember { mutableStateOf<Dp?>(null) }

    CompositionLocalProvider(LocalKeyboardViewModel provides viewModel) {
        val config = viewModel.layoutConfig.value

        val isDark = isSystemInDarkTheme()
        val keyboardBgColor = if (isDark) Color.Black else Color.White

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            Spacer(modifier = Modifier.height(KeyboardDimens.popupHeadroom))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(keyboardBgColor)
                    .navigationBarsPadding()
                    .padding(
                        start = 5.dp,
                        end = 5.dp,
                        top = KeyboardDimens.topPadding,
                        bottom = KeyboardDimens.bottomChinPadding
                    )
            ) {
                if (config.ribbon.isNotEmpty()) {
                    RibbonRow(keys = config.ribbon)
                    Spacer(modifier = Modifier.height(KeyboardDimens.rowGap))
                }

                config.rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.keys.forEach { key ->
                            if (key.type == KeyType.SPACER) {
                                Spacer(modifier = Modifier.weight(key.weight))
                            } else {
                                val isUnifiedModifier = key.id == "ctrl" || key.id == "alt" || key.type == KeyType.ENTER
                                val boxModifier = when {
                                    key.type == KeyType.SHIFT -> {
                                        Modifier
                                            .weight(key.weight)
                                            .onSizeChanged { size ->
                                                val measuredDp = with(density) { size.width.toDp() }
                                                if (shiftKeyWidthDp != measuredDp) {
                                                    shiftKeyWidthDp = measuredDp
                                                }
                                            }
                                    }
                                    isUnifiedModifier && shiftKeyWidthDp != null -> {
                                        Modifier.width(shiftKeyWidthDp!!)
                                    }
                                    key.type == KeyType.SPACE -> {
                                        Modifier.weight(1f)
                                    }
                                    else -> {
                                        Modifier.weight(key.weight)
                                    }
                                }

                                Box(modifier = boxModifier) {
                                    if (key.type == KeyType.SPACE) {
                                        SpaceKeyView(key = key)
                                    } else {
                                        RemappableKeyView(key = key)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RibbonRow(keys: List<KeyDefinition>) {
    val viewModel = LocalKeyboardViewModel.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val ribbonBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF3F4F6)
    val ribbonBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE2E2E2)
    val ribbonTextColor = if (isDark) Color.White else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(KeyboardDimens.ribbonHeight),
        horizontalArrangement = Arrangement.spacedBy(KeyboardDimens.keyGap)
    ) {
        keys.forEach { key ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .border(0.5.dp, ribbonBorder, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .pointerInput(key.id) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.pressKey(key)
                        }
                    },
                color = ribbonBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = key.primaryChar,
                        color = ribbonTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

enum class ShiftHoldAction {
    NONE,
    TAB,
    ESC
}

@Composable
fun RemappableKeyView(key: KeyDefinition) {
    val viewModel = LocalKeyboardViewModel.current
    val view = LocalView.current
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    var isPressedDown by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedDirection by remember { mutableStateOf(PopupDirection.CENTER) }
    var shiftHoldAction by remember { mutableStateOf(ShiftHoldAction.NONE) }
    var popupHorizontalShiftDp by remember { mutableStateOf(0.dp) }
    var popupVerticalShiftDp by remember { mutableStateOf(0.dp) }

    val isShiftActive = viewModel.shiftState.value != marcinlowercase.w.model.ShiftState.OFF
    val isCtrlArmed = key.id == "ctrl" && viewModel.ctrlArmed.value
    val isAltArmed = key.id == "alt" && viewModel.altArmed.value
    val isModifierArmed = (key.type == KeyType.SHIFT && isShiftActive) || isCtrlArmed || isAltArmed

    val displayedChar = viewModel.resolveDisplayedChar(key)

    val keyBackground = when {
        isModifierArmed -> Color(0xFF2563EB)
        key.type == KeyType.CHARACTER -> if (isDark) Color(0xFF1C1C1E) else Color.White
        else -> if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
    }

    val keyTextColor = when {
        isModifierArmed -> Color.White
        isDark -> Color.White
        else -> Color(0xFF111827)
    }

    val keyBorderColor = if (isDark) Color(0xFF38383A) else Color(0xFFD1D5DB)
    val hintColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF9CA3AF)
    val popupBg = if (isDark) Color(0xFF1C1C1E) else Color.White
    val popupBorder = if (isDark) Color(0xFF38383A) else Color(0xFFD1D5DB)
    val unselectedPopupTextColor = if (isDark) Color.White else Color(0xFF111827)

    val visualWidthFraction = if (key.id == "a" || key.id == "l") 0.666667f else 1f
    val visualAlignment = when (key.id) {
        "a" -> Alignment.CenterEnd
        "l" -> Alignment.CenterStart
        else -> Alignment.Center
    }

    val cellHeight = KeyboardDimens.keyHeight + KeyboardDimens.rowGap

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .zIndex(if (isPressedDown || showPopup) 10f else 0f)
            .pointerInput(key.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    isPressedDown = true
                    if (key.type == KeyType.SHIFT) {
                        viewModel.isShiftPhysicallyHeld.value = true
                    }

                    if (key.type == KeyType.BACKSPACE) {
                        viewModel.pressKey(key)

                        val releasedEarly = withTimeoutOrNull(350L) {
                            waitForUpOrCancellation()
                        }

                        if (releasedEarly == null) {
                            val repeatJob = coroutineScope.launch {
                                while (isActive) {
                                    delay(50L)
                                    val deleted = viewModel.pressKey(key)
                                    if (deleted) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                }
                            }
                            waitForUpOrCancellation()
                            repeatJob.cancel()
                        }
                        isPressedDown = false
                        return@awaitEachGesture
                    }

                    var fingerReleasedEarly = false
                    withTimeoutOrNull(220L) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                fingerReleasedEarly = true
                                break
                            }
                        }
                    }

                    if (fingerReleasedEarly) {
                        isPressedDown = false
                        if (key.type == KeyType.SHIFT) {
                            viewModel.isShiftPhysicallyHeld.value = false
                            viewModel.onGlideEnd()
                        }
                        viewModel.pressKey(key)
                    } else {
                        isPressedDown = false
                        if (key.type == KeyType.SHIFT) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            shiftHoldAction = ShiftHoldAction.NONE
                            showPopup = true

                            val startY = down.position.y
                            val verticalThresholdPx = with(density) { 26.dp.toPx() }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val deltaY = change.position.y - startY
                                    val newAction = when {
                                        deltaY < -verticalThresholdPx -> ShiftHoldAction.TAB
                                        deltaY > verticalThresholdPx -> ShiftHoldAction.ESC
                                        else -> ShiftHoldAction.NONE
                                    }
                                    if (newAction != shiftHoldAction) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        shiftHoldAction = newAction
                                    }
                                } else {
                                    viewModel.isShiftPhysicallyHeld.value = false
                                    viewModel.onGlideEnd()
                                    when (shiftHoldAction) {
                                        ShiftHoldAction.TAB -> viewModel.pressKey(KeyDefinition("tab", "TAB", type = KeyType.MODIFIER))
                                        ShiftHoldAction.ESC -> viewModel.pressKey(KeyDefinition("esc", "ESC", type = KeyType.MODIFIER))
                                        ShiftHoldAction.NONE -> Unit
                                    }
                                    showPopup = false
                                    break
                                }
                            }
                        } else {
                            val sec = when {
                                key.secondaries?.hasOptions() == true -> key.secondaries
                                key.popups.isNotEmpty() -> when (key.popups.size) {
                                    1 -> SecondaryBindings(center = key.popups[0])
                                    2 -> SecondaryBindings(center = key.popups[0], right = key.popups[1])
                                    3 -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2])
                                    4 -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2], bottom = key.popups[3])
                                    else -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2], bottom = key.popups[3], left = key.popups[4])
                                }
                                else -> null
                            }

                            if (sec?.hasOptions() == true) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                selectedDirection = PopupDirection.CENTER
                                showPopup = true

                                val startX = down.position.x
                                val startY = down.position.y
                                val swipeThresholdPx = with(density) { 22.dp.toPx() }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.pressed) {
                                        val deltaX = change.position.x - startX
                                        val deltaY = change.position.y - startY
                                        val absX = kotlin.math.abs(deltaX)
                                        val absY = kotlin.math.abs(deltaY)

                                        val newDir = when {
                                            absX < swipeThresholdPx && absY < swipeThresholdPx -> PopupDirection.CENTER
                                            absY >= absX -> if (deltaY < 0 && sec.top != null) PopupDirection.TOP else if (deltaY > 0 && sec.bottom != null) PopupDirection.BOTTOM else PopupDirection.CENTER
                                            else -> if (deltaX < 0 && sec.left != null) PopupDirection.LEFT else if (deltaX > 0 && sec.right != null) PopupDirection.RIGHT else PopupDirection.CENTER
                                        }

                                        if (newDir != selectedDirection) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            selectedDirection = newDir
                                        }
                                    } else {
                                        val symbol = sec.get(selectedDirection) ?: sec.center
                                        if (symbol != null) {
                                            viewModel.selectPopup(symbol)
                                        }
                                        showPopup = false
                                        break
                                    }
                                }
                            } else {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) break
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(visualWidthFraction)
                .fillMaxHeight()
                .align(visualAlignment)
                .padding(
                    horizontal = KeyboardDimens.keyGap / 2,
                    vertical = KeyboardDimens.rowGap / 2
                )
                .onGloballyPositioned { coordinates ->
                    val rootWidth = coordinates.findRootCoordinates().size.width.toFloat()
                    val keyCenterInRoot = coordinates.positionInRoot().x + (coordinates.size.width / 2f)
                    val keyTopInRoot = coordinates.positionInRoot().y

                    val secOptions = when {
                        key.secondaries?.hasOptions() == true -> key.secondaries
                        key.popups.isNotEmpty() -> when (key.popups.size) {
                            1 -> SecondaryBindings(center = key.popups[0])
                            2 -> SecondaryBindings(center = key.popups[0], right = key.popups[1])
                            3 -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2])
                            4 -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2], bottom = key.popups[3])
                            else -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2], bottom = key.popups[3], left = key.popups[4])
                        }
                        else -> null
                    }

                    val hasTop = secOptions?.top != null
                    val hasBottom = secOptions?.bottom != null
                    val rowsCount = when {
                        hasTop && hasBottom -> 3
                        hasTop || hasBottom -> 2
                        else -> 1
                    }
                    val cardHeightPx = with(density) { ((KeyboardDimens.popupCircleSize * rowsCount) + 8.dp).toPx() }
                    val idealTopInRoot = keyTopInRoot - cardHeightPx - with(density) { 6.dp.toPx() }
                    val shiftYPx = if (idealTopInRoot < 0f) -idealTopInRoot else 0f
                    val shiftYDp = with(density) { shiftYPx.toDp() }
                    if (popupVerticalShiftDp != shiftYDp) {
                        popupVerticalShiftDp = shiftYDp
                    }

                    val hasLeft = secOptions?.left != null
                    val hasRight = secOptions?.right != null
                    val colsCount = when {
                        hasLeft && hasRight -> 3
                        hasLeft || hasRight -> 2
                        else -> 1
                    }
                    val popupHalfWidthPx = with(density) { ((KeyboardDimens.popupCircleSize * colsCount) / 2f).toPx() }

                    val popupLeftInRoot = keyCenterInRoot - popupHalfWidthPx
                    val popupRightInRoot = keyCenterInRoot + popupHalfWidthPx

                    val shiftPx = when {
                        popupLeftInRoot < 0f -> -popupLeftInRoot
                        popupRightInRoot > rootWidth -> rootWidth - popupRightInRoot
                        else -> 0f
                    }

                    val shiftDp = with(density) { shiftPx.toDp() }
                    if (popupHorizontalShiftDp != shiftDp) {
                        popupHorizontalShiftDp = shiftDp
                    }
                }
                .clip(RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                .background(keyBackground)
                .border(0.5.dp, keyBorderColor, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
        ) {
            if (key.hint != null) {
                Text(
                    text = key.hint,
                    color = hintColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }

            val iconResId = when (key.type) {
                KeyType.SHIFT -> when (viewModel.shiftState.value) {
                    marcinlowercase.w.model.ShiftState.OFF -> R.drawable.ic_shift_off
                    marcinlowercase.w.model.ShiftState.SHIFTED -> R.drawable.ic_shift_on
                    marcinlowercase.w.model.ShiftState.CAPS_LOCK -> R.drawable.ic_caps_lock
                }
                KeyType.BACKSPACE -> R.drawable.ic_backspace
                KeyType.ENTER -> R.drawable.ic_new_line
                else -> null
            }

            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = keyTextColor,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center)
                )
            } else {
                Text(
                    text = displayedChar,
                    color = keyTextColor,
                    fontSize = if (key.type == KeyType.MODIFIER) 13.sp else 20.sp,
                    fontWeight = if (key.type == KeyType.MODIFIER) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (key.type == KeyType.CHARACTER) {
            Box(
                modifier = Modifier
                    .align(visualAlignment)
                    .offset(x = popupHorizontalShiftDp, y = (-60).dp)
                    .requiredSize(KeyboardDimens.popupCircleSize)
                    .graphicsLayer {
                        alpha = if (isPressedDown && !showPopup) 1f else 0f
                    }
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .background(popupBg, CircleShape)
                    .border(0.5.dp, popupBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayedChar,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = unselectedPopupTextColor
                )
            }
        }

        if (showPopup) {
            if (key.type == KeyType.SHIFT) {
                val totalShiftHeight = KeyboardDimens.popupCircleSize * 3

                Column(
                    modifier = Modifier
                        .align(visualAlignment)
                        .offset(x = popupHorizontalShiftDp, y = (-60).dp - KeyboardDimens.popupCircleSize)
                        .wrapContentSize(unbounded = true)
                        .requiredWidth(KeyboardDimens.popupCircleSize)
                        .requiredHeight(totalShiftHeight)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .background(popupBg, CircleShape)
                        .border(0.5.dp, popupBorder, CircleShape),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isShiftMode = viewModel.shiftState.value != marcinlowercase.w.model.ShiftState.OFF
                    val tabText = if (isShiftMode) "TAB" else "tab"
                    val escText = if (isShiftMode) "ESC" else "esc"

                    val shiftOptions = listOf(
                        ShiftHoldAction.TAB,
                        ShiftHoldAction.NONE,
                        ShiftHoldAction.ESC
                    )

                    shiftOptions.forEach { action ->
                        val isSelected = shiftHoldAction == action
                        val itemColor = if (isSelected) Color.White else unselectedPopupTextColor

                        Box(
                            modifier = Modifier.requiredSize(KeyboardDimens.popupCircleSize),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .requiredSize(KeyboardDimens.popupCircleSize - 6.dp)
                                        .background(Color(0xFF2563EB), CircleShape)
                                )
                            }
                            when (action) {
                                ShiftHoldAction.TAB -> Text(
                                    text = tabText,
                                    color = itemColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                ShiftHoldAction.NONE -> Icon(
                                    painter = painterResource(id = R.drawable.ic_shift_on),
                                    contentDescription = null,
                                    tint = itemColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                ShiftHoldAction.ESC -> Text(
                                    text = escText,
                                    color = itemColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                val sec = when {
                    key.secondaries?.hasOptions() == true -> key.secondaries
                    key.popups.isNotEmpty() -> when (key.popups.size) {
                        1 -> SecondaryBindings(center = key.popups[0])
                        2 -> SecondaryBindings(center = key.popups[0], right = key.popups[1])
                        3 -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2])
                        4 -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2], bottom = key.popups[3])
                        else -> SecondaryBindings(center = key.popups[0], top = key.popups[1], right = key.popups[2], bottom = key.popups[3], left = key.popups[4])
                    }
                    else -> null
                }

                if (sec?.hasOptions() == true) {
                    val slotSize = KeyboardDimens.popupCircleSize
                    val cardPadding = 4.dp
                    val cardCornerRadius = slotSize / 2

                    val hasTop = sec.top != null
                    val hasBottom = sec.bottom != null
                    val hasLeft = sec.left != null
                    val hasRight = sec.right != null

                    val rowsCount = when {
                        hasTop && hasBottom -> 3
                        hasTop || hasBottom -> 2
                        else -> 1
                    }

                    val columnsCount = when {
                        hasLeft && hasRight -> 3
                        hasLeft || hasRight -> 2
                        else -> 1
                    }

                    val cardWidth = (slotSize * columnsCount) + (cardPadding * 2)
                    val cardHeight = (slotSize * rowsCount) + (cardPadding * 2)

                    val xAlignmentOffset = when {
                        !hasLeft && hasRight -> (slotSize / 2)
                        hasLeft && !hasRight -> -(slotSize / 2)
                        else -> 0.dp
                    }

                    Box(
                        modifier = Modifier
                            .align(visualAlignment)
                            .offset(x = popupHorizontalShiftDp + xAlignmentOffset, y = -cardHeight - 6.dp + popupVerticalShiftDp)
                            .wrapContentSize(unbounded = true)
                            .requiredWidth(cardWidth)
                            .requiredHeight(cardHeight)
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(cardCornerRadius))
                            .background(popupBg, RoundedCornerShape(cardCornerRadius))
                            .border(0.5.dp, popupBorder, RoundedCornerShape(cardCornerRadius))
                            .padding(cardPadding)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (hasTop) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (hasLeft) Spacer(modifier = Modifier.requiredSize(slotSize))

                                    Box(
                                        modifier = Modifier.requiredSize(slotSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        sec.top?.let { symbol ->
                                            val isSelected = selectedDirection == PopupDirection.TOP
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .requiredSize(slotSize - 6.dp)
                                                        .background(Color(0xFF2563EB), CircleShape)
                                                )
                                            }
                                            Text(
                                                text = symbol,
                                                color = if (isSelected) Color.White else unselectedPopupTextColor,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    if (hasRight) Spacer(modifier = Modifier.requiredSize(slotSize))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (hasLeft) {
                                    Box(
                                        modifier = Modifier.requiredSize(slotSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        sec.left?.let { symbol ->
                                            val isSelected = selectedDirection == PopupDirection.LEFT
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .requiredSize(slotSize - 6.dp)
                                                        .background(Color(0xFF2563EB), CircleShape)
                                                )
                                            }
                                            Text(
                                                text = symbol,
                                                color = if (isSelected) Color.White else unselectedPopupTextColor,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier.requiredSize(slotSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    sec.center?.let { symbol ->
                                        val isSelected = selectedDirection == PopupDirection.CENTER
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .requiredSize(slotSize - 6.dp)
                                                    .background(Color(0xFF2563EB), CircleShape)
                                            )
                                        }
                                        Text(
                                            text = symbol,
                                            color = if (isSelected) Color.White else unselectedPopupTextColor,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (hasRight) {
                                    Box(
                                        modifier = Modifier.requiredSize(slotSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        sec.right?.let { symbol ->
                                            val isSelected = selectedDirection == PopupDirection.RIGHT
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .requiredSize(slotSize - 6.dp)
                                                        .background(Color(0xFF2563EB), CircleShape)
                                                )
                                            }
                                            Text(
                                                text = symbol,
                                                color = if (isSelected) Color.White else unselectedPopupTextColor,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            if (hasBottom) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (hasLeft) Spacer(modifier = Modifier.requiredSize(slotSize))

                                    Box(
                                        modifier = Modifier.requiredSize(slotSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        sec.bottom?.let { symbol ->
                                            val isSelected = selectedDirection == PopupDirection.BOTTOM
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .requiredSize(slotSize - 6.dp)
                                                        .background(Color(0xFF2563EB), CircleShape)
                                                )
                                            }
                                            Text(
                                                text = symbol,
                                                color = if (isSelected) Color.White else unselectedPopupTextColor,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    if (hasRight) Spacer(modifier = Modifier.requiredSize(slotSize))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceKeyView(key: KeyDefinition) {
    val viewModel = LocalKeyboardViewModel.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val spaceBg = if (isDark) Color(0xFF1C1C1E) else Color.White
    val spaceBorder = if (isDark) Color(0xFF38383A) else Color(0xFFD1D5DB)
    val spaceTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF9CA3AF)
    val cellHeight = KeyboardDimens.keyHeight + KeyboardDimens.rowGap

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .pointerInput(key.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    var accumulatedDrag = 0f
                    val stepThreshold = 30f
                    var hasGlided = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (!change.pressed) {
                            if (!hasGlided) {
                                viewModel.pressKey(key)
                            } else {
                                viewModel.onGlideEnd()
                            }
                            break
                        } else {
                            val dragX = change.position.x - change.previousPosition.x
                            accumulatedDrag += dragX

                            if (kotlin.math.abs(accumulatedDrag) >= stepThreshold) {
                                hasGlided = true
                                val steps = (accumulatedDrag / stepThreshold).toInt()
                                if (steps != 0) {
                                    val moved = viewModel.glideCursor(steps)
                                    if (moved) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    accumulatedDrag -= steps * stepThreshold
                                }
                            }
                        }
                    }
                }
            }
            .padding(
                horizontal = KeyboardDimens.keyGap / 2,
                vertical = KeyboardDimens.rowGap / 2
            )
            .clip(RoundedCornerShape(KeyboardDimens.keyCornerRadius))
            .background(spaceBg)
            .border(0.5.dp, spaceBorder, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "space",
                color = spaceTextColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}