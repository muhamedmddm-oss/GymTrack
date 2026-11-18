package com.example.gymtrack.workout.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtrack.data.entity.UserProfileEntity

/**
 * A workout plan. [isArchived] plans are hidden from the UI but kept in the database because
 * exercises/sessions still reference their days (history is never silently deleted).
 *
 * [userId] (added in database v2) links the plan to its owner. It is nullable and SET NULL so
 * plans created before profiles existed, or after a profile is removed, keep working.
 */
@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId")]
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean = false,
    val userId: Long? = null
)
