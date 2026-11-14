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

        if (lc == 'w') {
            val uIdx = word.indexOfAny(charArrayOf('u', 'U', 'ư', 'Ư'))
            val oIdx = word.indexOfAny(charArrayOf('o', 'O', 'ơ', 'Ơ'))
            if (uIdx >= 0 && oIdx == uIdx + 1) {
                val uMeta = parseVowel(word[uIdx])
                val oMeta = parseVowel(word[oIdx])
                if (uMeta != null && oMeta != null) {
                    val hasTrailingChars = oIdx < word.lastIndex
                    if (hasTrailingChars) {
                        val isBothHorn = uMeta.hat == 2 && oMeta.hat == 2
                        val targetHat = if (isBothHorn) 0 else 2
                        val newU = composeVowel('u', targetHat, uMeta.tone, uMeta.isUpper)
                        val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                        val replaced = word.substring(0, uIdx) + newU + newO + word.substring(oIdx + 1)
                        return if (isBothHorn) replaced + inputChar else replaced
                    } else {
                        val isHorn = oMeta.hat == 2
                        val targetHat = if (isHorn) 0 else 2
                        val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                        val replaced = word.substring(0, oIdx) + newO
                        return if (isHorn) replaced + inputChar else replaced
                    }
                }
            }

            val aIdx = word.indexOfAny(charArrayOf('a', 'A'))
            if (uIdx >= 0 && aIdx == uIdx + 1) {
                val uMeta = parseVowel(word[uIdx])
                if (uMeta != null) {
                    val isHorn = uMeta.hat == 2
                    val targetHat = if (isHorn) 0 else 2
                    val newU = composeVowel('u', targetHat, uMeta.tone, uMeta.isUpper)
                    val replaced = word.substring(0, uIdx) + newU + word.substring(uIdx + 1)
                    return if (isHorn) replaced + inputChar else replaced
                }
            }

            val targetIdx = word.lastIndexOfAny(charArrayOf('u', 'U', 'ư', 'Ư', 'o', 'O', 'ơ', 'Ơ', 'a', 'A', 'ă', 'Ă'))
            if (targetIdx >= 0) {
                val meta = parseVowel(word[targetIdx])
                if (meta != null) {
                    when (meta.base) {
                        'u' -> {
                            val isHorn = meta.hat == 2
                            val targetHat = if (isHorn) 0 else 2
                            val newChar = composeVowel('u', targetHat, meta.tone, meta.isUpper)
                            val replaced = word.substring(0, targetIdx) + newChar + word.substring(targetIdx + 1)
                            return if (isHorn) replaced + inputChar else replaced
                        }
                        'o' -> {
                            val isHorn = meta.hat == 2
                            val targetHat = if (isHorn) 0 else 2
                            val newChar = composeVowel('o', targetHat, meta.tone, meta.isUpper)
                            val replaced = word.substring(0, targetIdx) + newChar + word.substring(targetIdx + 1)
                            return if (isHorn) replaced + inputChar else replaced
                        }
                        'a' -> {
                            val isBreve = meta.hat == 3
                            val targetHat = if (isBreve) 0 else 3
                            val newChar = composeVowel('a', targetHat, meta.tone, meta.isUpper)
                            val replaced = word.substring(0, targetIdx) + newChar + word.substring(targetIdx + 1)
                            return if (isBreve) replaced + inputChar else replaced
                        }
                    }
                }
            }
        }

        if (lc == 'a' || lc == 'e' || lc == 'o') {
            val targetChars = when (lc) {
                'a' -> charArrayOf('a', 'A', 'â', 'Â')
                'e' -> charArrayOf('e', 'E', 'ê', 'Ê')
                'o' -> charArrayOf('o', 'O', 'ô', 'Ô')
                else -> charArrayOf()
            }
            val targetIdx = word.lastIndexOfAny(targetChars)
            if (targetIdx >= 0) {
                val meta = parseVowel(word[targetIdx])
                if (meta != null && meta.base == lc) {
                    val isCircumflex = meta.hat == 1
                    val targetHat = if (isCircumflex) 0 else 1
                    val newChar = composeVowel(meta.base, targetHat, meta.tone, meta.isUpper)
                    val replaced = word.substring(0, targetIdx) + newChar + word.substring(targetIdx + 1)
                    return if (isCircumflex) replaced + inputChar else replaced
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

        when (inputChar) {
            '6' -> {
                val idx = word.lastIndexOfAny(charArrayOf('a', 'A', 'e', 'E', 'o', 'O', 'â', 'Â', 'ê', 'Ê', 'ô', 'Ô'))
                if (idx >= 0) {
                    val meta = parseVowel(word[idx]) ?: return null
                    val newHat = if (meta.hat == 1) 0 else 1
                    val newChar = composeVowel(meta.base, newHat, meta.tone, meta.isUpper)
                    return if (newHat == 0) word.substring(0, idx) + newChar + word.substring(idx + 1) + "6"
                    else word.substring(0, idx) + newChar + word.substring(idx + 1)
                }
            }
            '7' -> {
                val uIdx = word.indexOfAny(charArrayOf('u', 'U', 'ư', 'Ư'))
                val oIdx = word.indexOfAny(charArrayOf('o', 'O', 'ơ', 'Ơ'))
                if (uIdx >= 0 && oIdx == uIdx + 1) {
                    val uMeta = parseVowel(word[uIdx])
                    val oMeta = parseVowel(word[oIdx])
                    if (uMeta != null && oMeta != null) {
                        val hasTrailingChars = oIdx < word.lastIndex
                        if (hasTrailingChars) {
                            val isBothHorn = uMeta.hat == 2 && oMeta.hat == 2
                            val targetHat = if (isBothHorn) 0 else 2
                            val newU = composeVowel('u', targetHat, uMeta.tone, uMeta.isUpper)
                            val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                            val replaced = word.substring(0, uIdx) + newU + newO + word.substring(oIdx + 1)
                            return if (isBothHorn) replaced + "7" else replaced
                        } else {
                            val isHorn = oMeta.hat == 2
                            val targetHat = if (isHorn) 0 else 2
                            val newO = composeVowel('o', targetHat, oMeta.tone, oMeta.isUpper)
                            val replaced = word.substring(0, oIdx) + newO
                            return if (isHorn) replaced + "7" else replaced
                        }
                    }
                }
                val idx = word.lastIndexOfAny(charArrayOf('o', 'O', 'u', 'U', 'ơ', 'Ơ', 'ư', 'Ư'))
                if (idx >= 0) {
                    val meta = parseVowel(word[idx]) ?: return null
                    val newHat = if (meta.hat == 2) 0 else 2
                    val newChar = composeVowel(meta.base, newHat, meta.tone, meta.isUpper)
                    return if (newHat == 0) word.substring(0, idx) + newChar + word.substring(idx + 1) + "7"
                    else word.substring(0, idx) + newChar + word.substring(idx + 1)
                }
            }
            '8' -> {
                val idx = word.lastIndexOfAny(charArrayOf('a', 'A', 'ă', 'Ă'))
                if (idx >= 0) {
                    val meta = parseVowel(word[idx]) ?: return null
                    val newHat = if (meta.hat == 3) 0 else 3
                    val newChar = composeVowel('a', newHat, meta.tone, meta.isUpper)
                    return if (newHat == 0) word.substring(0, idx) + newChar + word.substring(idx + 1) + "8"
                    else word.substring(0, idx) + newChar + word.substring(idx + 1)
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

    private fun resolveToneVowelIndex(word: String, vowelIndices: List<Int>): Int {
        if (vowelIndices.size == 1) return vowelIndices[0]

        if (vowelIndices.size == 2) {
            val hasConsonantAfter = vowelIndices.last() < word.lastIndex
            if (hasConsonantAfter) return vowelIndices[1]

            val firstMeta = parseVowel(word[vowelIndices[0]])
            val secondMeta = parseVowel(word[vowelIndices[1]])

            if (firstMeta != null && (firstMeta.hat == 1 || firstMeta.hat == 2)) return vowelIndices[0]
            if (secondMeta != null && (secondMeta.hat == 1 || secondMeta.hat == 2)) return vowelIndices[1]

            val pair = "${firstMeta?.base}${secondMeta?.base}"
            if (pair == "oa" || pair == "oe" || pair == "uy") return vowelIndices[1]
            return vowelIndices[0]
        }

        return vowelIndices[1]
    }
}