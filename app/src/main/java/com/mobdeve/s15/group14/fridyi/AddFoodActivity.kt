package com.mobdeve.s15.group14.fridyi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*

class AddFoodActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var selectedImageUri: Uri? = null
    private var selectedExpirationDate: Calendar = Calendar.getInstance()
    private lateinit var ivPreview: ImageView
    private lateinit var spStorage: Spinner
    private lateinit var tvWarning: TextView

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivPreview.setImageURI(it)
            ivPreview.setPadding(0, 0, 0, 0)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                ivPreview.setImageBitmap(bitmap)
                ivPreview.setPadding(0, 0, 0, 0)
                // Placeholder URI for camera captured image
                selectedImageUri = Uri.parse("content://camera_captured_bitmap")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        database = AppDatabase.getDatabase(this)

        try {
            val etName = findViewById<EditText>(R.id.etName)
            val etQuantity = findViewById<EditText>(R.id.et_quantity)
            val spUnit = findViewById<Spinner>(R.id.sp_unit)
            spStorage = findViewById(R.id.sp_storage)
            tvWarning = findViewById(R.id.tv_no_storage_warning)
            val etExpiration = findViewById<EditText>(R.id.etExpiration)
            val btnSave = findViewById<Button>(R.id.btnSave)
            ivPreview = findViewById(R.id.iv_item_preview)

            setupSpinners()

            findViewById<Button>(R.id.btn_gallery).setOnClickListener {
                galleryLauncher.launch("image/*")
            }

            findViewById<Button>(R.id.btn_take_photo).setOnClickListener {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(intent)
            }

            etExpiration.setOnClickListener {
                showDatePicker(etExpiration)
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString().trim()
                val quantityStr = etQuantity.text.toString().trim()
                val quantity = quantityStr.toDoubleOrNull() ?: 0.0
                val unit = spUnit.selectedItem?.toString() ?: ""
                val storage = if (spStorage.adapter != null && spStorage.adapter.count > 0) spStorage.selectedItem.toString() else ""
                val expirationStr = etExpiration.text.toString().trim()

                if (name.isEmpty() || quantity <= 0 || storage.isEmpty() || expirationStr.isEmpty()) {
                    Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentTime = System.currentTimeMillis()
                val foodItem = FoodItem(
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    storageLocation = storage,
                    category = "General",
                    dateLogged = currentTime,
                    expirationDate = selectedExpirationDate.timeInMillis,
                    imageUri = selectedImageUri?.toString()
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        database.foodDao().insertFoodItem(foodItem)
                        scheduleNotification(name, selectedExpirationDate.timeInMillis)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddFoodActivity, "Item Saved Successfully!", Toast.LENGTH_SHORT).show()
                            
                            // Clear fields instead of finishing, to stay on the screen
                            etName.text.clear()
                            etQuantity.text.clear()
                            etExpiration.text.clear()
                            ivPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                            ivPreview.setPadding(16, 16, 16, 16)
                            selectedImageUri = null
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AddFoodActivity, "Error saving item: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            // Bottom Nav
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
                    else -> false
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Layout Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSpinners() {
        val unitAdapter = ArrayAdapter.createFromResource(this, R.array.units_array, android.R.layout.simple_spinner_item)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        findViewById<Spinner>(R.id.sp_unit).adapter = unitAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            val storageTypes = database.storageDao().getAllStorageTypes().map { it.name }
            withContext(Dispatchers.Main) {
                val storageAdapter = ArrayAdapter(this@AddFoodActivity, android.R.layout.simple_spinner_item, storageTypes)
                storageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spStorage.adapter = storageAdapter
                
                if (storageTypes.isEmpty()) {
                    tvWarning.visibility = View.VISIBLE
                    findViewById<Button>(R.id.btnSave).isEnabled = false
                } else {
                    tvWarning.visibility = View.GONE
                    findViewById<Button>(R.id.btnSave).isEnabled = true
                }
            }
        }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedExpirationDate.set(year, month, day)
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            editText.setText(sdf.format(selectedExpirationDate.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun scheduleNotification(itemName: String, triggerTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("FOOD_NAME", itemName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            itemName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 12+ check for exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }
}