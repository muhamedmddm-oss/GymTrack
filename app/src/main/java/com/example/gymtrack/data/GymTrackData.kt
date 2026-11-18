package com.example.gymtrack.data

import android.content.Context
import com.example.gymtrack.data.repository.ExerciseRepository
import com.example.gymtrack.data.repository.UserProfileRepository
import com.example.gymtrack.data.repository.WorkoutSessionRepository
import com.example.gymtrack.workout.WorkoutModule

/**
 * Single entry point to every repository of the app (all share the one [GymTrackDatabase]).
 *
 *   val profileRepo  = GymTrackData.profiles(context)
 *   val exerciseRepo = GymTrackData.exercises(context)
 *   val sessionRepo  = GymTrackData.sessions(context)
 *   val workoutRepo  = GymTrackData.workouts(context)   // Person 2's WorkoutRepository
 *
 * Always pass any Context; the application context is used internally.
 */
object GymTrackData {
    fun database(context: Context): GymTrackDatabase = GymTrackDatabase.getInstance(context)

    fun profiles(context: Context) = UserProfileRepository(database(context).userProfileDao())
    fun exercises(context: Context) = ExerciseRepository(database(context).exerciseDao())
    fun sessions(context: Context) = WorkoutSessionRepository(database(context).workoutSessionDao())
    fun workouts(context: Context) = WorkoutModule.repository(context)
}
