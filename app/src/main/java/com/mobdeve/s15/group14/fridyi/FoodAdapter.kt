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
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvExpiration: TextView = itemView.findViewById(R.id.tvExpiration)
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

        val date = Date(currentItem.expirationDate)
        val format = SimpleDateFormat("MMM-dd-yy", Locale.getDefault())
        holder.tvExpiration.text = format.format(date).uppercase()

        // Simple freshness color logic
        val daysLeft = (currentItem.expirationDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
        val colorRes = when {
            daysLeft < 0 -> R.color.freshness_3
            daysLeft < 3 -> R.color.freshness_2
            else -> R.color.freshness_1
        }
        holder.statusBar.setBackgroundResource(colorRes)
        holder.ivFoodImage.setImageResource(android.R.drawable.ic_menu_gallery)
    }

    override fun getItemCount(): Int = foodList.size

    fun getItemAt(position: Int): FoodItem = foodList[position]

    fun updateData(newFoodList: List<FoodItem>) {
        this.foodList = newFoodList
        notifyDataSetChanged()
    }
}