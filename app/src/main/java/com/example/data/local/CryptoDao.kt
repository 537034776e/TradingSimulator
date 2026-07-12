package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoDao {

    // --- User Profile (Cash Balance) ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity)

    @Query("UPDATE user_profile SET cashBalance = :newBalance WHERE id = 1")
    suspend fun updateCashBalance(newBalance: Double)

    @Query("UPDATE user_profile SET cashBalance = cashBalance + :amount, totalDeposited = totalDeposited + :amount WHERE id = 1")
    suspend fun simulateDeposit(amount: Double)

    @Query("UPDATE user_profile SET cashBalance = :exactBalance, totalDeposited = :exactBalance WHERE id = 1")
    suspend fun setExactBalance(exactBalance: Double)

    // --- Crypto Holdings ---
    @Query("SELECT * FROM crypto_holdings")
    fun getAllHoldingsFlow(): Flow<List<CryptoHoldingEntity>>

    @Query("SELECT * FROM crypto_holdings")
    suspend fun getAllHoldings(): List<CryptoHoldingEntity>

    @Query("SELECT * FROM crypto_holdings WHERE symbol = :symbol")
    suspend fun getHoldingBySymbol(symbol: String): CryptoHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: CryptoHoldingEntity)

    @Query("DELETE FROM crypto_holdings WHERE symbol = :symbol")
    suspend fun deleteHolding(symbol: String)

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // --- Combined Transaction for Buy/Sell ---
    @Transaction
    suspend fun executeTrade(
        isBuy: Boolean,
        symbol: String,
        name: String,
        quantity: Double,
        pricePerUnit: Double,
        newCashBalance: Double
    ) {
        // Update user cash balance
        updateCashBalance(newCashBalance)

        // Insert or update holdings
        val existingHolding = getHoldingBySymbol(symbol)
        if (isBuy) {
            if (existingHolding != null) {
                val totalQuantity = existingHolding.quantity + quantity
                val totalCost = (existingHolding.quantity * existingHolding.averagePurchasePrice) + (quantity * pricePerUnit)
                val avgPrice = if (totalQuantity > 0) totalCost / totalQuantity else pricePerUnit
                insertHolding(
                    CryptoHoldingEntity(
                        symbol = symbol,
                        name = name,
                        quantity = totalQuantity,
                        averagePurchasePrice = avgPrice
                    )
                )
            } else {
                insertHolding(
                    CryptoHoldingEntity(
                        symbol = symbol,
                        name = name,
                        quantity = quantity,
                        averagePurchasePrice = pricePerUnit
                    )
                )
            }
        } else {
            // Sell
            if (existingHolding != null) {
                val remainingQty = existingHolding.quantity - quantity
                if (remainingQty <= 0.0001) {
                    deleteHolding(symbol)
                } else {
                    insertHolding(
                        CryptoHoldingEntity(
                            symbol = symbol,
                            name = name,
                            quantity = remainingQty,
                            averagePurchasePrice = existingHolding.averagePurchasePrice
                        )
                    )
                }
            }
        }

        // Add to Transaction History
        insertTransaction(
            TransactionEntity(
                symbol = symbol,
                coinName = name,
                type = if (isBuy) "BUY" else "SELL",
                quantity = quantity,
                pricePerUnit = pricePerUnit,
                totalValue = quantity * pricePerUnit
            )
        )
    }

    @Transaction
    suspend fun executeCryptoToCryptoConversion(
        sourceSymbol: String,
        sourceName: String,
        sourceQuantity: Double,
        sourcePriceUsd: Double,
        targetSymbol: String,
        targetName: String,
        targetQuantity: Double,
        targetPriceUsd: Double
    ) {
        // 1. Decrease source asset
        val existingSource = getHoldingBySymbol(sourceSymbol)
        if (existingSource != null) {
            val remainingQty = existingSource.quantity - sourceQuantity
            if (remainingQty <= 0.0001) {
                deleteHolding(sourceSymbol)
            } else {
                insertHolding(
                    CryptoHoldingEntity(
                        symbol = sourceSymbol,
                        name = sourceName,
                        quantity = remainingQty,
                        averagePurchasePrice = existingSource.averagePurchasePrice
                    )
                )
            }
        }

        // 2. Increase target asset
        val existingTarget = getHoldingBySymbol(targetSymbol)
        if (existingTarget != null) {
            val totalQuantity = existingTarget.quantity + targetQuantity
            val totalCost = (existingTarget.quantity * existingTarget.averagePurchasePrice) + (targetQuantity * targetPriceUsd)
            val avgPrice = if (totalQuantity > 0) totalCost / totalQuantity else targetPriceUsd
            insertHolding(
                CryptoHoldingEntity(
                    symbol = targetSymbol,
                    name = targetName,
                    quantity = totalQuantity,
                    averagePurchasePrice = avgPrice
                )
            )
        } else {
            insertHolding(
                CryptoHoldingEntity(
                    symbol = targetSymbol,
                    name = targetName,
                    quantity = targetQuantity,
                    averagePurchasePrice = targetPriceUsd
                )
            )
        }

        // 3. Insert transaction history
        val totalValue = sourceQuantity * sourcePriceUsd
        insertTransaction(
            TransactionEntity(
                symbol = sourceSymbol,
                coinName = sourceName,
                type = "SELL",
                quantity = sourceQuantity,
                pricePerUnit = sourcePriceUsd,
                totalValue = totalValue
            )
        )
        insertTransaction(
            TransactionEntity(
                symbol = targetSymbol,
                coinName = targetName,
                type = "BUY",
                quantity = targetQuantity,
                pricePerUnit = targetPriceUsd,
                totalValue = totalValue
            )
        )
    }
}
