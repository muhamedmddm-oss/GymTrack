package com.example.gymtrack.statistics

import com.example.gymtrack.data.dao.SessionWithDetails
import java.util.Calendar

enum class StatisticsRange(val label: String, val days: Int?) {
    DAYS_7("7 days", 7), DAYS_30("30 days", 30), MONTHS_3("3 months", 90), ALL("All time", null)
}

data class WorkoutStatistics(
    val sessions: Int = 0,
    val totalExercises: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalDurationMinutes: Long = 0,
    val averageDurationMinutes: Long = 0,
    val weeklySessions: List<Int> = emptyList(),
    val volumeByWeek: List<Double> = emptyList(),
    val topExercises: List<Pair<String, Int>> = emptyList(),
    val bestLiftKg: Double? = null,
    val lastSessionAt: Long? = null
)

fun calculateWorkoutStatistics(
    sessions: List<SessionWithDetails>,
    range: StatisticsRange,
    now: Long = System.currentTimeMillis()
): WorkoutStatistics {
    val start = range.days?.let { now - it * 24L * 60L * 60L * 1000L }
    val completed = sessions.filter { it.session.endTime != null && (start == null || it.session.startTime >= start) }
    val allExercises = completed.flatMap { it.exercises }
    val completedSets = allExercises.flatMap { it.sets }.filter { it.isCompleted }
    val durations = completed.map { it.session.durationSeconds.coerceAtLeast(0) }
    val weekBuckets = MutableList(8) { 0 }
    val volumeBuckets = MutableList(8) { 0.0 }
    val weekMillis = 7L * 24L * 60L * 60L * 1000L
    completed.forEach { session ->
        val bucket = ((now - session.session.startTime).coerceAtLeast(0) / weekMillis).toInt().coerceIn(0, 7)
        weekBuckets[7 - bucket]++
        volumeBuckets[7 - bucket] += session.exercises.flatMap { it.sets }
            .filter { it.isCompleted }.sumOf { it.reps * (it.weightKg ?: 0.0) }
    }
    val top = allExercises.groupingBy { it.exercise.exerciseName }.eachCount()
        .entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
    return WorkoutStatistics(
        sessions = completed.size,
        totalExercises = allExercises.size,
        totalSets = completedSets.size,
        totalReps = completedSets.sumOf { it.reps },
        totalVolumeKg = completedSets.sumOf { it.reps * (it.weightKg ?: 0.0) },
        totalDurationMinutes = durations.sum() / 60,
        averageDurationMinutes = if (durations.isEmpty()) 0 else durations.average().toLong() / 60,
        weeklySessions = weekBuckets,
        volumeByWeek = volumeBuckets,
        topExercises = top,
        bestLiftKg = completedSets.mapNotNull { it.weightKg }.maxOrNull(),
        lastSessionAt = completed.maxByOrNull { it.session.startTime }?.session?.startTime
    )
}
