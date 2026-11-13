package com.jyotix.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Jyotix Material 3 dark color scheme.
 *
 * The app forces a dark navy-blue background regardless of system theme, as
 * required by the visual identity.
 */
private val JyotixColorScheme = darkColorScheme(
    primary = GoldenPrimary,
    onPrimary = NavyBackground,
    primaryContainer = GoldenDark,
    onPrimaryContainer = OffWhite,
    secondary = GoldenBright,
    onSecondary = NavyBackground,
    secondaryContainer = NavyPanelAlt,
    onSecondaryContainer = OffWhite,
    tertiary = GoldenBright,
    onTertiary = NavyBackground,
    background = NavyBackground,
    onBackground = OffWhite,
    surface = NavyPanel,
    onSurface = OffWhite,
    surfaceVariant = NavyPanelAlt,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = OffWhite,
    outline = LightGray,
    outlineVariant = BorderBlack
)

/**
 * The root composable that applies the Jyotix theme.
 *
 * Always uses the dark navy-blue palette, applies Dubai font (if available),
 * and forces RTL layout direction for Persian.
 */
@Composable
fun JyotixTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = JyotixColorScheme
    val typography = jyotixTypography(context)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
