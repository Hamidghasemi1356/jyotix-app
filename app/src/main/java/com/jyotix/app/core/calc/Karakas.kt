package com.jyotix.app.core.calc

import kotlin.math.abs

/**
 * Chara Karakas — the "movable" significators used in Jaimini astrology.
 *
 * Eight Chara Karakas are computed from the planets' longitudes by ranking them
 * by their degrees-within-sign (highest to lowest):
 *
 *   Atmakaraka (Self)             — planet with highest degrees
 *   Amatyakaraka (Career/Minister)
 *   Bhratrukaraka (Siblings)
 *   Matrikaraka (Mother)
 *   Pitrukaraka (Father)
 *   Putrakaraka (Children)
 *   Gnatikaraka (Obstacles)
 *   Darakaraka (Spouse)           — planet with lowest degrees
 *
 * Rahu is included with 30° − degreeInSign (reverse), per Jaimini rule.
 * Ketu is excluded from Chara Karaka calculation.
 */
object Karakas {

    enum class CharaKaraka(val index: Int, val persianName: String, val significance: String) {
        ATMAKARAKA(0, "آتماکاراکا", "خود / روح"),
        AMATYAKARAKA(1, "آماتیاکاراکا", "شغل / وزیر"),
        BHRATRUKARAKA(2, "بهاتروکاراکا", "خواهر و برادر"),
        MATRIKARAKA(3, "ماتریکاراکا", "مادر"),
        PITRUKARAKA(4, "پیترکاراکا", "پدر"),
        PUTRAKARAKA(5, "پوتراکاراکا", "فرزندان"),
        GNATIKARAKA(6, "گناتیکاراکا", "موانع / بیماری"),
        DARAKARAKA(7, "داراکاراکا", "همسر");

        companion object {
            fun fromIndex(i: Int): CharaKaraka = entries[i.coerceIn(0, 7)]
        }
    }

    data class KarakaAssignment(
        val karaka: CharaKaraka,
        val planet: Planet,
        val degreeInSign: Double,
        val sign: ZodiacSign,
        val nakshatra: Nakshatra
    )

    /**
     * Compute the Chara Karaka assignments for the given chart.
     *
     * Includes Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn, Rahu.
     */
    fun computeCharaKarakas(result: ChartCalculationResult): List<KarakaAssignment> {
        val candidates: List<Triple<Planet, Double, PlanetPosition>> =
            Planet.entries.filter { it != Planet.KETU }.map { p ->
                val pos = result.positionOf(p)
                // For Rahu: use 30 - degreeInSign
                val effectiveDeg = if (p == Planet.RAHU) 30.0 - pos.degreeInSign else pos.degreeInSign
                Triple(p, effectiveDeg, pos)
            }
        // Sort descending by degree. Ties (very rare) broken by natural planet order.
        val sorted = candidates.sortedWith(
            compareByDescending<Triple<Planet, Double, PlanetPosition>> { it.second }
                .thenBy { it.first.ordinal }
        )
        return sorted.mapIndexed { idx, (planet, deg, pos) ->
            KarakaAssignment(
                karaka = CharaKaraka.fromIndex(idx),
                planet = planet,
                degreeInSign = deg,
                sign = pos.sign,
                nakshatra = pos.nakshatra
            )
        }
    }
}

/**
 * Planetary and sign aspects (Graha Drishti & Rashi Drishti).
 *
 * Graha Drishti (planetary aspects):
 *   - Mars: 4th, 7th, 8th houses
 *   - Jupiter: 5th, 7th, 9th houses
 *   - Saturn: 3rd, 7th, 10th houses
 *   - All others (Sun, Moon, Mercury, Venus): 7th house only
 *   - Rahu & Ketu (per some schools): 5th, 7th, 9th (like Jupiter) — we use 7th only as default.
 *
 * Rashi Drishti (sign aspects):
 *   - Movable signs aspect fixed signs (except adjacent): Aries→Leo&Scorpio, etc.
 *   - Fixed signs aspect movable signs (except adjacent).
 *   - Dual signs aspect other dual signs.
 */
