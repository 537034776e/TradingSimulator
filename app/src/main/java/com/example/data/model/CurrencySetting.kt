package com.example.data.model

enum class CurrencySetting(
    val symbol: String,
    val code: String,
    val usdToCurrencyRate: Double,
    val isSymbolSuffix: Boolean = false
) {
    USD("$", "USD", 1.0),
    EUR("€", "EUR", 0.92),
    JPY("¥", "JPY", 158.0),
    GBP("£", "GBP", 0.79),
    CHF("Fr.", "CHF", 0.89, true);

    fun format(valueInUsd: Double): String {
        val converted = valueInUsd * usdToCurrencyRate
        val formattedValue = if (this == JPY) {
            String.format("%,.0f", converted)
        } else {
            String.format("%,.2f", converted)
        }
        return if (isSymbolSuffix) {
            "$formattedValue $symbol"
        } else {
            "$symbol$formattedValue"
        }
    }
}
