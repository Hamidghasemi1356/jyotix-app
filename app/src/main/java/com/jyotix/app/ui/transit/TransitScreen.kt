package com.jyotix.app.ui.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.JyotixApp
import com.jyotix.app.R
import com.jyotix.app.core.calc.CalculationEngine
import com.jyotix.app.core.calc.Planet
import com.jyotix.app.core.calc.SwissEphemeris
import com.jyotix.app.core.calc.TransitEngine
import com.jyotix.app.core.chart.NorthIndianChart
import com.jyotix.app.core.chart.buildRenderData
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.OffWhite
import com.jyotix.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import swisseph.SwissEph

/**
 * Daily Transit dashboard.
 *
 * Shows the current planetary positions in a North Indian chart, plus a list
 * of each planet's sign, degrees, Nakshatra, Pada, and retrograde status.
 */
@Composable
fun TransitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp

    var snapshot by remember { mutableStateOf<TransitEngine.TransitSnapshot?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            SwissEphemeris.ensureInitialized(context)
            val engine = app.calculationEngine
            val transit = TransitEngine(engine)
            // Use Tehran coordinates as default
            val s = transit.snapshot(35.6892, 51.3890)
            withContext(Dispatchers.Main) { snapshot = s }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
    ) {
        JyotixTopBar(title = resStr(R.string.module_transit), onBack = onBack)

        val snap = snapshot
        if (snap == null) {
            Text(text = resStr(R.string.loading), color = OffWhite, modifier = Modifier.padding(20.dp))
            return
        }

        // Current transit chart (D1 layout for "now")
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "چارت ترانزیت لحظه‌ای", color = GoldenPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Build a ChartCalculationResult-like object for the chart renderer.
            // We don't store full chart but the renderer only needs planets + ascendant.
            // For simplicity, we wrap snapshot as a pseudo-chart:
            val pseudoResult = com.jyotix.app.core.calc.ChartCalculationResult(
                birthData = com.jyotix.app.core.calc.BirthData(
                    year = 2000, month = 1, day = 1, hour = 12, minute = 0,
                    timeZoneId = "UTC", utcOffsetHours = 0.0,
                    latitudeDeg = 35.6892, longitudeDeg = 51.3890
                ),
                config = com.jyotix.app.core.calc.CalculationConfig(),
                julianDay = snap.julianDay,
                ayanamsaValue = 0.0,
                planets = snap.positions,
                ascendant = snap.ascendant ?: com.jyotix.app.core.calc.AscendantInfo(
                    longitude = 0.0,
                    houseCusps = (0 until 12).map { it * 30.0 },
                    mcLongitude = 0.0,
                    houseSystem = com.jyotix.app.core.calc.HouseSystem.WHOLE_SIGN
                )
            )
            val data = buildRenderData(pseudoResult, title = null)
            NorthIndianChart(data = data, modifier = Modifier.fillMaxWidth())
        }

        // List of planet positions
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "موقعیت سیارات در لحظه:", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            snap.positions.values.sortedBy { it.planet.ordinal }.forEach { pos ->
                Text(
                    text = "${pos.planet.persianName}: ${pos.sign.persianName} ${pos.formattedDegree} — ${pos.nakshatra.persianName} پادا ${pos.pada}${if (pos.isRetrograde) " (رجعی)" else ""}",
                    color = OffWhite,
                    fontSize = 12.sp
                )
            }
        }

        // Retrograde planets
        if (snap.retrogradePlanets.isNotEmpty()) {
            JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(text = "سیارات رجوعی:", color = GoldenPrimary, fontSize = 14.sp)
                Text(text = snap.retrogradePlanets.joinToString("، ") { it.persianName }, color = OffWhite, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun resStr(id: Int): String = androidx.compose.ui.res.stringResource(id)
