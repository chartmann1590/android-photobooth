package com.charles.photobooth.monetization

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charles.photobooth.BuildConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonetizationViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val quotaRepository = PhotoQuotaRepository(application)
    private val rewardedAdManager = RewardedAdManager(application)
    private val billingRepository = BillingRepository(application, quotaRepository, viewModelScope)

    val quotaState: StateFlow<PhotoQuotaState> = quotaRepository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoQuotaState(),
    )
    val rewardedAdState: StateFlow<RewardedAdState> = rewardedAdManager.state
    val billingState: StateFlow<BillingUiState> = billingRepository.state

    init {
        if (!BuildConfig.WEDDING_MODE) {
            rewardedAdManager.loadAd()
            billingRepository.startConnection()
        }
    }

    suspend fun reservePhotos(photoCount: Int): Boolean = quotaRepository.reservePhotos(photoCount)

    fun refundPhotos(photoCount: Int) {
        viewModelScope.launch {
            quotaRepository.refundPhotos(photoCount)
        }
    }

    fun watchRewardedAd(activity: Activity) {
        if (BuildConfig.WEDDING_MODE) return
        rewardedAdManager.showAd(activity) {
            viewModelScope.launch {
                quotaRepository.grantAdReward()
            }
        }
    }

    fun loadRewardedAd() {
        if (BuildConfig.WEDDING_MODE) return
        rewardedAdManager.loadAd()
    }

    fun buyUnlimited(activity: Activity) {
        if (BuildConfig.WEDDING_MODE) return
        billingRepository.launchUnlimitedPurchase(activity)
    }
}
