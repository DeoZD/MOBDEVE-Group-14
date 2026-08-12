package com.mobdeve.s15.group14.fridyi

import androidx.room.*

@Dao
interface StorageDao {
    @Query("SELECT * FROM storage_types")
    fun getAllStorageTypes(): List<StorageType>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStorageType(storageType: StorageType): Long

    @Delete
    fun deleteStorageType(storageType: StorageType): Int
}