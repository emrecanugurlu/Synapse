package com.cnsoftware.synapse.ui.labels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.domain.Label as LabelModel
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.ui.common.EmptyState
import com.cnsoftware.synapse.ui.theme.Spacing
import com.cnsoftware.synapse.ui.theme.imageVector
import com.cnsoftware.synapse.ui.theme.toContainerColor
import org.koin.compose.viewmodel.koinViewModel
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_back
import synapse.shared.generated.resources.action_cancel
import synapse.shared.generated.resources.action_delete
import synapse.shared.generated.resources.label_create
import synapse.shared.generated.resources.labels_delete_message
import synapse.shared.generated.resources.labels_delete_title
import synapse.shared.generated.resources.labels_empty_description
import synapse.shared.generated.resources.labels_empty_title
import synapse.shared.generated.resources.labels_merge
import synapse.shared.generated.resources.labels_merge_menu
import synapse.shared.generated.resources.labels_merge_message
import synapse.shared.generated.resources.labels_merge_title
import synapse.shared.generated.resources.labels_rename
import synapse.shared.generated.resources.nav_labels
import org.jetbrains.compose.resources.stringResource

@Composable
fun LabelsScreen(
    onBack: () -> Unit,
    viewModel: LabelsViewModel = koinViewModel(),
) {
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<LabelModel?>(null) }
    var deleteTarget by remember { mutableStateOf<LabelModel?>(null) }
    var mergeSource by remember { mutableStateOf<LabelModel?>(null) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nav_labels)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (labels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.Label,
                        title = stringResource(Res.string.labels_empty_title),
                        description = stringResource(Res.string.labels_empty_description),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(labels, key = { it.id }) { label ->
                        LabelRow(
                            label = label,
                            canMerge = labels.size > 1,
                            onEdit = { editTarget = label },
                            onMerge = { mergeSource = label },
                            onDelete = { deleteTarget = label },
                        )
                    }
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

    deleteTarget?.let { label ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(Res.string.labels_delete_title)) },
            text = { Text(stringResource(Res.string.labels_delete_message, label.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(label)
                    deleteTarget = null
                }) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }

    mergeSource?.let { source ->
        val targets = labels.filterNot { it.id == source.id }
        AlertDialog(
            onDismissRequest = { mergeSource = null },
            title = { Text(stringResource(Res.string.labels_merge_title, source.name)) },
            text = {
                Column {
                    Text(
                        text = stringResource(Res.string.labels_merge_message, source.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    targets.forEach { target ->
                        TextButton(
                            onClick = {
                                viewModel.merge(source, target)
                                mergeSource = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(target.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mergeSource = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun LabelRow(
    label: LabelModel,
    canMerge: Boolean,
    onEdit: () -> Unit,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenPadding, vertical = Spacing.xs),
    ) {
        // İkon, kullanıcının seçtiği renkli daire içinde: liste taranırken
        // etiketler birbirinden renkle ayrışıyor.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
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
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.labels_rename))
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, enabled = canMerge) {
                Icon(Icons.Default.MergeType, contentDescription = stringResource(Res.string.labels_merge))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.labels_merge_menu)) },
                    onClick = {
                        menuExpanded = false
                        onMerge()
                    },
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
        }
    }
}
