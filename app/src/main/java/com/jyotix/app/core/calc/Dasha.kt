package com.jyotix.app.core.calc

import swisseph.SwissEph
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Vimshottari Dasha calculation.
 *
 * The Vimshottari Dasha is a 120-year planetary period system based on the
 * Moon's Nakshatra at birth. Each of the 9 grahas rules a fixed number of
 * years in a fixed sequence (Ketu, Venus, Sun, Moon, Mars, Rahu, Jupiter,
 * Saturn, Mercury).
 *
 * The exact starting offset within the Mahadasha is determined by the
 * fraction of the Moon's Nakshatra already traversed at birth.
 */
object Dasha {

    /** The Vimshottari sequence, in order, starting from Krittika (Sun's dasha begins the cycle).
     *  But the astronomical sequence starting from Ashvini begins with Ketu. */
    private val SEQUENCE: List<Planet> = listOf(
        Planet.KETU, Planet.VENUS, Planet.SUN, Planet.MOON, Planet.MARS,
        Planet.RAHU, Planet.JUPITER, Planet.SATURN, Planet.MERCURY
    )

    /** Total dasha cycle = 120 years. */
    const val TOTAL_YEARS: Double = 120.0

    /** Lookup a planet's index in the dasha sequence (0..8). */
    private fun seqIndexOf(planet: Planet): Int = SEQUENCE.indexOf(planet)

    /**
     * One Dasha level — Mahadasha, Antardasha, or Pratyantardasha.
     *
     * [startJd] and [endJd] are Julian Day numbers. Use [SwissEph.swe_revjul]
     * to convert back to a calendar date.
     */
    data class DashaLevel(
        val lord: Planet,
        val startJd: Double,
        val endJd: Double,
        val level: Int  // 1 = Maha, 2 = Antar, 3 = Pratyantar
    ) {
        val durationYears: Double get() = (endJd - startJd) / 365.25
        val durationDays: Double get() = endJd - startJd

        fun formatDate(jd: Double): String {
            val cal = Calendar.getInstance(TimeZone.getDefault())
            val ymd = IntArray(3)
            val hms = DoubleArray(3)
            SwissEph.swe_revjul(jd, SwissEph.SE_GREG_CAL, ymd, hms)
            return String.format("%04d-%02d-%02d", ymd[0], ymd[1], ymd[2])
        }

        val startDateString: String get() = formatDate(startJd)
        val endDateString: String get() = formatDate(endJd)
    }

    /** A complete Mahadasha with its nested Antardashas. */
    data class Mahadasha(
        val level: DashaLevel,
        val antardashas: List<Antardasha>
    )

    /** An Antardasha with its nested Pratyantardashas. */
    data class Antardasha(
        val level: DashaLevel,
        val pratyantardashas: List<DashaLevel>
    )

