package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s15.group14.fridyi.databinding.ItemFoodBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodAdapter(
    private var foodList: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(val binding: ItemFoodBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(currentItem: FoodItem) {
            binding.tvFoodName.text = currentItem.name
            binding.tvStorageLocation.text = currentItem.storageLocation
            binding.tvQuantity.text = "${currentItem.quantity} ${currentItem.unit}"

            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            binding.tvDateAdded.text = "Added: ${sdf.format(Date(currentItem.dateLogged))}"

            val format = SimpleDateFormat("MMM-dd-yy", Locale.US)
            binding.tvExpiration.text = "EXP: ${format.format(Date(currentItem.expirationDate)).uppercase()}"

            val diff = currentItem.expirationDate - System.currentTimeMillis()
            val daysLeft = diff / (1000 * 60 * 60 * 24)

            binding.tvTimeLeft.text = when {
                daysLeft < 0 -> "EXPIRED"
                daysLeft == 0L -> "Expires today"
                daysLeft < 7 -> "$daysLeft days left"
                daysLeft < 30 -> "${daysLeft / 7} weeks left"
                daysLeft < 365 -> "${daysLeft / 30} months left"
                else -> "${daysLeft / 365} years left"
            }

            val colorRes = when {
                daysLeft < 0 -> R.color.freshness_3 // Red
                daysLeft < 30 -> R.color.freshness_2 // Orange
                else -> R.color.freshness_1 // Green
            }
            binding.statusBar.setBackgroundResource(colorRes)

            if (currentItem.imageUri != null) {
                binding.ivFoodImage.setImageURI(android.net.Uri.parse(currentItem.imageUri))
                binding.ivFoodImage.setPadding(0, 0, 0, 0)
            } else {
                binding.ivFoodImage.setImageResource(android.R.drawable.ic_menu_gallery)
                binding.ivFoodImage.setPadding(16, 16, 16, 16)
            }

            binding.root.setOnClickListener {
                onItemClick(currentItem)
            }
            
            binding.ibEdit.setOnClickListener {
                // Future implementation for specific edit action
                onItemClick(currentItem)
            }

            binding.ibDelete.setOnClickListener {
                // Future implementation for specific delete action
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foodList[position])
    }

    override fun getItemCount(): Int = foodList.size

    fun getItemAt(position: Int): FoodItem = foodList[position]

    fun updateData(newFoodList: List<FoodItem>) {
        this.foodList = newFoodList
        notifyDataSetChanged()
    }
}
