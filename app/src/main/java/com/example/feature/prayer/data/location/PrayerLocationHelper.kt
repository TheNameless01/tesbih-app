package com.example.feature.prayer.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class CityPreset(
    val arabicName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String
)

object PrayerLocationPresets {
    val ALGERIAN_CITIES = listOf(
        CityPreset("الجزائر العاصمة", "الجزائر", 36.7538, 3.0588, "Africa/Algiers"),
        CityPreset("وهران", "الجزائر", 35.6987, -0.6349, "Africa/Algiers"),
        CityPreset("قسنطينة", "الجزائر", 36.3650, 6.6147, "Africa/Algiers"),
        CityPreset("عنابة", "الجزائر", 36.9000, 7.7667, "Africa/Algiers"),
        CityPreset("سطيف", "الجزائر", 36.1905, 5.4137, "Africa/Algiers"),
        CityPreset("باتنة", "الجزائر", 35.5558, 6.1741, "Africa/Algiers"),
        CityPreset("البليدة", "الجزائر", 36.4700, 2.8300, "Africa/Algiers"),
        CityPreset("تلمسان", "الجزائر", 34.8783, -1.3150, "Africa/Algiers"),
        CityPreset("تيزي وزو", "الجزائر", 36.7169, 4.0497, "Africa/Algiers"),
        CityPreset("بجاية", "الجزائر", 36.7559, 5.0843, "Africa/Algiers"),
        CityPreset("بسكرة", "الجزائر", 34.8500, 5.7333, "Africa/Algiers"),
        CityPreset("ورقلة", "الجزائر", 31.9500, 5.3333, "Africa/Algiers"),
        CityPreset("غرداية", "الجزائر", 32.4900, 3.6736, "Africa/Algiers"),
        CityPreset("الشلف", "الجزائر", 36.1650, 1.3340, "Africa/Algiers"),
        CityPreset("سكيكدة", "الجزائر", 36.8790, 6.9060, "Africa/Algiers"),
        CityPreset("جيجل", "الجزائر", 36.8200, 5.7667, "Africa/Algiers"),
        CityPreset("مستغانم", "الجزائر", 35.9333, 0.0833, "Africa/Algiers"),
        CityPreset("تمنراست", "الجزائر", 22.7850, 5.5228, "Africa/Algiers")
    )

    val ISLAMIC_WORLD = listOf(
        CityPreset("مكة المكرمة", "السعودية", 21.4225, 39.8262, "Asia/Riyadh"),
        CityPreset("المدينة المنورة", "السعودية", 24.5247, 39.5692, "Asia/Riyadh"),
        CityPreset("القدس الشريف", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem"),
        CityPreset("القاهرة", "مصر", 30.0444, 31.2357, "Africa/Cairo"),
        CityPreset("تونس العاصمة", "تونس", 36.8065, 10.1815, "Africa/Tunis"),
        CityPreset("الرباط", "المغرب", 34.0209, -6.8416, "Africa/Casablanca"),
        CityPreset("طرابلس", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli"),
        CityPreset("اسطنبول", "تركيا", 41.0082, 28.9784, "Europe/Istanbul"),
        CityPreset("باريس", "فرنسا", 48.8566, 2.3522, "Europe/Paris"),
        CityPreset("لندن", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London")
    )

    val ALL_PRESETS = ALGERIAN_CITIES + ISLAMIC_WORLD
}

class PrayerLocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrLastLocation(): Location? {
        if (!hasLocationPermission()) return null

        return withContext(Dispatchers.IO) {
            try {
                // Try to get fresh location first
                val cts = CancellationTokenSource()
                val freshLocation = suspendCancellableCoroutine<Location?> { cont ->
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                }

                if (freshLocation != null) {
                    return@withContext freshLocation
                }

                // Fallback to last known location
                suspendCancellableCoroutine<Location?> { cont ->
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun resolveLocationName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale("ar"))
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val country = addr.countryName
                        return@withContext when {
                            !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city، $country"
                            !city.isNullOrBlank() -> city
                            !country.isNullOrBlank() -> country
                            else -> "موقعي الحالي"
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore geocoder timeout or connectivity failure
            }

            // Fallback match nearest preset
            val nearest = PrayerLocationPresets.ALL_PRESETS.minByOrNull { preset ->
                val dLat = preset.latitude - latitude
                val dLng = preset.longitude - longitude
                dLat * dLat + dLng * dLng
            }

            if (nearest != null) {
                "${nearest.arabicName}، ${nearest.country}"
            } else {
                "موقعي الحالي"
            }
        }
    }
}
