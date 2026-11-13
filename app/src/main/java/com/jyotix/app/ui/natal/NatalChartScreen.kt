package com.jyotix.app.ui.natal

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.JyotixApp
import com.jyotix.app.R
import com.jyotix.app.core.calc.AyanamsaMode
import com.jyotix.app.core.calc.Aspects
import com.jyotix.app.core.calc.BirthData
import com.jyotix.app.core.calc.CalculationConfig
import com.jyotix.app.core.calc.CalculationEngine
import com.jyotix.app.core.calc.ChartCalculationResult
import com.jyotix.app.core.calc.Dasha
import com.jyotix.app.core.calc.Karakas
import com.jyotix.app.core.calc.Nakshatra
import com.jyotix.app.core.calc.Panchanga
import com.jyotix.app.core.calc.Planet
import com.jyotix.app.core.calc.SwissEphemeris
import com.jyotix.app.core.calc.Vargas
import com.jyotix.app.core.chart.NorthIndianChart
import com.jyotix.app.core.chart.buildDivisionalRenderData
import com.jyotix.app.core.chart.buildRenderData
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.NavyPanel
import com.jyotix.app.ui.theme.NavyPanelAlt
import com.jyotix.app.ui.theme.OffWhite
import com.jyotix.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The main natal chart screen.
 *
 * Shows D1, D9, D10 vertically; dropdown for other Vargas; top dropdowns for
 * Karakas, Panchanga, Aspects, Nakshatras, etc.; and the Vimshottari Dasha list.
 */
@Composable
fun NatalChartScreen(
    chartId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp
    val db = remember { JyotixDatabase.get(context) }

    var chart by remember { mutableStateOf<ChartCalculationResult?>(null) }
    var selectedVarga by remember { mutableStateOf(Vargas.Varga.D9) }
    var topDropdown by remember { mutableStateOf(TopDropdown.NONE) }

    LaunchedEffect(chartId) {
        val entity = db.savedChartDao().byId(chartId) ?: return@LaunchedEffect
        // Re-calculate from stored birth data (more accurate than JSON round-trip)
        withContext(Dispatchers.IO) {
            SwissEphemeris.ensureInitialized(context)
            val engine = app.calculationEngine
            val birth = BirthData(
                year = entity.birthYear,
                month = entity.birthMonth,
                day = entity.birthDay,
                hour = entity.birthHour,
                minute = entity.birthMinute,
                timeZoneId = entity.timeZoneId,
                utcOffsetHours = entity.utcOffsetHours,
                latitudeDeg = entity.latitude,
                longitudeDeg = entity.longitude,
                cityName = entity.cityName,
                countryName = entity.countryName
            )
            val result = try { engine.computeNatalChart(birth) } catch (_: Throwable) { null }
            withContext(Dispatchers.Main) { chart = result }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
    ) {
        JyotixTopBar(title = resStr(R.string.module_natal), onBack = onBack)

        val ch = chart
        if (ch == null) {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(text = resStr(R.string.loading), color = OffWhite)
            }
            return
        }

        // === Top dropdowns row ===
        TopDropdownsRow(
            selected = topDropdown,
            onSelect = { topDropdown = if (topDropdown == it) TopDropdown.NONE else it },
            chart = ch
        )

        // === D1 ===
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = resStr(R.string.chart_d1), color = GoldenPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val data = buildRenderData(ch, title = null)
            NorthIndianChart(data = data, modifier = Modifier.fillMaxWidth())
        }

        // === D9 ===
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = resStr(R.string.chart_d9), color = GoldenPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val d9Planets = Vargas.computeDivisionalChart(ch, Vargas.Varga.D9)
            val d9Asc = Vargas.navamsaLongitude(ch.ascendant.longitude)
            val data = buildDivisionalRenderData(d9Asc, d9Planets, title = null)
            NorthIndianChart(data = data, modifier = Modifier.fillMaxWidth())
        }

        // === D10 ===
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(text = resStr(R.string.chart_d10), color = GoldenPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val d10Planets = Vargas.computeDivisionalChart(ch, Vargas.Varga.D10)
            val d10Asc = Vargas.divisionalLongitude(Vargas.Varga.D10, ch.ascendant.longitude)
            val data = buildDivisionalRenderData(d10Asc, d10Planets, title = null)
            NorthIndianChart(data = data, modifier = Modifier.fillMaxWidth())
        }

        // === Other Vargas dropdown ===
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            VargaSelector(selected = selectedVarga, onSelect = { selectedVarga = it })
            Spacer(modifier = Modifier.height(8.dp))
            val others = Vargas.computeDivisionalChart(ch, selectedVarga)
            val oAsc = Vargas.divisionalLongitude(selectedVarga, ch.ascendant.longitude)
            val data = buildDivisionalRenderData(oAsc, others, title = "${selectedVarga.sanskritName} (${selectedVarga.persianName})")
            NorthIndianChart(data = data, modifier = Modifier.fillMaxWidth())
        }

        // === Vimshottari Dasha ===
        JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            DashaSection(chart = ch)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

