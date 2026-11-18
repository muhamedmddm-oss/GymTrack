package com.example.gymtrack.workout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.example.gymtrack.data.GymTrackDatabase
import com.example.gymtrack.data.RoomWorkoutDayUsageProvider
import com.example.gymtrack.workout.data.repository.WorkoutRepositoryImpl
import com.example.gymtrack.workout.domain.repository.WorkoutDayUsageProvider
import com.example.gymtrack.workout.domain.repository.WorkoutRepository

/**
 * Tiny manual "dependency container" for the module (the project has no Hilt/Koin).
 *
 * By default the repository uses [RoomWorkoutDayUsageProvider] (exercise counts + "has exercises or
 * sessions" from the shared database), so nothing needs to be configured. [usageProvider] is only an
 * optional override; if used, set it ONCE before any workout screen opens.
 *
 * Person 5/6/others: call [repository] to read plans/days (it is a singleton).
 */
object WorkoutModule {

    @Volatile
    var usageProvider: WorkoutDayUsageProvider? = null

    @Volatile
    private var repositoryInstance: WorkoutRepository? = null

    fun repository(context: Context): WorkoutRepository =
        repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: run {
                val db = GymTrackDatabase.getInstance(context)
                WorkoutRepositoryImpl(
                    dao = db.workoutDao(),
                    usage = usageProvider ?: RoomWorkoutDayUsageProvider(db.exerciseDao(), db.workoutSessionDao())
                )
            }.also { repositoryInstance = it }
        }

    /** Builds a ViewModelProvider.Factory for one ViewModel using the shared repository. */
    inline fun <reified VM : ViewModel> factory(
        crossinline create: () -> VM
    ): ViewModelProvider.Factory = viewModelFactory { initializer { create() } }
}
