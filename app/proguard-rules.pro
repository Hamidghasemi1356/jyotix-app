# Jyotix ProGuard rules

# Swiss Ephemeris
-keep class swisseph.** { *; }
-keepclassmembers class swisseph.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Moshi
-keepclassmembers class **$$JsonAdapter { *; }
-keep class com.jyotix.app.** { *; }

# Kotlin Metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
