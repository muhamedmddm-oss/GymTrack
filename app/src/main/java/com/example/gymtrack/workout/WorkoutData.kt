package com.example.gymtrack.workout

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gymtrack.data.entity.UserProfileEntity
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.example.gymtrack.data.GymTrackDatabase
import com.example.gymtrack.data.RoomWorkoutDayUsageProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull


enum class Weekday(val displayName: String, val shortName: String) {
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun"),
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri");

    companion object {
        /** Returns the weekday for a stored name, or null if it is unknown. */
        fun fromName(name: String): Weekday? = entries.firstOrNull { it.name == name }
    }
}



@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val weekday: String,
    val orderIndex: Int,
    val isArchived: Boolean = false
)



@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId")]
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isArchived: Boolean = false,
    val userId: Long? = null
)

// ---- WorkoutDao ----

data class DayWithPlanName(
    @Embedded val day: WorkoutDayEntity,
    val planName: String
)

@Dao
abstract class WorkoutDao {


    @Query("SELECT * FROM workout_plans WHERE isArchived = 0 ORDER BY createdAt DESC, id DESC")
    abstract fun observePlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_days WHERE isArchived = 0 ORDER BY planId, orderIndex")
    abstract fun observeAllDays(): Flow<List<WorkoutDayEntity>>

    @Query(
        "SELECT workout_days.*, workout_plans.name AS planName FROM workout_days " +
            "INNER JOIN workout_plans ON workout_plans.id = workout_days.planId " +
            "WHERE workout_days.id = :dayId AND workout_days.isArchived = 0 " +
            "AND workout_plans.isArchived = 0"
    )
    abstract fun observeDayWithPlanName(dayId: Long): Flow<DayWithPlanName?>

    @Query("SELECT * FROM workout_plans WHERE id = :planId AND isArchived = 0")
    abstract suspend fun getPlan(planId: Long): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_days WHERE planId = :planId AND isArchived = 0 ORDER BY orderIndex")
    abstract suspend fun getDays(planId: Long): List<WorkoutDayEntity>

    /** Includes archived days: needed to decide whether a plan can be physically deleted. */
    @Query("SELECT * FROM workout_days WHERE planId = :planId")
    abstract suspend fun getAllDaysIncludingArchived(planId: Long): List<WorkoutDayEntity>

    @Query("SELECT * FROM workout_days WHERE id = :dayId AND isArchived = 0")
    abstract suspend fun getDay(dayId: Long): WorkoutDayEntity?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM workout_days WHERE planId = :planId")
    abstract suspend fun maxOrderIndex(planId: Long): Int

    @Insert
    abstract suspend fun insertPlan(plan: WorkoutPlanEntity): Long

    @Insert
    abstract suspend fun insertDay(day: WorkoutDayEntity): Long

    @Insert
    abstract suspend fun insertDays(days: List<WorkoutDayEntity>)

    @Update
    abstract suspend fun updateDay(day: WorkoutDayEntity)

    @Query("UPDATE workout_plans SET name = :name WHERE id = :planId")
    abstract suspend fun renamePlan(planId: Long, name: String)

    @Query("DELETE FROM workout_plans WHERE id = :planId")
    abstract suspend fun deletePlanById(planId: Long)

    @Query("DELETE FROM workout_days WHERE id = :dayId")
    abstract suspend fun deleteDayById(dayId: Long)

    @Query("UPDATE workout_days SET isArchived = 1 WHERE id = :dayId")
    abstract suspend fun archiveDay(dayId: Long)

    @Query("UPDATE workout_plans SET isArchived = 1 WHERE id = :planId")
    abstract suspend fun archivePlan(planId: Long)

    @Query("UPDATE workout_days SET isArchived = 1 WHERE planId = :planId")
    abstract suspend fun archiveDaysOfPlan(planId: Long)


    @Transaction
    open suspend fun createPlanWithDays(plan: WorkoutPlanEntity, days: List<WorkoutDayEntity>): Long {
        val planId = insertPlan(plan)
        insertDays(days.map { it.copy(planId = planId) })
        return planId
    }

    @Transaction
    open suspend fun applyPlanEdit(
        planId: Long,
        name: String,
        updates: List<WorkoutDayEntity>,
        inserts: List<WorkoutDayEntity>,
        deleteIds: List<Long>,
        archiveIds: List<Long>
    ) {
        renamePlan(planId, name)
        deleteIds.forEach { deleteDayById(it) }
        archiveIds.forEach { archiveDay(it) }
        updates.forEach { updateDay(it) }
        if (inserts.isNotEmpty()) insertDays(inserts)
    }

    @Transaction
    open suspend fun archivePlanWithDays(planId: Long) {
        archiveDaysOfPlan(planId)
        archivePlan(planId)
    }
}



interface WorkoutDayUsageProvider {
    fun observeExerciseCounts(): Flow<Map<Long, Int>>

    suspend fun isDayInUse(dayId: Long): Boolean
}

object NoWorkoutDayUsage : WorkoutDayUsageProvider {
    override fun observeExerciseCounts(): Flow<Map<Long, Int>> = flowOf(emptyMap())
    override suspend fun isDayInUse(dayId: Long): Boolean = false
}


