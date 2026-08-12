package com.charles.photobooth.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BugReport(
    val number: Int,
    val title: String,
    val status: String, // "open" or "closed"
    val createdAt: String,
    val htmlUrl: String
)

private val Context.bugReportsDataStore by preferencesDataStore(name = "bug_reports")

class BugReportRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private object Keys {
        val BUG_REPORTS_LIST = stringPreferencesKey("bug_reports_list")
    }

    val bugReports: Flow<List<BugReport>> = context.bugReportsDataStore.data.map { prefs ->
        val jsonStr = prefs[Keys.BUG_REPORTS_LIST]
        if (jsonStr.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(BugReport.serializer()), jsonStr)
            }.getOrDefault(emptyList())
        }
    }

    suspend fun getBugReportsList(): List<BugReport> {
        return bugReports.first()
    }

    suspend fun saveBugReport(report: BugReport) {
        context.bugReportsDataStore.edit { prefs ->
            val currentList = getBugReportsList().toMutableList()
            // Avoid duplicates by removing existing item with same number
            currentList.removeAll { it.number == report.number }
            currentList.add(0, report) // Add to beginning (newest first)
            val jsonStr = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(BugReport.serializer()), currentList)
            prefs[Keys.BUG_REPORTS_LIST] = jsonStr
        }
    }

    suspend fun updateBugReportStatus(number: Int, status: String) {
        context.bugReportsDataStore.edit { prefs ->
            val currentList = getBugReportsList().map {
                if (it.number == number) it.copy(status = status) else it
            }
            val jsonStr = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(BugReport.serializer()), currentList)
            prefs[Keys.BUG_REPORTS_LIST] = jsonStr
        }
    }
}
