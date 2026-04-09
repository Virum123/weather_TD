package com.example.weathertd.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.weathertd.model.WeatherSnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

class WeatherCache(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSnapshot(snapshot: WeatherSnapshot) {
        val updatedSnapshots = loadSnapshots()
            .filterNot { existing ->
                existing.locationId == snapshot.locationId &&
                    abs(existing.observedAtEpochSeconds - snapshot.observedAtEpochSeconds) <= SAME_HOUR_WINDOW
            }
            .filter { existing ->
                snapshot.observedAtEpochSeconds - existing.observedAtEpochSeconds <= RETENTION_WINDOW
            }
            .toMutableList()
            .apply { add(snapshot) }

        preferences.edit()
            .putString(SNAPSHOTS_KEY, gson.toJson(updatedSnapshots))
            .apply()
    }

    fun findYesterdaySnapshot(
        locationId: String,
        observedAtEpochSeconds: Long,
    ): WeatherSnapshot? {
        val target = observedAtEpochSeconds - ONE_DAY_SECONDS
        return loadSnapshots()
            .filter { snapshot -> snapshot.locationId == locationId }
            .minByOrNull { snapshot -> abs(snapshot.observedAtEpochSeconds - target) }
            ?.takeIf { snapshot ->
                abs(snapshot.observedAtEpochSeconds - target) <= YESTERDAY_MATCH_WINDOW
            }
    }

    private fun loadSnapshots(): List<WeatherSnapshot> {
        val raw = preferences.getString(SNAPSHOTS_KEY, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<WeatherSnapshot>>() {}.type
            gson.fromJson<List<WeatherSnapshot>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFERENCES_NAME = "weather_td_cache"
        const val SNAPSHOTS_KEY = "snapshots"
        const val ONE_DAY_SECONDS = 86_400L
        const val SAME_HOUR_WINDOW = 1_800L
        const val YESTERDAY_MATCH_WINDOW = 7_200L
        const val RETENTION_WINDOW = 60L * 60L * 72L
    }
}
