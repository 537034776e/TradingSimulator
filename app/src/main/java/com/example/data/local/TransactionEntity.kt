package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val coinName: String,
    val type: String, // "BUY" or "SELL"
    val quantity: Double,
    val pricePerUnit: Double,
    val totalValue: Double,
    val timestamp: Long = System.currentTimeMillis()
)
