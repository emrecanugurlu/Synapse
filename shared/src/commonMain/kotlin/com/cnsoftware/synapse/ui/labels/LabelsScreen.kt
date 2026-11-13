package com.cnsoftware.synapse.ui.labels

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.domain.Label as LabelModel
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.common.ActionsBottomSheet
import com.cnsoftware.synapse.ui.common.EmptyState
import com.cnsoftware.synapse.ui.common.SheetActionRow
import com.cnsoftware.synapse.ui.common.SheetTitle
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.ui.theme.toContainerColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_open_menu
import synapse.shared.generated.resources.action_cancel
import synapse.shared.generated.resources.action_delete
import synapse.shared.generated.resources.action_more
import synapse.shared.generated.resources.label_create
import synapse.shared.generated.resources.labels_delete_message
import synapse.shared.generated.resources.labels_delete_title
import synapse.shared.generated.resources.labels_edit_title
import synapse.shared.generated.resources.labels_empty_description
import synapse.shared.generated.resources.labels_empty_title
import synapse.shared.generated.resources.labels_merge_menu
import synapse.shared.generated.resources.labels_merge_message
import synapse.shared.generated.resources.labels_merge_title
import synapse.shared.generated.resources.nav_labels

@Composable
fun LabelsScreen(
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    viewModel: LabelsViewModel = koinViewModel(),
) {
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<LabelModel?>(null) }
    var actionsTarget by remember { mutableStateOf<LabelModel?>(null) }
    var deleteTarget by remember { mutableStateOf<LabelModel?>(null) }
    var mergeSource by remember { mutableStateOf<LabelModel?>(null) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.nav_labels),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(Res.string.action_open_menu),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(Res.string.label_create),
                )
            }
        },
    ) { padding ->
        if (labels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Default.Label,
                    title = stringResource(Res.string.labels_empty_title),
                    description = stringResource(Res.string.labels_empty_description),
                )
            }
        } else {
            // Düz liste: satırlar zaten kendi renkli ikon daireleriyle
            // birbirinden ayrışıyor, ayrıca bir kart kabuğu gerekmiyor.
            LazyColumn(
                contentPadding = PaddingValues(vertical = Spacing.sm),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(labels, key = { it.id }) { label ->
                    LabelRow(
                        label = label,
                        onEdit = { editTarget = label },
                        onActions = { actionsTarget = label },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        LabelEditorDialog(
            existing = null,
            onConfirm = { name, color, icon ->
                viewModel.create(name, color, icon)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    editTarget?.let { label ->
        LabelEditorDialog(
            existing = label,
            onConfirm = { name, color, icon ->
                viewModel.update(label, name, color, icon)
                editTarget = null
            },
            onDismiss = { editTarget = null },
        )
    }

    // Satır başına üç ikon butonu yerine tek bir ⋮; işlemler burada.
    actionsTarget?.let { label ->
        ActionsBottomSheet(onDismiss = { actionsTarget = null }) {
            SheetTitle(label.name)
            SheetActionRow(
                icon = Icons.Default.Edit,
                label = stringResource(Res.string.labels_edit_title),
                onClick = {
                    actionsTarget = null
                    editTarget = label
                },
            )
            if (labels.size > 1) {
                SheetActionRow(
                    icon = Icons.Default.MergeType,
                    label = stringResource(Res.string.labels_merge_menu),
                    onClick = {
                        actionsTarget = null
                        mergeSource = label
                    },
                )
            }
            SheetActionRow(
                icon = Icons.Default.Delete,
                label = stringResource(Res.string.action_delete),
                destructive = true,
                onClick = {
                    actionsTarget = null
                    deleteTarget = label
                },
            )
        }
    }

    deleteTarget?.let { label ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(Res.string.labels_delete_title)) },
            text = { Text(stringResource(Res.string.labels_delete_message, label.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(label)
                        deleteTarget = null
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    // Hedef seçimi de alt sayfada: eskiden dialog içinde düz metin butonlar
    // listesiydi, hangi etikete gittiği renk/simge olmadan okunmuyordu.
    mergeSource?.let { source ->
        ActionsBottomSheet(onDismiss = { mergeSource = null }) {
            SheetTitle(stringResource(Res.string.labels_merge_title, source.name))
            Text(
                text = stringResource(Res.string.labels_merge_message, source.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    bottom = Spacing.md,
                ),
            )
            labels.filterNot { it.id == source.id }.forEach { target ->
                LabelRow(
                    label = target,
                    onEdit = {
                        viewModel.merge(source, target)
                        mergeSource = null
                    },
                    onActions = null,
                )
            }
        }
    }
}

/**
 * Etiket satırı.
 *
 * Satırın kendisi düzenlemeyi açıyor (birincil eylem); ikincil işlemler tek bir
 * ⋮ ile alt sayfaya taşındı. Önceden her satırda üç ayrı ikon butonu vardı ve
 * bunlardan biri tek maddelik bir açılır menü açıyordu.
 */
@Composable
private fun LabelRow(
    label: LabelModel,
    onEdit: () -> Unit,
    onActions: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            // Kart kabuğu kalktığı için satırlar ekran kenar boşluğunu
            // kendileri veriyor; solda diğer ekranlarla aynı hizada.
            .padding(
                start = Spacing.screenPadding,
                end = Spacing.sm,
                top = Spacing.sm,
                bottom = Spacing.sm,
            ),
    ) {
        // İkon, kullanıcının seçtiği renkli daire içinde: liste taranırken
        // etiketler birbirinden renkle ayrışıyor.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (label.color == NoteColor.DEFAULT) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        label.color.toContainerColor()
                    },
                ),
        ) {
            Icon(
                imageVector = label.icon.imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onActions != null) {
            IconButton(onClick = onActions) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.action_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
