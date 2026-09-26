package com.rork.soundbar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.soundbar.data.GenreCatalog

/** Read-only genre tag. The recipe vocabulary that repeats on every surface. */
@Composable
fun IngredientTag(
    label: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = label,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/** Selectable base-spirit chip with its genre glyph. Springs slightly when picked. */
@Composable
fun BaseSpiritChip(
    genreId: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "chipScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = GenreCatalog.icon(genreId),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = GenreCatalog.name(genreId),
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
