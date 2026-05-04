package com.survivemum.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. Update the entities list to include PatientEntity
@Database(
    entities = [
        UserEntity::class,
        PatientEntity::class, // Added: The "drawer" for mother records
        VisitEntity::class,
        AlertEntity::class,
        VitalReadingEntity::class,
        CryEventEntity::class,
        NewbornRecordEntity::class,
        PreferenceEntity::class
    ],
    version = 1, // Keep at 1 for now since this is the first build
    exportSchema = false
)
abstract class SurviveMumDatabase : RoomDatabase() {

    // 2. Connect the PatientDao to the database
    abstract fun patientDao(): PatientDao

    abstract fun userDao(): UserDao
    abstract fun visitDao(): VisitDao
    abstract fun alertDao(): AlertDao
    abstract fun vitalReadingDao(): VitalReadingDao
    abstract fun cryEventDao(): CryEventDao
    abstract fun newbornRecordDao(): NewbornRecordDao
    abstract fun preferenceDao(): PreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: SurviveMumDatabase? = null

        /**
         * Gets the database instance. Using the "Singleton" pattern ensures
         * we don't open multiple files on the laptop, which saves memory.
         */
        fun getDatabase(context: Context): SurviveMumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SurviveMumDatabase::class.java,
                    "survivemum_database" // The physical filename on the device
                )
                    .fallbackToDestructiveMigration() // Recreates the DB if you change the Entity later
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}