package com.jyotix.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.jyotix.app.core.auth.AuthManager
import com.jyotix.app.core.calc.CalculationEngine
import com.jyotix.app.core.calc.SwissEphemeris
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.location.CityRepository
import com.jyotix.app.education.EducationRepository
import com.jyotix.app.notifications.AlertScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application-wide singleton container.
 *
 * Holds references to the calculation engine, database, and repositories so
 * that they are created once and shared across screens. Initialization is
 * performed in [onCreate] on a background coroutine to avoid blocking the
 * UI thread.
 */
class JyotixApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var calculationEngine: CalculationEngine
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var cityRepository: CityRepository
        private set

    lateinit var educationRepository: EducationRepository
        private set

    lateinit var alertScheduler: AlertScheduler
        private set

    val database: JyotixDatabase by lazy { JyotixDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch {
            try {
                SwissEphemeris.ensureInitialized(this@JyotixApp)
                calculationEngine = CalculationEngine()
                Log.i(TAG, "Calculation engine ready")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize calculation engine", t)
            }
            try {
                cityRepository = CityRepository(this@JyotixApp).also { it.ensureLoaded() }
                educationRepository = EducationRepository(this@JyotixApp).also { it.ensureLoaded() }
                Log.i(TAG, "City + education databases loaded")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load databases", t)
            }
            try {
                alertScheduler = AlertScheduler(this@JyotixApp, calculationEngine)
                alertScheduler.scheduleDailyScan()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to schedule alert worker", t)
            }
        }
        authManager = AuthManager(this)
        // Configure WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    companion object {
        private const val TAG = "JyotixApp"
        @Volatile lateinit var instance: JyotixApp
            private set
    }
}
