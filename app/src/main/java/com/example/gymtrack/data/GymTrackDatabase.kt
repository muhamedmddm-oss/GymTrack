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
import com.example.gymtrack.data.migration.MIGRATION_3_4
import com.example.gymtrack.workout.WorkoutDao
import com.example.gymtrack.workout.WorkoutDayEntity
import com.example.gymtrack.workout.WorkoutPlanEntity


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
    version = 4,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
