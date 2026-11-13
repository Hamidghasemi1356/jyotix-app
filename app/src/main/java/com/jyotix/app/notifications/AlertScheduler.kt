package com.jyotix.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jyotix.app.MainActivity
import com.jyotix.app.R
import com.jyotix.app.core.calc.CalculationEngine
import com.jyotix.app.core.calc.TransitEngine
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.db.entities.AlertPreferenceEntity
import com.jyotix.app.core.db.entities.CachedAlertEntity
import swisseph.SwissEph
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling and dispatching astronomical alerts via WorkManager.
 *
 * A periodic worker ([AlertScanWorker]) runs daily, scanning the next 7 days
 * for astronomical events using the [TransitEngine], and caches them in the
 * Room database. Alerts matching enabled categories are then posted as
 * Android notifications.
 */
class AlertScheduler(
    private val context: Context,
    private val engine: CalculationEngine
) {

    fun scheduleDailyScan() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val request = PeriodicWorkRequestBuilder<AlertScanWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ALERT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(ALERT_WORK_NAME)
    }

    companion object {
        const val ALERT_WORK_NAME = "jyotix_daily_alert_scan"
        const val CHANNEL_ID = "jyotix_alerts"
        const val CHANNEL_NAME_FA = "هشدارهای نجومی جیوتیکس"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mgr = context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME_FA,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for planetary events, transits, eclipses, and Moon in Scorpio."
                    enableVibration(true)
                }
                mgr?.createNotificationChannel(channel)
            }
        }
    }
}

/**
 * Worker that runs daily to scan for upcoming astronomical events.
 */
class AlertScanWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val app = applicationContext as? com.jyotix.app.JyotixApp
            ?: return Result.success()
        val engine = app.calculationEngine
        if (!::engine.isInitialized) return Result.retry()
        val transit = TransitEngine(engine)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val fromJd = SwissEph.swe_julday(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0,
            SwissEph.SE_GREG_CAL
        )
        val toJd = fromJd + 7.0  // scan 7 days ahead
        val events = transit.scanEvents(fromJd, toJd)

        val db = JyotixDatabase.get(applicationContext)
        val prefDao = db.alertPreferenceDao()
        val alertDao = db.cachedAlertDao()

        // Ensure default prefs exist
        ensureDefaultPrefs(prefDao)

        // Cache events
        for (e in events) {
            val category = when (e) {
                is TransitEngine.AstronomicalEvent.SignIngress -> "sign_change"
                is TransitEngine.AstronomicalEvent.RetrogradeStation -> "retrograde"
                is TransitEngine.AstronomicalEvent.Conjunction -> "conjunction"
                is TransitEngine.AstronomicalEvent.MoonInScorpioEvent -> "moon_scorpio"
                else -> "transits"
            }
            val pref = prefDao.byCategory(category)
            if (pref?.enabled == false) continue
            alertDao.insert(
                CachedAlertEntity(
                    category = category,
                    titleFa = e.title,
                    bodyFa = e.description,
                    eventJd = e.jd
                )
            )
        }

        AlertScheduler.ensureChannel(applicationContext)
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }

    private suspend fun ensureDefaultPrefs(dao: com.jyotix.app.core.db.dao.AlertPreferenceDao) {
        val defaults = listOf("sign_change", "retrograde", "conjunction", "eclipse", "moon_scorpio", "transits")
        for (c in defaults) {
            if (dao.byCategory(c) == null) {
                dao.upsert(AlertPreferenceEntity(category = c, enabled = true, leadTimeHours = 12))
            }
        }
    }
}
