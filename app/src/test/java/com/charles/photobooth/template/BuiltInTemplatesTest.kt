package com.charles.photobooth.template

import org.junit.Assert.*
import org.junit.Test

class BuiltInTemplatesTest {

    @Test
    fun `singlePortrait uses 4x6 dimensions`() {
        val template = BuiltInTemplates.singlePortrait("Test Event")
        assertEquals(OUTPUT_4X6_WIDTH, template.widthPx)
        assertEquals(OUTPUT_4X6_HEIGHT, template.heightPx)
    }

    @Test
    fun `singlePortrait has one frame`() {
        val template = BuiltInTemplates.singlePortrait("Test Event")
        assertEquals(1, template.frames.size)
    }

    @Test
    fun `singlePortrait has one overlay with event name`() {
        val template = BuiltInTemplates.singlePortrait("Wedding 2026")
        assertEquals(1, template.overlays.size)
        assertEquals("Wedding 2026", template.overlays[0].text)
    }

    @Test
    fun `singlePortrait frame covers most of canvas`() {
        val template = BuiltInTemplates.singlePortrait("Test")
        val frame = template.frames[0]
        assertTrue(frame.leftPercent < 0.1f)
        assertTrue(frame.topPercent < 0.1f)
        assertTrue(frame.widthPercent > 0.8f)
        assertTrue(frame.heightPercent > 0.7f)
    }

    @Test
    fun `photoStrip2x2 has four frames`() {
        val template = BuiltInTemplates.photoStrip2x2("Test")
        assertEquals(4, template.frames.size)
    }

    @Test
    fun `photoStrip2x2 uses 4x6 dimensions`() {
        val template = BuiltInTemplates.photoStrip2x2("Test")
        assertEquals(OUTPUT_4X6_WIDTH, template.widthPx)
        assertEquals(OUTPUT_4X6_HEIGHT, template.heightPx)
    }

    @Test
    fun `photoStrip2x2 has one overlay`() {
        val template = BuiltInTemplates.photoStrip2x2("Test")
        assertEquals(1, template.overlays.size)
    }

    @Test
    fun `photoStrip2x2 frames are in two columns`() {
        val template = BuiltInTemplates.photoStrip2x2("Test")
        val leftFrames = template.frames.filter { it.leftPercent < 0.3f }
        val rightFrames = template.frames.filter { it.leftPercent > 0.4f }
        assertEquals(2, leftFrames.size)
        assertEquals(2, rightFrames.size)
    }

    @Test
    fun `photoStripVertical has three frames`() {
        val template = BuiltInTemplates.photoStripVertical("Test")
        assertEquals(3, template.frames.size)
    }

    @Test
    fun `photoStripVertical uses 4x6 dimensions`() {
        val template = BuiltInTemplates.photoStripVertical("Test")
        assertEquals(OUTPUT_4X6_WIDTH, template.widthPx)
        assertEquals(OUTPUT_4X6_HEIGHT, template.heightPx)
    }

    @Test
    fun `photoStripVertical has one overlay`() {
        val template = BuiltInTemplates.photoStripVertical("Test")
        assertEquals(1, template.overlays.size)
    }

    @Test
    fun `different event names produce different overlays`() {
        val t1 = BuiltInTemplates.singlePortrait("Event A")
        val t2 = BuiltInTemplates.singlePortrait("Event B")
        assertNotEquals(t1.overlays[0].text, t2.overlays[0].text)
    }

    @Test
    fun `all templates have null background image path`() {
        val templates = listOf(
            BuiltInTemplates.singlePortrait("Test"),
            BuiltInTemplates.photoStrip2x2("Test"),
            BuiltInTemplates.photoStripVertical("Test"),
        )
        templates.forEach {
            assertNull(it.backgroundImagePath)
        }
    }

