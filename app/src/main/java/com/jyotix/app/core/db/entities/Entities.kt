package com.jyotix.app.core.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A registered user account.
 *
 * Authentication is done via Firebase Phone Auth; the row here stores the
 * local profile (display name, default location, settings).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val phone: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "default_latitude") val defaultLatitude: Double = 35.6892,
    @ColumnInfo(name = "default_longitude") val defaultLongitude: Double = 51.3890,
    @ColumnInfo(name = "default_timezone_id") val defaultTimezoneId: String = "Asia/Tehran",
    @ColumnInfo(name = "default_city_name") val defaultCityName: String = "Tehran",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_login_at") val lastLoginAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_premium") val isPremium: Boolean = false
)

/**
 * A saved birth chart owned by a user.
 *
 * The full calculation result is stored as JSON in [chartJson] for offline
 * re-display without re-running the ephemeris.
 */
@Entity(tableName = "saved_charts", foreignKeys = [
    androidx.room.ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["phone"],
        childColumns = ["owner_phone"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
data class SavedChartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner_phone") val ownerPhone: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "birth_year") val birthYear: Int,
    @ColumnInfo(name = "birth_month") val birthMonth: Int,
    @ColumnInfo(name = "birth_day") val birthDay: Int,
    @ColumnInfo(name = "birth_hour") val birthHour: Int,
    @ColumnInfo(name = "birth_minute") val birthMinute: Int,
    @ColumnInfo(name = "birth_second") val birthSecond: Int = 0,
    @ColumnInfo(name = "time_zone_id") val timeZoneId: String,
    @ColumnInfo(name = "utc_offset_hours") val utcOffsetHours: Double,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "city_name") val cityName: String,
    @ColumnInfo(name = "country_name") val countryName: String,
    @ColumnInfo(name = "chart_json") val chartJson: String,  // serialized ChartCalculationResult
    @ColumnInfo(name = "ayanamsa") val ayanamsa: String = "LAHIRI",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "notes") val notes: String = ""
)

/**
 * A searchable city in the Jyotix built-in database.
 */
@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "name_fa") val nameFa: String,
    @ColumnInfo(name = "country") val country: String,
    @ColumnInfo(name = "country_fa") val countryFa: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "timezone_id") val timezoneId: String,
    @ColumnInfo(name = "utc_offset_hours") val utcOffsetHours: Double,
    @ColumnInfo(name = "population") val population: Long = 0
)

/**
 * Educational content (CMS-driven).
 */
@Entity(tableName = "education_articles")
data class EducationArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category") val category: String,  // "intro_jyotish", "zodiac", "planets", etc.
    @ColumnInfo(name = "title_fa") val titleFa: String,
    @ColumnInfo(name = "title_en") val titleEn: String,
    @ColumnInfo(name = "body_fa") val bodyFa: String,
    @ColumnInfo(name = "body_en") val bodyEn: String,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Notification preferences for astronomical alerts.
 */
@Entity(tableName = "alert_preferences")
data class AlertPreferenceEntity(
    @PrimaryKey val category: String,  // "sign_change", "retrograde", "conjunction", "eclipse", "moon_scorpio", "transits"
    @ColumnInfo(name = "enabled") val enabled: Boolean = true,
    @ColumnInfo(name = "lead_time_hours") val leadTimeHours: Int = 12
)

/**
 * Cached alert notifications.
 */
@Entity(tableName = "cached_alerts")
data class CachedAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "title_fa") val titleFa: String,
    @ColumnInfo(name = "body_fa") val bodyFa: String,
    @ColumnInfo(name = "event_jd") val eventJd: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "shown") val shown: Boolean = false
)
