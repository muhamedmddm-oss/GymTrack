package com.example.gymtrack.activeworkout.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrack.activeworkout.timer.formatElapsedTime
import com.example.gymtrack.data.GymTrackData
import com.example.gymtrack.workout.WorkoutModule
import com.example.gymtrack.workout.DeleteConfirmationDialog
import com.example.gymtrack.workout.OrangeButton
import com.example.gymtrack.workout.SectionLabel
import com.example.gymtrack.workout.WorkoutTopBar
import com.example.gymtrack.workout.WorkoutColors
import com.example.gymtrack.workout.workoutTextFieldColors

/** All the things the screen can ask the ViewModel to do (keeps the composable signatures short). */
class ActiveWorkoutActions(
    val onBack: () -> Unit,
    val onStart: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onReset: () -> Unit,
    val onAddExercise: (name: String, setCount: Int) -> Unit,
    val onRemoveExercise: (exerciseId: Long) -> Unit,
    val onRepsChange: (exerciseId: Long, setId: Long, text: String) -> Unit,
    val onWeightChange: (exerciseId: Long, setId: Long, text: String) -> Unit,
    val onSetCompletedChange: (exerciseId: Long, setId: Long, completed: Boolean) -> Unit,
    val onAddSet: (exerciseId: Long) -> Unit,
    val onDeleteSet: (exerciseId: Long, setId: Long) -> Unit,
    val onFinish: () -> Unit,
    val onDiscard: () -> Unit,
    val onMessageShown: () -> Unit
)

/**
 * Stateful entry point of the "Active Workout" destination.
 * [dayId] is the workout day chosen on the Workout Details screen (teammate's part).
 * [onWorkoutFinished] receives the id of the saved session and opens the Workout Summary.
 */
