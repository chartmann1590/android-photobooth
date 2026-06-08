package com.charles.photobooth.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.charles.photobooth.BuildConfig
import java.io.File
import java.util.Locale

object DiagnosticsHelper {

    fun gatherDiagnostics(context: Context, includeDiagnostics: Boolean): String {
        if (!includeDiagnostics) return ""

        val brand = Build.BRAND
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        val sdkLevel = Build.VERSION.SDK_INT
        val appVersionName = BuildConfig.VERSION_NAME
        val appVersionCode = BuildConfig.VERSION_CODE
        val locale = Locale.getDefault().toString()

        // Storage space on context.filesDir
        val filesDir = context.filesDir
        val freeBytes = getFreeSpace(filesDir)
        val totalBytes = getTotalSpace(filesDir)
        val freeMB = freeBytes / (1024 * 1024)
        val totalMB = totalBytes / (1024 * 1024)

        // System memory usage
        val memoryInfo = getMemoryInfo(context)
        val availableMegs = memoryInfo.availMem / (1024 * 1024)
        val totalMegs = memoryInfo.totalMem / (1024 * 1024)

        return """

---

### Device & Diagnostics Info
- **Brand**: $brand
- **Model**: $model
- **Manufacturer**: $manufacturer
- **Android Version**: $androidVersion (API $sdkLevel)
- **App Version**: $appVersionName (Code $appVersionCode)
- **Locale**: $locale
- **Storage (Free/Total)**: ${freeMB}MB / ${totalMB}MB
- **System Memory (Free/Total)**: ${availableMegs}MB / ${totalMegs}MB

### AI / LLM Models Configuration
- **Status**: No AI/LLM models configured in this app.
""".trimIndent()
    }

    private fun getFreeSpace(dir: File): Long {
        return try {
            val stat = StatFs(dir.path)
            stat.availableBytes
        } catch (e: Exception) {
            0L
        }
    }

    private fun getTotalSpace(dir: File): Long {
        return try {
            val stat = StatFs(dir.path)
            stat.totalBytes
        } catch (e: Exception) {
            0L
        }
    }

    private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }
}
