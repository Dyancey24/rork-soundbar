package com.rork.soundbar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.LeaderboardData
import com.rork.soundbar.ui.components.HairlineDivider
import com.rork.soundbar.ui.theme.LocalConcept

/** One ranked line on the board: the reader themself or an added friend. */
private data class LeaderboardEntry(
    val id: String,
    val name: String,
    val points: Long,
    val isMe: Boolean
)

/**
 * The friends leaderboard: the reader's score and friend code, a row to add
 * a friend by code, and the ranked board beneath. The board lives in the
 * cloud under the signed-in account, so guests are shown the sign-in door.
 */
@Composable
fun LeaderboardScreen(
    signedIn: Boolean,
    points: Long,
    leaderboard: LeaderboardData?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onAddFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // The board belongs to the account, so it loads on arrival.
    LaunchedEffect(signedIn) {
        if (signedIn) onRefresh()
    }

    // The reader's local score is always fresher than any cloud snapshot.
    val entries = remember(leaderboard, points) {
        buildList {
            leaderboard?.me?.let { me ->
                add(LeaderboardEntry(me.id, me.name, maxOf(me.points, points), isMe = true))
            }
            leaderboard?.friends?.forEach { friend ->
                add(LeaderboardEntry(friend.id, friend.name, friend.points, isMe = false))
            }
        }.sortedByDescending { it.points }
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
                            text = concept.leaderboardTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = concept.leaderboardTagline,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            if (!signedIn) {
                item("invitation") {
                    SignInInvitation(onClick = onSignIn)
                }
            } else {
                item("me") {
                    MeCard(
                        points = points,
                        code = leaderboard?.me?.code,
                        onCopy = { code ->
                            clipboard.setText(AnnotatedString(code))
                            Toast.makeText(context, concept.codeCopiedMessage, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item("add") {
                    AddFriendCard(onAdd = onAddFriend)
                }
                item("board") {
                    BoardCard(entries = entries, isLoading = isLoading, onRemove = onRemoveFriend)
                }
            }
        }
    }
}

/** The reader's own plaque: their live score and the code friends trade. */
@Composable
private fun MeCard(
    points: Long,
    code: String?,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = concept.youLabel.take(1),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = concept.youLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = points.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " points",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }
            if (code != null) {
                HairlineDivider(modifier = Modifier.padding(top = 12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = concept.yourCodeLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = code,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = { onCopy(code) }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = concept.yourCodeLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** The friend-code exchange: type a friend's code, pour them onto the board. */
@Composable
private fun AddFriendCard(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    val concept = LocalConcept.current
    var code by remember { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = concept.addFriendLabel,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { value -> code = value.uppercase().take(8) },
                    placeholder = { Text(text = concept.friendCodeHint, fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onAdd(code)
                        code = ""
                    },
                    enabled = code.isNotBlank(),
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Text(text = concept.friendAddButton)
                }
            }
        }
    }
}

/** The ranked board: the reader plus every friend they've added. */
@Composable
private fun BoardCard(
    entries: List<LeaderboardEntry>,
    isLoading: Boolean,
    onRemove: (String) -> Unit,
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
                text = concept.friendsTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            when {
                entries.isEmpty() && isLoading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
                entries.isEmpty() -> {
                    Text(
                        text = concept.leaderboardEmpty,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {
                    entries.forEachIndexed { index, entry ->
                        BoardRow(rank = index + 1, entry = entry, onRemove = onRemove)
                        if (index < entries.lastIndex) {
                            HairlineDivider()
                        }
                    }
                }
            }
        }
    }
}

/** One ranked line: medal-coloured rank, name, score, and the way out for friends. */
@Composable
private fun BoardRow(
    rank: Int,
    entry: LeaderboardEntry,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val medal = when (rank) {
        1 -> Color(0xFFD4AF37)
        2 -> Color(0xFFAEB6C2)
        3 -> Color(0xFFB08D57)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Surface(shape = CircleShape, color = medal, modifier = Modifier.size(26.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = rank.toString(),
                    color = if (rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = if (entry.isMe) "${entry.name} · ${concept.youLabel}" else entry.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (entry.isMe) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        Text(
            text = entry.points.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!entry.isMe) {
            IconButton(onClick = { onRemove(entry.id) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = concept.friendRemovedMessage,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
