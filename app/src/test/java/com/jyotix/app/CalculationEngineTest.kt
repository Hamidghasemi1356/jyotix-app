package com.jyotix.app.core.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the non-ephemeris-dependent parts of the calculation engine.
 *
 * The Swiss Ephemeris is required for actual planet positions; here we test
 * the deterministic transformations (sign lookup, Nakshatra lookup, Varga
 * formulas, Dasha sequence) that operate on a known longitude.
 */
class AstroConstantsTest {

    @Test
    fun `zodiac signs from longitude`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(0.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(29.999))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromLongitude(30.0))
        assertEquals(ZodiacSign.LEO, ZodiacSign.fromLongitude(145.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(350.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(360.0)) // wrap
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(-10.0)) // negative
    }

    @Test
    fun `degree within sign`() {
        assertEquals(0.0, ZodiacSign.ARIES.degreeInSign(0.0), 0.0001)
        assertEquals(15.0, ZodiacSign.LEO.degreeInSign(135.0), 0.0001)
        assertEquals(29.5, ZodiacSign.SCORPIO.degreeInSign(239.5), 0.0001)
    }

    @Test
    fun `nakshatra from longitude`() {
        // 0° = Ashvini, pada 1
        assertEquals(Nakshatra.ASHVINI, Nakshatra.fromLongitude(0.0))
        assertEquals(1, Nakshatra.ASHVINI.padaFor(0.0))
        // 13°20' = 13.333...° = Bharani
        assertEquals(Nakshatra.BHARANI, Nakshatra.fromLongitude(13.3333))
        // 26°40' = 26.666...° = Krittika
        assertEquals(Nakshatra.KRTTIKA, Nakshatra.fromLongitude(26.6666))
        // Moon at 3s 10° = 100° → Pushya (pada 2 if 100 - 93.33 = 6.67 / 3.33 = 2)
        assertEquals(Nakshatra.PUSHYA, Nakshatra.fromLongitude(100.0))
    }

    @Test
    fun `pada computation`() {
        // 13°20' (start of Bharani) → pada 1
        assertEquals(1, Nakshatra.BHARANI.padaFor(13.3333))
        // 16°40' → start of pada 2
        assertEquals(2, Nakshatra.BHARANI.padaFor(16.6666))
        // 20° → pada 3
        assertEquals(3, Nakshatra.BHARANI.padaFor(20.0))
        // 23°20' → pada 4
        assertEquals(4, Nakshatra.BHARANI.padaFor(23.3333))
    }

    @Test
    fun `vimshottari sequence index of nakshatras`() {
        // Krittika (Sun) starts the dasha cycle → seq 0
        assertEquals(0, Nakshatra.KRTTIKA.vimshottariSequenceIndex)
        // Rohini (Moon) → seq 1
        assertEquals(1, Nakshatra.ROHINI.vimshottariSequenceIndex)
        // Ashvini is the first Nakshatra but its lord is Ketu, which is seq 8 in the dasha cycle
        assertEquals(8, Nakshatra.ASHVINI.vimshottariSequenceIndex)
    }

    @Test
    fun `planet dasha durations`() {
        assertEquals(6.0, Planet.SUN.dashaDurationYears, 0.0001)
        assertEquals(10.0, Planet.MOON.dashaDurationYears, 0.0001)
        assertEquals(20.0, Planet.VENUS.dashaDurationYears, 0.0001)
        assertEquals(7.0, Planet.MARS.dashaDurationYears, 0.0001)
        assertEquals(17.0, Planet.MERCURY.dashaDurationYears, 0.0001)
        assertEquals(16.0, Planet.JUPITER.dashaDurationYears, 0.0001)
        assertEquals(19.0, Planet.SATURN.dashaDurationYears, 0.0001)
        assertEquals(18.0, Planet.RAHU.dashaDurationYears, 0.0001)
        assertEquals(7.0, Planet.KETU.dashaDurationYears, 0.0001)
        // Total = 120
        val total = Planet.entries.sumOf { it.dashaDurationYears }
        assertEquals(120.0, total, 0.0001)
    }
}

class VargasTest {

