# Swiss Ephemeris JAR

Place the official Swiss Ephemeris Java library JAR here:

```
app/libs/swisseph.jar
```

## Source

Download the Swiss Ephemeris Java sources from Astrodienst:

https://www.astro.com/ftp/swisseph/src/

Look for `swisseph.zip` (Java source distribution) or a pre-built
`swisseph.jar`. The package provides the `swisseph.SwissEph` class used
throughout Jyotix.

If only sources are available, build the JAR with:

```bash
unzip swisseph.zip -d swisseph-src
cd swisseph-src
javac -d build $(find . -name "*.java")
jar cf ../swisseph.jar -C build .
```

## Native Alternative

If you prefer the JNI native binding instead of the pure-Java port:

1. Place `.so` files in `app/src/main/jniLibs/<abi>/libsweph.so`
   for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
2. Remove the `implementation(files("libs/swisseph.jar"))` line from
   `app/build.gradle.kts`.
3. The Kotlin wrapper `core/calc/SwissEphemeris.kt` is compatible with
   both bindings.

## License

The Swiss Ephemeris is licensed by Astrodienst under the Swiss Ephemeris
Public License (SEPL). See:
https://www.astro.com/swisseph/swepha_e.htm
