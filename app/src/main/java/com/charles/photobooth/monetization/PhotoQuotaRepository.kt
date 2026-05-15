package com.charles.photobooth.monetization

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        PhotoQuotaState(
            dateKey = today,
            photosUsedToday = if (storedDate == today) prefs[photosUsedKey] ?: 0 else 0,
            adPhotosEarnedToday = if (storedDate == today) prefs[adPhotosEarnedKey] ?: 0 else 0,
            hasUnlimitedPhotos = prefs[unlimitedKey] == true,
        )
    }

    suspend fun reservePhotos(photoCount: Int): Boolean {
        var reserved = false
        context.photoQuotaDataStore.edit { prefs ->
            val current = prefs.toQuotaState()
            val next = PhotoQuotaPolicy.reserve(current, photoCount) ?: return@edit
            prefs[dateKey] = next.dateKey
            prefs[photosUsedKey] = next.photosUsedToday
            prefs[adPhotosEarnedKey] = next.adPhotosEarnedToday
            prefs[unlimitedKey] = next.hasUnlimitedPhotos
            reserved = true
        }
        return reserved
    }

    suspend fun refundPhotos(photoCount: Int) {
        context.photoQuotaDataStore.edit { prefs ->
            val next = PhotoQuotaPolicy.refund(prefs.toQuotaState(), photoCount)
            prefs[dateKey] = next.dateKey
            prefs[photosUsedKey] = next.photosUsedToday
            prefs[adPhotosEarnedKey] = next.adPhotosEarnedToday
            prefs[unlimitedKey] = next.hasUnlimitedPhotos
        }
    }

    suspend fun grantAdReward(): Boolean {
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
