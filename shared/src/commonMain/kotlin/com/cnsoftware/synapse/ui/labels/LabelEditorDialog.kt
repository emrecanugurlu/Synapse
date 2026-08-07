package com.cnsoftware.synapse.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.LabelIcon
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.common.LabelBadge
import com.cnsoftware.synapse.ui.common.RoundedTextField
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.ui.theme.toContainerColor
import org.jetbrains.compose.resources.stringResource
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_cancel
import synapse.shared.generated.resources.action_save
import synapse.shared.generated.resources.label_create
import synapse.shared.generated.resources.label_icon
import synapse.shared.generated.resources.label_name
import synapse.shared.generated.resources.label_new
import synapse.shared.generated.resources.label_color
import synapse.shared.generated.resources.labels_edit_title

/**
 * Etiket oluşturma ve düzenleme için ortak dialog: ad, renk ve ikon.
 *
 * [existing] null ise oluşturma, doluysa düzenleme modunda açılır — iki ayrı
 * dialog tutmak yerine tek bir yerde, çünkü alanlar birebir aynı.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelEditorDialog(
    existing: Label?,
    onConfirm: (name: String, color: NoteColor, icon: LabelIcon) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember { mutableStateOf(existing?.color ?: NoteColor.DEFAULT) }
    var icon by remember { mutableStateOf(existing?.icon ?: LabelIcon.LABEL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (existing == null) Res.string.label_new else Res.string.labels_edit_title,
                ),
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Seçimlerin canlı önizlemesi: kullanıcı listede ne göreceğini
                // onaylamadan önce görüyor.
                LabelPreview(name = name, color = color, icon = icon)

                Spacer(modifier = Modifier.height(Spacing.lg))

                RoundedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(Res.string.label_name),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.lg))
                SectionTitle(stringResource(Res.string.label_color))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NoteColor.entries.forEach { option ->
                        SelectableSwatch(
                            selected = option == color,
                            background = option.toContainerColor(),
                            onClick = { color = option },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
                SectionTitle(stringResource(Res.string.label_icon))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LabelIcon.entries.forEach { option ->
                        SelectableIcon(
                            selected = option == icon,
                            icon = option,
                            onClick = { icon = option },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, color, icon) },
                enabled = name.isNotBlank(),
            ) {
                Text(
                    stringResource(
                        if (existing == null) Res.string.label_create else Res.string.action_save,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Spacing.sm),
    )
}

/** Listede nasıl görüneceğinin birebir önizlemesi. */
@Composable
private fun LabelPreview(name: String, color: NoteColor, icon: LabelIcon) {
    val placeholder = stringResource(Res.string.label_new)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        LabelBadge(
            name = name.ifBlank { placeholder },
            color = color,
            icon = icon,
        )
    }
}

@Composable
private fun SelectableSwatch(
    selected: Boolean,
    background: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SelectableIcon(
    selected: Boolean,
    icon: LabelIcon,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            )
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
    }
}
