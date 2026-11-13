package com.jyotix.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.R
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.db.entities.AlertPreferenceEntity
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.BorderBlack
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.NavyPanel
import com.jyotix.app.ui.theme.OffWhite
import com.jyotix.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import swisseph.SwissEph
import java.util.Calendar
import java.util.TimeZone

private val ALERT_CATEGORIES = listOf(
    "sign_change" to R.string.alert_sign_change,
    "retrograde" to R.string.alert_retrograde,
    "conjunction" to R.string.alert_conjunction,
    "eclipse" to R.string.alert_eclipse,
    "moon_scorpio" to R.string.alert_moon_scorpio,
    "transits" to R.string.alert_transits
)

@Composable
fun AlertsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { JyotixDatabase.get(context) }
    val prefDao = db.alertPreferenceDao()
    val alertDao = db.cachedAlertDao()
    val scope = rememberCoroutineScope()

    val prefs by prefDao.all().collectAsState(initial = emptyList())
    val nowJd = SwissEph.swe_julday(
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR),
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.MONTH) + 1,
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_MONTH),
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY),
        SwissEph.SE_GREG_CAL
    )
    val alerts by alertDao.upcoming(nowJd - 30.0).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(NavyBackground)) {
        JyotixTopBar(title = resStr(R.string.module_alerts), onBack = onBack)

        // Notification preferences
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "دسته‌بندی هشدارها", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            ALERT_CATEGORIES.forEach { (cat, label) ->
                val pref = prefs.firstOrNull { it.category == cat }
                val enabled = pref?.enabled ?: true
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = resStr(label), color = OffWhite, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { v ->
                            scope.launch {
                                prefDao.upsert(AlertPreferenceEntity(category = cat, enabled = v, leadTimeHours = pref?.leadTimeHours ?: 12))
                            }
                        }
                    )
                }
            }
        }

        // Cached alerts
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = "هشدارهای اخیر و پیش‌رو", color = GoldenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            if (alerts.isEmpty()) {
                Text(text = "هیچ هشداری موجود نیست. به‌روزرسانی روزانه توسط سیستم WorkManager انجام می‌شود.", color = TextSecondary, fontSize = 12.sp)
            } else {
                alerts.forEach { a ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(NavyPanel, shape = RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(text = a.titleFa, color = OffWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(text = a.bodyFa, color = TextSecondary, fontSize = 11.sp)
                            Text(text = "زمان رویداد: ${formatJd(a.eventJd)}", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
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
