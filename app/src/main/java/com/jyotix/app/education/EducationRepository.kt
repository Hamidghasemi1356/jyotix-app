package com.jyotix.app.education

import android.content.Context
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.db.entities.EducationArticleEntity

/**
 * Loads the bundled education content (assets/education/education.json) into
 * the Room database on first launch.
 */
class EducationRepository(private val context: Context) {

    private val dao = JyotixDatabase.get(context).educationDao()

    suspend fun ensureLoaded() {
        val currentCount = try { dao.categories().let { /* cannot easily count via flow */ } } catch (_: Throwable) {}
        // Simpler: try inserting all; REPLACE on conflict handles re-runs.
        val articles = loadBundledArticles()
        if (articles.isNotEmpty()) dao.insertAll(articles)
    }

    private fun loadBundledArticles(): List<EducationArticleEntity> {
        return try {
            val json = context.assets.open("education/education.json").bufferedReader().use { it.readText() }
            parseEducationJson(json)
        } catch (e: Throwable) {
            android.util.Log.w(TAG, "Failed to load education content: ${e.message}")
            emptyList()
        }
    }

    private fun parseEducationJson(json: String): List<EducationArticleEntity> {
        val result = mutableListOf<EducationArticleEntity>()
        val trimmed = json.trim().removeSurrounding("[", "]")
        val items = trimmed.split("},").map { it.trim() }
        for ((idx, raw) in items.withIndex()) {
            val item = if (idx == items.lastIndex) raw.removeSuffix("]") else raw + "}"
            val fields = mutableMapOf<String, String>()
            val cleaned = item.trim().removePrefix("{").removeSuffix("}").trim()
            val regex = """"([^"]+)"\s*:\s*("[^"]*"|[^,}]+)""".toRegex()
            for (m in regex.findAll(cleaned)) {
                val key = m.groupValues[1]
                var value = m.groupValues[2]
                if (value.startsWith("\"")) value = value.removeSurrounding("\"")
                fields[key] = value
            }
            if (fields.isEmpty()) continue
            try {
                result.add(
                    EducationArticleEntity(
                        category = fields["category"] ?: "uncategorized",
                        titleFa = fields["title_fa"] ?: "",
                        titleEn = fields["title_en"] ?: "",
                        bodyFa = fields["body_fa"] ?: "",
                        bodyEn = fields["body_en"] ?: "",
                        imagePath = fields["image_path"],
                        orderIndex = fields["order_index"]?.toIntOrNull() ?: idx
                    )
                )
            } catch (_: Throwable) { /* skip */ }
        }
        return result
    }

    companion object {
        private const val TAG = "EducationRepository"
    }
}
