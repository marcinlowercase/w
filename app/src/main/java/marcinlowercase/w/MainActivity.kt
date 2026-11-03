package marcinlowercase.w

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import marcinlowercase.w.data.KeyboardPreferences
import marcinlowercase.w.ui.theme.WTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        SettingsTopBar()
                    }
                ) { innerPadding ->
                    SettingsScreen(modifier = Modifier.padding(innerPadding))
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
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { KeyboardPreferences(context) }
    val bottomPaddingState = preferences.bottomPaddingDp.collectAsState(initial = 24)
    var testInputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StatusCard(context)

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
                    onClick = {
                    },
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