package com.poimelder.app.overpass

import com.poimelder.app.model.Category
import com.poimelder.app.model.Poi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Holt POIs von der Overpass-API. Setzt einen aussagekräftigen User-Agent
 * (OSM-Nutzungsregeln). Reine Netzwerklogik; Query-Bau und Parsing sind ausgelagert
 * und separat testbar.
 *
 * Bei Serverfehlern (z.B. HTTP 504 durch Überlastung des Hauptservers) werden der
 * Reihe nach mehrere Overpass-Mirror probiert.
 */
class OverpassClient(
    private val endpoints: List<String> = DEFAULT_ENDPOINTS,
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val httpClient: OkHttpClient = defaultClient(),
) {

    /** Führt die Abfrage aus (auf dem IO-Dispatcher). Wirft, wenn alle Endpoints scheitern. */
    suspend fun fetchPois(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        categories: List<Category>,
    ): List<Poi> {
        if (categories.isEmpty()) return emptyList()
        val ql = OverpassQuery.build(lat, lon, radiusMeters, categories, timeoutSec = 60)
        return withContext(Dispatchers.IO) {
            var lastError: Throwable = IllegalStateException("Keine Overpass-Endpoints konfiguriert")
            for (endpoint in endpoints) {
                val attempt = runCatching { requestOnce(endpoint, ql, categories) }
                attempt.onSuccess { return@withContext it }
                attempt.onFailure { lastError = it }
            }
            throw lastError
        }
    }

    private fun requestOnce(endpoint: String, ql: String, categories: List<Category>): List<Poi> {
        val body = ql.toRequestBody(FORM_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", userAgent)
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Overpass HTTP ${response.code} @ ${hostOf(endpoint)}")
            }
            val json = response.body?.string().orEmpty()
            return OverpassParser.parse(json, categories)
        }
    }

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host }.getOrNull() ?: url

    companion object {
        /** Hauptserver zuerst, danach bewährte Mirror als Fallback. */
        val DEFAULT_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        )
        const val DEFAULT_USER_AGENT =
            "POIMelder/1.0 (Android; +https://github.com/Asparagus11/POIMelder)"

        private val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
