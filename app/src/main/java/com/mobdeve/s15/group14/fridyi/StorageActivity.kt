package com.mobdeve.s15.group14.fridyi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class StorageActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var adapter: StorageAdapter
    private var storageList = mutableListOf<StorageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ledger)

        database = AppDatabase.getDatabase(this)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_storage

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_storage -> true
                R.id.nav_list -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddFoodActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }

        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_add_storage).setOnClickListener {
            showAddStorageDialog()
        }

        loadData()
    }

    private fun setupRecyclerView() {
        val rvStorage = findViewById<RecyclerView>(R.id.rv_storage)
        adapter = StorageAdapter(storageList, 
            onItemClick = { storageItem ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("FILTER_STORAGE", storageItem.name)
                startActivity(intent)
            },
            onItemLongClick = { storageItem ->
                showDeleteConfirmationDialog(storageItem)
            }
        )
        rvStorage.adapter = adapter
        rvStorage.layoutManager = LinearLayoutManager(this)
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val storageTypes = database.storageDao().getAllStorageTypes()
            val allFood = database.foodDao().getAllFoodSortedByExpiration()
            
            val currentTime = System.currentTimeMillis()
            val oneMonthMillis = 30L * 24 * 60 * 60 * 1000

            val newList = storageTypes.map { type ->
                val foodInStorage = allFood.filter { it.storageLocation == type.name }
                var fresh = 0
                var expiring = 0
                var expired = 0
                
                foodInStorage.forEach { food ->
                    when {
                        food.expirationDate < currentTime -> expired++
                        food.expirationDate < currentTime + oneMonthMillis -> expiring++
                        else -> fresh++
                    }
                }

                StorageItem(
                    name = type.name,
                    description = type.description,
                    imageResId = type.iconResId,
                    freshCount = fresh,
                    expiringCount = expiring,
                    criticalCount = expired,
                    totalItems = foodInStorage.size
                )
            }

            withContext(Dispatchers.Main) {
                storageList.clear()
                storageList.addAll(newList)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showAddStorageDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_storage, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_storage_name)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_storage_desc)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_storage_type)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.storageDao().insertStorageType(
                            StorageType(name = name, description = desc, iconResId = R.drawable.ref_icon)
                        )
                        loadData()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmationDialog(storageItem: StorageItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_storage_title)
            .setMessage(R.string.delete_storage_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val storageType = database.storageDao().getAllStorageTypes().find { it.name == storageItem.name }
                    if (storageType != null) {
                        database.storageDao().deleteStorageType(storageType)
                        database.foodDao().deleteFoodByLocation(storageType.name)
                        loadData()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}