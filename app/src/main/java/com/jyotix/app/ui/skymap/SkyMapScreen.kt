package com.jyotix.app.ui.skymap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.JyotixApp
import com.jyotix.app.R
import com.jyotix.app.core.calc.Planet
import com.jyotix.app.core.calc.PlanetPosition
import com.jyotix.app.core.calc.SwissEphemeris
import com.jyotix.app.core.calc.TransitEngine
import com.jyotix.app.core.calc.ZodiacSign
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.GoldenBright
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.OffWhite
import com.jyotix.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Flat 2D sky map (skywalk). NOT a 3D globe.
 *
 * Displays a flat circular sky chart with:
 *   • The 12 zodiac signs around the outer ring.
 *   • Planet symbols at their current sidereal longitudes.
 *   • Sun and Moon marked distinctly.
 *   • Sign boundary lines at every 30°.
 *   • Sample constellation shapes (connecting lines between stars).
 *
 * Pinch to zoom and pan.
 */
@Composable
fun SkyMapScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp

    var positions by remember { mutableStateOf<Map<Planet, PlanetPosition>>(emptyMap()) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            SwissEphemeris.ensureInitialized(context)
            val engine = app.calculationEngine
            val transit = TransitEngine(engine)
            val snap = transit.snapshot(35.6892, 51.3890)
            withContext(Dispatchers.Main) { positions = snap.positions }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(NavyBackground)) {
        JyotixTopBar(title = resStr(R.string.module_sky_map), onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.5f, 4f)
                        pan += panChange
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSkyMap(positions, size.minDimension, zoom, pan)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { zoom = (zoom * 1.2f).coerceIn(0.5f, 4f) }) {
                Icon(Icons.Filled.Add, contentDescription = resStr(R.string.sky_zoom_in), tint = GoldenPrimary)
            }
            Text(text = "زوم: ${String.format("%.1f", zoom)}x", color = OffWhite)
            IconButton(onClick = { zoom = (zoom / 1.2f).coerceIn(0.5f, 4f) }) {
                Icon(Icons.Filled.Remove, contentDescription = resStr(R.string.sky_zoom_out), tint = GoldenPrimary)
            }
            IconButton(onClick = { zoom = 1f; pan = Offset.Zero }) {
                Text(text = resStr(R.string.sky_reset), color = GoldenPrimary)
            }
        }
    }
}

private fun DrawScope.drawSkyMap(
    positions: Map<Planet, PlanetPosition>,
    size: Float,
    zoom: Float,
    pan: Offset
) {
    val cx = size / 2f + pan.x
    val cy = size / 2f + pan.y
    val outerR = size * 0.45f * zoom
    val innerR = outerR * 0.85f
    val planetRingR = outerR * 0.65f

    drawRect(color = NavyBackground, size = androidx.compose.ui.geometry.Size(size, size))

    drawCircle(color = OffWhite, radius = outerR, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
    drawCircle(color = OffWhite, radius = innerR, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))

    for (i in 0 until 12) {
        val angle = (i * 30.0 - 90.0) * PI / 180.0
        val x1 = cx + (cos(angle) * innerR).toFloat()
        val y1 = cy + (sin(angle) * innerR).toFloat()
        val x2 = cx + (cos(angle) * outerR).toFloat()
        val y2 = cy + (sin(angle) * outerR).toFloat()
        drawLine(OffWhite, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1f)
    }

    for (i in 0 until 12) {
        val sign = ZodiacSign.fromIndex(i)
        val midAngle = (i * 30.0 + 15.0 - 90.0) * PI / 180.0
        val r = (innerR + outerR) / 2f
        val x = cx + (cos(midAngle) * r).toFloat()
        val y = cy + (sin(midAngle) * r).toFloat()
        drawText(sign.symbol, x, y, size * 0.02f, GoldenPrimary)
    }

    for (i in 0 until 12) {
        val baseAngle = (i * 30.0 - 90.0) * PI / 180.0
        val s1 = Offset(cx + cos(baseAngle + 0.05) * (outerR * 0.7), cy + sin(baseAngle + 0.05) * (outerR * 0.7))
        val s2 = Offset(cx + cos(baseAngle + 0.10) * (outerR * 0.6), cy + sin(baseAngle + 0.10) * (outerR * 0.6))
        val s3 = Offset(cx + cos(baseAngle + 0.15) * (outerR * 0.7), cy + sin(baseAngle + 0.15) * (outerR * 0.7))
        drawLine(TextSecondary, s1, s2, strokeWidth = 0.8f)
        drawLine(TextSecondary, s2, s3, strokeWidth = 0.8f)
        drawCircle(OffWhite, radius = 1.5f, center = s1)
        drawCircle(OffWhite, radius = 2.0f, center = s2)
        drawCircle(OffWhite, radius = 1.5f, center = s3)
    }

    for ((planet, pos) in positions) {
        if (planet == Planet.KETU) continue
        val angle = (pos.longitude - 90.0) * PI / 180.0
        val r = planetRingR
        val x = cx + (cos(angle) * r).toFloat()
        val y = cy + (sin(angle) * r).toFloat()
        val color = when (planet) {
            Planet.SUN -> GoldenBright
            Planet.MOON -> OffWhite
            else -> GoldenPrimary
        }
        drawCircle(color, radius = 5f * zoom, center = Offset(x, y))
        drawText(planet.symbol, x, y + 15f, size * 0.025f, color)
    }
}

private fun DrawScope.drawText(text: String, x: Float, y: Float, sizePx: Float, color: Color) {
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        textSize = sizePx
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y + sizePx / 3f, paint)
}

@Composable
private fun resStr(id: Int): String = androidx.compose.ui.res.stringResource(id)
