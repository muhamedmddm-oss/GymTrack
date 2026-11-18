package com.example.gymtrack.data.repository

import com.example.gymtrack.data.dao.UserProfileDao
import com.example.gymtrack.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/** Access to the user profile (onboarding / profile screens). */
class UserProfileRepository(
    private val dao: UserProfileDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun observeProfile(): Flow<UserProfileEntity?> = dao.observeCurrent()

    suspend fun getProfile(): UserProfileEntity? = dao.getCurrent()

    /** Creates the profile on first save, afterwards updates it. Returns the profile id. */
    suspend fun saveProfile(
        name: String,
        heightCm: Double?,
        weightKg: Double?,
        trainingDaysPerWeek: Int,
        age: Int? = null
    ): Long {
        val now = clock()
        val current = dao.getCurrent()
        return if (current == null) {
            dao.insert(
                UserProfileEntity(
                    name = name.trim(),
                    heightCm = heightCm,
                    weightKg = weightKg,
                    trainingDaysPerWeek = trainingDaysPerWeek,
                    age = age,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            dao.update(
                current.copy(
                    name = name.trim(),
                    heightCm = heightCm,
                    weightKg = weightKg,
                    trainingDaysPerWeek = trainingDaysPerWeek,
                    age = age,
                    updatedAt = now
                )
            )
            current.id
        }
    }
}
