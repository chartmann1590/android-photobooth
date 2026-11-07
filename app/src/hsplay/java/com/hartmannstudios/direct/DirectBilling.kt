package com.hartmannstudios.direct

import android.app.Activity
import android.content.Context

/**
 * Play build: a no-op twin of the direct build's DirectBilling (src/direct/java). Google Play
 * builds keep using Google Play Billing; nothing here runs or touches the network.
 */
object DirectBilling {
    const val isDirect = false
    const val BASE_URL = ""
    const val DOWNLOAD_PAGE = ""
    @JvmStatic fun init(context: Context, app: String) {}
    @JvmStatic fun entitlements(): Set<String> = emptySet()
    @JvmStatic fun isEntitled(vararg keys: String): Boolean = false
    @JvmStatic fun email(): String? = null
    @JvmStatic fun addListener(l: () -> Unit) {}
    @JvmStatic fun removeListener(l: () -> Unit) {}
    @JvmStatic @JvmOverloads fun purchase(activity: Activity, productKey: String, onResult: ((Boolean) -> Unit)? = null) {}
    @JvmStatic @JvmOverloads fun restore(activity: Activity, onResult: ((Boolean) -> Unit)? = null) {}
    @JvmStatic fun manageSubscription(activity: Activity) {}
    @JvmStatic fun openDownloadPage(context: Context) {}
}
