package com.jyotix.app.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jyotix.app.core.db.dao.AlertPreferenceDao
import com.jyotix.app.core.db.dao.CachedAlertDao
import com.jyotix.app.core.db.dao.CityDao
import com.jyotix.app.core.db.dao.EducationDao
import com.jyotix.app.core.db.dao.SavedChartDao
import com.jyotix.app.core.db.dao.UserDao
import com.jyotix.app.core.db.entities.AlertPreferenceEntity
import com.jyotix.app.core.db.entities.CachedAlertEntity
import com.jyotix.app.core.db.entities.CityEntity
import com.jyotix.app.core.db.entities.EducationArticleEntity
import com.jyotix.app.core.db.entities.SavedChartEntity
import com.jyotix.app.core.db.entities.UserEntity

/**
 * Main Jyotix local database.
 *
 * Holds:
 *   • The current user profile (after Firebase phone-auth).
 *   • Saved birth charts with full calculation results cached as JSON.
 *   • A built-in searchable city database.
 *   • Education articles (CMS-driven content).
 *   • Alert preferences and cached alert notifications.
 */
@Database(
    entities = [
        UserEntity::class,
        SavedChartEntity::class,
        CityEntity::class,
        EducationArticleEntity::class,
        AlertPreferenceEntity::class,
        CachedAlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JyotixDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun savedChartDao(): SavedChartDao
    abstract fun cityDao(): CityDao
    abstract fun educationDao(): EducationDao
    abstract fun alertPreferenceDao(): AlertPreferenceDao
    abstract fun cachedAlertDao(): CachedAlertDao

    companion object {
        @Volatile private var INSTANCE: JyotixDatabase? = null

        fun get(context: Context): JyotixDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                JyotixDatabase::class.java,
                "jyotix.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
