package com.cnsoftware.synapse.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.domain.SyncStatus
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.reminder.ReminderScheduler
import com.cnsoftware.synapse.reminder.reminderMessage
import com.cnsoftware.synapse.share.ShareService
import com.cnsoftware.synapse.share.formatNoteForShare
import com.cnsoftware.synapse.util.currentTimeMillis
import com.cnsoftware.synapse.util.generateId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val color: NoteColor = NoteColor.DEFAULT,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val labels: List<Label> = emptyList(),
    val reminderAt: Long? = null,
    val isArchived: Boolean = false,
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
)

/**
 * Not editörü. Faz 3: checklist modu + 600ms debounce ile arka planda otomatik kaydetme.
 * `save()` ise ekrandan çıkarken (geri tuşu) beklemeden anında kaydeder.
 *
 * isPinned/isArchived/isDeleted/remoteId/syncStatus alanları UI'da düzenlenmiyor ama
 * kaydederken korunuyor -- aksi halde arşiv/çöp kutusundan açılan bir not,
 * otomatik kaydetmede yanlışlıkla "aktif" durumuna dönerdi.
 */
class NoteEditorViewModel(
    private val noteId: String?,
    private val repository: NoteRepository,
    private val reminderScheduler: ReminderScheduler,
    private val shareService: ShareService,
    private val settings: AppSettings,
    /** Yeni not doğrudan liste modunda açılsın mı (ana ekrandaki "Yeni liste" kısayolu). */
    startAsChecklist: Boolean = false,
    /**
     * Dışarıdan gelen metinle açılan yeni not (başka uygulamadan paylaşma).
     * Var olan bir not açılırken yok sayılır.
     */
    initialTitle: String = "",
    initialContent: String = "",
    /**
     * Yeni notun alacağı id. Gezinme durumu bu id'yi sakladığı için, süreç
     * öldürülüp geri yüklendiğinde editör aynı nota bağlanır; rastgele bir id
     * üretilseydi daha önce kaydedilmiş notun yanına ikinci bir kopya yazılırdı.
     */
    private val newNoteId: String? = null,
) : ViewModel() {

    private val firstChecklistItemId = generateId()

    private val _uiState = MutableStateFlow(
        NoteEditorUiState(
            id = noteId ?: newNoteId ?: generateId(),
            isNew = noteId == null,
            isLoading = noteId != null,
            title = if (noteId == null) initialTitle else "",
            content = if (noteId == null) initialContent else "",
            // Yeni not liste olarak açılıyorsa boş bir madde ile başla.
            isChecklist = noteId == null && startAsChecklist,
            checklistItems = if (noteId == null && startAsChecklist) {
                listOf(ChecklistItem(id = firstChecklistItemId, text = "", isChecked = false, position = 0))
            } else {
                emptyList()
            },
        ),
    )
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    /**
     * "Son düzenleme" göstergesi.
     *
     * Bilerek [NoteEditorUiState] dışında tutuluyor: kaydetme sonrası bu değeri
     * uiState'e yazmak, otomatik kaydetmeyi tetikleyen akışı yeniden ateşleyip
     * 600ms'de bir kendini tekrar eden sonsuz bir kaydetme döngüsü yaratıyordu.
     */
    private val _lastSavedAt = MutableStateFlow(0L)
    val lastSavedAt: StateFlow<Long> = _lastSavedAt.asStateFlow()

    /** En son kaydedilen içerik; "kaydedilmemiş değişiklik var mı" bunun üzerinden bulunur. */
    private val _savedSnapshot = MutableStateFlow<NoteEditorUiState?>(null)

    val autoSaveEnabled: StateFlow<Boolean> = settings.autoSaveEnabled

    /** Otomatik kaydetme kapalıyken Kaydet düğmesini ve çıkış uyarısını yönetir. */
    // Eagerly: abone olunmasa da güncel kalmalı — Kaydet düğmesinin durumu
    // ekran ilk çizildiğinde de doğru olmalı.
    val hasUnsavedChanges: StateFlow<Boolean> =
        combine(_uiState, _savedSnapshot) { state, saved ->
            !state.isLoading && state.hasContent() && state != saved
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Etiket seçici diyaloğu için tüm mevcut etiketler. */
    val allLabels: StateFlow<List<Label>> = repository.observeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var createdAt: Long = currentTimeMillis()
    private var isPinned: Boolean = false
    private var isArchivedFlag: Boolean = false
    private var isDeletedFlag: Boolean = false
    private var remoteId: String? = null
    private var syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val note = repository.getNoteById(noteId)
                if (note != null) {
                    applyLoaded(note)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    // Yüklenen hâli "kaydedilmiş" say: kullanıcı dokunmadan
                    // "kaydedilmemiş değişiklik var" uyarısı çıkmasın.
                    _savedSnapshot.value = _uiState.value
                }
            }
        } else if (newNoteId != null) {
            // Yalnızca süreç ölümünden sonraki geri yüklemede bir şey bulur:
            // bu id ile daha önce kaydedilmiş not varsa kaldığı yerden devam et.
            val initial = _uiState.value
            viewModelScope.launch {
                val note = repository.getNoteById(newNoteId) ?: return@launch
                // Kullanıcı bu arada yazmaya başladıysa onun yazdığı kalsın.
                if (_uiState.value == initial) applyLoaded(note)
            }
        }

        // Arka planda otomatik kaydetme: her değişiklikten 600ms sonra.
        // Ayarlardan kapatılmışsa hiçbir şey yazılmaz; kullanıcı Kaydet'e basar.
        uiState
            .drop(1)
            .debounce(600)
            .onEach { state -> if (settings.autoSaveEnabled.value) persist(state) }
            .launchIn(viewModelScope)
    }

    /** Veritabanındaki notu editör durumuna ve korunan bayraklara yazar. */
    private fun applyLoaded(note: Note) {
        _lastSavedAt.value = note.updatedAt
        createdAt = note.createdAt
        isPinned = note.isPinned
        isArchivedFlag = note.isArchived
        isDeletedFlag = note.isDeleted
        remoteId = note.remoteId
        syncStatus = note.syncStatus
        _uiState.update {
            it.copy(
                id = note.id,
                title = note.title,
                content = note.content,
                color = note.color,
                isChecklist = note.isChecklist,
                checklistItems = note.checklistItems,
                labels = note.labels,
                reminderAt = note.reminderAt,
                isArchived = note.isArchived,
                isNew = false,
                isLoading = false,
            )
        }
        // Yüklenen hâli "kaydedilmiş" say: kullanıcı dokunmadan
        // "kaydedilmemiş değişiklik var" uyarısı çıkmasın.
        _savedSnapshot.value = _uiState.value
    }

    /**
     * Editör ekranı her açıldığında çağrılır.
     *
     * ViewModel not id'siyle Activity ömrü boyunca önbellekte kalıyor; aynı not
     * ikinci kez açıldığında `init` yeniden çalışmıyor. Arada listeden yapılan
     * sabitleme, arşivleme, çöpe atma, etiketleme veya yedekten geri yükleme
     * burada yeniden okunmazsa, sonraki kayıt bunların hepsini eski değerlerle ezer.
     *
     * Kaydedilmemiş değişiklik varsa dokunulmaz: bu yalnızca ekran döndürme gibi
     * editörden hiç çıkılmayan durumlarda olur ve yazılan metin korunmalıdır.
     */
    fun onOpened() {
        val state = _uiState.value
        if (state.isLoading || state.isNew) return
        if (state != _savedSnapshot.value) return
        viewModelScope.launch {
            val note = repository.getNoteById(state.id) ?: return@launch
            // Okuma sürerken yazılmaya başlandıysa kullanıcının yazdığı kalsın.
            if (_uiState.value == _savedSnapshot.value) applyLoaded(note)
        }
    }

    /**
     * Kaydedilmemiş değişiklikleri atar (otomatik kaydetme kapalıyken "Vazgeç").
     * ViewModel önbellekte kaldığı için, bu yapılmazsa atılan metin not bir
     * sonraki açılışta geri gelir.
     */
    fun discardChanges() {
        _savedSnapshot.value?.let { saved -> _uiState.value = saved }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun onColorChange(color: NoteColor) {
        _uiState.update { it.copy(color = color) }
    }

    /**
     * Metin <-> checklist dönüşümü içerik kaybetmez:
     * metinden checkliste geçerken her satır bir öğe olur,
     * checklistten metne dönerken her öğe bir satıra yazılır.
     */
    fun toggleChecklistMode() {
        _uiState.update { state ->
            if (!state.isChecklist) {
                val lines = state.content.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val items = if (lines.isEmpty()) {
                    listOf(ChecklistItem(id = generateId(), text = "", isChecked = false, position = 0))
                } else {
                    lines.mapIndexed { index, line ->
                        ChecklistItem(id = generateId(), text = line, isChecked = false, position = index)
                    }
                }
                state.copy(isChecklist = true, checklistItems = items, content = "")
            } else {
                val text = state.checklistItems
                    .filter { it.text.isNotBlank() }
                    .joinToString(separator = "\n") { it.text }
                state.copy(isChecklist = false, content = text, checklistItems = emptyList())
            }
        }
    }

    /**
     * Klavyeden yeni öğeye geçiş için: eklenen öğenin odaklanması gerektiğini UI'a bildirir.
     * UI odağı verdikten sonra [onFocusHandled] ile temizler.
     */
    private val _pendingFocusItemId = MutableStateFlow<String?>(
        // Liste olarak açıldıysa imleç ilk maddede olsun.
        if (noteId == null && startAsChecklist) firstChecklistItemId else null,
    )
    val pendingFocusItemId: StateFlow<String?> = _pendingFocusItemId.asStateFlow()

    fun onFocusHandled() {
        _pendingFocusItemId.value = null
    }

    fun addChecklistItem() = addChecklistItemAfter(afterItemId = null)

    /**
     * [afterItemId] verilirse o öğenin hemen ardına, verilmezse listenin sonuna ekler.
     * Enter'a basıldığında araya öğe eklemek için kullanılır.
     */
    fun addChecklistItemAfter(afterItemId: String?) {
        val newItem = ChecklistItem(id = generateId(), text = "", isChecked = false, position = 0)
        _uiState.update { state ->
            val index = afterItemId
                ?.let { id -> state.checklistItems.indexOfFirst { it.id == id } }
                ?.takeIf { it >= 0 }
                ?.plus(1)
                ?: state.checklistItems.size
            val items = state.checklistItems.toMutableList().apply { add(index, newItem) }
            state.copy(checklistItems = items.reindexed())
        }
        _pendingFocusItemId.value = newItem.id
    }

    /**
     * Boş bir öğede geri silme: öğeyi kaldırır ve odağı bir öncekine taşır.
     * Listede tek öğe kalmışsa dokunmaz.
     */
    fun onChecklistBackspaceOnEmpty(itemId: String) {
        val state = _uiState.value
        if (state.checklistItems.size <= 1) return
        val index = state.checklistItems.indexOfFirst { it.id == itemId }
        if (index <= 0) return
        val previousId = state.checklistItems[index - 1].id
        _uiState.update {
            it.copy(checklistItems = it.checklistItems.filterNot { item -> item.id == itemId }.reindexed())
        }
        _pendingFocusItemId.value = previousId
    }

    private fun List<ChecklistItem>.reindexed(): List<ChecklistItem> =
        mapIndexed { index, item -> if (item.position == index) item else item.copy(position = index) }

    fun onChecklistItemTextChange(itemId: String, text: String) {
        _uiState.update { state ->
            state.copy(
                checklistItems = state.checklistItems.map {
                    if (it.id == itemId) it.copy(text = text) else it
                },
            )
        }
    }

    /**
     * İşaretlenen öğe listenin sonuna, işareti kaldırılan ise tamamlanmamışların
     * sonuna taşınır. Böylece veri sırası ekrandaki sırayla birebir aynı kalır —
     * sürükleyerek sıralamanın doğru çalışması buna bağlı.
     */
    fun toggleChecklistItemChecked(itemId: String) {
        _uiState.update { state ->
            val items = state.checklistItems.toMutableList()
            val index = items.indexOfFirst { it.id == itemId }
            if (index < 0) return@update state
            val toggled = items.removeAt(index).let { it.copy(isChecked = !it.isChecked) }
            if (toggled.isChecked) {
                items.add(toggled)
            } else {
                items.add(items.count { !it.isChecked }, toggled)
            }
            state.copy(checklistItems = items.reindexed())
        }
    }

    /** Sürükle-bırak ile yeniden sıralama: öğeyi [targetIndex] konumuna taşır. */
    fun moveChecklistItem(itemId: String, targetIndex: Int) {
        _uiState.update { state ->
            val items = state.checklistItems.toMutableList()
            val from = items.indexOfFirst { it.id == itemId }
            if (from < 0) return@update state
            val to = targetIndex.coerceIn(0, items.lastIndex)
            if (from == to) return@update state
            items.add(to, items.removeAt(from))
            state.copy(checklistItems = items.reindexed())
        }
    }

    fun removeChecklistItem(itemId: String) {
        _uiState.update { state ->
            state.copy(checklistItems = state.checklistItems.filterNot { it.id == itemId }.reindexed())
        }
    }

    /** Tamamlanmış öğeleri listeden siler; bekleyenler sırasını korur. */
    fun clearCompletedChecklistItems() {
        _uiState.update { state ->
            state.copy(checklistItems = state.checklistItems.filterNot { it.isChecked }.reindexed())
        }
    }

    /**
     * Tümünü işaretler; zaten hepsi işaretliyse işaretleri kaldırır.
     * Tek bir düğmenin iki yönlü çalışması için.
     */
    fun toggleAllChecklistItems() {
        _uiState.update { state ->
            if (state.checklistItems.isEmpty()) return@update state
            val allChecked = state.checklistItems.all { it.isChecked }
            val updated = state.checklistItems.map { it.copy(isChecked = !allChecked) }
            // İşaretlenenler listenin sonuna gider; hepsi aynı duruma geçtiği
            // için mevcut sıra korunuyor, yeniden düzenlemeye gerek yok.
            state.copy(checklistItems = updated.reindexed())
        }
    }

    fun toggleLabel(label: Label) {
        _uiState.update { state ->
            val labels = if (state.labels.any { it.id == label.id }) {
                state.labels.filterNot { it.id == label.id }
            } else {
                state.labels + label
            }
            state.copy(labels = labels)
        }
    }

    fun share() {
        val state = _uiState.value
        val text = formatNoteForShare(state.title, state.content, state.isChecklist, state.checklistItems)
        if (text.isNotBlank()) shareService.shareText(text)
    }

    fun setReminder(triggerAtMillis: Long) {
        _uiState.update { it.copy(reminderAt = triggerAtMillis) }
        val state = _uiState.value
        reminderScheduler.schedule(
            noteId = state.id,
            title = state.title,
            message = reminderMessage(state.isChecklist, state.content, state.checklistItems),
            triggerAtMillis = triggerAtMillis,
        )
    }

    fun clearReminder() {
        reminderScheduler.cancel(_uiState.value.id)
        _uiState.update { it.copy(reminderAt = null) }
    }

    fun createAndAttachLabel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            // Aynı isimde etiket varsa onu kullan (Label.name UNIQUE).
            val existing = allLabels.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            val label = existing ?: repository.createLabel(trimmed)
            _uiState.update { state ->
                if (state.labels.any { it.id == label.id }) state else state.copy(labels = state.labels + label)
            }
        }
    }

    /** Ekrandan çıkarken (geri tuşu) çağrılır; debounce'u beklemeden anında kaydeder. */
    fun save() {
        viewModelScope.launch { persist(_uiState.value) }
    }

    fun delete() {
        if (_uiState.value.isNew) return
        val id = _uiState.value.id
        reminderScheduler.cancel(id)
        // Bayrak da güncellenmeli: bekleyen bir otomatik kayıt notu
        // isDeleted = false ile yeniden yazıp çöpten geri getirirdi.
        isDeletedFlag = true
        // Silinen nota ait kaydedilmemiş son değişiklikler atılır; böylece
        // bekleyen otomatik kayıt da yazacak bir şey bulamaz.
        discardChanges()
        viewModelScope.launch { repository.moveToTrash(id) }
    }

    /** Notu arşive alır ya da arşivden çıkarır; ekrandan çıkılmadan da kaydedilir. */
    fun toggleArchived() {
        val archived = !_uiState.value.isArchived
        isArchivedFlag = archived
        _uiState.update { it.copy(isArchived = archived) }
        viewModelScope.launch { persist(_uiState.value) }
    }

    private suspend fun persist(state: NoteEditorUiState) {
        if (state.isLoading) return
        if (!state.hasContent()) return
        // Değişiklik yoksa yazma: aksi halde notu yalnızca açıp kapatmak
        // "son düzenleme" zamanını değiştirip notu listenin başına taşıyor,
        // çöpteki notta ise kalıcı silme süresini sıfırlıyordu.
        if (state == _savedSnapshot.value) return

        val savedAt = currentTimeMillis()
        repository.upsertNote(
            Note(
                id = state.id,
                title = state.title,
                content = if (state.isChecklist) "" else state.content,
                color = state.color,
                isChecklist = state.isChecklist,
                checklistItems = if (state.isChecklist) state.checklistItems else emptyList(),
                labels = state.labels,
                reminderAt = state.reminderAt,
                isPinned = isPinned,
                isArchived = isArchivedFlag,
                isDeleted = isDeletedFlag,
                createdAt = createdAt,
                updatedAt = savedAt,
                remoteId = remoteId,
                syncStatus = syncStatus,
            ),
        )
        // "Son düzenleme" bilgisi kaydedilen değeri yansıtsın.
        // uiState'e YAZILMIYOR: orası otomatik kaydetmeyi tetikleyen akış.
        _lastSavedAt.value = savedAt
        _savedSnapshot.value = state
    }
}

/** Kaydedilmeye değer bir içerik var mı (başlık ya da gövde/madde). */
private fun NoteEditorUiState.hasContent(): Boolean {
    val body = if (isChecklist) {
        checklistItems.any { it.text.isNotBlank() }
    } else {
        content.isNotBlank()
    }
    return title.isNotBlank() || body
}
