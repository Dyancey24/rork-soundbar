package com.rork.soundbar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Blend
import com.rork.soundbar.data.BlendBar
import com.rork.soundbar.data.GenreCatalog
import com.rork.soundbar.ui.TasteStats
import com.rork.soundbar.ui.components.ConceptToggle
import com.rork.soundbar.ui.components.IngredientTag
import com.rork.soundbar.ui.components.SectionTitle
import com.rork.soundbar.ui.theme.Concept
import com.rork.soundbar.ui.theme.LocalConcept
import java.util.Calendar

/** Blends displayed per shelf plank, like bottles behind the bar. */
private const val SHELF_CAPACITY = 3

/** Which stat card is expanded, if any. */
private enum class StatDetail { GENRES, HOURS, STREAK }

/**
 * The personal collection, staged like the back bar: most-played blends sit on
 * the top shelf, the rest line up on wooden planks below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    shelf: List<Blend>,
    playCounts: Map<String, Int>,
    stats: TasteStats,
    onOpenBlend: (String) -> Unit,
    onStartMixing: () -> Unit,
    onToggleConcept: () -> Unit,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    // House recipes re-serve under the current concept; a user's own mixes keep their identity.
    val displayShelf = shelf.map { BlendBar.adapt(it, concept) }
    val regulars = displayShelf.sortedByDescending { playCounts[it.id] ?: 0 }
        .filter { (playCounts[it.id] ?: 0) > 0 }
        .take(SHELF_CAPACITY)
    val rest = displayShelf.filterNot { blend -> regulars.any { it.id == blend.id } }
    var expandedDetail by rememberSaveable { mutableStateOf<StatDetail?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        BarGlow()
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item("header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = concept.shelfTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = concept.shelfCount(displayShelf.size),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    ConceptToggle(concept = concept, onToggle = onToggleConcept)
                }
            }

            if (displayShelf.isEmpty()) {
                item("empty") { EmptyShelf(onStartMixing = onStartMixing) }
            }

            if (regulars.isNotEmpty()) {
                item("top-label") {
                    SectionTitle(concept.topShelfTitle, modifier = Modifier.padding(top = 4.dp))
                }
                item("top-shelf") {
                    ShelfRow(
                        bottles = regulars,
                        playCounts = playCounts,
                        showPlays = true,
                        onOpenBlend = onOpenBlend
                    )
                }
            }

            if (rest.isNotEmpty()) {
                item("rest-label") {
                    SectionTitle(concept.restTitle, modifier = Modifier.padding(top = 4.dp))
                }
                rest.chunked(SHELF_CAPACITY).forEach { row ->
                    item(key = "shelf-${row.first().id}") {
                        ShelfRow(
                            bottles = row,
                            playCounts = playCounts,
                            showPlays = false,
                            onOpenBlend = onOpenBlend
                        )
                    }
                }
            }

            item("taste-hub") {
                TasteHub(
                    stats = stats,
                    playCounts = playCounts,
                    displayShelf = displayShelf,
                    expandedDetail = expandedDetail,
                    onToggleDetail = { detail ->
                        expandedDetail = if (expandedDetail == detail) null else detail
                    }
                )
            }
        }
        }
    }
}

/**
 * The taste hub at the foot of the shelf: one plaque holding the three
 * headline numbers. Tap a number and its story unfolds right beneath it,
 * inside the same card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TasteHub(
    stats: TasteStats,
    playCounts: Map<String, Int>,
    displayShelf: List<Blend>,
    expandedDetail: StatDetail?,
    onToggleDetail: (StatDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    var showChart by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = concept.tasteHubTitle,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = concept.tasteHubHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                HubSegment(
                    icon = Icons.Outlined.PieChart,
                    value = stats.genresExplored.toString(),
                    label = "genres explored",
                    selected = expandedDetail == StatDetail.GENRES,
                    onClick = { onToggleDetail(StatDetail.GENRES) },
                    modifier = Modifier.weight(1f)
                )
                HubSegmentDivider()
                HubSegment(
                    icon = Icons.Outlined.Schedule,
                    value = stats.hoursListened.toString(),
                    label = "hours listened",
                    selected = expandedDetail == StatDetail.HOURS,
                    onClick = { onToggleDetail(StatDetail.HOURS) },
                    modifier = Modifier.weight(1f)
                )
                HubSegmentDivider()
                HubSegment(
                    icon = Icons.Outlined.LocalFireDepartment,
                    value = stats.streakDays.toString(),
                    label = concept.streakLabel,
                    selected = expandedDetail == StatDetail.STREAK,
                    onClick = { onToggleDetail(StatDetail.STREAK) },
                    modifier = Modifier.weight(1f)
                )
            }
            AnimatedVisibility(
                visible = expandedDetail != null,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
            ) {
                val current = expandedDetail ?: return@AnimatedVisibility
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (current) {
                                    StatDetail.GENRES -> concept.genreDetailTitle
                                    StatDetail.HOURS -> concept.hoursDetailTitle
                                    StatDetail.STREAK -> concept.streakDetailTitle
                                },
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showChart = !showChart },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = if (showChart) {
                                        Icons.AutoMirrored.Outlined.Notes
                                    } else {
                                        Icons.Outlined.BarChart
                                    },
                                    contentDescription = concept.chartToggleLabel(showChart),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        when (current) {
                            StatDetail.GENRES -> {
                                if (stats.genreTally.isEmpty()) {
                                    DetailBody(
                                        text = concept.genreDetailEmptyBody,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                } else if (showChart) {
                                    GenreBarChart(
                                        tally = stats.genreTally,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                } else {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        stats.genreTally.forEach { (genreId, count) ->
                                            IngredientTag(
                                                concept.genreTallyLabel(GenreCatalog.name(genreId), count)
                                            )
                                        }
                                    }
                                }
                            }
                            StatDetail.HOURS -> {
                                if (showChart) {
                                    PourIconRow(
                                        totalPours = stats.totalPours,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                } else {
                                    val topPlay = playCounts.maxByOrNull { it.value }?.takeIf { it.value > 0 }
                                    val favourite = topPlay?.let { entry ->
                                        displayShelf.firstOrNull { it.id == entry.key }?.name
                                            ?: BlendBar.findHouseBlend(entry.key, concept)?.name
                                    }
                                    DetailBody(
                                        text = concept.hoursDetailBody(stats.totalPours, favourite),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                            StatDetail.STREAK -> {
                                if (stats.recentMixDays.size == 7 && showChart) {
                                    StreakStrip(
                                        recentDays = stats.recentMixDays,
                                        streakDays = stats.streakDays,
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                } else {
                                    DetailBody(
                                        text = concept.streakDetailBody(stats.streakDays, stats.mixDaysCount),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One tappable number inside the taste hub. */
