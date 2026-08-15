package com.mobdeve.s15.group14.fridyi

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mobdeve.s15.group14.fridyi.databinding.ActivityAddFoodBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddFoodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFoodBinding
    private lateinit var database: AppDatabase
    private var selectedImageUri: Uri? = null
    private var selectedExpirationDate: Calendar = Calendar.getInstance()
    private var isEditMode = false
    private var foodId: Long = 0L
    private var originalDateLogged: Long = System.currentTimeMillis()

    // OCR Image Picker for "Simple Scan"
    private val ocrImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivItemPreview.setImageURI(it)
            binding.ivItemPreview.setPadding(0, 0, 0, 0)
            processImageForTextRecognition(it)
        }
    }

    // Standard Gallery Picker for Item Image
    private val galleryImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivItemPreview.setImageURI(it)
            binding.ivItemPreview.setPadding(0, 0, 0, 0)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                binding.ivItemPreview.setImageBitmap(bitmap)
                binding.ivItemPreview.setPadding(0, 0, 0, 0)
                // In a real app, you'd save this bitmap to a file and use its URI
                selectedImageUri = Uri.parse("content://camera_captured_bitmap")
            }
        }
    }

    // Notification Permission Request (Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scheduleNotification("Test Food Item", System.currentTimeMillis() + 5000)
            Toast.makeText(this, "Notification scheduled for 5 seconds from now", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission is required for alerts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
        foodId = intent.getLongExtra("FOOD_ID", 0L)

        if (isEditMode) {
            binding.tvTitle.text = "Edit Item"
            binding.btnSave.text = "Update Item"
        }

        setupSpinners()
        setupListeners()
        setupBottomNavigation()
    }

    private fun setupSpinners() {
        // Unit Spinner
        val unitAdapter = ArrayAdapter.createFromResource(this, R.array.units_array, android.R.layout.simple_spinner_item)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spUnit.adapter = unitAdapter

        // Storage Spinner from Database
        lifecycleScope.launch(Dispatchers.IO) {
            val storageTypes = database.storageDao().getAllStorageTypes().map { it.name }
            val foodItem = if (isEditMode) database.foodDao().getFoodItemById(foodId) else null
            
            withContext(Dispatchers.Main) {
                val storageAdapter = ArrayAdapter(this@AddFoodActivity, android.R.layout.simple_spinner_item, storageTypes)
                storageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spStorage.adapter = storageAdapter
                
                if (storageTypes.isEmpty()) {
                    binding.tvNoStorageWarning.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                } else {
                    binding.tvNoStorageWarning.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }

                if (isEditMode && foodItem != null) {
                    prefillForm(foodItem)
                }
            }
        }
    }

    private fun prefillForm(item: FoodItem) {
        originalDateLogged = item.dateLogged
        binding.etName.setText(item.name)
        binding.etQuantity.setText(item.quantity.toString())
        
        // Unit Spinner
        val unitAdapter = binding.spUnit.adapter
        if (unitAdapter != null) {
            for (i in 0 until unitAdapter.count) {
                if (unitAdapter.getItem(i).toString() == item.unit) {
                    binding.spUnit.setSelection(i)
                    break
                }
            }
        }

        // Storage Spinner
        val storageAdapter = binding.spStorage.adapter
        if (storageAdapter != null) {
            for (i in 0 until storageAdapter.count) {
                if (storageAdapter.getItem(i).toString() == item.storageLocation) {
                    binding.spStorage.setSelection(i)
                    break
                }
            }
        }

        // Expiration Date
        selectedExpirationDate.timeInMillis = item.expirationDate
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        binding.etExpiration.setText(sdf.format(selectedExpirationDate.time))

        // Image
        item.imageUri?.let { uriString ->
            if (uriString.isNotEmpty() && uriString != "content://camera_captured_bitmap") {
                selectedImageUri = Uri.parse(uriString)
                binding.ivItemPreview.setImageURI(selectedImageUri)
                binding.ivItemPreview.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun setupListeners() {
        // Simple Scan -> Opens Image Selection for OCR
        binding.llSimpleScan.setOnClickListener {
            ocrImagePickerLauncher.launch("image/*")
        }

        // Gallery Button
        binding.btnGallery.setOnClickListener {
            galleryImagePickerLauncher.launch("image/*")
        }

        // Camera Button
        binding.btnTakePhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }

        // Expiration Date Picker
        binding.etExpiration.setOnClickListener {
            showDatePicker()
        }

        // Debug Test Notification Button (5 Seconds)
        binding.btnTestNotification.setOnClickListener {
            checkAndTriggerTestNotification()
        }

        // Save Food Button
        binding.btnSave.setOnClickListener {
            saveFoodItem()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_add
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedExpirationDate.set(year, month, day, 0, 0, 0)
            selectedExpirationDate.set(Calendar.MILLISECOND, 0)
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            binding.etExpiration.setText(sdf.format(selectedExpirationDate.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun processImageForTextRecognition(uri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, uri)
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to load image for scanning: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Scanning image for text...", Toast.LENGTH_SHORT).show()

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedCandidates = mutableListOf<String>()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val cleanedText = line.text.trim()
                        if (cleanedText.isNotEmpty() && cleanedText.length > 1 && !detectedCandidates.contains(cleanedText)) {
                            detectedCandidates.add(cleanedText)
                        }
                    }
                }

                when {
                    detectedCandidates.isEmpty() -> {
                        Toast.makeText(this, "No readable text found in image.", Toast.LENGTH_LONG).show()
                    }
                    detectedCandidates.size == 1 -> {
                        val singleText = detectedCandidates[0]
                        binding.etName.setText(singleText)
                        Toast.makeText(this, "Detected name: $singleText", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        showTextSelectionOverlay(detectedCandidates)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Text scanning failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTextSelectionOverlay(candidates: List<String>) {
        val itemsArray = candidates.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Food Item Name")
            .setItems(itemsArray) { _, which ->
                val selectedText = itemsArray[which]
                binding.etName.setText(selectedText)
                Toast.makeText(this, "Applied: $selectedText", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun checkAndTriggerTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        val foodName = binding.etName.text.toString().trim().ifEmpty { "Test Food Item" }
        scheduleNotification(foodName, System.currentTimeMillis() + 5000)
        Toast.makeText(this, "Notification scheduled for 5 seconds from now", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleNotification(itemName: String, triggerTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_FOOD_NAME, itemName)
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, itemName.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            itemName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

    private fun saveFoodItem() {
        val name = binding.etName.text.toString().trim()
        val quantityStr = binding.etQuantity.text.toString().trim()
        val quantity = quantityStr.toDoubleOrNull() ?: 0.0
        val unit = binding.spUnit.selectedItem?.toString() ?: ""
        val storage = if (binding.spStorage.adapter != null && binding.spStorage.adapter.count > 0) binding.spStorage.selectedItem.toString() else ""
        val expirationStr = binding.etExpiration.text.toString().trim()

        if (name.isEmpty() || quantity <= 0 || storage.isEmpty() || expirationStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTime = if (isEditMode) originalDateLogged else System.currentTimeMillis()
        
        val foodItem = FoodItem(
            id = if (isEditMode) foodId else 0L,
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
                    val message = if (isEditMode) "Item Updated Successfully!" else "Item Saved Successfully!"
                    Toast.makeText(this@AddFoodActivity, message, Toast.LENGTH_SHORT).show()
                    
                    if (isEditMode) {
                        finish() // Return to previous screen after editing
                    } else {
                        // Clear fields instead of finishing for new items
                        binding.etName.text.clear()
                        binding.etQuantity.text.clear()
                        binding.etExpiration.text.clear()
                        binding.ivItemPreview.setImageResource(android.R.drawable.ic_menu_gallery)
                        binding.ivItemPreview.setPadding(16, 16, 16, 16)
                        selectedImageUri = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddFoodActivity, "Error saving item: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
