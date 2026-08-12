package com.mobdeve.s15.group14.fridyi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class StorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_ledger)

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
    }

    private fun setupRecyclerView() {
        val rvStorage = findViewById<RecyclerView>(R.id.rv_storage)
        val sampleStorage = listOf(
            StorageItem("Refrigerator", "main fridge space", R.drawable.ref_icon, 14, 2, 4, 20),
            StorageItem("Crisper", "vegetable storage", R.drawable.crisper_icon, 10, 1, 3, 14),
            StorageItem("Freezer", "frozen goods", R.drawable.freezer_icon, 18, 0, 0, 18)
        )
        
        rvStorage.adapter = StorageAdapter(sampleStorage)
        rvStorage.layoutManager = LinearLayoutManager(this)
    }
}