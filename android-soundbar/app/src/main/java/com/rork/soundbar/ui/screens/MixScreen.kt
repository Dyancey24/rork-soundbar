package com.rork.soundbar.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Genre
import com.rork.soundbar.data.GenreCatalog
import com.rork.soundbar.data.Ingredient
import com.rork.soundbar.ui.components.BaseSpiritChip
import com.rork.soundbar.ui.components.ConceptToggle
import com.rork.soundbar.ui.components.Eyebrow
import com.rork.soundbar.ui.components.GlassLayer
import com.rork.soundbar.ui.components.MixingGlass
import com.rork.soundbar.ui.theme.Concept
import com.rork.soundbar.ui.theme.LocalConcept

/** How the genre tray can be ordered, so 20 bottles never become a wall. */
private enum class GenreSort(val label: String) {
    SUGGESTED("Suggested"),
    AZ("A–Z"),
    ENERGY("Energy")
}

/** Must stay in sync with the view model's mix limit. */
private const val MAX_ADDITIONS = 4

/** The mixing station: choose a base, dose in genres, watch the vessel fill. */
@Composable
fun MixScreen(
    baseGenreId: String,
    mix: List<Ingredient>,
    isShaking: Boolean,
    onSelectBase: (String) -> Unit,
    onAddIngredient: (String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onAdjustDose: (String, Int) -> Unit,
    onToggleConcept: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val base = GenreCatalog.find(baseGenreId)
    var sort by rememberSaveable { mutableStateOf(GenreSort.SUGGESTED) }
    var query by rememberSaveable { mutableStateOf("") }

    val suggestedIds = remember(baseGenreId) { base.pairings.toSet() }
    val trayGenres = remember(baseGenreId, sort, query) {
        val source = when (sort) {
            GenreSort.SUGGESTED -> GenreCatalog.mixers.sortedByDescending { it.id in suggestedIds }
            GenreSort.AZ -> GenreCatalog.mixers.sortedBy { it.name }
            GenreSort.ENERGY -> GenreCatalog.mixers.sortedByDescending { it.energy }
        }
        if (query.isBlank()) {
            source
        } else {
            source.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.flavor.contains(query, ignoreCase = true)
            }
        }
    }
    val canAdd = mix.size < MAX_ADDITIONS

    /** The house's wild card: never a curated pairing, never already in the glass. */
    val onSurprise: () -> Unit = {
        val inMix = mix.map { it.genreId }.toSet()
        val pool = GenreCatalog.mixers
            .filter { it.id !in inMix && it.id !in suggestedIds }
            .ifEmpty { GenreCatalog.mixers.filter { it.id !in inMix } }
        pool.shuffled().firstOrNull()?.let { onAddIngredient(it.id) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BarGlow()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item("title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = concept.mixingTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Eyebrow(concept.baseLabel, modifier = Modifier.padding(top = 10.dp))
                    }
                    ConceptToggle(concept = concept, onToggle = onToggleConcept)
                }
            }

            item("bases") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(GenreCatalog.bases, key = { it.id }) { genre ->
                        BaseSpiritChip(
                            genreId = genre.id,
                            selected = genre.id == baseGenreId,
                            onClick = { onSelectBase(genre.id) }
                        )
                    }
                }
            }

            item("glass") {
                RecipeGlassCard(
                    base = base,
                    mix = mix,
                    isShaking = isShaking,
                    onAdjustDose = onAdjustDose,
                    onRemoveIngredient = onRemoveIngredient
                )
            }

            item("tray-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Eyebrow(concept.trayLabel, modifier = Modifier.weight(1f))
                    Text(
                        text = "${mix.size} of $MAX_ADDITIONS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item("search") {
                TraySearchField(
                    query = query,
                    hint = concept.traySearchHint,
                    onQueryChange = { query = it }
                )
            }

            item("sort") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenreSort.entries.forEach { option ->
                        SortChip(
                            label = option.label,
                            selected = sort == option,
                            onClick = { sort = option }
                        )
                    }
                }
            }

            item("tray") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    item(key = "surprise") {
                        SurpriseTile(
                            enabled = canAdd,
                            onClick = onSurprise
                        )
                    }
                    items(trayGenres, key = { it.id }) { genre ->
                        val added = mix.any { it.genreId == genre.id }
                        GenreTile(
                            genre = genre,
                            added = added,
                            suggested = genre.id in suggestedIds,
                            showSuggestedBadge = sort == GenreSort.SUGGESTED && query.isBlank(),
                            enabled = added || canAdd,
                            onToggle = {
                                if (added) onRemoveIngredient(genre.id) else onAddIngredient(genre.id)
                            }
                        )
                    }
                }
            }

            if (query.isNotBlank() && trayGenres.isEmpty()) {
                item("no-results") {
                    Text(
                        text = concept.noResultsLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/** The recipe card: recipe lines on the left with leader rules, the live vessel on the right. */
@Composable
private fun RecipeGlassCard(
    base: Genre,
    mix: List<Ingredient>,
    isShaking: Boolean,
    onAdjustDose: (String, Int) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val accent = MaterialTheme.colorScheme.primary
    val layers = listOf(GlassLayer(base.liquid, 4)) +
        mix.map { GlassLayer(GenreCatalog.find(it.genreId).liquid, it.parts) }

    val shake = rememberInfiniteTransition(label = "shake")
    val wobble by shake.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 220
                -5f at 0
                5f at 110
                -5f at 220
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, bottom = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RecipeLine(
                    title = base.name,
                    subtitle = "BASE",
                    subtitleColor = accent,
                    trailing = null
                )
                if (mix.isEmpty()) {
                    Text(
                        text = concept.neatLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
                mix.forEach { ingredient ->
                    RecipeLine(
                        title = GenreCatalog.name(ingredient.genreId),
                        subtitle = "+ ${concept.doseLabel(ingredient.parts)}",
                        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        trailing = {
                            DoseStepper(
                                parts = ingredient.parts,
                                onDecrease = {
                                    if (ingredient.parts <= 1) {
                                        onRemoveIngredient(ingredient.genreId)
                                    } else {
                                        onAdjustDose(ingredient.genreId, -1)
                                    }
                                },
                                onIncrease = { onAdjustDose(ingredient.genreId, 1) }
                            )
                        }
                    )
                }
            }
            MixingGlass(
                layers = layers,
                bowl = concept == Concept.KITCHEN,
                accent = accent,
                accentSoft = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .width(148.dp)
                    .height(220.dp)
                    .rotate(if (isShaking) wobble else 0f)
            )
        }
    }
}

/** A single recipe line with the hairline rule that runs toward the vessel. */
@Composable
private fun RecipeLine(
    title: String,
    subtitle: String,
    subtitleColor: Color,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(10.dp))
            LeaderRule(modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
    }
}

/** Thin accent rule ending in a dot — the pointer from the recipe text to the vessel. */
@Composable
private fun LeaderRule(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.height(8.dp)) {
        val y = size.height / 2f
        drawLine(
            color = accent.copy(alpha = 0.55f),
            start = Offset(0f, y),
            end = Offset(size.width - 4.dp.toPx(), y),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(
            color = accent,
            radius = 2.5.dp.toPx(),
            center = Offset(size.width - 2.dp.toPx(), y)
        )
    }
}

@Composable
private fun DoseStepper(
    parts: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(2.dp)
        ) {
            StepperButton(Icons.Filled.Remove, "Less", onDecrease)
            Text(
                text = parts.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(18.dp),
                textAlign = TextAlign.Center
            )
            StepperButton(Icons.Filled.Add, "More", onIncrease)
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
        }
    }
}

