package com.charles.photobooth.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryActionStateTest {

    @Test
    fun `idle state`() {
        val state = GalleryActionState.Idle
        assertEquals("Idle", state::class.simpleName)
    }

    @Test
    fun `uploading state`() {
        val state = GalleryActionState.Uploading
        assertEquals("Uploading", state::class.simpleName)
    }

    @Test
    fun `sending state`() {
        val state = GalleryActionState.Sending
        assertEquals("Sending", state::class.simpleName)
    }

    @Test
    fun `error state holds message`() {
        val state = GalleryActionState.Error("Upload failed: 500")
        assertEquals("Upload failed: 500", state.message)
    }
}
