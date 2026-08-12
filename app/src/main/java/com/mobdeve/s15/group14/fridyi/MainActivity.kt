package com.mobdeve.s15.group14.fridyi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FoodAdapter
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize Database instance
        database = AppDatabase.getDatabase(this)

        // 2. Setup RecyclerView with Adapter
        val recyclerView = findViewById<RecyclerView>(R.id.rvFoodList)
        adapter = FoodAdapter(emptyList())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 3. Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_list
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_storage -> {
                    startActivity(Intent(this, StorageActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_list -> true
                R.id.nav_add -> {
                    startActivity(Intent(this, AddFoodActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    // startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // 4. Setup ItemTouchHelper for swipe to delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = adapter.getItemAt(position)

                lifecycleScope.launch(Dispatchers.IO) {
                    database.foodDao().deleteFoodItem(itemToDelete)
                    loadFoodItems()
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        // Add sample data if database is empty
        addSampleData()
    }

    private fun addSampleData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = database.foodDao().getAllFoodSortedByExpiration().size
            if (count == 0) {
                val cal = Calendar.getInstance()
                val now = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val inWeek = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_YEAR, -10)
                val expired = cal.timeInMillis

                val samples = listOf(
                    FoodItem(name = "Frozen Chicken", quantity = 2.0, unit = "kg bag", storageLocation = "Freezer", category = "Poultry", dateLogged = now, expirationDate = inWeek),
                    FoodItem(name = "Gummy Candy", quantity = 500.0, unit = "g pack", storageLocation = "Pantry", category = "Sweets", dateLogged = now, expirationDate = inWeek),
                    FoodItem(name = "Cheesecake", quantity = 1.0, unit = "slice", storageLocation = "Refrigerator", category = "Dessert", dateLogged = now, expirationDate = now)
                )
                
                samples.forEach { database.foodDao().insertFoodItem(it) }
                loadFoodItems()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFoodItems()
    }

    private fun loadFoodItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = database.foodDao().getAllFoodSortedByExpiration()
            withContext(Dispatchers.Main) {
                adapter.updateData(items)
            }
        }
    }
}