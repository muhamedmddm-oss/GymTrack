package com.example.gymtrack.activeworkout

import com.example.gymtrack.activeworkout.timer.WorkoutTimer
import com.example.gymtrack.activeworkout.timer.formatElapsedTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTimerTest {

    @Test
    fun newTimer_isPausedAtZero() {
        val timer = WorkoutTimer()
        assertFalse(timer.isRunning)
        assertEquals(0L, timer.elapsedMillis(10_000))
    }

    @Test
    fun runningTimer_countsFromStartTime() {
        val timer = WorkoutTimer().resume(1_000)
        assertTrue(timer.isRunning)
        assertEquals(5_000L, timer.elapsedMillis(6_000))
    }

    @Test
    fun pause_freezesElapsedTime() {
        val paused = WorkoutTimer().resume(0).pause(4_000)
        assertEquals(4_000L, paused.elapsedMillis(4_000))
        assertEquals(4_000L, paused.elapsedMillis(60_000)) // time passes, value stays
    }

    @Test
    fun resume_keepsTimeFromBeforeThePause() {
        val timer = WorkoutTimer().resume(0).pause(4_000).resume(10_000)
        assertEquals(7_000L, timer.elapsedMillis(13_000)) // 4s + 3s
    }

    @Test
    fun resumeWhileRunning_andPauseWhilePaused_changeNothing() {
        val running = WorkoutTimer().resume(1_000)
        assertEquals(running, running.resume(9_000))
        val paused = running.pause(2_000)
        assertEquals(paused, paused.pause(9_000))
    }

    @Test
    fun reset_goesBackToZeroPaused() {
        val reset = WorkoutTimer().resume(0).pause(4_000).reset()
        assertEquals(WorkoutTimer(), reset)
    }

    @Test
    fun clockGoingBackwards_neverGivesNegativeTime() {
        assertEquals(0L, WorkoutTimer().resume(5_000).elapsedMillis(1_000))
    }

    @Test
    fun format_usesMmSsThenHhMmSs() {
        assertEquals("00:00", formatElapsedTime(0))
        assertEquals("01:05", formatElapsedTime(65))
        assertEquals("59:59", formatElapsedTime(3_599))
        assertEquals("01:00:00", formatElapsedTime(3_600))
        assertEquals("02:03:04", formatElapsedTime(7_384))
        assertEquals("00:00", formatElapsedTime(-5))
    }
}