@Composable
fun ActiveWorkoutRoute(
    dayId: Long,
    onBack: () -> Unit,
    onWorkoutFinished: (sessionId: Long) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: ActiveWorkoutViewModel = viewModel(
        factory = WorkoutModule.factory {
            ActiveWorkoutViewModel(
                workoutRepository = GymTrackData.workouts(appContext),
                exerciseRepository = GymTrackData.exercises(appContext),
                sessionRepository = GymTrackData.sessions(appContext),
                dayId = dayId
            )
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigation is triggered by state, once.
    LaunchedEffect(state.finishedSessionId) {
        state.finishedSessionId?.let { onWorkoutFinished(it) }
    }
    LaunchedEffect(state.leaveScreen) {
        if (state.leaveScreen) onBack()
    }

    val actions = remember(viewModel, onBack) {
        ActiveWorkoutActions(
            onBack = onBack,
            onStart = viewModel::onStartWorkout,
            onPause = viewModel::onPauseWorkout,
            onResume = viewModel::onResumeWorkout,
            onReset = viewModel::onResetTimer,
            onAddExercise = viewModel::onAddExercise,
            onRemoveExercise = viewModel::onRemoveExercise,
            onRepsChange = viewModel::onRepsChanged,
            onWeightChange = viewModel::onWeightChanged,
            onSetCompletedChange = viewModel::onSetCompletedChanged,
            onAddSet = viewModel::onAddSet,
            onDeleteSet = viewModel::onDeleteSet,
            onFinish = viewModel::onFinishWorkout,
            onDiscard = viewModel::onDiscardWorkout,
            onMessageShown = viewModel::onMessageShown
        )
    }
    ActiveWorkoutScreen(state = state, actions = actions)
}

@Composable
fun ActiveWorkoutScreen(state: ActiveWorkoutUiState, actions: ActiveWorkoutActions) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var exerciseIdToRemove by rememberSaveable { mutableStateOf<Long?>(null) }

    // Feedback to the user (exercise added/removed, invalid action, ...).
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            actions.onMessageShown()
        }
    }

    Scaffold(
        containerColor = WorkoutColors.Background,
        topBar = {
            WorkoutTopBar(
                title = "Active Workout",
                onBack = actions.onBack,
                actions = {
                    if (state.hasStarted) {
                        OverflowMenu(onDiscard = { showDiscardDialog = true })
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = WorkoutColors.CardRaised,
                    contentColor = WorkoutColors.TextPrimary,
                    actionColor = WorkoutColors.Orange
                )
            }
        },
        bottomBar = {
            if (!state.isLoading && state.loadError == null) {
                FinishBar(
                    isFinishing = state.isFinishing,
                    onClick = {
                        // Only ask for confirmation when finishing is possible; otherwise the
                        // ViewModel explains what is missing (not started / no exercises).
                        if (state.hasStarted && state.exercises.isNotEmpty()) showFinishDialog = true
                        else actions.onFinish()
                    }
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
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = WorkoutColors.Orange
                )

                state.loadError != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.loadError,
                        color = WorkoutColors.TextPrimary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OrangeButton(text = "Go back", onClick = actions.onBack)
                }

                else -> WorkoutContent(
                    state = state,
                    actions = actions,
                    onAddExerciseClick = { showAddDialog = true },
                    onResetClick = { showResetDialog = true },
                    onRemoveExerciseClick = { exerciseIdToRemove = it }
                )
            }
        }
    }

    // ---- dialogs ----
    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, sets ->
                actions.onAddExercise(name, sets)
                showAddDialog = false
            }
        )
    }
    if (showFinishDialog) {
        DeleteConfirmationDialog(
            title = "Finish workout?",
            message = "You completed ${state.completedSets} of ${state.totalSets} sets " +
                "(${state.completedExercises} of ${state.totalExercises} exercises). " +
                "The timer stops and the workout is saved.",
            confirmLabel = "Finish",
            destructive = false,
            onConfirm = {
                showFinishDialog = false
                actions.onFinish()
            },
            onDismiss = { showFinishDialog = false }
        )
    }
    if (showResetDialog) {
        DeleteConfirmationDialog(
            title = "Reset timer?",
            message = "The timer goes back to 00:00. Your exercises and sets are kept.",
            confirmLabel = "Reset",
            onConfirm = {
                showResetDialog = false
                actions.onReset()
            },
            onDismiss = { showResetDialog = false }
        )
    }
    if (showDiscardDialog) {
        DeleteConfirmationDialog(
            title = "Discard workout?",
            message = "This deletes the whole workout and everything you logged in it.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardDialog = false
                actions.onDiscard()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
    exerciseIdToRemove?.let { id ->
        val exercise = state.exercises.firstOrNull { it.id == id }
        if (exercise == null) {
            exerciseIdToRemove = null
        } else {
            DeleteConfirmationDialog(
                title = "Remove exercise?",
                message = "${exercise.name} and its ${exercise.sets.size} sets will be removed from this workout.",
                confirmLabel = "Remove",
                onConfirm = {
                    exerciseIdToRemove = null
                    actions.onRemoveExercise(id)
                },
                onDismiss = { exerciseIdToRemove = null }
            )
        }
    }
}

// ====================================================================== main content

@Composable
private fun WorkoutContent(
    state: ActiveWorkoutUiState,
    actions: ActiveWorkoutActions,
    onAddExerciseClick: () -> Unit,
    onResetClick: () -> Unit,
    onRemoveExerciseClick: (Long) -> Unit
) {
    LazyColumn(
        // widthIn keeps the layout readable on tablets / landscape.
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "timer") {
            TimerCard(
                state = state,
                onStart = actions.onStart,
                onPause = actions.onPause,
                onResume = actions.onResume,
                onReset = onResetClick
            )
        }
        item(key = "progress") { ProgressCard(state) }
        item(key = "exercises_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("Exercises")
                OutlinedButton(
                    onClick = onAddExerciseClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WorkoutColors.Orange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = WorkoutColors.Orange)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Exercise", color = WorkoutColors.Orange, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (state.exercises.isEmpty()) {
            item(key = "empty") { EmptyExercises(state) }
        } else {
            items(state.exercises, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onRemove = { onRemoveExerciseClick(exercise.id) },
                    onRepsChange = { setId, text -> actions.onRepsChange(exercise.id, setId, text) },
                    onWeightChange = { setId, text -> actions.onWeightChange(exercise.id, setId, text) },
                    onCompletedChange = { setId, done -> actions.onSetCompletedChange(exercise.id, setId, done) },
                    onAddSet = { actions.onAddSet(exercise.id) },
                    onDeleteSet = { setId -> actions.onDeleteSet(exercise.id, setId) }
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    state: ActiveWorkoutUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
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
            Text(
                text = state.dayName,
                color = WorkoutColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(text = state.planName, color = WorkoutColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatElapsedTime(state.elapsedSeconds),
                color = WorkoutColors.Orange,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = when (state.timerStatus) {
                    TimerStatus.NOT_STARTED -> "Not started"
                    TimerStatus.RUNNING -> "In progress"
                    TimerStatus.PAUSED -> "Paused"
                },
                color = WorkoutColors.TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            when (state.timerStatus) {
                TimerStatus.NOT_STARTED -> OrangeButton(
                    text = "Start Workout",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                )

                TimerStatus.RUNNING -> TimerButtons("Pause", onPause, onReset)

                TimerStatus.PAUSED -> TimerButtons(
                    primaryLabel = if (state.elapsedSeconds == 0L) "Start" else "Resume",
                    onPrimary = onResume,
                    onReset = onReset
                )
            }
        }
    }
}

@Composable
private fun TimerButtons(primaryLabel: String, onPrimary: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OrangeButton(text = primaryLabel, onClick = onPrimary, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, WorkoutColors.Outline)
        ) {
            Text("Reset", color = WorkoutColors.TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProgressCard(state: ActiveWorkoutUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.completedExercises}/${state.totalExercises} exercises completed",
                    color = WorkoutColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${(state.progress * 100).toInt()}%",
                    color = WorkoutColors.Orange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = WorkoutColors.Orange,
                trackColor = WorkoutColors.Outline
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${state.completedSets} of ${state.totalSets} sets done",
                color = WorkoutColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptyExercises(state: ActiveWorkoutUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            text = when {
                !state.hasStarted && state.plannedExerciseCount > 0 ->
                    "Press Start Workout to begin. The ${state.plannedExerciseCount} exercises planned for this day will be loaded."

                !state.hasStarted -> "Press Start Workout to begin, then add your exercises."
                else -> "No exercises yet. Tap Add Exercise to log your first one."
            },
            color = WorkoutColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ====================================================================== exercise + sets

@Composable
private fun ExerciseCard(
    exercise: ExerciseUi,
    onRemove: () -> Unit,
    onRepsChange: (setId: Long, text: String) -> Unit,
    onWeightChange: (setId: Long, text: String) -> Unit,
    onCompletedChange: (setId: Long, completed: Boolean) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (setId: Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkoutColors.Card)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        color = WorkoutColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exercise.completedSets}/${exercise.sets.size} sets",
                        color = WorkoutColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (exercise.isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Exercise completed",
                        tint = WorkoutColors.Orange
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove ${exercise.name}", tint = WorkoutColors.Error)
                }
            }
            Spacer(Modifier.height(8.dp))
            SetHeaderRow()
            exercise.sets.forEach { set ->
                // key(): each set keeps its own typing state even when rows above are deleted.
                androidx.compose.runtime.key(set.id) {
                    SetRow(
                        set = set,
                        onRepsChange = { onRepsChange(set.id, it) },
                        onWeightChange = { onWeightChange(set.id, it) },
                        onCompletedChange = { onCompletedChange(set.id, it) },
                        onDelete = { onDeleteSet(set.id) }
                    )
                }
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, contentDescription = null, tint = WorkoutColors.Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Set", color = WorkoutColors.Orange, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeaderLabel("SET", Modifier.width(28.dp))
        HeaderLabel("REPS", Modifier.weight(1f))
        HeaderLabel("KG", Modifier.weight(1f))
        HeaderLabel("DONE", Modifier.width(48.dp))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun HeaderLabel(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = WorkoutColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SetRow(
    set: SetUi,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = set.setNumber.toString(),
            modifier = Modifier.width(28.dp),
            color = WorkoutColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        SetInputField(
            initialValue = set.reps,
            placeholder = "0",
            keyboardType = KeyboardType.Number,
            sanitize = ::sanitizeRepsInput,
            onValueChange = onRepsChange,
            modifier = Modifier.weight(1f)
        )
        SetInputField(
            initialValue = set.weightKg,
            placeholder = "kg",
            keyboardType = KeyboardType.Decimal,
            sanitize = ::sanitizeWeightInput,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = set.isCompleted,
            onCheckedChange = onCompletedChange,
            modifier = Modifier.width(48.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = WorkoutColors.Orange,
                checkmarkColor = WorkoutColors.OnOrange,
                uncheckedColor = WorkoutColors.Outline
            )
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete set ${set.setNumber}",
                tint = WorkoutColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * A numeric text field. The text is kept in LOCAL state (so typing never fights with the flow of
 * ViewModel updates) and every change is cleaned by [sanitize] and sent up to the ViewModel -> Room.
 */
@Composable
private fun SetInputField(
    initialValue: String,
    placeholder: String,
    keyboardType: KeyboardType,
    sanitize: (String) -> String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(initialValue) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val clean = sanitize(raw)
            text = clean
            onValueChange(clean)
        },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 16.sp),
        placeholder = {
            Text(
                text = placeholder,
                modifier = Modifier.fillMaxWidth(),
                color = WorkoutColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = workoutTextFieldColors()
    )
}

// ====================================================================== small pieces

@Composable
private fun OverflowMenu(onDiscard: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = WorkoutColors.TextPrimary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = WorkoutColors.CardRaised
        ) {
            DropdownMenuItem(
                text = { Text("Discard workout", color = WorkoutColors.Error) },
                onClick = {
                    expanded = false
                    onDiscard()
                }
            )
        }
    }
}

@Composable
private fun FinishBar(isFinishing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkoutColors.Background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        OrangeButton(
            text = if (isFinishing) "Saving..." else "Finish Workout",
            onClick = onClick,
            enabled = !isFinishing,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        )
    }
}

@Composable
private fun AddExerciseDialog(onDismiss: () -> Unit, onConfirm: (name: String, sets: Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var setsText by rememberSaveable { mutableStateOf("3") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val nameError = if (name.isBlank()) "Enter an exercise name" else null
    val sets = setsText.toIntOrNull()
    val setsError = if (sets == null || sets !in 1..MAX_SETS_PER_EXERCISE) {
        "Enter 1 to $MAX_SETS_PER_EXERCISE sets"
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WorkoutColors.Card,
        title = { Text("Add Exercise", color = WorkoutColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_EXERCISE_NAME_LENGTH) },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    isError = submitted && nameError != null,
                    supportingText = { if (submitted && nameError != null) Text(nameError) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = workoutTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = setsText,
                    onValueChange = { setsText = sanitizeRepsInput(it).take(2) },
                    label = { Text("Number of sets") },
                    singleLine = true,
                    isError = submitted && setsError != null,
                    supportingText = { if (submitted && setsError != null) Text(setsError) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = workoutTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (nameError == null && setsError == null && sets != null) onConfirm(name, sets)
            }) {
                Text("Add", color = WorkoutColors.Orange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = WorkoutColors.TextPrimary) }
        }
    )
}
