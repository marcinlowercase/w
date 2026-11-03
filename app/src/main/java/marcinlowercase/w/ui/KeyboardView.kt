package marcinlowercase.w.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import marcinlowercase.w.R
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.PopupDirection
import marcinlowercase.w.model.SecondaryBindings

object KeyboardDimens {
    val popupHeadroom = 160.dp
    val keyHeight = 53.dp
    val rowGap = 6.dp
    val keyGap = 6.dp
    val popupCircleSize = keyHeight + rowGap
    val ribbonHeight = 38.dp
    val bottomChinPadding = 32.dp
    val topPadding = 8.dp
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
            .height(KeyboardDimens.ribbonHeight)
            .graphicsLayer {
                alpha = if (viewModel.activePopupKeyId.value != null) 0.35f else 1f
            },
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
    var selectedDirection by remember { mutableStateOf(PopupDirection.CENTER) }
    var shiftHoldAction by remember { mutableStateOf(ShiftHoldAction.NONE) }
    var popupHorizontalShiftDp by remember { mutableStateOf(0.dp) }
    var keycapCenterOffsetInCellDp by remember { mutableStateOf(0.dp) }
    var keycapWidthDp by remember { mutableStateOf(0.dp) }

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
    val popupAlignment = when (key.id) {
        "a" -> Alignment.TopEnd
        "l" -> Alignment.TopStart
        else -> Alignment.TopCenter
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
                            viewModel.activePopupKeyId.value = key.id
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
                                    viewModel.activePopupKeyId.value = null
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
                                viewModel.activePopupKeyId.value = key.id
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
                                        viewModel.activePopupKeyId.value = null
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
                .graphicsLayer {
                    val activeId = viewModel.activePopupKeyId.value
                    alpha = if (activeId != null && activeId != key.id) 0.35f else 1f
                }
                .padding(
                    horizontal = KeyboardDimens.keyGap / 2,
                    vertical = KeyboardDimens.rowGap / 2
                )
                .onGloballyPositioned { coordinates ->
                    val rootWidth = coordinates.findRootCoordinates().size.width.toFloat()
                    val keyCenterInRoot = coordinates.positionInRoot().x + (coordinates.size.width / 2f)
                    val measuredKeyWidth = with(density) { coordinates.size.width.toDp() }
                    if (keycapWidthDp != measuredKeyWidth) {
                        keycapWidthDp = measuredKeyWidth
                    }

                    val parentCoords = coordinates.parentLayoutCoordinates
                    if (parentCoords != null) {
                        val cellCenterInRoot = parentCoords.positionInRoot().x + (parentCoords.size.width / 2f)
                        val offsetPx = keyCenterInRoot - cellCenterInRoot
                        val offsetDp = with(density) { offsetPx.toDp() }
                        if (keycapCenterOffsetInCellDp != offsetDp) {
                            keycapCenterOffsetInCellDp = offsetDp
                        }
                    }

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

                    val leftBoundaryPx = keyCenterInRoot - with(density) { 30.dp.toPx() }
                    val rightBoundaryPx = keyCenterInRoot + with(density) { 30.dp.toPx() }

                    val shiftPx = when {
                        leftBoundaryPx < 0f -> -leftBoundaryPx
                        rightBoundaryPx > rootWidth -> rootWidth - rightBoundaryPx
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

        val slotWidth = if (keycapWidthDp > 0.dp) keycapWidthDp else (KeyboardDimens.keyHeight * 0.65f)
        val slotHeight = KeyboardDimens.keyHeight

        if (key.type == KeyType.CHARACTER) {
            val previewPlacementY = -(KeyboardDimens.rowGap / 2) - slotHeight

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = keycapCenterOffsetInCellDp, y = previewPlacementY)
                    .requiredWidth(slotWidth)
                    .requiredHeight(slotHeight)
                    .graphicsLayer {
                        alpha = if (isPressedDown && !showPopup) 1f else 0f
                    }
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .background(Color(0xFF2563EB), RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .border(0.5.dp, Color(0xFF2563EB), RoundedCornerShape(KeyboardDimens.keyCornerRadius)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayedChar,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }

        if (showPopup) {
            if (key.type == KeyType.SHIFT) {
                val totalShiftHeight = (slotHeight * 3) + (KeyboardDimens.rowGap * 2)
                val shiftPlacementY = -(KeyboardDimens.rowGap / 2) - totalShiftHeight

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = keycapCenterOffsetInCellDp, y = shiftPlacementY)
                        .wrapContentSize(unbounded = true)
                        .requiredWidth(slotWidth)
                        .requiredHeight(totalShiftHeight)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                        .background(popupBg, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                        .border(0.5.dp, keyBorderColor, RoundedCornerShape(KeyboardDimens.keyCornerRadius)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(KeyboardDimens.rowGap)
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
                            modifier = Modifier
                                .requiredWidth(slotWidth)
                                .requiredHeight(slotHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(slotWidth - 6.dp)
                                        .requiredHeight(slotHeight - 6.dp)
                                        .background(Color(0xFF2563EB), RoundedCornerShape(KeyboardDimens.keyCornerRadius))
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
                    val hasTop = sec.top != null
                    val hasBottom = sec.bottom != null
                    val hasLeft = sec.left != null
                    val hasRight = sec.right != null

                    val rowCount = (if (hasTop) 1 else 0) + 1 + (if (hasBottom) 1 else 0)
                    val colCount = (if (hasLeft) 1 else 0) + 1 + (if (hasRight) 1 else 0)

                    val popupHeight = (slotHeight * rowCount) + (KeyboardDimens.rowGap * (rowCount - 1))
                    val popupWidth = (slotWidth * colCount) + (KeyboardDimens.keyGap * (colCount - 1))

                    val xOffset = when {
                        !hasLeft && hasRight -> (slotWidth + KeyboardDimens.keyGap) / 2
                        hasLeft && !hasRight -> -((slotWidth + KeyboardDimens.keyGap) / 2)
                        else -> 0.dp
                    }

                    val popupPlacementY = when (rowCount) {
                        3 -> -(slotHeight * 2 + KeyboardDimens.rowGap * 2)
                        2 -> -(slotHeight * 1.5f + KeyboardDimens.rowGap * 1.5f)
                        1 -> -(KeyboardDimens.rowGap / 2 + slotHeight)
                        else -> 0.dp
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(
                                x = keycapCenterOffsetInCellDp + xOffset,
                                y = popupPlacementY
                            )
                            .wrapContentSize(unbounded = true)
                            .requiredWidth(popupWidth)
                            .requiredHeight(popupHeight)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                            .background(popupBg, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                            .border(0.5.dp, popupBorder, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(KeyboardDimens.rowGap)
                        ) {
                            if (hasTop) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(KeyboardDimens.keyGap)
                                ) {
                                    if (hasLeft) Spacer(modifier = Modifier.requiredWidth(slotWidth).requiredHeight(slotHeight))

                                    val isSelected = selectedDirection == PopupDirection.TOP
                                    Box(
                                        modifier = Modifier
                                            .requiredWidth(slotWidth)
                                            .requiredHeight(slotHeight)
                                            .background(
                                                if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sec.top ?: "",
                                            color = if (isSelected) Color.White else unselectedPopupTextColor,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    if (hasRight) Spacer(modifier = Modifier.requiredWidth(slotWidth).requiredHeight(slotHeight))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(KeyboardDimens.keyGap)
                            ) {
                                if (hasLeft) {
                                    val isSelected = selectedDirection == PopupDirection.LEFT
                                    Box(
                                        modifier = Modifier
                                            .requiredWidth(slotWidth)
                                            .requiredHeight(slotHeight)
                                            .background(
                                                if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sec.left ?: "",
                                            color = if (isSelected) Color.White else unselectedPopupTextColor,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                val isCenterSelected = selectedDirection == PopupDirection.CENTER
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(slotWidth)
                                        .requiredHeight(slotHeight)
                                        .background(
                                            if (isCenterSelected) Color(0xFF2563EB) else Color.Transparent,
                                            RoundedCornerShape(KeyboardDimens.keyCornerRadius)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sec.center ?: "",
                                        color = if (isCenterSelected) Color.White else unselectedPopupTextColor,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (hasRight) {
                                    val isSelected = selectedDirection == PopupDirection.RIGHT
                                    Box(
                                        modifier = Modifier
                                            .requiredWidth(slotWidth)
                                            .requiredHeight(slotHeight)
                                            .background(
                                                if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sec.right ?: "",
                                            color = if (isSelected) Color.White else unselectedPopupTextColor,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            if (hasBottom) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(KeyboardDimens.keyGap)
                                ) {
                                    if (hasLeft) Spacer(modifier = Modifier.requiredWidth(slotWidth).requiredHeight(slotHeight))

                                    val isSelected = selectedDirection == PopupDirection.BOTTOM
                                    Box(
                                        modifier = Modifier
                                            .requiredWidth(slotWidth)
                                            .requiredHeight(slotHeight)
                                            .background(
                                                if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sec.bottom ?: "",
                                            color = if (isSelected) Color.White else unselectedPopupTextColor,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    if (hasRight) Spacer(modifier = Modifier.requiredWidth(slotWidth).requiredHeight(slotHeight))
                                }
                            }
                        }
                    }
                }            }
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
            .graphicsLayer {
                alpha = if (viewModel.activePopupKeyId.value != null) 0.35f else 1f
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