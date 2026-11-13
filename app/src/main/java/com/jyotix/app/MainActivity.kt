package com.jyotix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jyotix.app.ui.navigation.JyotixNavHost
import com.jyotix.app.ui.theme.JyotixTheme

/**
 * The single-activity entry point for Jyotix.
 *
 * Uses the SplashScreen API for an instant branded launch, then renders the
 * Jetpack Compose navigation host under the Jyotix theme with forced RTL
 * layout direction for Persian.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JyotixTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        JyotixNavHost()
                    }
                }
            }
        }
    }
}
