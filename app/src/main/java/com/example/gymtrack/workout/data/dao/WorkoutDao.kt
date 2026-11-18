package com.example.gymtrack.workout.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gymtrack.workout.data.entity.WorkoutDayEntity
import com.example.gymtrack.workout.data.entity.WorkoutPlanEntity
import kotlinx.coroutines.flow.Flow

/** Result of the day + plan-name join used by the details screen. */
data class DayWithPlanName(
    @Embedded val day: WorkoutDayEntity,
    val planName: String
)

@Dao
abstract class WorkoutDao {

    // ---- Reads (archived rows are always excluded) ----

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

    // ---- Single-row writes ----

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

    // ---- Multi-row writes (each runs in ONE transaction) ----

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
