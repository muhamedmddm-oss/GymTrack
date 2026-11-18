package com.example.gymtrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gymtrack.data.dao.ExerciseDao
import com.example.gymtrack.data.dao.UserProfileDao
import com.example.gymtrack.data.dao.WorkoutSessionDao
import com.example.gymtrack.data.entity.ExerciseEntity
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.entity.UserProfileEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import com.example.gymtrack.data.migration.MIGRATION_1_2
import com.example.gymtrack.data.migration.MIGRATION_2_3
import com.example.gymtrack.workout.data.dao.WorkoutDao
import com.example.gymtrack.workout.data.entity.WorkoutDayEntity
import com.example.gymtrack.workout.data.entity.WorkoutPlanEntity

/**
 * The ONE Room database of GymTrack (Person 7). Never create another instance:
 * always use [getInstance] (or the repositories in [GymTrackData]).
 *
 * Tables: user_profile, workout_plans, workout_days, exercises,
 *         workout_sessions, session_exercises, session_sets.
 *
 * Version history:
 *  1 - temporary Person 2 database (workout_plans, workout_days)
 *  2 - real database: + user_profile, exercises, workout_sessions, session_exercises,
 *      session_sets, and workout_plans.userId  (see [MIGRATION_1_2])
 *  3 - user_profile.age (see [MIGRATION_2_3])
 *
 * When you change the schema: bump [version], add a Migration in data/migration/Migrations.kt and
 * register it in [getInstance]. Never use fallbackToDestructiveMigration().
 */
@Database(
    entities = [
        UserProfileEntity::class,
        WorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class GymTrackDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        const val DATABASE_NAME = "gymtrack.db"

        @Volatile
        private var instance: GymTrackDatabase? = null

        fun getInstance(context: Context): GymTrackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymTrackDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
