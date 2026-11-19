package com.example.gymtrack.activeworkout.timer

import java.util.Locale

/**
 * Immutable, pure timer logic of the Active Workout screen (no Android / Compose code inside).
 *
 * The timer never "counts" anything. It only remembers two numbers:
 *  - [accumulatedMillis]: active time already collected before the current running segment
 *  - [runningSinceMillis]: wall-clock time at which the current segment started (null = paused)
 *
 * elapsed = accumulated + (now - runningSince)   -> so the value is ALWAYS correct, no matter how many
 * times the UI recomposes, the screen rotates, or how late the next tick arrives.
 */
data class WorkoutTimer(
    val accumulatedMillis: Long = 0L,
    val runningSinceMillis: Long? = null
) {
    val isRunning: Boolean get() = runningSinceMillis != null

    fun elapsedMillis(nowMillis: Long): Long {
        val runningPart = runningSinceMillis?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L
        return accumulatedMillis + runningPart
    }

    /** Starts the timer (first start) or resumes it after a pause. Does nothing if already running. */
    fun resume(nowMillis: Long): WorkoutTimer =
        if (isRunning) this else copy(runningSinceMillis = nowMillis)

    /** Freezes the elapsed time. Does nothing if already paused. */
    fun pause(nowMillis: Long): WorkoutTimer =
        if (!isRunning) this else copy(accumulatedMillis = elapsedMillis(nowMillis), runningSinceMillis = null)

    /** Back to 00:00, paused. */
    fun reset(): WorkoutTimer = WorkoutTimer()
}

/** MM:SS below one hour, HH:MM:SS from one hour on. Locale.US so digits are never localised. */
fun formatElapsedTime(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, secs)
    }
}
