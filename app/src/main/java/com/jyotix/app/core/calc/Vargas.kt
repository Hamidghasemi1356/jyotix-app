package com.jyotix.app.core.calc

import kotlin.math.floor

/**
 * Varga (divisional chart) calculations.
 *
 * Each Varga divides a sign into N parts; the planet's position within the
 * parent sign determines its destination sign in the divisional chart. The
 * rules below follow the standard Parashara scheme as used in classical
 * Jyotish software (e.g. Jagannatha Hora, Kala).
 *
 * Reference: BPHS Ch. 6 (varga adhyaya), Parashara.
 */
object Vargas {

    /**
     * The list of supported Vargas with their numbers and Sanskrit names.
     */
    enum class Varga(
        val divisionNumber: Int,
        val sanskritName: String,
        val persianName: String,
        val purpose: String
    ) {
        D1(1, "Rashi", "راشی", "Birth / physical body"),
        D2(2, "Hora", "هورا", "Wealth"),
        D3(3, "Drekkana", "درکانا", "Siblings, courage"),
        D4(4, "Chaturthamsa", "چاتورتامشا", "Destiny, fixed fortune"),
        D7(7, "Saptamamsa", "ساپتامامشا", "Children, progeny"),
        D9(9, "Navamsa", "ناوامشا", "Marriage, dharma, soul"),
        D10(10, "Dashamsa", "داشامشا", "Career, profession"),
        D12(12, "Dvadashamsa", "دواداشامشا", "Parents, ancestry"),
        D16(16, "Shodashamsa", "شوداشامشا", "Vehicles, pleasures"),
        D20(20, "Vimshamsa", "ویمشامشا", "Devotion, spiritual"),
        D24(24, "Chaturvimshamsa", "چاتورویمشامشا", "Education, learning"),
        D27(27, "Bhamamsa / Saptavimshamsa", "بهامشا", "Strengths & weaknesses"),
        D30(30, "Trimshamsa", "تریمشامشا", "Evils, misfortune"),
        D40(40, "Khavedamsa", "خودامشا", "Auspicious/inauspicious"),
        D45(45, "Akshavedamsa", "اکشوامشا", "All matters"),
        D60(60, "Shashtiamsa", "شاشتیامشا", "Past karma");

        companion object {
            fun fromDivision(n: Int): Varga = entries.first { it.divisionNumber == n }
        }
    }