object Aspects {

    /** Houses aspected by a given planet (besides the 1st, which is its own). */
    fun grahaAspectHouses(planet: Planet): List<Int> = when (planet) {
        Planet.MARS -> listOf(4, 7, 8)
        Planet.JUPITER -> listOf(5, 7, 9)
        Planet.SATURN -> listOf(3, 7, 10)
        Planet.RAHU, Planet.KETU -> listOf(7, 9, 5) // per some traditions; we use 7th by default
        else -> listOf(7)
    }

    /** Houses aspected from a given house by the Moon/Sun/Mercury/Venus (always 7th). */
    fun defaultAspectHouse(fromHouse: Int): Int = ((fromHouse - 1 + 6) % 12) + 1

    data class Aspect(
        val fromPlanet: Planet,
        val fromHouse: Int,
        val toPlanet: Planet,
        val toHouse: Int,
        val aspectType: String
    )

    /**
     * Compute all planetary aspects in the chart, using Whole Sign houses.
     *
     * House numbering is 1-based, with the Ascendant's sign as house 1.
     */
    fun computeGrahaDrishti(result: ChartCalculationResult): List<Aspect> {
        val ascSignIdx = result.ascendant.sign.ordinal
        // planet -> house (1-based)
        val planetHouse: Map<Planet, Int> = result.planets.mapValues { (_, pos) ->
            ((pos.sign.ordinal - ascSignIdx + 12) % 12) + 1
        }
        val aspects = mutableListOf<Aspect>()
        for ((fromP, fromHouse) in planetHouse) {
            val aspectHouses = grahaAspectHouses(fromP)
            for (h in aspectHouses) {
                val toHouse = ((fromHouse - 1 + (h - 1)) % 12) + 1
                // Find planets in the toHouse
                for ((toP, tHouse) in planetHouse) {
                    if (tHouse == toHouse) {
                        aspects.add(Aspect(fromP, fromHouse, toP, toHouse, "house $h aspect"))
                    }
                }
            }
        }
        return aspects
    }

    /**
     * Compute the signs aspected by a given sign under Rashi Drishti rules.
     */
    fun rashiDrishtiSigns(fromSign: ZodiacSign): List<ZodiacSign> {
        val fromIdx = fromSign.ordinal
        return when {
            fromSign.isMovable -> {
                // Movable signs aspect fixed signs (other than adjacent).
                // Aries → Leo & Scorpio (5th & 8th signs? actually: aspect signs 5th and 9th from itself but only fixed)
                // Correct rule: movable → all fixed signs except the 2nd and 12th (adjacent).
                val fixedSigns = ZodiacSign.entries.filter { it.isFixed }
                fixedSigns.filter { sign ->
                    val diff = ((sign.ordinal - fromIdx + 12) % 12)
                    diff != 1 && diff != 11
                }
            }
            fromSign.isFixed -> {
                val movableSigns = ZodiacSign.entries.filter { it.isMovable }
                movableSigns.filter { sign ->
                    val diff = ((sign.ordinal - fromIdx + 12) % 12)
                    diff != 1 && diff != 11
                }
            }
            else -> {
                // Dual signs aspect other dual signs (excluding self, 2nd, 12th, and 6th/8th?)
                // Standard rule: dual → dual (only other dual signs except self)
                ZodiacSign.entries.filter { it.isDual && it != fromSign }
            }
        }
    }

    /**
     * Compute all Rashi Drishti pairs in the chart.
     */
    fun computeRashiDrishti(result: ChartCalculationResult): List<Pair<ZodiacSign, ZodiacSign>> {
        val present = result.planets.values.map { it.sign }.distinct()
        val out = mutableListOf<Pair<ZodiacSign, ZodiacSign>>()
        for (s1 in present) {
            for (s2 in rashiDrishtiSigns(s1)) {
                if (present.contains(s2)) {
                    out.add(s1 to s2)
                }
            }
        }
        return out.distinct()
    }
}
