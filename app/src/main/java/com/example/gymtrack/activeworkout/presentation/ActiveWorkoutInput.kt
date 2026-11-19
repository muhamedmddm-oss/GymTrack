package com.example.gymtrack.activeworkout.presentation

/**
 * Pure helpers that clean what the user types into the reps / weight fields.
 * The field can therefore never contain invalid text, and the same functions are used by the UI and
 * the ViewModel (and by the unit tests).
 */

const val MAX_REPS_DIGITS = 3
const val MAX_WEIGHT_INT_DIGITS = 4
const val MAX_WEIGHT_DECIMALS = 2

/** Converts Arabic-Indic / any Unicode digit to a plain 0-9 digit, leaves other characters alone. */
private fun normalizeDigits(raw: String): String = raw.map { ch ->
    val digit = Character.digit(ch, 10)
    if (digit >= 0) '0' + digit else ch
}.joinToString("")

/** Digits only, max [MAX_REPS_DIGITS] long, no leading zeros ("007" -> "7", "0" -> ""). */
fun sanitizeRepsInput(raw: String): String =
    normalizeDigits(raw)
        .filter { it in '0'..'9' }
        .take(MAX_REPS_DIGITS)
        .trimStart('0')

/** Digits plus at most one decimal point ("," is accepted as the point), e.g. "42.5". */
fun sanitizeWeightInput(raw: String): String {
    val cleaned = normalizeDigits(raw)
        .replace(',', '.')
        .replace('\u066B', '.') // Arabic decimal separator
        .filter { it in '0'..'9' || it == '.' }
    val dotIndex = cleaned.indexOf('.')
    if (dotIndex == -1) return cleaned.take(MAX_WEIGHT_INT_DIGITS)
    val integerPart = cleaned.substring(0, dotIndex).take(MAX_WEIGHT_INT_DIGITS)
    val decimalPart = cleaned.substring(dotIndex + 1).replace(".", "").take(MAX_WEIGHT_DECIMALS)
    return "$integerPart.$decimalPart"
}

/** Blank / invalid reps are stored as 0 (= "not entered yet"). */
fun parseReps(text: String): Int = text.toIntOrNull()?.coerceAtLeast(0) ?: 0

/** Blank weight means bodyweight / not entered -> null. */
fun parseWeight(text: String): Double? = text.toDoubleOrNull()?.takeIf { it >= 0.0 }

/** 40.0 -> "40", 42.5 -> "42.5", null -> "". */
fun formatWeight(weightKg: Double?): String = when {
    weightKg == null -> ""
    weightKg % 1.0 == 0.0 -> weightKg.toLong().toString()
    else -> weightKg.toString()
}
