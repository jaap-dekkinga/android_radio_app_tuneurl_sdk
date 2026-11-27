package com.tuneurlradio.app.data.ksal

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Current conditions for Salina from the National Weather Service
 * (api.weather.gov, no key). Port of iOS WeatherStore.swift.
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    data class Conditions(val temperature: Int, val unit: String, val summary: String)

    data class State(val conditions: Conditions? = null, val isLoading: Boolean = false)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val mutex = Mutex()
    private var hourlyUrl: String? = null
    private var lastUpdate = 0L

    suspend fun refreshIfNeeded() {
        if (System.currentTimeMillis() - lastUpdate < 15 * 60 * 1000L) return
        refresh()
    }

    suspend fun refresh() {
        if (!mutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                val hourly = hourlyUrl ?: get("https://api.weather.gov/points/${Ksal.LATITUDE},${Ksal.LONGITUDE}")
                    .obj("properties")?.str("forecastHourly")
                    ?.also { hourlyUrl = it }
                    ?: return@withContext
                val now = get(hourly).obj("properties")?.arr("periods")
                    ?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject ?: return@withContext
                _state.value = _state.value.copy(
                    conditions = Conditions(
                        temperature = now.get("temperature").asInt,
                        unit = now.str("temperatureUnit") ?: "F",
                        summary = now.str("shortForecast").orEmpty()
                    )
                )
                lastUpdate = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Keep the last value; the UI shows a fallback when there is none.
            Log.w("KsalWeather", "Weather request failed", e)
        } finally {
            _state.value = _state.value.copy(isLoading = false)
            mutex.unlock()
        }
    }

    private fun get(url: String) = okHttpClient.newCall(
        Request.Builder()
            .url(url)
            // NWS asks every client to identify itself.
            .header("User-Agent", "TuneURLRadio Android app (ksal.com news)")
            .header("Accept", "application/geo+json")
            .build()
    ).execute().use { response ->
        if (!response.isSuccessful) throw Exception("NWS answered ${response.code}")
        JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
    }
}
