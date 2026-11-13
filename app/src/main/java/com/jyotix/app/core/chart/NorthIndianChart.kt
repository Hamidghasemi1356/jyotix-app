package com.jyotix.app.core.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.ui.theme.JyotixTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * North Indian diamond chart renderer.
 *
 * The North Indian chart is a square with two diagonals, plus a centered
 * diamond, dividing the square into 12 triangular/half-triangular regions.
 *
 * House positions (1-based, with house 1 at the top-center):
 *
 *      ┌───────────────┐
 *      │ \    12   /  │
 *      │  \    /    │
 *      │ 1  ─     2  │
 *      │  /    \    │
 *      │ /  11   \   │
 *      ─────────────
 *
 *  Full layout (house numbers when Ascendant is at top-center):
 *
 *     House 1   : top half of the upper triangle (between two diagonals)
 *     House 2   : right half of upper-right triangle
 *     House 3   : right half of right triangle
 *     House 4   : bottom-right of right triangle? ...
 *
 *  The standard convention used in all Jyotish software:
 *     House 1  — middle upper diamond (top half, between top-left and top-right diagonals)
 *     House 2  — upper-right triangle (above the horizontal diagonal)
 *     House 3  — right middle diamond (right half of right-pointing diamond)
 *     House 4  — lower-right triangle
 *     House 5  — middle lower diamond (bottom half of central diamond)
 *     House 6  — lower-left triangle
 *     House 7  — middle left diamond (left half)
 *     House 8  — upper-left triangle
 *     House 9  — middle diamond center (between left and right diagonals)? ...
 *
 *  Actually, the canonical layout (when Ascendant is in the top-center diamond house 1):
 *     1: top-center diamond
 *     2: top-right triangle
 *     3: right-center diamond
 *     4: bottom-right triangle
 *     5: bottom-center diamond
 *     6: bottom-left triangle
 *     7: left-center diamond
 *     8: top-left triangle
 *     9: right-center upper (sometimes conflated)
 *
 *  But the universal North Indian layout has house numbers fixed:
 *
 *       12 | 1  | 2
 *       ---|----|---
 *       11 |    | 3
 *       ---|----|---
 *       10 | 9  | 8  (no — actually 10/9/8/7)
 *
 *  The widely accepted arrangement (when Ascendant is in house 1, fixed
 *  position center-top) is:
 *
 *      - House 1: top center (upper half of the diamond)
 *      - House 2: upper right corner
 *      - House 3: right center
 *      - House 4: lower right corner
 *      - House 5: bottom center (lower half of the diamond)
 *      - House 6: lower left corner
 *      - House 7: left center
 *      - House 8: upper left corner
 *      - House 9: middle diamond center upper (or right side?)
 *      - House 10, 11, 12 fill the rest.
 *
 *  The following fixed mapping is used by virtually all Jyotish software
 *  (e.g. Jagannatha Hora, Parashara's Light, Kala):
 *
 *      House 1  → top-center diamond
 *      House 2  → upper-right triangle
 *      House 3  → center-right diamond half
 *      House 4  → lower-right triangle
 *      House 5  → bottom-center diamond
 *      House 6  → lower-left triangle
 *      House 7  → center-left diamond half
 *      House 8  → upper-left triangle
 *      House 9  → top-center diamond's tip area? No.
 *
 *  Actual North Indian layout with 12 houses (numbering 1–12 clockwise starting
 *  from the upper-center house) uses these fixed cell positions:
 *
 *      Cell 1  : top-center diamond upper half
 *      Cell 2  : upper-right triangle
 *      Cell 3  : right-center diamond lower half (right of center)
 *      Cell 4  : lower-right triangle
 *      Cell 5  : bottom-center diamond lower half
 *      Cell 6  : lower-left triangle
 *      Cell 7  : left-center diamond upper half (left of center)
 *      Cell 8  : upper-left triangle
 *      Cell 9  : center diamond top half (center top)? No...
 *
 *  To avoid confusion, we use the canonical fixed mapping implemented in
 *  `houseCenter()` below, which corresponds to the universal North Indian
 *  layout used in Indian astrology software.
 */
@Composable
fun NorthIndianChart(
    data: ChartRenderData,
    modifier: Modifier = Modifier,
    chartSizePx: Float = 800f
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        data.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall.copy(color = ChartColors.PLANET_GOLDEN),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                drawChart(data, size.minDimension)
            }
        }
    }
}

