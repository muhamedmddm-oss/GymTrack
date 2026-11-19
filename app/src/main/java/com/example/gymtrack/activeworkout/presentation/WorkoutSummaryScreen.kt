package com.example.gymtrack.activeworkout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrack.activeworkout.timer.formatElapsedTime
import com.example.gymtrack.data.GymTrackData
import com.example.gymtrack.data.dao.SessionExerciseWithSets
import com.example.gymtrack.data.dao.SessionWithDetails
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.workout.WorkoutModule
import com.example.gymtrack.workout.presentation.components.OrangeButton
import com.example.gymtrack.workout.presentation.components.SectionLabel
import com.example.gymtrack.workout.presentation.components.WorkoutTopBar
import com.example.gymtrack.workout.presentation.theme.WorkoutColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stateful entry point: loads the finished workout from Room using the id sent by Finish Workout. */
@Composable
fun WorkoutSummaryRoute(
    sessionId: Long,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: WorkoutSummaryViewModel = viewModel(
        factory = WorkoutModule.factory {
            WorkoutSummaryViewModel(GymTrackData.sessions(appContext), sessionId)
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutSummaryScreen(state = state, onBack = onBack, onDone = onDone)
}

@Composable
fun WorkoutSummaryScreen(state: WorkoutSummaryUiState, onBack: () -> Unit, onDone: () -> Unit) {
    Scaffold(
        containerColor = WorkoutColors.Background,
        topBar = { WorkoutTopBar(title = "Workout Summary", onBack = onBack) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WorkoutColors.Background)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                OrangeButton(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WorkoutColors.Background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val details = state.details
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = WorkoutColors.Orange
                )

                details == null -> Text(
                    text = "This workout could not be found.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    color = WorkoutColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                else -> SummaryContent(details)
            }
        }
    }
}

@Composable
private fun SummaryContent(details: SessionWithDetails) {
    val stats = buildSummaryStats(details)
    val exercises = details.exercises.sortedWith(compareBy({ it.exercise.orderIndex }, { it.exercise.id }))
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SummaryHeader(details) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Exercises", "${stats.completedExercises}/${stats.totalExercises}")
                StatTile("Sets", "${stats.completedSets}/${stats.totalSets}")
                StatTile("Volume", "${formatWeight(stats.totalVolumeKg)} kg")
            }
        }
        item { SectionLabel("Workout details") }
        if (exercises.isEmpty()) {
            item { Text("No exercises were recorded.", color = WorkoutColors.TextSecondary) }
        }
        items(exercises, key = { it.exercise.id }) { SummaryExerciseCard(it) }
    }
}

@Composable
private fun SummaryHeader(details: SessionWithDetails) {
    val session = details.session
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = WorkoutColors.Orange,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Workout complete!", color = WorkoutColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${session.dayName} · ${session.planName}", color = WorkoutColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Text("DURATION", color = WorkoutColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = formatElapsedTime(session.durationSeconds),
                color = WorkoutColors.Orange,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(session.startTime)),
                color = WorkoutColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: String) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = WorkoutColors.Orange, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(label, color = WorkoutColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SummaryExerciseCard(item: SessionExerciseWithSets) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.exercise.exerciseName, color = WorkoutColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            item.sets.sortedBy { it.setNumber }.forEach { set ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Set ${set.setNumber}  ·  ${setDescription(set)}",
                        modifier = Modifier.weight(1f),
                        color = if (set.isCompleted) WorkoutColors.TextPrimary else WorkoutColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    if (set.isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = WorkoutColors.Orange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/** "10 reps × 40 kg", or "10 reps" when no weight was entered. */
private fun setDescription(set: SessionSetEntity): String {
    val reps = if (set.reps > 0) "${set.reps} reps" else "no reps"
    val weight = set.weightKg?.let { " × ${formatWeight(it)} kg" }.orEmpty()
    return reps + weight
}
