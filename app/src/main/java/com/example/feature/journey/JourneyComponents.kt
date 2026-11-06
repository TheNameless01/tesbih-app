package com.example.feature.journey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.entity.DhikrEntity
import com.example.core.database.entity.DhikrJourneyItemEntity
import com.example.core.engine.JourneyUiState
import com.example.core.ui.ArabicUtils
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.NotoNaskhArabicFamily
import com.example.ui.theme.SuccessGreen

/**
 * Compact top bar when a Guided Journey is active
 */
@Composable
fun GuidedJourneyHeader(
    journeyTitle: String,
    currentIndex: Int,
    totalItems: Int,
    overallProgress: Float,
    useArabicIndic: Boolean,
    onOpenList: () -> Unit,
    onExitJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "رحلة $journeyTitle",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الذكر ${ArabicUtils.formatNumber((currentIndex + 1).toLong(), useArabicIndic)} من ${ArabicUtils.formatNumber(totalItems.toLong(), useArabicIndic)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenList,
                        modifier = Modifier.size(28.dp).testTag("journey_open_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = "قائمة الأذكار",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onExitJourney,
                        modifier = Modifier.size(28.dp).testTag("journey_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إنهاء الرحلة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Overall Journey Progress Bar
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Compact navigation bar below the counter during a journey
 */
@Composable
fun JourneyNavigationControls(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onOpenList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        OutlinedButton(
            onClick = onPrevious,
            enabled = hasPrevious,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("journey_prev_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "السابق",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("السابق", style = MaterialTheme.typography.labelMedium)
        }

        // Dhikr List Button
        FilledTonalButton(
            onClick = onOpenList,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("journey_list_button")
        ) {
            Icon(
                imageVector = Icons.Default.FormatListNumbered,
                contentDescription = "الأذكار",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("الأذكار", style = MaterialTheme.typography.labelMedium)
        }

        // Skip Button
        TextButton(
            onClick = onSkip,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("journey_skip_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "تخطي",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text("تخطي", style = MaterialTheme.typography.labelMedium)
        }

        // Next Button
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("journey_next_button")
        ) {
            Text(if (hasNext) "التالي" else "إتمام", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "التالي",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Completion Panel when a Dhikr is completed
 */
@Composable
fun DhikrCompletionPanel(
    onNextDhikr: () -> Unit,
    onRepeatDhikr: () -> Unit,
    onFinishJourney: () -> Unit,
    isLastItem: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "تم الإكمال",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "أحسنت، تم إكمال الذكر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "أتممت هذا الذكر بنجاح. يمكنك المتابعة للذكر التالي أو الإعادة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary: Next Dhikr or Complete Journey
                Button(
                    onClick = onNextDhikr,
                    modifier = Modifier.weight(1.3f).testTag("panel_next_dhikr_button")
                ) {
                    Text(
                        text = if (isLastItem) "إنهاء الرحلة ✓" else "الذكر التالي",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary: Repeat Dhikr
                OutlinedButton(
                    onClick = onRepeatDhikr,
                    modifier = Modifier.weight(1f).testTag("panel_repeat_dhikr_button")
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = "إعادة", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة الذكر")
                }

                // Tertiary: Finish
                TextButton(
                    onClick = onFinishJourney,
                    modifier = Modifier.weight(0.9f).testTag("panel_exit_journey_button")
                ) {
                    Text("إنهاء")
                }
            }
        }
    }
}

/**
 * Next Dhikr Preview Dialog
 */
@Composable
fun NextDhikrPreviewDialog(
    nextDhikr: DhikrEntity?,
    targetCount: Long,
    useArabicIndic: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    if (nextDhikr == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "الذكر التالي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = nextDhikr.title ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nextDhikr.arabicText,
                    fontFamily = NotoNaskhArabicFamily,
                    fontSize = 19.sp,
                    lineHeight = 32.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "التكرار المقترح: ${ArabicUtils.formatNumber(targetCount, useArabicIndic)} مرة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = nextDhikr.sourceReference,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                modifier = Modifier.testTag("preview_start_dhikr_button")
            ) {
                Text("ابدأ الذكر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Incomplete Warning Dialog
 */
@Composable
fun IncompleteDhikrWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "لم تكمل هذا الذكر بعد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "لم تبلغ العدد المقترح لهذا الذكر. هل تود المتابعة إلى الذكر التالي مع حفظ تقدمك الحالي؟",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_incomplete_continue_button")
            ) {
                Text("متابعة")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_incomplete_stay_button")
            ) {
                Text("البقاء هنا")
            }
        }
    )
}

/**
 * Dhikr List Modal Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyListModalSheet(
    journeyTitle: String,
    items: List<DhikrJourneyItemEntity>,
    dhikrMap: Map<String, DhikrEntity>,
    currentIndex: Int,
    useArabicIndic: Boolean,
    onSelectItem: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "قائمة رحلة $journeyTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "اختر أي ذكر للانتقال إليه مباشرة:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    val dhikr = dhikrMap[item.dhikrId]
                    val isCurrent = index == currentIndex
                    val (statusLabel, statusColor) = when (item.status) {
                        "COMPLETED" -> "مكتمل ✓" to SuccessGreen
                        "IN_PROGRESS" -> "قيد التقدم" to MaterialTheme.colorScheme.primary
                        "SKIPPED" -> "تم تخطيه" to MaterialTheme.colorScheme.onSurfaceVariant
                        else -> "لم يبدأ" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectItem(index) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ArabicUtils.formatNumber((index + 1).toLong(), useArabicIndic)}.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = dhikr?.title ?: (dhikr?.arabicText?.take(22) ?: "ذكر"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "المستهدف: ${ArabicUtils.formatNumber(item.targetCount, useArabicIndic)} | المنجز: ${ArabicUtils.formatNumber(item.currentCount, useArabicIndic)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Journey Overview Dialog / Sheet
 */
@Composable
fun JourneyOverviewDialog(
    collectionTitle: String,
    items: List<Pair<DhikrEntity, Long>>,
    hasExistingJourney: Boolean,
    completedCount: Int,
    useArabicIndic: Boolean,
    onStart: (forceRestart: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "رحلة $collectionTitle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "رحلة متتابعة ترشدك خطوة بخطوة لإتمام أذكار هذه المجموعة بنظام وهدوء.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "عدد الأذكار: ${ArabicUtils.formatNumber(items.size.toLong(), useArabicIndic)} أذكار",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasExistingJourney) {
                        Text(
                            text = "المنجز سابقاً: ${ArabicUtils.formatNumber(completedCount.toLong(), useArabicIndic)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "الأذكار المشمولة في هذه الرحلة:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(items) { idx, (dhikr, target) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ArabicUtils.formatNumber((idx + 1).toLong(), useArabicIndic)}. ${dhikr.title ?: dhikr.arabicText.take(20)}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${ArabicUtils.formatNumber(target, useArabicIndic)} مرة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasExistingJourney) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onStart(false) },
                        modifier = Modifier.testTag("resume_journey_button")
                    ) {
                        Text("متابعة الرحلة")
                    }
                    OutlinedButton(
                        onClick = { onStart(true) },
                        modifier = Modifier.testTag("restart_journey_button")
                    ) {
                        Text("بدء من جديد")
                    }
                }
            } else {
                Button(
                    onClick = { onStart(false) },
                    modifier = Modifier.testTag("start_journey_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "ابدأ", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ابدأ الرحلة")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Dedicated Journey Completion Screen
 */
@Composable
fun JourneyCompletionScreen(
    journeyTitle: String,
    completedItemsCount: Int,
    totalItemsCount: Int,
    totalRepetitions: Long,
    startedAt: Long,
    useArabicIndic: Boolean,
    onReturnHome: () -> Unit,
    onViewHistory: () -> Unit,
    onRestartJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "تمت الرحلة",
                tint = GoldGlow,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ما شاء الله",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "أكملت رحلة $journeyTitle",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الأذكار المكتملة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${ArabicUtils.formatNumber(completedItemsCount.toLong(), useArabicIndic)} من ${ArabicUtils.formatNumber(totalItemsCount.toLong(), useArabicIndic)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إجمالي التكرارات:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${ArabicUtils.formatNumber(totalRepetitions, useArabicIndic)} تسبيحة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تاريخ الإتمام:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ArabicUtils.formatDateTime(System.currentTimeMillis(), useArabicIndic),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReturnHome,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("journey_complete_return_home")
        ) {
            Text("العودة للرئيسية", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.weight(1f).testTag("journey_complete_view_history")
            ) {
                Text("عرض السجل")
            }

            FilledTonalButton(
                onClick = onRestartJourney,
                modifier = Modifier.weight(1f).testTag("journey_complete_restart")
            ) {
                Text("إعادة الرحلة")
            }
        }
    }
}
