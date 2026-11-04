package marcinlowercase.w.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import marcinlowercase.w.R
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.PopupDirection
import marcinlowercase.w.model.SecondaryBindings
import kotlin.time.Duration.Companion.milliseconds

object KeyboardDimens {
    val popupHeadroom = 300.dp
    val keyHeight = 53.dp
    val rowGap = 4.dp
    val keyGap = 4.dp
    val ribbonHeight = 38.dp
    val keyCornerRadius = 12.dp
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

    // Explicitly observe the State so recomposition triggers when layout changes
    val config by viewModel.layoutConfig

    CompositionLocalProvider(LocalKeyboardViewModel provides viewModel) {

        val isDark = isSystemInDarkTheme()
        val keyboardBgColor = if (isDark) Color.Black else Color.White

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            Spacer(modifier = Modifier.height(KeyboardDimens.popupHeadroom))

            val dynamicBottomPadding = viewModel.bottomChinPaddingDp.value.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(keyboardBgColor)
                    .navigationBarsPadding()
                    .padding(
                        start = KeyboardDimens.keyGap / 2,
                        end = KeyboardDimens.keyGap / 2,
                        top = KeyboardDimens.keyGap / 2,
                        bottom = 0.dp
                    )
            ) {
                if (config.ribbon.isNotEmpty()) {
                    RibbonRow(keys = config.ribbon)
                    Spacer(modifier = Modifier.height(KeyboardDimens.rowGap))
                }

                config.rows.forEachIndexed { rowIndex, row ->
                    val isBottomRow = rowIndex == config.rows.lastIndex
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
                                        SpaceKeyView(key = key, isBottomRow = isBottomRow)
                                    } else {
                                        RemappableKeyView(key = key, isBottomRow = isBottomRow)
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
    val activeFont = viewModel.fontFamily.value

    val ribbonBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFEEEEEE)
    val ribbonTextColor = if (isDark) Color.White else Color.Black

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
                    .pointerInput(key.id) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.playKeySound(KeyType.MODIFIER)
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
                        fontFamily = activeFont
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
fun RemappableKeyView(key: KeyDefinition, isBottomRow: Boolean = false) {
    val viewModel = LocalKeyboardViewModel.current
    val liveKey = viewModel.layoutConfig.value.rows.flatMap { it.keys }
        .find { it.id == key.id } ?: key
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

    val popupTransitionProgress by animateFloatAsState(
        targetValue = if (showPopup) 1f else 0f,
        animationSpec = if (showPopup) {
            tween(durationMillis = 70, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "popupTransition"
    )
    val highlightColor = viewModel.highlightColor.value
    val onHighlightColor = if (highlightColor.luminance() > 0.5f) Color.Black else Color.White

    val isShiftActive = viewModel.shiftState.value != marcinlowercase.w.model.ShiftState.OFF
    val isCtrlArmed = key.id == "ctrl" && viewModel.ctrlArmed.value
    val isAltArmed = key.id == "alt" && viewModel.altArmed.value
    val isModifierArmed = (key.type == KeyType.SHIFT && isShiftActive) || isCtrlArmed || isAltArmed

    val displayedChar = viewModel.resolveDisplayedChar(key)

    val keyBackground = when {
        isModifierArmed -> highlightColor
        isPressedDown -> if (isDark) Color.Black else Color.White
        key.type == KeyType.CHARACTER -> if (isDark) Color(0xFF1C1C1E) else Color(0xFFEEEEEE)
        else -> if (isDark) Color(0xFF2C2C2E) else Color(0xFFDCE0E5)
    }
    val keyTextColor = when {
        isModifierArmed -> onHighlightColor
        isDark -> Color.White
        else -> Color.Black
    }

    val hintColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF9CA3AF)
    val popupBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFEEEEEE)
//    val popupBorder = if (isDark) Color(0xFF38383A) else Color(0xFFD1D5DB)
    val unselectedPopupTextColor = if (isDark) Color.White else Color.Black

    val activeFont = viewModel.fontFamily.value

    val visualWidthFraction = if (key.id == "a" || key.id == "l") 0.666667f else 1f
    val visualAlignment = when (key.id) {
        "a" -> Alignment.CenterEnd
        "l" -> Alignment.CenterStart
        else -> Alignment.Center
    }

    val extraBottomPadding = if (isBottomRow) viewModel.bottomChinPaddingDp.value.dp else 0.dp
    val cellHeight = KeyboardDimens.keyHeight + KeyboardDimens.rowGap + extraBottomPadding

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .zIndex(if (isPressedDown || showPopup) 10f else 0f)
            .pointerInput(key.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val currentKey = viewModel.getKey(key.id)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.playKeySound(currentKey.type)
                    isPressedDown = true

                    if (currentKey.type == KeyType.BACKSPACE) {
                        viewModel.pressKey(currentKey)

                        val releasedEarly = withTimeoutOrNull(350L) {
                            waitForUpOrCancellation()
                        }

                        if (releasedEarly == null) {
                            val repeatJob = coroutineScope.launch {
                                while (isActive) {
                                    delay(50L.milliseconds)
                                    val deleted = viewModel.pressKey(viewModel.getKey(key.id))
                                    if (deleted) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.playKeySound(KeyType.BACKSPACE)
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
                        viewModel.pressKey(viewModel.getKey(key.id))
                    } else {
                        isPressedDown = false
                        val activeKey = viewModel.getKey(key.id)
                        if (activeKey.type == KeyType.SHIFT) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            shiftHoldAction = ShiftHoldAction.TAB
                            viewModel.activePopupKeyId.value = activeKey.id
                            showPopup = true

                            val startY = down.position.y
                            val verticalThresholdPx = with(density) { 26.dp.toPx() }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val deltaY = change.position.y - startY
                                    val newAction = if (deltaY > verticalThresholdPx) {
                                        ShiftHoldAction.ESC
                                    } else {
                                        ShiftHoldAction.TAB
                                    }
                                    if (newAction != shiftHoldAction) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        shiftHoldAction = newAction
                                    }
                                } else {
                                    viewModel.activePopupKeyId.value = null
                                    when (shiftHoldAction) {
                                        ShiftHoldAction.TAB,
                                        ShiftHoldAction.NONE -> viewModel.pressKey(KeyDefinition("tab", "TAB", type = KeyType.MODIFIER))
                                        ShiftHoldAction.ESC -> viewModel.pressKey(KeyDefinition("esc", "ESC", type = KeyType.MODIFIER))
                                    }
                                    showPopup = false
                                    break
                                }
                            }
                        } else {
                            val sec = when {
                                activeKey.secondaries?.hasOptions() == true -> activeKey.secondaries
                                activeKey.popups.isNotEmpty() -> when (activeKey.popups.size) {
                                    1 -> SecondaryBindings(center = activeKey.popups[0])
                                    2 -> SecondaryBindings(center = activeKey.popups[0], right = activeKey.popups[1])
                                    3 -> SecondaryBindings(center = activeKey.popups[0], top = activeKey.popups[1], right = activeKey.popups[2])
                                    4 -> SecondaryBindings(center = activeKey.popups[0], top = activeKey.popups[1], right = activeKey.popups[2], bottom = activeKey.popups[3])
                                    else -> SecondaryBindings(center = activeKey.popups[0], top = activeKey.popups[1], right = activeKey.popups[2], bottom = activeKey.popups[3], left = activeKey.popups[4])
                                }
                                else -> null
                            }

                            if (sec?.hasOptions() == true) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                selectedDirection = PopupDirection.CENTER
                                viewModel.activePopupKeyId.value = activeKey.id
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

                                        val isRightEdgeKey = activeKey.id in listOf("0", "p", "l")
                                        val canSwipeRight = sec.right != null && !isRightEdgeKey

                                        val newDir = when {
                                            absX < swipeThresholdPx && absY < swipeThresholdPx -> PopupDirection.CENTER
                                            absY >= absX -> if (deltaY < 0 && sec.top != null) PopupDirection.TOP else if (deltaY > 0 && sec.bottom != null) PopupDirection.BOTTOM else PopupDirection.CENTER
                                            else -> if (deltaX < 0 && sec.left != null) PopupDirection.LEFT else if (deltaX > 0 && canSwipeRight) PopupDirection.RIGHT else PopupDirection.CENTER
                                        }

                                        if (newDir != selectedDirection) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            selectedDirection = newDir
                                        }
                                    } else {
                                        viewModel.activePopupKeyId.value = null
                                        viewModel.commitSecondary(activeKey.id, selectedDirection)
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
                    start = KeyboardDimens.keyGap / 2,
                    end = KeyboardDimens.keyGap / 2,
                    top = KeyboardDimens.rowGap / 2,
                    bottom = (KeyboardDimens.rowGap / 2) + extraBottomPadding
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
        ) {
            if (key.hint != null) {
                Text(
                    text = key.hint,
                    color = hintColor,
                    fontSize = 11.sp,
                    fontFamily = activeFont,
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
                KeyType.BACKSPACE -> if (isShiftActive) R.drawable.ic_delete else R.drawable.ic_backspace
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
                val visualLabel = key.displayLabel ?: displayedChar
                val labelFontSize = if (key.type == KeyType.MODIFIER) {
                    13.sp
                } else {
                    when (visualLabel.length) {
                        0, 1 -> 20.sp
                        2 -> 15.sp
                        3 -> 13.sp
                        else -> 11.sp
                    }
                }

                Text(
                    text = visualLabel,
                    color = keyTextColor,
                    fontSize = labelFontSize,
                    fontWeight = if (key.type == KeyType.MODIFIER) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = activeFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        val slotWidth = if (keycapWidthDp > 0.dp) keycapWidthDp else (KeyboardDimens.keyHeight * 0.65f)
        val slotHeight = KeyboardDimens.keyHeight
        val primaryPopupMargin = KeyboardDimens.rowGap
        val primaryPopupWidth = slotWidth + (primaryPopupMargin * 2)
        val primaryPopupHeight = slotHeight + (primaryPopupMargin * 2)

        if (key.type == KeyType.CHARACTER) {
            val previewPlacementY = -(KeyboardDimens.rowGap + slotHeight + primaryPopupMargin)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = keycapCenterOffsetInCellDp, y = previewPlacementY)
                    .requiredWidth(primaryPopupWidth)
                    .requiredHeight(primaryPopupHeight)
                    .graphicsLayer {
                        val previewAlpha = if (isPressedDown && !showPopup) {
                            1f
                        } else if (showPopup) {
                            1f - popupTransitionProgress
                        } else {
                            0f
                        }
                        alpha = previewAlpha
                    }
                    .background(highlightColor, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .border(0.5.dp, highlightColor, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .padding(primaryPopupMargin),
                contentAlignment = Alignment.Center
            ) {
                val previewLabel = key.displayLabel ?: displayedChar
                val previewFontSize = when (previewLabel.length) {
                    0, 1 -> 26.sp
                    2 -> 20.sp
                    3 -> 16.sp
                    else -> 13.sp
                }

                Text(
                    text = previewLabel,
                    fontSize = previewFontSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = activeFont,
                    color = onHighlightColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showPopup) {
            if (key.type == KeyType.SHIFT) {
                val shiftRowCount = 2
                val popupMargin = KeyboardDimens.rowGap
                val shiftPopupHeight = (slotHeight * shiftRowCount) + (KeyboardDimens.rowGap * (shiftRowCount - 1)) + (popupMargin * 2)
                val shiftPopupWidth = slotWidth + (popupMargin * 2)
                val shiftPlacementY = -(slotHeight * 1.5f + KeyboardDimens.rowGap * 1.5f + popupMargin)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = keycapCenterOffsetInCellDp, y = shiftPlacementY)
                        .wrapContentSize(unbounded = true)
                        .requiredWidth(shiftPopupWidth)
                        .requiredHeight(shiftPopupHeight)
                        .graphicsLayer {
                            alpha = popupTransitionProgress
                            scaleX = 0.88f + (0.12f * popupTransitionProgress)
                            scaleY = 0.88f + (0.12f * popupTransitionProgress)
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .background(popupBg, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
//                        .border(0.5.dp, popupBorder, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                        .padding(popupMargin)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(KeyboardDimens.rowGap)
                    ) {
                        val isShiftMode = viewModel.shiftState.value != marcinlowercase.w.model.ShiftState.OFF
                        val tabText = if (isShiftMode) "TAB" else "tab"
                        val escText = if (isShiftMode) "ESC" else "esc"

                        val shiftOptions = listOf(
                            ShiftHoldAction.TAB,
                            ShiftHoldAction.ESC
                        )

                        shiftOptions.forEach { action ->
                            val isSelected = shiftHoldAction == action
                            val itemColor = if (isSelected) onHighlightColor else unselectedPopupTextColor

                            Box(
                                modifier = Modifier
                                    .requiredWidth(slotWidth)
                                    .requiredHeight(slotHeight)
                                    .background(
                                        if (isSelected) highlightColor else Color.Transparent,
                                        RoundedCornerShape(KeyboardDimens.keyCornerRadius - KeyboardDimens.keyGap)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (action == ShiftHoldAction.TAB) tabText else escText,
                                    color = itemColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = activeFont
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
                    val popupMargin = KeyboardDimens.rowGap
                    val popupHeight = (slotHeight * rowCount) + (KeyboardDimens.rowGap * (rowCount - 1)) + (popupMargin * 2)
                    val popupWidth = (slotWidth * colCount) + (KeyboardDimens.keyGap * (colCount - 1)) + (popupMargin * 2)

                    val xOffset = when {
                        !hasLeft && hasRight -> (slotWidth + KeyboardDimens.keyGap) / 2
                        hasLeft && !hasRight -> -((slotWidth + KeyboardDimens.keyGap) / 2)
                        else -> 0.dp
                    }

                    val popupPlacementY = when (rowCount) {
                        3 -> -(slotHeight * 2 + KeyboardDimens.rowGap * 2 + popupMargin)
                        2 -> -(slotHeight * 1.5f + KeyboardDimens.rowGap * 1.5f + popupMargin)
                        1 -> -(KeyboardDimens.rowGap + slotHeight + popupMargin)
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
                            .graphicsLayer {
                                alpha = popupTransitionProgress
                                scaleX = 0.88f + (0.12f * popupTransitionProgress)
                                scaleY = 0.88f + (0.12f * popupTransitionProgress)
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                            .background(popupBg, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
//                            .border(0.5.dp, popupBorder, RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                            .padding(popupMargin)
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
                                                if (isSelected) highlightColor else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius - KeyboardDimens.keyGap)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val topFontSize = when (sec.top.length) {
                                            0, 1 -> 22.sp
                                            2 -> 16.sp
                                            3 -> 13.sp
                                            else -> 11.sp
                                        }

                                        Text(
                                            text = sec.top,
                                            color = if (isSelected) onHighlightColor else unselectedPopupTextColor,
                                            fontSize = topFontSize,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = activeFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                                if (isSelected) highlightColor else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius - KeyboardDimens.keyGap)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val leftFontSize = when (sec.left.length) {
                                            0, 1 -> 22.sp
                                            2 -> 16.sp
                                            3 -> 13.sp
                                            else -> 11.sp
                                        }

                                        Text(
                                            text = sec.left,
                                            color = if (isSelected) onHighlightColor else unselectedPopupTextColor,
                                            fontSize = leftFontSize,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = activeFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                val isCenterSelected = selectedDirection == PopupDirection.CENTER
                                Box(
                                    modifier = Modifier
                                        .requiredWidth(slotWidth)
                                        .requiredHeight(slotHeight)
                                        .background(
                                            if (isCenterSelected) highlightColor else Color.Transparent,
                                            RoundedCornerShape(KeyboardDimens.keyCornerRadius - KeyboardDimens.keyGap)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val centerText = sec.center ?: ""
                                    val centerFontSize = when (centerText.length) {
                                        0, 1 -> 22.sp
                                        2 -> 16.sp
                                        3 -> 13.sp
                                        else -> 11.sp
                                    }

                                    Text(
                                        text = centerText,
                                        color = if (isCenterSelected) onHighlightColor else unselectedPopupTextColor,
                                        fontSize = centerFontSize,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = activeFont,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (hasRight) {
                                    val isSelected = selectedDirection == PopupDirection.RIGHT
                                    Box(
                                        modifier = Modifier
                                            .requiredWidth(slotWidth)
                                            .requiredHeight(slotHeight)
                                            .background(
                                                if (isSelected) highlightColor else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius - KeyboardDimens.keyGap)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val rightFontSize = when (sec.right.length) {
                                            0, 1 -> 22.sp
                                            2 -> 16.sp
                                            3 -> 13.sp
                                            else -> 11.sp
                                        }

                                        Text(
                                            text = sec.right,
                                            color = if (isSelected) onHighlightColor else unselectedPopupTextColor,
                                            fontSize = rightFontSize,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = activeFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                                if (isSelected) highlightColor else Color.Transparent,
                                                RoundedCornerShape(KeyboardDimens.keyCornerRadius - KeyboardDimens.keyGap)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bottomFontSize = when (sec.bottom.length) {
                                            0, 1 -> 22.sp
                                            2 -> 16.sp
                                            3 -> 13.sp
                                            else -> 11.sp
                                        }

                                        Text(
                                            text = sec.bottom,
                                            color = if (isSelected) onHighlightColor else unselectedPopupTextColor,
                                            fontSize = bottomFontSize,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = activeFont,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
fun SpaceKeyView(key: KeyDefinition, isBottomRow: Boolean = false) {
    val viewModel = LocalKeyboardViewModel.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    var isPressedDown by remember { mutableStateOf(false) }
    val isShiftOn = viewModel.shiftState.value != marcinlowercase.w.model.ShiftState.OFF

    val extraBottomPadding = if (isBottomRow) viewModel.bottomChinPaddingDp.value.dp else 0.dp
    val spaceBg = when {
        isPressedDown -> if (isDark) Color(0xFF323236) else Color(0xFFD1D5DB)
        isDark -> Color(0xFF1C1C1E)
        else -> Color(0xFFEEEEEE)
    }
    val spaceTextColor = if (isDark) Color.White else Color.Black
    val cellHeight = KeyboardDimens.keyHeight + KeyboardDimens.rowGap + extraBottomPadding
    val spaceLabel = if (isShiftOn) "SPACE" else "space"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .pointerInput(key.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.playKeySound(KeyType.SPACE)
                    isPressedDown = true

                    var accumulatedDrag = 0f
                    val stepThreshold = 30f
                    var hasGlided = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (!change.pressed) {
                            isPressedDown = false
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
                start = KeyboardDimens.keyGap / 2,
                end = KeyboardDimens.keyGap / 2,
                top = KeyboardDimens.rowGap / 2,
                bottom = (KeyboardDimens.rowGap / 2) + extraBottomPadding
            )
            .clip(RoundedCornerShape(KeyboardDimens.keyCornerRadius))
            .background(spaceBg)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = spaceLabel,
                color = spaceTextColor,
                fontSize = 12.sp,
                fontFamily = viewModel.fontFamily.value
            )
        }
    }
}