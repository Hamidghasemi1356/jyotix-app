package com.jyotix.app.core.calc

import swisseph.SwissEph
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.absoluteValue

/**
 * Panchanga — the five limbs of the Vedic almanac.
 *
 * The five limbs are:
 *   1. Tithi — lunar day (30 per lunar month, 15 each of Shukla & Krishna)
 *   2. Vaara — weekday (Sunday..Saturday)
 *   3. Nakshatra — the Moon's Nakshatra (27 of them)
 *   4. Yoga — derived from the sum of Sun and Moon longitudes (27 yogas)
 *   5. Karana — half-tithi (11 distinct karanas)
 *
 * All calculations use the **sidereal** (Lahiri) frame and the mean-orbit-based
 * tithi definition, which is the standard Jyotish convention.
 */
object Panchanga {

    enum class Paksha(val persianName: String) {
        SHUKLA("شوکلا/رو ادامه"), KRISHNA("کریشنا/رو descending")
    }

    /** The 30 Tithis of a lunar month, 15 Shukla + 15 Krishna. */
    enum class Tithi(val index: Int, val persianName: String, val deity: String) {
        SHUKLA_PRATIPADA(0, "شوکلا پرتیپادا", "Agni"),
        SHUKLA_DWITIYA(1, "شوکلا دووتییا", "Brahma"),
        SHUKLA_TRITIYA(2, "شوکلا تریتییا", "Gauri"),
        SHUKLA_CHATURTHI(3, "شوکلا چاتورتی", "Ganesha"),
        SHUKLA_PANCHAMI(4, "شوکلا پنچامی", "Naga"),
        SHUKLA_SHASHTHI(5, "شوکلا شاشتی", "Kartikeya"),
        SHUKLA_SAPTAMI(6, "شوکلا ساپتامی", "Surya"),
        SHUKLA_ASHTAMI(7, "شوکلا اشتمی", "Shiva"),
        SHUKLA_NAVAMI(8, "شوکلا ناومی", "Durga"),
        SHUKLA_DASHAMI(9, "شوکلا داشمی", "Yama"),
        SHUKLA_EKADASHI(10, "شوکلا اکادشی", "Vishvedevas"),
        SHUKLA_DWADASHI(11, "شوکلا دوادشی", "Vishnu"),
        SHUKLA_TRAYODASHI(12, "شوکلا ترایداشی", "Kamadeva"),
        SHUKLA_CHATURDASHI(13, "شوکلا چاتورداشی", "Shiva"),
        PURNIMA(14, "پورنیما/تاریخ پورنیم", "Chandra"),
        KRISHNA_PRATIPADA(15, "کریشنا پرتیپادا", "Agni"),
        KRISHNA_DWITIYA(16, "کریشنا دووتییا", "Brahma"),
        KRISHNA_TRITIYA(17, "کریشنا تریتییا", "Gauri"),
        KRISHNA_CHATURTHI(18, "کریشنا چاتورتی", "Ganesha"),
        KRISHNA_PANCHAMI(19, "کریشنا پنچامی", "Naga"),
        KRISHNA_SHASHTHI(20, "کریشنا شاشتی", "Kartikeya"),
        KRISHNA_SAPTAMI(21, "کریشنا ساپتامی", "Surya"),
        KRISHNA_ASHTAMI(22, "کریشنا اشتمی", "Shiva"),
        KRISHNA_NAVAMI(23, "کریشنا ناومی", "Durga"),
        KRISHNA_DASHAMI(24, "کریشنا داشمی", "Yama"),
        KRISHNA_EKADASHI(25, "کریشنا اکادشی", "Vishvedevas"),
        KRISHNA_DWADASHI(26, "کریشنا دوادشی", "Vishnu"),
        KRISHNA_TRAYODASHI(27, "کریشنا ترایداشی", "Kamadeva"),
        KRISHNA_CHATURDASHI(28, "کریشنا چاتورداشی", "Kali"),
        AMAVASYA(29, "آماواسیا/ماه جدید", "Pitrs");

        companion object {
            fun fromIndex(i: Int): Tithi = entries[i.coerceIn(0, 29)]
            val isShukla: Boolean get() = index < 15
        }
        val paksha: Paksha get() = if (index < 15) Paksha.SHUKLA else Paksha.KRISHNA
    }

    /** The 27 Yogas, derived from (Sun + Moon) longitude / 13°20′. */
    enum class Yoga(val index: Int, val persianName: String, val meaning: String) {
        VISHKAMBHA(0, "ویشکامبا", "supporting"),
        PRITI(1, "پریتی", "love"),
        AYUSHMAN(2, "آیوشمن", "long-lived"),
        SAUBHAGYA(3, "سابهاگیا", "good fortune"),
        SHOBHANA(4, "شوبهانا", "splendid"),
        ATIGANDA(5, "اتیگندا", "great danger"),
        SUKARMA(6, "سوکارما", "good deeds"),
        DHRTI(7, "دهریتی", "steadfast"),
        SHULA(8, "شولا", "spear"),
        GANDA(9, "گندا", "danger"),
        VRIDDHI(10, "وریدتی", "growth"),
        DHRUVA(11, "دهرووا", "constant"),
        VYAGHATA(12, "ویاغاتا", "obstruction"),
        HARSHANA(13, "هارشانا", "joyful"),
        VAJRA(14, "واجرا", "diamond"),
        SIDDHI(15, "سیددی", "success"),
        VYATIPATA(16, "ویاتیپاتا", "calamity"),
        VARIYANA(17, "واریانا", "excellent"),
        PARIGHA(18, "پاریگها", "obstacle"),
        SHIVA(19, "شیوا", "auspicious"),
        SIDDHA(20, "سیده‌ها", "perfected"),
        SADHYA(21, "سادیا", "attainable"),
        SUBHA(22, "سوبه‌ها", "auspicious"),
        SHUKLA(23, "شوکلا", "bright"),
        BRAHMA(24, "برهما", "creator"),
        INDRA(25, "ایندرا", "king of gods"),
        VAIDHRITI(26, "وایدریتی", "inflexible");

