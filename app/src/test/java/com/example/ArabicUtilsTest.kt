package com.example

import com.example.core.ui.ArabicUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicUtilsTest {

    @Test
    fun testFormatNumberArabicIndic() {
        assertEquals("١٢٣", ArabicUtils.formatNumber(123L, true))
        assertEquals("٠", ArabicUtils.formatNumber(0L, true))
        assertEquals("١٠٠", ArabicUtils.formatNumber(100L, true))
    }

    @Test
    fun testFormatNumberWestern() {
        assertEquals("123", ArabicUtils.formatNumber(123L, false))
        assertEquals("0", ArabicUtils.formatNumber(0L, false))
    }

    @Test
    fun testNormalizeArabic() {
        // Tashkeel removal and normalization
        val original = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ"
        val normalized = ArabicUtils.normalizeArabic(original)
        assertEquals("سبحان الله وبحمده", normalized)

        val alef = "أَكْبَرُ"
        assertEquals("اكبر", ArabicUtils.normalizeArabic(alef))
    }
}
