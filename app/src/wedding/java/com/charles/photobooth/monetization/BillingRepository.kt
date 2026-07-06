package com.charles.photobooth.monetization

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val UNLIMITED_PRODUCT_ID = "wedding_photos"

data class BillingUiState(
    val isReady: Boolean = true,
    val price: String = "",
    val isPurchasing: Boolean = false,
    val message: String? = null,
)

class BillingRepository(
    @Suppress("UNUSED_PARAMETER") context: Context,
    @Suppress("UNUSED_PARAMETER") quotaRepository: PhotoQuotaRepository,
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state

    fun startConnection() = Unit

    fun launchUnlimitedPurchase(
        @Suppress("UNUSED_PARAMETER") activity: Activity,
    ) = Unit
}