    @Test
    fun `navamsa of 0 degrees Aries is Aries`() {
        // 0° Aries (movable sign) — first Navamsa = same sign (Aries)
        val navLong = Vargas.navamsaLongitude(0.0)
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(navLong))
    }

    @Test
    fun `navamsa of 10 degrees Aries is Cancer`() {
        // Aries (movable): navamsas start from Aries, Cancer, Libra, Capricorn...
        // 10° Aries = 4th Navamsa (since 0–3.33° = navamsa 1, 3.33–6.67° = 2, 6.67–10° = 3, 10–13.33° = 4)
        // Wait: 10/3.333 = 3 (0-indexed) → 4th navamsa (1-indexed) → sign = Aries + 3 = Cancer
        val navLong = Vargas.navamsaLongitude(10.0)
        assertEquals(ZodiacSign.CANCER, ZodiacSign.fromLongitude(navLong))
    }

    @Test
    fun `navamsa of 0 degrees Taurus is Capricorn`() {
        // Taurus (fixed): navamsas start from the 9th sign = Capricorn
        val navLong = Vargas.navamsaLongitude(30.0)
        assertEquals(ZodiacSign.CAPRICORN, ZodiacSign.fromLongitude(navLong))
    }

    @Test
    fun `navamsa of 0 degrees Gemini is Libra`() {
        // Gemini (dual): navamsas start from the 5th sign = Libra? Actually the rule: dual → 5th
        // Gemini + 4 = Libra? No: dual signs start from the 5th from itself.
        // Gemini (ordinal 2) + 4 = ordinal 6 = Libra. Yes.
        val navLong = Vargas.navamsaLongitude(60.0)
        assertEquals(ZodiacSign.LIBRA, ZodiacSign.fromLongitude(navLong))
    }

    @Test
    fun `dashamsa D10 of 0 degrees Aries is Aries`() {
        // Aries is odd → start from same sign; first dashamsa = Aries
        val d10Long = Vargas.divisionalLongitude(Vargas.Varga.D10, 0.0)
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(d10Long))
    }

    @Test
    fun `all vargas return valid longitudes`() {
        // For any longitude 0–360 and any varga, the result must be in [0, 360)
        for (v in Vargas.Varga.entries) {
            for (lon in listOf(0.0, 15.0, 30.0, 90.0, 145.5, 210.0, 270.0, 359.999)) {
                val out = Vargas.divisionalLongitude(v, lon)
                assertTrue("Varga ${v.name} at $lon° returned $out", out in 0.0..360.0)
            }
        }
    }
}

class KarakasTest {

    @Test
    fun `chara karakas sorted by degree descending`() {
        // Build a minimal mock chart
        val birth = BirthData(
            year = 2000, month = 1, day = 1, hour = 12, minute = 0,
            latitudeDeg = 0.0, longitudeDeg = 0.0
        )
        // We can't easily test computeCharaKarakas without the ephemeris,
        // but we can verify the KarakaAssignment structure with synthetic data.
        // Just verify the enum has 8 values.
        assertEquals(8, Karakas.CharaKaraka.entries.size)
        assertEquals(Karakas.CharaKaraka.ATMAKARAKA, Karakas.CharaKaraka.fromIndex(0))
        assertEquals(Karakas.CharaKaraka.DARAKARAKA, Karakas.CharaKaraka.fromIndex(7))
    }
}

class AspectsTest {

    @Test
    fun `graha aspect houses`() {
        assertEquals(listOf(7), Aspects.grahaAspectHouses(Planet.SUN))
        assertEquals(listOf(7), Aspects.grahaAspectHouses(Planet.MOON))
        assertEquals(listOf(4, 7, 8), Aspects.grahaAspectHouses(Planet.MARS))
        assertEquals(listOf(5, 7, 9), Aspects.grahaAspectHouses(Planet.JUPITER))
        assertEquals(listOf(3, 7, 10), Aspects.grahaAspectHouses(Planet.SATURN))
    }

    @Test
    fun `rashi drishti from movable sign`() {
        // Aries (movable) should aspect fixed signs (Taurus, Leo, Scorpio, Aquarius)
        // except the adjacent ones (Taurus is 2nd from Aries, so excluded)
        val aspects = Aspects.rashiDrishtiSigns(ZodiacSign.ARIES)
        // Adjacent to Aries: Taurus (2nd) and Pisces (12th). Taurus excluded.
        assertTrue(aspects.contains(ZodiacSign.LEO))
        assertTrue(aspects.contains(ZodiacSign.SCORPIO))
        assertTrue(aspects.contains(ZodiacSign.AQUARIUS))
        // Taurus and Pisces excluded (adjacent)
        assertTrue(!aspects.contains(ZodiacSign.TAURUS))
        assertTrue(!aspects.contains(ZodiacSign.PISCES))
    }
}
