package com.cnsoftware.synapse.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.share.ShareService
import com.cnsoftware.synapse.share.toShareText
import com.cnsoftware.synapse.domain.SyncStatus
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.PreferencesStore
import com.cnsoftware.synapse.reminder.ReminderCoordinator
import com.cnsoftware.synapse.ui.common.UndoEvent
import com.cnsoftware.synapse.util.currentTimeMillis
import com.cnsoftware.synapse.util.generateId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import synapse.shared.generated.resources.Res
import synapse.shared.generated.resources.undo_archived
import synapse.shared.generated.resources.undo_trashed

/** Etiket metinleri UI katmanında çözümlenir (yerelleştirme için). */
enum class SortOption {
    UPDATED,
    CREATED,
    COLOR,
}

private const val DAY_IN_MILLIS = 24L * 60 * 60 * 1000

class NoteListViewModel(
    private val repository: NoteRepository,
    private val shareService: ShareService,
    private val preferences: PreferencesStore,
    private val settings: AppSettings,
    private val reminders: ReminderCoordinator,
) : ViewModel() {

    init {
        // Açılışta, ayarlarda seçilen süreden eski çöp kutusu notlarını kalıcı olarak temizle.
        viewModelScope.launch {
            val retentionMillis = settings.trashRetention.value.days * DAY_IN_MILLIS
            repository.purgeTrashedBefore(currentTimeMillis() - retentionMillis)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(
        preferences.getString(PreferencesStore.KEY_SORT_OPTION)
            ?.let { saved -> SortOption.entries.firstOrNull { it.name == saved } }
            ?: SortOption.UPDATED,
    )
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedLabelId = MutableStateFlow<String?>(null)
    val selectedLabelId: StateFlow<String?> = _selectedLabelId.asStateFlow()

    val labels: StateFlow<List<Label>> = repository.observeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Veritabanından ilk sonuç gelene kadar true. Başlangıç değeri boş liste
    // olduğu için, bu bayrak olmadan ekran bir an "Henüz not yok" gösteriyordu.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = combine(_searchQuery, _selectedLabelId) { query, labelId ->
        query to labelId
    }
        .flatMapLatest { (query, labelId) ->
            when {
                query.isNotBlank() -> repository.searchActiveNotes(query)
                labelId != null -> repository.observeActiveNotesByLabel(labelId)
                else -> repository.observeActiveNotes()
            }
        }
        .combine(_sortOption) { notes, sort -> notes.sortedWith(sort.comparator()) }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
        preferences.putString(PreferencesStore.KEY_SORT_OPTION, option.name)
    }

    /** Aynı etikete tekrar dokunmak filtreyi kaldırır (çip davranışı). */
    fun onLabelFilterChange(labelId: String?) {
        _selectedLabelId.value = if (_selectedLabelId.value == labelId) null else labelId
    }

    /**
     * Filtreyi doğrudan ayarlar (drawer'dan gezinme).
     * Burada "aç/kapa" mantığı yok: kullanıcı çekmeceden bir etiket seçtiğinde
     * beklentisi o etikete gitmek, seçiliyse filtreyi kaldırmak değil.
     */
    fun setLabelFilter(labelId: String?) {
        _selectedLabelId.value = labelId
    }

    private val _undoEvent = MutableStateFlow<UndoEvent?>(null)
    val undoEvent: StateFlow<UndoEvent?> = _undoEvent.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    /**
     * Seçilenlerin tamamı sabitlenmişse true; üst bardaki butonun "sabitlemeyi
     * kaldır"a dönmesi için. Buton her iki durumda aynı görünürse kullanıcı
     * zaten sabitli bir nota tekrar dokunup hiçbir şey olmadığını görüyor.
     */
    val allSelectedPinned: StateFlow<Boolean> =
        combine(_selectedIds, notes) { ids, allNotes ->
            val selected = allNotes.filter { it.id in ids }
            selected.isNotEmpty() && selected.all { it.isPinned }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleSelection(noteId: String) {
        _selectedIds.value = _selectedIds.value.let { ids ->
            if (noteId in ids) ids - noteId else ids + noteId
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun archiveSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            ids.forEach { repository.setArchived(it, true) }
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_archived, ids.size) {
                ids.forEach { repository.setArchived(it, false) }
            }
        }
    }

    /** Seçilenlerden en az biri sabit değilse hepsini sabitler; hepsi sabitse kaldırır. */
    fun togglePinSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        val selectedNotes = notes.value.filter { it.id in ids }
        clearSelection()
        viewModelScope.launch {
            val shouldPin = selectedNotes.any { !it.isPinned }
            selectedNotes.forEach { repository.setPinned(it.id, shouldPin) }
        }
    }

    // --- Toplu etiketleme ---

    private val _labelPickerOpen = MutableStateFlow(false)
    val labelPickerOpen: StateFlow<Boolean> = _labelPickerOpen.asStateFlow()

    /**
     * Seçilen notların TAMAMINDA bulunan etiketlerin id'leri.
     * Kutu işaretliyse "hepsinde var" demek; dokunmak hepsinden kaldırır.
     * Kısmen varsa işaretsiz görünür ve dokunmak hepsine ekler.
     */
    val labelIdsOnAllSelected: StateFlow<Set<String>> =
        combine(_selectedIds, notes) { ids, allNotes ->
            val selected = allNotes.filter { it.id in ids }
            if (selected.isEmpty()) {
                emptySet()
            } else {
                selected
                    .map { note -> note.labels.map { it.id }.toSet() }
                    .reduce { acc, labels -> acc intersect labels }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Etiket seçici açılırken seçim korunur; kapanınca temizlenir. */
    fun openLabelPicker() {
        if (_selectedIds.value.isEmpty()) return
        _labelPickerOpen.value = true
    }

    fun closeLabelPicker() {
        _labelPickerOpen.value = false
        clearSelection()
    }

    fun toggleLabelForSelection(labelId: String) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        val assigned = labelId !in labelIdsOnAllSelected.value
        viewModelScope.launch {
            repository.setLabelForNotes(ids, labelId, assigned)
        }
    }

    fun duplicateSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch { ids.forEach { duplicateById(it) } }
    }

    fun shareSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        val text = notes.value.filter { it.id in ids }.toShareText()
        clearSelection()
        if (text.isNotBlank()) shareService.shareText(text)
    }

    fun trashSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            ids.forEach { repository.moveToTrash(it) }
            // Çöpteki not için bildirim çalmasın; geri alınırsa yeniden kurulur.
            reminders.cancel(ids)
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_trashed, ids.size) {
                ids.forEach { repository.restoreFromTrash(it) }
                reminders.reschedule(ids)
            }
        }
    }

    /** Etiket filtresi şeridinin görünürlüğü; yalnızca Ayarlar'dan değiştirilir. */
    val labelFiltersVisible: StateFlow<Boolean> = settings.labelFiltersVisible

    private val _isGridView = MutableStateFlow(
        preferences.getBoolean(PreferencesStore.KEY_GRID_VIEW, default = true),
    )
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleViewMode() {
        val newValue = !_isGridView.value
        _isGridView.value = newValue
        preferences.putBoolean(PreferencesStore.KEY_GRID_VIEW, newValue)
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    }

    /**
     * Notun tam bir kopyasını oluşturur. Hatırlatıcı ve sync bilgileri
     * kopyalanmaz (çift bildirim/uzak kayıt çakışması olmasın diye).
     */
    private suspend fun duplicateById(noteId: String) {
        val full = repository.getNoteById(noteId) ?: return
        val now = currentTimeMillis()
        repository.upsertNote(
            full.copy(
                id = generateId(),
                checklistItems = full.checklistItems.map { it.copy(id = generateId()) },
                isPinned = false,
                reminderAt = null,
                remoteId = null,
                syncStatus = SyncStatus.LOCAL_ONLY,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    fun archive(note: Note) {
        viewModelScope.launch {
            repository.setArchived(note.id, true)
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_archived, 1) {
                repository.setArchived(note.id, false)
            }
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            repository.moveToTrash(note.id)
            reminders.cancel(listOf(note.id))
            _undoEvent.value = UndoEvent(currentTimeMillis(), Res.string.undo_trashed, 1) {
                repository.restoreFromTrash(note.id)
                reminders.reschedule(listOf(note.id))
            }
        }
    }

    fun performUndo(event: UndoEvent) {
        viewModelScope.launch { event.undo() }
        dismissUndo(event)
    }

    /** Snackbar kapandığında, bu arada yeni bir olay geldiyse onu ezmemek için karşılaştırmalı temizler. */
    fun dismissUndo(event: UndoEvent) {
        _undoEvent.compareAndSet(event, null)
    }

    /**
     * Sabitlenmiş notlar her sıralamada üstte kalır.
     *
     * Sonda `id` ile eşitlik bozulur: aynı milisaniyede oluşturulan/düzenlenen
     * iki not için sıra belirsiz kalırsa, veritabanı her sorguda farklı bir sıra
     * döndürüp kartların yer değiştirmesine yol açıyor.
     */
    private fun SortOption.comparator(): Comparator<Note> {
        val pinnedFirst = compareByDescending<Note> { it.isPinned }
        return when (this) {
            SortOption.UPDATED -> pinnedFirst.thenByDescending { it.updatedAt }
            SortOption.CREATED -> pinnedFirst.thenByDescending { it.createdAt }
            SortOption.COLOR -> pinnedFirst.thenBy { it.color.ordinal }.thenByDescending { it.updatedAt }
        }.thenBy { it.id }
    }
}
