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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.JyotixApp
import com.jyotix.app.R
import com.jyotix.app.core.auth.AuthManager
import com.jyotix.app.core.calc.BirthData
import com.jyotix.app.core.calc.CalculationEngine
import com.jyotix.app.core.calc.ChartCalculationResult
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.db.entities.SavedChartEntity
import com.jyotix.app.core.db.entities.CityEntity
import com.jyotix.app.core.location.CityRepository
import com.jyotix.app.ui.components.JyotixButton
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.ErrorRed
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.NavyPanelAlt
import com.jyotix.app.ui.theme.OffWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Birth information form.
 *
 * The user enters birth date, time, and selects a city from the searchable
 * database. On "Calculate", the chart is computed and saved to Room, then the
 * navigation passes the new [SavedChartEntity.id] to the chart viewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthFormScreen(
    onCalculated: (chartId: Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp
    val scope = rememberCoroutineScope()
    val db = remember { JyotixDatabase.get(context) }
    val cityRepo = remember { CityRepository(context) }

    var name by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("1990") }
    var month by remember { mutableStateOf("1") }
    var day by remember { mutableStateOf("1") }
    var hour by remember { mutableStateOf("12") }
    var minute by remember { mutableStateOf("0") }
    var selectedCity by remember { mutableStateOf<CityEntity?>(null) }
    var cityQuery by remember { mutableStateOf("") }
    var cityResults by remember { mutableStateOf<List<CityEntity>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var calculating by remember { mutableStateOf(false) }

    LaunchedEffect(cityQuery) {
        if (cityQuery.length >= 2) {
            cityResults = withContext(Dispatchers.IO) { cityRepo.search(cityQuery) }
        } else {
            cityResults = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
    ) {
        JyotixTopBar(title = resString(R.string.calculate_chart), onBack = onBack)

        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نام شخص", color = OffWhite) },
                singleLine = true,
                textStyle = TextStyle(color = OffWhite, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth()
            )

            // Date selector
            JyotixPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = resString(R.string.select_date), color = GoldenPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}",
                    color = OffWhite,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { showDatePicker = true }
                )
            }

            // Time selector
            JyotixPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = resString(R.string.select_time), color = GoldenPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}",
                    color = OffWhite,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { showTimePicker = true }
                )
            }

            // City search
            OutlinedTextField(
                value = cityQuery,
                onValueChange = { cityQuery = it; selectedCity = null },
                label = { Text(resString(R.string.search_city), color = OffWhite) },
                singleLine = true,
                textStyle = TextStyle(color = OffWhite, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth()
            )
            if (cityResults.isNotEmpty() && selectedCity == null) {
                LazyColumn(
                    modifier = Modifier.height(180.dp).fillMaxWidth().background(NavyPanelAlt)
                ) {
                    items(cityResults) { city ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCity = city
                                    cityQuery = "${city.nameFa} (${city.countryFa})"
                                    cityResults = emptyList()
                                }
                                .padding(8.dp)
                        ) {
                            Text(text = city.nameFa, color = OffWhite, fontSize = 14.sp)
                            Text(text = "${city.name}, ${city.country}", color = OffWhite, fontSize = 11.sp)
                        }
                    }
                }
            }
            selectedCity?.let { c ->
                Text(
                    text = " Lat ${c.latitude}, Lon ${c.longitude}, TZ ${c.timezoneId}",
                    color = GoldenPrimary,
                    fontSize = 12.sp
                )
            }

            if (error != null) {
                Text(text = error ?: "", color = ErrorRed, fontSize = 13.sp)
            }

            JyotixButton(
                text = resString(R.string.calculate_chart),
                onClick = {
                    val city = selectedCity
                    val y = year.toIntOrNull()
                    val m = month.toIntOrNull()
                    val d = day.toIntOrNull()
                    val h = hour.toIntOrNull()
                    val min = minute.toIntOrNull()
                    if (y == null || m == null || d == null || h == null || min == null) {
                        error = "تاریخ یا ساعت نامعتبر است."
                        return@JyotixButton
                    }
                    if (city == null) {
                        error = "شهر تولد را انتخاب کنید."
                        return@JyotixButton
                    }
                    error = null
                    calculating = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Ensure ephemeris ready
                            com.jyotix.app.core.calc.SwissEphemeris.ensureInitialized(context)
                            val engine = app.calculationEngine
                            val birth = BirthData(
                                year = y, month = m, day = d, hour = h, minute = min,
                                timeZoneId = city.timezoneId,
                                utcOffsetHours = city.utcOffsetHours,
                                latitudeDeg = city.latitude,
                                longitudeDeg = city.longitude,
                                cityName = city.name,
                                countryName = city.country
                            )
                            val result = engine.computeNatalChart(birth)
                            // Persist
                            val authPhone = app.authManager.currentPhone ?: "anonymous"
                            val chartJson = serializeResult(result)
                            val id = db.savedChartDao().insert(
                                SavedChartEntity(
                                    ownerPhone = authPhone,
                                    name = name.ifBlank { "Chart-$y-$m-$d" },
                                    birthYear = y, birthMonth = m, birthDay = d,
                                    birthHour = h, birthMinute = min,
                                    timeZoneId = city.timezoneId,
                                    utcOffsetHours = city.utcOffsetHours,
                                    latitude = city.latitude,
                                    longitude = city.longitude,
                                    cityName = city.name,
                                    countryName = city.country,
                                    chartJson = chartJson
                                )
                            )
                            withContext(Dispatchers.Main) {
                                calculating = false
                                onCalculated(id)
                            }
                        } catch (t: Throwable) {
                            withContext(Dispatchers.Main) {
                                calculating = false
                                error = "خطا در محاسبه: ${t.message}"
                            }
                        }
                    }
                },
                enabled = !calculating,
                modifier = Modifier.fillMaxWidth()
            )
            if (calculating) {
                Text(text = resString(R.string.loading), color = GoldenPrimary, fontSize = 13.sp)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val cal = Calendar.getInstance().apply { timeInMillis = ms }
                        year = cal.get(Calendar.YEAR).toString()
                        month = (cal.get(Calendar.MONTH) + 1).toString()
                        day = cal.get(Calendar.DAY_OF_MONTH).toString()
                    }
                    showDatePicker = false
                }) { Text("تأیید", color = GoldenPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(resString(R.string.cancel), color = OffWhite) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = hour.toIntOrNull() ?: 12, initialMinute = minute.toIntOrNull() ?: 0, is24Hour = true)
        TimePickerDialog(
            state = timeState,
            onDismiss = { showTimePicker = false },
            confirm = {
                hour = timeState.hour.toString()
                minute = timeState.minute.toString()
                showTimePicker = false
            }
        )
    }
}

