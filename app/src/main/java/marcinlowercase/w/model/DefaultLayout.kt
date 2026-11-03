package marcinlowercase.w.model

object DefaultLayout {
    fun create(): KeyboardLayoutConfig {
        val numberRow = KeyboardRow(
            listOf(
                KeyDefinition("1", "1", hint = "`", popups = listOf("`")),
                KeyDefinition("2", "2", hint = "~", popups = listOf("~")),
                KeyDefinition("3", "3"),
                KeyDefinition("4", "4"),
                KeyDefinition("5", "5"),
                KeyDefinition("6", "6"),
                KeyDefinition("7", "7"),
                KeyDefinition("8", "8"),
                KeyDefinition("9", "9"),
                KeyDefinition("0", "0")
            )
        )

        val rowOne = KeyboardRow(
            listOf(
                KeyDefinition("q", "q", hint = "{", popups = listOf("{", "\\")),
                KeyDefinition("w", "w", hint = "}", popups = listOf("}", "`")),
                KeyDefinition("e", "e", hint = "[", popups = listOf("[")),
                KeyDefinition("r", "r", hint = "]", popups = listOf("]")),
                KeyDefinition("t", "t", hint = "<", popups = listOf("<")),
                KeyDefinition("y", "y", hint = ">", popups = listOf(">")),
                KeyDefinition("u", "u", hint = "=", popups = listOf("=")),
                KeyDefinition(
                    id = "i",
                    primaryChar = "i",
                    hint = "+",
                    secondaries = SecondaryBindings(
                        center = "+",
                        top = "*",
                        left = "&",
                        right = "%"
                    )
                ),
                KeyDefinition("o", "o", hint = "-", popups = listOf("-", "_")),
                KeyDefinition(
                    id = "p",
                    primaryChar = "p",
                    hint = "|",
                    secondaries = SecondaryBindings(
                        center = "|",
                        top = "/",
                        bottom = "\\",
                        left = "="
                    )
                )
            )
        )

        val rowTwo = KeyboardRow(
            listOf(
                KeyDefinition(
                    id = "a",
                    primaryChar = "a",
                    hint = ";",
                    secondaries = SecondaryBindings(
                        center = ";",
                        top = ":",
                        right = "@",
                        bottom = "#"
                    ),
                    weight = 1.5f
                ),
                KeyDefinition("s", "s", hint = ":", popups = listOf(":")),
                KeyDefinition("d", "d", hint = "\"", popups = listOf("\"")),
                KeyDefinition("f", "f", hint = "'", popups = listOf("'")),
                KeyDefinition("g", "g", hint = "`", popups = listOf("`")),
                KeyDefinition("h", "h", hint = "/", popups = listOf("/")),
                KeyDefinition("j", "j", hint = "\\", popups = listOf("\\")),
                KeyDefinition("k", "k", hint = "?", popups = listOf("?")),
                KeyDefinition("l", "l", hint = "!", popups = listOf("!"), weight = 1.5f)
            )
        )

        val rowThree = KeyboardRow(
            listOf(
                KeyDefinition("shift", "⇧", type = KeyType.SHIFT, weight = 1.5f),
                KeyDefinition("z", "z", hint = ",", popups = listOf(",", "<")),
                KeyDefinition("x", "x", hint = ".", popups = listOf(".", ">")),
                KeyDefinition(
                    id = "c",
                    primaryChar = "c",
                    hint = "(",
                    secondaries = SecondaryBindings(
                        center = "(",
                        top = "{",
                        right = "[",
                        bottom = "]",
                        left = ")"
                    )
                ),
                KeyDefinition("v", "v", hint = ")", popups = listOf(")")),
                KeyDefinition("b", "b", hint = "[", popups = listOf("[")),
                KeyDefinition("n", "n", hint = "]", popups = listOf("]")),
                KeyDefinition("m", "m", hint = "_", popups = listOf("_")),
                KeyDefinition("backspace", "⌫", type = KeyType.BACKSPACE, weight = 1.5f)
            )
        )

        val rowFour = KeyboardRow(
            listOf(
                KeyDefinition("ctrl", "CTRL", type = KeyType.MODIFIER, weight = 1.5f),
                KeyDefinition("alt", "ALT", type = KeyType.MODIFIER, weight = 1.5f),
                KeyDefinition("space", " ", type = KeyType.SPACE, weight = 5.5f),
                KeyDefinition("enter", "↵", type = KeyType.ENTER, weight = 1.5f)
            )
        )

        return KeyboardLayoutConfig(
            ribbon = emptyList(),
            rows = listOf(numberRow, rowOne, rowTwo, rowThree, rowFour)
        )
    }
}