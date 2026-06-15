package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto_holdings")
data class CryptoHoldingEntity(
    @PrimaryKey val symbol: String, // e.g. "BTC"
    val name: String,               // e.g. "Bitcoin"
    val quantity: Double,           // amount owned
    val averagePurchasePrice: Double // average buy price
)
