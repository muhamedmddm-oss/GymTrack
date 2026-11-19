package com.example.gymtrack.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymtrack.GymSetupApp
import com.example.gymtrack.activeworkout.ActiveWorkoutRoutes
import com.example.gymtrack.activeworkout.activeWorkoutNavGraph
import com.example.gymtrack.home.HomeScreen
import com.example.gymtrack.workout.navigation.WorkoutRoutes
import com.example.gymtrack.workout.navigation.workoutNavGraph

object AppRoutes {
    const val SETUP = "setup"   // Person 1: splash, onboarding, profile
    const val HOME = "home"
}

/**
 * The ONE NavHost of the app (PERSON 7 owns this file; other people add their
 * `xxxNavGraph(navController)` calls inside the NavHost block below).
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRoutes.SETUP) {
        // Person 1 – onboarding. Returning users (profile already saved) go straight to Home.
        composable(AppRoutes.SETUP) {
            GymSetupApp(
                onFinished = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.HOME) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreen()

                // TEMPORARY entry point (Person 2) so the workout module can be opened today.
                // PERSON 5: add an `onOpenWorkoutPlans` callback to HomeScreen, call
                // navController.navigate(WorkoutRoutes.PLANS) from it, then delete this button.
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(WorkoutRoutes.PLANS) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    containerColor = Color(0xFFFF8A00),
                    contentColor = Color(0xFF101010)
                ) {
                    Text("My Workout Plans")
                }
            }
        }

        // Person 2 – Workout Schedule Management
        // Person 4 hooks in through onStartWorkout: Workout Day Details -> Active Workout.
        // Person 3: also pass onAddExercises = { dayId -> ... } once your screens exist.
        workoutNavGraph(
            navController,
            onStartWorkout = { dayId -> navController.navigate(ActiveWorkoutRoutes.activeWorkout(dayId)) }
        )

        // Person 4 – Active Workout & Timer, then Workout Summary (Done -> back to Home).
        activeWorkoutNavGraph(
            navController,
            onSummaryDone = { navController.popBackStack(AppRoutes.HOME, inclusive = false) }
        )
    }
}
