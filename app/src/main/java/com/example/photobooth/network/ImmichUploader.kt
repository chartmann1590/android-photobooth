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

class ImmichUploader(
    private val settings: UploadSettings,
    private val client: OkHttpClient = OkHttpClient(),
) : ImageUploader {
    override suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "assetData",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull()),
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
                throw IllegalStateException("Immich upload failed: ${response.code}")
            }
            val text = response.body?.string() ?: ""
            val json = JSONObject(text)
            // Use originalFileName or derived URL as reference; Immich is typically private.
            json.optString("id")
        }
    }
}

