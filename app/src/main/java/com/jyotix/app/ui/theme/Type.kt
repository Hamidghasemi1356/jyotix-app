package com.jyotix.app.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle

/**
 * Jyotix typography — Dubai Medium font as primary, with graceful fallback
 * to the system Persian-capable font if Dubai TTFs are not bundled.
 *
 * To enable Dubai font, place `dubai_medium.ttf` and `dubai_bold.ttf` in
 * `app/src/main/res/font/`. If the TTFs are missing, [loadDubaiFamily]
 * returns null and the system default is used.
 */
object JyotixFontProvider {
    private var cached: FontFamily? = null
    @Volatile private var tried = false

    /**
     * Attempt to load the Dubai font family. Returns null if the TTF files
     * are not present in the resources.
     */
    @Synchronized
    fun loadDubaiFamily(context: Context): FontFamily? {
        if (tried) return cached
        tried = true
        val mediumId = context.resources.getIdentifier("dubai_medium", "font", context.packageName)
        val boldId = context.resources.getIdentifier("dubai_bold", "font", context.packageName)
        if (mediumId == 0 && boldId == 0) return null
        cached = FontFamily(
            listOfNotNull(
                if (mediumId != 0) Font(mediumId, FontWeight.Medium) else null,
                if (boldId != 0) Font(boldId, FontWeight.Bold) else null
            )
        )
        return cached
    }
}

/**
 * Build the Jyotix Typography using the Dubai font family if available,
 * otherwise the system default (which renders Persian correctly).
 */
fun jyotixTypography(context: Context): Typography {
    val dubai = JyotixFontProvider.loadDubaiFamily(context)
    val base = Typography()
    return if (dubai != null) {
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = dubai),
            displayMedium = base.displayMedium.copy(fontFamily = dubai),
            displaySmall = base.displaySmall.copy(fontFamily = dubai),
            headlineLarge = base.headlineLarge.copy(fontFamily = dubai),
            headlineMedium = base.headlineMedium.copy(fontFamily = dubai),
            headlineSmall = base.headlineSmall.copy(fontFamily = dubai),
            titleLarge = base.titleLarge.copy(fontFamily = dubai),
            titleMedium = base.titleMedium.copy(fontFamily = dubai),
            titleSmall = base.titleSmall.copy(fontFamily = dubai),
            bodyLarge = base.bodyLarge.copy(fontFamily = dubai),
            bodyMedium = base.bodyMedium.copy(fontFamily = dubai),
            bodySmall = base.bodySmall.copy(fontFamily = dubai),
            labelLarge = base.labelLarge.copy(fontFamily = dubai),
            labelMedium = base.labelMedium.copy(fontFamily = dubai),
            labelSmall = base.labelSmall.copy(fontFamily = dubai)
        )
    } else {
        base
    }
}