        companion object {
            fun fromIndex(i: Int): Yoga = entries[i.coerceIn(0, 26)]
        }
    }

    /** The 11 distinct Karanas (7 movable + 4 fixed). */
    enum class Karana(val index: Int, val persianName: String) {
        BAVA(0, "باوا"),
        BALAVA(1, "بالوا"),
        KAULAVA(2, "کولاوا"),
        TAITILA(3, "تاتیلا"),
        GARIJA(4, "گاریجا"),
        VANIJA(5, "وانیجا"),
        VISHTI(6, "ویشتی"),
        SHAKUNI(7, "شاکونی"),
        CHATUSHPADA(8, "چاتوشپادا"),
        NAGAVA(9, "ناگاوا"),
        KIMSTUGHNA(10, "کیمستوغنا");

        companion object {
            fun fromIndex(i: Int): Karana = entries[i.coerceIn(0, 10)]
        }
    }

    enum class Vaara(val index: Int, val persianName: String, val lord: Planet) {
        RAVIVARA(0, "یکشنبه", Planet.SUN),
        SOMAVARA(1, "دوشنبه", Planet.MOON),
        MANGALAVARA(2, "سه‌شنبه", Planet.MARS),
        BUDHAVARA(3, "چهارشنبه", Planet.MERCURY),
        GURUVARA(4, "پنج‌شنبه", Planet.JUPITER),
        SHUKRAVARA(5, "جمعه", Planet.VENUS),
        SHANIVARA(6, "شنبه", Planet.SATURN);

        companion object {
            fun fromIndex(i: Int): Vaara = entries[i.coerceIn(0, 6)]
        }
    }

    /** Full Panchanga for a single Julian Day. */
    data class PanchangaResult(
        val tithi: Tithi,
        val tithiFraction: Double,
        val vaara: Vaara,
        val nakshatra: Nakshatra,
        val nakshatraFraction: Double,
        val yoga: Yoga,
        val yogaFraction: Double,
        val karana: Karana,
        val karanaFraction: Double,
        val moonLongitude: Double,
        val sunLongitude: Double
    )

    /**
     * Compute the Panchanga at the given Julian Day.
     *
     * Requires the Swiss Ephemeris to be initialized.
     */
    fun compute(jd: Double, engine: CalculationEngine): PanchangaResult {
        val sun = engine.computePlanet(Planet.SUN, jd)
        val moon = engine.computePlanet(Planet.MOON, jd)

        val moonMinusSun = (((moon.longitude - sun.longitude) % 360.0) + 360.0) % 360.0
        // Tithi = (Moon - Sun) / 12 degrees. 0..29.
        val tithiIndex = (moonMinusSun / 12.0).toInt().coerceIn(0, 29)
        val tithiFraction = (moonMinusSun - tithiIndex * 12.0) / 12.0

        // Nakshatra: based on Moon longitude
        val nak = Nakshatra.fromLongitude(moon.longitude)
        val nakFraction = (moon.longitude - nak.startLongitude) / AstroConstants.NAKSHATRA_WIDTH

        // Yoga: (Sun + Moon) / 13°20′
        val sumLong = (sun.longitude + moon.longitude) % 360.0
        val yogaIndex = (sumLong / AstroConstants.NAKSHATRA_WIDTH).toInt().coerceIn(0, 26)
        val yogaFraction = (sumLong - yogaIndex * AstroConstants.NAKSHATRA_WIDTH) / AstroConstants.NAKSHATRA_WIDTH

        // Karana: half-tithi. 60 karanas per lunar month (each tithi has 2 karanas).
        // Movable karanas (7) repeat 8 times except for the last tithi of Krishna (Amavasya).
        val karanaIndex = (moonMinusSun / 6.0).toInt().coerceIn(0, 59)
        // Map to the 11-name scheme
        val karanaNameIndex: Int = when (karanaIndex) {
            0 -> Karana.KIMSTUGHNA.index
            57 -> Karana.SHAKUNI.index
            58 -> Karana.CHATUSHPADA.index
            59 -> Karana.NAGAVA.index
            else -> ((karanaIndex - 1) % 7)  // Bava..Vishti cycle
        }

        // Vaara — weekday based on the local calendar date for this JD
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val ymd = IntArray(3)
        val hms = DoubleArray(3)
        SwissEph.swe_revjul(jd + 0.5, SwissEph.SE_GREG_CAL, ymd, hms) // +0.5 to get local midnight JD
        cal.clear()
        cal.set(ymd[0], ymd[1] - 1, ymd[2])
        val vaaraIdx = (cal.get(Calendar.DAY_OF_WEEK) - 1) % 7 // Calendar.SUNDAY = 1 → 0

        return PanchangaResult(
            tithi = Tithi.fromIndex(tithiIndex),
            tithiFraction = tithiFraction,
            vaara = Vaara.fromIndex(vaaraIdx),
            nakshatra = nak,
            nakshatraFraction = nakFraction,
            yoga = Yoga.fromIndex(yogaIndex),
            yogaFraction = yogaFraction,
            karana = Karana.fromIndex(karanaNameIndex),
            karanaFraction = ((moonMinusSun - karanaIndex * 6.0) / 6.0),
            moonLongitude = moon.longitude,
            sunLongitude = sun.longitude
        )
    }
}
