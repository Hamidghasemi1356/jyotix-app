package com.jyotix.app.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jyotix.app.core.db.entities.EducationArticleEntity
import com.jyotix.app.core.db.entities.AlertPreferenceEntity
import com.jyotix.app.core.db.entities.CachedAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EducationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<EducationArticleEntity>): List<Long>

    @Query("SELECT * FROM education_articles WHERE category = :category ORDER BY order_index ASC")
    fun byCategory(category: String): Flow<List<EducationArticleEntity>>

    @Query("SELECT * FROM education_articles ORDER BY category, order_index ASC")
    fun all(): Flow<List<EducationArticleEntity>>

    @Query("SELECT * FROM education_articles WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): EducationArticleEntity?

    @Query("SELECT DISTINCT category FROM education_articles")
    fun categories(): Flow<List<String>>
}

@Dao
interface AlertPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: AlertPreferenceEntity)

    @Query("SELECT * FROM alert_preferences")
    fun all(): Flow<List<AlertPreferenceEntity>>

    @Query("SELECT * FROM alert_preferences WHERE category = :category LIMIT 1")
    suspend fun byCategory(category: String): AlertPreferenceEntity?
}

@Dao
interface CachedAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: CachedAlertEntity): Long

    @Query("SELECT * FROM cached_alerts WHERE event_jd >= :fromJd ORDER BY event_jd ASC")
    fun upcoming(fromJd: Double): Flow<List<CachedAlertEntity>>

    @Query("SELECT * FROM cached_alerts ORDER BY event_jd ASC")
    fun all(): Flow<List<CachedAlertEntity>>

    @Query("DELETE FROM cached_alerts WHERE event_jd < :beforeJd")
    suspend fun purgeBefore(beforeJd: Double)

    @Query("UPDATE cached_alerts SET shown = 1 WHERE id = :id")
    suspend fun markShown(id: Long)
}
