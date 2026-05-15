package com.charles.photobooth.monetization

const val FREE_PHOTOS_PER_DAY = 15
const val PHOTOS_PER_REWARDED_AD = 15
const val MAX_AD_PHOTOS_PER_DAY = 60
const val MAX_FREE_AND_AD_PHOTOS_PER_DAY = FREE_PHOTOS_PER_DAY + MAX_AD_PHOTOS_PER_DAY

data class PhotoQuotaState(
    val dateKey: String = "",
    val photosUsedToday: Int = 0,
    val adPhotosEarnedToday: Int = 0,
    val hasUnlimitedPhotos: Boolean = false,
) {
    val dailyLimit: Int = FREE_PHOTOS_PER_DAY + adPhotosEarnedToday.coerceIn(0, MAX_AD_PHOTOS_PER_DAY)
    val remainingPhotos: Int = if (hasUnlimitedPhotos) Int.MAX_VALUE else (dailyLimit - photosUsedToday).coerceAtLeast(0)
    val canEarnAdReward: Boolean = !hasUnlimitedPhotos && adPhotosEarnedToday < MAX_AD_PHOTOS_PER_DAY
    val isExhausted: Boolean = !hasUnlimitedPhotos && remainingPhotos <= 0
}

object PhotoQuotaPolicy {
    fun canReserve(state: PhotoQuotaState, photoCount: Int): Boolean {
        if (photoCount <= 0) return false
        if (state.hasUnlimitedPhotos) return true
        return photoCount <= state.remainingPhotos
    }

    fun reserve(state: PhotoQuotaState, photoCount: Int): PhotoQuotaState? {
        if (!canReserve(state, photoCount)) return null
        if (state.hasUnlimitedPhotos) return state
        return state.copy(photosUsedToday = state.photosUsedToday + photoCount)
    }

    fun refund(state: PhotoQuotaState, photoCount: Int): PhotoQuotaState {
        if (state.hasUnlimitedPhotos || photoCount <= 0) return state
        return state.copy(photosUsedToday = (state.photosUsedToday - photoCount).coerceAtLeast(0))
    }

    fun grantAdReward(state: PhotoQuotaState): PhotoQuotaState? {
        if (!state.canEarnAdReward) return null
        val reward = minOf(PHOTOS_PER_REWARDED_AD, MAX_AD_PHOTOS_PER_DAY - state.adPhotosEarnedToday)
        return state.copy(adPhotosEarnedToday = state.adPhotosEarnedToday + reward)
    }
}
