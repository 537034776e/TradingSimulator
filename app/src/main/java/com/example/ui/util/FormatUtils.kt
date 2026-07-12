package com.example.ui.util

import java.math.BigDecimal
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
    /**
     * Formats crypto quantities based on value:
     * - If value is very small (< 1.0 and > 0.0), it shows all digits after the decimal point
     *   up to the first digit greater than 0.
     * - Otherwise (value == 0 or value >= 1.0), it shows exactly two decimal places.
     */
    fun formatCryptoQuantity(value: Double): String {
        val absValue = abs(value)
        if (absValue == 0.0) return "0.00"
        if (absValue >= 1.0) {
            return String.format(Locale.getDefault(), "%.2f", value)
        }
        
        val bd = try {
            BigDecimal(absValue.toString())
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
        val plain = bd.toPlainString()
        val dotIndex = plain.indexOf('.')
        if (dotIndex == -1) {
            return String.format(Locale.getDefault(), "%.2f", value)
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
