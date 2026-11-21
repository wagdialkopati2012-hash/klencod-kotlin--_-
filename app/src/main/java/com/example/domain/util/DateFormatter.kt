package com.example.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val arabicLocale = Locale.forLanguageTag("ar")

    fun formatGregorianFull(millis: Long): String {
        val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", arabicLocale)
        return CurrencyFormatter.normalizeDigits(sdf.format(Date(millis)))
    }

    fun formatShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        val sdf = SimpleDateFormat("MMMM yyyy", arabicLocale)
        return CurrencyFormatter.normalizeDigits(sdf.format(Date(millis)))
    }
}
