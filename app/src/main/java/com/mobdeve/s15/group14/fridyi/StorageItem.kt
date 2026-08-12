package com.mobdeve.s15.group14.fridyi

data class StorageItem(
    val name: String,
    val description: String,
    val imageResId: Int,
    val freshCount: Int,
    val expiringCount: Int,
    val criticalCount: Int,
    val totalItems: Int
)