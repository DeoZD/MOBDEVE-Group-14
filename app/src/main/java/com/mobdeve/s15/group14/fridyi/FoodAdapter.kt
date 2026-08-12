package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodAdapter(private var foodList: List<FoodItem>) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFoodImage: ImageView = itemView.findViewById(R.id.iv_food_image)
        val tvName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_storage_location)
        val tvDateAdded: TextView = itemView.findViewById(R.id.tv_date_added)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvExpiration: TextView = itemView.findViewById(R.id.tvExpiration)
        val tvTimeLeft: TextView = itemView.findViewById(R.id.tv_time_left)
        val statusBar: View = itemView.findViewById(R.id.status_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val currentItem = foodList[position]

        holder.tvName.text = currentItem.name
        holder.tvLocation.text = currentItem.storageLocation
        holder.tvQuantity.text = "${currentItem.quantity} ${currentItem.unit}"

        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        holder.tvDateAdded.text = "Added: ${sdf.format(Date(currentItem.dateLogged))}"

        val format = SimpleDateFormat("MMM-dd-yy", Locale.US)
        holder.tvExpiration.text = "EXP: ${format.format(Date(currentItem.expirationDate)).uppercase()}"

        val diff = currentItem.expirationDate - System.currentTimeMillis()
        val daysLeft = diff / (1000 * 60 * 60 * 24)

        holder.tvTimeLeft.text = when {
            daysLeft < 0 -> "EXPIRED"
            daysLeft < 7 -> "$daysLeft days left"
            daysLeft < 30 -> "${daysLeft / 7} weeks left"
            daysLeft < 365 -> "${daysLeft / 30} months left"
            else -> "${daysLeft / 365} years left"
        }

        val colorRes = when {
            daysLeft < 0 -> R.color.freshness_3 // Red
            daysLeft < 30 -> R.color.freshness_2 // Orange (Requirement says < 1 month is expiring soon)
            else -> R.color.freshness_1 // Green
        }
        holder.statusBar.setBackgroundResource(colorRes)
        
        if (currentItem.imageUri != null) {
            holder.ivFoodImage.setImageURI(android.net.Uri.parse(currentItem.imageUri))
            holder.ivFoodImage.setPadding(0, 0, 0, 0)
        } else {
            holder.ivFoodImage.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.ivFoodImage.setPadding(16, 16, 16, 16)
        }
    }

    override fun getItemCount(): Int = foodList.size

    fun getItemAt(position: Int): FoodItem = foodList[position]

    fun updateData(newFoodList: List<FoodItem>) {
        this.foodList = newFoodList
        notifyDataSetChanged()
    }
}