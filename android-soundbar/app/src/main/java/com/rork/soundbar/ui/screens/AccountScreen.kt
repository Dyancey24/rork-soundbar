package com.rork.soundbar.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Genre
import com.rork.soundbar.data.GenreCatalog
import com.rork.soundbar.data.Rewards
import com.rork.soundbar.data.StreamingPlatform
import com.rork.soundbar.ui.components.ConceptToggle
import com.rork.soundbar.ui.components.HairlineDivider
import com.rork.soundbar.ui.components.SectionTitle
import com.rork.soundbar.ui.theme.LocalConcept
import java.util.Locale

/**
 * The members' desk: the account plaque, the rewards hub where listening pays
 * points and genre badges lift the multiplier, and the house settings. Open to
 * guests too — signing in is offered, never demanded.
 */
@Composable
fun AccountScreen(
    signedIn: Boolean,
    accountName: String?,
    accountEmail: String?,
    points: Long,
    genreBadges: Set<String>,
    genreSongs: Map<String, Int>,
    eventBadges: Set<String>,
    songsHeard: Int,
    albumsCompleted: Int,
    selectedPlatform: StreamingPlatform,
    onSelectPlatform: (StreamingPlatform) -> Unit,
    onToggleConcept: () -> Unit,
    isSharing: Boolean,
    isSharingAvailable: Boolean,
    onSetSharing: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item("header") {
                Column {
                    Text(
                        text = concept.accountTitle,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = concept.openLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            item("account") {
                if (signedIn && accountName != null) {
                    AccountCard(name = accountName, email = accountEmail, onSignOut = onSignOut)
                } else {
                    SignInInvitation(onClick = onSignIn)
                }
            }

            item("rewards") {
                RewardsCard(
                    points = points,
                    genreBadges = genreBadges,
                    genreSongs = genreSongs,
                    eventBadges = eventBadges,
                    songsHeard = songsHeard,
                    albumsCompleted = albumsCompleted
                )
            }

            item("badges") {
                BadgeGrid(earned = genreBadges, songsByGenre = genreSongs)
            }

            if (eventBadges.contains(Rewards.FOUNDER_BADGE) || Rewards.isFounderWindowOpen()) {
                item("founder") {
                    FounderCard(isEarned = eventBadges.contains(Rewards.FOUNDER_BADGE))
                }
            }

            item("settings-label") {
                SectionTitle(concept.settingsTitle, modifier = Modifier.padding(top = 8.dp))
            }

            item("settings") {
                SettingsCard(
                    selectedPlatform = selectedPlatform,
                    onSelectPlatform = onSelectPlatform,
                    onToggleConcept = onToggleConcept,
                    isSharing = isSharing,
                    isSharingAvailable = isSharingAvailable,
                    onSetSharing = onSetSharing
                )
            }
        }
    }
}

/** The signed-in reader's plaque: initial, name, email, and the way out. */
@Composable
private fun AccountCard(
    name: String,
    email: String?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = name.trim().take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!email.isNullOrBlank()) {
                    Text(
                        text = email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            TextButton(onClick = onSignOut) {
                Text(text = "Sign out", fontSize = 12.sp)
            }
        }
    }
}

/** The ledger: points earned, the badge multiplier in force, and how they accrue. */
@Composable
private fun RewardsCard(
    points: Long,
    genreBadges: Set<String>,
    genreSongs: Map<String, Int>,
    eventBadges: Set<String>,
    songsHeard: Int,
    albumsCompleted: Int,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val genreLevels = Rewards.totalLevels(genreBadges, genreSongs)
    val multiplier = Rewards.multiplier(genreLevels, eventBadges.size * Rewards.EVENT_BADGE_BONUS)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = concept.rewardsTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = concept.rewardsTagline,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "×%.2f", multiplier),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = points.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " points",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 7.dp)
                )
            }
            HairlineDivider(modifier = Modifier.padding(top = 12.dp))
            RewardRow(label = "Songs heard", value = songsHeard.toString(), note = "+${Rewards.SONG_POINTS} pts each")
            RewardRow(label = "Albums finished", value = albumsCompleted.toString(), note = "+${Rewards.ALBUM_POINTS} pts each")
            RewardRow(label = concept.badgesTitle, value = "$genreLevels lv", note = "+1.25% a level")
            RewardRow(label = concept.eventBadgesTitle, value = eventBadges.size.toString(), note = "+25% each")
            Text(
                text = concept.rewardsExplainer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun RewardRow(label: String, value: String, note: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = note,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Every genre in the house, one badge each; earned badges glow in their own liquid colour. */
@Composable
private fun BadgeGrid(earned: Set<String>, songsByGenre: Map<String, Int>, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = concept.badgesTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${earned.size} of ${GenreCatalog.all.size} badges · ${Rewards.totalLevels(earned, songsByGenre)} levels · each level +1.25%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            GenreCatalog.all.chunked(4).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (rowIndex == 0) 14.dp else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { genre ->
                        BadgeTile(
                            genre = genre,
                            isEarned = earned.contains(genre.id),
                            level = maxOf(1, Rewards.level(songsByGenre[genre.id] ?: 0)),
                            progress = Rewards.levelProgress(songsByGenre[genre.id] ?: 0),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(
    genre: Genre,
    isEarned: Boolean,
    level: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val tileAlpha = if (isEarned) 1f else 0.35f
    val ringFill by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "badgeRingFill"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(54.dp).alpha(tileAlpha)
        ) {
            // The ring: a quiet track with the genre's own liquid colour filling
            // around it as the next level draws near.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                if (ringFill > 0f) {
                    drawArc(
                        color = genre.liquid,
                        startAngle = -90f,
                        sweepAngle = 360f * ringFill,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = genre.liquid,
                border = BorderStroke(
                    1.dp,
                    if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = GenreCatalog.icon(genre.id),
                        contentDescription = if (isEarned) genre.name else null,
                        tint = if (isEarned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Text(
            text = genre.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 4.dp)
                .alpha(tileAlpha)
        )
        if (isEarned) {
            Text(
                text = if (level >= Rewards.MAX_LEVEL) "Lv $level · max" else "Lv $level",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Text(
                text = concept.badgeLocked,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                modifier = Modifier.alpha(tileAlpha)
            )
        }
    }
}

/**
 * The limited event badge: minted for the house's earliest guests during the
 * launch window, kept forever, and worth a flat bonus on top of the genre board.
 */
@Composable
private fun FounderCard(isEarned: Boolean, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (isEarned) concept.founderTitle else null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = concept.founderTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = concept.founderDescription,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isEarned) MaterialTheme.colorScheme.primary else Color.Transparent,
                border = if (isEarned) {
                    null
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                },
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(
                    text = if (isEarned) concept.founderEarnedLabel else concept.founderBonusLabel,
                    color = if (isEarned) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/** The switches of the house: theme, streaming house, and nearby sharing. */
@Composable
private fun SettingsCard(
    selectedPlatform: StreamingPlatform,
    onSelectPlatform: (StreamingPlatform) -> Unit,
    onToggleConcept: () -> Unit,
    isSharing: Boolean,
    isSharingAvailable: Boolean,
    onSetSharing: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = concept.themeSettingLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                ConceptToggle(concept = concept, onToggle = onToggleConcept)
            }
            HairlineDivider()
            PlatformRow(
                label = concept.streamingLabel,
                selected = selectedPlatform,
                onSelect = onSelectPlatform
            )
            HairlineDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = concept.sharingTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isSharingAvailable) concept.sharingHint else concept.sharingUnavailable,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = isSharing,
                    onCheckedChange = onSetSharing,
                    enabled = isSharingAvailable,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
