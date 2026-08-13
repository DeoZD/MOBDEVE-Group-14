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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FoodAdapter
    private lateinit var database: AppDatabase
    private var allItems = listOf<FoodItem>()
    private var filteredItems = mutableListOf<FoodItem>()
    private var selectedStorages = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)
        intent.getStringExtra("FILTER_STORAGE")?.let {
            selectedStorages.add(it)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_food_list)
        adapter = FoodAdapter(emptyList()) { item ->
            val intent = Intent(this, AddFoodActivity::class.java).apply {
                putExtra("EDIT_MODE", true)
                putExtra("FOOD_ID", item.id)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupSearch()
        setupChips()
        setupStorageFilterButton()
        setupBottomNav()
        setupSwipeToDelete(recyclerView)

        addSampleData()
    }

    private fun setupStorageFilterButton() {
        findViewById<Button>(R.id.btn_storage_filter).setOnClickListener {
            showStorageSelectionDialog()
        }
    }

    private fun showStorageSelectionDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val storageNames = database.storageDao().getAllStorageTypes().map { it.name }.toTypedArray()
            val checkedItems = storageNames.map { selectedStorages.contains(it) }.toBooleanArray()

            withContext(Dispatchers.Main) {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Select Storage Locations")
                    .setMultiChoiceItems(storageNames, checkedItems) { _, index, isChecked ->
                        if (isChecked) {
                            selectedStorages.add(storageNames[index])
                        } else {
                            selectedStorages.remove(storageNames[index])
                        }
                    }
                    .setPositiveButton("Apply") { _, _ ->
                        applyFilters()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group_filters)
        chipGroup.isSingleSelection = false
        
        // Populate category chips dynamically if needed, but for now use existing ones
        // The requirements say "Multiple tabs can be chosen at once"
        
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            applyFilters()
        }
    }

    private fun setupBottomNav() {
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
                R.id.nav_profile -> true
                else -> false
            }
        }
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val background = ColorDrawable(Color.RED)
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val itemToDelete = adapter.getItemAt(position)
                lifecycleScope.launch(Dispatchers.IO) {
                    database.foodDao().deleteFoodItem(itemToDelete)
                    loadFoodItems()
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun applyFilters() {
        val query = findViewById<EditText>(R.id.et_search).text.toString().lowercase()
        val chipGroup = findViewById<ChipGroup>(R.id.chip_group_filters)
        val checkedChips = chipGroup.checkedChipIds.map { findViewById<Chip>(it).text.toString() }

        var list = allItems.filter { it.name.lowercase().contains(query) || it.storageLocation.lowercase().contains(query) }

        if (selectedStorages.isNotEmpty()) {
            list = list.filter { selectedStorages.contains(it.storageLocation) }
        }

        if (checkedChips.isNotEmpty()) {
            list = list.filter { item ->
                checkedChips.any { chipText ->
                    when (chipText) {
                        "Expiring soon" -> {
                            val daysLeft = (item.expirationDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                            daysLeft in 0..29
                        }
                        "Expired" -> item.expirationDate < System.currentTimeMillis()
                        else -> item.category == chipText
                    }
                }
            }
        }

        // Sorting: Expiring soon (0-29 days), then expired (<0), then fresh (>=30)
        val currentTime = System.currentTimeMillis()
        val sortedList = list.sortedWith(compareBy({ 
            val daysLeft = (it.expirationDate - currentTime) / (1000 * 60 * 60 * 24)
            when {
                daysLeft in 0..29 -> 0 // Expiring soon
                daysLeft < 0 -> 1    // Expired
                else -> 2            // Fresh
            }
        }, { it.expirationDate }))

        adapter.updateData(sortedList)
    }

    private fun addSampleData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val storageCount = database.storageDao().getAllStorageTypes().size
            if (storageCount == 0) {
                database.storageDao().insertStorageType(StorageType(name = "Refrigerator", description = "Main fridge", iconResId = R.drawable.ref_icon))
                database.storageDao().insertStorageType(StorageType(name = "Freezer", description = "Deep freeze", iconResId = R.drawable.freezer_icon))
                database.storageDao().insertStorageType(StorageType(name = "Crisper", description = "Fresh produce", iconResId = R.drawable.crisper_icon))
            }

            val count = database.foodDao().getAllFoodSortedByExpiration().size
            if (count == 0) {
                val cal = Calendar.getInstance()
                val now = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_YEAR, 5)
                val expiringSoon = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_YEAR, -10)
                val expired = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_YEAR, 40)
                val fresh = cal.timeInMillis

                val samples = listOf(
                    FoodItem(name = "Chicken", quantity = 2.0, unit = "kg", storageLocation = "Freezer", category = "Poultry", dateLogged = now, expirationDate = expiringSoon),
                    FoodItem(name = "Milk", quantity = 1.0, unit = "L", storageLocation = "Refrigerator", category = "Dairy", dateLogged = now, expirationDate = expired),
                    FoodItem(name = "Rice", quantity = 5.0, unit = "kg", storageLocation = "Pantry", category = "Grains", dateLogged = now, expirationDate = fresh)
                )
                
                samples.forEach { database.foodDao().insertFoodItem(it) }
            }
            loadFoodItems()
        }
    }

    override fun onResume() {
        super.onResume()
        loadFoodItems()
    }

    private fun loadFoodItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            allItems = database.foodDao().getAllFoodSortedByExpiration()
            withContext(Dispatchers.Main) {
                applyFilters()
            }
        }
    }
}