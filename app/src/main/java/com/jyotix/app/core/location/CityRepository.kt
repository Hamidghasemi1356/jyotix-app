package com.jyotix.app.core.location

import android.content.Context
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.db.entities.CityEntity

/**
 * Loads the bundled city database (JSON in assets/cities/) into Room on first
 * launch, and provides search for the birth-information form.
 */
class CityRepository(private val context: Context) {

    private val dao = JyotixDatabase.get(context).cityDao()

    /**
     * Populate the city database from the bundled JSON asset if it is empty.
     * Safe to call on every launch — only inserts if database is empty.
     */
    suspend fun ensureLoaded() {
        if (dao.count() > 0) return
        val cities = loadBundledCities()
        if (cities.isNotEmpty()) dao.insertAll(cities)
    }

    /**
     * Search cities by name (in English or Persian).
     */
    suspend fun search(query: String, limit: Int = 50): List<CityEntity> {
        if (query.isBlank()) return emptyList()
        return dao.search(query, limit)
    }

    /**
     * Get a city by exact name.
     */
    suspend fun byName(name: String): CityEntity? = dao.byName(name)

    /**
     * Load cities from assets/cities/cities.json.
     *
     * Format (sample):
     * ```json
     * [
     *   {"name":"Tehran","name_fa":"تهران","country":"Iran","country_fa":"ایران",
     *    "latitude":35.6892,"longitude":51.3890,"timezone_id":"Asia/Tehran",
     *    "utc_offset_hours":3.5,"population":8693706},
     *   ...
     * ]
     * ```
     */
    private fun loadBundledCities(): List<CityEntity> {
        return try {
            val json = context.assets.open("cities/cities.json").bufferedReader().use { it.readText() }
            parseCitiesJson(json)
        } catch (e: Throwable) {
            android.util.Log.w(TAG, "Failed to load bundled cities: ${e.message}")
            emptyList()
        }
    }

    /**
     * Minimal JSON parser for the cities file. Avoids the overhead of a
     * full Moshi adapter for the simple flat array structure.
     */
    private fun parseCitiesJson(json: String): List<CityEntity> {
        val result = mutableListOf<CityEntity>()
        // Strip outer brackets
        val trimmed = json.trim().removeSurrounding("[", "]")
        // Split on }, {
        val items = trimmed.split("},", "}")
        for (item in items) {
            val fields = mutableMapOf<String, String>()
            val cleaned = item.trim().removePrefix("{").trim()
            // Match "key": value,
            val regex = """"([^"]+)"\s*:\s*("[^"]*"|[^,}\s]+)""".toRegex()
            for (m in regex.findAll(cleaned)) {
                val key = m.groupValues[1]
                val value = m.groupValues[2].removeSurrounding("\"")
                fields[key] = value
            }
            if (fields.isEmpty()) continue
            try {
                result.add(
                    CityEntity(
                        name = fields["name"] ?: "",
                        nameFa = fields["name_fa"] ?: "",
                        country = fields["country"] ?: "",
                        countryFa = fields["country_fa"] ?: "",
                        latitude = fields["latitude"]?.toDoubleOrNull() ?: 0.0,
                        longitude = fields["longitude"]?.toDoubleOrNull() ?: 0.0,
                        timezoneId = fields["timezone_id"] ?: "UTC",
                        utcOffsetHours = fields["utc_offset_hours"]?.toDoubleOrNull() ?: 0.0,
                        population = fields["population"]?.toLongOrNull() ?: 0L
                    )
                )
            } catch (_: Throwable) { /* skip malformed */ }
        }
        return result
    }

    companion object {
        private const val TAG = "CityRepository"
    }
}
