package com.cnsoftware.synapse.ui.archive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.cnsoftware.synapse.ui.common.UndoSnackbarEffect
import org.koin.compose.viewmodel.koinViewModel
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.action_back
import synapse.shared.generated.resources.empty_archive_description
import synapse.shared.generated.resources.empty_archive_title
import synapse.shared.generated.resources.empty_search_archive_description
import synapse.shared.generated.resources.empty_search_title
import synapse.shared.generated.resources.search_archive_hint
import synapse.shared.generated.resources.selection_delete
import synapse.shared.generated.resources.selection_unarchive
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit,
    viewModel: ArchiveViewModel = koinViewModel(),
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val undoEvent by viewModel.undoEvent.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectionMode = selectedIds.isNotEmpty()
    var searchActive by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    UndoSnackbarEffect(
        event = undoEvent,
        snackbarHostState = snackbarHostState,
        onUndo = viewModel::performUndo,
        onDismiss = viewModel::dismissUndo,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when {
                selectionMode -> SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onClose = viewModel::clearSelection,
                ) {
                    IconButton(onClick = viewModel::unarchiveSelected) {
                        Icon(Icons.Default.Unarchive, contentDescription = stringResource(Res.string.selection_unarchive))
                    }
                    IconButton(onClick = viewModel::trashSelected) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.selection_delete))
                    }
                }
                else -> PillSearchBar(
                    query = query,
                    searchActive = searchActive,
                    placeholder = stringResource(Res.string.search_archive_hint),
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
                )
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
                        description = stringResource(Res.string.empty_search_archive_description, query),
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.Inventory2,
                        title = stringResource(Res.string.empty_archive_title),
                        description = stringResource(Res.string.empty_archive_description),
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
}
