package com.mobdeve.s15.group14.fridyi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddFoodActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        val etName = findViewById<EditText>(R.id.etName)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etExpiration = findViewById<EditText>(R.id.etExpiration)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.nav_add
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
                R.id.nav_list -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add -> true
                R.id.nav_profile -> {
                    // startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val expirationStr = etExpiration.text.toString().trim()

            if (name.isEmpty() || expirationStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simple logic to parse days from expiration string for now
            val daysToExpire = expirationStr.toIntOrNull() ?: 1

            // Calculate dates in milliseconds
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTime
            calendar.add(Calendar.DAY_OF_YEAR, daysToExpire)
            val expirationTime = calendar.timeInMillis

            val foodItem = FoodItem(
                name = name,
                quantity = 1.0,
                unit = "pcs",
                storageLocation = if (location.isEmpty()) "Fridge" else location,
                category = "General",
                dateLogged = currentTime,
                expirationDate = expirationTime
            )

            // Save to Room DB and schedule notification
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                db.foodDao().insertFoodItem(foodItem)

                scheduleNotification(name, expirationTime)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddFoodActivity, "Item Saved!", Toast.LENGTH_SHORT).show()
                    finish() // Closes this screen and returns to MainActivity
                }
            }
        }
    }

    private fun scheduleNotification(itemName: String, triggerTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("FOOD_NAME", itemName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            itemName.hashCode(), // Unique Request Code per food item name
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule exact or approximate alarm for the expiration date
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }
}