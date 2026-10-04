package com.rork.soundbar.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.ui.components.SharingSwitch
import com.rork.soundbar.data.ArtistBook
import com.rork.soundbar.data.Blend
import com.rork.soundbar.data.BlendBar
import com.rork.soundbar.data.Garnish
import com.rork.soundbar.data.GenreCatalog
import com.rork.soundbar.data.GuestCard
import com.rork.soundbar.data.RecipeArtist
import com.rork.soundbar.ui.components.BlendArtwork
import com.rork.soundbar.ui.components.AvatarArt
import com.rork.soundbar.ui.components.ConceptToggle
import com.rork.soundbar.ui.components.Eyebrow
import com.rork.soundbar.ui.components.HairlineDivider
import com.rork.soundbar.ui.components.IngredientTag
import com.rork.soundbar.ui.components.NotePaper
import com.rork.soundbar.ui.components.PourButton
import com.rork.soundbar.ui.theme.Concept
import com.rork.soundbar.ui.theme.LocalConcept

/**
 * The Cookbook: every saved playlist written up as a recipe card — the dish's
 * photograph, its name, when it was last poured, the five artists on it (two
 * popular, three deep cuts), and the user's own margin notes. The signature
 * playlist wears a badge, and a second tab holds recipes collected from guests
 * nearby.
 */
@Composable
fun CookbookScreen(
    shelf: List<Blend>,
    guests: List<GuestCard>,
    notes: Map<String, String>,
    lastPlayed: Map<String, Long>,
    signatureId: String?,
    playingBlendId: String?,
    isPlaying: Boolean,
    isSharing: Boolean,
    isSharingAvailable: Boolean,
    onSetSharing: (Boolean) -> Unit,
    onRemoveGuest: (String) -> Unit,
    onOpenBlend: (String) -> Unit,
    onPlayBlend: (Blend) -> Unit,
    onUpdateNote: (String, String) -> Unit,
    onStartMixing: () -> Unit,
    onToggleConcept: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    // House recipes re-serve under the current concept; a user's own mixes keep their identity.
    val recipes = shelf.map { BlendBar.adapt(it, concept) }
    var tab by rememberSaveable { mutableStateOf(TAB_MINE) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item("header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = concept.cookbookTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (tab == TAB_MINE) {
                                concept.cookbookCount(recipes.size)
                            } else {
                                concept.guestCount(guests.size)
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    ConceptToggle(concept = concept, onToggle = onToggleConcept)
                }
            }

            item("tabs") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabChip(
                        label = concept.myRecipesTab,
                        selected = tab == TAB_MINE,
                        onClick = { tab = TAB_MINE }
                    )
                    TabChip(
                        label = if (guests.isEmpty()) {
                            concept.guestTab
                        } else {
                            "${concept.guestTab} · ${guests.size}"
                        },
                        selected = tab == TAB_GUESTS,
                        onClick = { tab = TAB_GUESTS }
                    )
                }
            }

            if (tab == TAB_MINE) {
                if (recipes.isEmpty()) {
                    item("empty") { EmptyCookbook(onStartMixing = onStartMixing) }
                }

                items(recipes, key = { it.id }) { blend ->
                    RecipeCard(
                        blend = blend,
                        note = notes[blend.id].orEmpty(),
                        lastPlayedAt = lastPlayed[blend.id],
                        isSignature = signatureId == blend.id,
                        isPlaying = playingBlendId == blend.id && isPlaying,
                        onOpen = { onOpenBlend(blend.id) },
                        onPlay = { onPlayBlend(blend) },
                        onNoteChange = { onUpdateNote(blend.id, it) }
                    )
                }
            } else {
                item("sharing") {
                    SharingCard(
                        isSharing = isSharing,
                        isSharingAvailable = isSharingAvailable,
                        onSetSharing = onSetSharing
                    )
                }

                if (guests.isEmpty()) {
                    item("guest-empty") { GuestEmptyCard() }
                } else {
                    items(guests, key = { it.blend.id }) { card ->
                        GuestRecipeCard(
                            card = card,
                            isPlaying = playingBlendId == card.blend.id && isPlaying,
                            onOpen = { onOpenBlend(card.blend.id) },
                            onPlay = { onPlayBlend(card.blend) },
                            onRemove = { onRemoveGuest(card.blend.id) }
                        )
                    }
                }
            }
        }
    }
}

private const val TAB_MINE = "mine"
private const val TAB_GUESTS = "guests"

/** One pill in the cookbook's tab switcher. */
@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

/**
 * The nearby-sharing switch: opt in once and the house pour drifts out to
 * guests nearby while theirs drift in. Only playlist data is exchanged.
 */
