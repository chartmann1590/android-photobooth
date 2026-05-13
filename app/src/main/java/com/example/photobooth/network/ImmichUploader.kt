package com.example.photobooth.network

import com.example.photobooth.settings.UploadSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
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
        val mimeType = when (file.extension.lowercase()) {
            "gif" -> "image/gif"
            "png" -> "image/png"
            else -> "image/jpeg"
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "assetData",
                file.name,
                file.asRequestBody(mimeType.toMediaTypeOrNull()),
            )
            .apply {
                if (settings.immichAlbumId.isNotBlank()) {
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
}
