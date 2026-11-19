package com.example.gymtrack.data.repository

import com.example.gymtrack.data.dao.SessionExerciseDraft
import com.example.gymtrack.data.dao.SessionWithDetails
import com.example.gymtrack.data.dao.WorkoutSessionDao
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/** Workout sessions and their history (Person 4 writes, Persons 5 and 6 read). */
class WorkoutSessionRepository(
    private val dao: WorkoutSessionDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    // ---- Running a workout ----

    /** Starts a session. [dayName]/[planName] are copied so history stays readable later. */
    suspend fun startSession(
        dayId: Long?,
        dayName: String,
        planName: String,
        startRunning: Boolean = false
    ): Long {
        val now = clock()
        return dao.insertSession(
            WorkoutSessionEntity(
                workoutDayId = dayId,
                dayName = dayName,
                planName = planName,
                startTime = now,
                // Active Workout: the timer starts together with the session.
                runningSinceMillis = if (startRunning) now else null
            )
        )
    }

    /** The unfinished workout of a day (so the Active Workout screen can resume it), or null. */
    suspend fun getUnfinishedSessionForDay(dayId: Long): WorkoutSessionEntity? =
        dao.getUnfinishedSessionForDay(dayId)

    suspend fun getSessionDetails(sessionId: Long): SessionWithDetails? = dao.getSessionWithDetails(sessionId)

    /** Stores the timer so the elapsed time survives rotation, process death and closing the app. */
    suspend fun saveTimerState(sessionId: Long, accumulatedMillis: Long, runningSinceMillis: Long?) =
        dao.updateTimerState(sessionId, accumulatedMillis, runningSinceMillis)

    /** Adds the planned exercises of a day (with their empty sets) to a session in one transaction. */
    suspend fun addExercisesWithSets(sessionId: Long, exercises: List<SessionExerciseDraft>) =
        dao.addExercisesWithSets(sessionId, exercises)

    /** Adds one exercise with [setCount] empty sets. Returns the new session-exercise id. */
    suspend fun addExerciseWithSets(
        sessionId: Long,
        exerciseId: Long?,
        exerciseName: String,
        orderIndex: Int,
        sets: List<SessionSetEntity>
    ): Long = dao.insertExerciseWithSets(
        SessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            orderIndex = orderIndex
        ),
        sets
    )

    suspend fun getSets(sessionExerciseId: Long): List<SessionSetEntity> = dao.getSets(sessionExerciseId)

    suspend fun updateSets(sets: List<SessionSetEntity>) = dao.updateSets(sets)

    suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    /** Removes an exercise (and, through CASCADE, its sets) from a session. */
    suspend fun removeExerciseFromSession(sessionExerciseId: Long) = dao.deleteSessionExercise(sessionExerciseId)

    /**
     * Ends the session with the pause-aware duration measured by the Active Workout timer
     * (unlike [finishSession], which would count pauses as workout time).
     */
    suspend fun finishSessionWithDuration(sessionId: Long, durationMillis: Long) {
        val safeMillis = durationMillis.coerceAtLeast(0)
        dao.markFinished(
            sessionId = sessionId,
            endTime = clock(),
            durationSeconds = safeMillis / 1000,
            accumulatedMillis = safeMillis
        )
    }

    /** Adds an exercise to a running session. Returns the session-exercise id. */
    suspend fun addExerciseToSession(
        sessionId: Long,
        exerciseId: Long?,
        exerciseName: String,
        orderIndex: Int
    ): Long = dao.insertSessionExercise(
        SessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            orderIndex = orderIndex
        )
    )

    suspend fun logSet(
        sessionExerciseId: Long,
        setNumber: Int,
        reps: Int,
        weightKg: Double?,
        completed: Boolean = true
    ): Long = dao.insertSet(
        SessionSetEntity(
            sessionExerciseId = sessionExerciseId,
            setNumber = setNumber,
            reps = reps,
            weightKg = weightKg,
            isCompleted = completed,
            completedAt = if (completed) clock() else null
        )
    )

    suspend fun updateSet(set: SessionSetEntity) = dao.updateSet(set)

    /** Ends the session and stores its duration. No-op if the session does not exist. */
    suspend fun finishSession(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        val end = clock()
        dao.updateSession(
            session.copy(endTime = end, durationSeconds = ((end - session.startTime) / 1000).coerceAtLeast(0))
        )
    }

    /** Saves an already-finished workout (session + exercises + sets) in one transaction. */
    suspend fun saveCompletedSession(
        session: WorkoutSessionEntity,
        exercises: List<SessionExerciseDraft>
    ): Long = dao.insertSessionWithDetails(session, exercises)

    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    // ---- Reading history ----

    fun observeAllSessions(): Flow<List<WorkoutSessionEntity>> = dao.observeAllSessions()
    fun observeSessionsForDay(dayId: Long): Flow<List<WorkoutSessionEntity>> = dao.observeSessionsForDay(dayId)
    fun observeSessionDetails(sessionId: Long): Flow<SessionWithDetails?> = dao.observeSessionWithDetails(sessionId)
    fun observeAllSessionDetails(): Flow<List<SessionWithDetails>> = dao.observeAllSessionsWithDetails()
    fun observeLastFinishedSession(): Flow<WorkoutSessionEntity?> = dao.observeLastFinishedSession()
    fun observeFinishedSessionCount(): Flow<Int> = dao.observeFinishedSessionCount()
    fun observeFinishedSessionsBetween(startInclusive: Long, endExclusive: Long): Flow<List<WorkoutSessionEntity>> =
        dao.observeFinishedSessionsBetween(startInclusive, endExclusive)
}