enum class TopDropdown { NONE, KARAKAS, PANCHANGA, GRAHA_DRISHTI, RASHI_DRISHTI, NAKSHATRA, PADA, DEGREES, NAK_POS }

@Composable
private fun TopDropdownsRow(
    selected: TopDropdown,
    onSelect: (TopDropdown) -> Unit,
    chart: ChartCalculationResult
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TopDropdownChip(resStr(R.string.chara_karakas), selected == TopDropdown.KARAKAS) { onSelect(TopDropdown.KARAKAS) }
            TopDropdownChip(resStr(R.string.panchanga), selected == TopDropdown.PANCHANGA) { onSelect(TopDropdown.PANCHANGA) }
            TopDropdownChip(resStr(R.string.graha_drishti), selected == TopDropdown.GRAHA_DRISHTI) { onSelect(TopDropdown.GRAHA_DRISHTI) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TopDropdownChip(resStr(R.string.rashi_drishti), selected == TopDropdown.RASHI_DRISHTI) { onSelect(TopDropdown.RASHI_DRISHTI) }
            TopDropdownChip(resStr(R.string.nakshatra_table), selected == TopDropdown.NAKSHATRA) { onSelect(TopDropdown.NAKSHATRA) }
            TopDropdownChip(resStr(R.string.pada_info), selected == TopDropdown.PADA) { onSelect(TopDropdown.PADA) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TopDropdownChip(resStr(R.string.planetary_degrees), selected == TopDropdown.DEGREES) { onSelect(TopDropdown.DEGREES) }
            TopDropdownChip(resStr(R.string.planetary_nakshatra), selected == TopDropdown.NAK_POS) { onSelect(TopDropdown.NAK_POS) }
        }
        Spacer(modifier = Modifier.height(4.dp))
        when (selected) {
            TopDropdown.NONE -> {}
            TopDropdown.KARAKAS -> KarakasPanel(chart)
            TopDropdown.PANCHANGA -> PanchangaPanel(chart)
            TopDropdown.GRAHA_DRISHTI -> GrahaDrishtiPanel(chart)
            TopDropdown.RASHI_DRISHTI -> RashiDrishtiPanel(chart)
            TopDropdown.NAKSHATRA -> NakshatraTablePanel(chart)
            TopDropdown.PADA -> PadaPanel(chart)
            TopDropdown.DEGREES -> PlanetaryDegreesPanel(chart)
            TopDropdown.NAK_POS -> NakshatraPositionsPanel(chart)
        }
    }
}

