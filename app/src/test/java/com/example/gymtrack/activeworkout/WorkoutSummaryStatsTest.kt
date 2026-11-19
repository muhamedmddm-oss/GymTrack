package com.example.gymtrack.activeworkout

import com.example.gymtrack.activeworkout.presentation.buildSummaryStats
import com.example.gymtrack.data.dao.SessionExerciseWithSets
import com.example.gymtrack.data.dao.SessionWithDetails
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSummaryStatsTest {

    private fun set(n: Int, reps: Int, kg: Double?, done: Boolean) =
        SessionSetEntity(sessionExerciseId = 1, setNumber = n, reps = reps, weightKg = kg, isCompleted = done)

    @Test
    fun stats_countOnlyCompletedSets_forVolume() {
        val details = SessionWithDetails(
            session = WorkoutSessionEntity(workoutDayId = 1, dayName = "Push", planName = "PPL", startTime = 0),
            exercises = listOf(
                SessionExerciseWithSets(
                    SessionExerciseEntity(id = 1, sessionId = 1, exerciseId = null, exerciseName = "Bench"),
                    listOf(set(1, 10, 50.0, true), set(2, 8, 60.0, true))
                ),
                SessionExerciseWithSets(
                    SessionExerciseEntity(id = 2, sessionId = 1, exerciseId = null, exerciseName = "Dips"),
                    listOf(set(1, 12, null, true), set(2, 10, null, false))
                )
            )
        )
        val stats = buildSummaryStats(details)
        assertEquals(2, stats.totalExercises)
        assertEquals(1, stats.completedExercises)
        assertEquals(4, stats.totalSets)
        assertEquals(3, stats.completedSets)
        assertEquals(10 * 50.0 + 8 * 60.0, stats.totalVolumeKg, 0.0)
    }
}
