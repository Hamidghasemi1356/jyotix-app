package com.jyotix.app.core.calc

import swisseph.SwissEph
import java.util.Calendar
import java.util.TimeZone

/**
 * Planetary transit calculations.
 *
 * Computes current planetary positions and detects significant transit
 * events: sign changes (ingresses), retrograde stations, conjunctions, and
 * the Moon's passage through Scorpio (Qamar Dar Aqrab).
 */
class TransitEngine(private val engine: CalculationEngine) {

    /**
     * Snapshot of current planetary positions and Moon-in-Scorpio status.
     */
    data class TransitSnapshot(
        val julianDay: Double,
        val positions: Map<Planet, PlanetPosition>,
        val moonInScorpio: Boolean,
        val retrogradePlanets: List<Planet>,
        val ascendant: AscendantInfo?
    )

    /** Take a transit snapshot for "now" or any given Julian Day. */
    fun snapshot(latitude: Double, longitude: Double, jd: Double? = null): TransitSnapshot {
        val realJd = jd ?: nowJd()
        val positions: Map<Planet, PlanetPosition> = Planet.entries.associateWith { p ->
            engine.computePlanet(p, realJd)
        }
        val moonInScorpio = positions[Planet.MOON]?.sign == ZodiacSign.SCORPIO
        val retrograde = positions.values.filter { it.isRetrograde }.map { it.planet }
        val asc = try { engine.computeAscendant(realJd, latitude, longitude) } catch (_: Throwable) { null }
        return TransitSnapshot(realJd, positions, moonInScorpio, retrograde, asc)
    }

    /**
     * Compare transits to a saved natal chart: returns a list of planets whose
     * transit position is in the same sign as the natal position (transit over
     * natal) and those aspecting natal planets.
     */
    data class TransitNatalComparison(
        val transitsOverNatal: List<Pair<Planet, Planet>>, // transit planet, natal planet
        val aspectsToNatal: List<Aspects.Aspect>
    )

    fun compareTransitsToNatal(
        natal: ChartCalculationResult,
        atJd: Double
    ): TransitNatalComparison {
        val transitPositions: Map<Planet, PlanetPosition> = Planet.entries.associateWith { p ->
            engine.computePlanet(p, atJd)
        }
        val overlaps = mutableListOf<Pair<Planet, Planet>>()
        for ((tP, tPos) in transitPositions) {
            for ((nP, nPos) in natal.planets) {
                if (tPos.sign == nPos.sign) overlaps.add(tP to nP)
            }
        }
        // Build a pseudo-result with the transit positions to use the aspects engine
        val transitChart = ChartCalculationResult(
            birthData = natal.birthData,
            config = natal.config,
            julianDay = atJd,
            ayanamsaValue = engine.ayanamsa(atJd),
            planets = transitPositions,
            ascendant = natal.ascendant
        )
        return TransitNatalComparison(overlaps, Aspects.computeGrahaDrishti(transitChart))
    }

    // ===== Moon in Scorpio (Qamar Dar Aqrab) =====

    data class MoonInScorpioPeriod(
        val startJd: Double,
        val endJd: Double,
        val moonEntryLongitude: Double, // = 210° (Scorpio start)
        val moonExitLongitude: Double   // = 240° (Sagittarius start)
    ) {
        val durationHours: Double get() = (endJd - startJd) * 24.0
    }

    /**
     * Find the next Moon-in-Scorpio period after the given Julian Day.
     *
     * The Moon takes about 2.25–2.5 days to cross a sign. We sample every 30
     * minutes to find the exact entry and exit, then refine with a binary
     * search to within ±1 minute.
     */
    fun findNextMoonInScorpio(afterJd: Double): MoonInScorpioPeriod {
        val samplingStepDays = 30.0 / (24.0 * 60.0) // 30 minutes
        var currentJd = afterJd
        // First, scan forward at coarse resolution until we find Moon entering Scorpio
        var inScorpio = false
        var entryJd: Double? = null
        var exitJd: Double? = null
        var safety = 0
        while (safety < 2000) {
            val moon = engine.computePlanet(Planet.MOON, currentJd)
            val isNow = moon.sign == ZodiacSign.SCORPIO
            if (!inScorpio && isNow) {
                entryJd = refineScorpioBoundary(currentJd - samplingStepDays, currentJd, entering = true)
                inScorpio = true
            } else if (inScorpio && !isNow) {
                exitJd = refineScorpioBoundary(currentJd - samplingStepDays, currentJd, entering = false)
                break
            }
            currentJd += samplingStepDays
            safety++
        }
        return MoonInScorpioPeriod(
            startJd = entryJd ?: afterJd,
            endJd = exitJd ?: (afterJd + 3.0),
            moonEntryLongitude = ZodiacSign.SCORPIO.degreeStart,
            moonExitLongitude = ZodiacSign.SAGITTARIUS.degreeStart
        )
    }

    /**
     * Refine the Moon's exact entry/exit at the Scorpio boundary using binary
     * search within [lowJd, highJd]. Resolution: 1 minute.
     */
    private fun refineScorpioBoundary(lowJd: Double, highJd: Double, entering: Boolean): Double {
        var lo = lowJd
        var hi = highJd
        val minute = 1.0 / (24.0 * 60.0)
        while (hi - lo > minute) {
            val mid = (lo + hi) / 2.0
            val moonAtMid = engine.computePlanet(Planet.MOON, mid)
            val inScorpio = moonAtMid.sign == ZodiacSign.SCORPIO
            if (entering) {
                if (inScorpio) hi = mid else lo = mid
            } else {
                if (inScorpio) lo = mid else hi = mid
            }
        }
        return (lo + hi) / 2.0
    }

