package com.example.feature.prayer.presentation.setup

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.feature.prayer.data.location.CityPreset
import com.example.feature.prayer.data.location.PrayerLocationHelper
import com.example.feature.prayer.data.location.PrayerLocationPresets
import com.example.feature.prayer.domain.model.AsrSchool
import com.example.feature.prayer.domain.model.CalculationMethods
import com.example.feature.prayer.domain.model.LocationMode
import kotlinx.coroutines.launch

@Composable
fun PrayerQuickSetupDialog(
    locationHelper: PrayerLocationHelper,
    onDismiss: () -> Unit,
    onSaveAndDownload: (
        locationMode: LocationMode,
        lat: Double,
        lng: Double,
        name: String,
        tz: String,
        methodId: Int,
        schoolId: Int,
        enableAdhan: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var locationMode by remember { mutableStateOf(LocationMode.AUTO) }
    var selectedCity by remember { mutableStateOf(PrayerLocationPresets.ALGERIAN_CITIES.first()) }
    var selectedMethodId by remember { mutableIntStateOf(19) } // Algeria default
    var selectedSchoolId by remember { mutableIntStateOf(0) }
    var enableAdhan by remember { mutableStateOf(false) }

    var isResolvingGps by remember { mutableStateOf(false) }
    var gpsResolvedName by remember { mutableStateOf<String?>(null) }
    var gpsLat by remember { mutableStateOf(36.7538) }
    var gpsLng by remember { mutableStateOf(3.0588) }

    var showCityDropdown by remember { mutableStateOf(false) }
    var showMethodDropdown by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isResolvingGps = true
            scope.launch {
                val loc = locationHelper.getCurrentOrLastLocation()
                if (loc != null) {
                    gpsLat = loc.latitude
                    gpsLng = loc.longitude
                    val resolved = locationHelper.resolveLocationName(loc.latitude, loc.longitude)
                    gpsResolvedName = resolved
                    locationMode = LocationMode.AUTO
                    Toast.makeText(context, "تم تحديد الموقع: $resolved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر قراءة الموقع، يرجى اختيار مدينتك يدوياً", Toast.LENGTH_LONG).show()
                    locationMode = LocationMode.MANUAL
                }
                isResolvingGps = false
            }
        } else {
            Toast.makeText(context, "تم رفض إذن الموقع، يمكنك اختيار مدينتك يدوياً", Toast.LENGTH_SHORT).show()
            locationMode = LocationMode.MANUAL
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mosque,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ضبط مواقيت الصلاة والأذان",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "اختر طريقة تحديد موقعك لحساب مواقيت الصلاة بدقة شرعية عالية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location Mode Selection
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = locationMode == LocationMode.AUTO,
                        onClick = {
                            if (locationHelper.hasLocationPermission()) {
                                isResolvingGps = true
                                scope.launch {
                                    val loc = locationHelper.getCurrentOrLastLocation()
                                    if (loc != null) {
                                        gpsLat = loc.latitude
                                        gpsLng = loc.longitude
                                        gpsResolvedName = locationHelper.resolveLocationName(loc.latitude, loc.longitude)
                                    }
                                    isResolvingGps = false
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            locationMode = LocationMode.AUTO
                        },
                        label = { Text("الموقع التلقائي (GPS)") },
                        leadingIcon = {
                            if (isResolvingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilterChip(
                        selected = locationMode == LocationMode.MANUAL,
                        onClick = { locationMode = LocationMode.MANUAL },
                        label = { Text("اختيار يدوي") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location Details Box
                if (locationMode == LocationMode.MANUAL) {
                    Column {
                        Text(
                            text = "المدينة المختارة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showCityDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${selectedCity.arabicName} (${selectedCity.country})",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showCityDropdown,
                            onDismissRequest = { showCityDropdown = false }
                        ) {
                            PrayerLocationPresets.ALL_PRESETS.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text("${city.arabicName} - ${city.country}") },
                                    onClick = {
                                        selectedCity = city
                                        showCityDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "الموقع الحالي: ${gpsResolvedName ?: "الجزائر العاصمة (افتراضي)"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calculation Method
                Text(
                    text = "طريقة الحساب الشرعية:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showMethodDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = CalculationMethods.getById(selectedMethodId).arabicName,
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
                    CalculationMethods.all.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.arabicName) },
                            onClick = {
                                selectedMethodId = m.id
                                showMethodDropdown = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Asr School
                Text(
                    text = "مذهب حساب صلاة العصر:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedSchoolId == 0,
                        onClick = { selectedSchoolId = 0 }
                    )
                    Text(
                        text = "الجمهور (المالكي والشافعي)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { selectedSchoolId = 0 }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedSchoolId == 1,
                        onClick = { selectedSchoolId = 1 }
                    )
                    Text(
                        text = "المذهب الحنفي",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { selectedSchoolId = 1 }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Adhan Toggle Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تفعيل صوت الأذان",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "بصوت الشيخ رابح بن دراح الجزائري",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enableAdhan,
                        onCheckedChange = { enableAdhan = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = if (locationMode == LocationMode.AUTO) gpsLat else selectedCity.latitude
                    val lng = if (locationMode == LocationMode.AUTO) gpsLng else selectedCity.longitude
                    val name = if (locationMode == LocationMode.AUTO) {
                        gpsResolvedName ?: "الجزائر العاصمة"
                    } else {
                        "${selectedCity.arabicName}، ${selectedCity.country}"
                    }
                    val tz = if (locationMode == LocationMode.AUTO) "Africa/Algiers" else selectedCity.timezone

                    onSaveAndDownload(
                        locationMode,
                        lat,
                        lng,
                        name,
                        tz,
                        selectedMethodId,
                        selectedSchoolId,
                        enableAdhan
                    )
                },
                modifier = Modifier.testTag("save_prayer_setup_button")
            ) {
                Text("حفظ وتنزيل المواقيت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("تخطي الآن")
            }
        }
    )
}
