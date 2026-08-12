package com.mobdeve.s15.group14.fridyi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_types")
data class StorageType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val iconResId: Int
)
