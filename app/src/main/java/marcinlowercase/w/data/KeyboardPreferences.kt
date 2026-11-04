package marcinlowercase.w.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "keyboard_preferences")

class KeyboardPreferences(private val context: Context) {

    companion object {
        val KEY_BOTTOM_PADDING = intPreferencesKey("bottom_chin_padding")
        val KEY_LAYOUT_CONFIG_JSON = stringPreferencesKey("layout_config_json")
        val KEY_HIGHLIGHT_COLOR = androidx.datastore.preferences.core.longPreferencesKey("highlight_color_long")
        val KEY_CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val KEY_CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
    }

    val bottomPaddingDp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOTTOM_PADDING] ?: 32
    }

    suspend fun setBottomPaddingDp(paddingDp: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BOTTOM_PADDING] = paddingDp
        }
    }

    val highlightColorLong: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_HIGHLIGHT_COLOR] ?: 0xFF001FFFL
    }

    suspend fun setHighlightColorLong(colorValue: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HIGHLIGHT_COLOR] = colorValue
        }
    }

    val customFontPath: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_FONT_PATH]
    }

    val customFontName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_FONT_NAME]
    }

    suspend fun setCustomFont(path: String?, name: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) {
                preferences.remove(KEY_CUSTOM_FONT_PATH)
                preferences.remove(KEY_CUSTOM_FONT_NAME)
            } else {
                preferences[KEY_CUSTOM_FONT_PATH] = path
                preferences[KEY_CUSTOM_FONT_NAME] = name ?: "Custom Font"
            }
        }
    }

    val layoutConfigJson: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAYOUT_CONFIG_JSON]
    }

    suspend fun setLayoutConfigJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAYOUT_CONFIG_JSON] = json
        }
    }
}