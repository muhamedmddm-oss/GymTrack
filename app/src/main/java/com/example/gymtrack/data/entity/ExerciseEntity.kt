package com.example.gymtrack.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtrack.workout.WorkoutDayEntity


@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("workoutDayId")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutDayId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val orderIndex: Int = 0,
    val notes: String? = null
)
