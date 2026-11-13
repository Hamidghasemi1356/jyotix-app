package com.jyotix.app.core.calc

/**
 * Core astronomical and astrological constants used throughout Jyotix.
 *
 * All longitudes are expressed in degrees [0, 360) unless otherwise noted.
 * The sidereal (Lahiri) frame is the default. Conversions to tropical are
 * done by adding the Ayanamsa.
 */
object AstroConstants {

    /** Number of seconds per day. */
    const val SECONDS_PER_DAY: Double = 86400.0

    /** Julian date of J2000.0 epoch (2000-01-01 12:00 TT). */
    const val J2000: Double = 2451545.0

    /** Sidereal year in Julian days. */
    const val SIDEREAL_YEAR_DAYS: Double = 365.256363004

    /** Lunar synodic month in days. */
    const val SYNODIC_MONTH_DAYS: Double = 29.530588853

    /** Number of Nakshatras. */
    const val NAKSHATRA_COUNT: Int = 27

    /** Width of a Nakshatra in degrees (360 / 27). */
    val NAKSHATRA_WIDTH: Double = 360.0 / 27.0

    /** Each Nakshatra has 4 Padas. */
    const val PADA_COUNT_PER_NAKSHATRA: Int = 4

    /** Width of a Pada in degrees (13°20′ / 4 = 3°20′). */
    val PADA_WIDTH: Double = NAKSHATRA_WIDTH / PADA_COUNT_PER_NAKSHATRA

    /** Number of zodiac signs. */
    const val SIGN_COUNT: Int = 12

    /** Width of a zodiac sign in degrees. */
    const val SIGN_WIDTH: Double = 30.0

    /** Lahiri (Chitrapaksha) Ayanamsa reference epoch. */
    const val LAHIRI_REFERENCE_YEAR: Double = 285.0
}

/**
 * The 12 zodiac signs of the sidereal Zodiac, in order starting from Aries (0°).
 *
 * Each sign is 30° wide. The enum's [ordinal]×30 gives the sign's starting longitude.
 */
enum class ZodiacSign(
    val degreeStart: Double,
    val sanskritName: String,
    val persianName: String,
    val symbol: String,
    val element: Element,
    val modality: Modality,
    val ruler: Planet
) {
    ARIES(0.0, "Mesha", "حمل", "♈", Element.FIRE, Modality.CARDINAL, Planet.MARS),
    TAURUS(30.0, "Vrishabha", "ثور", "♉", Element.EARTH, Modality.FIXED, Planet.VENUS),
    GEMINI(60.0, "Mithuna", "جوزا", "♊", Element.AIR, Modality.MUTABLE, Planet.MERCURY),
    CANCER(90.0, "Karka", "سرطان", "♋", Element.WATER, Modality.CARDINAL, Planet.MOON),
    LEO(120.0, "Simha", "اسد", "♌", Element.FIRE, Modality.FIXED, Planet.SUN),
    VIRGO(150.0, "Kanya", "سنبله", "♍", Element.EARTH, Modality.MUTABLE, Planet.MERCURY),
    LIBRA(180.0, "Tula", "میزان", "♎", Element.AIR, Modality.CARDINAL, Planet.VENUS),
    SCORPIO(210.0, "Vrishchika", "عقرب", "♏", Element.WATER, Modality.FIXED, Planet.MARS),
    SAGITTARIUS(240.0, "Dhanu", "قوس", "♐", Element.FIRE, Modality.MUTABLE, Planet.JUPITER),
    CAPRICORN(270.0, "Makara", "جدی", "♑", Element.EARTH, Modality.CARDINAL, Planet.SATURN),
    AQUARIUS(300.0, "Kumbha", "دلو", "♒", Element.AIR, Modality.FIXED, Planet.SATURN),
    PISCES(330.0, "Meena", "حوت", "♓", Element.WATER, Modality.MUTABLE, Planet.JUPITER);

    companion object {
        /** Return the sign that contains the given absolute longitude [0,360). */
        fun fromLongitude(longitude: Double): ZodiacSign {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            return entries[(normalized / AstroConstants.SIGN_WIDTH).toInt().coerceIn(0, 11)]
        }

        /** Zero-based index of the sign (Aries = 0). */
        fun fromIndex(index: Int): ZodiacSign = entries[index.coerceIn(0, 11)]
    }

    /** Degrees of the planet within the current sign (0–30). */
    fun degreeInSign(longitude: Double): Double {
        val normalized = ((longitude % 360.0) + 360.0) % 360.0
        return normalized - degreeStart
    }

    /** Whether this sign is odd (male) — Aries, Gemini, Leo, Libra, Sagittarius, Aquarius. */
    val isOdd: Boolean get() = ordinal % 2 == 0

    val isMovable: Boolean get() = modality == Modality.CARDINAL
    val isFixed: Boolean get() = modality == Modality.FIXED
    val isDual: Boolean get() = modality == Modality.MUTABLE
}

enum class Element { FIRE, EARTH, AIR, WATER }
enum class Modality { CARDINAL, FIXED, MUTABLE }