@Composable
private fun TopDropdownChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) GoldenPrimary else NavyPanelAlt
    val fg = if (active) NavyBackground else OffWhite
    Box(
        modifier = Modifier
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Text(text = label, color = fg, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VargaSelector(selected: Vargas.Varga, onSelect: (Vargas.Varga) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "${selected.sanskritName} — ${selected.persianName}",
            onValueChange = {},
            readOnly = true,
            label = { Text(resStr(R.string.select_varga), color = OffWhite) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = androidx.compose.ui.text.TextStyle(color = OffWhite, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Vargas.Varga.entries.forEach { v ->
                DropdownMenuItem(
                    text = { Text("${v.sanskritName} (${v.divisionNumber}) — ${v.persianName}") },
                    onClick = { onSelect(v); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DashaSection(chart: ChartCalculationResult) {
    Text(text = "Vimshottari Dasha", color = GoldenPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    Spacer(modifier = Modifier.height(8.dp))
    val dashas = remember(chart) { Dasha.fromNatalChart(chart, includeLevels = 3) }
    dashas.take(3).forEach { maha ->
        Text(
            text = "${maha.level.lord.persianName} (${maha.level.startDateString} – ${maha.level.endDateString})",
            color = OffWhite,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        // First 3 Antardashas each
        maha.antardashas.take(3).forEach { antar ->
            Text(
                text = "   ${antar.level.lord.persianName} — ${antar.level.startDateString} → ${antar.level.endDateString}",
                color = OffWhite,
                fontSize = 12.sp
            )
            // First 2 Pratyantardashas each
            antar.pratyantardashas.take(2).forEach { p ->
                Text(
                    text = "      ${p.lord.persianName} — ${p.startDateString} → ${p.endDateString}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun KarakasPanel(chart: ChartCalculationResult) {
    val karakas = remember(chart) { Karakas.computeCharaKarakas(chart) }
    Column {
        Text("چارا کاراکاها (Chara Karakas):", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        karakas.forEach { k ->
            Text(
                "${k.karaka.persianName}: ${k.planet.persianName} — ${k.sign.persianName} (${String.format("%.2f", k.degreeInSign)}°)",
                color = OffWhite,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PanchangaPanel(chart: ChartCalculationResult) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp
    val p = remember(chart) {
        try {
            Panchanga.compute(chart.julianDay, app.calculationEngine)
        } catch (_: Throwable) { null }
    }
    if (p == null) {
        Text("Panchanga در دسترس نیست.", color = TextSecondary, fontSize = 12.sp)
        return
    }
    Column {
        Text("پنچانگا (Panchanga):", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text("Tithi: ${p.tithi.persianName} (${String.format("%.2f", p.tithiFraction * 100)}%)", color = OffWhite, fontSize = 12.sp)
        Text("Vaara: ${p.vaara.persianName}", color = OffWhite, fontSize = 12.sp)
        Text("Nakshatra: ${p.nakshatra.persianName} (${String.format("%.2f", p.nakshatraFraction * 100)}%)", color = OffWhite, fontSize = 12.sp)
        Text("Yoga: ${p.yoga.persianName}", color = OffWhite, fontSize = 12.sp)
        Text("Karana: ${p.karana.persianName}", color = OffWhite, fontSize = 12.sp)
    }
}

@Composable
private fun GrahaDrishtiPanel(chart: ChartCalculationResult) {
    val aspects = remember(chart) { Aspects.computeGrahaDrishti(chart) }
    Column {
        Text("درشتی سیارات (Graha Drishti):", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (aspects.isEmpty()) {
            Text("هیچ درشتی‌ای یافت نشد.", color = TextSecondary, fontSize = 12.sp)
        }
        aspects.take(15).forEach {
            Text("${it.fromPlanet.persianName} (H${it.fromHouse}) → ${it.toPlanet.persianName} (H${it.toHouse})", color = OffWhite, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RashiDrishtiPanel(chart: ChartCalculationResult) {
    val aspects = remember(chart) { Aspects.computeRashiDrishti(chart) }
    Column {
        Text("درشتی برج‌ها (Rashi Drishti):", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (aspects.isEmpty()) {
            Text("هیچ درشتی‌ای یافت نشد.", color = TextSecondary, fontSize = 12.sp)
        }
        aspects.take(15).forEach {
            Text("${it.first.persianName} → ${it.second.persianName}", color = OffWhite, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NakshatraTablePanel(chart: ChartCalculationResult) {
    Column {
        Text("جدول نکشتراها:", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        chart.planets.values.sortedBy { it.nakshatra.index }.forEach { pos ->
            Text("${pos.planet.persianName}: ${pos.nakshatra.persianName} — پادا ${pos.pada}", color = OffWhite, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PadaPanel(chart: ChartCalculationResult) {
    Column {
        Text("اطلاعات پادا (Pada):", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        chart.planets.values.forEach { pos ->
            val padaNakshatra = pos.nakshatra
            val padaLongitude = padaNakshatra.startLongitude + (pos.pada - 1) * com.jyotix.app.core.calc.AstroConstants.PADA_WIDTH
            Text("${pos.planet.persianName}: ${padaNakshatra.persianName} پادا ${pos.pada} (${String.format("%.2f", padaLongitude)}°)", color = OffWhite, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlanetaryDegreesPanel(chart: ChartCalculationResult) {
    Column {
        Text("درجات سیارات:", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        chart.planets.values.sortedBy { it.planet.ordinal }.forEach { pos ->
            Text("${pos.planet.persianName}: ${pos.formattedDegree} در ${pos.sign.persianName}${if (pos.isRetrograde) " (رجعی)" else ""}", color = OffWhite, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NakshatraPositionsPanel(chart: ChartCalculationResult) {
    Column {
        Text("موقعیت نکشترایی سیارات:", color = GoldenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        chart.planets.values.forEach { pos ->
            val within = pos.longitude - pos.nakshatra.startLongitude
            Text("${pos.planet.persianName}: ${pos.nakshatra.persianName} (${String.format("%.2f", within)}° درون، ${(pos.nakshatra.startLongitude + within)}° مطلق)", color = OffWhite, fontSize = 12.sp)
        }
    }
}

@Composable
private fun resStr(id: Int): String = androidx.compose.ui.res.stringResource(id)
