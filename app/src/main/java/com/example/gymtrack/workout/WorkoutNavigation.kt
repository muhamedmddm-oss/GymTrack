package com.example.gymtrack.workout

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// ---- WorkoutNavigation.kt ----

object WorkoutRoutes {
    const val PLANS = "workout_plans"
    const val CREATE_PLAN = "create_workout_plan"

    const val ARG_PLAN_ID = "planId"
    const val EDIT_PLAN = "edit_workout_plan/{$ARG_PLAN_ID}"

    const val ARG_DAY_ID = "dayId"
    const val DAY_DETAILS = "workout_day_details/{$ARG_DAY_ID}"

    fun editPlan(planId: Long) = "edit_workout_plan/$planId"
    fun dayDetails(dayId: Long) = "workout_day_details/$dayId"
}


fun NavGraphBuilder.workoutNavGraph(
    navController: NavController,
    onAddExercises: ((dayId: Long) -> Unit)? = null,
    exerciseContent: (@Composable (dayId: Long) -> Unit)? = null,
    onStartWorkout: ((dayId: Long) -> Unit)? = null
) {
    composable(WorkoutRoutes.PLANS) {
        WorkoutPlansRoute(
            onBack = { navController.popBackStack() },
            onAddPlan = { navController.navigate(WorkoutRoutes.CREATE_PLAN) },
            onEditPlan = { planId -> navController.navigate(WorkoutRoutes.editPlan(planId)) },
            onOpenDay = { dayId -> navController.navigate(WorkoutRoutes.dayDetails(dayId)) }
        )
    }

    composable(WorkoutRoutes.CREATE_PLAN) {
        CreateWorkoutPlanScreen(
            onClose = { navController.popBackStack() },
            onSaved = { navController.popBackStack() }
        )
    }

    composable(
        route = WorkoutRoutes.EDIT_PLAN,
        arguments = listOf(navArgument(WorkoutRoutes.ARG_PLAN_ID) { type = NavType.LongType })
    ) { entry ->
        val planId = entry.arguments?.getLong(WorkoutRoutes.ARG_PLAN_ID)
        if (planId != null) {
            EditWorkoutPlanScreen(
                planId = planId,
                onClose = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }

    composable(
        route = WorkoutRoutes.DAY_DETAILS,
        arguments = listOf(navArgument(WorkoutRoutes.ARG_DAY_ID) { type = NavType.LongType })
    ) { entry ->
        val dayId = entry.arguments?.getLong(WorkoutRoutes.ARG_DAY_ID)
        if (dayId != null) {
            WorkoutDayDetailsRoute(
                dayId = dayId,
                onBack = { navController.popBackStack() },
                onEditPlan = { planId -> navController.navigate(WorkoutRoutes.editPlan(planId)) },
                onAddExercises = onAddExercises,
                exerciseContent = exerciseContent,
                onStartWorkout = onStartWorkout
            )
        }
    }
}
