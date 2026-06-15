package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinLoreResponse(
    @Json(name = "data") val data: List<CoinLoreCoinDto>
)

@JsonClass(generateAdapter = true)
data class CoinLoreCoinDto(
    @Json(name = "id") val id: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "nameid") val nameId: String,
    @Json(name = "rank") val rank: Int,
    @Json(name = "price_usd") val priceUsd: String,
    @Json(name = "percent_change_24h") val percentChange24h: String,
    @Json(name = "percent_change_1h") val percentChange1h: String?,
    @Json(name = "percent_change_7d") val percentChange7d: String?,
    @Json(name = "market_cap_usd") val marketCapUsd: String?,
    @Json(name = "volume24") val volume24: Double?,
    @Json(name = "csupply") val csupply: String?
)
