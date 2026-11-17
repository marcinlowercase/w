package marcinlowercase.w.model

enum class VietnameseMode {
    OFF,
    TELEX,
    VNI
}

object VietnameseEngine {

    private data class VowelRow(val base: Char, val hat: Int, val tones: String)

    private val vowelRows = listOf(
        VowelRow('a', 0, "aáàảãạ"),
        VowelRow('a', 3, "ăắằẳẵặ"),
        VowelRow('a', 1, "âấầẩẫậ"),
        VowelRow('e', 0, "eéèẻẽẹ"),
        VowelRow('e', 1, "êếềểễệ"),
        VowelRow('i', 0, "iíìỉĩị"),
        VowelRow('o', 0, "oóòỏõọ"),
        VowelRow('o', 1, "ôốồổỗộ"),
        VowelRow('o', 2, "ơớờởỡợ"),
        VowelRow('u', 0, "uúùủũụ"),
        VowelRow('u', 2, "ưứừửữự"),
        VowelRow('y', 0, "yýỳỷỹỵ")
    )

    private data class VowelMeta(val base: Char, val hat: Int, val tone: Int, val isUpper: Boolean)

    private fun parseVowel(c: Char): VowelMeta? {
        val lc = c.lowercaseChar()
        for (row in vowelRows) {
            val idx = row.tones.indexOf(lc)
            if (idx >= 0) {
                return VowelMeta(row.base, row.hat, idx, c.isUpperCase())
            }
        }
        return null
    }

    private fun composeVowel(base: Char, hat: Int, tone: Int, isUpper: Boolean): Char {
        val row = vowelRows.find { it.base == base && it.hat == hat } ?: vowelRows.first { it.base == base }
        val char = row.tones[tone.coerceIn(0, row.tones.lastIndex)]
        return if (isUpper) char.uppercaseChar() else char
    }

    private fun isVowel(c: Char): Boolean = parseVowel(c) != null

    fun transform(word: String, inputChar: Char, mode: VietnameseMode): String? {
        if (mode == VietnameseMode.OFF || word.isEmpty()) return null
        return when (mode) {
            VietnameseMode.TELEX -> transformTelex(word, inputChar)
            VietnameseMode.VNI -> transformVni(word, inputChar)
            VietnameseMode.OFF -> null
        }
    }

