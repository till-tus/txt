package com.example.textlauncher.data

import com.example.textlauncher.domain.WeatherSnapshot
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONObject

class OpenMeteoWeatherRepository {
    fun loadCurrentWeather(latitude: Double, longitude: Double): WeatherSnapshot {
        val url = URL(
            String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=%.5f&longitude=%.5f" +
                    "&current=temperature_2m" +
                    "&hourly=precipitation_probability" +
                    "&forecast_days=1&timezone=auto",
                latitude,
                longitude,
            ),
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Open-Meteo returned HTTP $responseCode")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            payload.toWeatherSnapshot()
        } finally {
            connection.disconnect()
        }
    }

    private fun String.toWeatherSnapshot(): WeatherSnapshot {
        val root = JSONObject(this)
        val current = root.getJSONObject("current")
        val currentHour = current.optString("time").takeIf { it.length >= 13 }
            ?.substring(0, 13)
            ?.plus(":00")
        val hourly = root.optJSONObject("hourly")
        return WeatherSnapshot(
            temperatureCelsius = current.getDouble("temperature_2m"),
            precipitationChancePercent = hourly?.precipitationChanceFor(currentHour),
        )
    }

    private fun JSONObject.precipitationChanceFor(currentHour: String?): Int? {
        val times = optJSONArray("time") ?: return null
        val chances = optJSONArray("precipitation_probability") ?: return null
        val index = if (currentHour == null) {
            0
        } else {
            (0 until times.length()).firstOrNull { times.optString(it) == currentHour } ?: 0
        }
        return chances.optInt(index).coerceIn(0, 100)
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
    }
}
