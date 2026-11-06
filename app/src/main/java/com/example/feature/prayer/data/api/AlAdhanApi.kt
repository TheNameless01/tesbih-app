package com.example.feature.prayer.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AlAdhanResponse(
    @Json(name = "code") val code: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "data") val data: List<AlAdhanDayData>?
)

@JsonClass(generateAdapter = true)
data class AlAdhanDayData(
    @Json(name = "timings") val timings: AlAdhanTimings?,
    @Json(name = "date") val date: AlAdhanDateContainer?,
    @Json(name = "meta") val meta: AlAdhanMeta?
)

@JsonClass(generateAdapter = true)
data class AlAdhanTimings(
    @Json(name = "Fajr") val fajr: String?,
    @Json(name = "Sunrise") val sunrise: String?,
    @Json(name = "Dhuhr") val dhuhr: String?,
    @Json(name = "Asr") val asr: String?,
    @Json(name = "Maghrib") val maghrib: String?,
    @Json(name = "Isha") val isha: String?
)

@JsonClass(generateAdapter = true)
data class AlAdhanDateContainer(
    @Json(name = "readable") val readable: String?,
    @Json(name = "timestamp") val timestamp: String?,
    @Json(name = "gregorian") val gregorian: AlAdhanGregorianDate?,
    @Json(name = "hijri") val hijri: AlAdhanHijriDate?
)

@JsonClass(generateAdapter = true)
data class AlAdhanGregorianDate(
    @Json(name = "date") val date: String?, // "06-11-2026" (DD-MM-YYYY)
    @Json(name = "day") val day: String?,
    @Json(name = "month") val month: AlAdhanMonth?,
    @Json(name = "year") val year: String?
)

@JsonClass(generateAdapter = true)
data class AlAdhanMonth(
    @Json(name = "number") val number: Int?,
    @Json(name = "en") val en: String?
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijriDate(
    @Json(name = "date") val date: String?,
    @Json(name = "day") val day: String?,
    @Json(name = "month") val month: AlAdhanHijriMonth?,
    @Json(name = "year") val year: String?
)

@JsonClass(generateAdapter = true)
data class AlAdhanHijriMonth(
    @Json(name = "number") val number: Int?,
    @Json(name = "en") val en: String?,
    @Json(name = "ar") val ar: String?
)

@JsonClass(generateAdapter = true)
data class AlAdhanMeta(
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "method") val method: AlAdhanMethodMeta?
)

@JsonClass(generateAdapter = true)
data class AlAdhanMethodMeta(
    @Json(name = "id") val id: Int?,
    @Json(name = "name") val name: String?
)

interface AlAdhanApiService {

    @GET("v1/calendar/{year}/{month}")
    suspend fun getMonthlyCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 19,
        @Query("school") school: Int = 0
    ): AlAdhanResponse

    companion object {
        private const val BASE_URL = "https://api.aladhan.com/"

        fun create(): AlAdhanApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(AlAdhanApiService::class.java)
        }
    }
}