    private fun transformTelex(word: String, inputChar: Char): String? {
        val lc = inputChar.lowercaseChar()

        if (lc == 'd') {
            if (word.endsWith("đ")) return word.dropLast(1) + "dd"
            if (word.endsWith("Đ")) return word.dropLast(1) + if (inputChar.isUpperCase()) "DD" else "Dd"
            if (word.endsWith("d")) return word.dropLast(1) + (if (word.last().isUpperCase()) "Đ" else "đ")
            if (word.endsWith("D")) return word.dropLast(1) + "Đ"

            if (word.startsWith("đ")) return "d" + word.substring(1) + inputChar
            if (word.startsWith("Đ")) return "D" + word.substring(1) + inputChar
            if (word.startsWith("d")) return "đ" + word.substring(1)
            if (word.startsWith("D")) return "Đ" + word.substring(1)
        }

        val toneTarget = when (lc) {
            's' -> 1
            'f' -> 2
            'r' -> 3
            'x' -> 4
            'j' -> 5
            'z' -> 0
            else -> -1
        }
        if (toneTarget >= 0) {
            val toneApplied = applyTone(word, toneTarget, inputChar)
            if (toneApplied != null) return toneApplied
        }

        val vowelIndices = getVowelIndices(word)

        if (lc == 'w') {
            val uoIdx = vowelIndices.firstOrNull { idx ->
                parseVowel(word[idx])?.base == 'u' &&
                    (idx + 1) in vowelIndices &&
                    parseVowel(word[idx + 1])?.base == 'o'
            }
            if (uoIdx != null) {
                val oIdx = uoIdx + 1
                val uMeta = parseVowel(word[uoIdx])
                val oMeta = parseVowel(word[oIdx])
                if (uMeta != null && oMeta != null) {
                    val hasTrailingChars = oIdx < word.lastIndex
                    if (hasTrailingChars) {
                        val isBothHorn = uMeta.hat == 2 && oMeta.hat == 2
                        val targetHat = if (isBothHorn) 0 else 2
                        val newU = composeVowel('u', targetHat, uMeta.tone, uMeta.isUpper)
                        val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                        val replaced = word.substring(0, uoIdx) + newU + newO + word.substring(oIdx + 1)
                        val normalized = normalizeTonePosition(replaced)
                        return if (isBothHorn) normalized + inputChar else normalized
                    } else {
                        val isHorn = oMeta.hat == 2
                        val targetHat = if (isHorn) 0 else 2
                        val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                        val replaced = word.substring(0, oIdx) + newO
                        val normalized = normalizeTonePosition(replaced)
                        return if (isHorn) normalized + inputChar else normalized
                    }
                }
            }

            val uaIdx = vowelIndices.firstOrNull { idx ->
                parseVowel(word[idx])?.base == 'u' &&
                    (idx + 1) in vowelIndices &&
                    parseVowel(word[idx + 1])?.base == 'a'
            }
            if (uaIdx != null) {
                val uMeta = parseVowel(word[uaIdx])
                if (uMeta != null) {
                    val isHorn = uMeta.hat == 2
                    val targetHat = if (isHorn) 0 else 2
                    val newU = composeVowel('u', targetHat, uMeta.tone, uMeta.isUpper)
                    val replaced = word.substring(0, uaIdx) + newU + word.substring(uaIdx + 1)
                    val normalized = normalizeTonePosition(replaced)
                    return if (isHorn) normalized + inputChar else normalized
                }
            }

            val targetIdx = vowelIndices.lastOrNull { idx ->
                val meta = parseVowel(word[idx])
                meta != null && (
                    ((meta.base == 'u' || meta.base == 'o') && (meta.hat == 0 || meta.hat == 2)) ||
                        (meta.base == 'a' && (meta.hat == 0 || meta.hat == 3))
                    )
            }
            if (targetIdx != null) {
                val meta = parseVowel(word[targetIdx])
                if (meta != null) {
                    val expectedHat = if (meta.base == 'a') 3 else 2
                    val isModified = meta.hat == expectedHat
                    val targetHat = if (isModified) 0 else expectedHat
                    val newChar = composeVowel(meta.base, targetHat, meta.tone, meta.isUpper)
                    val replaced = word.substring(0, targetIdx) + newChar + word.substring(targetIdx + 1)
                    val normalized = normalizeTonePosition(replaced)
                    return if (isModified) normalized + inputChar else normalized
                }
            }
        }

        if (lc == 'a' || lc == 'e' || lc == 'o') {
            val targetIdx = vowelIndices.lastOrNull { idx ->
                val meta = parseVowel(word[idx])
                meta != null && meta.base == lc && (meta.hat == 0 || meta.hat == 1)
            }
            if (targetIdx != null) {
                val meta = parseVowel(word[targetIdx])
                if (meta != null) {
                    val isCircumflex = meta.hat == 1
                    val targetHat = if (isCircumflex) 0 else 1
                    val newChar = composeVowel(meta.base, targetHat, meta.tone, meta.isUpper)
                    val replaced = word.substring(0, targetIdx) + newChar + word.substring(targetIdx + 1)
                    val normalized = normalizeTonePosition(replaced)
                    return if (isCircumflex) normalized + inputChar else normalized
                }
            }
        }

        return null
    }

