package com.example.gymtrack.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtrack.workout.data.entity.WorkoutDayEntity

/**
 * One performed workout (Person 4). History is preserved on purpose:
 *  - [dayName] / [planName] are copies, so the session still reads correctly after the plan is renamed or deleted.
 *  - FK is SET NULL: if the day row is ever removed, the session stays and only loses the link.
 * [endTime] is null while the workout is running; [durationSeconds] is filled when it finishes.
 */
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
    val durationSeconds: Long = 0
)
