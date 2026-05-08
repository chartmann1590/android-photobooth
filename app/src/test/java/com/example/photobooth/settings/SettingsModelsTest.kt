package com.example.photobooth.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelsTest {

    @Test
    fun `event settings defaults`() {
        val settings = EventSettings()
        assertEquals("My Event", settings.eventName)
        assertEquals("EVENT_yyyyMMdd_HHmmss", settings.filenamePattern)
        assertNull(settings.currentTemplateId)
        assertNull(settings.selectedFrameId)
    }

    @Test
    fun `upload settings defaults`() {
        val settings = UploadSettings()
        assertTrue(settings.useAnonymousHost)
        assertEquals(AnonymousHostType.None, settings.anonymousHostType)
        assertEquals("", settings.immichBaseUrl)
        assertEquals("", settings.immichApiToken)
        assertEquals("", settings.immichAlbumId)
    }

    @Test
    fun `sms settings defaults`() {
        val settings = SmsGatewaySettings()
        assertEquals("", settings.baseUrl)
        assertEquals("", settings.username)
        assertEquals("", settings.password)
        assertFalse(settings.useCloudServer)
    }

    @Test
    fun `smtp settings defaults`() {
        val settings = SmtpSettings()
        assertEquals("", settings.host)
        assertEquals(587, settings.port)
        assertTrue(settings.useSslTls)
        assertEquals("", settings.username)
        assertEquals("", settings.password)
        assertEquals("", settings.fromAddress)
        assertEquals("", settings.fromName)
        assertEquals("Your photo from {eventName}", settings.defaultSubjectTemplate)
        assertEquals("Thanks for visiting {eventName}!", settings.defaultBodyTemplate)
    }

    @Test
    fun `camera settings defaults`() {
        val settings = CameraSettings()
        assertTrue(settings.useFrontCamera)
    }

    @Test
    fun `all settings composes from defaults`() {
        val settings = AllSettings()
        assertEquals("My Event", settings.event.eventName)
        assertTrue(settings.upload.useAnonymousHost)
        assertEquals(587, settings.smtp.port)
        assertTrue(settings.camera.useFrontCamera)
    }

    @Test
    fun `smtp email template placeholder replacement`() {
        val settings = SmtpSettings()
        val subject = settings.defaultSubjectTemplate.replace("{eventName}", "Wedding 2026")
        assertEquals("Your photo from Wedding 2026", subject)

        val body = settings.defaultBodyTemplate.replace("{eventName}", "Wedding 2026")
        assertEquals("Thanks for visiting Wedding 2026!", body)
    }

    @Test
    fun `data class copy preserves values`() {
        val original = UploadSettings(useAnonymousHost = true, immichBaseUrl = "https://photos.example.com")
        val modified = original.copy(useAnonymousHost = false)
        assertFalse(modified.useAnonymousHost)
        assertEquals("https://photos.example.com", modified.immichBaseUrl)
    }
}
