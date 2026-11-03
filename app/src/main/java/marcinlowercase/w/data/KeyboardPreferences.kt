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
    }

    val bottomPaddingDp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOTTOM_PADDING] ?: 32
    }

    suspend fun setBottomPaddingDp(paddingDp: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BOTTOM_PADDING] = paddingDp
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