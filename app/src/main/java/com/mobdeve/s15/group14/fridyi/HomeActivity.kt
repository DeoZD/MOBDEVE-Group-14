package com.mobdeve.s15.group14.fridyi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import android.graphics.Color
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var cvQuickView: MaterialCardView
    private lateinit var tabExpiring: TextView
    private lateinit var tabExpired: TextView
    private var isExpiringTab = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        database = AppDatabase.getDatabase(this)
        cvQuickView = findViewById(R.id.cv_quick_view)
        tabExpiring = findViewById(R.id.tab_expiring)
        tabExpired = findViewById(R.id.tab_expired)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_home

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_storage -> {
                    startActivity(Intent(this, StorageActivity::class.java))
                    finish()
                    true
                }
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

        tabExpiring.setOnClickListener {
            isExpiringTab = true
            updateTabs()
        }

        tabExpired.setOnClickListener {
            isExpiringTab = false
            updateTabs()
        }

        updateTabs()
        setupRecipeSuggestions()
    }

    private fun updateTabs() {
        if (isExpiringTab) {
            tabExpiring.setBackgroundResource(R.drawable.tab_selected_bg)
            tabExpiring.setTextColor(ContextCompat.getColor(this, R.color.white))
            tabExpired.setBackgroundResource(0)
            tabExpired.setTextColor(Color.GRAY)
            cvQuickView.setStrokeColor(ContextCompat.getColorStateList(this, R.color.primary_green))
        } else {
            tabExpired.setBackgroundResource(R.drawable.tab_selected_bg)
            tabExpired.setTextColor(ContextCompat.getColor(this, R.color.white))
            tabExpiring.setBackgroundResource(0)
            tabExpiring.setTextColor(Color.GRAY)
            cvQuickView.setStrokeColor(ContextCompat.getColorStateList(this, R.color.primary_green))
        }
        loadQuickViewData()
    }

    private fun loadQuickViewData() {
        val rvExpiring = findViewById<RecyclerView>(R.id.rv_expiring_soon)
        lifecycleScope.launch(Dispatchers.IO) {
            val allItems = database.foodDao().getAllFoodSortedByExpiration()
            val currentTime = System.currentTimeMillis()
            val oneMonthMillis = 30L * 24 * 60 * 60 * 1000
            
            val filtered = if (isExpiringTab) {
                allItems.filter { it.expirationDate in currentTime..(currentTime + oneMonthMillis) }
            } else {
                allItems.filter { it.expirationDate < currentTime }
            }

            withContext(Dispatchers.Main) {
                rvExpiring.adapter = ExpiringSoonAdapter(filtered)
                rvExpiring.layoutManager = LinearLayoutManager(this@HomeActivity)
            }
        }
    }

    private fun setupRecipeSuggestions() {
        val rvRecipes = findViewById<RecyclerView>(R.id.rv_recipe_suggestions)
        val recipes = listOf(
            RecipeItem("Spaghetti", "Rich, slow-simmered marinara sauce with garden-fresh basil over perfectly...", R.drawable.spaghetti),
            RecipeItem("Grilled Salmon", "Atlantic salmon fillet seared with garlic herb butter, served alongside crisp gr...", R.drawable.grilled_salmon),
            RecipeItem("Caesar Salad", "Crisp organic romaine lettuce tossed with creamy parmesan dressing, garli...", R.drawable.caesar_salad)
        )
        rvRecipes.adapter = RecipeAdapter(recipes)
        rvRecipes.layoutManager = LinearLayoutManager(this)
    }
}