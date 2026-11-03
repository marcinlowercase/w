package marcinlowercase.w.model

object DefaultLayout {
    fun create(): KeyboardLayoutConfig {
        val numberRow = KeyboardRow(
            listOf(
                KeyDefinition("1", "1", hint = "`", secondaries = SecondaryBindings(center = "`")),
                KeyDefinition("2", "2", hint = "~", secondaries = SecondaryBindings(center = "~")),
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
                KeyDefinition("q", "q", hint = "{", secondaries = SecondaryBindings(center = "{", right = "\\")),
                KeyDefinition("w", "w", hint = "}", secondaries = SecondaryBindings(center = "}", right = "`")),
                KeyDefinition("e", "e", hint = "[", secondaries = SecondaryBindings(center = "[")),
                KeyDefinition("r", "r", hint = "]", secondaries = SecondaryBindings(center = "]")),
                KeyDefinition("t", "t", hint = "<", secondaries = SecondaryBindings(center = "<")),
                KeyDefinition("y", "y", hint = ">", secondaries = SecondaryBindings(center = ">")),
                KeyDefinition("u", "u", hint = "=", secondaries = SecondaryBindings(center = "=")),
                KeyDefinition("i", "i", hint = "+", secondaries = SecondaryBindings(center = "+")),
                KeyDefinition("o", "o", hint = "-", secondaries = SecondaryBindings(center = "-", right = "_")),
                KeyDefinition("p", "p", hint = "|", secondaries = SecondaryBindings(center = "|", top = "/", bottom = "\\", left = "="))
            )
        )

        val rowTwo = KeyboardRow(
            listOf(
                KeyDefinition("a", "a", hint = ";", secondaries = SecondaryBindings(center = ";", top = ":", right = "@", bottom = "#"), weight = 1.5f),
                KeyDefinition("s", "s", hint = ":", secondaries = SecondaryBindings(center = ":")),
                KeyDefinition("d", "d", hint = "\"", secondaries = SecondaryBindings(center = "\"")),
                KeyDefinition("f", "f", hint = "'", secondaries = SecondaryBindings(center = "'")),
                KeyDefinition("g", "g", hint = "`", secondaries = SecondaryBindings(center = "`")),
                KeyDefinition("h", "h", hint = "/", secondaries = SecondaryBindings(center = "/")),
                KeyDefinition("j", "j", hint = "\\", secondaries = SecondaryBindings(center = "\\")),
                KeyDefinition("k", "k", hint = "?", secondaries = SecondaryBindings(center = "?")),
                KeyDefinition("l", "l", hint = "!", secondaries = SecondaryBindings(center = "!"), weight = 1.5f)
            )
        )

        val rowThree = KeyboardRow(
            listOf(
                KeyDefinition("shift", "⇧", type = KeyType.SHIFT, weight = 1.5f),
                KeyDefinition("z", "z", hint = ",", secondaries = SecondaryBindings(center = ",", right = "<")),
                KeyDefinition("x", "x", hint = ".", secondaries = SecondaryBindings(center = ".", right = ">")),
                KeyDefinition("c", "c", hint = "(", secondaries = SecondaryBindings(center = "(", top = "{", right = "[", bottom = "]", left = ")")),
                KeyDefinition("v", "v", hint = ")", secondaries = SecondaryBindings(center = ")")),
                KeyDefinition("b", "b", hint = "[", secondaries = SecondaryBindings(center = "[")),
                KeyDefinition("n", "n", hint = "]", secondaries = SecondaryBindings(center = "]")),
                KeyDefinition("m", "m", hint = "_", secondaries = SecondaryBindings(center = "_")),
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