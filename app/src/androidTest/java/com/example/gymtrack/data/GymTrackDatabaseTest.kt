package com.example.gymtrack.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymtrack.data.dao.SessionExerciseDraft
import com.example.gymtrack.data.entity.ExerciseEntity
import com.example.gymtrack.data.entity.SessionExerciseEntity
import com.example.gymtrack.data.entity.SessionSetEntity
import com.example.gymtrack.data.entity.UserProfileEntity
import com.example.gymtrack.data.entity.WorkoutSessionEntity
import com.example.gymtrack.data.repository.UserProfileRepository
import com.example.gymtrack.workout.data.entity.WorkoutDayEntity
import com.example.gymtrack.workout.data.entity.WorkoutPlanEntity
import com.example.gymtrack.workout.data.repository.WorkoutRepositoryImpl
import com.example.gymtrack.workout.domain.model.Weekday
import com.example.gymtrack.workout.domain.model.WorkoutDayInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-SQLite tests of the shared database (needs an emulator/device):
 * ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class GymTrackDatabaseTest {

    private lateinit var db: GymTrackDatabase
    private lateinit var workouts: WorkoutRepositoryImpl

    private val ppl = listOf(
        WorkoutDayInput(name = "Push", weekday = Weekday.SATURDAY),
        WorkoutDayInput(name = "Pull", weekday = Weekday.MONDAY),
        WorkoutDayInput(name = "Legs", weekday = Weekday.WEDNESDAY)
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymTrackDatabase::class.java
        ).allowMainThreadQueries().build()
        workouts = WorkoutRepositoryImpl(
            dao = db.workoutDao(),
            usage = RoomWorkoutDayUsageProvider(db.exerciseDao(), db.workoutSessionDao())
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun createPlanWithDays() = workouts.createPlan("PPL", ppl).let { workouts.getPlan(it)!! }

    // 1 + 2: user profile
    @Test
    fun insertAndRetrieveUserProfile() = runBlocking {
        val repo = UserProfileRepository(db.userProfileDao()) { 5_000L }
        val id = repo.saveProfile("Mohamed", 180.0, 80.0, 3)

        val profile = db.userProfileDao().getById(id)!!
        assertEquals("Mohamed", profile.name)
        assertEquals(180.0, profile.heightCm!!, 0.0)
        assertEquals(3, profile.trainingDaysPerWeek)
        assertEquals(id, repo.observeProfile().first()!!.id)

        repo.saveProfile("Mohamed A.", 181.0, 79.0, 4)           // second save updates, no new row
        assertEquals(1, db.userProfileDao().count())
        assertEquals(4, repo.getProfile()!!.trainingDaysPerWeek)
    }

    // 3 + 4 + 5: plan with several days
    @Test
    fun createPlan_addDays_retrieveDaysOfPlan() = runBlocking {
        val plan = createPlanWithDays()
        assertEquals(listOf("Push", "Pull", "Legs"), plan.days.map { it.name })
        assertEquals(3, db.workoutDao().getDays(plan.id).size)
    }

    @Test
    fun plan_belongsToUser_andSurvivesUserDeletion() = runBlocking {
        val now = 1L
        val userId = db.userProfileDao().insert(UserProfileEntity(name = "A", trainingDaysPerWeek = 3, createdAt = now, updatedAt = now))
        val planId = db.workoutDao().insertPlan(WorkoutPlanEntity(name = "P", createdAt = now, userId = userId))
        assertEquals(userId, db.workoutDao().getPlan(planId)!!.userId)

        db.userProfileDao().delete(db.userProfileDao().getById(userId)!!)
        assertNull(db.workoutDao().getPlan(planId)!!.userId)       // plan kept, link cleared
    }

    // 6: exercises on a day
    @Test
    fun addExercisesToDay_andObserveCounts() = runBlocking {
        val plan = createPlanWithDays()
        val push = plan.days.first()
        db.exerciseDao().insert(ExerciseEntity(workoutDayId = push.id, name = "Bench Press", sets = 4, reps = 8, weightKg = 60.0))
        db.exerciseDao().insert(ExerciseEntity(workoutDayId = push.id, name = "Overhead Press", sets = 3, reps = 10, orderIndex = 1))

        assertEquals(2, db.exerciseDao().getForDay(push.id).size)
        assertEquals(mapOf(push.id to 2), RoomWorkoutDayUsageProvider(db.exerciseDao(), db.workoutSessionDao()).observeExerciseCounts().first())
        assertEquals(2, workouts.getPlan(plan.id)!!.days.first().exerciseCount)
    }

    // 7: updating a plan keeps exercises attached
    @Test
    fun updatePlan_keepsExistingDayIds_soExercisesStayAttached() = runBlocking {
        val plan = createPlanWithDays()
        val push = plan.days.first()
        db.exerciseDao().insert(ExerciseEntity(workoutDayId = push.id, name = "Bench", sets = 3, reps = 10))

        workouts.updatePlan(
            plan.id, "PPL v2",
            listOf(
                WorkoutDayInput(push.id, "Push Heavy", Weekday.SUNDAY),
                WorkoutDayInput(plan.days[1].id, "Pull", Weekday.MONDAY),
                WorkoutDayInput(plan.days[2].id, "Legs", Weekday.WEDNESDAY)
            )
        )
        assertEquals("PPL v2", workouts.getPlan(plan.id)!!.name)
        assertEquals(1, db.exerciseDao().getForDay(push.id).size)
    }

    // 8: deleting a day
    @Test
    fun deleteDay_unusedIsRemoved_withExercisesIsArchived() = runBlocking {
        val plan = createPlanWithDays()
        val (push, pull, _) = plan.days
        db.exerciseDao().insert(ExerciseEntity(workoutDayId = push.id, name = "Bench", sets = 3, reps = 10))

        workouts.deleteDay(pull.id)
        workouts.deleteDay(push.id)

        assertNull(db.workoutDao().getDay(pull.id))                         // really gone
        assertNull(db.workoutDao().getDay(push.id))                         // hidden (archived)…
        assertEquals(1, db.exerciseDao().getForDay(push.id).size)           // …but exercise still valid
        assertEquals(listOf("Legs"), workouts.getPlan(plan.id)!!.days.map { it.name })
    }

    // 9: history preserved
    @Test
    fun deletingPlan_keepsSessionsAndTheirHistory() = runBlocking {
        val plan = createPlanWithDays()
        val push = plan.days.first()
        val sessionId = db.workoutSessionDao().insertSessionWithDetails(
            WorkoutSessionEntity(workoutDayId = push.id, dayName = "Push", planName = "PPL", startTime = 1_000, endTime = 4_600_000, durationSeconds = 4_599),
            listOf(
                SessionExerciseDraft(
                    SessionExerciseEntity(sessionId = 0, exerciseId = null, exerciseName = "Bench", orderIndex = 0),
                    listOf(SessionSetEntity(sessionExerciseId = 0, setNumber = 1, reps = 8, weightKg = 60.0, isCompleted = true))
                )
            )
        )

        workouts.deletePlan(plan.id)                                       // plan has history -> archived

        assertTrue(workouts.observePlans().first().isEmpty())              // gone from the UI
        val saved = db.workoutSessionDao().getSessionWithDetails(sessionId)!!
        assertEquals("Push", saved.session.dayName)
        assertEquals("Bench", saved.exercises.single().exercise.exerciseName)
        assertEquals(60.0, saved.exercises.single().sets.single().weightKg!!, 0.0)
    }

    @Test
    fun physicallyRemovingADay_keepsItsSessions_withNullLink() = runBlocking {
        val plan = createPlanWithDays()
        val legs = plan.days.last()
        val sessionId = db.workoutSessionDao().insertSession(
            WorkoutSessionEntity(workoutDayId = legs.id, dayName = "Legs", planName = "PPL", startTime = 1)
        )
        db.workoutDao().deleteDayById(legs.id)

        val session = db.workoutSessionDao().getSession(sessionId)!!
        assertNull(session.workoutDayId)
        assertEquals("Legs", session.dayName)
    }

    @Test
    fun deletingAnExercise_doesNotChangePastSessions() = runBlocking {
        val plan = createPlanWithDays()
        val dayId = plan.days.first().id
        val exerciseId = db.exerciseDao().insert(ExerciseEntity(workoutDayId = dayId, name = "Squat", sets = 3, reps = 5))
        val sessionId = db.workoutSessionDao().insertSessionWithDetails(
            WorkoutSessionEntity(workoutDayId = dayId, dayName = "Push", planName = "PPL", startTime = 1),
            listOf(SessionExerciseDraft(SessionExerciseEntity(sessionId = 0, exerciseId = exerciseId, exerciseName = "Squat"), emptyList()))
        )

        db.exerciseDao().deleteById(exerciseId)

        val ex = db.workoutSessionDao().getSessionExercises(sessionId).single()
        assertNull(ex.exerciseId)
        assertEquals("Squat", ex.exerciseName)
    }

    @Test
    fun deletingSession_removesItsExercisesAndSets() = runBlocking {
        val sessionId = db.workoutSessionDao().insertSessionWithDetails(
            WorkoutSessionEntity(workoutDayId = null, dayName = "X", planName = "Y", startTime = 1),
            listOf(SessionExerciseDraft(
                SessionExerciseEntity(sessionId = 0, exerciseId = null, exerciseName = "Row"),
                listOf(SessionSetEntity(sessionExerciseId = 0, setNumber = 1, reps = 10))
            ))
        )
        val exerciseId = db.workoutSessionDao().getSessionExercises(sessionId).single().id

        db.workoutSessionDao().deleteSession(sessionId)

        assertTrue(db.workoutSessionDao().getSessionExercises(sessionId).isEmpty())
        assertTrue(db.workoutSessionDao().getSets(exerciseId).isEmpty())
    }

    // 10: foreign keys
    @Test
    fun foreignKeys_areEnforced() {
        assertThrows(Exception::class.java) {                              // exercise for a missing day
            runBlocking { db.exerciseDao().insert(ExerciseEntity(workoutDayId = 999, name = "Ghost", sets = 1, reps = 1)) }
        }
        assertThrows(Exception::class.java) {                              // day for a missing plan
            runBlocking { db.workoutDao().insertDay(WorkoutDayEntity(planId = 999, name = "Ghost", weekday = "MONDAY", orderIndex = 0)) }
        }
    }

    @Test
    fun dayWithExercises_cannotBePhysicallyDeleted() = runBlocking {
        val plan = createPlanWithDays()
        val dayId = plan.days.first().id
        db.exerciseDao().insert(ExerciseEntity(workoutDayId = dayId, name = "Bench", sets = 3, reps = 10))
        assertThrows(Exception::class.java) { runBlocking { db.workoutDao().deleteDayById(dayId) } }
        assertNotNull(db.exerciseDao().getForDay(dayId).firstOrNull())
    }

    @Test
    fun deletingPlanRow_cascadesToItsDays() = runBlocking {
        val plan = createPlanWithDays()
        db.workoutDao().deletePlanById(plan.id)
        assertTrue(db.workoutDao().getAllDaysIncludingArchived(plan.id).isEmpty())
    }

    // 11: transactions
    @Test
    fun failedSessionInsert_rollsBackCompletely() {
        val failing = listOf(
            SessionExerciseDraft(
                SessionExerciseEntity(sessionId = 0, exerciseId = 12345 /* does not exist */, exerciseName = "Bad"),
                emptyList()
            )
        )
        assertThrows(Exception::class.java) {
            runBlocking {
                db.workoutSessionDao().insertSessionWithDetails(
                    WorkoutSessionEntity(workoutDayId = null, dayName = "D", planName = "P", startTime = 1), failing
                )
            }
        }
        runBlocking { assertTrue(db.workoutSessionDao().observeAllSessions().first().isEmpty()) }   // no half-saved session
    }

    @Test
    fun createPlanWithDays_isAtomic() = runBlocking {
        val bad = listOf(WorkoutDayEntity(planId = 0, name = "x", weekday = "MONDAY", orderIndex = 0))
        // Valid plan + valid day works…
        val id = db.workoutDao().createPlanWithDays(WorkoutPlanEntity(name = "A", createdAt = 1), bad)
        assertEquals(1, db.workoutDao().getDays(id).size)
        // …and a plan pointing at a non-existent user fails as a whole, leaving nothing behind.
        assertThrows(Exception::class.java) {
            runBlocking { db.workoutDao().createPlanWithDays(WorkoutPlanEntity(name = "B", createdAt = 2, userId = 999), bad) }
        }
        assertEquals(1, db.workoutDao().observePlans().first().size)
    }
}
