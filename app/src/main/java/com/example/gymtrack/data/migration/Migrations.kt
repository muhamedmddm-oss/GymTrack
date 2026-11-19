package com.example.gymtrack.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 (temporary Person 2 database: workout_plans + workout_days)  ->  v2 (real GymTrack database).
 *
 * Non-destructive: every existing plan and day is kept untouched.
 *  - New tables are only CREATED.
 *  - workout_plans gets its new nullable `userId` column with ALTER TABLE ... ADD COLUMN.
 *    We deliberately do NOT recreate workout_plans (the usual copy/drop/rename trick): dropping it
 *    would trigger ON DELETE CASCADE and wipe all rows of workout_days.
 *
 * The SQL must match what Room generates for the entities, otherwise Room refuses to open the
 * database. Keep it in sync with the entity classes if they ever change.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`heightCm` REAL, " +
                "`weightKg` REAL, " +
                "`trainingDaysPerWeek` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )

        // New nullable column + foreign key + index on the existing plans table.
        db.execSQL(
            "ALTER TABLE `workout_plans` ADD COLUMN `userId` INTEGER " +
                "REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_plans_userId` ON `workout_plans` (`userId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutDayId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`sets` INTEGER NOT NULL, " +
                "`reps` INTEGER NOT NULL, " +
                "`weightKg` REAL, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "`notes` TEXT, " +
                "FOREIGN KEY(`workoutDayId`) REFERENCES `workout_days`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercises_workoutDayId` ON `exercises` (`workoutDayId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutDayId` INTEGER, " +
                "`dayName` TEXT NOT NULL, " +
                "`planName` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, " +
                "`endTime` INTEGER, " +
                "`durationSeconds` INTEGER NOT NULL, " +
                "FOREIGN KEY(`workoutDayId`) REFERENCES `workout_days`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_workoutDayId` ON `workout_sessions` (`workoutDayId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_startTime` ON `workout_sessions` (`startTime`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` INTEGER NOT NULL, " +
                "`exerciseId` INTEGER, " +
                "`exerciseName` TEXT NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_sessionId` ON `session_exercises` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercises_exerciseId` ON `session_exercises` (`exerciseId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_sets` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionExerciseId` INTEGER NOT NULL, " +
                "`setNumber` INTEGER NOT NULL, " +
                "`reps` INTEGER NOT NULL, " +
                "`weightKg` REAL, " +
                "`isCompleted` INTEGER NOT NULL, " +
                "`completedAt` INTEGER, " +
                "FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_sets_sessionExerciseId` ON `session_sets` (`sessionExerciseId`)")
    }
}

/** v2 -> v3: the onboarding screen also collects the user's age. Nullable column, existing rows keep working. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `age` INTEGER")
    }
}

/**
 * v3 -> v4 (Active Workout timer): the timer state of a running workout is stored on its session row,
 * so a workout (and its elapsed time) can be resumed after the app was closed.
 * Existing sessions keep working: accumulatedMillis starts at 0 and runningSinceMillis at NULL.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `accumulatedMillis` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `runningSinceMillis` INTEGER")
    }
}
