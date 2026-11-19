package com.example.gymtrack.activeworkout

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gymtrack.activeworkout.presentation.ActiveWorkoutRoute
import com.example.gymtrack.activeworkout.presentation.WorkoutSummaryRoute

/** Route strings of the Active Workout module (Person 4). */
object ActiveWorkoutRoutes {
    const val ARG_DAY_ID = "dayId"
    const val ACTIVE_WORKOUT = "active_workout/{$ARG_DAY_ID}"

    const val ARG_SESSION_ID = "sessionId"
    const val WORKOUT_SUMMARY = "workout_summary/{$ARG_SESSION_ID}"

    fun activeWorkout(dayId: Long) = "active_workout/$dayId"
    fun workoutSummary(sessionId: Long) = "workout_summary/$sessionId"
}

/**
 * Registers the 2 destinations of this module on the app's EXISTING NavHost:
 *   active_workout/{dayId}        <- opened from Workout Day Details ("Start Workout")
 *   workout_summary/{sessionId}   <- opened by "Finish Workout"
 *
 * Only ids travel through navigation; each screen reads its data from Room.
 * [onSummaryDone] is called by the Done button of the summary (the app decides where to go, e.g. Home).
 */
fun NavGraphBuilder.activeWorkoutNavGraph(
    navController: NavController,
    onSummaryDone: () -> Unit
) {
    composable(
        route = ActiveWorkoutRoutes.ACTIVE_WORKOUT,
        arguments = listOf(navArgument(ActiveWorkoutRoutes.ARG_DAY_ID) { type = NavType.LongType })
    ) { entry ->
        val dayId = entry.arguments?.getLong(ActiveWorkoutRoutes.ARG_DAY_ID)
        if (dayId != null) {
            ActiveWorkoutRoute(
                dayId = dayId,
                onBack = { navController.popBackStack() },
                onWorkoutFinished = { sessionId ->
                    navController.navigate(ActiveWorkoutRoutes.workoutSummary(sessionId)) {
                        // The finished workout must not be reachable again with the Back button.
                        popUpTo(ActiveWorkoutRoutes.ACTIVE_WORKOUT) { inclusive = true }
                    }
                }
            )
        }
    }

    composable(
        route = ActiveWorkoutRoutes.WORKOUT_SUMMARY,
        arguments = listOf(navArgument(ActiveWorkoutRoutes.ARG_SESSION_ID) { type = NavType.LongType })
    ) { entry ->
        val sessionId = entry.arguments?.getLong(ActiveWorkoutRoutes.ARG_SESSION_ID)
        if (sessionId != null) {
            WorkoutSummaryRoute(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onDone = onSummaryDone
            )
        }
    }
}
