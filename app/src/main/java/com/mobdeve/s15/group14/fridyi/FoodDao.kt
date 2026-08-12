package com.mobdeve.s15.group14.fridyi

import androidx.room.*

@Dao
interface FoodDao {

    // 1. Get all food items, sorted by expiration date (closest first)
    @Query("SELECT * FROM food_inventory ORDER BY expirationDate ASC")
    fun getAllFoodSortedByExpiration(): List<FoodItem>

    // 2. Insert a new food item. If it already exists, replace it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFoodItem(foodItem: FoodItem): Long

    // 3. Delete a food item from our ledger
    @Delete
    fun deleteFoodItem(foodItem: FoodItem)

    // 4. Custom filter: Get items by a specific storage location
    @Query("SELECT * FROM food_inventory WHERE storageLocation = :location")
    fun getFoodByLocation(location: String): List<FoodItem>

    // 5. Get a specific food item by ID
    @Query("SELECT * FROM food_inventory WHERE id = :id")
    fun getFoodById(id: Long): FoodItem?

    // 6. Update an existing food item
    @Update
    fun updateFoodItem(foodItem: FoodItem)
}