package com.example.gymtrack.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `age` INTEGER")
    }
}


val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `accumulatedMillis` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `runningSinceMillis` INTEGER")
    }
}
