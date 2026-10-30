package marcinlowercase.w.model

import kotlinx.serialization.Serializable

enum class ShiftState {
    OFF,
    SHIFTED,
    CAPS_LOCK
}

enum class KeyType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    MODIFIER,
    ARROW,
    SPACER
}

@Serializable
data class KeyDefinition(
    val id: String,
    val primaryChar: String,
    val hint: String? = null,
    val popups: List<String> = emptyList(),
    val type: KeyType = KeyType.CHARACTER,
    val weight: Float = 1.0f
)

@Serializable
data class KeyboardRow(
    val keys: List<KeyDefinition>
)

@Serializable
data class KeyboardLayoutConfig(
    val ribbon: List<KeyDefinition>,
    val rows: List<KeyboardRow>
)