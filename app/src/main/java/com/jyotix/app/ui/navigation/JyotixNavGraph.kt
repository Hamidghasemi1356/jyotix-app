package com.jyotix.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jyotix.app.ui.alerts.AlertsScreen
import com.jyotix.app.ui.auth.LoginScreen
import com.jyotix.app.ui.auth.RegisterScreen
import com.jyotix.app.ui.dashboard.DashboardScreen
import com.jyotix.app.ui.education.EducationDetailScreen
import com.jyotix.app.ui.education.EducationListScreen
import com.jyotix.app.ui.moonscorpio.MoonScorpioScreen
import com.jyotix.app.ui.natal.BirthFormScreen
import com.jyotix.app.ui.natal.NatalChartScreen
import com.jyotix.app.ui.skymap.SkyMapScreen
import com.jyotix.app.ui.transit.TransitScreen

/**
 * Centralized navigation routes for Jyotix.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BIRTH_FORM = "birth_form"
    const val NATAL_CHART = "natal_chart/{chartId}"
    const val TRANSIT = "transit"
    const val MOON_SCORPIO = "moon_scorpio"
    const val SKY_MAP = "sky_map"
    const val EDUCATION = "education"
    const val EDUCATION_DETAIL = "education_detail/{articleId}"
    const val ALERTS = "alerts"

    fun natalChart(chartId: Long): String = "natal_chart/$chartId"
    fun educationDetail(articleId: Long): String = "education_detail/$articleId"
}

@Composable
fun JyotixNavHost() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onLogin = { navController.navigate(Routes.LOGIN) },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onNatal = { navController.navigate(Routes.BIRTH_FORM) },
                    onTransit = { navController.navigate(Routes.TRANSIT) },
                    onMoonScorpio = { navController.navigate(Routes.MOON_SCORPIO) },
                    onSkyMap = { navController.navigate(Routes.SKY_MAP) },
                    onEducation = { navController.navigate(Routes.EDUCATION) },
                    onAlerts = { navController.navigate(Routes.ALERTS) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onVerified = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.BIRTH_FORM) {
                BirthFormScreen(
                    onCalculated = { chartId ->
                        navController.navigate(Routes.natalChart(chartId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.NATAL_CHART,
                arguments = listOf(navArgument("chartId") { type = NavType.LongType })
            ) { backStack ->
                val chartId = backStack.arguments?.getLong("chartId") ?: 0L
                NatalChartScreen(
                    chartId = chartId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.TRANSIT) {
                TransitScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MOON_SCORPIO) {
                MoonScorpioScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SKY_MAP) {
                SkyMapScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EDUCATION) {
                EducationListScreen(
                    onArticleClick = { id -> navController.navigate(Routes.educationDetail(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.EDUCATION_DETAIL,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("articleId") ?: 0L
                EducationDetailScreen(articleId = id, onBack = { navController.popBackStack() })
            }
            composable(Routes.ALERTS) {
                AlertsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
