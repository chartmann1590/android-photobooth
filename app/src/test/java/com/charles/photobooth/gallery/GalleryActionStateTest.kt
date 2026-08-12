package com.charles.photobooth.gallery

import com.charles.photobooth.data.MediaType
import com.charles.photobooth.data.PhotoEntity
import com.charles.photobooth.settings.ShareSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `video actions allow upload QR and delete only`() {
        val video = PhotoEntity(
            eventName = "Wedding",
            localPath = "/tmp/video.mp4",
            uploadedUrl = "https://example.com/video.mp4",
            mediaType = MediaType.VIDEO,
        )

        val actions = availableGalleryActions(video, ShareSettings())

        assertTrue(GalleryAction.UPLOAD in actions)
        assertTrue(GalleryAction.QR_CODE in actions)
        assertTrue(GalleryAction.DELETE in actions)
        assertFalse(GalleryAction.EMAIL in actions)
        assertFalse(GalleryAction.SMS in actions)
        assertFalse(GalleryAction.PRINT in actions)
        assertFalse(GalleryAction.ANDROID_SHARE in actions)
    }

    @Test
    fun `photo actions follow share settings`() {
        val photo = PhotoEntity(eventName = "Wedding", localPath = "/tmp/photo.jpg")

        val actions = availableGalleryActions(
            photo,
            ShareSettings(enableEmailShare = true, enableSmsShare = false, enablePrintShare = true),
        )

        assertTrue(GalleryAction.UPLOAD in actions)
        assertTrue(GalleryAction.EMAIL in actions)
        assertFalse(GalleryAction.SMS in actions)
        assertTrue(GalleryAction.PRINT in actions)
        assertTrue(GalleryAction.ANDROID_SHARE in actions)
    }
}
