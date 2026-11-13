package com.jyotix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.ui.theme.BorderBlack
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.LightGray
import com.jyotix.app.ui.theme.NavyPanel
import com.jyotix.app.ui.theme.NavyPanelAlt
import com.jyotix.app.ui.theme.OffWhite

/**
 * A consistent top app bar for Jyotix screens.
 */
@Composable
fun JyotixTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyPanel)
            .border(width = 1.dp, color = BorderBlack)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = GoldenPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
        Text(
            text = title,
            color = GoldenPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        if (action != null) action() else Spacer(modifier = Modifier.width(48.dp))
    }
}

/**
 * A rectangular dashboard card with an icon, title, and consistent Jyotix design.
 */
@Composable
fun DashboardCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Box(
        modifier = modifier
            .height(130.dp)
            .background(NavyPanel, shape = RoundedCornerShape(4.dp))
            .border(width = 1.dp, color = BorderBlack, shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = OffWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A standard Jyotix button: rectangular, light gray / off-white, consistent size.
 */
@Composable
fun JyotixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bgColor = if (enabled) OffWhite else LightGray
    val textColor = if (enabled) NavyPanel else LightGray
    Box(
        modifier = modifier
            .height(48.dp)
            .background(bgColor, shape = RoundedCornerShape(2.dp))
            .border(width = 1.dp, color = BorderBlack, shape = RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Panel container with navy background and black border.
 */
@Composable
fun JyotixPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .background(NavyPanel)
            .border(width = 1.dp, color = BorderBlack)
            .padding(12.dp)
    ) {
        content()
    }
}
