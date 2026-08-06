package com.neubofy.reality.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CalendarEvent::class, AppGroupEntity::class, AppLimitEntity::class, ChatSession::class, ChatMessageEntity::class, TapasyaSession::class, DailyStats::class, NightlySession::class, NightlyStep::class, TaskListConfig::class, HabitEntity::class, HabitEntryEntity::class], version = 17, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun appGroupDao(): AppGroupDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun chatDao(): ChatDao
    abstract fun tapasyaSessionDao(): TapasyaSessionDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun nightlyDao(): NightlyDao
    abstract fun taskListConfigDao(): TaskListConfigDao
    abstract fun habitDao(): HabitDao

    companion object {
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE nightly_sessions ADD COLUMN reportContent TEXT")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_stats ADD COLUMN totalPlannedMinutes INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE daily_stats ADD COLUMN totalEffectiveMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `task_list_configs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `googleListId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `description` TEXT NOT NULL)")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE calendar_events ADD COLUMN source TEXT NOT NULL DEFAULT 'GOOGLE'")
                database.execSQL("ALTER TABLE calendar_events ADD COLUMN repeatRule TEXT")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // nightly_sessions columns
                if (!hasColumn(database, "nightly_sessions", "isPlanVerified")) {
                    database.execSQL("ALTER TABLE nightly_sessions ADD COLUMN isPlanVerified INTEGER NOT NULL DEFAULT 0")
                }
                if (!hasColumn(database, "nightly_sessions", "reflectionXp")) {
                    database.execSQL("ALTER TABLE nightly_sessions ADD COLUMN reflectionXp INTEGER NOT NULL DEFAULT 0")
                }
                
                // nightly_steps columns
                if (!hasColumn(database, "nightly_steps", "resultJson")) {
                    database.execSQL("ALTER TABLE nightly_steps ADD COLUMN resultJson TEXT")
                }
                if (!hasColumn(database, "nightly_steps", "linkUrl")) {
                    database.execSQL("ALTER TABLE nightly_steps ADD COLUMN linkUrl TEXT")
                }
            }
            
            private fun hasColumn(db: androidx.sqlite.db.SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
                val cursor = db.query("PRAGMA table_info($tableName)")
                try {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex == -1) return false
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == columnName) {
                            return true
                        }
                    }
                } finally {
                    cursor.close()
                }
                return false
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_startTime_endTime` ON `calendar_events` (`startTime`, `endTime`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_tapasya_sessions_startTime` ON `tapasya_sessions` (`startTime`)")
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `uuid` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `targetValue` REAL NOT NULL,
                        `targetType` INTEGER NOT NULL,
                        `unit` TEXT NOT NULL,
                        `freqNumerator` INTEGER NOT NULL,
                        `freqDenominator` INTEGER NOT NULL,
                        `color` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `autoSourceType` TEXT NOT NULL,
                        `autoSourceTarget` REAL NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'HEALTH',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_entries` (
                        `habitId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `value` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `date`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_entries_habitId` ON `habit_entries` (`habitId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_entries_date` ON `habit_entries` (`date`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reality_database"
                )
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
