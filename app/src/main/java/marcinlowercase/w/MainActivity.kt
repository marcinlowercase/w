package marcinlowercase.w

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import marcinlowercase.w.data.KeyboardPreferences
import marcinlowercase.w.model.DefaultLayout
import marcinlowercase.w.model.KeyDefinition
import marcinlowercase.w.model.KeyType
import marcinlowercase.w.model.KeyboardLayoutConfig
import marcinlowercase.w.model.KeyboardRow
import marcinlowercase.w.model.SecondaryBindings
import marcinlowercase.w.ui.theme.WTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WTheme {
                var currentScreen by remember { mutableStateOf("settings") }

                if (currentScreen == "key_mapping") {
                    BackHandler { currentScreen = "settings" }
                    KeyMappingScreen(onNavigateBack = { currentScreen = "settings" })
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { SettingsTopBar() }
                    ) { innerPadding ->
                        SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToMapping = { currentScreen = "key_mapping" }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "the keyboard of oo1 studio",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToMapping: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { KeyboardPreferences(context) }
    val bottomPaddingState = preferences.bottomPaddingDp.collectAsState(initial = 24)
    val highlightColorState = preferences.highlightColorLong.collectAsState(initial = 0xFF001FFFL)
    val customFontNameState = preferences.customFontName.collectAsState(initial = null)
    val soundEnabledState = preferences.soundEnabled.collectAsState(initial = true)
    var testInputText by remember { mutableStateOf("") }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    val fontFile = File(context.filesDir, "custom_keyboard_font.ttf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        fontFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Custom Font"
                    preferences.setCustomFont(fontFile.absolutePath, fileName)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StatusCard(context)

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sound on Keypress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Play native system audio clicks when tapping keys.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabledState.value,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch {
                            preferences.setSoundEnabled(isChecked)
                        }
                    }
                )
            }
        }

        ColorPickerCard(
            currentColorLong = highlightColorState.value,
            onColorChanged = { colorVal ->
                coroutineScope.launch {
                    preferences.setHighlightColorLong(colorVal)
                }
            }
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Keyboard Font",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Import any TTF or OTF font file (like JetBrains Mono, Fira Code, or Hack) to use on all keycaps.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val highlightColorState = preferences.highlightColorLong.collectAsState(initial = 0xFF001FFF)
                val activeAccent = Color(highlightColorState.value)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Font:",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = customFontNameState.value ?: "Default Monospace",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeAccent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            fontPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import Font")
                    }

                    if (customFontNameState.value != null) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val fontFile = File(context.filesDir, "custom_keyboard_font.ttf")
                                    if (fontFile.exists()) fontFile.delete()
                                    preferences.setCustomFont(null, null)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Layout & Padding",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Adjust the bottom chin space to accommodate your device's screen size or navigation mode.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bottom Chin Padding",
                        fontWeight = FontWeight.Medium
                    )
                    val highlightColorState = preferences.highlightColorLong.collectAsState(initial = 0xFF001FFF)
                    val activeAccent = Color(highlightColorState.value)
                    Text(
                        text = "${bottomPaddingState.value} dp",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = activeAccent
                    )
                }

                Slider(
                    value = bottomPaddingState.value.toFloat(),
                    onValueChange = { newValue ->
                        coroutineScope.launch {
                            preferences.setBottomPaddingDp(newValue.toInt())
                        }
                    },
                    valueRange = 0f..64f,
                    steps = 31
                )
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Key Mapping",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Customize secondary options, directional flicks, and custom symbols for every keycap.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                FilledTonalButton(
                    onClick = onNavigateToMapping,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configure Key Bindings")
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Test Input",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    placeholder = { Text("Tap here to test padding and input") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun StatusCard(context: Context) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Keyboard Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("System Settings")
                }
                Button(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Switch Active")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyMappingScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { KeyboardPreferences(context) }

    val savedJson by preferences.layoutConfigJson.collectAsState(initial = null)
    var layoutConfig by remember { mutableStateOf(DefaultLayout.create()) }
    var selectedKeyId by remember { mutableStateOf("c") }

    LaunchedEffect(savedJson) {
        val currentJson = savedJson
        if (!currentJson.isNullOrEmpty()) {
            runCatching {
                layoutConfig = Json.decodeFromString<KeyboardLayoutConfig>(currentJson)
            }
        }
    }

    val highlightColorState = preferences.highlightColorLong.collectAsState(initial = 0xFF001FFF)
    val activeAccent = Color(highlightColorState.value)

    val selectedKey = remember(layoutConfig, selectedKeyId) {
        layoutConfig.rows.flatMap { it.keys }.find { it.id == selectedKeyId }
    }

    fun saveConfig(updated: KeyboardLayoutConfig) {
        layoutConfig = updated
        coroutineScope.launch {
            preferences.setLayoutConfigJson(Json.encodeToString(updated))
        }
    }

    fun updateSelectedKey(transform: (KeyDefinition) -> KeyDefinition) {
        val updatedRows = layoutConfig.rows.map { row ->
            KeyboardRow(
                keys = row.keys.map { key ->
                    if (key.id == selectedKeyId) transform(key) else key
                }
            )
        }
        saveConfig(layoutConfig.copy(rows = updatedRows))
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val jsonText = Json { prettyPrint = true }.encodeToString(layoutConfig)
                    stream.write(jsonText.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<KeyboardLayoutConfig>(text)
                    saveConfig(parsed)
                }
            }
        }
    }

    var pendingSvgTarget by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val svgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val svgText = stream.bufferedReader().readText()
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("svg_code", svgText))
                    pendingSvgTarget?.invoke(svgText)
                }
            }
        }
    }

    fun pickSvg(onLoaded: (String) -> Unit) {
        pendingSvgTarget = onLoaded
        svgPickerLauncher.launch(arrayOf("image/svg+xml", "image/*", "text/*", "*/*"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key Mapping", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) {
                        Text("Import")
                    }
                    TextButton(onClick = { exportLauncher.launch("w_keyboard_backup.json") }) {
                        Text("Export")
                    }
                    TextButton(onClick = {
                        val default = DefaultLayout.create()
                        saveConfig(default)
                    }) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select a keycap above to customize its bindings:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    layoutConfig.rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.keys.forEach { key ->
                                if (key.type == KeyType.SPACER) {
                                    Spacer(modifier = Modifier.weight(key.weight))
                                } else {
                                    val isCharacterKey = key.type == KeyType.CHARACTER
                                    val isSelected = isCharacterKey && key.id == selectedKeyId
                                    val onAccent = if (activeAccent.luminance() > 0.5f) Color.Black else Color.White
                                    val keyBg = when {
                                        isSelected -> activeAccent
                                        isCharacterKey -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    }
                                    val textColor = when {
                                        isSelected -> onAccent
                                        isCharacterKey -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(key.weight)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(keyBg)
                                            .border(
                                                if (isSelected) 1.5.dp else 0.5.dp,
                                                if (isSelected) Color.White else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable(enabled = isCharacterKey) {
                                                selectedKeyId = key.id
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val previewText = key.displayLabel ?: key.primaryChar
                                        if (marcinlowercase.w.ui.isSvgOrPath(previewText)) {
                                            marcinlowercase.w.ui.SvgPathIcon(
                                                raw = previewText,
                                                tint = textColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = previewText,
                                                color = textColor,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedKey != null && selectedKey.type == KeyType.CHARACTER) {
                val sec = selectedKey.secondaries ?: SecondaryBindings()
                val isFirstColumn = selectedKey.id in listOf("1", "q", "a")
                val isLastColumn = selectedKey.id in listOf("0", "p", "l")

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Editing: '${selectedKey.primaryChar}'",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        OutlinedTextField(
                            value = selectedKey.primaryChar,
                            onValueChange = { newChar ->
                                updateSelectedKey { it.copy(primaryChar = newChar) }
                            },
                            label = { Text("Primary Key / Macro Payload") },
                            placeholder = { Text("e.g. 'c' or 'Ctrl+C' or code snippet") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = selectedKey.displayLabel ?: "",
                            onValueChange = { newLabel ->
                                updateSelectedKey { it.copy(displayLabel = newLabel.ifEmpty { null }) }
                            },
                            label = { Text("Keycap Display Label (Optional)") },
                            placeholder = { Text("e.g. 'COPY' or SVG") },
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        pickSvg { svgText ->
                                            updateSelectedKey { it.copy(displayLabel = svgText.ifEmpty { null }) }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Open SVG", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider()

                        Text(
                            text = "Directional Secondaries (Plus Pattern)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        SecondaryDirectionEditor(
                            title = "Center (Default Hold / Corner Hint)",
                            contentValue = sec.center ?: selectedKey.hint ?: "",
                            onContentChange = { newContent ->
                                val cleaned = newContent.ifEmpty { null }
                                val displayCleaned = sec.centerDisplay?.ifEmpty { null }
                                val hintText = displayCleaned ?: cleaned
                                updateSelectedKey {
                                    it.copy(
                                        hint = hintText,
                                        secondaries = sec.copy(center = cleaned)
                                    )
                                }
                            },
                            displayValue = sec.centerDisplay ?: "",
                            onDisplayChange = { newDisplay ->
                                val cleanedDisplay = newDisplay.ifEmpty { null }
                                val centerContent = sec.center ?: selectedKey.hint
                                val hintText = cleanedDisplay ?: centerContent
                                updateSelectedKey {
                                    it.copy(
                                        hint = hintText,
                                        secondaries = sec.copy(centerDisplay = cleanedDisplay)
                                    )
                                }
                            },
                            onPickSvg = {
                                pickSvg { svgText ->
                                    val cleanedDisplay = svgText.ifEmpty { null }
                                    val centerContent = sec.center ?: selectedKey.hint
                                    val hintText = cleanedDisplay ?: centerContent
                                    updateSelectedKey {
                                        it.copy(
                                            hint = hintText,
                                            secondaries = sec.copy(centerDisplay = cleanedDisplay)
                                        )
                                    }
                                }
                            }
                        )

                        SecondaryDirectionEditor(
                            title = "Swipe Up",
                            contentValue = sec.top ?: "",
                            onContentChange = { newTop ->
                                updateSelectedKey {
                                    it.copy(secondaries = sec.copy(top = newTop.ifEmpty { null }))
                                }
                            },
                            displayValue = sec.topDisplay ?: "",
                            onDisplayChange = { newTopDisplay ->
                                updateSelectedKey {
                                    it.copy(secondaries = sec.copy(topDisplay = newTopDisplay.ifEmpty { null }))
                                }
                            },
                            onPickSvg = {
                                pickSvg { svgText ->
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(topDisplay = svgText.ifEmpty { null }))
                                    }
                                }
                            }
                        )

                        SecondaryDirectionEditor(
                            title = "Swipe Down",
                            contentValue = sec.bottom ?: "",
                            onContentChange = { newBottom ->
                                updateSelectedKey {
                                    it.copy(secondaries = sec.copy(bottom = newBottom.ifEmpty { null }))
                                }
                            },
                            displayValue = sec.bottomDisplay ?: "",
                            onDisplayChange = { newBottomDisplay ->
                                updateSelectedKey {
                                    it.copy(secondaries = sec.copy(bottomDisplay = newBottomDisplay.ifEmpty { null }))
                                }
                            },
                            onPickSvg = {
                                pickSvg { svgText ->
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(bottomDisplay = svgText.ifEmpty { null }))
                                    }
                                }
                            }
                        )

                        SecondaryDirectionEditor(
                            title = if (isFirstColumn) "Swipe Left (Edge disabled)" else "Swipe Left",
                            contentValue = if (isFirstColumn) "" else (sec.left ?: ""),
                            onContentChange = { newLeft ->
                                if (!isFirstColumn) {
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(left = newLeft.ifEmpty { null }))
                                    }
                                }
                            },
                            displayValue = if (isFirstColumn) "" else (sec.leftDisplay ?: ""),
                            onDisplayChange = { newLeftDisplay ->
                                if (!isFirstColumn) {
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(leftDisplay = newLeftDisplay.ifEmpty { null }))
                                    }
                                }
                            },
                            onPickSvg = {
                                pickSvg { svgText ->
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(leftDisplay = svgText.ifEmpty { null }))
                                    }
                                }
                            },
                            enabled = !isFirstColumn
                        )

                        SecondaryDirectionEditor(
                            title = if (isLastColumn) "Swipe Right (Edge disabled)" else "Swipe Right",
                            contentValue = if (isLastColumn) "" else (sec.right ?: ""),
                            onContentChange = { newRight ->
                                if (!isLastColumn) {
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(right = newRight.ifEmpty { null }))
                                    }
                                }
                            },
                            displayValue = if (isLastColumn) "" else (sec.rightDisplay ?: ""),
                            onDisplayChange = { newRightDisplay ->
                                if (!isLastColumn) {
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(rightDisplay = newRightDisplay.ifEmpty { null }))
                                    }
                                }
                            },
                            onPickSvg = {
                                pickSvg { svgText ->
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(rightDisplay = svgText.ifEmpty { null }))
                                    }
                                }
                            },
                            enabled = !isLastColumn
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecondaryDirectionEditor(
    title: String,
    contentValue: String,
    onContentChange: (String) -> Unit,
    displayValue: String,
    onDisplayChange: (String) -> Unit,
    onPickSvg: () -> Unit,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = if (enabled) contentValue else "",
                onValueChange = onContentChange,
                enabled = enabled,
                label = { Text("Payload / Macro") },
                placeholder = { Text("Symbol or macro") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = if (enabled) displayValue else "",
                onValueChange = onDisplayChange,
                enabled = enabled,
                label = { Text("Display / SVG") },
                placeholder = { Text("Label or SVG") },
                trailingIcon = if (enabled) {
                    {
                        TextButton(
                            onClick = onPickSvg,
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Open SVG", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ColorPickerCard(
    currentColorLong: Long,
    onColorChanged: (Long) -> Unit
) {
    val currentColor = Color(currentColorLong)
    val onColor = if (currentColor.luminance() > 0.5f) Color.Black else Color.White
    val hexCode = remember(currentColorLong) {
        String.format("#%06X", (currentColorLong and 0xFFFFFFL))
    }
    var hexInput by remember(currentColorLong) { mutableStateOf(hexCode) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Highlight Color",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Enter any 6-digit hex code to customize the active modifiers and popup highlight.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sample Key $hexCode",
                    color = onColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            OutlinedTextField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = input
                    val clean = input.removePrefix("#").trim()
                    if (clean.length == 6) {
                        clean.toLongOrNull(16)?.let { parsedRgb ->
                            val fullColor = 0xFF000000L or parsedRgb
                            onColorChanged(fullColor)
                        }
                    }
                },
                label = { Text("Hex Color Code") },
                placeholder = { Text("#RRGGBB") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}