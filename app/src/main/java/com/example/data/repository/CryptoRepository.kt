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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException

class CryptoRepository(
    private val cryptoDao: CryptoDao,
    private val apiService: CoinLoreApiService
) {

    // Reactive StateFlow cache of the latest fetched tickers to support robust offline viewing and real-time streaming
    private val _tickersFlow = MutableStateFlow<List<CoinLoreCoinDto>>(emptyList())
    val cachedTickers: List<CoinLoreCoinDto> get() = _tickersFlow.value

    /**
     * Periodically called to fetch latest price updates from the server and instantly update state.
     */
    suspend fun fetchLatestPrices() {
        try {
            val response = apiService.getTickers(start = 0, limit = 50)
            _tickersFlow.value = response.data
        } catch (e: IOException) {
            // Soft fail: preserve existing cache to avoid screen flickering/errors
        } catch (e: Exception) {
            // Soft fail
        }
    }

    /**
     * Retrieves the list of coins, merging real-time market prices with the local database holdings.
     * Incorporates network error handling and offline graceful degradation as per guidelines.
     */
    fun getCoinsFlow(forceRefresh: Boolean): Flow<Resource<List<CryptoCoin>>> = flow {
        // Emit initial status with cached data or empty
        emit(Resource.Loading(_tickersFlow.value.map { mapDtoToDomain(it, emptyList()) }))

        // Ensure default user profile is initialized
        initializeUserProfileIfNeeded()

        if (forceRefresh || _tickersFlow.value.isEmpty()) {
            try {
                val response = apiService.getTickers(start = 0, limit = 50)
                _tickersFlow.value = response.data
            } catch (e: IOException) {
                // Network error (offline state)
                emit(Resource.Error("Errore di rete. Visualizzando dati memorizzati.", _tickersFlow.value.map { mapDtoToDomain(it, emptyList()) }))
            } catch (e: Exception) {
                emit(Resource.Error("Errore sconosciuto durante il caricamento dei prezzi.", _tickersFlow.value.map { mapDtoToDomain(it, emptyList()) }))
            }
        }

        // Combine the tickers and database holdings so that ANY update in either causes an instant emission
        combine(
            _tickersFlow,
            cryptoDao.getAllHoldingsFlow()
        ) { tickers, holdings ->
            if (tickers.isEmpty() && holdings.isNotEmpty()) {
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
                tickers.map { dto ->
                    mapDtoToDomain(dto, holdings)
                }
            }
        }.collect { mergedCoins ->
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

    suspend fun updateCashBalance(newBalance: Double) {
        cryptoDao.updateCashBalance(newBalance)
    }
}

// Sealed wrapper class representing states cleanly as mandated for proper UI state representations
sealed class Resource<out T> {
    data class Loading<out T>(val data: T? = null) : Resource<T>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error<out T>(val message: String, val data: T? = null) : Resource<T>()
}
