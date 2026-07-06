package com.charles.photobooth.monetization

import android.content.Context
import com.google.android.gms.ads.MobileAds

object AdsInitializer {
    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }
}
