package com.example.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.engine.CounterEngine
import com.example.core.engine.CounterEvent
import com.example.core.engine.CounterUiState
import com.example.core.engine.HapticFeedbackHelper
import com.example.core.engine.JourneyEngine
import com.example.core.engine.JourneyUiState
import com.example.core.ui.ArabicUtils
import com.example.feature.journey.DhikrCompletionPanel
import com.example.feature.journey.GuidedJourneyHeader
import com.example.feature.journey.IncompleteDhikrWarningDialog
import com.example.feature.journey.JourneyCompletionScreen
import com.example.feature.journey.JourneyListModalSheet
import com.example.feature.journey.JourneyNavigationControls
import com.example.feature.journey.JourneyOverviewDialog
import com.example.feature.journey.NextDhikrPreviewDialog
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.NotoNaskhArabicFamily
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.feature.prayer.presentation.home.PrayerTimesHomeCard
import com.example.feature.prayer.presentation.setup.PrayerQuickSetupDialog
import com.example.feature.prayer.presentation.timetable.PrayerTimetableDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    counterEngine: CounterEngine,
    journeyEngine: JourneyEngine,
    hapticHelper: HapticFeedbackHelper,
    hapticEnabled: Boolean,
    useArabicIndic: Boolean,
    uiState: CounterUiState,
    journeyState: JourneyUiState,
    prayerTimesRepo: com.example.feature.prayer.data.repository.PrayerTimesRepository,
    prayerPreferencesRepo: com.example.feature.prayer.data.repository.PrayerPreferencesRepository,
    prayerLocationHelper: com.example.feature.prayer.data.location.PrayerLocationHelper,
    onNavigateToAdhkar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prayerSettings by prayerPreferencesRepo.prayerSettingsFlow.collectAsState(initial = com.example.feature.prayer.domain.model.PrayerSettings())
    val prayerSchedule by prayerTimesRepo.observeDailySchedule().collectAsState(initial = null)
    var showPrayerSetupDialog by remember { mutableStateOf(false) }
    var showPrayerTimetableDialog by remember { mutableStateOf(false) }
    var isRefreshingPrayer by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showMilestoneBanner by remember { mutableStateOf(false) }
    val dhikr = uiState.dhikr

    // Tap pulse animation
    var tapScale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = tapScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { tapScale = 1f },
        label = "tap_scale"
    )

    // Listen for milestone events
    LaunchedEffect(Unit) {
        counterEngine.events.collectLatest { event ->
            when (event) {
                is CounterEvent.TargetCrossed -> {
                    if (hapticEnabled) {
                        hapticHelper.performTargetReachedHaptic()
                    }
                    showMilestoneBanner = true
                }
                is CounterEvent.Error -> {}
            }
        }
    }

    // If journey just completed, display dedicated JourneyCompletionScreen
    if (journeyState.isJourneyCompleted && journeyState.journey != null) {
        val j = journeyState.journey
        JourneyCompletionScreen(
            journeyTitle = j.title,
            completedItemsCount = j.completedItemsCount,
            totalItemsCount = j.totalItemsCount,
            totalRepetitions = j.totalRepetitions,
            startedAt = j.startedAt,
            useArabicIndic = useArabicIndic,
            onReturnHome = {
                scope.launch { journeyEngine.exitJourney() }
            },
            onViewHistory = {
                scope.launch {
                    journeyEngine.exitJourney()
                    onNavigateToHistory()
                }
            },
            onRestartJourney = {
                scope.launch { journeyEngine.restartJourney() }
            },
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: App Title & Dhikr switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "مُسَبِّح",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "رفيق الذكر والتسبيح",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onNavigateToAdhkar,
                    modifier = Modifier.testTag("choose_dhikr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "قائمة الأذكار",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "اختر ذكراً", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Prayer Times Card
            PrayerTimesHomeCard(
                settings = prayerSettings,
                schedule = prayerSchedule,
                isLoading = isRefreshingPrayer,
                useArabicIndic = useArabicIndic,
                onOpenSetup = { showPrayerSetupDialog = true },
                onOpenTimetable = { showPrayerTimetableDialog = true },
                onToggleExpanded = { expanded ->
                    scope.launch { prayerPreferencesRepo.setHomeCardExpanded(expanded) }
                },
                onRefresh = {
                    isRefreshingPrayer = true
                    scope.launch {
                        prayerTimesRepo.refreshPrayerTimes(force = true)
                        com.example.feature.prayer.scheduling.PrayerAlarmScheduler.reconcileAlarms(context)
                        isRefreshingPrayer = false
                    }
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // If a journey is active, show the Guided Journey Header
            if (journeyState.isJourneyActive && journeyState.journey != null) {
                val j = journeyState.journey
                val overallProgress = if (j.totalItemsCount > 0) {
                    j.completedItemsCount.toFloat() / j.totalItemsCount.toFloat()
                } else 0f

                GuidedJourneyHeader(
                    journeyTitle = j.title,
                    currentIndex = journeyState.currentItemIndex,
                    totalItems = j.totalItemsCount,
                    overallProgress = overallProgress,
                    useArabicIndic = useArabicIndic,
                    onOpenList = { journeyEngine.setDhikrListSheetVisible(true) },
                    onExitJourney = { scope.launch { journeyEngine.exitJourney() } },
                    modifier = Modifier.testTag("guided_journey_header")
                )
            } else if (journeyState.existingUnfinishedJourney != null) {
                // Subtle banner to resume unfinished journey
                val u = journeyState.existingUnfinishedJourney
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "رحلة ${u.title} قيد التقدم",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "أكملت ${ArabicUtils.formatNumber(u.completedItemsCount.toLong(), useArabicIndic)} من ${ArabicUtils.formatNumber(u.totalItemsCount.toLong(), useArabicIndic)} أذكار",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { scope.launch { journeyEngine.resumeJourney(u.id) } },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("متابعة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Current Dhikr Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { showDetailsDialog = true }
                    .testTag("dhikr_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dhikr?.title ?: "الذكر الحالي",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "تفاصيل الذكر",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = dhikr?.arabicText ?: "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                        fontFamily = NotoNaskhArabicFamily,
                        fontSize = 21.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dhikr?.sourceReference ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target Selection Chips (only show in free counting mode when not in guided journey)
            if (!journeyState.isJourneyActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val targets = listOf(33L, 100L, null)
                    targets.forEach { targetOption ->
                        val isSelected = uiState.target == targetOption
                        val label = when (targetOption) {
                            null -> "مفتوح ∞"
                            else -> ArabicUtils.formatNumber(targetOption, useArabicIndic)
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                scope.launch { counterEngine.setTarget(targetOption) }
                            },
                            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("target_chip_${targetOption ?: "unlimited"}")
                        )
                    }

                    // Custom Target Button
                    IconButton(
                        onClick = { showTargetDialog = true },
                        modifier = Modifier.testTag("custom_target_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "تحديد هدف مخصص",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Completion Panel during Journey when current Dhikr target is reached
            AnimatedVisibility(
                visible = journeyState.isJourneyActive && journeyState.isItemCompletedPromptVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val isLast = journeyState.currentItemIndex >= (journeyState.items.size - 1)
                DhikrCompletionPanel(
                    onNextDhikr = {
                        scope.launch { journeyEngine.requestNextDhikr() }
                    },
                    onRepeatDhikr = {
                        scope.launch { journeyEngine.repeatCurrentDhikr() }
                    },
                    onFinishJourney = {
                        scope.launch { journeyEngine.exitJourney() }
                    },
                    isLastItem = isLast,
                    modifier = Modifier.testTag("dhikr_completion_panel")
                )
            }

            // Milestone Banner for non-journey mode
            if (!journeyState.isJourneyActive) {
                AnimatedVisibility(
                    visible = showMilestoneBanner || (uiState.targetReached && uiState.target != null),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "تم الهدف",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ما شاء الله، أتممت الهدف المستهدف! استمر بالتسبيح مباركاً.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Tasbeeh Counting Circle
            val animatedProgress by animateFloatAsState(
                targetValue = uiState.progress,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progress"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 3.dp,
                        color = if (uiState.targetReached && uiState.target != null) GoldGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        tapScale = 0.94f
                        if (hapticEnabled) {
                            hapticHelper.performTapHaptic()
                        }
                        scope.launch {
                            counterEngine.increment()
                        }
                    }
                    .testTag("counter_tap_area")
            ) {
                // Circular progress ring
                if (uiState.target != null) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                }

                // Center Count & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ArabicUtils.formatNumber(uiState.count, useArabicIndic),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("counter_text")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val targetLabel = if (uiState.target != null) {
                        "من ${ArabicUtils.formatNumber(uiState.target, useArabicIndic)}"
                    } else {
                        "تسبيح مستمر"
                    }
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "المس الشاشة للتسبيح",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Undo, Reset, Complete Session
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo Button
                OutlinedButton(
                    onClick = {
                        if (hapticEnabled) hapticHelper.performTapHaptic()
                        scope.launch { counterEngine.undo() }
                    },
                    enabled = uiState.undoAvailable,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "تراجع",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تراجع", style = MaterialTheme.typography.labelMedium)
                }

                // Reset Button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    enabled = uiState.count > 0,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تصفير",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تصفير", style = MaterialTheme.typography.labelMedium)
                }

                // If in journey, complete session finishes or stays
                if (!journeyState.isJourneyActive) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch { counterEngine.completeSession() }
                        },
                        enabled = uiState.count > 0,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("complete_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "إنهاء الجلسة",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "ختم الورد", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Journey Navigation Controls (Previous, Dhikr List, Skip, Next)
            if (journeyState.isJourneyActive) {
                Spacer(modifier = Modifier.height(8.dp))
                JourneyNavigationControls(
                    hasPrevious = journeyState.currentItemIndex > 0,
                    hasNext = journeyState.currentItemIndex < (journeyState.items.size - 1),
                    onPrevious = { scope.launch { journeyEngine.previousDhikr() } },
                    onNext = { scope.launch { journeyEngine.requestNextDhikr() } },
                    onSkip = { scope.launch { journeyEngine.skipCurrentDhikr() } },
                    onOpenList = { journeyEngine.setDhikrListSheetVisible(true) },
                    modifier = Modifier.testTag("journey_navigation_controls")
                )
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "تصفير العداد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من تصفير العداد الحالي؟ لن يؤثر هذا على إحصائياتك السابقة أو بقية الرحلة.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        scope.launch { counterEngine.reset() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text(text = "نعم، تصفير")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = "إلغاء")
                }
            }
        )
    }

    // Custom Target Dialog
    if (showTargetDialog) {
        var inputTarget by remember { mutableStateOf(uiState.target?.toString() ?: "") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = {
                Text(
                    text = "تحديد هدف مخصص",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "أدخل عدد التكرارات المستهدف لهذا الذكر:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputTarget,
                        onValueChange = {
                            inputTarget = it.filter { ch -> ch.isDigit() }
                            isError = false
                        },
                        label = { Text("الهدف (مثال: 500)") },
                        isError = isError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_target_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = inputTarget.toLongOrNull()
                        if (num != null && num > 0) {
                            scope.launch { counterEngine.setTarget(num) }
                            showTargetDialog = false
                        } else {
                            isError = true
                        }
                    },
                    modifier = Modifier.testTag("save_custom_target_button")
                ) {
                    Text(text = "حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text(text = "إلغاء")
                }
            }
        )
    }

    // Dhikr Details Dialog
    if (showDetailsDialog && dhikr != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Text(
                    text = dhikr.title ?: "تفاصيل الذكر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = dhikr.arabicText,
                        fontFamily = NotoNaskhArabicFamily,
                        fontSize = 20.sp,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Text(
                            text = "المصدر: ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dhikr.sourceReference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text(
                            text = "حالة التحقق: ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (dhikr.verificationStatus == "REVIEWED") "تمت المراجعة والتحقق ✓" else "قيد المراجعة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text(text = "إغلاق")
                }
            }
        )
    }

    // Next Dhikr Preview Dialog
    if (journeyState.isNextPreviewVisible && journeyState.nextDhikr != null) {
        NextDhikrPreviewDialog(
            nextDhikr = journeyState.nextDhikr,
            targetCount = journeyState.nextItem?.targetCount ?: 33L,
            useArabicIndic = useArabicIndic,
            onStart = {
                scope.launch { journeyEngine.startNextDhikrFromPreview() }
            },
            onDismiss = {
                journeyEngine.dismissNextPreview()
            }
        )
    }

    // Incomplete Dhikr Warning Dialog
    if (journeyState.isIncompleteWarningVisible) {
        IncompleteDhikrWarningDialog(
            onConfirm = {
                scope.launch { journeyEngine.confirmProceedIncomplete() }
            },
            onDismiss = {
                journeyEngine.dismissIncompleteWarning()
            }
        )
    }

    // Journey List Sheet
    if (journeyState.isDhikrListSheetVisible && journeyState.journey != null) {
        JourneyListModalSheet(
            journeyTitle = journeyState.journey.title,
            items = journeyState.items,
            dhikrMap = journeyState.dhikrMap,
            currentIndex = journeyState.currentItemIndex,
            useArabicIndic = useArabicIndic,
            onSelectItem = { position ->
                scope.launch { journeyEngine.selectItemFromList(position) }
            },
            onDismiss = {
                journeyEngine.setDhikrListSheetVisible(false)
            }
        )
    }

    // Journey Overview Dialog
    if (journeyState.isOverviewVisible) {
        JourneyOverviewDialog(
            collectionTitle = journeyState.overviewCollectionTitle,
            items = journeyState.overviewItems,
            hasExistingJourney = journeyState.existingUnfinishedJourney != null,
            completedCount = journeyState.existingUnfinishedJourney?.completedItemsCount ?: 0,
            useArabicIndic = useArabicIndic,
            onStart = { forceRestart ->
                scope.launch {
                    val colId = journeyState.overviewCollectionId ?: "cat_morning"
                    val title = journeyState.overviewCollectionTitle
                    journeyEngine.startJourney(colId, title, forceRestart)
                }
            },
            onDismiss = {
                journeyEngine.dismissOverview()
            }
        )
    }

    // Prayer Quick Setup Dialog
    if (showPrayerSetupDialog) {
        PrayerQuickSetupDialog(
            locationHelper = prayerLocationHelper,
            onDismiss = { showPrayerSetupDialog = false },
            onSaveAndDownload = { mode, lat, lng, name, tz, methodId, schoolId, enableAdhan ->
                scope.launch {
                    prayerPreferencesRepo.setLocation(mode, lat, lng, name, tz)
                    prayerPreferencesRepo.setCalculationMethod(methodId)
                    prayerPreferencesRepo.setAsrSchool(schoolId)
                    prayerPreferencesRepo.setMasterAdhanEnabled(enableAdhan)
                    showPrayerSetupDialog = false
                    isRefreshingPrayer = true
                    prayerTimesRepo.refreshPrayerTimes(force = true)
                    com.example.feature.prayer.scheduling.PrayerAlarmScheduler.reconcileAlarms(context)
                    isRefreshingPrayer = false
                }
            }
        )
    }

    // Prayer Timetable Dialog
    if (showPrayerTimetableDialog) {
        PrayerTimetableDialog(
            schedule = prayerSchedule,
            settings = prayerSettings,
            isLoading = isRefreshingPrayer,
            useArabicIndic = useArabicIndic,
            onDismiss = { showPrayerTimetableDialog = false },
            onRefresh = {
                isRefreshingPrayer = true
                scope.launch {
                    prayerTimesRepo.refreshPrayerTimes(force = true)
                    com.example.feature.prayer.scheduling.PrayerAlarmScheduler.reconcileAlarms(context)
                    isRefreshingPrayer = false
                }
            }
        )
    }
}