internal fun WorkoutDayEntity.toDomain(exerciseCount: Int = 0): WorkoutDay = WorkoutDay(
    id = id,
    planId = planId,
    name = name,
    // Unknown stored values fall back to Monday instead of crashing the whole screen.
    weekday = Weekday.fromName(weekday) ?: Weekday.MONDAY,
    orderIndex = orderIndex,
    exerciseCount = exerciseCount
)

internal fun WorkoutPlanEntity.toDomain(days: List<WorkoutDay>): WorkoutPlan = WorkoutPlan(
    id = id,
    name = name,
    createdAt = createdAt,
    days = days
)


data class WorkoutPlan(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val days: List<WorkoutDay>
)


data class WorkoutDay(
    val id: Long,
    val planId: Long,
    val name: String,
    val weekday: Weekday,
    val orderIndex: Int,
    val exerciseCount: Int = 0
)

data class WorkoutDayDetails(
    val day: WorkoutDay,
    val planName: String
)

data class WorkoutDayInput(
    val id: Long = 0,
    val name: String,
    val weekday: Weekday
)



object WorkoutModule {

    @Volatile
    var usageProvider: WorkoutDayUsageProvider? = null

    @Volatile
    private var repositoryInstance: WorkoutRepository? = null

    fun repository(context: Context): WorkoutRepository =
        repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: run {
                val db = GymTrackDatabase.getInstance(context)
                WorkoutRepositoryImpl(
                    dao = db.workoutDao(),
                    usage = usageProvider ?: RoomWorkoutDayUsageProvider(db.exerciseDao(), db.workoutSessionDao())
                )
            }.also { repositoryInstance = it }
        }

    /** Builds a ViewModelProvider.Factory for one ViewModel using the shared repository. */
    inline fun <reified VM : ViewModel> factory(
        crossinline create: () -> VM
    ): ViewModelProvider.Factory = viewModelFactory { initializer { create() } }
}


data class DayFormInput(val name: String, val weekday: Weekday?)

data class DayValidation(
    val nameError: String? = null,
    val weekdayError: String? = null
) {
    val isValid: Boolean get() = nameError == null && weekdayError == null
}

data class PlanValidation(
    val planNameError: String? = null,
    val daysError: String? = null,
    val dayErrors: List<DayValidation> = emptyList()
) {
    val isValid: Boolean
        get() = planNameError == null && daysError == null && dayErrors.all { it.isValid }
}

object WorkoutPlanValidator {
    const val MIN_DAYS = 1
    const val MAX_DAYS = 7
    const val MAX_PLAN_NAME_LENGTH = 40
    const val MAX_DAY_NAME_LENGTH = 30

    fun validate(planName: String, days: List<DayFormInput>): PlanValidation {
        val planNameError = when {
            planName.isBlank() -> "Enter a workout plan name"
            planName.trim().length > MAX_PLAN_NAME_LENGTH ->
                "Plan name must be $MAX_PLAN_NAME_LENGTH characters or fewer"
            else -> null
        }

        val daysError = when {
            days.size < MIN_DAYS -> "Select at least $MIN_DAYS training day"
            days.size > MAX_DAYS -> "A plan can have at most $MAX_DAYS training days"
            else -> null
        }

        val seen = mutableSetOf<Weekday>()
        val dayErrors = days.map { day ->
            val nameError = when {
                day.name.isBlank() -> "Enter a name for this day"
                day.name.trim().length > MAX_DAY_NAME_LENGTH ->
                    "Day name must be $MAX_DAY_NAME_LENGTH characters or fewer"
                else -> null
            }
            val weekday = day.weekday
            val weekdayError = when {
                weekday == null -> "Choose a weekday"
                !seen.add(weekday) -> "${weekday.displayName} is already used by another day"
                else -> null
            }
            DayValidation(nameError, weekdayError)
        }

        return PlanValidation(planNameError, daysError, dayErrors)
    }
}



interface WorkoutRepository {
    fun observePlans(): Flow<List<WorkoutPlan>>

    fun observeDay(dayId: Long): Flow<WorkoutDayDetails?>

    suspend fun getPlan(planId: Long): WorkoutPlan?

    /** Creates a plan and all its days in one transaction. Returns the new plan id. */
    suspend fun createPlan(name: String, days: List<WorkoutDayInput>): Long

    suspend fun updatePlan(planId: Long, name: String, days: List<WorkoutDayInput>)

    suspend fun deletePlan(planId: Long)

    suspend fun addDay(planId: Long, day: WorkoutDayInput): Long

    suspend fun updateDay(dayId: Long, name: String, weekday: Weekday)

    suspend fun deleteDay(dayId: Long)
}

// ---- WorkoutRepositoryImpl.kt ----

