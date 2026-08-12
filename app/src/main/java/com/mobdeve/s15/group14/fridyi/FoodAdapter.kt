package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodAdapter(
    private var foodList: List<FoodItem>,
    private val onItemClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFoodImage: ImageView = itemView.findViewById(R.id.ivFoodImage)
        val tvName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvExpiration: TextView = itemView.findViewById(R.id.tvExpiration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val currentItem = foodList[position]

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }

        holder.tvName.text = currentItem.name
        holder.tvQuantity.text = "${currentItem.quantity} ${currentItem.unit}"

        val date = Date(currentItem.expirationDate)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvExpiration.text = "Exp: ${format.format(date)}"

        // Load image using Coil
        if (!currentItem.imageUri.isNullOrEmpty()) {
            holder.ivFoodImage.load(currentItem.imageUri) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_camera)
                error(android.R.drawable.ic_menu_camera)
            }
        } else {
            holder.ivFoodImage.setImageResource(android.R.drawable.ic_menu_camera)
        }
    }

    override fun getItemCount(): Int = foodList.size

    fun getItemAt(position: Int): FoodItem = foodList[position]

    fun updateData(newFoodList: List<FoodItem>) {
        this.foodList = newFoodList
        notifyDataSetChanged()
    }
}
