package com.jyotix.app.ui.education

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.R
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.BorderBlack
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.NavyBackground
import com.jyotix.app.ui.theme.NavyPanel
import com.jyotix.app.ui.theme.NavyPanelAlt
import com.jyotix.app.ui.theme.OffWhite
import com.jyotix.app.ui.theme.TextSecondary

private val CATEGORIES = listOf(
    "intro_jyotish" to R.string.edu_intro_jyotish,
    "zodiac" to R.string.edu_zodiac_signs,
    "planets" to R.string.edu_planets,
    "houses" to R.string.edu_houses,
    "nakshatras" to R.string.edu_nakshatras,
    "vargas" to R.string.edu_vargas,
    "dashas" to R.string.edu_dashas,
    "panchanga" to R.string.edu_panchanga,
    "aspects" to R.string.edu_aspects,
    "transit" to R.string.edu_transit
)

@Composable
fun EducationListScreen(
    onArticleClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { JyotixDatabase.get(context).educationDao() }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val category = CATEGORIES[selectedCategoryIndex].first
    val articles by dao.byCategory(category).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().background(NavyBackground)) {
        JyotixTopBar(title = resStr(R.string.module_education), onBack = onBack)

        TabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = NavyPanel,
            contentColor = GoldenPrimary
        ) {
            CATEGORIES.forEachIndexed { i, (_, label) ->
                Tab(
                    selected = i == selectedCategoryIndex,
                    onClick = { selectedCategoryIndex = i },
                    text = { Text(resStr(label), color = if (i == selectedCategoryIndex) GoldenPrimary else OffWhite, fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(articles) { article ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(NavyPanel, shape = RoundedCornerShape(4.dp))
                        .clickable { onArticleClick(article.id) }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = article.titleFa, color = OffWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = article.titleEn, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EducationDetailScreen(articleId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { JyotixDatabase.get(context).educationDao() }
    var article by remember { mutableStateOf<com.jyotix.app.core.db.entities.EducationArticleEntity?>(null) }
    LaunchedEffectImpl(articleId) {
        article = dao.byId(articleId)
    }

    Column(modifier = Modifier.fillMaxSize().background(NavyBackground)) {
        JyotixTopBar(title = article?.titleFa ?: resStr(R.string.module_education), onBack = onBack)
        val a = article
        if (a != null) {
            JyotixPanel(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(text = a.titleFa, color = GoldenPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = a.bodyFa, color = OffWhite, fontSize = 14.sp)
            }
        } else {
            Text(text = resStr(R.string.loading), color = OffWhite, modifier = Modifier.padding(20.dp))
        }
    }
}

/** Simple wrapper for LaunchedEffect that uses IO dispatcher. */
@Composable
private fun LaunchedEffectImpl(key: Any?, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}

@Composable
private fun resStr(id: Int): String = androidx.compose.ui.res.stringResource(id)
