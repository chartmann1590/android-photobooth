package com.charles.photobooth

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.charles.photobooth.monetization.AdsInitializer
import com.charles.photobooth.template.BuiltInWeddingFrame
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.consentDataStore by preferencesDataStore(name = "consent")

class PhotoboothApp : Application() {

    enum class ConsentStatus {
        UNKNOWN, GRANTED, DENIED
    }

    override fun onCreate() {
        super.onCreate()
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            val options = com.google.firebase.FirebaseOptions.Builder()
                .setProjectId("android-photobooth")
                .setApplicationId("1:88402935520:android:ac471347e021190dbe9a85")
                .setApiKey("AIzaSyBV-yRlL51GRiLcaR5IicFR6nvLfQIwYcY")
                .setStorageBucket("android-photobooth.firebasestorage.app")
                .build()
            com.google.firebase.FirebaseApp.initializeApp(this, options)
        }
        if (!BuildConfig.WEDDING_MODE) {
            AdsInitializer.initialize(this)
        }
        if (!BuildConfig.WEDDING_MODE) {
            val consent = runBlocking { getConsentStatus() }
            FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(consent == ConsentStatus.GRANTED)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            BuiltInWeddingFrame.ensureSeeded(this@PhotoboothApp)
        }
    }

    suspend fun getConsentStatus(): ConsentStatus {
        val key = booleanPreferencesKey("analytics_consent_granted")
        val granted = consentDataStore.data.map { prefs ->
            when {
                prefs.contains(key) -> if (prefs[key] == true) ConsentStatus.GRANTED else ConsentStatus.DENIED
                else -> ConsentStatus.UNKNOWN
            }
        }.first()
        return granted
    }

    suspend fun setConsent(granted: Boolean) {
        val key = booleanPreferencesKey("analytics_consent_granted")
        consentDataStore.edit { prefs ->
            prefs[key] = granted
        }
        if (!BuildConfig.WEDDING_MODE) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(granted)
        }
    }

    suspend fun needsConsent(): Boolean {
        return getConsentStatus() == ConsentStatus.UNKNOWN
    }

    suspend fun hasSeenTutorial(): Boolean {
        val key = booleanPreferencesKey("tutorial_seen")
        return consentDataStore.data.map { prefs ->
            prefs[key] == true
        }.first()
    }

    suspend fun setTutorialSeen() {
        val key = booleanPreferencesKey("tutorial_seen")
        consentDataStore.edit { prefs ->
            prefs[key] = true
        }
    }
}