    /**
     * Compute the entire Vimshottari Dasha sequence for the given Moon position.
     *
     * @param moonSiderealLongitude Sidereal longitude of the Moon at birth (degrees, 0–360).
     * @param birthJd Julian Day of birth (UT).
     * @param includeLevels 1 = Maha only, 2 = Maha+Antar, 3 = full tree. Default 3.
     * @return List of [Mahadasha], starting from the Mahadasha active at birth.
     */
    fun computeVimshottari(
        moonSiderealLongitude: Double,
        birthJd: Double,
        includeLevels: Int = 3
    ): List<Mahadasha> {
        val nakshatra = Nakshatra.fromLongitude(moonSiderealLongitude)
        // Lord of the birth Nakshatra determines the first Mahadasha
        val firstLord = nakshatra.vimshottariLord
        val firstSeqIdx = seqIndexOf(firstLord)

        // Fraction of the Nakshatra already traversed at birth
        val withinNakshatra = ((moonSiderealLongitude % 360.0) + 360.0) % 360.0 - nakshatra.startLongitude
        val nakFraction = withinNakshatra / AstroConstants.NAKSHATRA_WIDTH
        // Years already elapsed in the first Mahadasha
        val yearsElapsed = nakFraction * firstLord.dashaDurationYears
        val daysElapsed = yearsElapsed * 365.25

        val result = mutableListOf<Mahadasha>()
        // Build nine Mahadashas, starting from the first active one
        for (offset in 0 until 9) {
            val seqIdx = (firstSeqIdx + offset) % 9
            val lord = SEQUENCE[seqIdx]
            val mahaYears = lord.dashaDurationYears
            val mahaStartJd: Double
            val mahaEndJd: Double
            if (offset == 0) {
                // First Mahadasha: starts before birth, ends after
                mahaStartJd = birthJd - daysElapsed
                mahaEndJd = mahaStartJd + mahaYears * 365.25
            } else {
                val prev = result.last().level
                mahaStartJd = prev.endJd
                mahaEndJd = mahaStartJd + mahaYears * 365.25
            }
            val mahaLevel = DashaLevel(lord, mahaStartJd, mahaEndJd, 1)

            val antardashas = mutableListOf<Antardasha>()
            if (includeLevels >= 2) {
                for (subOffset in 0 until 9) {
                    val subLord = SEQUENCE[(seqIdx + subOffset) % 9]
                    val antarYears = mahaYears * (subLord.dashaDurationYears / TOTAL_YEARS)
                    val antarStartJd: Double
                    if (antardashas.isEmpty()) antarStartJd = mahaStartJd
                    else antarStartJd = antardashas.last().level.endJd
                    val antarEndJd = antarStartJd + antarYears * 365.25
                    val antarLevel = DashaLevel(subLord, antarStartJd, antarEndJd, 2)

                    val pratyantas = mutableListOf<DashaLevel>()
                    if (includeLevels >= 3) {
                        for (pOffset in 0 until 9) {
                            val pLord = SEQUENCE[(seqIdx + subOffset + pOffset) % 9]
                            // Use subLord's sequence start? Actually pratyantardasha starts from subLord.
                            // Correct rule: pratyantardasha starts from the antardasha lord itself.
                            // So we re-seed from subLord's seqIdx:
                            val pSeqIdx = seqIndexOf(subLord)
                            val realLord = SEQUENCE[(pSeqIdx + pOffset) % 9]
                            val pYears = antarYears * (realLord.dashaDurationYears / TOTAL_YEARS)
                            val pStartJd: Double
                            if (pratyantas.isEmpty()) pStartJd = antarStartJd
                            else pStartJd = pratyantas.last().endJd
                            val pEndJd = pStartJd + pYears * 365.25
                            pratyantas.add(DashaLevel(realLord, pStartJd, pEndJd, 3))
                        }
                    }
                    antardashas.add(Antardasha(antarLevel, pratyantas))
                }
            }
            result.add(Mahadasha(mahaLevel, antardashas))
        }
        return result
    }

    /**
     * Compute the Vimshottari Dasha sequence from a complete natal chart result.
     */
    fun fromNatalChart(result: ChartCalculationResult, includeLevels: Int = 3): List<Mahadasha> {
        val moonLong = result.positionOf(Planet.MOON).longitude
        return computeVimshottari(moonLong, result.julianDay, includeLevels)
    }

    /**
     * Find the currently active Dasha at the given Julian Day.
     *
     * Returns a triple of (Mahadasha, Antardasha, Pratyantardasha) levels, or
     * nulls for levels below the active one if includeLevels is too low.
     */
    fun currentActive(
        dashas: List<Mahadasha>,
        jd: Double
    ): Triple<DashaLevel?, DashaLevel?, DashaLevel?> {
        val maha = dashas.firstOrNull { jd >= it.level.startJd && jd < it.level.endJd }
            ?: return Triple(null, null, null)
        val antar = maha.antardashas.firstOrNull { jd >= it.level.startJd && jd < it.level.endJd }
        val pratyanta = antar?.pratyantardashas?.firstOrNull { jd >= it.startJd && jd < it.endJd }
        return Triple(maha.level, antar?.level, pratyanta)
    }
}
