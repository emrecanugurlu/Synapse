package com.cnsoftware.synapse.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.reminder.ReminderCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: NoteRepository,
    private val reminders: ReminderCoordinator,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // İlk sonuç gelene kadar true; boş liste ile "çöp kutusu boş" mesajını karıştırmamak için.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.observeTrashedNotes() else repository.searchTrashedNotes(query)
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun toggleSelection(noteId: String) {
        _selectedIds.value = _selectedIds.value.let { if (noteId in it) it - noteId else it + noteId }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun restore(note: Note) {
        viewModelScope.launch {
            repository.restoreFromTrash(note.id)
            // Çöpe atılırken iptal edilen hatırlatıcı, zamanı geçmediyse geri gelsin.
            reminders.reschedule(listOf(note.id))
        }
    }

    fun restoreSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            ids.forEach { repository.restoreFromTrash(it) }
            reminders.reschedule(ids)
        }
    }

    fun deleteForever(note: Note) {
        // Bu düzeltmeden önce çöpe atılmış notların alarmı hâlâ kurulu olabilir.
        reminders.cancel(listOf(note.id))
        viewModelScope.launch { repository.deletePermanently(note.id) }
    }

    fun deleteSelectedForever() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        reminders.cancel(ids)
        viewModelScope.launch { ids.forEach { repository.deletePermanently(it) } }
    }

    fun emptyTrash() {
        clearSelection()
        viewModelScope.launch {
            reminders.cancel(repository.observeTrashedNotes().first().map { it.id })
            repository.emptyTrash()
        }
    }
}
