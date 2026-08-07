package com.cnsoftware.synapse.ui.trash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cnsoftware.synapse.ui.common.EmptyState
import com.cnsoftware.synapse.ui.common.LoadingPlaceholder
import com.cnsoftware.synapse.ui.common.NoteCard
import com.cnsoftware.synapse.ui.common.NoteStaggeredGrid
import com.cnsoftware.synapse.ui.common.PillSearchBar
import com.cnsoftware.synapse.ui.common.SelectionTopBar
import org.koin.compose.viewmodel.koinViewModel
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_back
import synapse.shared.generated.resources.action_cancel
import synapse.shared.generated.resources.action_delete
import synapse.shared.generated.resources.empty_search_description
import synapse.shared.generated.resources.empty_search_title
import synapse.shared.generated.resources.empty_trash_description
import synapse.shared.generated.resources.empty_trash_title
import synapse.shared.generated.resources.search_trash_hint
import synapse.shared.generated.resources.selection_delete_forever
import synapse.shared.generated.resources.selection_restore
import synapse.shared.generated.resources.trash_confirm_delete_message
import synapse.shared.generated.resources.trash_confirm_delete_title
import synapse.shared.generated.resources.trash_confirm_empty_message
import synapse.shared.generated.resources.trash_empty_all
import org.jetbrains.compose.resources.stringResource

private enum class TrashConfirm { DELETE_SELECTED, EMPTY_ALL }

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit,
    viewModel: TrashViewModel = koinViewModel(),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectionMode = selectedIds.isNotEmpty()
    var searchActive by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<TrashConfirm?>(null) }

    // Geri: önce seçim/arama katmanını kapat, yoksa listeye dön.
    BackHandler {
        when {
            selectionMode -> viewModel.clearSelection()
            searchActive -> {
                searchActive = false
                viewModel.onSearchQueryChange("")
            }
            else -> onBack()
        }
    }

    Scaffold(
        topBar = {
            when {
                selectionMode -> SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onClose = viewModel::clearSelection,
                ) {
                    IconButton(onClick = viewModel::restoreSelected) {
                        Icon(Icons.Default.RestoreFromTrash, contentDescription = stringResource(Res.string.selection_restore))
                    }
                    IconButton(onClick = { confirm = TrashConfirm.DELETE_SELECTED }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = stringResource(Res.string.selection_delete_forever))
                    }
                }
                else -> PillSearchBar(
                    query = query,
                    searchActive = searchActive,
                    placeholder = stringResource(Res.string.search_trash_hint),
                    onQueryChange = viewModel::onSearchQueryChange,
                    onActivateSearch = { searchActive = true },
                    onCloseSearch = {
                        searchActive = false
                        viewModel.onSearchQueryChange("")
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                        }
                    },
                ) {
                    if (notes.isNotEmpty()) {
                        IconButton(onClick = { confirm = TrashConfirm.EMPTY_ALL }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(Res.string.trash_empty_all))
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (isLoading) {
            LoadingPlaceholder(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        NoteStaggeredGrid(
            notes = notes,
            empty = {
                if (query.isNotBlank()) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = stringResource(Res.string.empty_search_title),
                        description = stringResource(Res.string.empty_search_description, query),
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.DeleteOutline,
                        title = stringResource(Res.string.empty_trash_title),
                        description = stringResource(Res.string.empty_trash_description),
                    )
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { note ->
            NoteCard(
                note = note,
                onClick = {
                    if (selectionMode) viewModel.toggleSelection(note.id) else onNoteClick(note.id)
                },
                onLongClick = { viewModel.toggleSelection(note.id) },
                selected = note.id in selectedIds,
            )
        }
    }

    confirm?.let { pending ->
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(
                    when (pending) {
                        TrashConfirm.DELETE_SELECTED -> stringResource(Res.string.trash_confirm_delete_title)
                        TrashConfirm.EMPTY_ALL -> stringResource(Res.string.trash_empty_all)
                    },
                )
            },
            text = {
                Text(
                    when (pending) {
                        TrashConfirm.DELETE_SELECTED ->
                            stringResource(Res.string.trash_confirm_delete_message, count)
                        TrashConfirm.EMPTY_ALL ->
                            stringResource(Res.string.trash_confirm_empty_message)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (pending) {
                        TrashConfirm.DELETE_SELECTED -> viewModel.deleteSelectedForever()
                        TrashConfirm.EMPTY_ALL -> viewModel.emptyTrash()
                    }
                    confirm = null
                }) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}
