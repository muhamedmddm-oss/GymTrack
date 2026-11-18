package com.example.gymtrack.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.gymtrack.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert
    suspend fun insert(profile: UserProfileEntity): Long

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Delete
    suspend fun delete(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getById(id: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun observeById(id: Long): Flow<UserProfileEntity?>

    /** The (single) current user = the first profile row, or null before onboarding finished. */
    @Query("SELECT * FROM user_profile ORDER BY id LIMIT 1")
    suspend fun getCurrent(): UserProfileEntity?

    @Query("SELECT * FROM user_profile ORDER BY id LIMIT 1")
    fun observeCurrent(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY id")
    fun observeAll(): Flow<List<UserProfileEntity>>

    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun count(): Int
}