@Composable
private fun HubSegment(
    icon: ImageVector,
    value: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlight by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(180),
        label = "hubSegment"
    )
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(highlight)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Hairline between the hub's numbers. */
@Composable
private fun HubSegmentDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .width(1.dp)
            .height(48.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}

@Composable
private fun DetailBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = modifier
    )
}

/**
 * The flavour board drawn as proportional bars: one track per genre, filled
 * against the week's biggest taste, with its tally on the right.
 */
@Composable
private fun GenreBarChart(tally: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val top = tally.take(6)
    val max = top.maxOf { it.second }.coerceAtLeast(1)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        top.forEach { (genreId, count) ->
            val fill by animateFloatAsState(
                targetValue = count / max.toFloat(),
                animationSpec = tween(500),
                label = "genreBar"
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = GenreCatalog.name(genreId),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "× $count",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fill)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        if (tally.size > top.size) {
            Text(
                text = "+ ${tally.size - top.size} more",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** Every pour as a little glass (or plate in the kitchen), lined up in a row. */
@Composable
private fun PourIconRow(totalPours: Int, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    val icon = if (concept == Concept.KITCHEN) Icons.Outlined.Restaurant else Icons.Outlined.LocalBar
    val shown = totalPours.coerceAtMost(12)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (totalPours == 0) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(shown) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (totalPours > 12) {
                Text(
                    text = "+${totalPours - 12}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = concept.poursChartCaption(totalPours),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The last seven days as flame pips — filled on days something was mixed. */
@Composable
private fun StreakStrip(recentDays: List<Boolean>, streakDays: Int, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    val letters = remember { lastSevenDayLetters() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            recentDays.forEachIndexed { index, active ->
                val isToday = index == recentDays.lastIndex
                val ring = if (active || isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .border(1.dp, ring, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            Icon(
                                imageVector = Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = letters[index],
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        Text(
            text = "$streakDays${concept.streakLabel}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

/** One-letter weekday labels for the streak strip, oldest day first. */
private fun lastSevenDayLetters(): List<String> {
    val base = Calendar.getInstance()
    return (6 downTo 0).map { offset ->
        val day = (base.clone() as Calendar)
        day.add(Calendar.DAY_OF_MONTH, -offset)
        when (day.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "M"
            Calendar.TUESDAY -> "T"
            Calendar.WEDNESDAY -> "W"
            Calendar.THURSDAY -> "T"
            Calendar.FRIDAY -> "F"
            else -> "S"
        }
    }
}

/** Bottles standing bottom-aligned on a wooden plank, with shelf-talker labels below. */
@Composable
private fun ShelfRow(
    bottles: List<Blend>,
    playCounts: Map<String, Int>,
    showPlays: Boolean,
    onOpenBlend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            bottles.forEach { blend ->
                if (LocalConcept.current == Concept.KITCHEN) {
                    DishTile(
                        blend = blend,
                        onClick = { onOpenBlend(blend.id) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    BottleTile(
                        blend = blend,
                        onClick = { onOpenBlend(blend.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            repeat(SHELF_CAPACITY - bottles.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Plank()
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
        ) {
            bottles.forEach { blend ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = blend.name,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showPlays) {
                        Text(
                            text = "${playCounts[blend.id] ?: 0} plays",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            repeat(SHELF_CAPACITY - bottles.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * One saved blend as a decanter: glass neck and body, filled with the base
 * genre's liquid, capped in the theme accent, wearing a cream paper label.
 */
@Composable
private fun BottleTile(
    blend: Blend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val liquid = GenreCatalog.find(blend.baseGenreId).liquid
    val glass = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Cap
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 9.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            // Neck
            Box(
                modifier = Modifier
                    .size(width = 13.dp, height = 13.dp)
                    .background(glass)
            )
            // Body
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 96.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
                    .background(glass)
            ) {
                // Liquid fill, leaving headspace under the shoulder.
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(liquid.copy(alpha = 0.88f), liquid)
                                )
                            )
                    )
                }
                // Glass highlight stripe
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 7.dp)
                        .size(width = 4.dp, height = 62.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                )
                // Paper label
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 42.dp, height = 30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(concept.bottleLabel),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 1.dp)
                                .background(concept.bottleLabelInk.copy(alpha = 0.4f))
                        )
                        Text(
                            text = GenreCatalog.name(blend.baseGenreId).uppercase(),
                            fontSize = 7.sp,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = concept.bottleLabelInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, start = 3.dp, end = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * One saved blend as a plated dish under a glass cloche — the kitchen's take
 * on the bottle shelf. The food mound takes the base genre's colour.
 */
@Composable
private fun DishTile(
    blend: Blend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liquid = GenreCatalog.find(blend.baseGenreId).liquid
    val glass = MaterialTheme.colorScheme.surfaceContainerHigh
    val domeShape = RoundedCornerShape(
        topStart = 34.dp,
        topEnd = 34.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Cloche knob
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(3.dp))
            // Glass dome
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 44.dp)
                    .clip(domeShape)
                    .background(glass)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, domeShape)
            ) {
                // Food mound peeking out from under the dome
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(
                            Brush.verticalGradient(listOf(liquid.copy(alpha = 0.9f), liquid))
                        )
                )
                // Dome highlight
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 11.dp, top = 9.dp)
                        .size(width = 4.dp, height = 20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
            // Plate and foot
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(width = 78.dp, height = 9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(glass)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 46.dp, height = 3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
        }
    }
}

/** Ghost bottle for the empty state — glass only, nothing poured yet. */
@Composable
private fun GhostBottle(modifier: Modifier = Modifier) {
    val glass = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 9.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Box(
            modifier = Modifier
                .size(width = 13.dp, height = 13.dp)
                .background(glass)
        )
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 96.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 10.dp,
                        bottomEnd = 10.dp
                    )
                )
                .background(glass)
        )
    }
}

/** Empty cloche for the kitchen's empty state — dome and plate, nothing served. */
@Composable
private fun GhostDish(modifier: Modifier = Modifier) {
    val glass = MaterialTheme.colorScheme.surfaceContainerHigh
    val domeShape = RoundedCornerShape(
        topStart = 34.dp,
        topEnd = 34.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 44.dp)
                .clip(domeShape)
                .background(glass)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, domeShape)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(width = 78.dp, height = 9.dp)
                .clip(RoundedCornerShape(50))
                .background(glass)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 46.dp, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        )
    }
}

/** The wooden plank the bottles stand on, with its shadow edge. */
@Composable
private fun Plank(modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .background(Brush.verticalGradient(listOf(concept.plankColor, concept.plankEdge)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .height(2.dp)
                .background(concept.plankEdge.copy(alpha = 0.45f))
        )
    }
}

@Composable
private fun EmptyShelf(onStartMixing: () -> Unit, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    Card(
        onClick = onStartMixing,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = concept.emptyTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = concept.emptyBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = concept.emptyCta,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                repeat(SHELF_CAPACITY) {
                    if (concept == Concept.KITCHEN) {
                        GhostDish(modifier = Modifier.weight(1f))
                    } else {
                        GhostBottle(modifier = Modifier.weight(1f))
                    }
                }
            }
            Plank()
        }
    }
}