    private fun transformVni(word: String, inputChar: Char): String? {
        if (!inputChar.isDigit()) return null

        if (inputChar == '9') {
            val lastD = word.lastIndexOfAny(charArrayOf('d', 'D', 'đ', 'Đ'))
            if (lastD >= 0) {
                val ch = word[lastD]
                val replacement = when (ch) {
                    'd' -> "đ"
                    'D' -> "Đ"
                    'đ' -> "d9"
                    'Đ' -> "D9"
                    else -> return null
                }
                return word.substring(0, lastD) + replacement + word.substring(lastD + 1)
            }
            return null
        }

        val toneTarget = when (inputChar) {
            '1' -> 1
            '2' -> 2
            '3' -> 3
            '4' -> 4
            '5' -> 5
            '0' -> 0
            else -> -1
        }
        if (toneTarget >= 0) {
            val toneApplied = applyTone(word, toneTarget, inputChar)
            if (toneApplied != null) return toneApplied
        }

        val vowelIndices = getVowelIndices(word)

        when (inputChar) {
            '6' -> {
                val idx = vowelIndices.lastOrNull { i ->
                    val meta = parseVowel(word[i])
                    meta != null && (meta.base == 'a' || meta.base == 'e' || meta.base == 'o') && (meta.hat == 0 || meta.hat == 1)
                }
                if (idx != null) {
                    val meta = parseVowel(word[idx]) ?: return null
                    val newHat = if (meta.hat == 1) 0 else 1
                    val newChar = composeVowel(meta.base, newHat, meta.tone, meta.isUpper)
                    val replaced = normalizeTonePosition(word.substring(0, idx) + newChar + word.substring(idx + 1))
                    return if (newHat == 0) replaced + "6" else replaced
                }
            }
            '7' -> {
                val uoIdx = vowelIndices.firstOrNull { idx ->
                    parseVowel(word[idx])?.base == 'u' &&
                        (idx + 1) in vowelIndices &&
                        parseVowel(word[idx + 1])?.base == 'o'
                }
                if (uoIdx != null) {
                    val oIdx = uoIdx + 1
                    val uMeta = parseVowel(word[uoIdx])
                    val oMeta = parseVowel(word[oIdx])
                    if (uMeta != null && oMeta != null) {
                        val hasTrailingChars = oIdx < word.lastIndex
                        if (hasTrailingChars) {
                            val isBothHorn = uMeta.hat == 2 && oMeta.hat == 2
                            val targetHat = if (isBothHorn) 0 else 2
                            val newU = composeVowel('u', targetHat, uMeta.tone, uMeta.isUpper)
                            val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                            val replaced = normalizeTonePosition(word.substring(0, uoIdx) + newU + newO + word.substring(oIdx + 1))
                            return if (isBothHorn) replaced + "7" else replaced
                        } else {
                            val isHorn = oMeta.hat == 2
                            val targetHat = if (isHorn) 0 else 2
                            val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                            val replaced = normalizeTonePosition(word.substring(0, oIdx) + newO)
                            return if (isHorn) replaced + "7" else replaced
                        }
                    }
                }

                val uaIdx = vowelIndices.firstOrNull { idx ->
                    parseVowel(word[idx])?.base == 'u' &&
                        (idx + 1) in vowelIndices &&
                        parseVowel(word[idx + 1])?.base == 'a'
                }
                if (uaIdx != null) {
                    val uMeta = parseVowel(word[uaIdx]) ?: return null
                    val newHat = if (uMeta.hat == 2) 0 else 2
                    val newU = composeVowel('u', newHat, uMeta.tone, uMeta.isUpper)
                    val replaced = normalizeTonePosition(word.substring(0, uaIdx) + newU + word.substring(uaIdx + 1))
                    return if (newHat == 0) replaced + "7" else replaced
                }

                val idx = vowelIndices.lastOrNull { i ->
                    val meta = parseVowel(word[i])
                    meta != null && (meta.base == 'o' || meta.base == 'u') && (meta.hat == 0 || meta.hat == 2)
                }
                if (idx != null) {
                    val meta = parseVowel(word[idx]) ?: return null
                    val newHat = if (meta.hat == 2) 0 else 2
                    val newChar = composeVowel(meta.base, newHat, meta.tone, meta.isUpper)
                    val replaced = normalizeTonePosition(word.substring(0, idx) + newChar + word.substring(idx + 1))
                    return if (newHat == 0) replaced + "7" else replaced
                }
            }
            '8' -> {
                val idx = vowelIndices.lastOrNull { i ->
                    val meta = parseVowel(word[i])
                    meta != null && meta.base == 'a' && (meta.hat == 0 || meta.hat == 3)
                }
                if (idx != null) {
                    val meta = parseVowel(word[idx]) ?: return null
                    val newHat = if (meta.hat == 3) 0 else 3
                    val newChar = composeVowel('a', newHat, meta.tone, meta.isUpper)
                    val replaced = normalizeTonePosition(word.substring(0, idx) + newChar + word.substring(idx + 1))
                    return if (newHat == 0) replaced + "8" else replaced
                }
            }
        }

        return null
    }

