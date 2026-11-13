package com.jyotix.app.core.calc

import swisseph.SwissEph
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Core astronomical calculation engine.
 *
 * All planet longitudes, the Ascendant, and house cusps are computed here
 * via the Swiss Ephemeris. The class is **thread-safe** for read-only use
 * after initialization: each public function synchronizes on the singleton
 * [SwissEph] instance.
 *
 * All returned longitudes are **sidereal** (Lahiri by default) and in the
 * range [0, 360).
 *
 * Accuracy:
 *   • With bundled .se1 ephemeris files: ≤ 0.001″ for planets 1800–2400.
 *   • With Moshier fallback: ≤ 1″ for planets, ≤ 5″ for Moon.
 *
 * See [SwissEphemeris] for setup; this class assumes [SwissEphemeris.ensureInitialized]
 * has already been called.
 */
class CalculationEngine(private val config: CalculationConfig = CalculationConfig()) {

    /** The Swiss Ephemeris instance — guaranteed non-null after [SwissEphemeris.ensureInitialized]. */
    private val swissEph: SwissEph get() = SwissEphemeris.get()

    /** Bit flags for `swe_calc_ut`. */
    private val calcFlags: Int get() {
        var flags = if (config.useSwissEphemeris) SwissEph.SEFLG_SWIEPH else SwissEph.SEFLG_MOSEPH
        if (config.sidereal) flags = flags or SwissEph.SEFLG_SIDEREAL
        if (config.topocentric) flags = flags or SwissEph.SEFLG_TOPOCTR
        if (config.trueNodes) flags = flags or SwissEph.SEFLG_TRUEPOS
        return flags
    }

    init {
        // Apply Ayanamsa mode
        swissEph.swe_set_sid_mode(config.ayanamsa.swissEphConstant)
    }

    /**
     * Compute the Julian Day Number (UT) from a [BirthData].
     */
    fun julianDay(birth: BirthData): Double {
        val tz = try { TimeZone.getTimeZone(birth.timeZoneId) } catch (_: Throwable) {
            TimeZone.getTimeZone("UTC")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            // Convert local to UTC by subtracting the offset
            set(birth.year, birth.month - 1, birth.day, birth.hour, birth.minute, birth.second)
            // Calendar in UTC already; if the original timeZoneId != UTC, shift
            val offsetMillis = TimeZone.getTimeZone(birth.timeZoneId).getOffset(
                birth.year, birth.month - 1, birth.day, Calendar.DAY_OF_MONTH, if (cal[Calendar.AM_PM] == 0) Calendar.AM else Calendar.PM, cal[Calendar.DST_OFFSET]
            )
            add(Calendar.MILLISECOND, -offsetMillis)
        }
        return SwissEph.swe_julday(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0,
            SwissEph.SE_GREG_CAL
        )
    }

    /**
     * Compute the Ayanamsa value at the given Julian Day.
     */
    fun ayanamsa(jd: Double): Double {
        return synchronized(swissEph) {
            swissEph.swe_get_ayanamsa_ut(jd)
        }
    }

    /**
     * Compute the position of a single planet at the given Julian Day.
     */
    fun computePlanet(planet: Planet, jd: Double): PlanetPosition = synchronized(swissEph) {
        val bodyCode = planet.swissEphId
        val errBuf = StringBuffer(256)
        val lon = DoubleArray(6)
        val flags = calcFlags
        val ret = swissEph.swe_calc_ut(jd, bodyCode, flags, lon, errBuf)
        if (ret < 0) {
            // Try fallback: Moshier
            val fallbackFlags = (flags and SwissEph.SEFLG_SWIEPH.inv()) or SwissEph.SEFLG_MOSEPH
            val ret2 = swissEph.swe_calc_ut(jd, bodyCode, fallbackFlags, lon, errBuf)
            if (ret2 < 0) error("Swiss Ephemeris calc failed for ${planet.name}: $errBuf")
        }
        val longitude = ((lon[0] % 360.0) + 360.0) % 360.0
        val dailyMotion = lon[3]
        val retrograde = when {
            !planet.canRetrograde -> RetrogradeStatus.DIRECT
            dailyMotion < -0.0001 -> RetrogradeStatus.RETROGRADE
            abs(dailyMotion) < 0.0001 -> RetrogradeStatus.STATIONARY
            else -> RetrogradeStatus.DIRECT
        }
        // For Ketu (south node): 180° opposite Rahu
        val finalLongitude = if (planet == Planet.KETU) {
            val rahuLon = ((lon[0] % 360.0) + 360.0) % 360.0
            ((rahuLon + 180.0) % 360.0 + 360.0) % 360.0
        } else longitude

        PlanetPosition(
            planet = planet,
            longitude = finalLongitude,
            dailyMotion = if (planet == Planet.KETU) -dailyMotion else dailyMotion,
            retrogradeStatus = if (planet == Planet.KETU) {
                // Ketu moves opposite Rahu
                if (dailyMotion > 0.0001) RetrogradeStatus.RETROGRADE
                else RetrogradeStatus.DIRECT
            } else retrograde
        )
    }

