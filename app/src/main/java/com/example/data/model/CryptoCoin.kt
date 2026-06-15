package com.example.data.model

data class CryptoCoin(
    val id: String,
    val symbol: String,
    val name: String,
    val nameId: String,
    val rank: Int,
    val priceUsd: Double,
    val percentChange24h: Double,
    val imageUrl: String,
    // Add portfolio info if owned
    val quantityOwned: Double = 0.0,
    val averagePurchasePrice: Double = 0.0
) {
    val totalValue: Double get() = quantityOwned * priceUsd
    val profitLoss: Double get() = if (quantityOwned > 0) totalValue - (quantityOwned * averagePurchasePrice) else 0.0
    val profitLossPercent: Double get() = if (quantityOwned > 0 && averagePurchasePrice > 0) ((priceUsd - averagePurchasePrice) / averagePurchasePrice) * 100.0 else 0.0
}
