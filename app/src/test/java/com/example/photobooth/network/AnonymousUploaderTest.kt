package com.example.photobooth.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class AnonymousUploaderTest {
    @Test
    fun upload_returns_first_successful_host_url() {
        runBlocking {
            val uploader = AnonymousUploader(
                listOf(
                    FailingUploader("first failed"),
                    StaticUploader("https://storage.to/abc"),
                ),
            )

            val file = File.createTempFile("anon", ".jpg")
            try {
                assertEquals("https://storage.to/abc", uploader.upload(file))
            } finally {
                file.delete()
            }
        }
    }

    @Test
    fun upload_throws_last_error_when_all_hosts_fail() {
        runBlocking {
            val uploader = AnonymousUploader(
                listOf(
                    FailingUploader("first failed"),
                    FailingUploader("second failed"),
                ),
            )

            val file = File.createTempFile("anon", ".jpg")
            try {
                uploader.upload(file)
                fail("Expected exception")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("second failed"))
            } finally {
                file.delete()
            }
        }
    }

    private class StaticUploader(private val url: String) : ImageUploader {
        override suspend fun upload(file: File): String = url
    }

    private class FailingUploader(private val message: String) : ImageUploader {
        override suspend fun upload(file: File): String {
            throw IllegalStateException(message)
        }
    }
}
