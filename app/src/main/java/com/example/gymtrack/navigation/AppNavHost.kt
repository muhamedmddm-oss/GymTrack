
package com.example.gymtrack.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymtrack.GymSetupApp
import com.example.gymtrack.activeworkout.ActiveWorkoutRoutes
import com.example.gymtrack.activeworkout.activeWorkoutNavGraph
import com.example.gymtrack.home.BottomNavigationBar
import com.example.gymtrack.home.HomeScreen
import com.example.gymtrack.statistics.WorkoutStatisticsRoute
import com.example.gymtrack.workout.WorkoutRoutes
import com.example.gymtrack.workout.workoutNavGraph

object AppRoutes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val STATISTICS = "statistics"
    const val PROFILE = "profile"
}

/**
 * The single shared Bottom Navigation Bar for GymTrack.
 * HomeScreen only displays its content.
 */
@Composable
fun AppNavHost() {

    val navController = rememberNavController()

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    val showBottomBar =
        currentRoute != null && currentRoute != AppRoutes.SETUP

    val selectedTab = when (currentRoute) {
        AppRoutes.HOME -> 0
        AppRoutes.STATISTICS -> 2
        AppRoutes.PROFILE -> 3
        else -> 1
    }

    Scaffold(
        containerColor = Color(0xFF101010),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->

                        val destination = when (tab) {
                            0 -> AppRoutes.HOME
                            1 -> WorkoutRoutes.PLANS
                            2 -> AppRoutes.STATISTICS
                            else -> AppRoutes.PROFILE
                        }

                        if (currentRoute != destination) {
                            navController.navigate(destination) {
                                launchSingleTop = true
                                restoreState = true

                                popUpTo(AppRoutes.HOME) {
                                    saveState = true
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = AppRoutes.SETUP,
            modifier = Modifier
                .padding(innerPadding)
        ) {

            // Person 1 – Setup and Profile
            composable(AppRoutes.SETUP) {
                GymSetupApp(
                    onFinished = {
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(AppRoutes.SETUP) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            // Home Page
            composable(AppRoutes.HOME) {
                HomeScreen()
            }

            // Person 6 – live statistics calculated from completed Room sessions.
            composable(AppRoutes.STATISTICS) {
                WorkoutStatisticsRoute()
            }

            // Profile data is already persisted by the shared Person 7 database.
            composable(AppRoutes.PROFILE) {
                com.example.gymtrack.home.ProfileScreen()
            }

            // Person 2 – Workout Schedule Management
            workoutNavGraph(
                navController = navController,
                onStartWorkout = { dayId ->
                    navController.navigate(
                        ActiveWorkoutRoutes.activeWorkout(dayId)
                    )
                }
            )

            // Person 4 – Active Workout and Timer
            activeWorkoutNavGraph(
                navController = navController,
                onSummaryDone = {
                    navController.popBackStack(
                        AppRoutes.HOME,
                        inclusive = false
                    )
                }
            )
        }
    }
}
