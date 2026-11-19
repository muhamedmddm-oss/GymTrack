package com.example.gymtrack.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtrack.workout.WorkoutDayEntity


@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("workoutDayId"), Index("startTime")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutDayId: Long?,
    val dayName: String,
    val planName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val durationSeconds: Long = 0,

    @ColumnInfo(defaultValue = "0") val accumulatedMillis: Long = 0,
    /** Wall-clock time at which the timer was last started/resumed; null while paused (v4). */
    val runningSinceMillis: Long? = null
)
