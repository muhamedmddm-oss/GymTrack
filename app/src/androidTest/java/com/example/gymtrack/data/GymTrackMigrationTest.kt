package com.example.gymtrack.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gymtrack.data.entity.ExerciseEntity
import com.example.gymtrack.data.entity.UserProfileEntity
import com.example.gymtrack.data.migration.MIGRATION_1_2
import com.example.gymtrack.data.migration.MIGRATION_2_3
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds a REAL version-1 database (the temporary Person 2 schema, written with plain SQL),
 * fills it with data, then opens it with the final Room database and checks that nothing was lost.
 */
@RunWith(AndroidJUnit4::class)
class GymTrackMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createVersion1Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `workout_days` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planId` INTEGER NOT NULL, `name` TEXT NOT NULL, `weekday` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, FOREIGN KEY(`planId`) REFERENCES `workout_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_days_planId` ON `workout_days` (`planId`)")
                db.execSQL("INSERT INTO workout_plans (id, name, createdAt, isArchived) VALUES (1, 'My PPL', 1000, 0)")
                db.execSQL("INSERT INTO workout_plans (id, name, createdAt, isArchived) VALUES (2, 'Old plan', 500, 1)")
                db.execSQL("INSERT INTO workout_days (id, planId, name, weekday, orderIndex, isArchived) VALUES (1, 1, 'Push', 'SATURDAY', 0, 0)")
                db.execSQL("INSERT INTO workout_days (id, planId, name, weekday, orderIndex, isArchived) VALUES (2, 1, 'Pull', 'MONDAY', 1, 0)")
                db.execSQL("INSERT INTO workout_days (id, planId, name, weekday, orderIndex, isArchived) VALUES (3, 2, 'Legs', 'FRIDAY', 0, 1)")
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
        helper.close()
    }

    private fun openMigrated(): GymTrackDatabase =
        Room.databaseBuilder(context, GymTrackDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Test
    fun migrate1To3_keepsAllPlansAndDays() = runBlocking {
        createVersion1Database()
        val db = openMigrated()
        try {
            val plan = db.workoutDao().getPlan(1)!!
            assertEquals("My PPL", plan.name)
            assertEquals(1000L, plan.createdAt)
            assertNull(plan.userId)                                          // new column starts empty

            assertEquals(listOf("Push", "Pull"), db.workoutDao().getDays(1).map { it.name })
            assertEquals("SATURDAY", db.workoutDao().getDays(1).first().weekday)

            // Archived rows survive too.
            assertEquals(1, db.workoutDao().getAllDaysIncludingArchived(2).size)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate1To3_newTablesAreUsable_andForeignKeysWork() = runBlocking {
        createVersion1Database()
        val db = openMigrated()
        try {
            val userId = db.userProfileDao().insert(
                UserProfileEntity(name = "Saif", trainingDaysPerWeek = 3, createdAt = 1, updatedAt = 1, age = 25)
            )
            assertEquals("Saif", db.userProfileDao().getById(userId)!!.name)
            assertEquals(25, db.userProfileDao().getById(userId)!!.age)       // v3 column

            db.exerciseDao().insert(ExerciseEntity(workoutDayId = 1, name = "Bench", sets = 3, reps = 10))
            assertEquals(1, db.exerciseDao().countForDay(1))

            // Old data still intact after using the new tables.
            assertEquals(2, db.workoutDao().getDays(1).size)
        } finally {
            db.close()
        }
    }
}
