package com.mobdeve.s15.group14.fridyi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AddFoodActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etUnit: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDays: EditText
    private lateinit var ivSelectedImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null
    private var editingFoodId: Long = -1L

    // Register Gallery Image Picker Launcher
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivSelectedImage.setImageURI(it)
            scanImageText(it) // Automatically run OCR on selected image
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        etName = findViewById(R.id.etName)
        etQuantity = findViewById(R.id.etQuantity)
        etUnit = findViewById(R.id.etUnit)
        etLocation = findViewById(R.id.etLocation)
        etDays = findViewById(R.id.etDaysToExpire)
        ivSelectedImage = findViewById(R.id.ivSelectedImage)
        tvTitle = findViewById(R.id.tvAddFoodTitle) // Title in the layout
        btnSave = findViewById(R.id.btnSave)

        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        val btnDebugAlarm = findViewById<Button>(R.id.btnDebugAlarm)

        editingFoodId = intent.getLongExtra("FOOD_ID", -1L)

        if (editingFoodId != -1L) {
            setupEditMode()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveFoodItem(debugTriggerSeconds = null)
        }

        // DEBUG MODE: Trigger alarm in 5 seconds for demo testing
        btnDebugAlarm.setOnClickListener {
            saveFoodItem(debugTriggerSeconds = 5)
            Toast.makeText(this, "DEBUG: Expiration alarm set for 5 seconds!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupEditMode() {
        tvTitle.text = "Edit Food Item"
        btnSave.text = "Update Item"

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val item = db.foodDao().getFoodById(editingFoodId)
            
            withContext(Dispatchers.Main) {
                item?.let {
                    etName.setText(it.name)
                    etQuantity.setText(it.quantity.toString())
                    etUnit.setText(it.unit)
                    etLocation.setText(it.storageLocation)
                    
                    // Calculate remaining days
                    val diff = it.expirationDate - System.currentTimeMillis()
                    val days = TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
                    etDays.setText(days.toString())

                    if (!it.imageUri.isNullOrEmpty()) {
                        selectedImageUri = Uri.parse(it.imageUri)
                        ivSelectedImage.load(selectedImageUri)
                    }
                }
            }
        }
    }

    private fun saveFoodItem(debugTriggerSeconds: Int?) {
        val name = etName.text.toString().trim()
        val qtyStr = etQuantity.text.toString().trim()
        val unit = etUnit.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val daysStr = etDays.text.toString().trim()

        if (name.isEmpty() || qtyStr.isEmpty() || daysStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = qtyStr.toDoubleOrNull() ?: 1.0
        val daysToExpire = daysStr.toIntOrNull() ?: 1
        val currentTime = System.currentTimeMillis()

        val triggerTimeMillis: Long = if (debugTriggerSeconds != null) {
            currentTime + (debugTriggerSeconds * 1000L)
        } else {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTime
            calendar.add(Calendar.DAY_OF_YEAR, daysToExpire)
            calendar.timeInMillis
        }

        val foodItem = FoodItem(
            id = if (editingFoodId != -1L) editingFoodId else 0,
            name = name,
            quantity = quantity,
            unit = if (unit.isEmpty()) "pcs" else unit,
            storageLocation = if (location.isEmpty()) "Fridge" else location,
            dateLogged = currentTime,
            expirationDate = triggerTimeMillis,
            imageUri = selectedImageUri?.toString()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            
            if (editingFoodId != -1L) {
                db.foodDao().updateFoodItem(foodItem)
            } else {
                db.foodDao().insertFoodItem(foodItem)
            }

            scheduleNotification(name, triggerTimeMillis)

            withContext(Dispatchers.Main) {
                val message = if (editingFoodId != -1L) "Item Updated!" else "Item Saved!"
                Toast.makeText(this@AddFoodActivity, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Free, On-Device Google ML Kit Text Recognition
    private fun scanImageText(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text.lines().firstOrNull { it.isNotBlank() }
                    if (!detectedText.isNullOrEmpty()) {
                        etName.setText(detectedText.trim())
                        Toast.makeText(this, "Scanned: $detectedText", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "OCR Scan failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Optional Gemini 1.5/2.0 Flash Integration Function
    private suspend fun extractReceiptItemsGemini(bitmap: Bitmap): String? {
        return try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = "YOUR_SECURE_API_KEY",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                }
            )

            val prompt = "Analyze image. Extract item names and total price paid as JSON array of objects with keys 'itemName' and 'itemPrice'."
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }
}
