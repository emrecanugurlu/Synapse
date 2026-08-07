package com.cnsoftware.synapse.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.toContainerColor
import com.cnsoftware.synapse.util.formatDateTime
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.card_more_items
import synapse.shared.generated.resources.card_more_labels
import org.jetbrains.compose.resources.stringResource

private const val MAX_PREVIEW_ITEMS = 5
private const val MAX_PREVIEW_LABELS = 3

/**
 * Not/Arşiv/Çöp Kutusu ekranlarının ortak kart bileşeni.
 * `topEndAction` ve `bottomActions` ekrana göre değişen aksiyonları (pin, arşivle, geri yükle, kalıcı sil vb.) enjekte etmek içindir.
 * `onLongClick` verilirse (çoklu seçim için) kart uzun basmayı da dinler; `selected` seçim kenarlığını çizer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    topEndAction: (@Composable () -> Unit)? = null,
    bottomActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = note.color.toContainerColor()),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 6.dp else 1.dp,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                topEndAction?.invoke()
            }

            if (note.isChecklist) {
                if (note.checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    // Önizlemede de bekleyenler önce gelsin; tamamlananlar listeyi doldurmasın.
                    val previewItems = note.checklistItems.sortedBy { it.isChecked }
                    Column {
                        previewItems.take(MAX_PREVIEW_ITEMS).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.height(16.dp).width(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                )
                            }
                        }
                        val remaining = note.checklistItems.size - MAX_PREVIEW_ITEMS
                        if (remaining > 0) {
                            Text(
                                text = stringResource(Res.string.card_more_items, remaining),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            note.reminderAt?.let { reminderAt ->
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(14.dp).width(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDateTime(reminderAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (note.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Kullanıcının etikete verdiği renk ve ikon burada da geçerli;
                    // rozet tanımı `LabelBadge` içinde tek yerde duruyor.
                    note.labels.take(MAX_PREVIEW_LABELS).forEach { label ->
                        LabelBadge(
                            name = label.name,
                            color = label.color,
                            icon = label.icon,
                            modifier = Modifier.wrapContentWidth(),
                        )
                    }
                    if (note.labels.size > MAX_PREVIEW_LABELS) {
                        Text(
                            text = stringResource(Res.string.card_more_labels, note.labels.size - MAX_PREVIEW_LABELS),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = Spacing.sm, vertical = 4.dp),
                        )
                    }
                }
            }

            if (bottomActions != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    content = bottomActions,
                )
            }
        }
    }
}
