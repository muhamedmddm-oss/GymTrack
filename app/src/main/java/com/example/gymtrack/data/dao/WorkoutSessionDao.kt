package com.example.gymtrack.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/** One performed exercise with all its sets. */
data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SessionSetEntity>
)

/** A full workout: the session, its exercises and their sets (the complete history record). */
data class SessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>
)

/** Input for [WorkoutSessionDao.insertSessionWithDetails]. */
data class SessionExerciseDraft(
    val exercise: SessionExerciseEntity,
    val sets: List<SessionSetEntity>
)

@Dao
abstract class WorkoutSessionDao {

    // ---- Sessions ----

    @Insert
    abstract suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    abstract suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun getSession(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    abstract fun observeAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE workoutDayId = :dayId ORDER BY startTime DESC")
    abstract fun observeSessionsForDay(dayId: Long): Flow<List<WorkoutSessionEntity>>

    /** Finished sessions that started in [startInclusive, endExclusive) – for Home/Statistics. */
    @Query(
        "SELECT * FROM workout_sessions WHERE endTime IS NOT NULL " +
            "AND startTime >= :startInclusive AND startTime < :endExclusive ORDER BY startTime DESC"
    )
    abstract fun observeFinishedSessionsBetween(startInclusive: Long, endExclusive: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC LIMIT 1")
    abstract fun observeLastFinishedSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE endTime IS NOT NULL")
    abstract fun observeFinishedSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE workoutDayId = :dayId")
    abstract suspend fun countForDay(dayId: Long): Int

    // ---- Active Workout (Person 4): resume, timer state, finishing ----

    /** The workout of [dayId] that was started but never finished (used to resume it). */
    @Query(
        "SELECT * FROM workout_sessions WHERE workoutDayId = :dayId AND endTime IS NULL " +
            "ORDER BY startTime DESC LIMIT 1"
    )
    abstract suspend fun getUnfinishedSessionForDay(dayId: Long): WorkoutSessionEntity?

    @Query(
        "UPDATE workout_sessions SET accumulatedMillis = :accumulatedMillis, " +
            "runningSinceMillis = :runningSinceMillis WHERE id = :sessionId"
    )
    abstract suspend fun updateTimerState(sessionId: Long, accumulatedMillis: Long, runningSinceMillis: Long?)

    /** Closes a session with the real (pause-aware) duration and stops its timer. */
    @Query(
        "UPDATE workout_sessions SET endTime = :endTime, durationSeconds = :durationSeconds, " +
            "accumulatedMillis = :accumulatedMillis, runningSinceMillis = NULL WHERE id = :sessionId"
    )
    abstract suspend fun markFinished(sessionId: Long, endTime: Long, durationSeconds: Long, accumulatedMillis: Long)

    // ---- Exercises and sets of a session ----

    @Insert
    abstract suspend fun insertSessionExercise(exercise: SessionExerciseEntity): Long

    @Insert
    abstract suspend fun insertSet(set: SessionSetEntity): Long

    @Insert
    abstract suspend fun insertSets(sets: List<SessionSetEntity>)

    @Update
    abstract suspend fun updateSet(set: SessionSetEntity)

    @Update
    abstract suspend fun updateSets(sets: List<SessionSetEntity>)

    @Query("DELETE FROM session_sets WHERE id = :setId")
    abstract suspend fun deleteSet(setId: Long)

    /** Removes one exercise from a session; its sets are removed by the CASCADE foreign key. */
    @Query("DELETE FROM session_exercises WHERE id = :sessionExerciseId")
    abstract suspend fun deleteSessionExercise(sessionExerciseId: Long)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex, id")
    abstract suspend fun getSessionExercises(sessionId: Long): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY setNumber")
    abstract suspend fun getSets(sessionExerciseId: Long): List<SessionSetEntity>

    // ---- Full history records ----

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    abstract fun observeSessionWithDetails(sessionId: Long): Flow<SessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun getSessionWithDetails(sessionId: Long): SessionWithDetails?

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    abstract fun observeAllSessionsWithDetails(): Flow<List<SessionWithDetails>>

    /** Saves a whole workout (session + exercises + sets) atomically. Returns the session id. */
    @Transaction
    open suspend fun insertSessionWithDetails(
        session: WorkoutSessionEntity,
        exercises: List<SessionExerciseDraft>
    ): Long {
        val sessionId = insertSession(session)
        exercises.forEach { draft ->
            val exerciseId = insertSessionExercise(draft.exercise.copy(sessionId = sessionId))
            insertSets(draft.sets.map { it.copy(sessionExerciseId = exerciseId) })
        }
        return sessionId
    }

    /** Inserts one exercise and its sets atomically. Returns the new session-exercise id. */
    @Transaction
    open suspend fun insertExerciseWithSets(exercise: SessionExerciseEntity, sets: List<SessionSetEntity>): Long {
        val exerciseId = insertSessionExercise(exercise)
        insertSets(sets.map { it.copy(sessionExerciseId = exerciseId) })
        return exerciseId
    }

    /** Adds several exercises (with their sets) to a running session in ONE transaction. */
    @Transaction
    open suspend fun addExercisesWithSets(sessionId: Long, exercises: List<SessionExerciseDraft>) {
        exercises.forEach { draft ->
            insertExerciseWithSets(draft.exercise.copy(sessionId = sessionId), draft.sets)
        }
    }
}
