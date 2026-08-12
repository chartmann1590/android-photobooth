package com.charles.photobooth.monetization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoQuotaPolicyTest {

    @Test
    fun `free users start with 15 photos per day`() {
        val state = PhotoQuotaState()

        assertEquals(15, state.remainingPhotos)
        assertTrue(PhotoQuotaPolicy.canReserve(state, 15))
        assertFalse(PhotoQuotaPolicy.canReserve(state, 16))
    }

    @Test
    fun `rewarded ads add 15 photos up to 60 extra`() {
        var state = PhotoQuotaState()

        repeat(4) {
            state = PhotoQuotaPolicy.grantAdReward(state)!!
        }

        assertEquals(60, state.adPhotosEarnedToday)
        assertEquals(75, state.remainingPhotos)
        assertFalse(state.canEarnAdReward)
        assertNull(PhotoQuotaPolicy.grantAdReward(state))
    }

    @Test
    fun `reservation consumes one quota per actual photo`() {
        val state = PhotoQuotaState(adPhotosEarnedToday = 15)

        val afterBoothSession = PhotoQuotaPolicy.reserve(state, 4)

        assertNotNull(afterBoothSession)
        assertEquals(4, afterBoothSession!!.photosUsedToday)
        assertEquals(26, afterBoothSession.remainingPhotos)
    }

    @Test
    fun `refund restores failed reserved photos`() {
        val reserved = PhotoQuotaPolicy.reserve(PhotoQuotaState(), 4)!!

        val refunded = PhotoQuotaPolicy.refund(reserved, 2)

        assertEquals(2, refunded.photosUsedToday)
        assertEquals(13, refunded.remainingPhotos)
    }

    @Test
    fun `unlimited users can reserve beyond daily limits`() {
        val state = PhotoQuotaState(
            photosUsedToday = 200,
            hasUnlimitedPhotos = true,
        )

        val reserved = PhotoQuotaPolicy.reserve(state, 100)

        assertNotNull(reserved)
        assertEquals(Int.MAX_VALUE, reserved!!.remainingPhotos)
        assertFalse(reserved.canEarnAdReward)
    }
}
