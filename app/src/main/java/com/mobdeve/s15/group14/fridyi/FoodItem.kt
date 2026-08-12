package com.mobdeve.s15.group14.fridyi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_inventory")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val quantity: Double,
    val unit: String,
    val storageLocation: String,
    val dateLogged: Long,
    val expirationDate: Long,
    val imageUri: String? = null // Optional image reference path
)