    /**
     * Compute the Ascendant (Lagna) and the twelve house cusps at the given
     * Julian Day for the given geographic coordinates.
     *
     * For Whole Sign houses (the Jyotish default), the cusps are simply the
     * sign-start longitudes starting from the Ascendant's sign.
     */
    fun computeAscendant(jd: Double, latitudeDeg: Double, longitudeDeg: Double): AscendantInfo = synchronized(swissEph) {
        val cusps = DoubleArray(37)
        val ascmc = DoubleArray(10)
        val hsysCode = when (config.houseSystem) {
            HouseSystem.WHOLE_SIGN -> 'W'
            HouseSystem.PLACIDUS -> 'P'
            HouseSystem.KOCH -> 'K'
            HouseSystem.REGIOMONTANUS -> 'R'
            HouseSystem.CAMPANUS -> 'C'
            HouseSystem.EQUAL -> 'A' // Equal house from Ascendant
            HouseSystem.PORPHYRIUS -> 'O'
            HouseSystem.Sripati -> 'S'
        }
        val flags = calcFlags
        swissEph.swe_houses(jd, flags, latitudeDeg, longitudeDeg, hsysCode.code, cusps, ascmc)
        // ascmc[0] = RAMC, [1] = MC, [2] = ARMC, [3] = Ascendant (tropical)
        val tropicalAsc = ascmc[1] // MC
        val mcTropical = ascmc[1]
        // Apply sidereal correction
        val ayanamsa = swissEph.swe_get_ayanamsa_ut(jd)
        // The Ascendant is cusps[1] in Placidus; for Whole Sign ('W') cusps[1..12] are sign starts
        val ascTropical = cusps[1]
        val ascSidereal = ((ascTropical - ayanamsa) % 360.0 + 360.0) % 360.0
        val mcSidereal = ((mcTropical - ayanamsa) % 360.0 + 360.0) % 360.0
        // Build house cusps (sidereal)
        val houseCuspSidereal: List<Double> = when (config.houseSystem) {
            HouseSystem.WHOLE_SIGN -> {
                val ascSign = ZodiacSign.fromLongitude(ascSidereal)
                (0 until 12).map { i ->
                    val s = ZodiacSign.fromIndex((ascSign.ordinal + i) % 12)
                    s.degreeStart
                }
            }
            else -> (1..12).map { i ->
                val c = cusps[i]
                ((c - ayanamsa) % 360.0 + 360.0) % 360.0
            }
        }
        AscendantInfo(
            longitude = ascSidereal,
            houseCusps = houseCuspSidereal,
            mcLongitude = mcSidereal,
            houseSystem = config.houseSystem
        )
    }

    /**
     * Compute the entire natal chart for the given [birth].
     *
     * This is the main entry point used by the natal chart screen.
     */
    fun computeNatalChart(birth: BirthData): ChartCalculationResult {
        val jd = julianDay(birth)
        val ayanamsa = ayanamsa(jd)
        val planets: Map<Planet, PlanetPosition> = Planet.entries.associateWith { p ->
            computePlanet(p, jd)
        }
        val ascendant = computeAscendant(jd, birth.latitudeDeg, birth.longitudeDeg)
        return ChartCalculationResult(
            birthData = birth,
            config = config,
            julianDay = jd,
            ayanamsaValue = ayanamsa,
            planets = planets,
            ascendant = ascendant
        )
    }

    /**
     * Compute the **current** transit positions for "now" (or any specified
     * Julian Day). Used by the Daily Transit dashboard.
     */
    fun computeTransitChart(
        latitudeDeg: Double,
        longitudeDeg: Double,
        jd: Double? = null
    ): ChartCalculationResult {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val realJd = jd ?: SwissEph.swe_julday(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0 + now.get(Calendar.SECOND) / 3600.0,
            SwissEph.SE_GREG_CAL
        )
        val ayanamsa = ayanamsa(realJd)
        val planets = Planet.entries.associateWith { p -> computePlanet(p, realJd) }
        val ascendant = computeAscendant(realJd, latitudeDeg, longitudeDeg)
        return ChartCalculationResult(
            birthData = BirthData(
                year = now.get(Calendar.YEAR),
                month = now.get(Calendar.MONTH) + 1,
                day = now.get(Calendar.DAY_OF_MONTH),
                hour = now.get(Calendar.HOUR_OF_DAY),
                minute = now.get(Calendar.MINUTE),
                timeZoneId = "UTC",
                utcOffsetHours = 0.0,
                latitudeDeg = latitudeDeg,
                longitudeDeg = longitudeDeg
            ),
            config = config,
            julianDay = realJd,
            ayanamsaValue = ayanamsa,
            planets = planets,
            ascendant = ascendant
        )
    }
}
