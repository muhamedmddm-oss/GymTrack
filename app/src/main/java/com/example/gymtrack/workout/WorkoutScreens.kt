package com.example.gymtrack.workout

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton

// ---- PlanEditorScreen.kt ----

@Composable
fun CreateWorkoutPlanScreen(onClose: () -> Unit, onSaved: () -> Unit) {
    PlanEditorRoute(planId = null, onClose = onClose, onSaved = onSaved)
}

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


@Composable
fun WorkoutDayDetailsRoute(
    dayId: Long,
    onBack: () -> Unit,
    onEditPlan: (planId: Long) -> Unit,
    onAddExercises: ((dayId: Long) -> Unit)? = null,
    exerciseContent: (@Composable (dayId: Long) -> Unit)? = null,
    onStartWorkout: ((dayId: Long) -> Unit)? = null
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
        onStartWorkout = onStartWorkout?.let { callback -> { callback(dayId) } },
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
    onStartWorkout: (() -> Unit)? = null,
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
                    // Person 4: entry point of the Active Workout screen (hidden until it is wired up).
                    if (onStartWorkout != null) {
                        OrangeButton(
                            text = "Start Workout",
                            onClick = onStartWorkout,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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

// ---- WorkoutPlansScreen.kt ----

/** Stateful entry point used by the navigation graph. */
@Composable
fun WorkoutPlansRoute(
    onBack: () -> Unit,
    onAddPlan: () -> Unit,
    onEditPlan: (planId: Long) -> Unit,
    onOpenDay: (dayId: Long) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: WorkoutPlanViewModel = viewModel(
        factory = WorkoutModule.factory { WorkoutPlanViewModel(WorkoutModule.repository(appContext)) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutPlansScreen(
        state = state,
        onBack = onBack,
        onAddPlan = onAddPlan,
        onEditPlan = onEditPlan,
        onOpenDay = onOpenDay,
        onDeletePlanClick = viewModel::onDeletePlanClick,
        onDeletePlanConfirm = viewModel::onDeletePlanConfirm,
        onDeletePlanDismiss = viewModel::onDeletePlanDismiss,
        onDeleteDayClick = viewModel::onDeleteDayClick,
        onDeleteDayConfirm = viewModel::onDeleteDayConfirm,
        onDeleteDayDismiss = viewModel::onDeleteDayDismiss,
        onErrorShown = viewModel::onErrorShown
    )
}

@Composable
fun WorkoutPlansScreen(
    state: WorkoutPlansUiState,
    onBack: () -> Unit,
    onAddPlan: () -> Unit,
    onEditPlan: (planId: Long) -> Unit,
    onOpenDay: (dayId: Long) -> Unit,
    onDeletePlanClick: (com.example.gymtrack.workout.WorkoutPlan) -> Unit,
    onDeletePlanConfirm: () -> Unit,
    onDeletePlanDismiss: () -> Unit,
    onDeleteDayClick: (com.example.gymtrack.workout.WorkoutDay) -> Unit,
    onDeleteDayConfirm: () -> Unit,
    onDeleteDayDismiss: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Scaffold(
        containerColor = WorkoutColors.Background,
        topBar = { WorkoutTopBar(title = "My Workout Plans", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlan,
                containerColor = WorkoutColors.Orange,
                contentColor = WorkoutColors.OnOrange,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Workout Plan", fontWeight = FontWeight.Bold) }
            )
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

                state.plans.isEmpty() -> EmptyPlansState()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = state.plans, key = { it.id }) { plan ->
                        WorkoutPlanCard(
                            plan = plan,
                            onEditPlan = { onEditPlan(plan.id) },
                            onDeletePlan = { onDeletePlanClick(plan) },
                            onViewDay = { onOpenDay(it.id) },
                            onEditDay = { onEditPlan(plan.id) },
                            onDeleteDay = onDeleteDayClick
                        )
                    }
                }
            }
        }
    }

    state.planPendingDelete?.let { plan ->
        val daysText = if (plan.days.size == 1) "its 1 workout day" else "its ${plan.days.size} workout days"
        DeleteConfirmationDialog(
            title = "Delete plan?",
            message = "\"${plan.name}\" and $daysText will be removed from your schedule. " +
                "Exercises and workout history that were already recorded are kept.",
            onConfirm = onDeletePlanConfirm,
            onDismiss = onDeletePlanDismiss
        )
    }

    state.dayPendingDelete?.let { day ->
        val extra = if (day.exerciseCount > 0) {
            " It has ${day.exerciseCount} exercise(s); they and any workout history are kept."
        } else {
            ""
        }
        DeleteConfirmationDialog(
            title = "Delete workout day?",
            message = "Remove \"${day.name}\" (${day.weekday.displayName}) from this plan?$extra",
            onConfirm = onDeleteDayConfirm,
            onDismiss = onDeleteDayDismiss
        )
    }
}
