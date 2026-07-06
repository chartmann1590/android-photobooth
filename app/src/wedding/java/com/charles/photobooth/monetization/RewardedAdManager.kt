package com.charles.photobooth.monetization

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RewardedAdState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val message: String? = null,
)

class RewardedAdManager(
    @Suppress("UNUSED_PARAMETER") context: Context,
) {
    private val _state = MutableStateFlow(RewardedAdState())
    val state: StateFlow<RewardedAdState> = _state

    fun loadAd() = Unit

    fun showAd(
        @Suppress("UNUSED_PARAMETER") activity: Activity,
        @Suppress("UNUSED_PARAMETER") onRewardEarned: () -> Unit,
    ) = Unit
}
