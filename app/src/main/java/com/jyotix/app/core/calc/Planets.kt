package com.jyotix.app.core.calc

/**
 * The nine classical Grahas used in Jyotish, plus the lunar nodes (Rahu & Ketu).
 *
 * The [swissEphId] is the planet code used by the Swiss Ephemeris library
 * (`swe_calc_ut` etc.). The corresponding codes for the mean lunar nodes
 * are used by default; true nodes can be configured in [CalculationConfig].
 */
enum class Planet(
    val swissEphId: Int,
    val sanskritName: String,
    val persianName: String,
    val symbol: String,
    val sanskritAbbrev: String,
    val defaultExaltationSign: ZodiacSign,
    val defaultDebilitationSign: ZodiacSign,
    val ownSigns: List<ZodiacSign>,
    /** Friends, neutral, enemies — used in dignity calculations. */
    val friends: List<Planet>,
    val neutral: List<Planet>,
    val enemies: List<Planet>,
    /** Vimshottari Dasha duration in years. */
    val dashaYears: Double,
    /** Whether the body is ever retrograde. Sun & Moon never are. */
    val canRetrograde: Boolean
) {
    SUN(
        swissEphId = 0, sanskritName = "Surya", persianName = "خورشید", symbol = "Su",
        sanskritAbbrev = "Su",
        defaultExaltationSign = ZodiacSign.ARIES, defaultDebilitationSign = ZodiacSign.LIBRA,
        ownSigns = listOf(ZodiacSign.LEO),
        friends = listOf(Planet.MOON, Planet.MARS, Planet.JUPITER),
        neutral = listOf(Planet.MERCURY),
        enemies = listOf(Planet.VENUS, Planet.SATURN),
        dashaYears = 6.0, canRetrograde = false
    ),
    MOON(
        swissEphId = 1, sanskritName = "Chandra", persianName = "ماه", symbol = "Mo",
        sanskritAbbrev = "Mo",
        defaultExaltationSign = ZodiacSign.TAURUS, defaultDebilitationSign = ZodiacSign.SCORPIO,
        ownSigns = listOf(ZodiacSign.CANCER),
        friends = listOf(Planet.SUN, Planet.MERCURY),
        neutral = listOf(Planet.MARS, Planet.JUPITER, Planet.VENUS, Planet.SATURN),
        enemies = listOf(),
        dashaYears = 10.0, canRetrograde = false
    ),
    MARS(
        swissEphId = 4, sanskritName = "Mangala", persianName = "مریخ", symbol = "Ma",
        sanskritAbbrev = "Ma",
        defaultExaltationSign = ZodiacSign.CAPRICORN, defaultDebilitationSign = ZodiacSign.CANCER,
        ownSigns = listOf(ZodiacSign.ARIES, ZodiacSign.SCORPIO),
        friends = listOf(Planet.SUN, Planet.MOON, Planet.JUPITER),
        neutral = listOf(Planet.VENUS, Planet.SATURN),
        enemies = listOf(Planet.MERCURY),
        dashaYears = 7.0, canRetrograde = true
    ),
    MERCURY(
        swissEphId = 2, sanskritName = "Budha", persianName = "عطارد", symbol = "Me",
        sanskritAbbrev = "Me",
        defaultExaltationSign = ZodiacSign.VIRGO, defaultDebilitationSign = ZodiacSign.PISCES,
        ownSigns = listOf(ZodiacSign.GEMINI, ZodiacSign.VIRGO),
        friends = listOf(Planet.SUN, Planet.VENUS),
        neutral = listOf(Planet.MARS, Planet.JUPITER, Planet.SATURN),
        enemies = listOf(Planet.MOON),
        dashaYears = 17.0, canRetrograde = true
    ),
    JUPITER(
        swissEphId = 5, sanskritName = "Guru", persianName = "مشتری", symbol = "Ju",
        sanskritAbbrev = "Gu",
        defaultExaltationSign = ZodiacSign.CANCER, defaultDebilitationSign = ZodiacSign.CAPRICORN,
        ownSigns = listOf(ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES),
        friends = listOf(Planet.SUN, Planet.MOON, Planet.MARS),
        neutral = listOf(Planet.SATURN),
        enemies = listOf(Planet.MERCURY, Planet.VENUS),
        dashaYears = 16.0, canRetrograde = true
    ),
    VENUS(
        swissEphId = 3, sanskritName = "Shukra", persianName = "زهره", symbol = "Ve",
        sanskritAbbrev = "Ve",
        defaultExaltationSign = ZodiacSign.PISCES, defaultDebilitationSign = ZodiacSign.VIRGO,
        ownSigns = listOf(ZodiacSign.TAURUS, ZodiacSign.LIBRA),
        friends = listOf(Planet.SATURN, Planet.MERCURY),
        neutral = listOf(Planet.JUPITER),
        enemies = listOf(Planet.SUN, Planet.MOON, Planet.MARS),
        dashaYears = 20.0, canRetrograde = true
    ),
    SATURN(
        swissEphId = 6, sanskritName = "Shani", persianName = "کیوان", symbol = "Sa",
        sanskritAbbrev = "Sa",
        defaultExaltationSign = ZodiacSign.LIBRA, defaultDebilitationSign = ZodiacSign.ARIES,
        ownSigns = listOf(ZodiacSign.CAPRICORN, ZodiacSign.AQUARIUS),
        friends = listOf(Planet.MERCURY, Planet.VENUS),
        neutral = listOf(Planet.JUPITER),
        enemies = listOf(Planet.SUN, Planet.MOON, Planet.MARS),
        dashaYears = 19.0, canRetrograde = true
    ),
    RAHU(
        swissEphId = 10, sanskritName = "Rahu", persianName = "رآهو", symbol = "Ra",
        sanskritAbbrev = "Ra",
        defaultExaltationSign = ZodiacSign.TAURUS, defaultDebilitationSign = ZodiacSign.SCORPIO,
        ownSigns = listOf(),
        friends = listOf(Planet.MERCURY, Planet.VENUS, Planet.SATURN),
        neutral = listOf(Planet.MARS, Planet.JUPITER),
        enemies = listOf(Planet.SUN, Planet.MOON),
        dashaYears = 18.0, canRetrograde = true
    ),
    KETU(
        swissEphId = 11, sanskritName = "Ketu", persianName = "کیتو", symbol = "Ke",
        sanskritAbbrev = "Ke",
        defaultExaltationSign = ZodiacSign.SCORPIO, defaultDebilitationSign = ZodiacSign.TAURUS,
        ownSigns = listOf(),
        friends = listOf(Planet.MARS, Planet.VENUS, Planet.SATURN),
        neutral = listOf(Planet.MERCURY, Planet.JUPITER),
        enemies = listOf(Planet.SUN, Planet.MOON),
        dashaYears = 7.0, canRetrograde = true
    );

    /** Vimshottari Dasha duration in years for this planet. */
    val dashaDurationYears: Double get() = dashaYears
}