/**
 * Minimal JSON serialization of [ChartCalculationResult] for offline cache.
 */
private fun serializeResult(result: ChartCalculationResult): String {
    val sb = StringBuilder()
    sb.append("{\"jd\":").append(result.julianDay).append(",")
    sb.append("\"ayanamsa\":").append(result.ayanamsaValue).append(",")
    sb.append("\"ascendant\":").append(result.ascendant.longitude).append(",")
    sb.append("\"planets\":[")
    val entries = result.planets.entries
    for ((i, e) in entries.withIndex()) {
        if (i > 0) sb.append(",")
        sb.append("{\"p\":\"").append(e.key.name).append("\",")
        sb.append("\"lon\":").append(e.value.longitude).append(",")
        sb.append("\"dm\":").append(e.value.dailyMotion).append(",")
        sb.append("\"ret\":").append(if (e.value.isRetrograde) 1 else 0).append("}")
    }
    sb.append("]}")
    return sb.toString()
}

@Composable
private fun resString(id: Int): String = androidx.compose.ui.res.stringResource(id)

/**
 * Workaround for Material3 TimePicker not having a built-in dialog wrapper.
 * The caller must pass an externally-remembered [state]; the dialog renders
 * it and calls [confirm] when the user accepts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    state: androidx.compose.material3.TimePickerState,
    onDismiss: () -> Unit,
    confirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = confirm) { Text("تأیید", color = GoldenPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(resString(R.string.cancel), color = OffWhite) }
        },
        text = { TimePicker(state = state) }
    )
}
