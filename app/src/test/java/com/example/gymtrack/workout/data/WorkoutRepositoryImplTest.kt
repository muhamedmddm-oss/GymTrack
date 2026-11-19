package com.example.gymtrack.workout.data

import com.example.gymtrack.workout.WorkoutRepositoryImpl
import com.example.gymtrack.workout.Weekday
import com.example.gymtrack.workout.WorkoutDayInput
import com.example.gymtrack.workout.WorkoutDayUsageProvider
import com.example.gymtrack.workout.testutil.FakeWorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutRepositoryImplTest {

    /** Test double for Person 3/4: lets a test decide which days have exercises/history. */
    private class FakeUsage : WorkoutDayUsageProvider {
        val counts = MutableStateFlow<Map<Long, Int>>(emptyMap())
        val inUse = mutableSetOf<Long>()
        override fun observeExerciseCounts(): Flow<Map<Long, Int>> = counts
        override suspend fun isDayInUse(dayId: Long) = dayId in inUse
    }

    private val dao = FakeWorkoutDao()
    private val usage = FakeUsage()
    private var now = 1_000L
    private val repo = WorkoutRepositoryImpl(dao, usage) { now++ }

    private val ppl = listOf(
        WorkoutDayInput(name = "Push", weekday = Weekday.SATURDAY),
        WorkoutDayInput(name = "Pull", weekday = Weekday.MONDAY),
        WorkoutDayInput(name = "Legs", weekday = Weekday.WEDNESDAY)
    )

    @Test
    fun createPlan_storesPlanAndDaysInOrder() = runTest {
        val id = repo.createPlan("  My PPL Program ", ppl)
        val plan = repo.getPlan(id)!!
        assertEquals("My PPL Program", plan.name)
        assertEquals(listOf("Push", "Pull", "Legs"), plan.days.map { it.name })
        assertEquals(listOf(0, 1, 2), plan.days.map { it.orderIndex })
        assertEquals(Weekday.SATURDAY, plan.days.first().weekday)
    }

    @Test
    fun createPlan_withFiveDays_works() = runTest {
        val five = Weekday.entries.take(5).mapIndexed { i, w -> WorkoutDayInput(name = "Day $i", weekday = w) }
        val id = repo.createPlan("5 day", five)
        assertEquals(5, repo.getPlan(id)!!.days.size)
    }

    @Test
    fun createPlan_rejectsInvalidInput() = runTest {
        assertThrows(IllegalArgumentException::class.java) { runBlockingCreate("", ppl) }
        assertThrows(IllegalArgumentException::class.java) { runBlockingCreate("Plan", emptyList()) }
        val dup = listOf(ppl[0], ppl[1].copy(weekday = Weekday.SATURDAY))
        assertThrows(IllegalArgumentException::class.java) { runBlockingCreate("Plan", dup) }
        val eight = List(8) { WorkoutDayInput(name = "d$it", weekday = Weekday.entries[it % 7]) }
        assertThrows(IllegalArgumentException::class.java) { runBlockingCreate("Plan", eight) }
    }

    private fun runBlockingCreate(name: String, days: List<WorkoutDayInput>) =
        kotlinx.coroutines.runBlocking { repo.createPlan(name, days) }

    @Test
    fun observePlans_isNewestFirst_andShowsExerciseCounts() = runTest {
        val first = repo.createPlan("First", ppl)
        val second = repo.createPlan("Second", ppl)
        val pushId = repo.getPlan(first)!!.days.first().id
        usage.counts.value = mapOf(pushId to 4)

        val plans = repo.observePlans().first()
        assertEquals(listOf(second, first), plans.map { it.id })
        assertEquals(4, plans.last().days.first().exerciseCount)
        assertEquals(0, plans.first().days.first().exerciseCount)
    }

    @Test
    fun updatePlan_renamesUpdatesInsertsAndDeletes() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val saved = repo.getPlan(id)!!.days

        repo.updatePlan(
            id, "PPL v2",
            listOf(
                WorkoutDayInput(saved[0].id, "Push Heavy", Weekday.SUNDAY),   // renamed + moved
                WorkoutDayInput(saved[1].id, "Pull", Weekday.MONDAY),         // unchanged
                // Legs removed
                WorkoutDayInput(0, "Arms", Weekday.FRIDAY)                    // new
            )
        )

        val plan = repo.getPlan(id)!!
        assertEquals("PPL v2", plan.name)
        assertEquals(listOf("Push Heavy", "Pull", "Arms"), plan.days.map { it.name })
        assertEquals(saved[0].id, plan.days[0].id)          // id preserved
        assertEquals(Weekday.SUNDAY, plan.days[0].weekday)
        assertFalse(plan.days.any { it.name == "Legs" })
        assertEquals(3, dao.days.value.size)                // Legs physically deleted (unused)
    }

    @Test
    fun updatePlan_archivesRemovedDayThatIsInUse_insteadOfDeleting() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val saved = repo.getPlan(id)!!.days
        usage.inUse += saved[2].id                           // Legs has exercises/history

        repo.updatePlan(id, "PPL", listOf(
            WorkoutDayInput(saved[0].id, "Push", Weekday.SATURDAY),
            WorkoutDayInput(saved[1].id, "Pull", Weekday.MONDAY)
        ))

        assertEquals(2, repo.getPlan(id)!!.days.size)         // hidden from the schedule…
        val legsRow = dao.days.value.single { it.id == saved[2].id }
        assertTrue(legsRow.isArchived)                        // …but the row (history) still exists
    }

    @Test
    fun deleteDay_unused_isRemoved_inUse_isArchived() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val saved = repo.getPlan(id)!!.days
        usage.inUse += saved[0].id

        repo.deleteDay(saved[0].id)
        repo.deleteDay(saved[1].id)

        assertTrue(dao.days.value.single { it.id == saved[0].id }.isArchived)
        assertTrue(dao.days.value.none { it.id == saved[1].id })
        assertEquals(listOf("Legs"), repo.getPlan(id)!!.days.map { it.name })
    }

    @Test
    fun deletePlan_unused_isRemovedCompletely() = runTest {
        val id = repo.createPlan("PPL", ppl)
        repo.deletePlan(id)
        assertNull(repo.getPlan(id))
        assertTrue(dao.plans.value.isEmpty())
        assertTrue(dao.days.value.isEmpty())
        assertTrue(repo.observePlans().first().isEmpty())
    }

    @Test
    fun deletePlan_withHistory_isArchivedSoHistorySurvives() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val saved = repo.getPlan(id)!!.days
        usage.inUse += saved[1].id

        repo.deletePlan(id)

        assertTrue(repo.observePlans().first().isEmpty())     // gone from the UI
        assertEquals(1, dao.plans.value.size)                 // still in the database
        assertTrue(dao.plans.value.single().isArchived)
        assertEquals(3, dao.days.value.size)
    }

    @Test
    fun deletePlan_remembersEarlierArchivedDays() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val saved = repo.getPlan(id)!!.days
        usage.inUse += saved[0].id
        repo.deleteDay(saved[0].id)                           // archived earlier

        repo.deletePlan(id)                                   // must NOT cascade-delete the archived day

        assertTrue(dao.days.value.any { it.id == saved[0].id })
    }

    @Test
    fun addDay_appendsAtTheEnd_andRejectsDuplicateWeekday() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val newId = repo.addDay(id, WorkoutDayInput(name = "Arms", weekday = Weekday.FRIDAY))
        val plan = repo.getPlan(id)!!
        assertEquals(3, plan.days.indexOfFirst { it.id == newId })
        assertEquals(3, plan.days.last().orderIndex)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.addDay(id, WorkoutDayInput(name = "Dup", weekday = Weekday.FRIDAY))
            }
        }
    }

    @Test
    fun updateDay_changesNameAndWeekday() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val push = repo.getPlan(id)!!.days.first()
        repo.updateDay(push.id, "Chest Day", Weekday.TUESDAY)
        val updated = repo.getPlan(id)!!.days.first { it.id == push.id }
        assertEquals("Chest Day", updated.name)
        assertEquals(Weekday.TUESDAY, updated.weekday)
    }

    @Test
    fun observeDay_returnsDayWithPlanName_andNullAfterDelete() = runTest {
        val id = repo.createPlan("PPL", ppl)
        val push = repo.getPlan(id)!!.days.first()

        val details = repo.observeDay(push.id).first()
        assertNotNull(details)
        assertEquals("PPL", details!!.planName)
        assertEquals("Push", details.day.name)

        repo.deleteDay(push.id)
        assertNull(repo.observeDay(push.id).first())
    }
}
