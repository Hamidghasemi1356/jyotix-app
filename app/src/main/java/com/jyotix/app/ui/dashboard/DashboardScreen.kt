package com.jyotix.app.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.JyotixApp
import com.jyotix.app.R
import com.jyotix.app.ui.components.DashboardCard
import com.jyotix.app.ui.components.JyotixButton
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.theme.BorderBlack
import com.jyotix.app.ui.theme.GoldenBright
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.NavyPanel
import com.jyotix.app.ui.theme.NavyPanelAlt
import com.jyotix.app.ui.theme.OffWhite

/**
 * The main Jyotix dashboard.
 *
 * Layout (top-to-bottom):
 *   1. Welcome panel with logo, welcome message, Login & Register buttons.
 *   2. Six module cards arranged in 2 columns (RTL ordering).
 */
@Composable
fun DashboardScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onNatal: () -> Unit,
    onTransit: () -> Unit,
    onMoonScorpio: () -> Unit,
    onSkyMap: () -> Unit,
    onEducation: () -> Unit,
    onAlerts: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp
    val authManager = app.authManager
    val signedIn = authManager.isSignedIn

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // === Welcome panel ===
        JyotixPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_jyotix_logo),
                    contentDescription = "Jyotix logo",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "JYOTIX",
                        color = GoldenPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.welcome),
                        color = OffWhite,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (signedIn) {
                    JyotixButton(
                        text = stringResource(R.string.logout),
                        onClick = { authManager.signOut() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    JyotixButton(
                        text = stringResource(R.string.login),
                        onClick = onLogin,
                        modifier = Modifier.weight(1f)
                    )
                    JyotixButton(
                        text = stringResource(R.string.register),
                        onClick = onRegister,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === Module cards in 2 columns (RTL: right column first) ===
        // Two-column grid layout, 3 rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First column (right in RTL)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(
                    title = stringResource(R.string.module_natal),
                    iconRes = R.drawable.ic_natal,
                    onClick = onNatal,
                    modifier = Modifier.fillMaxWidth()
                )
                DashboardCard(
                    title = stringResource(R.string.module_moon_scorpio),
                    iconRes = R.drawable.ic_moon_scorpio,
                    onClick = onMoonScorpio,
                    modifier = Modifier.fillMaxWidth()
                )
                DashboardCard(
                    title = stringResource(R.string.module_education),
                    iconRes = R.drawable.ic_education,
                    onClick = onEducation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Second column (left in RTL)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(
                    title = stringResource(R.string.module_transit),
                    iconRes = R.drawable.ic_transit,
                    onClick = onTransit,
                    modifier = Modifier.fillMaxWidth()
                )
                DashboardCard(
                    title = stringResource(R.string.module_sky_map),
                    iconRes = R.drawable.ic_sky_map,
                    onClick = onSkyMap,
                    modifier = Modifier.fillMaxWidth()
                )
                DashboardCard(
                    title = stringResource(R.string.module_alerts),
                    iconRes = R.drawable.ic_alerts,
                    onClick = onAlerts,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Small helper to use androidx.compose.ui.res.stringResource with the fa-IR locale. */
@Composable
private fun stringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)