    @Test
    fun `fromKey STRIP_2x2 returns four-frame template`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_STRIP_2X2, "Test")
        assertNotNull(template)
        assertEquals(4, template!!.frames.size)
    }

    @Test
    fun `fromKey STRIP_VERTICAL returns three-frame template`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_STRIP_VERTICAL, "Test")
        assertNotNull(template)
        assertEquals(3, template!!.frames.size)
    }

    @Test
    fun `fromKey NONE returns null`() {
        assertNull(BuiltInTemplates.fromKey(BuiltInTemplates.KEY_NONE, "Test"))
    }

    @Test
    fun `fromKey SINGLE returns null since single-shot uses simple renderer`() {
        assertNull(BuiltInTemplates.fromKey(BuiltInTemplates.KEY_SINGLE, "Test"))
    }

    @Test
    fun `fromKey unknown key returns null`() {
        assertNull(BuiltInTemplates.fromKey("BOGUS", "Test"))
    }

    @Test
    fun `fromKey constant matches Settings persisted value`() {
        // Settings UI persists "STRIP_2x2" (lowercase x) — fromKey must accept that exact form
        assertEquals("STRIP_2x2", BuiltInTemplates.KEY_STRIP_2X2)
        assertNotNull(BuiltInTemplates.fromKey("STRIP_2x2", "Test"))
    }

    @Test
    fun `event presets all use 4x6 dimensions`() {
        val presets = listOf(
            BuiltInTemplates.birthday("Title", "Date"),
            BuiltInTemplates.wedding("Title", "Date"),
            BuiltInTemplates.anniversary("Title", "Date"),
            BuiltInTemplates.holiday("Title", "Date"),
            BuiltInTemplates.generic("Title", "Date"),
        )
        presets.forEach {
            assertEquals(OUTPUT_4X6_WIDTH, it.widthPx)
            assertEquals(OUTPUT_4X6_HEIGHT, it.heightPx)
        }
    }

    @Test
    fun `event presets all have a single frame`() {
        val presets = listOf(
            BuiltInTemplates.birthday("T", "D"),
            BuiltInTemplates.wedding("T", "D"),
            BuiltInTemplates.anniversary("T", "D"),
            BuiltInTemplates.holiday("T", "D"),
            BuiltInTemplates.generic("T", "D"),
        )
        presets.forEach { assertEquals(1, it.frames.size) }
    }

    @Test
    fun `event preset includes title and date overlays when both provided`() {
        val template = BuiltInTemplates.birthday("Sarah's 30th", "May 24, 2026")
        assertEquals(2, template.overlays.size)
        assertEquals("Sarah's 30th", template.overlays[0].text)
        assertEquals("May 24, 2026", template.overlays[1].text)
    }

    @Test
    fun `event preset omits date overlay when date is blank`() {
        val template = BuiltInTemplates.wedding("Jane and John", "")
        assertEquals(1, template.overlays.size)
        assertEquals("Jane and John", template.overlays[0].text)
    }

    @Test
    fun `event presets each use distinct background colors`() {
        val colors = setOf(
            BuiltInTemplates.birthday("T", "D").backgroundColor,
            BuiltInTemplates.wedding("T", "D").backgroundColor,
            BuiltInTemplates.anniversary("T", "D").backgroundColor,
            BuiltInTemplates.holiday("T", "D").backgroundColor,
            BuiltInTemplates.generic("T", "D").backgroundColor,
        )
        assertEquals(5, colors.size)
    }

    @Test
    fun `fromKey BIRTHDAY returns single-frame preset with title and date`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_BIRTHDAY, "Sarah's 30th", "May 24, 2026")
        assertNotNull(template)
        assertEquals(1, template!!.frames.size)
        assertEquals(2, template.overlays.size)
        assertEquals("Sarah's 30th", template.overlays[0].text)
        assertEquals("May 24, 2026", template.overlays[1].text)
    }

    @Test
    fun `fromKey WEDDING returns wedding preset`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_WEDDING, "Jane and John", "")
        assertNotNull(template)
        assertEquals(1, template!!.frames.size)
        assertEquals(1, template.overlays.size)
    }

    @Test
    fun `fromKey ANNIVERSARY returns anniversary preset`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_ANNIVERSARY, "10 Years", "2016 - 2026")
        assertNotNull(template)
        assertEquals(2, template!!.overlays.size)
    }

    @Test
    fun `fromKey HOLIDAY returns holiday preset`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_HOLIDAY, "Christmas Party", "")
        assertNotNull(template)
        assertEquals(1, template!!.frames.size)
    }

    @Test
    fun `fromKey GENERIC returns generic preset`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_GENERIC, "Company Event", "Q4 2026")
        assertNotNull(template)
        assertEquals(2, template!!.overlays.size)
    }

    @Test
    fun `fromKey eventDate parameter is optional and defaults to empty`() {
        val template = BuiltInTemplates.fromKey(BuiltInTemplates.KEY_BIRTHDAY, "Title")
        assertNotNull(template)
        assertEquals(1, template!!.overlays.size)
    }

    @Test
    fun `event preset keys are stable strings`() {
        assertEquals("EVENT_BIRTHDAY", BuiltInTemplates.KEY_BIRTHDAY)
        assertEquals("EVENT_WEDDING", BuiltInTemplates.KEY_WEDDING)
        assertEquals("EVENT_ANNIVERSARY", BuiltInTemplates.KEY_ANNIVERSARY)
        assertEquals("EVENT_HOLIDAY", BuiltInTemplates.KEY_HOLIDAY)
        assertEquals("EVENT_GENERIC", BuiltInTemplates.KEY_GENERIC)
    }
}