    private fun getVowelIndices(word: String): List<Int> {
        val allIndices = word.indices.filter { isVowel(word[it]) }
        if (allIndices.isEmpty()) return emptyList()

        if (word.startsWith("gi", ignoreCase = true) && allIndices.contains(1) && allIndices.size > 1) {
            return allIndices.filter { it != 1 }
        }

        if (word.startsWith("qu", ignoreCase = true) && allIndices.contains(1) && allIndices.size > 1) {
            return allIndices.filter { it != 1 }
        }

        return allIndices
    }

    private fun applyTone(word: String, targetTone: Int, triggerChar: Char): String? {
        val vowelIndices = getVowelIndices(word)
        if (vowelIndices.isEmpty()) return null

        val currentTone = vowelIndices.map { parseVowel(word[it])?.tone ?: 0 }.firstOrNull { it > 0 } ?: 0

        if (targetTone > 0 && currentTone == targetTone) {
            val unToned = word.mapIndexed { idx, ch ->
                if (idx in vowelIndices) {
                    val meta = parseVowel(ch)
                    if (meta != null && meta.tone > 0) composeVowel(meta.base, meta.hat, 0, meta.isUpper) else ch
                } else ch
            }.joinToString("")
            return unToned + triggerChar
        }

        if (targetTone == 0) {
            if (currentTone == 0) return null
            return word.mapIndexed { idx, ch ->
                if (idx in vowelIndices) {
                    val meta = parseVowel(ch)
                    if (meta != null && meta.tone > 0) composeVowel(meta.base, meta.hat, 0, meta.isUpper) else ch
                } else ch
            }.joinToString("")
        }

        val targetIndex = resolveToneVowelIndex(word, vowelIndices)

        val sb = StringBuilder()
        for (i in word.indices) {
            val ch = word[i]
            if (i == targetIndex) {
                val meta = parseVowel(ch)
                if (meta != null) {
                    sb.append(composeVowel(meta.base, meta.hat, targetTone, meta.isUpper))
                } else {
                    sb.append(ch)
                }
            } else if (i in vowelIndices) {
                val meta = parseVowel(ch)
                if (meta != null) {
                    sb.append(composeVowel(meta.base, meta.hat, 0, meta.isUpper))
                } else {
                    sb.append(ch)
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun normalizeTonePosition(word: String): String {
        val vowelIndices = getVowelIndices(word)
        if (vowelIndices.size <= 1) return word

        val existingTone = vowelIndices.mapNotNull { parseVowel(word[it])?.tone }.firstOrNull { it > 0 } ?: 0
        if (existingTone == 0) return word

        val targetIndex = resolveToneVowelIndex(word, vowelIndices)
        val sb = StringBuilder()
        for (i in word.indices) {
            val ch = word[i]
            if (i == targetIndex) {
                val meta = parseVowel(ch)
                if (meta != null) {
                    sb.append(composeVowel(meta.base, meta.hat, existingTone, meta.isUpper))
                } else {
                    sb.append(ch)
                }
            } else if (i in vowelIndices) {
                val meta = parseVowel(ch)
                if (meta != null) {
                    sb.append(composeVowel(meta.base, meta.hat, 0, meta.isUpper))
                } else {
                    sb.append(ch)
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun resolveToneVowelIndex(word: String, vowelIndices: List<Int>): Int {
        if (vowelIndices.size == 1) return vowelIndices[0]

        val lastHatIndex = vowelIndices.lastOrNull { idx ->
            val hat = parseVowel(word[idx])?.hat ?: 0
            hat != 0
        }
        if (lastHatIndex != null) return lastHatIndex

        if (vowelIndices.size == 2) {
            val hasConsonantAfter = vowelIndices.last() < word.lastIndex
            if (hasConsonantAfter) return vowelIndices[1]

            val firstMeta = parseVowel(word[vowelIndices[0]])
            val secondMeta = parseVowel(word[vowelIndices[1]])

            val pair = "${firstMeta?.base}${secondMeta?.base}"
            if (pair == "oa" || pair == "oe" || pair == "uy") return vowelIndices[1]
            return vowelIndices[0]
        }

        if (vowelIndices.size == 3) {
            val firstBase = parseVowel(word[vowelIndices[0]])?.base
            val secondBase = parseVowel(word[vowelIndices[1]])?.base
            val thirdBase = parseVowel(word[vowelIndices[2]])?.base
            if (firstBase == 'u' && secondBase == 'y' && thirdBase == 'e') {
                return vowelIndices[2]
            }
        }

        return vowelIndices[1]
    }
}