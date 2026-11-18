package com.example.gymtrack.workout.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrack.workout.WorkoutModule
import com.example.gymtrack.workout.domain.model.WorkoutDayDetails
import com.example.gymtrack.workout.presentation.components.OrangeButton
import com.example.gymtrack.workout.presentation.components.SectionLabel
import com.example.gymtrack.workout.presentation.components.WorkoutTopBar
import com.example.gymtrack.workout.presentation.components.exerciseLabel
import com.example.gymtrack.workout.presentation.theme.WorkoutColors
import com.example.gymtrack.workout.presentation.viewmodel.WorkoutDayDetailsUiState
import com.example.gymtrack.workout.presentation.viewmodel.WorkoutDayDetailsViewModel

/**
 * Stateful entry point for the "Workout Day Details" destination.
 *
 * INTEGRATION POINT FOR PERSON 3 (exercises):
 *  - [exerciseContent]: pass your own composable to render the exercise list of the given day id.
 *    When null, a simple placeholder with the exercise count is shown.
 *  - [onAddExercises]: called with the day id when the user taps "Add Exercises". When null the
 *    button is hidden (until your screen exists).
 */
@Composable
fun WorkoutDayDetailsRoute(
    dayId: Long,
    onBack: () -> Unit,
    onEditPlan: (planId: Long) -> Unit,
    onAddExercises: ((dayId: Long) -> Unit)? = null,
    exerciseContent: (@Composable (dayId: Long) -> Unit)? = null
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: WorkoutDayDetailsViewModel = viewModel(
        factory = WorkoutModule.factory {
            WorkoutDayDetailsViewModel(WorkoutModule.repository(appContext), dayId)
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutDayDetailsScreen(
        state = state,
        onBack = onBack,
        onEditPlan = onEditPlan,
        exerciseSection = { details ->
            if (exerciseContent != null) {
                exerciseContent(details.day.id)
            } else {
                ExercisePlaceholder(
                    exerciseCount = details.day.exerciseCount,
                    onAddExercises = onAddExercises?.let { callback -> { callback(details.day.id) } }
                )
            }
        }
    )
}

@Composable
fun WorkoutDayDetailsScreen(
    state: WorkoutDayDetailsUiState,
    onBack: () -> Unit,
    onEditPlan: (planId: Long) -> Unit,
    exerciseSection: @Composable (WorkoutDayDetails) -> Unit
) {
    Scaffold(
        containerColor = WorkoutColors.Background,
        topBar = {
            WorkoutTopBar(
                title = "Workout Day",
                onBack = onBack,
                actions = {
                    state.details?.let { details ->
                        IconButton(onClick = { onEditPlan(details.day.planId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit plan",
                                tint = WorkoutColors.TextPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WorkoutColors.Background)
                .padding(innerPadding)
        ) {
            val details = state.details
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = WorkoutColors.Orange
                )

                details != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DayHeaderCard(details)
                    Column {
                        SectionLabel("Exercises")
                        Spacer(modifier = Modifier.height(10.dp))
                        exerciseSection(details)
                    }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.errorMessage ?: "This workout day no longer exists.",
                        color = WorkoutColors.TextPrimary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OrangeButton(text = "Go back", onClick = onBack)
                }
            }
        }
    }
}

@Composable
private fun DayHeaderCard(details: WorkoutDayDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = details.day.name,
                color = WorkoutColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow(label = "Weekday", value = details.day.weekday.displayName)
            Spacer(modifier = Modifier.height(6.dp))
            DetailRow(label = "Plan", value = details.planName)
            Spacer(modifier = Modifier.height(6.dp))
            DetailRow(label = "Exercises", value = exerciseLabel(details.day.exerciseCount))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, color = WorkoutColors.TextSecondary, fontSize = 12.sp)
        Text(text = value, color = WorkoutColors.Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Shown until Person 3 provides the real exercise list. */
@Composable
private fun ExercisePlaceholder(exerciseCount: Int, onAddExercises: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (exerciseCount == 0) {
                    "No exercises added to this day yet."
                } else {
                    exerciseLabel(exerciseCount) + " on this day."
                },
                color = WorkoutColors.TextSecondary,
                fontSize = 14.sp
            )
            if (onAddExercises != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OrangeButton(text = "Add Exercises", onClick = onAddExercises, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
