package com.example.gymtrack.statistics

import com.example.gymtrack.data.dao.SessionExerciseWithSets
import com.example.gymtrack.data.dao.SessionWithDetails
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutStatisticsTest {
    @Test fun completedSessionsOnlyContributeAndVolumeIsRepsTimesWeight() {
        val finished = SessionWithDetails(
            session = WorkoutSessionEntity(
                id = 1, workoutDayId = 2, dayName = "Push", planName = "PPL",
                startTime = 1_700_000_000_000, endTime = 1_700_000_300_000, durationSeconds = 300
            ),
            exercises = listOf(
                SessionExerciseWithSets(
                    exercise = SessionExerciseEntity(id = 3, sessionId = 1, exerciseId = null, exerciseName = "Bench"),
                    sets = listOf(
                        SessionSetEntity(id = 4, sessionExerciseId = 3, setNumber = 1, reps = 10, weightKg = 50.0, isCompleted = true),
                        SessionSetEntity(id = 5, sessionExerciseId = 3, setNumber = 2, reps = 8, weightKg = 50.0, isCompleted = false)
                    )
                )
            )
        )
        val running = finished.copy(session = finished.session.copy(id = 2, endTime = null))
        val stats = calculateWorkoutStatistics(listOf(finished, running), StatisticsRange.ALL, now = 1_700_000_400_000)
        assertEquals(1, stats.sessions)
        assertEquals(1, stats.totalSets)
        assertEquals(10, stats.totalReps)
        assertEquals(500.0, stats.totalVolumeKg, 0.001)
        assertEquals(5, stats.totalDurationMinutes)
        assertEquals("Bench", stats.topExercises.single().first)
    }

    @Test fun emptyHistoryReturnsZeroes() {
        val stats = calculateWorkoutStatistics(emptyList(), StatisticsRange.ALL)
        assertEquals(0, stats.sessions)
        assertEquals(0.0, stats.totalVolumeKg, 0.001)
        assertNull(stats.lastSessionAt)
    }
}
