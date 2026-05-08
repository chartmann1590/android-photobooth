package com.example.photobooth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoEntityTest {

    @Test
    fun `photo entity default values`() {
        val photo = PhotoEntity(
            eventName = "Test Event",
            localPath = "/data/photos/test.jpg",
        )
        assertEquals(0, photo.id)
        assertEquals("Test Event", photo.eventName)
        assertEquals("/data/photos/test.jpg", photo.localPath)
        assertNull(photo.uploadedUrl)
        assertNull(photo.templateId)
    }

    @Test
    fun `photo entity with all fields`() {
        val photo = PhotoEntity(
            id = 42,
            eventName = "Wedding",
            takenAtEpochSeconds = 1700000000L,
            localPath = "/photos/wedding.jpg",
            uploadedUrl = "https://photos.example.com/abc123",
            templateId = 5,
        )
        assertEquals(42, photo.id)
        assertEquals("Wedding", photo.eventName)
        assertEquals(1700000000L, photo.takenAtEpochSeconds)
        assertEquals("https://photos.example.com/abc123", photo.uploadedUrl)
        assertEquals(5L, photo.templateId)
    }

    @Test
    fun `photo entity timestamp auto-populated`() {
        val before = System.currentTimeMillis() / 1_000L
        val photo = PhotoEntity(
            eventName = "Test",
            localPath = "/test.jpg",
        )
        val after = System.currentTimeMillis() / 1_000L
        assert(photo.takenAtEpochSeconds in before..after)
    }
}

class TemplateEntityTest {

    @Test
    fun `template entity defaults`() {
        val template = TemplateEntity(
            name = "Test Frame",
            backgroundImagePath = "/frames/test.png",
            layoutJson = "{}",
        )
        assertEquals(0, template.id)
        assertEquals("Test Frame", template.name)
        assertEquals("/frames/test.png", template.backgroundImagePath)
        assertEquals("{}", template.layoutJson)
        assertFalse(template.isBuiltIn)
    }

    @Test
    fun `template entity with nullable background`() {
        val template = TemplateEntity(
            name = "No Background",
            backgroundImagePath = null,
            layoutJson = "{}",
        )
        assertNull(template.backgroundImagePath)
    }
}
