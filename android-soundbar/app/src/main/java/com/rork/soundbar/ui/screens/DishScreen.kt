package com.rork.soundbar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Blend
import com.rork.soundbar.data.Garnish
import com.rork.soundbar.data.GarnishBar
import com.rork.soundbar.data.GenreCatalog
import com.rork.soundbar.data.Track
import com.rork.soundbar.ui.components.BlendArtwork
import com.rork.soundbar.ui.components.HairlineDivider
import com.rork.soundbar.ui.components.IngredientTag
import com.rork.soundbar.ui.components.NowPouringBars
import com.rork.soundbar.ui.components.NotePaper
import com.rork.soundbar.ui.theme.Concept
import com.rork.soundbar.ui.theme.LocalConcept

/**
 * The served dish: a full recipe card for one blend. No tab bar here — the play
 * and save actions own the bottom edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishScreen(
    blend: Blend,
    playingTrackIndex: Int?,
    isPlaying: Boolean,
    isSaved: Boolean,
    isSignature: Boolean,
    isGuest: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onToggleSignature: () -> Unit,
    canGarnish: Boolean,
    onApplyGarnish: (Garnish) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onBack: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onToggleSave: () -> Unit,
    isQueueing: Boolean,
    onQueueSpotify: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = blend.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSignature) {
                        Icon(
                            imageVector = if (isSignature) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isSignature) concept.clearSignatureLabel else concept.setSignatureLabel,
                            tint = accent
                        )
                    }
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            imageVector = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSaved) concept.removeLabel else concept.saveLabel,
                            tint = accent
                        )
                    }
                    if (canRemove) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = concept.guestRemoveLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            DishActionBar(
                playLabel = if (isPlaying) concept.pauseLabel else concept.playLabel,
                queueLabel = concept.spotifyQueueLabel,
                savedLabel = if (isSaved) concept.savedLabel else concept.saveLabel,
                isSaved = isSaved,
                isQueueing = isQueueing,
                onPlay = { if (playingTrackIndex != null) onTogglePlay() else onPlayTrack(0) },
                onQueueSpotify = onQueueSpotify,
                onToggleSave = onToggleSave
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp)
        ) {
            item("hero") {
                BlendArtwork(
                    imageUrl = blend.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
            }

            item("recipe") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text(
                        text = blend.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = blend.tagline,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "${concept.servedLine} · ${blend.servingLabel}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IngredientTag("${GenreCatalog.name(blend.baseGenreId)} base", emphasized = true)
                        if (isGuest) {
                            IngredientTag(label = concept.guestTag, emphasized = true)
                        }
                    }
                    if (blend.ingredients.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            blend.ingredients.take(3).forEach { ingredient ->
                                IngredientTag(
                                    "${GenreCatalog.name(ingredient.genreId)} · ${concept.doseLabel(ingredient.parts)}"
                                )
                            }
                        }
                    }
                    if (blend.garnishes.isNotEmpty()) {
                        Text(
                            text = blend.garnishes.joinToString(
                                separator = " · ",
                                prefix = "${concept.garnishAppliedLabel} "
                            ) { garnishLabel(concept, it) },
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }

            item("note") {
                NotePaper(
                    note = note,
                    onNoteChange = onNoteChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (canGarnish) {
                item("garnish") {
                    val garnishOptions = remember(blend.id, blend.garnishes.size, blend.tracks.size) {
                        GarnishBar.options(blend)
                    }
                    if (garnishOptions.isNotEmpty()) {
                        GarnishCard(
                            options = garnishOptions,
                            onApply = onApplyGarnish,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            itemsIndexed(blend.tracks, key = { index, track -> "$index-${track.title}" }) { index, track ->
                TrackRow(
                    index = index,
                    track = track,
                    isCurrent = playingTrackIndex == index,
                    isPlaying = isPlaying && playingTrackIndex == index,
                    onClick = { onPlayTrack(index) }
                )
                if (index != blend.tracks.lastIndex) {
                    HairlineDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val subdued = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = if (isCurrent) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    NowPouringBars()
                } else {
                    Text(
                        text = "${index + 1}.",
                        color = if (isCurrent) accent else subdued,
                        fontSize = 14.sp
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} · ${GenreCatalog.name(track.genreId)}",
                    color = subdued,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = track.duration,
                color = if (isCurrent) accent else subdued,
                fontSize = 13.sp
            )
        }
    }
}

/** Themed copy for one garnish, reused by the recipe block and the tray. */
private fun garnishLabel(concept: Concept, garnish: Garnish): String = when (garnish.kind) {
    Garnish.GENRE -> concept.garnishGenreLabel(GenreCatalog.name(garnish.genreId))
    Garnish.ARTIST -> concept.garnishArtistLabel(garnish.track?.artist.orEmpty())
    else -> concept.garnishSongLabel(garnish.track?.title.orEmpty())
}

/**
 * The finishing-touch tray, offered once a mixed drink is served: a splash of a
 * genre, a twist of an artist, or a single song dropped on top. Each tap pours
 * the extra tracks straight into the blend.
 */
@Composable
private fun GarnishCard(
    options: List<Garnish>,
    onApply: (Garnish) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = concept.garnishTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = concept.garnishHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            options.forEach { garnish ->
                GarnishOptionRow(garnish = garnish, onApply = onApply)
            }
        }
    }
}

@Composable
private fun GarnishOptionRow(
    garnish: Garnish,
    onApply: (Garnish) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val concept = LocalConcept.current
    val icon = when (garnish.kind) {
        Garnish.GENRE -> GenreCatalog.icon(garnish.genreId)
        Garnish.ARTIST -> Icons.Outlined.Album
        else -> Icons.Outlined.MusicNote
    }
    val sub = when (garnish.kind) {
        Garnish.GENRE -> GenreCatalog.find(garnish.genreId).flavor
        Garnish.ARTIST -> {
            val genre = garnish.track?.genreId?.let { GenreCatalog.name(it) }
            if (genre == null) "one track" else "one track · $genre"
        }
        else -> garnish.track?.artist?.let { "$it · ${GenreCatalog.name(garnish.track.genreId)}" }
            ?: "one track"
    }
    Surface(
        onClick = { onApply(garnish) },
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = garnishLabel(concept, garnish),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sub,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add garnish",
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DishActionBar(
    playLabel: String,
    queueLabel: String,
    savedLabel: String,
    isSaved: Boolean,
    isQueueing: Boolean,
    onPlay: () -> Unit,
    onQueueSpotify: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = playLabel,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = onQueueSpotify,
                    enabled = !isQueueing,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    if (isQueueing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = queueLabel,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .width(52.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = savedLabel,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
