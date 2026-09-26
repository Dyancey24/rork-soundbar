package com.rork.soundbar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.ui.theme.LocalConcept

/**
 * A scrap of cream paper — the same stock as the bottle labels — where the user
 * scribbles their own notes on a recipe. Saves as they type via onNoteChange.
 */
@Composable
fun NotePaper(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val concept = LocalConcept.current
    val ink = concept.bottleLabelInk
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Eyebrow(
                text = concept.notesLabel,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(concept.bottleLabel)
        ) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                textStyle = TextStyle(
                    color = ink,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(14.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (note.isEmpty()) {
                            Text(
                                text = concept.noteHint,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = ink.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
