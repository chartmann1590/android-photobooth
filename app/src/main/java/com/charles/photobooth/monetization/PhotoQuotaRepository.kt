package com.charles.photobooth.monetization

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.charles.photobooth.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.photoQuotaDataStore by preferencesDataStore(name = "photo_quota")

class PhotoQuotaRepository(
    private val context: Context,
) {
    private val dateKey = stringPreferencesKey("date_key")
    private val photosUsedKey = intPreferencesKey("photos_used_today")
    private val adPhotosEarnedKey = intPreferencesKey("ad_photos_earned_today")
    private val unlimitedKey = booleanPreferencesKey("has_unlimited_photos")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val state: Flow<PhotoQuotaState> = context.photoQuotaDataStore.data.map { prefs ->
        val today = todayKey()
        val storedDate = prefs[dateKey]
        if (BuildConfig.WEDDING_MODE) {
            return@map PhotoQuotaState(
                dateKey = today,
                hasUnlimitedPhotos = true,
            )
        }
        PhotoQuotaState(
            dateKey = today,
            photosUsedToday = if (storedDate == today) prefs[photosUsedKey] ?: 0 else 0,
            adPhotosEarnedToday = if (storedDate == today) prefs[adPhotosEarnedKey] ?: 0 else 0,
            hasUnlimitedPhotos = prefs[unlimitedKey] == true,
        )
    }

    suspend fun reservePhotos(photoCount: Int): Boolean {
        if (BuildConfig.WEDDING_MODE) return photoCount > 0
        var reserved = false
        context.photoQuotaDataStore.edit { prefs ->
            val current = prefs.toQuotaState()
            val next = PhotoQuotaPolicy.reserve(current, photoCount)
            if (next == null) {
                // In debug builds, transparently refresh the quota and retry so a
                // tester is never blocked by the daily limit during development.
                if (BuildConfig.DEBUG) {
                    val refreshed = current.copy(photosUsedToday = 0, adPhotosEarnedToday = 0)
                    val retry = PhotoQuotaPolicy.reserve(refreshed, photoCount) ?: return@edit
                    prefs[dateKey] = retry.dateKey
                    prefs[photosUsedKey] = retry.photosUsedToday
                    prefs[adPhotosEarnedKey] = retry.adPhotosEarnedToday
                    prefs[unlimitedKey] = retry.hasUnlimitedPhotos
                    reserved = true
                }
                return@edit
            }
            prefs[dateKey] = next.dateKey
            prefs[photosUsedKey] = next.photosUsedToday
            prefs[adPhotosEarnedKey] = next.adPhotosEarnedToday
            prefs[unlimitedKey] = next.hasUnlimitedPhotos
            reserved = true

            // Debug only: when the counter would land at zero, recycle it back to a
            // full daily quota so the visible "X photos left today" rolls back to 15
            // instead of bottoming out and triggering the paywall.
            if (BuildConfig.DEBUG && !next.hasUnlimitedPhotos && next.remainingPhotos <= 0) {
                prefs[photosUsedKey] = 0
                prefs[adPhotosEarnedKey] = 0
            }
        }
        return reserved
    }

    suspend fun refundPhotos(photoCount: Int) {
        if (BuildConfig.WEDDING_MODE) return
        context.photoQuotaDataStore.edit { prefs ->
            val next = PhotoQuotaPolicy.refund(prefs.toQuotaState(), photoCount)
            prefs[dateKey] = next.dateKey
            prefs[photosUsedKey] = next.photosUsedToday
            prefs[adPhotosEarnedKey] = next.adPhotosEarnedToday
            prefs[unlimitedKey] = next.hasUnlimitedPhotos
        }
    }

    suspend fun grantAdReward(): Boolean {
        if (BuildConfig.WEDDING_MODE) return false
        var granted = false
        context.photoQuotaDataStore.edit { prefs ->
            val current = prefs.toQuotaState()
            val next = PhotoQuotaPolicy.grantAdReward(current) ?: return@edit
            prefs[dateKey] = next.dateKey
            prefs[photosUsedKey] = next.photosUsedToday
            prefs[adPhotosEarnedKey] = next.adPhotosEarnedToday
            prefs[unlimitedKey] = next.hasUnlimitedPhotos
            granted = true
        }
        return granted
    }

    /** Debug helper: zero out today's used + ad-earned counters so the visible
     *  "X photos left" snaps back to the base 15. Intended to be called from a
     *  debug-only Settings action. */
    suspend fun resetDailyQuota() {
        context.photoQuotaDataStore.edit { prefs ->
            prefs[dateKey] = todayKey()
            prefs[photosUsedKey] = 0
            prefs[adPhotosEarnedKey] = 0
        }
    }

    suspend fun setUnlimitedPhotos(enabled: Boolean) {
        context.photoQuotaDataStore.edit { prefs ->
            val current = prefs.toQuotaState()
            prefs[dateKey] = current.dateKey
            prefs[photosUsedKey] = current.photosUsedToday
            prefs[adPhotosEarnedKey] = current.adPhotosEarnedToday
            prefs[unlimitedKey] = enabled
        }
    }

    suspend fun currentState(): PhotoQuotaState = state.first()

    private fun todayKey(): String = dateFormat.format(Date())

    private fun androidx.datastore.preferences.core.Preferences.toQuotaState(): PhotoQuotaState {
        val today = todayKey()
        val storedDate = this[dateKey]
        return PhotoQuotaState(
            dateKey = today,
            photosUsedToday = if (storedDate == today) this[photosUsedKey] ?: 0 else 0,
            adPhotosEarnedToday = if (storedDate == today) this[adPhotosEarnedKey] ?: 0 else 0,
            hasUnlimitedPhotos = this[unlimitedKey] == true,
        )
    }
}
