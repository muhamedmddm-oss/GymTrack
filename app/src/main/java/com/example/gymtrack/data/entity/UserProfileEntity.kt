package com.example.gymtrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The app user. GymTrack is a single-user app, so normally there is exactly one row.
 * Height/weight are nullable so onboarding can save a partially filled profile.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val trainingDaysPerWeek: Int,
    val createdAt: Long,
    val updatedAt: Long,
    /** Added in database v3 (from the onboarding screen). */
    val age: Int? = null
)
