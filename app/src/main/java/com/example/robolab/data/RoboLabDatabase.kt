package com.example.robolab.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class RoboLabDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: RoboLabDatabase? = null

        fun getDatabase(context: Context): RoboLabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoboLabDatabase::class.java,
                    "robolab_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
