package com.charles.photobooth.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureUiStateTest {

    @Test
    fun `idle state`() {
        val state = CaptureUiState.Idle
        assertEquals("Idle", state::class.simpleName)
    }

    @Test
    fun `countdown state holds seconds`() {
        val state = CaptureUiState.CountingDown(3)
        assertEquals(3, state.secondsLeft)
    }

    @Test
    fun `countdown state with zero seconds`() {
        val state = CaptureUiState.CountingDown(0)
        assertEquals(0, state.secondsLeft)
    }

    @Test
    fun `capturing state`() {
        val state = CaptureUiState.Capturing
        assertEquals("Capturing", state::class.simpleName)
    }

    @Test
    fun `saved state holds photo id`() {
        val state = CaptureUiState.Saved(42)
        assertEquals(42, state.photoId)
    }

    @Test
    fun `error state holds message`() {
        val state = CaptureUiState.Error("Failed to save")
        assertEquals("Failed to save", state.message)
    }

    @Test
    fun `preview state holds photo id, path, and upload status`() {
        val state = CaptureUiState.Preview(
            photoId = 7L,
            photoPath = "/data/photos/photo_1.jpg",
            uploadStatus = UploadStatus.Uploading,
        )
        assertEquals(7L, state.photoId)
        assertEquals("/data/photos/photo_1.jpg", state.photoPath)
        assertEquals("Uploading", state.uploadStatus::class.simpleName)
    }

    @Test
    fun `upload status complete carries url`() {
        val status = UploadStatus.Complete("https://example.com/p/abc")
        assertEquals("https://example.com/p/abc", status.url)
    }

    @Test
    fun `upload status failed carries message`() {
        val status = UploadStatus.Failed("HTTP 503 from upstream")
        assertEquals("HTTP 503 from upstream", status.message)
    }

    @Test
    fun `preview state copy can transition upload status`() {
        val initial = CaptureUiState.Preview(
            photoId = 1L,
            photoPath = "/x.jpg",
            uploadStatus = UploadStatus.Uploading,
        )
        val completed = initial.copy(uploadStatus = UploadStatus.Complete("https://h/q"))
        assertEquals(1L, completed.photoId)
        assertEquals("/x.jpg", completed.photoPath)
        assertEquals("Complete", completed.uploadStatus::class.simpleName)
        assertEquals("https://h/q", (completed.uploadStatus as UploadStatus.Complete).url)
    }
}
