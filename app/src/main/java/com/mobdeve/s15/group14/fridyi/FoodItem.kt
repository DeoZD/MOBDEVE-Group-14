package com.mobdeve.s15.group14.fridyi

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "food_inventory")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val quantity: Double,
    val unit: String,
    val storageLocation: String,
    val category: String,
    val dateLogged: Long,       // Changed to Long for SQLite compatibility
    val expirationDate: Long    // Changed to Long for SQLite compatibility
)