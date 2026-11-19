package com.example.gymtrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val trainingDaysPerWeek: Int,
    val createdAt: Long,
    val updatedAt: Long,

    val age: Int? = null
)
