package com.cnsoftware.synapse.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.ui.common.UndoEvent
import com.cnsoftware.synapse.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.undo_trashed
import synapse.shared.generated.resources.undo_unarchived

class ArchiveViewModel(
    private val repository: NoteRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // İlk sonuç gelene kadar true; boş liste ile "arşiv boş" mesajını karıştırmamak için.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.observeArchivedNotes() else repository.searchArchivedNotes(query)
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val _undoEvent = MutableStateFlow<UndoEvent?>(null)
    val undoEvent: StateFlow<UndoEvent?> = _undoEvent.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun toggleSelection(noteId: String) {
        _selectedIds.value = _selectedIds.value.let { if (noteId in it) it - noteId else it + noteId }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun unarchiveSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            ids.forEach { repository.setArchived(it, false) }
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_unarchived, ids.size) {
                ids.forEach { repository.setArchived(it, true) }
            }
        }
    }

    fun trashSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            ids.forEach { repository.moveToTrash(it) }
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_trashed, ids.size) {
                ids.forEach { repository.restoreFromTrash(it) }
            }
        }
    }

    fun unarchive(note: Note) {
        viewModelScope.launch {
            repository.setArchived(note.id, false)
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_unarchived, 1) {
                repository.setArchived(note.id, true)
            }
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            repository.moveToTrash(note.id)
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_trashed, 1) {
                repository.restoreFromTrash(note.id)
            }
        }
    }

    fun performUndo(event: UndoEvent) {
        viewModelScope.launch { event.undo() }
        dismissUndo(event)
    }

    fun dismissUndo(event: UndoEvent) {
        _undoEvent.compareAndSet(event, null)
    }
}
