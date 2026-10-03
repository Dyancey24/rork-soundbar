package com.rork.soundbar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.Avatar
import com.rork.soundbar.data.Profile
import com.rork.soundbar.ui.components.AvatarArt
import com.rork.soundbar.ui.components.AvatarGlyphs
import com.rork.soundbar.ui.components.HairlineDivider
import com.rork.soundbar.ui.components.avatarPalette
import com.rork.soundbar.ui.theme.LocalConcept

/** Username cap, mirrored by the cloud's cleanName so both agree. */
private const val USERNAME_MAX = 24

/**
 * The profile desk: a username, an avatar mark drawn from the house's
 * catalogue, and the public opt-in. Everything is local until the opt-in is
 * on — then the name and mark ride the walk-by playlist and the leaderboard,
 * unless anonymous passes are switched on.
 */
@Composable
fun ProfileScreen(
    profile: Profile,
    onUpdate: (String, Avatar?, Boolean, Boolean) -> Unit,
    onShuffleAlias: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val context = LocalContext.current

    // The screen edits a draft that streams straight into the ViewModel, so
    // the shelf, the pass, and the cloud all follow along while you type.
    var username by remember { mutableStateOf(profile.username) }
    var avatar by remember { mutableStateOf(profile.avatar) }
    var isPublic by remember { mutableStateOf(profile.isPublic) }
    var anonymousPass by remember { mutableStateOf(profile.anonymousPass) }

    fun apply(
        nextName: String = username,
        nextAvatar: Avatar? = avatar,
        nextPublic: Boolean = isPublic,
        nextAnonymous: Boolean = anonymousPass
    ) {
        onUpdate(nextName, nextAvatar, nextPublic, nextAnonymous)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item("header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = concept.profileTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = concept.profileTagline,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            item("identity") {
                IdentityCard(
                    username = username,
                    avatar = avatar,
                    boardAlias = profile.boardAlias,
                    onNameChange = { value ->
                        username = value
                        apply(nextName = value)
                    },
                    onShuffle = onShuffleAlias
                )
            }

            item("marks") {
                MarksCard(
                    selected = avatar,
                    onSelect = { picked ->
                        // Tapping the chosen mark again puts it back down.
                        avatar = if (picked == avatar) null else picked
                        apply()
                    }
                )
            }

            item("public") {
                PublicCard(
                    isPublic = isPublic,
                    hasIdentity = username.isNotBlank() || avatar != null,
                    onToggle = { next ->
                        isPublic = next
                        apply(nextPublic = next)
                        val message = if (next) {
                            concept.profilePublicOnMessage
                        } else {
                            concept.profilePublicOffMessage
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // The anonymous-pass escape hatch only exists once the profile is
            // public — a private reader's passes are already anonymous.
            if (isPublic) {
                item("pass") {
                    PassCard(
                        anonymousPass = anonymousPass,
                        onToggle = { next ->
                            anonymousPass = next
                            apply(nextAnonymous = next)
                        }
                    )
                }
            }

            item("footnote") {
                Text(
                    text = concept.profileFootnote,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/** The name plate: the live avatar preview above the username field. */
@Composable
private fun IdentityCard(
    username: String,
    avatar: Avatar?,
    boardAlias: String,
    onNameChange: (String) -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            AvatarArt(
                avatar = avatar,
                fallbackText = username,
                size = 96.dp,
                contentDescription = concept.profileTitle
            )
            Text(
                text = username.ifBlank { concept.profileNameHint },
                color = if (username.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 17.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            OutlinedTextField(
                value = username,
                onValueChange = onNameChange,
                placeholder = { Text(text = concept.profileNameHint, fontSize = 14.sp) },
                label = { Text(text = concept.profileNameLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            // Until a username is chosen, the board calls the reader by the
            // house-dealt alias — reshufflable with one tap.
            if (username.isBlank() && boardAlias.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = concept.profileAliasLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = boardAlias,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onShuffle) {
                        Icon(
                            imageVector = Icons.Outlined.Shuffle,
                            contentDescription = concept.profileAliasShuffle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = concept.profileAliasHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** The mark and ink pickers, drawn from the shared avatar catalogue. */
@Composable
private fun MarksCard(
    selected: Avatar?,
    onSelect: (Avatar) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = concept.profileMarkLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            AvatarGlyphs.chunked(4).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (rowIndex == 0) 14.dp else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (glyph, icon, label) ->
                        val avatar = Avatar(glyph, selected?.palette ?: 0)
                        val isPicked = selected?.glyph == glyph
                        MarkTile(
                            avatar = avatar,
                            icon = icon,
                            label = label,
                            isPicked = isPicked,
                            onClick = { onSelect(avatar) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - row.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            HairlineDivider(modifier = Modifier.padding(vertical = 14.dp))
            Text(
                text = concept.profileColorLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(6) { palette ->
                    val isPicked = (selected?.palette ?: -1) == palette
                    InkSwatch(
                        paletteIndex = palette,
                        isPicked = isPicked,
                        onClick = {
                            onSelect(Avatar(selected?.glyph ?: "vinyl", palette))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** One mark choice: the glyph on a disc, ringed when it is the picked one. */
@Composable
private fun MarkTile(
    avatar: Avatar,
    icon: ImageVector,
    label: String,
    isPicked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (background, ink) = avatarPalette(avatar.palette)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = background,
            border = BorderStroke(
                if (isPicked) 2.dp else 1.dp,
                if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = ink,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(
            text = label,
            color = if (isPicked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** One ink choice: the palette's background disc with its ink glyph preview. */
@Composable
private fun InkSwatch(
    paletteIndex: Int,
    isPicked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (background, ink) = avatarPalette(paletteIndex)
    Surface(
        shape = CircleShape,
        color = background,
        border = BorderStroke(
            if (isPicked) 2.dp else 1.dp,
            if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Surface(
                shape = CircleShape,
                color = ink,
                modifier = Modifier.size(12.dp)
            ) {}
        }
        // The whole disc answers taps, including the ink dot drawn on top.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
        )
    }
}

/** The opt-in: what goes public when it's on, and the promise when it's off. */
@Composable
private fun PublicCard(
    isPublic: Boolean,
    hasIdentity: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPublic) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isPublic) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        val concept = LocalConcept.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = concept.profilePublicTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isPublic) concept.profilePublicOnHint else concept.profilePublicOffHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (isPublic && !hasIdentity) {
                    Text(
                        text = concept.profileNameHint,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            Switch(
                checked = isPublic,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/** The escape hatch: ride the pass, but leave the name at the door. */
@Composable
private fun PassCard(
    anonymousPass: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (anonymousPass) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            1.dp,
            if (anonymousPass) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = concept.profilePassTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (anonymousPass) {
                        concept.profilePassOnHint
                    } else {
                        concept.profilePassOffHint
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(
                checked = anonymousPass,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}