/** Pill-shaped search field that filters the tray by name or flavor notes. */
@Composable
private fun TraySearchField(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(accent),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.padding(
                            start = 10.dp,
                            end = 6.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        )
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = hint,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                Surface(
                    onClick = { onQueryChange("") },
                    shape = CircleShape,
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The wild card at the head of the tray. One tap pours a random unconventional
 * genre — never a curated pairing, never one already in the glass.
 */
@Composable
private fun SurpriseTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .width(118.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            color = accent.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = concept.surpriseLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = concept.surpriseHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/** One pill in the sort selector. */
@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/**
 * One bottle on the tray: glyph, name, and flavor notes. Added genres fill in
 * with the accent and show a check; the rest stay outlined. When the mix is
 * full, un-added tiles dim and stop responding.
 */
@Composable
private fun GenreTile(
    genre: Genre,
    added: Boolean,
    suggested: Boolean,
    showSuggestedBadge: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .width(118.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        Surface(
            onClick = onToggle,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = if (added) {
                BorderStroke(1.dp, accent)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (added) accent else MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (added) Icons.Filled.Check else GenreCatalog.icon(genre.id),
                        contentDescription = null,
                        tint = if (added) MaterialTheme.colorScheme.onPrimary else accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = genre.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (added) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = genre.flavor,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        // Small accent dot marking the house pairings for the current base.
        if (showSuggestedBadge && suggested && !added) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(6.dp)
                    .background(accent, CircleShape)
            )
        }
    }
}
