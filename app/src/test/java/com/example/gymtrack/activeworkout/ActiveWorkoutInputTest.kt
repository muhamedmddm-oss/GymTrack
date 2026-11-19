package com.example.gymtrack.activeworkout

import com.example.gymtrack.activeworkout.presentation.formatWeight
import com.example.gymtrack.activeworkout.presentation.parseReps
import com.example.gymtrack.activeworkout.presentation.parseWeight
import com.example.gymtrack.activeworkout.presentation.sanitizeRepsInput
import com.example.gymtrack.activeworkout.presentation.sanitizeWeightInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveWorkoutInputTest {

    @Test
    fun reps_keepOnlyDigits_noLeadingZero_maxThree() {
        assertEquals("12", sanitizeRepsInput("1a2"))
        assertEquals("7", sanitizeRepsInput("007"))
        assertEquals("", sanitizeRepsInput("0"))
        assertEquals("123", sanitizeRepsInput("12345"))
        assertEquals("15", sanitizeRepsInput("١٥")) // Arabic-Indic digits are converted
    }

    @Test
    fun weight_acceptsOneDecimalPoint_andComma() {
        assertEquals("42.5", sanitizeWeightInput("42.5"))
        assertEquals("42.5", sanitizeWeightInput("42,5"))
        assertEquals("1.25", sanitizeWeightInput("1.2.5"))
        assertEquals("10.55", sanitizeWeightInput("10.555"))
        assertEquals("1234", sanitizeWeightInput("12345"))
        assertEquals("80", sanitizeWeightInput("-8x0"))
    }

    @Test
    fun parsing_blankOrInvalid_isSafe() {
        assertEquals(0, parseReps(""))
        assertEquals(8, parseReps("8"))
        assertNull(parseWeight(""))
        assertEquals(42.5, parseWeight("42.5")!!, 0.0)
        assertEquals(5.0, parseWeight("5.")!!, 0.0)
    }

    @Test
    fun formatWeight_hidesUselessDecimals() {
        assertEquals("", formatWeight(null))
        assertEquals("40", formatWeight(40.0))
        assertEquals("42.5", formatWeight(42.5))
    }
}
