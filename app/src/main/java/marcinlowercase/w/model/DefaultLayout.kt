package marcinlowercase.w.model

import android.content.Context
import kotlinx.serialization.json.Json

object DefaultLayout {
    private var cachedConfig: KeyboardLayoutConfig? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun create(context: Context? = null): KeyboardLayoutConfig {
        if (context != null) {
            runCatching {
                val jsonString = context.assets.open("default_layout.json").bufferedReader().use { it.readText() }
                val parsed = json.decodeFromString<KeyboardLayoutConfig>(jsonString)
                cachedConfig = enforceSystemKeys(parsed)
            }
        }
        return cachedConfig ?: KeyboardLayoutConfig(ribbon = emptyList(), rows = emptyList())
    }

    private fun enforceSystemKeys(config: KeyboardLayoutConfig): KeyboardLayoutConfig {
        val updatedRows = config.rows.map { row ->
            KeyboardRow(
                keys = row.keys.map { key ->
                    when (key.id) {
                        "shift" -> key.copy(primaryChar = "⇧", type = KeyType.SHIFT, weight = if (key.weight <= 1.0f) 1.5f else key.weight)
                        "backspace" -> key.copy(primaryChar = "⌫", type = KeyType.BACKSPACE, weight = if (key.weight <= 1.0f) 1.5f else key.weight)
                        "ctrl" -> key.copy(primaryChar = "CTRL", type = KeyType.MODIFIER, weight = if (key.weight <= 1.0f) 1.5f else key.weight)
                        "alt" -> key.copy(primaryChar = "ALT", type = KeyType.MODIFIER, weight = if (key.weight <= 1.0f) 1.5f else key.weight)
                        "space" -> key.copy(primaryChar = " ", type = KeyType.SPACE, weight = if (key.weight <= 1.0f) 5.5f else key.weight)
                        "enter" -> key.copy(primaryChar = "↵", type = KeyType.ENTER, weight = if (key.weight <= 1.0f) 1.5f else key.weight)
                        else -> key
                    }
                }
            )
        }
        return config.copy(rows = updatedRows)
    }
}