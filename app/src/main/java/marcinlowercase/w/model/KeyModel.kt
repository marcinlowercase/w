package marcinlowercase.w.model

import kotlinx.serialization.Serializable

enum class ShiftState {
    OFF,
    SHIFTED,
    CAPS_LOCK
}

enum class PopupDirection {
    CENTER,
    TOP,
    RIGHT,
    BOTTOM,
    LEFT
}

@Serializable
data class SecondaryBindings(
    val center: String? = null,
    val top: String? = null,
    val right: String? = null,
    val bottom: String? = null,
    val left: String? = null
) {
    fun get(direction: PopupDirection): String? = when (direction) {
        PopupDirection.CENTER -> center
        PopupDirection.TOP -> top
        PopupDirection.RIGHT -> right
        PopupDirection.BOTTOM -> bottom
        PopupDirection.LEFT -> left
    }

    fun hasOptions(): Boolean = center != null || top != null || right != null || bottom != null || left != null
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
    val displayLabel: String? = null,
    val hint: String? = null,
    val popups: List<String> = emptyList(),
    val secondaries: SecondaryBindings? = null,
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