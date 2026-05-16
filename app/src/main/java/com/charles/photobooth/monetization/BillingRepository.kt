package com.charles.photobooth.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val UNLIMITED_PRODUCT_ID = "unlimited_photos"
private const val TAG = "BillingRepository"

data class BillingUiState(
    val isReady: Boolean = false,
    val price: String = "$9.99",
    val isPurchasing: Boolean = false,
    val message: String? = null,
)

class BillingRepository(
    context: Context,
    private val quotaRepository: PhotoQuotaRepository,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state

    private var unlimitedProductDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryExistingPurchases()
            return
        }
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Log.d(
                        TAG,
                        "onBillingSetupFinished code=${billingResult.responseCode} msg=${billingResult.debugMessage}",
                    )
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _state.value = _state.value.copy(isReady = true, message = null)
                        queryProductDetails()
                        queryExistingPurchases()
                    } else {
                        _state.value = _state.value.copy(
                            message = "Billing setup failed (${responseCodeName(billingResult.responseCode)}): ${billingResult.debugMessage}",
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "onBillingServiceDisconnected")
                    _state.value = _state.value.copy(isReady = false)
                }
            },
        )
    }

    fun launchUnlimitedPurchase(activity: Activity) {
        val productDetails = unlimitedProductDetails
        if (!billingClient.isReady || productDetails == null) {
            _state.value = _state.value.copy(message = "Google Play purchase details are still loading.")
            startConnection()
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        _state.value = _state.value.copy(isPurchasing = true, message = null)
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(isPurchasing = false, message = result.debugMessage)
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        _state.value = _state.value.copy(isPurchasing = false)
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::processPurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _state.value = _state.value.copy(message = billingResult.debugMessage)
        }
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(UNLIMITED_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
            Log.d(
                TAG,
                "queryProductDetails code=${billingResult.responseCode} " +
                    "msg=${billingResult.debugMessage} " +
                    "fetched=${productDetailsResult.productDetailsList.size} " +
                    "unfetched=${productDetailsResult.unfetchedProductList.size} " +
                    "pkg=${appContext.packageName}",
            )
            productDetailsResult.unfetchedProductList.forEach { unfetched ->
                Log.w(
                    TAG,
                    "Unfetched product id=${unfetched.productId} statusCode=${unfetched.statusCode} type=${unfetched.productType}",
                )
            }
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(
                    message = "Play product query failed (${responseCodeName(billingResult.responseCode)}): ${billingResult.debugMessage}",
                )
                return@queryProductDetailsAsync
            }
            val productDetails = productDetailsResult.productDetailsList.firstOrNull()
            unlimitedProductDetails = productDetails
            if (productDetails != null) {
                val offer = productDetails.oneTimePurchaseOfferDetails
                _state.value = _state.value.copy(
                    isReady = true,
                    price = offer?.formattedPrice ?: "$9.99",
                    message = null,
                )
            } else {
                val unfetched = productDetailsResult.unfetchedProductList
                    .firstOrNull { it.productId == UNLIMITED_PRODUCT_ID }
                val diagnostic = if (unfetched != null) {
                    "Play could not fetch '$UNLIMITED_PRODUCT_ID' (statusCode=${unfetched.statusCode}). " +
                        diagnosticHint(appContext.packageName)
                } else {
                    "Play returned no details for '$UNLIMITED_PRODUCT_ID'. " +
                        diagnosticHint(appContext.packageName)
                }
                _state.value = _state.value.copy(
                    isReady = true,
                    message = diagnostic,
                )
            }
        }
    }

    private fun diagnosticHint(packageName: String): String =
        "Check: (1) installed from Play (internal testing OK) — sideloaded debug builds cannot fetch IAPs; " +
            "(2) signed with the upload key matching the Play listing; " +
            "(3) package '$packageName' matches the Play Console app; " +
            "(4) product is Active and you waited ~2h after activation; " +
            "(5) the signed-in Google account is a licensed tester."

    private fun responseCodeName(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.OK -> "OK"
        BillingClient.BillingResponseCode.USER_CANCELED -> "USER_CANCELED"
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE"
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "BILLING_UNAVAILABLE"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "ITEM_UNAVAILABLE"
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        BillingClient.BillingResponseCode.ERROR -> "ERROR"
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "ITEM_ALREADY_OWNED"
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "ITEM_NOT_OWNED"
        BillingClient.BillingResponseCode.NETWORK_ERROR -> "NETWORK_ERROR"
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "FEATURE_NOT_SUPPORTED"
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "SERVICE_DISCONNECTED"
        else -> "UNKNOWN($code)"
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::processPurchase)
            }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (!purchase.products.contains(UNLIMITED_PRODUCT_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        scope.launch(Dispatchers.IO) {
            quotaRepository.setUnlimitedPhotos(true)
        }
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(message = billingResult.debugMessage)
                }
            }
        }
    }
}
