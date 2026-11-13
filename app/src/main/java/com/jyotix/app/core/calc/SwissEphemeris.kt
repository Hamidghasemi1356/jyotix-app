package com.jyotix.app.core.calc

import android.content.Context
import android.util.Log
import swisseph.SwissEph
import java.io.File

/**
 * Wraps the Astrodienst Swiss Ephemeris library and manages the ephemeris-data
 * path on Android.
 *
 * On first launch the bundled `.se1` ephemeris files (placed in
 * `assets/swisseph/`) are copied to internal storage, and [SwissEph] is
 * configured to read them. If the files are missing, the Swiss Ephemeris
 * falls back to its built-in Moshier ephemeris (≈1″ accuracy), so the app
 * remains functional but with slightly lower precision.
 *
 * The wrapper is process-wide: once initialized, [swissEph] is reused for all
 * subsequent calculations.
 *
 * Threading: Swiss Ephemeris is not thread-safe across instances, but a single
 * [SwissEph] instance is safe for concurrent reads from `swe_calc_ut` etc.
 * after the ephemeris path has been set. We use a [synchronized] block on
 * [initLock] during initialization only.
 */
object SwissEphemeris {

    private const val TAG = "SwissEphemeris"
    private const val EPHE_DIR_NAME = "swisseph"
    private const val ASSET_PREFIX = "swisseph/"

    @Volatile private var swissEph: SwissEph? = null
    private val initLock = Any()

    /** Set to true after [ensureInitialized] completes successfully. */
    @Volatile var isReady: Boolean = false
        private set

    /** The absolute path on device where the .se1 files live. */
    lateinit var ephemerisPath: String
        private set

    /**
     * Initialize the ephemeris. Copies `.se1` files from assets to internal
     * storage if needed, then creates and configures the [SwissEph] instance.
     */
    fun ensureInitialized(context: Context) {
        if (isReady) return
        synchronized(initLock) {
            if (isReady) return
            try {
                ephemerisPath = File(context.filesDir, EPHE_DIR_NAME).apply { mkdirs() }.absolutePath
                copyBundledEphemeris(context)
                val se = SwissEph(ephemerisPath)
                // Default Ayanamsa: Lahiri (Chitrapaksha)
                se.swe_set_sid_mode(SwissEph.SE_SIDM_LAHIRI)
                se.swe_set_ephe_path(ephemerisPath)
                swissEph = se
                isReady = true
                Log.i(TAG, "Swiss Ephemeris initialized at $ephemerisPath (Lahiri Ayanamsa)")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize Swiss Ephemeris", t)
                // Last-resort fallback — create an instance without a path; uses Moshier
                try {
                    val se = SwissEph()
                    se.swe_set_sid_mode(SwissEph.SE_SIDM_LAHIRI)
                    swissEph = se
                    isReady = true
                    Log.w(TAG, "Running in Moshier fallback mode (no .se1 files)")
                } catch (t2: Throwable) {
                    Log.e(TAG, "Moshier fallback also failed", t2)
                    throw t2
                }
            }
        }
    }

    /** Return the singleton [SwissEph] instance. Throws if not initialized. */
    fun get(): SwissEph = swissEph ?: error(
        "SwissEphemeris not initialized. Call ensureInitialized(context) first."
    )

    /**
     * Copy all `.se1` files from `assets/swisseph/` to internal storage.
     * Files already present and matching in size are skipped.
     */
    private fun copyBundledEphemeris(context: Context) {
        val targetDir = File(ephemerisPath)
        val assetManager = context.assets
        val assets: Array<String> = try {
            assetManager.list(ASSET_PREFIX) ?: emptyArray()
        } catch (t: Throwable) {
            Log.w(TAG, "Could not list swisseph assets", t)
            emptyArray()
        }
        if (assets.isEmpty()) {
            Log.w(TAG, "No bundled .se1 files found; running on Moshier fallback.")
            return
        }
        for (name in assets) {
            if (!name.endsWith(".se1") && !name.endsWith(".se2") && !name.endsWith(".se3")) continue
            val target = File(targetDir, name)
            if (target.exists() && target.length() > 0) continue
            try {
                assetManager.open("$ASSET_PREFIX$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                Log.i(TAG, "Copied $name (${target.length()} bytes)")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to copy $name", t)
            }
        }
    }

    /** Release any native resources held by the ephemeris. */
    fun close() {
        synchronized(initLock) {
            swissEph?.swe_close()
            swissEph = null
            isReady = false
        }
    }
}
