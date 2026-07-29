package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FoodAdapter(private var foodList: List<FoodItem>) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        // Bind basic text
        holder.tvName.text = currentItem.name
        holder.tvQuantity.text = "${currentItem.quantity} ${currentItem.unit}"

        // Convert the Long timestamp back to a readable date string
        val date = Date(currentItem.expirationDate)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvExpiration.text = "Exp: ${format.format(date)}"
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    fun getItemAt(position: Int): FoodItem {
        return foodList[position]
    }

    fun updateData(newFoodList: List<FoodItem>) {
        this.foodList = newFoodList
        notifyDataSetChanged()
    }
}