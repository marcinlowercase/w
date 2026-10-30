package marcinlowercase.w.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.KeyboardLayoutConfig

object KeyboardDimens {
    val popupHeadroom = 60.dp
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            Spacer(modifier = Modifier.height(KeyboardDimens.popupHeadroom))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = KeyboardDimens.rowGap / 2),
                        horizontalArrangement = Arrangement.spacedBy(KeyboardDimens.keyGap)
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
                    .border(0.5.dp, Color(0xFFE2E2E2), RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                    .pointerInput(key.id) {
                        detectTapGestures {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.pressKey(key)
                        }
                    },
                color = Color(0xFFF3F4F6)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = key.primaryChar,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun RemappableKeyView(key: KeyDefinition) {
    val viewModel = LocalKeyboardViewModel.current
    val view = LocalView.current
    val density = LocalDensity.current
    var isPressedDown by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var popupHorizontalShiftDp by remember { mutableStateOf(0.dp) }

    val isShiftActive = viewModel.shiftState.value != marcinlowercase.w.model.ShiftState.OFF
    val displayedChar = viewModel.resolveDisplayedChar(key)

    val keyBackground = when {
        key.type == KeyType.SHIFT && isShiftActive -> Color(0xFF2563EB)
        key.type == KeyType.CHARACTER -> Color.White
        else -> Color(0xFFE5E7EB)
    }

    val keyTextColor = when {
        key.type == KeyType.SHIFT && isShiftActive -> Color.White
        else -> Color(0xFF111827)
    }

    val visualWidthFraction = if (key.id == "a" || key.id == "l") 0.666667f else 1f
    val visualAlignment = when (key.id) {
        "a" -> Alignment.CenterEnd
        "l" -> Alignment.CenterStart
        else -> Alignment.Center
    }

    val popupMenuOffsetPx = with(density) { (-64).dp.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(KeyboardDimens.keyHeight)
            .zIndex(if (isPressedDown || showPopup) 10f else 0f)
            .onGloballyPositioned { coordinates ->
                val rootWidth = coordinates.findRootCoordinates().size.width.toFloat()
                val keyCenterInRoot = coordinates.positionInRoot().x + (coordinates.size.width / 2f)
                val popupRadiusPx = with(density) { (KeyboardDimens.popupCircleSize / 2f).toPx() }

                val popupLeftInRoot = keyCenterInRoot - popupRadiusPx
                val popupRightInRoot = keyCenterInRoot + popupRadiusPx

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
            .pointerInput(key.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    isPressedDown = true

                    val up = withTimeoutOrNull(220L) {
                        waitForUpOrCancellation()
                    }

                    if (up != null) {
                        isPressedDown = false
                        viewModel.pressKey(key)
                    } else {
                        isPressedDown = false
                        if (key.popups.isNotEmpty()) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            selectedIndex = 0
                            showPopup = true

                            val itemWidthPx = with(density) { KeyboardDimens.popupCircleSize.toPx() }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val newIndex = (change.position.x / itemWidthPx).toInt().coerceIn(0, key.popups.lastIndex)
                                    if (newIndex != selectedIndex) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        selectedIndex = newIndex
                                    }
                                } else {
                                    val symbol = key.popups.getOrNull(selectedIndex) ?: key.popups.first()
                                    viewModel.selectPopup(symbol)
                                    showPopup = false
                                    break
                                }
                            }
                        } else {
                            waitForUpOrCancellation()
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
                .clip(RoundedCornerShape(KeyboardDimens.keyCornerRadius))
                .background(keyBackground)
                .border(0.5.dp, Color(0xFFD1D5DB), RoundedCornerShape(KeyboardDimens.keyCornerRadius))
        ) {
            if (key.hint != null) {
                Text(
                    text = key.hint,
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }

            Text(
                text = displayedChar,
                color = keyTextColor,
                fontSize = if (key.type == KeyType.MODIFIER) 13.sp else 20.sp,
                fontWeight = if (key.type == KeyType.MODIFIER) FontWeight.SemiBold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Center)
            )
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
                    .background(Color.White, CircleShape)
                    .border(0.5.dp, Color(0xFFD1D5DB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayedChar,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF111827)
                )
            }
        }

        if (showPopup && key.popups.isNotEmpty()) {
            val activeSymbol = key.popups.getOrNull(selectedIndex.coerceAtLeast(0))
                ?: key.popups.firstOrNull()
                ?: ""

            Box(
                modifier = Modifier
                    .align(visualAlignment)
                    .offset(x = popupHorizontalShiftDp, y = (-60).dp)
                    .requiredSize(KeyboardDimens.popupCircleSize)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .background(Color.White, CircleShape)
                    .border(0.5.dp, Color(0xFFD1D5DB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeSymbol,
                    color = Color(0xFF2563EB),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        }
    }


@Composable
fun SpaceKeyView(key: KeyDefinition) {
    val viewModel = LocalKeyboardViewModel.current
    val view = LocalView.current
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val stepThreshold = 30f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(KeyboardDimens.keyHeight)
            .clip(RoundedCornerShape(KeyboardDimens.keyCornerRadius))
            .border(0.5.dp, Color(0xFFD1D5DB), RoundedCornerShape(KeyboardDimens.keyCornerRadius))
            .pointerInput(key.id) {
                detectTapGestures(
                    onPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val released = tryAwaitRelease()
                        if (released) {
                            viewModel.pressKey(key)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount.x
                        if (kotlin.math.abs(accumulatedDrag) >= stepThreshold) {
                            val steps = (accumulatedDrag / stepThreshold).toInt()
                            if (steps != 0) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                viewModel.glideCursor(steps)
                                accumulatedDrag -= steps * stepThreshold
                            }
                        }
                    }
                )
            },
        color = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "space",
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}