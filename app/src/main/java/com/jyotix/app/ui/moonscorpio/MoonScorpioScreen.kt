package com.jyotix.app.ui.moonscorpio

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
import com.jyotix.app.core.calc.SwissEphemeris
import com.jyotix.app.core.calc.TransitEngine
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.OffWhite
import com.jyotix.app.ui.theme.ScorpioActive
import com.jyotix.app.ui.theme.SuccessGreen
import com.jyotix.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import swisseph.SwissEph
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Qamar Dar Aqrab (Moon in Scorpio) dashboard.
 *
 * Computes the actual periods when the Moon transits the sidereal sign of
 * Scorpio using the [TransitEngine]. Displays current status, current period
 * (if any), and upcoming periods.
 */
@Composable
fun MoonScorpioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp

    var status by remember { mutableStateOf<String?>(null) }
    var currentPeriod by remember { mutableStateOf<TransitEngine.MoonInScorpioPeriod?>(null) }
    var upcoming by remember { mutableStateOf<List<TransitEngine.MoonInScorpioPeriod>>(emptyList()) }
    var previous by remember { mutableStateOf<TransitEngine.MoonInScorpioPeriod?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            SwissEphemeris.ensureInitialized(context)
            val engine = app.calculationEngine
            val transit = TransitEngine(engine)
            val nowJd = SwissEph.swe_julday(
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).get(java.util.Calendar.YEAR),
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).get(java.util.Calendar.MONTH) + 1,
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).get(java.util.Calendar.DAY_OF_MONTH),
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).get(java.util.Calendar.HOUR_OF_DAY) +
                    java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).get(java.util.Calendar.MINUTE) / 60.0,
                SwissEph.SE_GREG_CAL
            )
            val snap = transit.snapshot(35.6892, 51.3890, nowJd)
            val inScorpio = snap.moonInScorpio
            val current = if (inScorpio) transit.findNextMoonInScorpio(nowJd - 3.0) else null
            val next = if (inScorpio) transit.findNextMoonInScorpio(current?.endJd ?: nowJd)
                       else transit.findNextMoonInScorpio(nowJd)
            val upcomingList = transit.findUpcomingMoonInScorpio(next.startJd + 0.01, 5)
            val prev = transit.findPreviousMoonInScorpio(nowJd)

            withContext(Dispatchers.Main) {
                status = if (inScorpio) "قمر اکنون در عقرب است" else "قمر اکنون در عقرب نیست"
                currentPeriod = if (inScorpio) current else null
                upcoming = listOf(next) + upcomingList.drop(1)
                previous = prev
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
    ) {
        JyotixTopBar(title = resStr(R.string.module_moon_scorpio), onBack = onBack)

        if (loading) {
            Text(text = resStr(R.string.loading), color = OffWhite, modifier = Modifier.padding(20.dp))
            return
        }

        // Current status
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "وضعیت فعلی", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            val (color, msg) = if (currentPeriod != null) ScorpioActive to (status ?: "")
                              else SuccessGreen to (status ?: "")
            Text(text = msg, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Current period (if any)
        currentPeriod?.let { p ->
            JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(text = "دوره جاری قمر در عقرب", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("شروع: ${formatJd(p.startJd)}", color = OffWhite, fontSize = 13.sp)
                Text("پایان: ${formatJd(p.endJd)}", color = OffWhite, fontSize = 13.sp)
                Text("مدت: ${String.format("%.2f", p.durationHours)} ساعت", color = TextSecondary, fontSize = 12.sp)
            }
        }

        // Previous period
        previous?.let { p ->
            JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(text = "دوره قبلی", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("شروع: ${formatJd(p.startJd)}", color = OffWhite, fontSize = 13.sp)
                Text("پایان: ${formatJd(p.endJd)}", color = OffWhite, fontSize = 13.sp)
            }
        }

        // Upcoming periods
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "دوره‌های پیش‌رو (۵ دوره آینده)", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            upcoming.forEachIndexed { i, p ->
                Text(
                    text = "${i + 1}. ${formatJd(p.startJd)} → ${formatJd(p.endJd)} (${String.format("%.1f", p.durationHours)} ساعت)",
                    color = OffWhite,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

private fun formatJd(jd: Double): String {
    val ymd = IntArray(3)
    val hms = DoubleArray(3)
    SwissEph.swe_revjul(jd, SwissEph.SE_GREG_CAL, ymd, hms)
    val hour = hms[0].toInt()
    val min = ((hms[0] - hour) * 60).toInt()
    return String.format("%04d-%02d-%02d %02d:%02d", ymd[0], ymd[1], ymd[2], hour, min)
}

@Composable
private fun resStr(id: Int): String = androidx.compose.ui.res.stringResource(id)
