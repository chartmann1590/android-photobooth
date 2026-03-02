package com.example.photobooth.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

interface ImageUploader {
    suspend fun upload(file: File): String
}

class ZeroX0Uploader(
    private val client: OkHttpClient = OkHttpClient(),
) : ImageUploader {
    override suspend fun upload(file: File): String = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
            .build()

        val request = Request.Builder()
            .url("https://0x0.st")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed: ${response.code}")
            }
            val url = response.body?.string()?.trim()
            url ?: throw IllegalStateException("Empty response from host")
        }
    }
}

