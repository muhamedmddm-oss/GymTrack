package com.example.gymtrack.workout.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrack.workout.WorkoutModule
import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.domain.validation.WorkoutPlanValidator
import com.example.gymtrack.workout.presentation.components.DayCountSelector
import com.example.gymtrack.workout.presentation.components.DeleteConfirmationDialog
import com.example.gymtrack.workout.presentation.components.EditorDayCard
import com.example.gymtrack.workout.presentation.components.ErrorText
import com.example.gymtrack.workout.presentation.components.OrangeButton
import com.example.gymtrack.workout.presentation.components.SectionLabel
import com.example.gymtrack.workout.presentation.components.WorkoutTopBar
import com.example.gymtrack.workout.presentation.theme.WorkoutColors
import com.example.gymtrack.workout.presentation.theme.workoutTextFieldColors
import com.example.gymtrack.workout.presentation.viewmodel.PlanEditorEvent
import com.example.gymtrack.workout.presentation.viewmodel.PlanEditorUiState
import com.example.gymtrack.workout.presentation.viewmodel.WorkoutPlanEditorViewModel

/** Create Workout Plan screen. */
@Composable
fun CreateWorkoutPlanScreen(onClose: () -> Unit, onSaved: () -> Unit) {
    PlanEditorRoute(planId = null, onClose = onClose, onSaved = onSaved)
}

/** Edit Workout Plan screen (preserves existing days, so exercises stay attached). */
@Composable
fun EditWorkoutPlanScreen(planId: Long, onClose: () -> Unit, onSaved: () -> Unit) {
    PlanEditorRoute(planId = planId, onClose = onClose, onSaved = onSaved)
}

@Composable
private fun PlanEditorRoute(planId: Long?, onClose: () -> Unit, onSaved: () -> Unit) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: WorkoutPlanEditorViewModel = viewModel(
        factory = WorkoutModule.factory {
            WorkoutPlanEditorViewModel(WorkoutModule.repository(appContext), planId)
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlanEditorEvent.Saved -> onSaved()
                PlanEditorEvent.Close -> onClose()
            }
        }
    }

    // System back = same behaviour as the top-bar arrow (asks before discarding changes).
    BackHandler(onBack = viewModel::onBackRequested)

    PlanEditorScreen(
        state = state,
        onBack = viewModel::onBackRequested,
        onPlanNameChange = viewModel::onPlanNameChange,
        onTrainingDaysSelected = viewModel::onTrainingDaysSelected,
        onDayNameChange = viewModel::onDayNameChange,
        onWeekdaySelected = viewModel::onWeekdaySelected,
        onAddDay = viewModel::onAddDay,
        onRemoveDay = viewModel::onRemoveDay,
        onSave = viewModel::onSave,
        onSaveErrorShown = viewModel::onSaveErrorShown,
        onConfirmRemoval = viewModel::onConfirmRemoval,
        onDismissRemoval = viewModel::onDismissRemoval,
        onDiscardConfirm = viewModel::onDiscardConfirm,
        onDiscardDismiss = viewModel::onDiscardDismiss
    )
}

