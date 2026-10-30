package com.example.core.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.core.database.MusabbihDatabase
import com.example.core.database.entity.CounterSessionEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.database.entity.SessionDailyTotalsEntity
import com.example.core.datastore.UserPreferences
import com.example.core.ui.ArabicUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

data class BackupPreview(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val customDhikrsCount: Int,
    val sessionsCount: Int,
    val dailyTotalsCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null
)

class BackupManager(
    private val context: Context,
    private val database: MusabbihDatabase
) {

    suspend fun exportBackup(outputStream: OutputStream, userPreferences: UserPreferences): Boolean {
        return try {
            val root = JSONObject()
            root.put("schemaVersion", 1)
            root.put("appVersion", "1.0.0")
            root.put("exportedAt", System.currentTimeMillis())

            // 1. Preferences
            val prefsObj = JSONObject()
            prefsObj.put("themeMode", userPreferences.themeMode)
            prefsObj.put("useArabicIndicNumerals", userPreferences.useArabicIndicNumerals)
            prefsObj.put("hapticEnabled", userPreferences.hapticEnabled)
            prefsObj.put("morningReminderEnabled", userPreferences.morningReminderEnabled)
            prefsObj.put("morningReminderHour", userPreferences.morningReminderHour)
            prefsObj.put("morningReminderMinute", userPreferences.morningReminderMinute)
            prefsObj.put("eveningReminderEnabled", userPreferences.eveningReminderEnabled)
            prefsObj.put("eveningReminderHour", userPreferences.eveningReminderHour)
            prefsObj.put("eveningReminderMinute", userPreferences.eveningReminderMinute)
            root.put("preferences", prefsObj)

            // 2. Custom Dhikrs
            val customDhikrs = database.dhikrDao().getAllCustomDhikrs()
            val dhikrsArray = JSONArray()
            for (d in customDhikrs) {
                val dObj = JSONObject()
                dObj.put("id", d.id)
                dObj.put("arabicText", d.arabicText)
                dObj.put("normalizedSearchText", d.normalizedSearchText)
                dObj.put("title", d.title ?: "")
                dObj.put("categoryId", d.categoryId)
                dObj.put("sourceType", d.sourceType)
                dObj.put("sourceReference", d.sourceReference)
                dObj.put("verificationStatus", d.verificationStatus)
                dObj.put("defaultTarget", d.defaultTarget ?: -1L)
                dObj.put("isFavorite", d.isFavorite)
                dObj.put("createdAt", d.createdAt)
                dObj.put("updatedAt", d.updatedAt)
                dhikrsArray.put(dObj)
            }
            root.put("customDhikrs", dhikrsArray)

            // 3. Counter Sessions
            val sessions = database.counterDao().getAllSessionsList()
            val sessionsArray = JSONArray()
            for (s in sessions) {
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("dhikrId", s.dhikrId)
                sObj.put("currentCount", s.currentCount)
                sObj.put("targetCount", s.targetCount ?: -1L)
                sObj.put("startedAt", s.startedAt)
                sObj.put("updatedAt", s.updatedAt)
                sObj.put("completedAt", s.completedAt ?: -1L)
                sObj.put("status", s.status)
                sessionsArray.put(sObj)
            }
            root.put("sessions", sessionsArray)

            // 4. Session Daily Totals
            val totals = database.counterDao().getAllSessionDailyTotalsList()
            val totalsArray = JSONArray()
            for (t in totals) {
                val tObj = JSONObject()
                tObj.put("sessionId", t.sessionId)
                tObj.put("localDate", t.localDate)
                tObj.put("timezoneId", t.timezoneId)
                tObj.put("repetitions", t.repetitions)
                totalsArray.put(tObj)
            }
            root.put("dailyTotals", totalsArray)

            val jsonBytes = root.toString(2).toByteArray(StandardCharsets.UTF_8)
            outputStream.write(jsonBytes)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun parseAndPreview(inputStream: InputStream): Pair<BackupPreview, JSONObject?> {
        return try {
            val jsonString = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonString)

            val schemaVersion = root.optInt("schemaVersion", 0)
            if (schemaVersion <= 0 || schemaVersion > 1) {
                return BackupPreview(
                    schemaVersion = schemaVersion,
                    appVersion = root.optString("appVersion", "unknown"),
                    exportedAt = root.optLong("exportedAt", 0L),
                    customDhikrsCount = 0,
                    sessionsCount = 0,
                    dailyTotalsCount = 0,
                    isValid = false,
                    errorMessage = "نسخة احتياطية غير متوافقة أو تالفة"
                ) to null
            }

            val customDhikrsArray = root.optJSONArray("customDhikrs") ?: JSONArray()
            val sessionsArray = root.optJSONArray("sessions") ?: JSONArray()
            val totalsArray = root.optJSONArray("dailyTotals") ?: JSONArray()

            val preview = BackupPreview(
                schemaVersion = schemaVersion,
                appVersion = root.optString("appVersion", "1.0.0"),
                exportedAt = root.optLong("exportedAt", 0L),
                customDhikrsCount = customDhikrsArray.length(),
                sessionsCount = sessionsArray.length(),
                dailyTotalsCount = totalsArray.length(),
                isValid = true,
                errorMessage = null
            )
            preview to root
        } catch (e: Exception) {
            BackupPreview(
                schemaVersion = 0,
                appVersion = "",
                exportedAt = 0L,
                customDhikrsCount = 0,
                sessionsCount = 0,
                dailyTotalsCount = 0,
                isValid = false,
                errorMessage = "ملف غير صالح: " + (e.localizedMessage ?: "تنسيق غير معروف")
            ) to null
        }
    }

    suspend fun restoreFromBackup(root: JSONObject): Boolean {
        return try {
            database.withTransaction {
                // 1. Restore Custom Dhikrs (preserve built-ins)
                val customDhikrsArray = root.optJSONArray("customDhikrs") ?: JSONArray()
                val dhikrsToInsert = mutableListOf<DhikrEntity>()
                for (i in 0 until customDhikrsArray.length()) {
                    val item = customDhikrsArray.getJSONObject(i)
                    val id = item.getString("id")
                    val arabicText = item.getString("arabicText")
                    val defaultTargetVal = item.optLong("defaultTarget", -1L)
                    val target = if (defaultTargetVal > 0) defaultTargetVal else null

                    dhikrsToInsert.add(
                        DhikrEntity(
                            id = id,
                            arabicText = arabicText,
                            normalizedSearchText = item.optString("normalizedSearchText", ArabicUtils.normalizeArabic(arabicText)),
                            title = item.optString("title").ifBlank { null },
                            categoryId = item.optString("categoryId", "cat_general"),
                            sourceType = item.optString("sourceType", "CUSTOM"),
                            sourceReference = item.optString("sourceReference", "خاص بالمستخدم"),
                            verificationStatus = "REVIEWED",
                            defaultTarget = target,
                            isBuiltIn = false,
                            isFavorite = item.optBoolean("isFavorite", false),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                            contentRevision = 1
                        )
                    )
                }
                if (dhikrsToInsert.isNotEmpty()) {
                    database.dhikrDao().insertDhikrs(dhikrsToInsert)
                }

                // 2. Restore Sessions
                val sessionsArray = root.optJSONArray("sessions") ?: JSONArray()
                for (i in 0 until sessionsArray.length()) {
                    val s = sessionsArray.getJSONObject(i)
                    val targetVal = s.optLong("targetCount", -1L)
                    val completedAtVal = s.optLong("completedAt", -1L)
                    database.counterDao().insertSession(
                        CounterSessionEntity(
                            id = s.getString("id"),
                            dhikrId = s.getString("dhikrId"),
                            currentCount = s.getLong("currentCount"),
                            targetCount = if (targetVal > 0) targetVal else null,
                            startedAt = s.getLong("startedAt"),
                            updatedAt = s.getLong("updatedAt"),
                            completedAt = if (completedAtVal > 0) completedAtVal else null,
                            status = s.optString("status", "PAUSED")
                        )
                    )
                }

                // 3. Restore Daily Totals
                val totalsArray = root.optJSONArray("dailyTotals") ?: JSONArray()
                for (i in 0 until totalsArray.length()) {
                    val t = totalsArray.getJSONObject(i)
                    database.counterDao().insertOrUpdateDailyTotal(
                        SessionDailyTotalsEntity(
                            sessionId = t.getString("sessionId"),
                            localDate = t.getString("localDate"),
                            timezoneId = t.optString("timezoneId", "UTC"),
                            repetitions = t.getLong("repetitions")
                        )
                    )
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
