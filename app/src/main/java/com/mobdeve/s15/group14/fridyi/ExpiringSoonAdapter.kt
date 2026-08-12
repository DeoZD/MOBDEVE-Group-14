package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class ExpiringSoonAdapter(private val items: List<FoodItem>) :
    RecyclerView.Adapter<ExpiringSoonAdapter.ExpiringSoonViewHolder>() {

    class ExpiringSoonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFoodName: TextView = itemView.findViewById(R.id.tv_food_name)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvQuantity: TextView = itemView.findViewById(R.id.tv_quantity)
        val tvTimeLeft: TextView = itemView.findViewById(R.id.tv_time_left)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpiringSoonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expiring_soon, parent, false)
        return ExpiringSoonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpiringSoonViewHolder, position: Int) {
        val item = items[position]
        holder.tvFoodName.text = item.name
        holder.tvLocation.text = item.storageLocation
        holder.tvQuantity.text = "${item.quantity} ${item.unit}"
        
        val diff = item.expirationDate - System.currentTimeMillis()
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        
        holder.tvTimeLeft.text = when {
            days < 0 -> "Expired"
            days == 0L -> "Today"
            days == 1L -> "Tomorrow"
            days < 7 -> "$days days"
            else -> "${days / 7} weeks"
        }

        val colorRes = if (days < 0) R.color.freshness_3 else R.color.freshness_2
        holder.tvTimeLeft.setBackgroundResource(colorRes)
    }

    override fun getItemCount(): Int = items.size
}