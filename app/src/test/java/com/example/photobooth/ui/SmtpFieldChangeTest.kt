package com.example.photobooth.ui.screens

import org.junit.Assert.*
import org.junit.Test

class SmtpFieldChangeTest {

    @Test
    fun `Host holds value`() {
        val change = SmtpFieldChange.Host("smtp.example.com")
        assertEquals("smtp.example.com", change.value)
    }

    @Test
    fun `Port holds value`() {
        val change = SmtpFieldChange.Port(587)
        assertEquals(587, change.value)
    }

    @Test
    fun `UseTls holds value`() {
        val change = SmtpFieldChange.UseTls(true)
        assertTrue(change.value)
    }

    @Test
    fun `Username holds value`() {
        val change = SmtpFieldChange.Username("user@example.com")
        assertEquals("user@example.com", change.value)
    }

    @Test
    fun `Password holds value`() {
        val change = SmtpFieldChange.Password("secret123")
        assertEquals("secret123", change.value)
    }

    @Test
    fun `FromAddress holds value`() {
        val change = SmtpFieldChange.FromAddress("noreply@example.com")
        assertEquals("noreply@example.com", change.value)
    }

    @Test
    fun `FromName holds value`() {
        val change = SmtpFieldChange.FromName("Photobooth")
        assertEquals("Photobooth", change.value)
    }

    @Test
    fun `all field changes are distinct types`() {
        val changes: List<SmtpFieldChange> = listOf(
            SmtpFieldChange.Host("a"),
            SmtpFieldChange.Port(25),
            SmtpFieldChange.UseTls(false),
            SmtpFieldChange.Username("u"),
            SmtpFieldChange.Password("p"),
            SmtpFieldChange.FromAddress("f"),
            SmtpFieldChange.FromName("n"),
        )
        assertEquals(7, changes.map { it::class }.toSet().size)
    }
}
