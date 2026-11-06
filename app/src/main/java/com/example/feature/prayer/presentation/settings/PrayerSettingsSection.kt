package com.example.feature.prayer.presentation.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.ArabicUtils
import com.example.feature.prayer.audio.AdhanAudioManager
import com.example.feature.prayer.data.location.PrayerLocationHelper
import com.example.feature.prayer.data.location.PrayerLocationPresets
import com.example.feature.prayer.data.repository.PrayerPreferencesRepository
import com.example.feature.prayer.data.repository.PrayerTimesRepository
import com.example.feature.prayer.domain.model.AudioSourceType
import com.example.feature.prayer.domain.model.CalculationMethods
import com.example.feature.prayer.domain.model.LocationMode
import com.example.feature.prayer.domain.model.PrayerSettings
import com.example.feature.prayer.domain.model.PrayerType
import com.example.feature.prayer.scheduling.PrayerAlarmScheduler
import kotlinx.coroutines.launch

@Composable
fun PrayerSettingsSection(
    settings: PrayerSettings,
    preferencesRepo: PrayerPreferencesRepository,
    timesRepo: PrayerTimesRepository,
    locationHelper: PrayerLocationHelper,
    useArabicIndic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val audioManager = remember { AdhanAudioManager(context) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.stopAdhan()
        }
    }

    var isRefreshingSchedule by remember { mutableStateOf(false) }
    var showCityDropdown by remember { mutableStateOf(false) }
    var showMethodDropdown by remember { mutableStateOf(false) }

    // SAF Audio Picker Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Take persistable permission if supported
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                // Validate audio file can be opened
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val size = inputStream.available()
                    inputStream.close()

                    // Extract file name
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "ملف صوتي مخصص"
                    scope.launch {
                        preferencesRepo.setAudioSource(
                            source = AudioSourceType.CUSTOM,
                            uri = uri.toString(),
                            fileName = fileName
                        )
                        Toast.makeText(context, "تم استيراد الصوت: $fileName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "تعذر قراءة الملف المحدد", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "الملف غير صالح أو تعذر الوصول إليه", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Location Permission Launcher
    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val loc = locationHelper.getCurrentOrLastLocation()
                if (loc != null) {
                    val resolved = locationHelper.resolveLocationName(loc.latitude, loc.longitude)
                    preferencesRepo.setLocation(
                        mode = LocationMode.AUTO,
                        lat = loc.latitude,
                        lng = loc.longitude,
                        name = resolved,
                        tz = "Africa/Algiers"
                    )
                    timesRepo.refreshPrayerTimes(force = true)
                    PrayerAlarmScheduler.reconcileAlarms(context)
                    Toast.makeText(context, "تم تحديث موقعك: $resolved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_settings_section"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mosque,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مواقيت الصلاة والأذان",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. الموقع (Location)
            Text(
                text = "الموقع الجغرافي",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = settings.locationMode == LocationMode.AUTO,
                    onClick = {
                        if (locationHelper.hasLocationPermission()) {
                            scope.launch {
                                val loc = locationHelper.getCurrentOrLastLocation()
                                if (loc != null) {
                                    val name = locationHelper.resolveLocationName(loc.latitude, loc.longitude)
                                    preferencesRepo.setLocation(
                                        mode = LocationMode.AUTO,
                                        lat = loc.latitude,
                                        lng = loc.longitude,
                                        name = name,
                                        tz = "Africa/Algiers"
                                    )
                                    timesRepo.refreshPrayerTimes(force = true)
                                    PrayerAlarmScheduler.reconcileAlarms(context)
                                }
                            }
                        } else {
                            locationPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    label = { Text("الموقع التلقائي (GPS)") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterChip(
                    selected = settings.locationMode == LocationMode.MANUAL,
                    onClick = {
                        showCityDropdown = true
                    },
                    label = { Text("اختيار مدينة") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الموقع الحالي: ${settings.locationName.ifBlank { "الجزائر العاصمة" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        isRefreshingSchedule = true
                        scope.launch {
                            timesRepo.refreshPrayerTimes(force = true)
                            PrayerAlarmScheduler.reconcileAlarms(context)
                            isRefreshingSchedule = false
                            Toast.makeText(context, "تم تحديث المواقيت بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isRefreshingSchedule) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", modifier = Modifier.size(18.dp))
                    }
                }
            }

            DropdownMenu(
                expanded = showCityDropdown,
                onDismissRequest = { showCityDropdown = false }
            ) {
                PrayerLocationPresets.ALL_PRESETS.forEach { city ->
                    DropdownMenuItem(
                        text = { Text("${city.arabicName} - ${city.country}") },
                        onClick = {
                            scope.launch {
                                preferencesRepo.setLocation(
                                    mode = LocationMode.MANUAL,
                                    lat = city.latitude,
                                    lng = city.longitude,
                                    name = "${city.arabicName}، ${city.country}",
                                    tz = city.timezone
                                )
                                timesRepo.refreshPrayerTimes(force = true)
                                PrayerAlarmScheduler.reconcileAlarms(context)
                            }
                            showCityDropdown = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. الحسابات (Calculations)
            Text(
                text = "طريقة الحساب والمذهب",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { showMethodDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = CalculationMethods.getById(settings.calculationMethodId).arabicName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = showMethodDropdown,
                onDismissRequest = { showMethodDropdown = false }
            ) {
                CalculationMethods.all.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.arabicName) },
                        onClick = {
                            scope.launch {
                                preferencesRepo.setCalculationMethod(method.id)
                                timesRepo.refreshPrayerTimes(force = true)
                                PrayerAlarmScheduler.reconcileAlarms(context)
                            }
                            showMethodDropdown = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.asrSchoolId == 0,
                    onClick = {
                        scope.launch {
                            preferencesRepo.setAsrSchool(0)
                            timesRepo.refreshPrayerTimes(force = true)
                            PrayerAlarmScheduler.reconcileAlarms(context)
                        }
                    }
                )
                Text(
                    text = "العصر: الجمهور (شافعي/مالكي)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        scope.launch {
                            preferencesRepo.setAsrSchool(0)
                            timesRepo.refreshPrayerTimes(force = true)
                            PrayerAlarmScheduler.reconcileAlarms(context)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = settings.asrSchoolId == 1,
                    onClick = {
                        scope.launch {
                            preferencesRepo.setAsrSchool(1)
                            timesRepo.refreshPrayerTimes(force = true)
                            PrayerAlarmScheduler.reconcileAlarms(context)
                        }
                    }
                )
                Text(
                    text = "الحنفي",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        scope.launch {
                            preferencesRepo.setAsrSchool(1)
                            timesRepo.refreshPrayerTimes(force = true)
                            PrayerAlarmScheduler.reconcileAlarms(context)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. تعديلات المواقيت (Adjustments)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تعديلات الدقائق (+/-)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            preferencesRepo.resetAllAdjustments()
                            PrayerAlarmScheduler.reconcileAlarms(context)
                            Toast.makeText(context, "تمت إعادة ضبط التعديلات إلى 0", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة ضبط", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Global offset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تعديل عام لكافة الصلوات: ${formatOffset(settings.globalAdjustmentMinutes, useArabicIndic)} دقيقة",
                    style = MaterialTheme.typography.bodySmall
                )
                Row {
                    IconButton(
                        onClick = {
                            scope.launch {
                                preferencesRepo.setGlobalAdjustment(settings.globalAdjustmentMinutes - 1)
                                PrayerAlarmScheduler.reconcileAlarms(context)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                preferencesRepo.setGlobalAdjustment(settings.globalAdjustmentMinutes + 1)
                                PrayerAlarmScheduler.reconcileAlarms(context)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Individual adjustments for main prayers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IndividualOffsetControl("الفجر", settings.individualAdjustments.fajr, useArabicIndic) {
                    scope.launch {
                        preferencesRepo.setIndividualAdjustment(
                            fajr = it,
                            sunrise = settings.individualAdjustments.sunrise,
                            dhuhr = settings.individualAdjustments.dhuhr,
                            asr = settings.individualAdjustments.asr,
                            maghrib = settings.individualAdjustments.maghrib,
                            isha = settings.individualAdjustments.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                IndividualOffsetControl("الظهر", settings.individualAdjustments.dhuhr, useArabicIndic) {
                    scope.launch {
                        preferencesRepo.setIndividualAdjustment(
                            fajr = settings.individualAdjustments.fajr,
                            sunrise = settings.individualAdjustments.sunrise,
                            dhuhr = it,
                            asr = settings.individualAdjustments.asr,
                            maghrib = settings.individualAdjustments.maghrib,
                            isha = settings.individualAdjustments.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                IndividualOffsetControl("العصر", settings.individualAdjustments.asr, useArabicIndic) {
                    scope.launch {
                        preferencesRepo.setIndividualAdjustment(
                            fajr = settings.individualAdjustments.fajr,
                            sunrise = settings.individualAdjustments.sunrise,
                            dhuhr = settings.individualAdjustments.dhuhr,
                            asr = it,
                            maghrib = settings.individualAdjustments.maghrib,
                            isha = settings.individualAdjustments.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IndividualOffsetControl("المغرب", settings.individualAdjustments.maghrib, useArabicIndic) {
                    scope.launch {
                        preferencesRepo.setIndividualAdjustment(
                            fajr = settings.individualAdjustments.fajr,
                            sunrise = settings.individualAdjustments.sunrise,
                            dhuhr = settings.individualAdjustments.dhuhr,
                            asr = settings.individualAdjustments.asr,
                            maghrib = it,
                            isha = settings.individualAdjustments.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                IndividualOffsetControl("العشاء", settings.individualAdjustments.isha, useArabicIndic) {
                    scope.launch {
                        preferencesRepo.setIndividualAdjustment(
                            fajr = settings.individualAdjustments.fajr,
                            sunrise = settings.individualAdjustments.sunrise,
                            dhuhr = settings.individualAdjustments.dhuhr,
                            asr = settings.individualAdjustments.asr,
                            maghrib = settings.individualAdjustments.maghrib,
                            isha = it
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                IndividualOffsetControl("الشروق", settings.individualAdjustments.sunrise, useArabicIndic) {
                    scope.launch {
                        preferencesRepo.setIndividualAdjustment(
                            fajr = settings.individualAdjustments.fajr,
                            sunrise = it,
                            dhuhr = settings.individualAdjustments.dhuhr,
                            asr = settings.individualAdjustments.asr,
                            maghrib = settings.individualAdjustments.maghrib,
                            isha = settings.individualAdjustments.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. الأذان والصوت (Adhan & Audio)
            Text(
                text = "الأذان والصوت",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Master Adhan Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المفتاح العام للأذان",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (settings.masterAdhanEnabled) "الأذان مفعل للصلوات المختارة أدناه" else "الأذان صامت لجميع الصلوات",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.masterAdhanEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            preferencesRepo.setMasterAdhanEnabled(enabled)
                            PrayerAlarmScheduler.reconcileAlarms(context)
                        }
                    },
                    modifier = Modifier.testTag("master_adhan_switch")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Individual Prayer Switches
            Text(
                text = "تفعيل الأذان لكل صلاة:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PrayerSwitchChip("الفجر", settings.individualAdhanSwitches.fajr) {
                    scope.launch {
                        preferencesRepo.setIndividualAdhanSwitches(
                            fajr = it,
                            dhuhr = settings.individualAdhanSwitches.dhuhr,
                            asr = settings.individualAdhanSwitches.asr,
                            maghrib = settings.individualAdhanSwitches.maghrib,
                            isha = settings.individualAdhanSwitches.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                PrayerSwitchChip("الظهر", settings.individualAdhanSwitches.dhuhr) {
                    scope.launch {
                        preferencesRepo.setIndividualAdhanSwitches(
                            fajr = settings.individualAdhanSwitches.fajr,
                            dhuhr = it,
                            asr = settings.individualAdhanSwitches.asr,
                            maghrib = settings.individualAdhanSwitches.maghrib,
                            isha = settings.individualAdhanSwitches.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                PrayerSwitchChip("العصر", settings.individualAdhanSwitches.asr) {
                    scope.launch {
                        preferencesRepo.setIndividualAdhanSwitches(
                            fajr = settings.individualAdhanSwitches.fajr,
                            dhuhr = settings.individualAdhanSwitches.dhuhr,
                            asr = it,
                            maghrib = settings.individualAdhanSwitches.maghrib,
                            isha = settings.individualAdhanSwitches.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PrayerSwitchChip("المغرب", settings.individualAdhanSwitches.maghrib) {
                    scope.launch {
                        preferencesRepo.setIndividualAdhanSwitches(
                            fajr = settings.individualAdhanSwitches.fajr,
                            dhuhr = settings.individualAdhanSwitches.dhuhr,
                            asr = settings.individualAdhanSwitches.asr,
                            maghrib = it,
                            isha = settings.individualAdhanSwitches.isha
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
                PrayerSwitchChip("العشاء", settings.individualAdhanSwitches.isha) {
                    scope.launch {
                        preferencesRepo.setIndividualAdhanSwitches(
                            fajr = settings.individualAdhanSwitches.fajr,
                            dhuhr = settings.individualAdhanSwitches.dhuhr,
                            asr = settings.individualAdhanSwitches.asr,
                            maghrib = settings.individualAdhanSwitches.maghrib,
                            isha = it
                        )
                        PrayerAlarmScheduler.reconcileAlarms(context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Source
            Text(
                text = "مصدر صوت الأذان:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.audioSource == AudioSourceType.BUNDLED,
                    onClick = {
                        scope.launch {
                            preferencesRepo.setAudioSource(AudioSourceType.BUNDLED)
                        }
                    }
                )
                Text(
                    text = "أذان الشيخ رابح بن دراح الجزائري (المدمج)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        scope.launch {
                            preferencesRepo.setAudioSource(AudioSourceType.BUNDLED)
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.audioSource == AudioSourceType.CUSTOM,
                    onClick = {
                        if (settings.customAudioUri != null) {
                            scope.launch {
                                preferencesRepo.setAudioSource(
                                    AudioSourceType.CUSTOM,
                                    settings.customAudioUri,
                                    settings.customAudioFileName
                                )
                            }
                        } else {
                            audioPickerLauncher.launch("audio/*")
                        }
                    }
                )
                Text(
                    text = if (settings.customAudioFileName != null) {
                        "صوت مخصص: ${settings.customAudioFileName}"
                    } else {
                        "استيراد صوت شخصي (MP3, M4A, WAV)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        audioPickerLauncher.launch("audio/*")
                    }
                )
            }

            // Audio Preview and SAF Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        if (isPreviewPlaying) {
                            audioManager.stopAdhan()
                            isPreviewPlaying = false
                        } else {
                            val started = audioManager.playAdhan(
                                audioSource = settings.audioSource,
                                customUriString = settings.customAudioUri,
                                durationSeconds = settings.playbackDurationSeconds,
                                onComplete = { isPreviewPlaying = false }
                            )
                            isPreviewPlaying = started
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPreviewPlaying) "إيقاف الصوت" else "استماع للأذان", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استيراد صوت", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدة تشغيل الأذان:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 30, 45, 60).forEach { sec ->
                        FilterChip(
                            selected = settings.playbackDurationSeconds == sec,
                            onClick = {
                                scope.launch {
                                    preferencesRepo.setPlaybackDuration(sec)
                                }
                            },
                            label = { Text("${ArabicUtils.formatNumber(sec, useArabicIndic)} ث") },
                            modifier = Modifier.padding(0.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. الإشعارات والأذونات (Permissions explanation)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حالة دقة الأذان والتنبيهات",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (canScheduleExact) {
                            "إذن المنبهات الدقيقة مفعل بنجاح، سيرفع الأذان في موعده بالثانية بإذن الله."
                        } else {
                            "المنبهات الدقيقة مقيدة بنظام توفير الطاقة؛ قد يتأخر صوت الأذان بضع دقائق إذا كان الهاتف في وضع السكون."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Text("تفعيل إذن المنبهات الدقيقة في إعدادات النظام", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndividualOffsetControl(
    name: String,
    currentOffset: Int,
    useArabicIndic: Boolean,
    onOffsetChanged: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "${formatOffset(currentOffset, useArabicIndic)} د",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                IconButton(
                    onClick = { onOffsetChanged(currentOffset - 1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { onOffsetChanged(currentOffset + 1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PrayerSwitchChip(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = if (checked) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
        } else null,
        modifier = Modifier.width(95.dp)
    )
}

private fun formatOffset(offset: Int, useArabicIndic: Boolean): String {
    val prefix = if (offset > 0) "+" else ""
    return prefix + ArabicUtils.formatNumber(offset, useArabicIndic)
}
