package com.example.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.MusabbihDatabase
import com.example.core.database.dao.DhikrUsageStat
import com.example.core.ui.ArabicUtils
import com.example.ui.theme.NotoNaskhArabicFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatisticsScreen(
    database: MusabbihDatabase,
    useArabicIndic: Boolean,
    modifier: Modifier = Modifier
) {
    val counterDao = database.counterDao()
    val todayDate = ArabicUtils.todayLocalDateString()

    val totalRepetitions by counterDao.observeTotalRepetitions().collectAsState(initial = 0L)
    val todayRepetitions by counterDao.observeTodayRepetitions(todayDate).collectAsState(initial = 0L)
    val completedSessionsCount by counterDao.observeCompletedSessionsCount().collectAsState(initial = 0)
    val uniqueDhikrCount by counterDao.observeUniqueDhikrPracticedCount().collectAsState(initial = 0)
    val mostUsedDhikr by counterDao.getMostUsedDhikr().collectAsState(initial = emptyList())

    // 7 days date range for chart
    val sevenDaysAgo = LocalDate.now().minusDays(6).toString()
    val weeklyStats by counterDao.getDailyRepetitionsSince(sevenDaysAgo).collectAsState(initial = emptyList())

    // Calculate streaks
    val streaks by produceState(initialValue = Pair(0, 0)) {
        val activeDates = counterDao.getAllActiveDates()
        value = calculateStreaks(activeDates)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "الإحصائيات والورد",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "متابعة إنجازك في التسبيح والأذكار",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Top Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(
                title = "تسبيحات اليوم",
                value = ArabicUtils.formatNumber(todayRepetitions ?: 0L, useArabicIndic),
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "إجمالي التسبيحات",
                value = ArabicUtils.formatNumber(totalRepetitions ?: 0L, useArabicIndic),
                icon = Icons.Default.Timeline,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricCard(
                title = "الأيام المتتالية (الحالي)",
                value = "${ArabicUtils.formatNumber(streaks.first, useArabicIndic)} يوم",
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = "أطول سلسلة",
                value = "${ArabicUtils.formatNumber(streaks.second, useArabicIndic)} يوم",
                icon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weekly Activity Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoGraph,
                        contentDescription = "النشاط الأسبوعي",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نشاط آخر ٧ أيام",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Build weekly bar chart
                val maxDaily = (weeklyStats.maxOfOrNull { it.totalRepetitions } ?: 1L).coerceAtLeast(10L)
                val dayList = (0..6).map { i ->
                    val date = LocalDate.now().minusDays((6 - i).toLong())
                    val dateStr = date.toString()
                    val reps = weeklyStats.find { it.localDate == dateStr }?.totalRepetitions ?: 0L
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ar"))
                    Triple(dayName, reps, dateStr == todayDate)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayList.forEach { (dayName, reps, isToday) ->
                        val ratio = (reps.toFloat() / maxDaily.toFloat()).coerceIn(0.06f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (reps > 0) {
                                Text(
                                    text = ArabicUtils.formatNumber(reps, useArabicIndic),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height((100 * ratio).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Most Used Dhikr
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "الأذكار الأكثر تكراراً",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (mostUsedDhikr.isEmpty()) {
                    Text(
                        text = "لا توجد أذكار مسجلة بعد. ابدأ بالتسبيح الآن لتظهر إحصائياتك هنا.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    val highestCount = mostUsedDhikr.firstOrNull()?.totalCount?.coerceAtLeast(1L) ?: 1L
                    mostUsedDhikr.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title ?: item.arabicText.take(24) + "...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${ArabicUtils.formatNumber(item.totalCount, useArabicIndic)} مرة",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (item.totalCount.toFloat() / highestCount.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extra Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "جلسات مكتملة: ${ArabicUtils.formatNumber(completedSessionsCount, useArabicIndic)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "أذكار مُمَارسة: ${ArabicUtils.formatNumber(uniqueDhikrCount, useArabicIndic)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Calculates current streak and maximum streak in days.
 */
private fun calculateStreaks(activeDatesDesc: List<String>): Pair<Int, Int> {
    if (activeDatesDesc.isEmpty()) return Pair(0, 0)

    val dateSet = activeDatesDesc.mapNotNull {
        try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            null
        }
    }.toSortedSet()

    if (dateSet.isEmpty()) return Pair(0, 0)

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    // Current streak
    var currentStreak = 0
    var checkDate = if (dateSet.contains(today)) today else if (dateSet.contains(yesterday)) yesterday else null

    while (checkDate != null && dateSet.contains(checkDate)) {
        currentStreak++
        checkDate = checkDate.minusDays(1)
    }

    // Longest streak
    var longestStreak = 0
    var tempStreak = 0
    var prevDate: LocalDate? = null

    for (d in dateSet) {
        if (prevDate == null || d.isEqual(prevDate.plusDays(1))) {
            tempStreak++
        } else {
            tempStreak = 1
        }
        if (tempStreak > longestStreak) longestStreak = tempStreak
        prevDate = d
    }

    return Pair(currentStreak, longestStreak)
}
