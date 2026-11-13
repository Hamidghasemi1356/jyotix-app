# JYOTIX — Android Jyotish Astrology Application

A complete, professional Android application for Vedic astrology (Jyotish)
with accurate astronomical calculations based on the Swiss Ephemeris.

## Build Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34 (compileSdk)
- Minimum Android 8.0 (API 26)

## Setup Before Building

### 1. Add the Swiss Ephemeris library

The `swisseph.jar` file (Astrodienst's pure-Java Swiss Ephemeris port) is
required for the calculation engine. Place it at:

```
app/libs/swisseph.jar
```

Download from https://www.astro.com/ftp/swisseph/src/ — look for the Java
distribution (typically `swisseph.jar` or built from the Java sources in
`swisseph.zip`). The official Java port is provided by Astrodienst and
includes the `swisseph.SwissEph` class used throughout this codebase.

If you prefer the JNI native binding (your selected option), instead place
`libsweph.so` files for each ABI in:

```
app/src/main/jniLibs/arm64-v8a/libsweph.so
app/src/main/jniLibs/armeabi-v7a/libsweph.so
app/src/main/jniLibs/x86_64/libsweph.so   (for emulator testing)
```

…and remove the `implementation(files("libs/swisseph.jar"))` line from
`app/build.gradle.kts`. The Kotlin wrapper in
`core/calc/SwissEphemeris.kt` works with either binding.

### 2. Add Swiss Ephemeris data files (.se1)

Place `.se1` files in:

```
app/src/main/assets/swisseph/
```

See `app/src/main/assets/swisseph/README.md` for the file list and download
URLs. At minimum for 1900–2100 coverage, copy:
- `sepl_19.se1`, `sepl_20.se1`, `sepl_21.se1` (planets)
- `semo_19.se1`, `semo_20.se1`, `semo_21.se1` (Moon)

If no `.se1` files are present, the engine falls back to the Moshier
ephemeris (≤1″ accuracy) and remains fully functional.

### 3. Add the Dubai font (optional)

To use Dubai Medium as the primary font, copy `dubai_medium.ttf` and
`dubai_bold.ttf` to `app/src/main/res/font/`. See
`app/src/main/res/font/README.md` for download instructions.

If absent, the app uses Android's system Persian-capable font.

### 4. Firebase Phone Authentication

1. Create a project at https://console.firebase.google.com/.
2. Add an Android app with package name `com.jyotix.app`.
3. Enable **Phone** as a sign-in provider under Authentication.
4. Add your debug-keystore SHA-1 fingerprint in Project Settings.
5. Download `google-services.json` and place it at `app/google-services.json`.

If you want to skip Phone Auth during development, set
`FIREBASE_PHONE_AUTH_ENABLED=false` in `secrets.properties` — the app will
accept any 6-digit code in offline mode and create a local user profile.

## Build & Run

Open the project in Android Studio, sync Gradle, then Run on an emulator
or device (API 26+).

## Architecture

The codebase is organized into modular components:

```
app/src/main/java/com/jyotix/app/
├── JyotixApp.kt              — Application, dependency container
├── MainActivity.kt           — Single-activity Compose host
├── core/
│   ├── calc/                 — Calculation engine (the heart)
│   │   ├── AstroConstants.kt
│   │   ├── Planets.kt        — Planet, Nakshatra, ZodiacSign enums
│   │   ├── CalculationConfig.kt — Ayanamsa, houses, BirthData
│   │   ├── SwissEphemeris.kt — Swiss Ephemeris wrapper
│   │   ├── CalculationEngine.kt — Main engine (planets, ascendant)
│   │   ├── Vargas.kt         — All 16 divisional charts (D1–D60)
│   │   ├── Dasha.kt          — Vimshottari Dasha (Maha/Antar/Pratyanta)
│   │   ├── Panchanga.kt      — Tithi, Vaara, Nakshatra, Yoga, Karana
│   │   ├── Karakas.kt        — Chara Karakas, Graha & Rashi Drishti
│   │   └── Transit.kt        — Transit engine + Moon-in-Scorpio detector
│   ├── chart/                — Chart rendering (North Indian diamond)
│   ├── db/                   — Room database, entities, DAOs
│   ├── auth/                 — Firebase Phone Auth wrapper
│   └── location/             — City database loader
├── ui/
│   ├── theme/                — JyotixMaterial3 theme (RTL, Dubai font)
│   ├── navigation/           — NavHost & routes
│   ├── dashboard/            — Main dashboard
│   ├── auth/                 — Login & Register (phone OTP)
│   ├── natal/                — Birth form + Natal chart viewer
│   ├── transit/              — Daily transit dashboard
│   ├── moonscorpio/          — Qamar Dar Aqrab dashboard
│   ├── skymap/               — Flat 2D sky map
│   ├── education/            — Education CMS browser
│   ├── alerts/               — Astronomical alerts + preferences
│   └── components/           — Shared Jyotix UI components
└── notifications/            — WorkManager-based alert scheduler
```

## Visual Identity

- Background: Deep navy blue `#0A1929`
- Primary text & planetary symbols: Golden yellow `#E5B14B`
- Chart borders & lines: White `#FFFFFF`
- Dashboard separators: Black `#000000`
- Buttons: Off-white / light gray
- Font: Dubai Medium (with system fallback)
- Chart: Traditional North Indian diamond layout
- Layout direction: RTL (forced for Persian UI)

## Calculation Accuracy

All planetary positions, Ascendant, house cusps, Nakshatras, Padas,
Ayanamsa, divisional charts, Dashas, Panchanga, and aspects are computed
from real astronomical data via the Swiss Ephemeris.

Default settings:
- Sidereal zodiac (Lahiri Ayanamsa)
- Mean lunar nodes (Rahu/Ketu)
- Whole Sign houses
- J2000.0 epoch reference

Ayanamsa can be configured to any of the 10 supported modes (Lahiri, Raman,
Krishnamurti, Yukteshwar, etc.).

## SMS Verification

Real SMS OTP via Firebase Phone Auth. For Iranian numbers, use
international format (+98…). The flow is:

1. User enters phone → `AuthManager.sendVerificationCode`.
2. Firebase sends an SMS with a 6-digit code.
3. User enters code → `AuthManager.verifyCode`.
4. On success, a local `UserEntity` is created in Room.

In some regions Firebase Phone Auth may require reCAPTCHA verification; the
SDK handles this automatically.

## License

The application source code is provided as-is. The Swiss Ephemeris is
licensed by Astrodienst under the Swiss Ephemeris Public License (SEPL) —
see https://www.astro.com/swisseph/. The Dubai font is owned by the
Government of Dubai (free for personal and commercial use).

## TODO / Known Limitations

- The bundled `.se1` files and `swisseph.jar` are not included in the
  source distribution for size and licensing reasons; see Setup above.
- Firebase Phone Auth requires a real `google-services.json` file.
- Eclipse calculations (Solar & Lunar) are scaffolded via the `scanEvents`
  routine but the precise Besselian element engine is not yet wired into
  the alerts UI; this is a planned enhancement.
