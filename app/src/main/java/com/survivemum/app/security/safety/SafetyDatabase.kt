package com.survivemum.app.security.safety

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for SurviveMum safety audit data.
 *
 * Currently holds only the safety_audit table. If other safety-related
 * tables are added later (e.g. tuning evidence, prompt-injection samples
 * captured for analysis), they go here so they share a single SQLite file.
 *
 * Singleton pattern: one database instance per process. Room handles
 * thread safety internally; we just need to avoid creating multiple
 * databases pointing at the same file.
 */
@Database(
    entities = [SafetyAuditEntry::class],
    version = 1,
    exportSchema = false
)
abstract class SafetyDatabase : RoomDatabase() {

    abstract fun safetyAuditDao(): SafetyAuditDao

    companion object {
        @Volatile
        private var INSTANCE: SafetyDatabase? = null

        fun getInstance(context: Context): SafetyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SafetyDatabase::class.java,
                    "survivemum_safety.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}