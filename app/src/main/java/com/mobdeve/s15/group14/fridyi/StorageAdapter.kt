package com.mobdeve.s15.group14.fridyi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StorageAdapter(private val storageList: List<StorageItem>) :
    RecyclerView.Adapter<StorageAdapter.StorageViewHolder>() {

    class StorageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivStorageImage: ImageView = itemView.findViewById(R.id.iv_storage_image)
        val tvStorageName: TextView = itemView.findViewById(R.id.tv_storage_name)
        val tvStorageDesc: TextView = itemView.findViewById(R.id.tv_storage_desc)
        val tvFresh: TextView = itemView.findViewById(R.id.tv_fresh_count)
        val tvExpiring: TextView = itemView.findViewById(R.id.tv_expiring_count)
        val tvCritical: TextView = itemView.findViewById(R.id.tv_critical_count)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_total_items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_storage, parent, false)
        return StorageViewHolder(view)
    }

    override fun onBindViewHolder(holder: StorageViewHolder, position: Int) {
        val item = storageList[position]
        holder.ivStorageImage.setImageResource(item.imageResId)
        holder.tvStorageName.text = item.name
        holder.tvStorageDesc.text = item.description
        holder.tvFresh.text = "${item.freshCount}/${item.totalItems}"
        holder.tvExpiring.text = "${item.expiringCount}/${item.totalItems}"
        holder.tvCritical.text = "${item.criticalCount}/${item.totalItems}"
        holder.tvTotal.text = "${item.totalItems} Items"
    }

    override fun getItemCount(): Int = storageList.size
}