package com.example.photobooth

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

class PhotoboothApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        Log.d("PhotoboothApp", "Firebase Crashlytics initialized")
    }
}