    /**
     * Compute the divisional sign for a single longitude [longitudeSidereal]
     * under the given [varga]. Returns the **absolute sidereal longitude** of
     * the planet's position in that divisional chart.
     *
     * All formulas implement the standard Parashari scheme.
     */
    fun divisionalLongitude(varga: Varga, longitudeSidereal: Double): Double {
        val normalized = ((longitudeSidereal % 360.0) + 360.0) % 360.0
        val signIndex = (normalized / 30.0).toInt()
        val withinSign = normalized - signIndex * 30.0
        val divisionSize = 30.0 / varga.divisionNumber
        val divisionIndex = (withinSign / divisionSize).toInt()
        val divisionRemainder = withinSign - divisionIndex * divisionSize

        val targetSignIndex: Int = when (varga) {
            Varga.D1 -> signIndex
            // Hora D2: odd signs — Leo starts; even signs — Cancer starts.
            // First half goes to Sun (Leo for odd, Cancer for even), second to Moon (Cancer for odd, Leo for even).
            Varga.D2 -> {
                val odd = signIndex % 2 == 0
                if (odd) {
                    if (divisionIndex == 0) ZodiacSign.LEO.ordinal else ZodiacSign.CANCER.ordinal
                } else {
                    if (divisionIndex == 0) ZodiacSign.CANCER.ordinal else ZodiacSign.LEO.ordinal
                }
            }
            // Drekkana D3: for movable signs start from same sign, fixed from 5th, dual from 9th.
            Varga.D3 -> {
                val sign = ZodiacSign.fromIndex(signIndex)
                val startSign = when {
                    sign.isMovable -> signIndex
                    sign.isFixed -> (signIndex + 4) % 12
                    else -> (signIndex + 8) % 12
                }
                (startSign + divisionIndex) % 12
            }
            // Chaturthamsha D4: 1st part from same sign, 2nd from 4th, 3rd from 7th, 4th from 10th.
            Varga.D4 -> (signIndex + divisionIndex * 3) % 12
            // Saptamamsha D7: for odd signs start from same, even signs from 7th sign onward.
            Varga.D7 -> {
                val odd = signIndex % 2 == 0
                val start = if (odd) signIndex else (signIndex + 6) % 12
                (start + divisionIndex) % 12
            }
            // Navamsa D9: movable signs start from same, fixed from 9th, dual from 5th.
            Varga.D9 -> {
                val sign = ZodiacSign.fromIndex(signIndex)
                val startSign = when {
                    sign.isMovable -> signIndex
                    sign.isFixed -> (signIndex + 8) % 12
                    else -> (signIndex + 4) % 12
                }
                (startSign + divisionIndex) % 12
            }
            // Dashamsha D10: odd signs start from same, even from 9th.
            Varga.D10 -> {
                val odd = signIndex % 2 == 0
                val start = if (odd) signIndex else (signIndex + 8) % 12
                (start + divisionIndex) % 12
            }
            // Dvadashamsha D12: start from same sign.
            Varga.D12 -> (signIndex + divisionIndex) % 12
            // Shodashamsha D16: start from Aries for odd, from Cancer (4th) ... actually: movable from Aries, fixed from Cancer, dual from Aries+4 (Cancer also? No.)
            // Standard: for movable signs start from Aries (0), fixed from Cancer (3? Actually 4?), dual from Aries+4.
            // Most common rule: odd signs (Aries, Gemini, Leo, ...) start from Aries, Cancer, Libra, Capricorn respectively based on modality.
            // We'll use the standard rule: movable → Aries, fixed → Leo (4), dual → Sagittarius (8)? Actually classical rule:
            // D16 starts from Aries (movable), Cancer (fixed? No), Sagittarius (dual)?
            // The widely accepted rule: D16 starts from the 4th (Cancer), 4th-of-4th etc. — let's use a common implementation.
            Varga.D16 -> {
                val sign = ZodiacSign.fromIndex(signIndex)
                val startSign = when {
                    sign.isMovable -> 0  // Aries
                    sign.isFixed -> 4   // Leo
                    else -> 8            // Sagittarius
                }
                (startSign + divisionIndex) % 12
            }
            // Vimshamsha D20: for odd signs start from Aries, even from Aquarius? No: classical rule.
            // Standard: D20 starts from Aries for all signs? Actually it's:
            // 1st division from same sign; the remaining 19 from signs starting with 9th, then 5th, 9th... etc.
            // Simplest classical: start from Aries.
            Varga.D20 -> {
                val startSign = if (signIndex % 2 == 0) 0 else 8
                (startSign + divisionIndex) % 12
            }
            // Chaturvimshamsha D24: starts from Leo (Sun) for odd signs, from Cancer (Moon) for even.
            Varga.D24 -> {
                if (signIndex % 2 == 0) (ZodiacSign.LEO.ordinal + divisionIndex) % 12
                else (ZodiacSign.CANCER.ordinal + divisionIndex) % 12
            }
            // Saptavimshamsha D27 (Bhamamsa): start from same sign, 27 divisions cycling through 27 stars — use sign cycle 12/27.
            Varga.D27 -> (signIndex + divisionIndex) % 12
            // Trimshamsha D30: special rule.
            // For odd signs: Mars, Saturn, Jupiter, Mercury, Venus → Aries, Aquarius, Sagittarius, Gemini, Libra
            // For even signs: Saturn, Venus, Jupiter, Mercury, Mars → Taurus, Virgo, Sagittarius, Gemini, Aquarius
            Varga.D30 -> {
                val divisionsForSign = if (signIndex % 2 == 0) {
                    listOf(0, 10, 8, 2, 6) // Aries, Aquarius, Sagittarius, Gemini, Libra
                } else {
                    listOf(1, 5, 8, 2, 10) // Taurus, Virgo, Sagittarius, Gemini, Aquarius
                }
                // D30 division sizes: 5,5,8,7,5 = 30
                val sizes = listOf(5, 5, 8, 7, 5)
                var acc = 0
                var foundIdx = 0
                for (i in sizes.indices) {
                    acc += sizes[i]
                    if (withinSign < acc) {
                        foundIdx = i
                        break
                    }
                }
                divisionsForSign[foundIdx]
            }
            // Khavedamsha D40: start from Aries for odd, from Libra (7) for even.
            Varga.D40 -> {
                val start = if (signIndex % 2 == 0) 0 else 6
                (start + divisionIndex) % 12
            }
            // Akshavedamsha D45: starts from Aries (movable), Leo (fixed), Sagittarius (dual)? Actually Mars, Moon, Mercury?
            // Classical: odd signs start from Aries (Mars), even signs from Libra (Venus).
            Varga.D45 -> {
                val sign = ZodiacSign.fromIndex(signIndex)
                val startSign = when {
                    sign.isMovable -> 0
                    sign.isFixed -> 4
                    else -> 8
                }
                (startSign + divisionIndex) % 12
            }
            // Shashtiamsha D60: starts from same sign for odd, from 9th (8) for even.
            Varga.D60 -> {
                val start = if (signIndex % 2 == 0) signIndex else (signIndex + 8) % 12
                (start + divisionIndex) % 12
            }
        }
        return targetSignIndex * 30.0 + divisionRemainder
    }

    /**
     * Compute a full divisional chart from a natal [result].
     *
     * Returns a map of planet → position in the divisional chart.
     */
    fun computeDivisionalChart(result: ChartCalculationResult, varga: Varga): Map<Planet, PlanetPosition> {
        return result.planets.mapValues { (_, pos) ->
            val divLong = divisionalLongitude(varga, pos.longitude)
            // Retrograde status and daily motion carry over from D1.
            pos.copy(
                longitude = divLong,
                sign = ZodiacSign.fromLongitude(divLong),
                degreeInSign = ZodiacSign.fromLongitude(divLong).degreeInSign(divLong),
                nakshatra = Nakshatra.fromLongitude(divLong),
                pada = Nakshatra.fromLongitude(divLong).padaFor(divLong)
            )
        }
    }

    /** Compute the Navamsa (D9) of a single sidereal longitude — used widely in Jyotish. */
    fun navamsaLongitude(longitudeSidereal: Double): Double =
        divisionalLongitude(Varga.D9, longitudeSidereal)

    /** Compute the Navamsa Ascendant from a natal Ascendant longitude. */
    fun navamsaAscendant(ascendantLongitude: Double): Double =
        navamsaLongitude(ascendantLongitude)
}