/**
 * All 27 Nakshatras in the Vimshottari order, starting from Krittika.
 *
 * Note: in the Vimshottari Dasha sequence the order starts with Krittika (ruled
 * by Sun) and proceeds through the Nakshatras in the natural order along the
 * zodiac (Krittika, Rohini, Mrigashira, Ardra, ...). The astronomical 0° Aries
 * reference falls in Ashwini, so [Nakshatra.fromLongitude] handles the offset.
 */
enum class Nakshatra(
    val index: Int,            // 0-based, in zodiacal order (0 = Ashwini)
    val startLongitude: Double,
    val sanskritName: String,
    val persianName: String,
    val deity: String,
    val vimshottariLord: Planet,
    val yogaTarara: String,
    val symbol: String
) {
    ASHVINI(0, 0.0, "Ashvini", "اشوینی", "Ashvini Kumaras", Planet.KETU, "Ashvini", "horse head"),
    BHARANI(1, AstroConstants.NAKSHATRA_WIDTH, "Bharani", "بهرانی", "Yama", Planet.VENUS, "Bharani", "yoni"),
    KRTTIKA(2, 2 * AstroConstants.NAKSHATRA_WIDTH, "Krittika", "کریتیکا", "Agni", Planet.SUN, "Krittika", "razor"),
    ROHINI(3, 3 * AstroConstants.NAKSHATRA_WIDTH, "Rohini", "روهینی", "Brahma", Planet.MOON, "Rohini", "chariot"),
    MRIGASHIRA(4, 4 * AstroConstants.NAKSHATRA_WIDTH, "Mrigashira", "مریگاشیرا", "Soma", Planet.MARS, "Mrigashira", "deer head"),
    ARDRA(5, 5 * AstroConstants.NAKSHATRA_WIDTH, "Ardra", "آردرا", "Rudra", Planet.RAHU, "Ardra", "teardrop"),
    PUNARVASU(6, 6 * AstroConstants.NAKSHATRA_WIDTH, "Punarvasu", "پونارواسو", "Aditi", Planet.JUPITER, "Punarvasu", "quiver"),
    PUSHYA(7, 7 * AstroConstants.NAKSHATRA_WIDTH, "Pushya", "پوشیا", "Brihaspati", Planet.SATURN, "Pushya", "cow udder"),
    ASLESHA(8, 8 * AstroConstants.NAKSHATRA_WIDTH, "Ashlesha", "آشلیشا", "Nagas", Planet.MERCURY, "Ashlesha", "coiled serpent"),
    MAGHA(9, 9 * AstroConstants.NAKSHATRA_WIDTH, "Magha", "ماگها", "Pitrs", Planet.KETU, "Magha", "royal throne"),
    PURVA_PHALGUNI(10, 10 * AstroConstants.NAKSHATRA_WIDTH, "Purva Phalguni", "پوروا پالگونی", "Bhaga", Planet.VENUS, "Purva Phalguni", "hammock"),
    UTTARA_PHALGUNI(11, 11 * AstroConstants.NAKSHATRA_WIDTH, "Uttara Phalguni", "اوتارا پالگونی", "Aryaman", Planet.SUN, "Uttara Phalguni", "bed"),
    HASTA(12, 12 * AstroConstants.NAKSHATRA_WIDTH, "Hasta", "هستا", "Savitar", Planet.MOON, "Hasta", "hand"),
    CHITRA(13, 13 * AstroConstants.NAKSHATRA_WIDTH, "Chitra", "چیترا", "Tvashtar", Planet.MARS, "Chitra", "jewel"),
    SVATI(14, 14 * AstroConstants.NAKSHATRA_WIDTH, "Svati", "سواتی", "Vayu", Planet.RAHU, "Svati", "young sprout"),
    VISHAKHA(15, 15 * AstroConstants.NAKSHATRA_WIDTH, "Vishakha", "ویشاخا", "Indragni", Planet.JUPITER, "Vishakha", "triumphal arch"),
    ANURADHA(16, 16 * AstroConstants.NAKSHATRA_WIDTH, "Anuradha", "آنورادها", "Mitra", Planet.SATURN, "Anuradha", "lotus"),
    JYESHITHA(17, 17 * AstroConstants.NAKSHATRA_WIDTH, "Jyeshtha", "جیشتا", "Indra", Planet.MERCURY, "Jyeshtha", "earring"),
    MULA(18, 18 * AstroConstants.NAKSHATRA_WIDTH, "Mula", "مولا", "Nirriti", Planet.KETU, "Mula", "bunched roots"),
    PURVA_ASHADHA(19, 19 * AstroConstants.NAKSHATRA_WIDTH, "Purva Ashadha", "پوروا آشادها", "Apah", Planet.VENUS, "Purva Ashadha", "fan"),
    UTTARA_ASHADHA(20, 20 * AstroConstants.NAKSHATRA_WIDTH, "Uttara Ashadha", "اوتارا آشادها", "Vishvadevas", Planet.SUN, "Uttara Ashadha", "elephant tusk"),
    SHRAVANA(21, 21 * AstroConstants.NAKSHATRA_WIDTH, "Shravana", "شروونا", "Vishnu", Planet.MOON, "Shravana", "ear"),
    DHANISHTHA(22, 22 * AstroConstants.NAKSHATRA_WIDTH, "Dhanishtha", "دهانیشتا", "Vasus", Planet.MARS, "Dhanishtha", "drum"),
    SHATABHISHAJ(23, 23 * AstroConstants.NAKSHATRA_WIDTH, "Shatabhishaj", "شاتاویشا", "Varuna", Planet.RAHU, "Shatabhishaj", "empty circle"),
    PURVA_BHADRAPADA(24, 24 * AstroConstants.NAKSHATRA_WIDTH, "Purva Bhadrapada", "پوروا بهادرا", "Aja Ekapada", Planet.JUPITER, "Purva Bhadrapada", "sword"),
    UTTARA_BHADRAPADA(25, 25 * AstroConstants.NAKSHATRA_WIDTH, "Uttara Bhadrapada", "اوتارا بهادرا", "Ahir Budhnya", Planet.SATURN, "Uttara Bhadrapada", "twins"),
    REVATI(26, 26 * AstroConstants.NAKSHATRA_WIDTH, "Revati", "رواتی", "Pushan", Planet.MERCURY, "Revati", "fish");

    companion object {
        /** Return the Nakshatra containing the given absolute sidereal longitude. */
        fun fromLongitude(longitude: Double): Nakshatra {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            val idx = (normalized / AstroConstants.NAKSHATRA_WIDTH).toInt().coerceIn(0, 26)
            return entries.first { it.index == idx }
        }
    }

    /** Compute the Pada (1-4) for a longitude inside this Nakshatra. */
    fun padaFor(longitude: Double): Int {
        val normalized = ((longitude % 360.0) + 360.0) % 360.0
        val within = normalized - startLongitude
        return (within / AstroConstants.PADA_WIDTH).toInt().coerceIn(0, 3) + 1
    }

    /** Vimshottari Dasha sequence number — Krittika (Sun) starts at 0. */
    val vimshottariSequenceIndex: Int get() = (index - 2 + 27) % 27
}
