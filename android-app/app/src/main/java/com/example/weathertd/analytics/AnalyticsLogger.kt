package com.example.weathertd.analytics

import android.content.Context
import android.os.Bundle
import com.example.weathertd.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics

interface AnalyticsLogger {
    fun logAppOpen()
    fun logViewMain(pointName: String, sourceName: String)
    fun logClickDetail(metricName: String)
    fun logStayTime(seconds: Long, pointName: String?)
}

class FirebaseAnalyticsLogger(context: Context) : AnalyticsLogger {

    private val analytics: FirebaseAnalytics? = runCatching {
        ensureFirebaseApp(context)
        FirebaseAnalytics.getInstance(context)
    }.getOrNull()

    override fun logAppOpen() {
        logEvent(FirebaseAnalytics.Event.APP_OPEN)
    }

    override fun logViewMain(pointName: String, sourceName: String) {
        logEvent("view_main") {
            putString("point_name", pointName)
            putString("location_source", sourceName)
        }
    }

    override fun logClickDetail(metricName: String) {
        logEvent("click_detail") {
            putString("metric_name", metricName)
        }
    }

    override fun logStayTime(seconds: Long, pointName: String?) {
        logEvent("stay_time") {
            putLong("seconds", seconds)
            pointName?.let { putString("point_name", it) }
        }
    }

    private fun logEvent(name: String, block: Bundle.() -> Unit = {}) {
        analytics?.logEvent(name, Bundle().apply(block))
    }

    private fun ensureFirebaseApp(context: Context) {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            return
        }

        val hasManualConfig = BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            BuildConfig.FIREBASE_GCM_SENDER_ID.isNotBlank()

        if (hasManualConfig) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setGcmSenderId(BuildConfig.FIREBASE_GCM_SENDER_ID)
                    .build(),
            )
            return
        }

        FirebaseApp.initializeApp(context)
    }
}