class WorkoutRepositoryImpl(
    private val dao: WorkoutDao,
    private val usage: WorkoutDayUsageProvider = NoWorkoutDayUsage,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : WorkoutRepository {

    override fun observePlans(): Flow<List<WorkoutPlan>> =
        combine(dao.observePlans(), dao.observeAllDays(), usage.observeExerciseCounts()) { plans, days, counts ->
            val daysByPlan = days.groupBy { it.planId }
            plans.map { plan ->
                val planDays = daysByPlan[plan.id].orEmpty().map { it.toDomain(counts[it.id] ?: 0) }
                plan.toDomain(planDays)
            }
        }

    override fun observeDay(dayId: Long): Flow<WorkoutDayDetails?> =
        combine(dao.observeDayWithPlanName(dayId), usage.observeExerciseCounts()) { row, counts ->
            row?.let { WorkoutDayDetails(it.day.toDomain(counts[it.day.id] ?: 0), it.planName) }
        }

    override suspend fun getPlan(planId: Long): WorkoutPlan? {
        val plan = dao.getPlan(planId) ?: return null
        val counts = firstCounts()
        return plan.toDomain(dao.getDays(planId).map { it.toDomain(counts[it.id] ?: 0) })
    }

    override suspend fun createPlan(name: String, days: List<WorkoutDayInput>): Long {
        checkPlanInput(name, days)
        val plan = WorkoutPlanEntity(name = name.trim(), createdAt = clock())
        val dayEntities = days.mapIndexed { index, day ->
            WorkoutDayEntity(planId = 0, name = day.name.trim(), weekday = day.weekday.name, orderIndex = index)
        }
        return dao.createPlanWithDays(plan, dayEntities)
    }

    override suspend fun updatePlan(planId: Long, name: String, days: List<WorkoutDayInput>) {
        checkPlanInput(name, days)
        require(dao.getPlan(planId) != null) { "Workout plan $planId does not exist" }

        val existing = dao.getDays(planId).associateBy { it.id }
        val updates = mutableListOf<WorkoutDayEntity>()
        val inserts = mutableListOf<WorkoutDayEntity>()
        days.forEachIndexed { index, input ->
            val current = existing[input.id]
            if (input.id != 0L && current != null) {
                updates += current.copy(name = input.name.trim(), weekday = input.weekday.name, orderIndex = index)
            } else {
                inserts += WorkoutDayEntity(
                    planId = planId,
                    name = input.name.trim(),
                    weekday = input.weekday.name,
                    orderIndex = index
                )
            }
        }

        // Days that disappeared: delete them, unless exercises/sessions still reference them.
        val keptIds = updates.map { it.id }.toSet()
        val removed = existing.values.filter { it.id !in keptIds }
        val (toArchive, toDelete) = removed.partition { usage.isDayInUse(it.id) }

        dao.applyPlanEdit(
            planId = planId,
            name = name.trim(),
            updates = updates,
            inserts = inserts,
            deleteIds = toDelete.map { it.id },
            archiveIds = toArchive.map { it.id }
        )
    }

    override suspend fun deletePlan(planId: Long) {
        // Archived days count too: a hard delete would cascade to them and wipe their history.
        val anyInUse = dao.getAllDaysIncludingArchived(planId).any { usage.isDayInUse(it.id) }
        if (anyInUse) dao.archivePlanWithDays(planId) else dao.deletePlanById(planId)
    }

    override suspend fun addDay(planId: Long, day: WorkoutDayInput): Long {
        require(day.name.isNotBlank()) { "Day name must not be blank" }
        val siblings = dao.getDays(planId)
        require(siblings.size < WorkoutPlanValidator.MAX_DAYS) { "A plan can have at most 7 days" }
        require(siblings.none { it.weekday == day.weekday.name }) { "${day.weekday.displayName} is already used" }
        return dao.insertDay(
            WorkoutDayEntity(
                planId = planId,
                name = day.name.trim(),
                weekday = day.weekday.name,
                orderIndex = dao.maxOrderIndex(planId) + 1
            )
        )
    }

    override suspend fun updateDay(dayId: Long, name: String, weekday: Weekday) {
        require(name.isNotBlank()) { "Day name must not be blank" }
        val day = dao.getDay(dayId) ?: throw IllegalArgumentException("Workout day $dayId does not exist")
        val clash = dao.getDays(day.planId).any { it.id != dayId && it.weekday == weekday.name }
        require(!clash) { "${weekday.displayName} is already used" }
        dao.updateDay(day.copy(name = name.trim(), weekday = weekday.name))
    }

    override suspend fun deleteDay(dayId: Long) {
        dao.getDay(dayId) ?: return
        if (usage.isDayInUse(dayId)) dao.archiveDay(dayId) else dao.deleteDayById(dayId)
    }

    private suspend fun firstCounts(): Map<Long, Int> =
        usage.observeExerciseCounts().firstOrNull() ?: emptyMap()

    private fun checkPlanInput(name: String, days: List<WorkoutDayInput>) {
        require(name.isNotBlank()) { "Plan name must not be blank" }
        require(days.size in WorkoutPlanValidator.MIN_DAYS..WorkoutPlanValidator.MAX_DAYS) {
            "A plan needs between 1 and 7 days"
        }
        require(days.all { it.name.isNotBlank() }) { "Day names must not be blank" }
        require(days.map { it.weekday }.toSet().size == days.size) { "Weekdays must be unique within a plan" }
    }
}