    /**
     * Find the most recent Moon-in-Scorpio period before [beforeJd].
     */
    fun findPreviousMoonInScorpio(beforeJd: Double): MoonInScorpioPeriod {
        // The Moon crosses Scorpio roughly every 27.3 days. Scan backward up to 35 days.
        var currentJd = beforeJd
        // Find a moment when Moon was NOT in Scorpio, then find the last Scorpio period
        for (i in 0 until 100) {
            val p = findNextMoonInScorpio(currentJd - 30.0)
            if (p.endJd < beforeJd) return p
            currentJd -= 1.0
        }
        return findNextMoonInScorpio(beforeJd - 28.0)
    }

    /**
     * Find the next N upcoming Moon-in-Scorpio periods after [afterJd].
     */
    fun findUpcomingMoonInScorpio(afterJd: Double, count: Int): List<MoonInScorpioPeriod> {
        val results = mutableListOf<MoonInScorpioPeriod>()
        var jd = afterJd
        repeat(count) {
            val p = findNextMoonInScorpio(jd)
            results.add(p)
            jd = p.endJd + 1.0
        }
        return results
    }

    // ===== Astronomical events =====

    sealed class AstronomicalEvent {
        abstract val jd: Double
        abstract val title: String
        abstract val description: String

        data class SignIngress(override val jd: Double, val planet: Planet, val newSign: ZodiacSign) : AstronomicalEvent() {
            override val title: String get() = "ورود ${planet.persianName} به ${newSign.persianName}"
            override val description: String get() = "سیاره ${planet.persianName} وارد برج ${newSign.persianName} می‌شود."
        }
        data class RetrogradeStation(override val jd: Double, val planet: Planet, val begins: Boolean) : AstronomicalEvent() {
            override val title: String get() =
                if (begins) "شروع رجعت ${planet.persianName}"
                else "پایان رجعت ${planet.persianName}"
            override val description: String get() =
                if (begins) "سیاره ${planet.persianName} از حرکت مستقیم خارج و رجوعی می‌شود."
                else "سیاره ${planet.persianName} به حرکت مستقیم بازمی‌گردد."
        }
        data class Conjunction(override val jd: Double, val planet1: Planet, val planet2: Planet, val separationDegrees: Double) : AstronomicalEvent() {
            override val title: String get() = "مقارنه ${planet1.persianName} و ${planet2.persianName}"
            override val description: String get() = "فاصله زاویه‌ای ${planet1.persianName} و ${planet2.persianName} به ${String.format("%.2f", separationDegrees)}° می‌رسد."
        }
        data class MoonInScorpioEvent(override val jd: Double, val begins: Boolean) : AstronomicalEvent() {
            override val title: String get() =
                if (begins) "قمر در عقرب - شروع"
                else "قمر در عقرب - پایان"
            override val description: String get() =
                if (begins) "ماه وارد برج عقرب می‌شود."
                else "ماه از برج عقرب خارج می‌شود."
        }
    }

    /**
     * Scan a date range for astronomical events. Used by the alerts system.
     */
    fun scanEvents(fromJd: Double, toJd: Double): List<AstronomicalEvent> {
        val events = mutableListOf<AstronomicalEvent>()
        val stepDays = 6.0 / 24.0 // 6 hours
        var prevPositions: Map<Planet, PlanetPosition>? = null
        var jd = fromJd
        while (jd < toJd) {
            val positions = Planet.entries.associateWith { p -> engine.computePlanet(p, jd) }
            prevPositions?.let { prev ->
                for (p in Planet.entries) {
                    val now = positions[p] ?: continue
                    val prevPos = prev[p] ?: continue
                    if (now.sign != prevPos.sign && p != Planet.KETU) {
                        // Sign ingress
                        events.add(AstronomicalEvent.SignIngress(jd, p, now.sign))
                    }
                    if (p.canRetrograde) {
                        if (!prevPos.isRetrograde && now.isRetrograde) {
                            events.add(AstronomicalEvent.RetrogradeStation(jd, p, begins = true))
                        }
                        if (prevPos.isRetrograde && !now.isRetrograde) {
                            events.add(AstronomicalEvent.RetrogradeStation(jd, p, begins = false))
                        }
                    }
                }
                // Conjunctions: any pair of planets with separation < 1°
                val planets = positions.keys.toList()
                for (i in planets.indices) {
                    for (j in (i + 1) until planets.size) {
                        val p1 = planets[i]; val p2 = planets[j]
                        val sep = absAngleDiff(positions[p1]!!.longitude, positions[p2]!!.longitude)
                        val prevSep = absAngleDiff(prev[p1]!!.longitude, prev[p2]!!.longitude)
                        if (sep < 1.0 && prevSep >= 1.0) {
                            events.add(AstronomicalEvent.Conjunction(jd, p1, p2, sep))
                        }
                    }
                }
                // Moon in Scorpio boundary
                val moonNow = positions[Planet.MOON]!!
                val moonPrev = prev[Planet.MOON]!!
                if (moonPrev.sign != ZodiacSign.SCORPIO && moonNow.sign == ZodiacSign.SCORPIO) {
                    events.add(AstronomicalEvent.MoonInScorpioEvent(jd, begins = true))
                }
                if (moonPrev.sign == ZodiacSign.SCORPIO && moonNow.sign != ZodiacSign.SCORPIO) {
                    events.add(AstronomicalEvent.MoonInScorpioEvent(jd, begins = false))
                }
            }
            prevPositions = positions
            jd += stepDays
        }
        return events
    }

    private fun absAngleDiff(a: Double, b: Double): Double {
        val d = (((a - b) % 360.0) + 360.0) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }

    private fun nowJd(): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return SwissEph.swe_julday(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0,
            SwissEph.SE_GREG_CAL
        )
    }
}
