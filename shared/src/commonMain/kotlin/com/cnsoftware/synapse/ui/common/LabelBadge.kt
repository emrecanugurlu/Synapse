package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.domain.LabelIcon
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.ui.theme.toContainerColor

/**
 * Etiketin her yerde aynı görünen rozeti: renkli zemin + ikon + ad.
 *
 * Not kartı, editör çipleri, drawer listesi ve etiket dialogu bunu paylaşıyor;
 * böylece kullanıcının seçtiği renk/ikon tek bir yerden yansıyor.
 */
@Composable
fun LabelBadge(
    name: String,
    color: NoteColor,
    icon: LabelIcon,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    // DEFAULT renk `surface`e düşüyor; kart zemininden ayrışması için
    // nötr rozet tonuna çevriliyor.
    val container = if (color == NoteColor.DEFAULT) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        color.toContainerColor()
    }
    val content = if (color == NoteColor.DEFAULT) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        // Renkli paletin tamamı açık tonlu; koyu metin her zaman okunur.
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
    ) {
        if (showIcon) {
            Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
