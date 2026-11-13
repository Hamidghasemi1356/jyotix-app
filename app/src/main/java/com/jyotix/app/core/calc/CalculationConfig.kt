package com.jyotix.app.core.calc

import java.util.Calendar
import java.util.TimeZone

/**
 * Configuration that controls how astronomical calculations are performed.
 *
 * Immutable; pass a copy with modified values when settings change.
 */
data class CalculationConfig(
    /** Ayanamsa (precession correction) mode. Default Lahiri (Chitrapaksha). */
    val ayanamsa: AyanamsaMode = AyanamsaMode.LAHIRI,
    /** Whether to use true (oscillating) lunar nodes or mean nodes. Default mean. */
    val trueNodes: Boolean = false,
    /** House system. Default Whole Sign (common in Jyotish). */
    val houseSystem: HouseSystem = HouseSystem.WHOLE_SIGN,
    /** Whether to use Tropical (false) or Sidereal (true) zodiac. */
    val sidereal: Boolean = true,
    /** Planetary calculation flag: Swiss Ephemeris (true) or Moshier (false). */
    val useSwissEphemeris: Boolean = true,
    /** Topocentric (true) or geocentric (false) positions for Sun/Moon. */
    val topocentric: Boolean = false,
    /** Whether to include the four karakas (Upagrahas) like Gulika in calculations. */
    val includeUpagrahas: Boolean = true
)

/** Common Ayanamsa modes supported by Swiss Ephemeris. */
enum class AyanamsaMode(val swissEphConstant: Int, val persianName: String) {
    LAHIRI(0, "لاهیری (چیتراپاکشا)"),
    RAMAN(1, "رامان"),
    KRISHNAMURTI(2, "کریشنامورتی"),
    YUKTESHWAR(3, "یوکتشور"),
    JN_BHASIN(4, "جی.ان. بهاسین"),
    BABYLonian_KUGLER(5, "کوگلر بابل"),
    ALDEBARAN_15TAU(6, "الدبران ۱۵ ثور"),
    HIPPARCHUS(7, "هیپارخوس"),
    DJWHAL_KHUL(8, "جوال خول"),
    GALACTIC_CENTER_0SAG(19, "مرکز کهکشان ۰ قوس")
}

/** House systems supported. */
enum class HouseSystem(val swissEphConstant: Int, val persianName: String) {
    WHOLE_SIGN(0, "کل نشانه"),
    PLACIDUS(80, "پلاکیدوس"),
    KOCH(75, "کوخ"),
    REGIOMONTANUS(82, "رجیومونتانوس"),
    CAMPANUS(67, "کامپانوس"),
    EQUAL(69, "برابر"),
    PORPHYRIUS(79, "پورفیری"),
    Sripati(83, "شری‌پاتی")
}

/** Whether a body is moving forward or retrograde at the calculation time. */
enum class RetrogradeStatus { DIRECT, RETROGRADE, STATIONARY }

/**
 * Birth (or event) information used as input to all calculations.
 *
 * Time zone is stored as [timeZoneId] (e.g. "Asia/Tehran") and also as raw
 * [utcOffsetHours] for cases where the time zone database is unavailable.
 */
data class BirthData(
    val year: Int,
    val month: Int,        // 1-12
    val day: Int,          // 1-31
    val hour: Int,         // 0-23
    val minute: Int,       // 0-59
    val second: Int = 0,
    val timeZoneId: String = "Asia/Tehran",
    val utcOffsetHours: Double = 3.5,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val altitudeMeters: Double = 0.0,
    val cityName: String = "",
    val countryName: String = ""
) {
    /** Convert to a [Calendar] in the original local time zone. */
    fun toCalendar(): Calendar {
        val tz = try { TimeZone.getTimeZone(timeZoneId) } catch (_: Throwable) {
            TimeZone.getTimeZone("UTC")
        }
        return Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
        }
    }
}

/** Result of a single body's position calculation. */
data class PlanetPosition(
    val planet: Planet,
    /** Sidereal longitude in degrees [0, 360). */
    val longitude: Double,
    /** Daily motion in degrees (negative if retrograde). */
    val dailyMotion: Double,
    /** Retrograde status derived from [dailyMotion]. */
    val retrogradeStatus: RetrogradeStatus,
    /** Sign containing this longitude. */
    val sign: ZodiacSign = ZodiacSign.fromLongitude(longitude),
    /** Degrees within the sign [0, 30). */
    val degreeInSign: Double = sign.degreeInSign(longitude),
    /** Nakshatra containing this longitude. */
    val nakshatra: Nakshatra = Nakshatra.fromLongitude(longitude),
    /** Pada (1-4) within the Nakshatra. */
    val pada: Int = nakshatra.padaFor(longitude)
) {
    /** True when the body is retrograde. */
    val isRetrograde: Boolean get() = retrogradeStatus == RetrogradeStatus.RETROGRADE

    /** Formatted degrees: "12°34'56"". */
    val formattedDegree: String get() {
        val deg = degreeInSign.toInt()
        val minutesAll = (degreeInSign - deg) * 60.0
        val min = minutesAll.toInt()
        val sec = ((minutesAll - min) * 60.0).toInt()
        return "%02d°%02d'%02d\"".format(deg, min, sec)
    }
}

/** Result of the Ascendant (Lagna) calculation. */
data class AscendantInfo(
    /** Sidereal longitude of the Ascendant in degrees [0, 360). */
    val longitude: Double,
    val sign: ZodiacSign = ZodiacSign.fromLongitude(longitude),
    val degreeInSign: Double = sign.degreeInSign(longitude),
    /** Longitudes of all 12 house cusps (Whole Sign = sign starts). */
    val houseCusps: List<Double>,
    /** MC (Midheaven) longitude in degrees. */
    val mcLongitude: Double,
    val houseSystem: HouseSystem
)

/** Full result of a chart calculation. */
data class ChartCalculationResult(
    val birthData: BirthData,
    val config: CalculationConfig,
    val julianDay: Double,
    val ayanamsaValue: Double,
    val planets: Map<Planet, PlanetPosition>,
    val ascendant: AscendantInfo,
    val calculationTime: Long = System.currentTimeMillis()
) {
    /** Convenience accessor: returns the planet's position, throwing if missing. */
    fun positionOf(planet: Planet): PlanetPosition =
        planets[planet] ?: error("Planet $planet not in result")
}
