package com.example.data.model

import java.util.Locale
import java.math.BigDecimal
import kotlin.math.abs

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
    CHF("Fr.", "CHF", 0.89, true),
    AUD("$", "AUD", 1.50),
    CAD("$", "CAD", 1.37),
    INR("₹", "INR", 83.5),
    CNY("¥", "CNY", 7.27),
    BRL("R$", "BRL", 5.40);

    fun format(valueInUsd: Double): String {
        val converted = valueInUsd * usdToCurrencyRate
        val formattedValue = if (this == JPY) {
            if (converted > 0.0 && converted < 1.0) {
                formatSmallValue(converted)
            } else {
                String.format(Locale.getDefault(), "%,.0f", converted)
            }
        } else {
            if (converted > 0.0 && converted < 1.0) {
                formatSmallValue(converted)
            } else {
                String.format(Locale.getDefault(), "%,.2f", converted)
            }
        }
        return if (isSymbolSuffix) {
            "$formattedValue $symbol"
        } else {
            "$symbol$formattedValue"
        }
    }

    private fun formatSmallValue(value: Double): String {
        val absValue = abs(value)
        if (absValue == 0.0) return "0.00"
        
        val bd = try {
            BigDecimal(absValue.toString())
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
        val plain = bd.toPlainString()
        val dotIndex = plain.indexOf('.')
        if (dotIndex == -1) {
            return String.format(Locale.getDefault(), "%,.2f", value)
        }
        
        var scale = 2
        for (i in (dotIndex + 1) until plain.length) {
            if (plain[i] != '0') {
                scale = i - dotIndex
                break
            }
        }
        val finalScale = maxOf(2, scale)
        return String.format(Locale.getDefault(), "%.${finalScale}f", value)
    }
}
