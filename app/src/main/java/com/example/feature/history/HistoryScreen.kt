package com.example.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.MusabbihDatabase
import com.example.core.database.entity.CounterSessionEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.ui.ArabicUtils
import com.example.ui.theme.NotoNaskhArabicFamily
import java.time.LocalDate
import java.time.ZoneId

enum class HistoryFilter(val label: String) {
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    ALL("الكل")
}

@Composable
fun HistoryScreen(
    database: MusabbihDatabase,
    useArabicIndic: Boolean,
    modifier: Modifier = Modifier
) {
    val counterDao = database.counterDao()
    val dhikrDao = database.dhikrDao()

    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedSessionForDetail by remember { mutableStateOf<Pair<CounterSessionEntity, DhikrEntity?>?>(null) }

    val allSessions by counterDao.getAllSessions().collectAsState(initial = emptyList())

    // Filter sessions based on timestamp
    val now = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val todayStartMillis = now.atStartOfDay(zone).toInstant().toEpochMilli()
    val weekStartMillis = now.minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
    val monthStartMillis = now.minusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()

    val filteredSessions = allSessions.filter { s ->
        when (selectedFilter) {
            HistoryFilter.TODAY -> s.startedAt >= todayStartMillis
            HistoryFilter.THIS_WEEK -> s.startedAt >= weekStartMillis
            HistoryFilter.THIS_MONTH -> s.startedAt >= monthStartMillis
            HistoryFilter.ALL -> true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Screen Header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "سجل الجلسات",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "أرشيف جلسات الذكر والتسبيح السابقة",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HistoryFilter.values()) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History List or Empty State
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "السجل فارغ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد جلسات مسجلة في هذه الفترة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions, key = { it.id }) { session ->
                    val dhikr by produceState<DhikrEntity?>(initialValue = null, key1 = session.dhikrId) {
                        value = dhikrDao.getDhikrById(session.dhikrId)
                    }

                    HistorySessionCard(
                        session = session,
                        dhikr = dhikr,
                        useArabicIndic = useArabicIndic,
                        onClick = {
                            selectedSessionForDetail = Pair(session, dhikr)
                        }
                    )
                }
            }
        }
    }

    // Session Detail Dialog
    if (selectedSessionForDetail != null) {
        val (session, dhikr) = selectedSessionForDetail!!
        AlertDialog(
            onDismissRequest = { selectedSessionForDetail = null },
            title = {
                Text(
                    text = "تفاصيل الجلسة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = dhikr?.title ?: "جلسة تسبيح",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dhikr?.arabicText ?: "",
                        fontFamily = NotoNaskhArabicFamily,
                        fontSize = 17.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "العدد المنجز: ${ArabicUtils.formatNumber(session.currentCount, useArabicIndic)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (session.targetCount != null) {
                        Text(
                            text = "المستهدف: ${ArabicUtils.formatNumber(session.targetCount, useArabicIndic)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "وقت البدء: ${ArabicUtils.formatDateTime(session.startedAt, useArabicIndic)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session.completedAt != null) {
                        Text(
                            text = "وقت الإتمام: ${ArabicUtils.formatDateTime(session.completedAt, useArabicIndic)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val statusText = when (session.status) {
                        "COMPLETED" -> "مكتملة ✓"
                        "ACTIVE" -> "نشطة حالياً"
                        else -> "موقوفة مؤقتاً"
                    }
                    Text(
                        text = "الحالة: $statusText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSessionForDetail = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun HistorySessionCard(
    session: CounterSessionEntity,
    dhikr: DhikrEntity?,
    useArabicIndic: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dhikr?.title ?: (dhikr?.arabicText?.take(30) ?: "تسبيح"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (session.status) {
                        "COMPLETED" -> Icons.Default.CheckCircle
                        "ACTIVE" -> Icons.Default.HourglassBottom
                        else -> Icons.Default.PauseCircle
                    }
                    val statusColor = when (session.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = session.status,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ArabicUtils.formatDateTime(session.startedAt, useArabicIndic),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ArabicUtils.formatNumber(session.currentCount, useArabicIndic),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (session.targetCount != null) {
                    Text(
                        text = "/ ${ArabicUtils.formatNumber(session.targetCount, useArabicIndic)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
