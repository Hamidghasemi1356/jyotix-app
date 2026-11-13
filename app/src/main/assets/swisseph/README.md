# Swiss Ephemeris Data Files (.se1)

This folder holds the Swiss Ephemeris `.se1` data files used by the
calculation engine for high-precision planetary positions.

## Required Files

For full Jyotix functionality (1800 AD – 2400 AD), download these files
from https://www.astro.com/ftp/swisseph-ephe/:

- `sepl_18.se1` — planets 1800–1899 (≈ 3.1 MB)
- `sepl_19.se1` — planets 1900–1999 (≈ 3.1 MB)
- `sepl_20.se1` — planets 2000–2099 (≈ 3.1 MB)
- `sepl_21.se1` — planets 2100–2199 (≈ 3.1 MB)
- `sepl_22.se1` — planets 2200–2299 (≈ 3.1 MB)
- `sepl_23.se1` — planets 2300–2399 (≈ 3.1 MB)
- `semo_18.se1` — Moon 1800–1899 (≈ 8.5 MB)
- `semo_19.se1` — Moon 1900–1999 (≈ 8.5 MB)
- `semo_20.se1` — Moon 2000–2099 (≈ 8.5 MB)
- `semo_21.se1` — Moon 2100–2199 (≈ 8.5 MB)
- `semo_22.se1` — Moon 2200–2299 (≈ 8.5 MB)
- `semo_23.se1` — Moon 2300–2399 (≈ 8.5 MB)
- `seas_18.se1` — asteroids 1800–1899 (optional)
- ...

For a smaller bundle covering 1900–2100, copy only `sepl_19.se1`, `sepl_20.se1`,
`sepl_21.se1`, `semo_19.se1`, `semo_20.se1`, `semo_21.se1`. This is ~35 MB.

## Installation

Copy the `.se1` files into this folder before building the APK:

```
app/src/main/assets/swisseph/sepl_18.se1
app/src/main/assets/swisseph/sepl_19.se1
...
```

At first launch, `SwissEphemeris.ensureInitialized()` copies them to internal
storage and the calculation engine reads them from there.

## Fallback Behavior

If no `.se1` files are present, Swiss Ephemeris falls back to its built-in
Moshier ephemeris (≤ 1″ accuracy for planets, ≤ 5″ for the Moon). The
application remains fully functional, with slightly lower precision.
