package com.example.gymtrack.workout.presentation.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colors copied from the existing HomeScreen so the workout screens look identical.
 * Kept inside this module so no shared theme file had to be changed.
 */
object WorkoutColors {
    val Background = Color(0xFF101010)
    val Card = Color(0xFF202020)
    val CardRaised = Color(0xFF2B2B2B)
    val Orange = Color(0xFFFF8A00)
    val OnOrange = Color(0xFF101010)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFAAAAAA)
    val Outline = Color(0xFF444444)
    val Error = Color(0xFFFF6B6B)
}

@Composable
fun workoutTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WorkoutColors.TextPrimary,
    unfocusedTextColor = WorkoutColors.TextPrimary,
    errorTextColor = WorkoutColors.TextPrimary,
    focusedContainerColor = WorkoutColors.Card,
    unfocusedContainerColor = WorkoutColors.Card,
    errorContainerColor = WorkoutColors.Card,
    cursorColor = WorkoutColors.Orange,
    errorCursorColor = WorkoutColors.Error,
    focusedBorderColor = WorkoutColors.Orange,
    unfocusedBorderColor = WorkoutColors.Outline,
    errorBorderColor = WorkoutColors.Error,
    focusedLabelColor = WorkoutColors.Orange,
    unfocusedLabelColor = WorkoutColors.TextSecondary,
    errorLabelColor = WorkoutColors.Error,
    errorSupportingTextColor = WorkoutColors.Error
)