@Composable
private fun SharingCard(
    isSharing: Boolean,
    isSharingAvailable: Boolean,
    onSetSharing: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Eyebrow(concept.sharingTitle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = concept.sharingLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                SharingSwitch(
                    checked = isSharing,
                    onChecked = onSetSharing
                )
            }
            Text(
                text = when {
                    !isSharingAvailable -> concept.sharingUnavailable
                    isSharing -> concept.sharingActive
                    else -> concept.sharingHint
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** One written-up recipe: photograph, name, serving history, artist lineup, and its note. */
@Composable
private fun RecipeCard(
    blend: Blend,
    note: String,
    lastPlayedAt: Long?,
    isSignature: Boolean,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val artists = remember(blend.id) { ArtistBook.recipeArtists(blend) }
    var flipped by rememberSaveable(blend.id) { mutableStateOf(false) }
    FlipCard(
        flipped = flipped,
        modifier = modifier.fillMaxWidth(),
        front = { active ->
            Card(
                onClick = onOpen,
                enabled = active,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Box {
                        BlendArtwork(
                            imageUrl = blend.imageUrl,
                            scrim = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                        )
                        FlipChip(
                            label = concept.recipeBackTitle,
                            enabled = active,
                            onClick = { flipped = !flipped },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow(blend.servingLabel)
                            if (isSignature) {
                                IngredientTag(
                                    label = concept.signatureTag,
                                    emphasized = true,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            // A guest recipe that was adopted onto the shelf
                            // keeps its badge for life.
                            if (blend.id.startsWith("guest-")) {
                                IngredientTag(
                                    label = concept.guestTag,
                                    emphasized = true,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        Text(
                            text = blend.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    PourButton(
                        onClick = onPlay,
                        filled = true,
                        isPlaying = isPlaying,
                        size = 44,
                        contentDescription = if (isPlaying) {
                            "Pause ${blend.name}"
                        } else {
                            "Play ${blend.name}"
                        }
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IngredientTag("${GenreCatalog.name(blend.baseGenreId)} base", emphasized = true)
                    blend.ingredients.take(2).forEach { ingredient ->
                        IngredientTag(GenreCatalog.name(ingredient.genreId))
                    }
                }
                if (lastPlayedAt != null) {
                    Text(
                        text = concept.servedAgo(lastPlayedAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                HairlineDivider(modifier = Modifier.padding(vertical = 14.dp))
                Eyebrow(concept.artistsLabel)
                Column {
                    artists.forEach { artist ->
                        ArtistRow(artist)
                    }
                }
                NotePaper(
                    note = note,
                    onNoteChange = onNoteChange,
                    modifier = Modifier.padding(top = 16.dp)
                )
                }
            }
            }
        },
        back = { active ->
            RecipeBack(blend = blend, active = active, onFlipBack = { flipped = false })
        }
    )
}

/**
 * A guest's recipe card: the playlist they shared, written up like any other —
 * with a guest badge and when it drifted in, but no notes of anyone's.
 */
@Composable
private fun GuestRecipeCard(
    card: GuestCard,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val blend = BlendBar.adapt(card.blend, concept)
    val artists = remember(blend.id) { ArtistBook.recipeArtists(blend) }
    var flipped by rememberSaveable(blend.id) { mutableStateOf(false) }
    FlipCard(
        flipped = flipped,
        modifier = modifier.fillMaxWidth(),
        front = { active ->
            Card(
                onClick = onOpen,
                enabled = active,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Box {
                        BlendArtwork(
                            imageUrl = blend.imageUrl,
                            scrim = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                        )
                        FlipChip(
                            label = concept.recipeBackTitle,
                            enabled = active,
                            onClick = { flipped = !flipped },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow(blend.servingLabel)
                            IngredientTag(
                                label = concept.guestTag,
                                emphasized = true,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            text = blend.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    PourButton(
                        onClick = onPlay,
                        filled = true,
                        isPlaying = isPlaying,
                        size = 44,
                        contentDescription = if (isPlaying) {
                            "Pause ${blend.name}"
                        } else {
                            "Play ${blend.name}"
                        }
                    )
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = concept.guestRemoveLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IngredientTag("${GenreCatalog.name(blend.baseGenreId)} base", emphasized = true)
                    blend.ingredients.take(2).forEach { ingredient ->
                        IngredientTag(GenreCatalog.name(ingredient.genreId))
                    }
                }
                Text(
                    text = concept.guestReceivedAgo(card.receivedAtMillis),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
                // When the sender opted into a public profile, their name and
                // mark drift in with the recipe; otherwise it stays anonymous.
                card.senderName?.takeIf { it.isNotBlank() }?.let { sender ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        AvatarArt(
                            avatar = card.senderAvatar,
                            fallbackText = sender,
                            size = 22.dp
                        )
                        Text(
                            text = concept.guestFromLabel(sender),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                HairlineDivider(modifier = Modifier.padding(vertical = 14.dp))
                Eyebrow(concept.artistsLabel)
                Column {
                    artists.forEach { artist ->
                        ArtistRow(artist)
                    }
                }
                }
            }
            }
        },
        back = { active ->
            RecipeBack(blend = blend, active = active, onFlipBack = { flipped = false })
        }
    )
}

/**
 * A two-faced playlist card: the photograph and write-up face front, and the
 * blend's namesake recipe — base, pairings with their measures, garnishes —
 * faces back. The card rotates around its Y axis; the hidden face drops out
 * of the hit test so only the visible side answers taps.
 */
@Composable
private fun FlipCard(
    flipped: Boolean,
    front: @Composable (Boolean) -> Unit,
    back: @Composable (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
        label = "recipeCardFlip"
    )
    val frontVisible = rotation <= 90f
    Box(modifier) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 16f * density
                }
                .alpha(if (frontVisible) 1f else 0f)
        ) { front(frontVisible) }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationY = rotation + 180f
                    cameraDistance = 16f * density
                }
                .alpha(if (frontVisible) 0f else 1f)
        ) { back(!frontVisible) }
    }
}

/** A small bound book stamped on the photograph; turning it flips the card to its recipe. */
@Composable
private fun FlipChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A tiny bound book: spine down the left edge, cover rounded on the far side.
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 3.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            )
            Icon(
                imageVector = Icons.Outlined.Book,
                contentDescription = label,
                modifier = Modifier
                    .padding(start = 3.dp)
                    .size(16.dp)
            )
        }
    }
}

/** The back of the card: the blend's namesake recipe, written up row by row. */
@Composable
private fun RecipeBack(
    blend: Blend,
    active: Boolean,
    onFlipBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val body: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Eyebrow(concept.recipeBackTitle)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = concept.recipeBackHint,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = blend.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = blend.tagline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            HairlineDivider(modifier = Modifier.padding(vertical = 12.dp))
            RecipeRow(
                genreId = blend.baseGenreId,
                dose = concept.recipeBaseWord,
                emphasized = true
            )
            blend.ingredients.forEach { ingredient ->
                RecipeRow(
                    genreId = ingredient.genreId,
                    dose = concept.doseLabel(ingredient.parts)
                )
            }
            if (blend.garnishes.isNotEmpty()) {
                Text(
                    text = blend.garnishes.joinToString(
                        separator = " · ",
                        prefix = "${concept.garnishAppliedLabel} "
                    ) { garnishLabel(concept, it) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            Text(
                text = concept.recipeBackHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
    // The face facing away from the reader must be composed with no pointer
    // input at all — a disabled card would still swallow the taps that were
    // meant for the face in front.
    if (active) {
        Card(
            onClick = onFlipBack,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) { body() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) { body() }
    }
}

/** One line of the build: genre glyph, name, and its measure. */
@Composable
private fun RecipeRow(
    genreId: String,
    dose: String,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GenreCatalog.icon(genreId),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = GenreCatalog.name(genreId),
            fontSize = 15.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        Text(
            text = dose,
            fontSize = 13.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasized) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** Themed copy for one garnish, matching the recipe block on the dish screen. */
private fun garnishLabel(concept: Concept, garnish: Garnish): String = when (garnish.kind) {
    Garnish.GENRE -> concept.garnishGenreLabel(GenreCatalog.name(garnish.genreId))
    Garnish.ARTIST -> concept.garnishArtistLabel(garnish.track?.artist.orEmpty())
    else -> concept.garnishSongLabel(garnish.track?.title.orEmpty())
}

/** A single credited artist with their popular / deep-cut tag. */
@Composable
private fun ArtistRow(artist: RecipeArtist, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (artist.isPopular) Icons.Outlined.Star else Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = if (artist.isPopular) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = artist.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        IngredientTag(if (artist.isPopular) concept.popularTag else concept.undergroundTag)
    }
}

@Composable
private fun EmptyCookbook(onStartMixing: () -> Unit, modifier: Modifier = Modifier) {
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
                text = concept.cookbookEmptyTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = concept.cookbookEmptyBody,
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
        }
    }
}

/** The empty state of the guest tab, pointing at the sharing switch above it. */
@Composable
private fun GuestEmptyCard(modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = concept.guestEmptyTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = concept.guestEmptyBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
