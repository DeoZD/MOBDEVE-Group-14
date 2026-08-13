package com.mobdeve.s15.group14.fridyi

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FoodItem::class, StorageType::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun storageDao(): StorageDao

    companion object {
        // Volatile ensures that changes to INSTANCE are immediately visible to other threads
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If the INSTANCE is not null, return it. Otherwise, create the database.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fridyi_database"
                )
                    .fallbackToDestructiveMigration() // Wipes and rebuilds DB if you change the FoodItem class later
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}