package marcinlowercase.w

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
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
                text = "w keyboard",
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
    var testInputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StatusCard(context)

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
                    Text(
                        text = "${bottomPaddingState.value} dp",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF2563EB)
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
                                        Text(
                                            text = key.primaryChar,
                                            color = textColor,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
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
                            label = { Text("Primary Key") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider()

                        Text(
                            text = "Directional Secondaries (Plus Pattern)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        val currentCenter = sec.center ?: selectedKey.hint ?: ""

                        OutlinedTextField(
                            value = currentCenter,
                            onValueChange = { newCenter ->
                                val cleaned = newCenter.ifEmpty { null }
                                updateSelectedKey {
                                    it.copy(
                                        hint = cleaned,
                                        secondaries = sec.copy(center = cleaned)
                                    )
                                }
                            },
                            label = { Text("Center (Default Hold / Corner Hint)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = sec.top ?: "",
                                onValueChange = { newTop ->
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(top = newTop.ifEmpty { null }))
                                    }
                                },
                                label = { Text("Swipe Up") },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = sec.bottom ?: "",
                                onValueChange = { newBottom ->
                                    updateSelectedKey {
                                        it.copy(secondaries = sec.copy(bottom = newBottom.ifEmpty { null }))
                                    }
                                },
                                label = { Text("Swipe Down") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = if (isFirstColumn) "" else (sec.left ?: ""),
                                onValueChange = { newLeft ->
                                    if (!isFirstColumn) {
                                        updateSelectedKey {
                                            it.copy(secondaries = sec.copy(left = newLeft.ifEmpty { null }))
                                        }
                                    }
                                },
                                enabled = !isFirstColumn,
                                label = { Text(if (isFirstColumn) "Left (Edge disabled)" else "Swipe Left") },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = if (isLastColumn) "" else (sec.right ?: ""),
                                onValueChange = { newRight ->
                                    if (!isLastColumn) {
                                        updateSelectedKey {
                                            it.copy(secondaries = sec.copy(right = newRight.ifEmpty { null }))
                                        }
                                    }
                                },
                                enabled = !isLastColumn,
                                label = { Text(if (isLastColumn) "Right (Edge disabled)" else "Swipe Right") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
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