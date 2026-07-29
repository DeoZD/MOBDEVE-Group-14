package com.mobdeve.s15.group14.fridyi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // 3. Handle Floating Action Button click to open Add Screen
        val fab = findViewById<FloatingActionButton>(R.id.fabAddFood)
        fab.setOnClickListener {
            val intent = Intent(this, AddFoodActivity::class.java)
            startActivity(intent)
        }

        // 4. Setup ItemTouchHelper for swipe to delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = adapter.getItemAt(position) // Add a getter method in FoodAdapter

                lifecycleScope.launch(Dispatchers.IO) {
                    database.foodDao().deleteFoodItem(itemToDelete)
                    loadFoodItems() // Reload updated list
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    // Refresh list every time user returns to this screen
    override fun onResume() {
        super.onResume()
        loadFoodItems()
    }

    private fun loadFoodItems() {
        // Coroutine: Launch on background thread (Dispatchers.IO)
        lifecycleScope.launch(Dispatchers.IO) {
            val items = database.foodDao().getAllFoodSortedByExpiration()

            // Switch back to Main thread (Dispatchers.Main) to update UI
            withContext(Dispatchers.Main) {
                adapter.updateData(items)
            }
        }
    }
}