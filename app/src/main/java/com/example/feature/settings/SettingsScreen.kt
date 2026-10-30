package com.example.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.backup.BackupManager
import com.example.core.backup.BackupPreview
import com.example.core.datastore.UserPreferences
import com.example.core.datastore.UserPreferencesRepository
import com.example.core.reminder.ReminderScheduler
import com.example.core.ui.ArabicUtils
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    backupManager: BackupManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showImportConfirmDialog by remember { mutableStateOf<Pair<BackupPreview, JSONObject>?>(null) }

    // Time Pickers
    var showMorningTimePicker by remember { mutableStateOf(false) }
    var showEveningTimePicker by remember { mutableStateOf(false) }

    // Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "تم تفعيل إذن التنبيهات بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "تم رفض الإذن. لن تظهر التنبيهات في شريط الإشعارات.", Toast.LENGTH_LONG).show()
        }
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        val success = backupManager.exportBackup(os, userPreferences)
                        if (success) {
                            Toast.makeText(context, "تم تصدير النسخة الاحتياطية بنجاح ✓", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "تعذر تصدير النسخة الاحتياطية", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { isStream ->
                        val (preview, root) = backupManager.parseAndPreview(isStream)
                        if (preview.isValid && root != null) {
                            showImportConfirmDialog = Pair(preview, root)
                        } else {
                            Toast.makeText(context, preview.errorMessage ?: "ملف غير صالح", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "تعذر قراءة الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
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
            text = "الإعدادات",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تخصيص مظهر وتجربة مُسَبِّح",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Section
        SettingsCard(title = "المظهر والسمة", icon = Icons.Default.DarkMode) {
            Text(
                text = "اختر السمة المفضلة للتطبيق:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "SYSTEM" to "تلقائي",
                    "DARK" to "ليلي",
                    "LIGHT" to "نهاري"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = userPreferences.themeMode == mode,
                        onClick = {
                            scope.launch { preferencesRepository.setThemeMode(mode) }
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Counting Section
        SettingsCard(title = "التسبيح والأرقام", icon = Icons.Default.Numbers) {
            // Number format toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الأرقام المشرقية (١٢٣)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (userPreferences.useArabicIndicNumerals) "الأرقام تظهر بصيغة: ١ ٢ ٣" else "الأرقام تظهر بصيغة: 1 2 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPreferences.useArabicIndicNumerals,
                    onCheckedChange = { checked ->
                        scope.launch { preferencesRepository.setUseArabicIndicNumerals(checked) }
                    },
                    modifier = Modifier.testTag("arabic_indic_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Haptics toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الاهتزاز اللمسي (Haptic)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "اهتزاز لطيف عند الضغط وإتمام الهدف",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPreferences.hapticEnabled,
                    onCheckedChange = { checked ->
                        scope.launch { preferencesRepository.setHapticEnabled(checked) }
                    },
                    modifier = Modifier.testTag("haptic_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Reminders Section
        SettingsCard(title = "التذكير اليومي بالأوراد", icon = Icons.Default.Alarm) {
            // Morning Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMorningTimePicker = true }
                ) {
                    Text(
                        text = "تذكير أذكار الصباح",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val timeFormatted = String.format("%02d:%02d", userPreferences.morningReminderHour, userPreferences.morningReminderMinute)
                    Text(
                        text = "الوقت المحدد: ${ArabicUtils.formatNumber(userPreferences.morningReminderHour.toLong(), userPreferences.useArabicIndicNumerals)}:${ArabicUtils.formatNumber(userPreferences.morningReminderMinute.toLong(), userPreferences.useArabicIndicNumerals)} (المس للتغيير)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = userPreferences.morningReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        scope.launch {
                            preferencesRepository.setMorningReminder(
                                enabled = enabled,
                                hour = userPreferences.morningReminderHour,
                                minute = userPreferences.morningReminderMinute
                            )
                            ReminderScheduler.scheduleMorningReminder(
                                context,
                                userPreferences.morningReminderHour,
                                userPreferences.morningReminderMinute,
                                enabled
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Evening Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEveningTimePicker = true }
                ) {
                    Text(
                        text = "تذكير أذكار المساء",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "الوقت المحدد: ${ArabicUtils.formatNumber(userPreferences.eveningReminderHour.toLong(), userPreferences.useArabicIndicNumerals)}:${ArabicUtils.formatNumber(userPreferences.eveningReminderMinute.toLong(), userPreferences.useArabicIndicNumerals)} (المس للتغيير)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = userPreferences.eveningReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) requestNotificationPermissionIfNeeded()
                        scope.launch {
                            preferencesRepository.setEveningReminder(
                                enabled = enabled,
                                hour = userPreferences.eveningReminderHour,
                                minute = userPreferences.eveningReminderMinute
                            )
                            ReminderScheduler.scheduleEveningReminder(
                                context,
                                userPreferences.eveningReminderHour,
                                userPreferences.eveningReminderMinute,
                                enabled
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Backup and Restore Section
        SettingsCard(title = "النسخ الاحتياطي والاستعادة", icon = Icons.Default.Download) {
            Text(
                text = "احفظ بياناتك محلياً كملف JSON واسترجعها عند الرغبة دون الحاجة لأي حساب أو اتصال بالإنترنت:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val fileName = "musabbih_backup_${ArabicUtils.todayLocalDateString()}.json"
                        exportLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f).testTag("export_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = "تصدير", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير النسخة")
                }

                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.weight(1f).testTag("import_backup_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "استيراد", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استيراد النسخة")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // About & Privacy Section
        SettingsCard(title = "حول التطبيق والخصوصية", icon = Icons.Default.Info) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مُسَبِّح v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "معلومات الخصوصية، مصادر الأذكار، والمزيد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "الخصوصية",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Morning TimePicker Dialog
    if (showMorningTimePicker) {
        val state = rememberTimePickerState(
            initialHour = userPreferences.morningReminderHour,
            initialMinute = userPreferences.morningReminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showMorningTimePicker = false },
            title = { Text("وقت تذكير أذكار الصباح") },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesRepository.setMorningReminder(
                                enabled = userPreferences.morningReminderEnabled,
                                hour = state.hour,
                                minute = state.minute
                            )
                            if (userPreferences.morningReminderEnabled) {
                                ReminderScheduler.scheduleMorningReminder(context, state.hour, state.minute, true)
                            }
                        }
                        showMorningTimePicker = false
                    }
                ) {
                    Text("تعيين")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMorningTimePicker = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Evening TimePicker Dialog
    if (showEveningTimePicker) {
        val state = rememberTimePickerState(
            initialHour = userPreferences.eveningReminderHour,
            initialMinute = userPreferences.eveningReminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEveningTimePicker = false },
            title = { Text("وقت تذكير أذكار المساء") },
            text = { TimePicker(state = state) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            preferencesRepository.setEveningReminder(
                                enabled = userPreferences.eveningReminderEnabled,
                                hour = state.hour,
                                minute = state.minute
                            )
                            if (userPreferences.eveningReminderEnabled) {
                                ReminderScheduler.scheduleEveningReminder(context, state.hour, state.minute, true)
                            }
                        }
                        showEveningTimePicker = false
                    }
                ) {
                    Text("تعيين")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEveningTimePicker = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Import Preview and Confirmation Dialog
    if (showImportConfirmDialog != null) {
        val (preview, root) = showImportConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = null },
            title = { Text("معاينة وتأكيد الاستعادة") },
            text = {
                Column {
                    Text("تم العثور على نسخة احتياطية صالحة:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• أذكار مخصصة: ${preview.customDhikrsCount}")
                    Text("• جلسات تسبيح: ${preview.sessionsCount}")
                    Text("• سجلات إحصائية: ${preview.dailyTotalsCount}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ملاحظة: سيتم دمج الأذكار والجلسات دون حذف الأذكار الأساسية الثابتة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = backupManager.restoreFromBackup(root)
                            if (success) {
                                Toast.makeText(context, "تمت استعادة البيانات بنجاح ✓", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تعذر استعادة البيانات", Toast.LENGTH_SHORT).show()
                            }
                            showImportConfirmDialog = null
                        }
                    }
                ) {
                    Text("تأكيد الاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "عن تطبيق مُسَبِّح (Musabbih)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "مُسَبِّح رفيق إسلامي متقن ومخلص للذكر والتسبيح، صُمم بعناية ليكون هادئاً، سريعاً، وخالياً من المشتتات والإعلانات.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "مبادئ الخصوصية والأمان:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• يعمل محلياً بالكامل دون أي اتصال بالإنترنت (Offline-first).\n• لا يطلب إذناً للوصول للإنترنت أو الموقع أو جهات الاتصال أو الكاميرا.\n• لا يجمع أي بيانات شخصية، وخالٍ تماماً من التتبع والتحليلات.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "دقة النصوص الشرعية:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "كافة الأذكار والأدعية الواردة مأخوذة بنصوصها الصحيحة من كتاب الله تعالى وسنة نبيه ﷺ الثابتة في صحيحي البخاري ومسلم والسنن المعتمدة مع توثيق أرقام ومصادر كل ذكر.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
