package com.example.photobooth.camera

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
}