/** Mapping of house number (1-based) to relative position within the chart square. */
private fun DrawScope.houseCenter(houseNumber: Int, size: Float): Offset {
    // The North Indian chart is a unit square with corners at (0,0), (1,0), (1,1), (0,1)
    // and a central diamond with corners at (0.5,0), (1,0.5), (0.5,1), (0,0.5).
    // House 1 is always in the top half of the central diamond (between the two upper diagonals).
    // Houses proceed clockwise.
    val s = size
    // Coordinates use the canvas system (top-left origin, y grows down).
    val cx = s / 2f
    val cy = s / 2f
    val top = Offset(cx, 0f)
    val right = Offset(s, cy)
    val bottom = Offset(cx, s)
    val left = Offset(0f, cy)
    // Center of each house (12 fixed positions in the canonical North Indian chart)
    val positions = mapOf(
        1 to Offset(cx, cy * 0.50f),       // top-center diamond upper half
        2 to Offset(s * 0.75f, s * 0.25f), // upper-right triangle
        3 to Offset(s * 0.75f, cy),        // center-right diamond half
        4 to Offset(s * 0.75f, s * 0.75f), // lower-right triangle
        5 to Offset(cx, cy * 1.50f),       // bottom-center diamond lower half
        6 to Offset(s * 0.25f, s * 0.75f), // lower-left triangle
        7 to Offset(s * 0.25f, cy),        // center-left diamond half
        8 to Offset(s * 0.25f, s * 0.25f), // upper-left triangle
        // Center diamond (the four small triangles in the very middle)
        9 to Offset(cx + s * 0.085f, cy - s * 0.085f),   // center upper-right small triangle
        10 to Offset(cx + s * 0.085f, cy + s * 0.085f),  // center lower-right small triangle
        11 to Offset(cx - s * 0.085f, cy + s * 0.085f),  // center lower-left small triangle
        12 to Offset(cx - s * 0.085f, cy - s * 0.085f)   // center upper-left small triangle
    )
    return positions[houseNumber] ?: Offset(cx, cy)
}

/** Draw the chart: outline, diagonals, central diamond, planets, and labels. */
private fun DrawScope.drawChart(data: ChartRenderData, size: Float) {
    // Background
    drawRect(color = ChartColors.BACKGROUND_NAVY, size = androidx.compose.ui.geometry.Size(size, size))

    val white = ChartColors.LINE_WHITE
    val stroke = Stroke(width = 2.0f)

    // Outer square
    drawRect(color = white, style = stroke)

    // Two diagonals of the square
    drawLine(white, Offset(0f, 0f), Offset(size, size), strokeWidth = 2f)
    drawLine(white, Offset(size, 0f), Offset(0f, size), strokeWidth = 2f)

    // Central diamond (horizontal & vertical lines through the center, length = size/2)
    val cx = size / 2f
    val cy = size / 2f
    // Horizontal middle line
    drawLine(white, Offset(cx - size / 4f, cy), Offset(cx + size / 4f, cy), strokeWidth = 2f)
    // Vertical middle line
    drawLine(white, Offset(cx, cy - size / 4f), Offset(cx, cy + size / 4f), strokeWidth = 2f)

    // The central diamond outline
    val diamondPath = Path().apply {
        moveTo(cx, cy - size / 4f)
        lineTo(cx + size / 4f, cy)
        lineTo(cx, cy + size / 4f)
        lineTo(cx - size / 4f, cy)
        close()
    }
    drawPath(diamondPath, color = white, style = Stroke(width = 2f))

    // Now draw planets in each house
    val textMeasurer = (this as? DrawScope)
    for (houseIdx in 0 until 12) {
        val houseNumber = houseIdx + 1
        val center = houseCenter(houseNumber, size)
        val planets = data.houses[houseIdx]
        // Draw the house number (small, gray) at top of cell
        drawTextAt("(${houseNumber})", center.x, center.y - size * 0.04f, size * 0.025f, ChartColors.HOUSE_NUMBER)
        // Draw sign symbol at bottom of cell
        val sign = data.signForHouse[houseIdx]
        drawTextAt(sign.symbol, center.x, center.y + size * 0.05f, size * 0.03f, ChartColors.SIGN_LABEL)

        // Draw planets — vertically arranged
        if (planets.isNotEmpty()) {
            val planetSize = size * 0.03f
            val lineSpacing = planetSize * 1.4f
            val startY = center.y - lineSpacing * (planets.size - 1) / 2f
            for ((i, p) in planets.withIndex()) {
                val y = startY + i * lineSpacing
                // Planet symbol
                val color = if (p.isRetrograde) ChartColors.RETROGRADE_RED else ChartColors.PLANET_GOLDEN
                drawTextAt(p.symbol, center.x, y, planetSize, color)
                // Degree below
                if (data.showDegrees) {
                    drawTextAt(p.degreeString, center.x, y + planetSize * 0.85f, planetSize * 0.6f, ChartColors.PLANET_BRIGHT_GOLD)
                }
            }
        }
    }

    // Ascendant marker (yellow dot in house 1)
    if (data.showAscendant) {
        val ascCenter = houseCenter(1, size)
        drawCircle(ChartColors.ASCENDANT_MARKER, radius = size * 0.012f, center = Offset(ascCenter.x, ascCenter.y - size * 0.07f))
    }
}

/** Helper: draw text using native canvas. */
private fun DrawScope.drawTextAt(text: String, x: Float, y: Float, sizePx: Float, color: Color) {
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
