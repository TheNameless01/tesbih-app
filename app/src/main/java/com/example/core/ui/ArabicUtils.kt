package com.example.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ArabicUtils {

    private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Formats a number to Arabic-Indic digits (١٢٣) or Western digits (123).
     */
    fun formatNumber(number: Long, useArabicIndic: Boolean): String {
        val str = number.toString()
        if (!useArabicIndic) return str
        val sb = java.lang.StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(ARABIC_INDIC_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(number: Int, useArabicIndic: Boolean): String {
        return formatNumber(number.toLong(), useArabicIndic)
    }

    /**
     * Strips Arabic diacritics (harakat/tashkeel) and normalizes letters for search.
     */
    fun normalizeArabic(input: String): String {
        if (input.isBlank()) return ""
        var s = input
            // Remove tashkeel
            .replace(Regex("[\u064B-\u0652\u0670\u0654\u0655]"), "")
            // Remove tatweel / kashida
            .replace("\u0640", "")
            // Normalize Alefs
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            // Normalize Taa Marbuta
            .replace('ة', 'ه')
            // Normalize Yaa / Alef Maqsura
            .replace('ى', 'ي')
            .trim()
            .lowercase()
        return s
    }

    /**
     * Formats timestamp to localized Arabic date time.
     */
    fun formatDateTime(epochMillis: Long, useArabicIndic: Boolean): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("ar"))
        val formatted = date.format(formatter)
        return if (useArabicIndic) {
            convertToIndic(formatted)
        } else {
            formatted
        }
    }

    fun formatDate(epochMillis: Long, useArabicIndic: Boolean): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar"))
        val formatted = date.format(formatter)
        return if (useArabicIndic) {
            convertToIndic(formatted)
        } else {
            formatted
        }
    }

    fun todayLocalDateString(): String {
        return LocalDate.now().toString()
    }

    private fun convertToIndic(text: String): String {
        val sb = java.lang.StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(ARABIC_INDIC_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