/** Stateless editor UI shared by the Create and Edit screens. */
@Composable
fun PlanEditorScreen(
    state: PlanEditorUiState,
    onBack: () -> Unit,
    onPlanNameChange: (String) -> Unit,
    onTrainingDaysSelected: (Int) -> Unit,
    onDayNameChange: (key: Long, name: String) -> Unit,
    onWeekdaySelected: (key: Long, weekday: Weekday) -> Unit,
    onAddDay: () -> Unit,
    onRemoveDay: (key: Long) -> Unit,
    onSave: () -> Unit,
    onSaveErrorShown: () -> Unit,
    onConfirmRemoval: () -> Unit,
    onDismissRemoval: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.saveError) {
        val message = state.saveError
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onSaveErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = WorkoutColors.Background,
        topBar = {
            WorkoutTopBar(
                title = if (state.isEditMode) "Edit Workout Plan" else "Create Workout Plan",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!state.isLoading && state.loadError == null) {
                Surface(color = WorkoutColors.Background) {
                    OrangeButton(
                        text = when {
                            state.isSaving -> "Saving…"
                            state.isEditMode -> "Save Changes"
                            else -> "Save Plan"
                        },
                        onClick = onSave,
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WorkoutColors.Background)
                .padding(innerPadding)
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
                    OrangeButton(text = "Go back", onClick = onBack)
                }

                else -> EditorForm(
                    state = state,
                    onPlanNameChange = onPlanNameChange,
                    onTrainingDaysSelected = onTrainingDaysSelected,
                    onDayNameChange = onDayNameChange,
                    onWeekdaySelected = onWeekdaySelected,
                    onAddDay = onAddDay,
                    onRemoveDay = onRemoveDay
                )
            }
        }
    }

    if (state.pendingRemovalKeys != null) {
        DeleteConfirmationDialog(
            title = "Remove workout day?",
            message = "${state.pendingRemovalExerciseCount} exercise(s) are attached to the day(s) you are " +
                "removing. They will disappear from your schedule, but the exercises and any workout " +
                "history are kept. Changes are applied when you tap Save.",
            confirmLabel = "Remove",
            onConfirm = onConfirmRemoval,
            onDismiss = onDismissRemoval
        )
    }

    if (state.showDiscardDialog) {
        DeleteConfirmationDialog(
            title = "Discard changes?",
            message = "You have unsaved changes. If you leave now they will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = onDiscardConfirm,
            onDismiss = onDiscardDismiss
        )
    }
}

@Composable
private fun EditorForm(
    state: PlanEditorUiState,
    onPlanNameChange: (String) -> Unit,
    onTrainingDaysSelected: (Int) -> Unit,
    onDayNameChange: (key: Long, name: String) -> Unit,
    onWeekdaySelected: (key: Long, weekday: Weekday) -> Unit,
    onAddDay: () -> Unit,
    onRemoveDay: (key: Long) -> Unit
) {
    val planNameError = if (state.showErrors) state.validation.planNameError else null
    val daysError = if (state.showErrors) state.validation.daysError else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "plan_name") {
            OutlinedTextField(
                value = state.planName,
                onValueChange = onPlanNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Workout plan name") },
                placeholder = { Text("e.g. My PPL Program") },
                singleLine = true,
                isError = planNameError != null,
                supportingText = planNameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                colors = workoutTextFieldColors()
            )
        }

        item(key = "day_count") {
            Column {
                SectionLabel("Training days per week")
                Spacer(modifier = Modifier.height(10.dp))
                DayCountSelector(
                    selectedCount = state.days.size,
                    onSelect = onTrainingDaysSelected,
                    maxDays = WorkoutPlanValidator.MAX_DAYS
                )
                if (daysError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ErrorText(daysError)
                }
            }
        }

        itemsIndexed(items = state.days, key = { _, draft -> draft.key }) { index, draft ->
            EditorDayCard(
                number = index + 1,
                draft = draft,
                validation = state.validation.dayErrors.getOrNull(index),
                showErrors = state.showErrors,
                takenByOthers = state.days
                    .filter { it.key != draft.key }
                    .mapNotNull { it.weekday }
                    .toSet(),
                canRemove = state.days.size > WorkoutPlanValidator.MIN_DAYS,
                onNameChange = { onDayNameChange(draft.key, it) },
                onWeekdaySelect = { onWeekdaySelected(draft.key, it) },
                onRemove = { onRemoveDay(draft.key) }
            )
        }

        if (state.days.size < WorkoutPlanValidator.MAX_DAYS) {
            item(key = "add_day") {
                OutlinedButton(
                    onClick = onAddDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WorkoutColors.Orange),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WorkoutColors.Orange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add workout day")
                }
            }
        }
    }
}
