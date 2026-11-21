package com.example.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    private val usSymbols = DecimalFormatSymbols(Locale.US)

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                '٫' -> sb.append('.')
                '،' -> sb.append(',')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun parseBigDecimal(input: String): BigDecimal? {
        val cleaned = normalizeDigits(input)
            .replace("$", "")
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun parsePositiveBigDecimal(input: String): BigDecimal? {
        val value = parseBigDecimal(input) ?: return null
        return if (value > BigDecimal.ZERO) value else null
    }

    fun formatUsd(amount: BigDecimal): String {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        val hasFraction = scaled.stripTrailingZeros().scale() > 0
        val pattern = if (hasFraction) "#,##0.00" else "#,##0"
        val df = DecimalFormat(pattern, usSymbols)
        return "$${df.format(scaled)}"
    }

    fun formatUsd(amountStr: String): String {
        val amount = parseBigDecimal(amountStr) ?: BigDecimal.ZERO
        return formatUsd(amount)
    }

    fun formatQuantity(quantity: BigDecimal): String {
        val stripped = quantity.stripTrailingZeros()
        val pattern = if (stripped.scale() > 0) "#,##0.###" else "#,##0"
        return DecimalFormat(pattern, usSymbols).format(stripped)
    }

    fun formatQuantity(quantityStr: String): String {
        val qty = parseBigDecimal(quantityStr) ?: return ""
        return formatQuantity(qty)
    }

    fun calculateLineTotal(unitPrice: BigDecimal, quantity: BigDecimal): BigDecimal {
        return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP)
    }

    fun toCanonicalString(value: BigDecimal, scale: Int = 2): String {
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }
}
