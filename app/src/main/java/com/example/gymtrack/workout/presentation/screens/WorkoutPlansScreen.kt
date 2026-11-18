package com.example.gymtrack.workout.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrack.workout.WorkoutModule
import com.example.gymtrack.workout.presentation.components.DeleteConfirmationDialog
import com.example.gymtrack.workout.presentation.components.EmptyPlansState
import com.example.gymtrack.workout.presentation.components.WorkoutPlanCard
import com.example.gymtrack.workout.presentation.components.WorkoutTopBar
import com.example.gymtrack.workout.presentation.theme.WorkoutColors
import com.example.gymtrack.workout.presentation.viewmodel.WorkoutPlanViewModel
import com.example.gymtrack.workout.presentation.viewmodel.WorkoutPlansUiState

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

/** "My Workout Plans" screen. Stateless so it is easy to preview and test. */
@Composable
fun WorkoutPlansScreen(
    state: WorkoutPlansUiState,
    onBack: () -> Unit,
    onAddPlan: () -> Unit,
    onEditPlan: (planId: Long) -> Unit,
    onOpenDay: (dayId: Long) -> Unit,
    onDeletePlanClick: (com.example.gymtrack.workout.domain.model.WorkoutPlan) -> Unit,
    onDeletePlanConfirm: () -> Unit,
    onDeletePlanDismiss: () -> Unit,
    onDeleteDayClick: (com.example.gymtrack.workout.domain.model.WorkoutDay) -> Unit,
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
