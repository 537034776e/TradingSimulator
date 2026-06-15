package com.example.data.repository

import com.example.data.local.CryptoDao
import com.example.data.local.CryptoHoldingEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.CryptoCoin
import com.example.data.remote.CoinLoreApiService
import com.example.data.remote.CoinLoreCoinDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

class CryptoRepository(
    private val cryptoDao: CryptoDao,
    private val apiService: CoinLoreApiService
) {

    // In-memory cache of the latest fetched tickers to support robust offline viewing of market rates
    private var cachedTickers: List<CoinLoreCoinDto> = emptyList()

    /**
     * Retrieves the list of coins, merging real-time market prices with the local database holdings.
     * Incorporates network error handling and offline graceful degradation as per guidelines.
     */
    fun getCoinsFlow(forceRefresh: Boolean): Flow<Resource<List<CryptoCoin>>> = flow {
        emit(Resource.Loading(cachedTickers.map { mapDtoToDomain(it, emptyList()) }))

        // Ensure default user profile is initialized
        initializeUserProfileIfNeeded()

        if (forceRefresh || cachedTickers.isEmpty()) {
            try {
                val response = apiService.getTickers(start = 0, limit = 50)
                cachedTickers = response.data
            } catch (e: IOException) {
                // Network error (offline state)
                emit(Resource.Error("Errore di rete. Visualizzando dati memorizzati.", cachedTickers.map { mapDtoToDomain(it, emptyList()) }))
            } catch (e: Exception) {
                emit(Resource.Error("Errore sconosciuto durante il caricamento dei prezzi.", cachedTickers.map { mapDtoToDomain(it, emptyList()) }))
            }
        }

        // Now, observe the local database holdings and merge them dynamically
        // so that any change in holdings updates the list of coins reactively!
        cryptoDao.getAllHoldingsFlow().collect { holdings ->
            val mergedCoins = if (cachedTickers.isEmpty() && holdings.isNotEmpty()) {
                // Entirely offline with no ticker cache, construct a basic coin representation from holdings
                holdings.map { holding ->
                    CryptoCoin(
                        id = holding.symbol,
                        symbol = holding.symbol,
                        name = holding.name,
                        nameId = holding.name.lowercase(),
                        rank = 999,
                        priceUsd = holding.averagePurchasePrice, // Fallback to buy price if offline
                        percentChange24h = 0.0,
                        imageUrl = getIconUrl(holding.symbol),
                        quantityOwned = holding.quantity,
                        averagePurchasePrice = holding.averagePurchasePrice
                    )
                }
            } else {
                cachedTickers.map { dto ->
                    mapDtoToDomain(dto, holdings)
                }
            }

            emit(Resource.Success(mergedCoins))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Clean mapping logic separating API Dto data representations from the Domain layer representation.
     */
    private fun mapDtoToDomain(dto: CoinLoreCoinDto, holdings: List<CryptoHoldingEntity>): CryptoCoin {
        val holding = holdings.find { it.symbol.equals(dto.symbol, ignoreCase = true) }
        val price = dto.priceUsd.toDoubleOrNull() ?: 0.0
        val change = dto.percentChange24h.toDoubleOrNull() ?: 0.0
        return CryptoCoin(
            id = dto.id,
            symbol = dto.symbol,
            name = dto.name,
            nameId = dto.nameId,
            rank = dto.rank,
            priceUsd = price,
            percentChange24h = change,
            imageUrl = getIconUrl(dto.symbol),
            quantityOwned = holding?.quantity ?: 0.0,
            averagePurchasePrice = holding?.averagePurchasePrice ?: 0.0
        )
    }

    private fun getIconUrl(symbol: String): String {
        // High fidelity asset loaders using absolute coin cap icons
        return "https://assets.coincap.io/assets/icons/${symbol.lowercase()}@2x.png"
    }

    private suspend fun initializeUserProfileIfNeeded() {
        val profile = cryptoDao.getUserProfile()
        if (profile == null) {
            cryptoDao.insertUserProfile(UserProfileEntity())
        }
    }

    // --- Local DB Streams ---
    fun getUserProfileFlow(): Flow<UserProfileEntity?> = cryptoDao.getUserProfileFlow()

    fun getAllHoldingsFlow(): Flow<List<CryptoHoldingEntity>> = cryptoDao.getAllHoldingsFlow()

    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = cryptoDao.getAllTransactionsFlow()

    suspend fun executeTrade(
        isBuy: Boolean,
        symbol: String,
        name: String,
        quantity: Double,
        pricePerUnit: Double,
        newCashBalance: Double
    ) {
        cryptoDao.executeTrade(isBuy, symbol, name, quantity, pricePerUnit, newCashBalance)
    }
}

// Sealed wrapper class representing states cleanly as mandated for proper UI state representations
sealed class Resource<out T> {
    data class Loading<out T>(val data: T? = null) : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error<out T>(val message: String, val data: T? = null) : Resource<T>()
}
