package com.charles.photobooth.monetization

import android.app.Activity
import android.content.Context
import com.charles.photobooth.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class RewardedAdState(
    val isLoading: Boolean = false,
    val isReady: Boolean = false,
    val message: String? = null,
)

class RewardedAdManager(
    private val context: Context,
) {
    private val _state = MutableStateFlow(RewardedAdState())
    val state: StateFlow<RewardedAdState> = _state

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private val adUnitId = BuildConfig.ADMOB_REWARDED_INTERSTITIAL_AD_UNIT_ID

    fun loadAd() {
        if (adUnitId.isBlank() || _state.value.isLoading || rewardedInterstitialAd != null) return
        _state.value = RewardedAdState(isLoading = true)
        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    rewardedInterstitialAd?.fullScreenContentCallback = fullScreenContentCallback()
                    _state.value = RewardedAdState(isReady = true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedInterstitialAd = null
                    _state.value = RewardedAdState(message = loadAdError.message)
                }
            },
        )
    }

    fun showAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
    ) {
        val ad = rewardedInterstitialAd
        if (ad == null) {
            _state.update { it.copy(message = "Ad is still loading. Try again in a moment.") }
            loadAd()
            return
        }
        ad.show(activity) {
            onRewardEarned()
        }
    }

    private fun fullScreenContentCallback(): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                _state.value = RewardedAdState()
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedInterstitialAd = null
                _state.value = RewardedAdState(message = adError.message)
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                _state.value = RewardedAdState(isReady = false)
            }
        }
    }
}
