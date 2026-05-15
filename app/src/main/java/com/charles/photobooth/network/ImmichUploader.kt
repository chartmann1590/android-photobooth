package com.charles.photobooth.network

import com.charles.photobooth.settings.UploadSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ImmichUploader(
    private val settings: UploadSettings,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build(),
) : ImageUploader {
    override suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalStateException("File not found: ${file.name}")
        }
        if (settings.immichBaseUrl.isBlank()) {
            throw IllegalStateException("Immich base URL not configured")
        }
        val mimeType = mediaMimeTypeFor(file)
        val modifiedAt = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
        val timestamp = isoTimestamp(modifiedAt)

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("deviceAssetId", "${file.nameWithoutExtension}-${modifiedAt}")
            .addFormDataPart("deviceId", "android-photobooth")
            .addFormDataPart("fileCreatedAt", timestamp)
            .addFormDataPart("fileModifiedAt", timestamp)
            .addFormDataPart("filename", file.name)
            .addFormDataPart(
                "assetData",
                file.name,
                file.asRequestBody(mimeType.toMediaTypeOrNull()),
            )
            .apply {
                if (mimeType.startsWith("video/")) {
                    addFormDataPart("duration", "0:00:08.000000")
                }
            }
            .apply {
                if (settings.immichAlbumSyncEnabled && settings.immichAlbumId.isNotBlank()) {
                    addFormDataPart("albumId", settings.immichAlbumId)
                }
            }
            .build()

        val base = settings.immichBaseUrl.trimEnd('/')
        val request = Request.Builder()
            .url("$base/api/assets")
            .header("x-api-key", settings.immichApiToken)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(200) ?: "No error details"
                throw IllegalStateException("Immich upload failed: ${response.code} - $errorBody")
            }
            val text = response.body?.string() ?: ""
            val json = JSONObject(text)
            val id = json.optString("id", "")
            if (id.isBlank()) {
                throw IllegalStateException("Immich upload succeeded but returned no asset ID")
            }
            id
        }
    }

    private fun isoTimestamp(epochMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(epochMillis))
    }
}
