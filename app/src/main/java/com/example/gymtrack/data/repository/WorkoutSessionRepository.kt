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
    suspend fun startSession(dayId: Long?, dayName: String, planName: String): Long =
        dao.insertSession(
            WorkoutSessionEntity(workoutDayId = dayId, dayName = dayName, planName = planName, startTime = clock())
        )

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
