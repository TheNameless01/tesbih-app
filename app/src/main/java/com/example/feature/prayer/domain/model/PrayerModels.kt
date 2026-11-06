package com.example.feature.prayer.domain.model

enum class PrayerType(val arabicName: String, val isPrayer: Boolean) {
    FAJR("الفجر", true),
    SUNRISE("الشروق", false),
    DHUHR("الظهر", true),
    ASR("العصر", true),
    MAGHRIB("المغرب", true),
    ISHA("العشاء", true);

    companion object {
        val prayersOnly = entries.filter { it.isPrayer }
    }
}

data class CalculationMethodInfo(
    val id: Int,
    val arabicName: String,
    val englishName: String,
    val isDefault: Boolean = false
)

object CalculationMethods {
    val ALGERIA = CalculationMethodInfo(19, "الجزائر (وزارة الشؤون الدينية والأوقاف)", "Algeria", isDefault = true)
    val MUSLIM_WORLD_LEAGUE = CalculationMethodInfo(3, "رابطة العالم الإسلامي", "Muslim World League")
    val UMM_AL_QURA = CalculationMethodInfo(4, "جامعة أم القرى (مكة المكرمة)", "Umm Al-Qura University, Makkah")
    val EGYPTIAN = CalculationMethodInfo(5, "الهيئة المصرية العامة للمساحة", "Egyptian General Authority of Survey")
    val DUBAI = CalculationMethodInfo(16, "دائرة الشؤون الإسلامية والعمل الخيري (دبي)", "Dubai")
    val TURKEY = CalculationMethodInfo(13, "رئاسة الشؤون الدينية (تركيا)", "Diyanet İşleri Başkanlığı, Turkey")
    val ISNA = CalculationMethodInfo(2, "الجمعية الإسلامية لأمريكا الشمالية (ISNA)", "ISNA")
    val KARACHI = CalculationMethodInfo(1, "جامعة العلوم الإسلامية بكراتشي", "University of Islamic Sciences, Karachi")
    val FRANCE = CalculationMethodInfo(12, "اتحاد المنظمات الإسلامية بفرنسا", "France")
    val TUNISIA = CalculationMethodInfo(18, "وزارة الشؤون الدينية (تونس)", "Tunisia")

    val all = listOf(
        ALGERIA,
        MUSLIM_WORLD_LEAGUE,
        UMM_AL_QURA,
        EGYPTIAN,
        DUBAI,
        TUNISIA,
        TURKEY,
        ISNA,
        KARACHI,
        FRANCE
    )

    fun getById(id: Int): CalculationMethodInfo {
        return all.find { it.id == id } ?: ALGERIA
    }
}

enum class AsrSchool(val id: Int, val arabicName: String) {
    STANDARD(0, "الشافعي / المالكي / الحنبلي (الجمهور)"),
    HANAFI(1, "الحنفي")
}

enum class LocationMode {
    AUTO,
    MANUAL
}

enum class AudioSourceType {
    BUNDLED, // Sheikh Rabah Ben Drah Algerian Adhan
    CUSTOM   // User-imported audio file (MP3, M4A, WAV)
}

data class IndividualAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    fun forPrayer(prayer: PrayerType): Int {
        return when (prayer) {
            PrayerType.FAJR -> fajr
            PrayerType.SUNRISE -> sunrise
            PrayerType.DHUHR -> dhuhr
            PrayerType.ASR -> asr
            PrayerType.MAGHRIB -> maghrib
            PrayerType.ISHA -> isha
        }
    }
}

data class IndividualAdhanSwitches(
    val fajr: Boolean = true,
    val dhuhr: Boolean = true,
    val asr: Boolean = true,
    val maghrib: Boolean = true,
    val isha: Boolean = true
) {
    fun isEnabledFor(prayer: PrayerType): Boolean {
        return when (prayer) {
            PrayerType.FAJR -> fajr
            PrayerType.SUNRISE -> false
            PrayerType.DHUHR -> dhuhr
            PrayerType.ASR -> asr
            PrayerType.MAGHRIB -> maghrib
            PrayerType.ISHA -> isha
        }
    }
}

data class PrayerSettings(
    val isConfigured: Boolean = false,
    val locationMode: LocationMode = LocationMode.AUTO,
    val latitude: Double = 36.7538, // Default Algiers coordinates
    val longitude: Double = 3.0588,
    val locationName: String = "",
    val timezone: String = "Africa/Algiers",
    val calculationMethodId: Int = 19, // Algeria default
    val asrSchoolId: Int = 0, // Standard
    val globalAdjustmentMinutes: Int = 0,
    val individualAdjustments: IndividualAdjustments = IndividualAdjustments(),
    val masterAdhanEnabled: Boolean = false, // OFF by default as required
    val individualAdhanSwitches: IndividualAdhanSwitches = IndividualAdhanSwitches(),
    val audioSource: AudioSourceType = AudioSourceType.BUNDLED,
    val customAudioUri: String? = null,
    val customAudioFileName: String? = null,
    val playbackDurationSeconds: Int = 30, // 15-60s
    val isHomeCardExpanded: Boolean = false,
    val lastRefreshEpoch: Long = 0L
)

data class SinglePrayerItem(
    val type: PrayerType,
    val rawTime: String, // "05:30"
    val adjustedTime: String, // "05:32"
    val epochMillis: Long,
    val isNext: Boolean = false
)

data class DailyPrayerSchedule(
    val dateString: String,
    val locationName: String,
    val timezone: String,
    val methodInfo: CalculationMethodInfo,
    val prayers: List<SinglePrayerItem>,
    val nextPrayer: SinglePrayerItem?,
    val countdownMillis: Long = 0L,
    val isStale: Boolean = false
)
