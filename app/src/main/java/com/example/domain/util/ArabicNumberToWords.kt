package com.example.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

object ArabicNumberToWords {

    private val ones = arrayOf(
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة",
        "ستة", "سبعة", "ثمانية", "تسعة", "عشرة",
        "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر",
        "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    )

    private val tens = arrayOf(
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون",
        "ستون", "سبعون", "ثمانون", "تسعون"
    )

    private val hundreds = arrayOf(
        "", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة",
        "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    )

    fun convertUsdToArabicWords(amount: BigDecimal): String {
        val rounded = amount.setScale(2, RoundingMode.HALF_UP)
        val dollars = rounded.toLong()
        val cents = rounded.remainder(BigDecimal.ONE).movePointRight(2).toInt()

        if (dollars == 0L && cents == 0) {
            return "فقط صفر دولار أمريكي لا غير"
        }

        val parts = mutableListOf<String>()
        if (dollars > 0L) {
            val dollarText = when (dollars) {
                1L -> "دولار أمريكي واحد"
                2L -> "دولاران أمريكيان"
                in 3L..10L -> "${convertLong(dollars)} دولارات أمريكية"
                else -> "${convertLong(dollars)} دولاراً أمريكياً"
            }
            parts.add(dollarText)
        }

        if (cents > 0) {
            val centText = when (cents) {
                1 -> "سنت واحد"
                2 -> "سنتان"
                in 3..10 -> "${convertBelowThousand(cents)} سنتات"
                else -> "${convertBelowThousand(cents)} سنتاً"
            }
            parts.add(centText)
        }

        return "فقط " + parts.joinToString(" و") + " لا غير"
    }

    private fun convertLong(number: Long): String {
        if (number == 0L) return "صفر"
        var n = number
        val billions = (n / 1_000_000_000L).toInt()
        n %= 1_000_000_000L
        val millions = (n / 1_000_000L).toInt()
        n %= 1_000_000L
        val thousands = (n / 1_000L).toInt()
        val remainder = (n % 1_000L).toInt()

        val groups = mutableListOf<String>()

        if (billions > 0) {
            groups.add(
                when (billions) {
                    1 -> "مليار"
                    2 -> "ملياران"
                    in 3..10 -> "${convertBelowThousand(billions)} مليارات"
                    else -> "${convertBelowThousand(billions)} ملياراً"
                }
            )
        }
        if (millions > 0) {
            groups.add(
                when (millions) {
                    1 -> "مليون"
                    2 -> "مليونان"
                    in 3..10 -> "${convertBelowThousand(millions)} ملايين"
                    else -> "${convertBelowThousand(millions)} مليوناً"
                }
            )
        }
        if (thousands > 0) {
            groups.add(
                when (thousands) {
                    1 -> "ألف"
                    2 -> "ألفان"
                    in 3..10 -> "${convertBelowThousand(thousands)} آلاف"
                    else -> "${convertBelowThousand(thousands)} ألفاً"
                }
            )
        }
        if (remainder > 0) {
            groups.add(convertBelowThousand(remainder))
        }

        return groups.joinToString(" و")
    }

    private fun convertBelowThousand(number: Int): String {
        if (number == 0) return ""
        val h = number / 100
        val rem = number % 100
        val parts = mutableListOf<String>()

        if (h > 0) {
            parts.add(hundreds[h])
        }
        if (rem > 0) {
            if (rem < 20) {
                parts.add(ones[rem])
            } else {
                val u = rem % 10
                val t = rem / 10
                if (u > 0) {
                    parts.add("${ones[u]} و${tens[t]}")
                } else {
                    parts.add(tens[t])
                }
            }
        }
        return parts.joinToString(" و")
    }
}
