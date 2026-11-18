package com.example.gymtrack.workout.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One training day of a plan. Deleting a plan row cascades to its days, but the repository only
 * hard-deletes plans/days that nothing else references; otherwise it sets [isArchived].
 * [weekday] stores the [com.example.gymtrack.workout.domain.model.Weekday] enum name.
 */
@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val weekday: String,
    val orderIndex: Int,
    val isArchived: Boolean = false
)
