package com.cnsoftware.synapse.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.cnsoftware.synapse.db.NoteDatabase
import com.cnsoftware.synapse.db.ChecklistItem as ChecklistItemEntity
import com.cnsoftware.synapse.db.Note as NoteEntity
import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Label
import com.cnsoftware.synapse.domain.LabelIcon
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.domain.SyncStatus
import com.cnsoftware.synapse.util.currentTimeMillis
import com.cnsoftware.synapse.util.generateId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight tabanlı, tamamen local NoteRepository implementasyonu.
 * İleride buluta taşınırken bu sınıfın yerine (ya da bunu saran) bir
 * senkronize repository eklenecek; NoteRepository interface'i sayesinde
 * UI katmanı bu değişimden etkilenmeyecek.
 */
class NoteRepositoryImpl(
    database: NoteDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : NoteRepository {

    private val queries = database.noteQueries
    private val labelQueries = database.labelQueries

    override fun observeActiveNotes(): Flow<List<Note>> =
        queries.selectActive()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.toDomainWithChecklistItems() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeArchivedNotes(): Flow<List<Note>> =
        queries.selectArchived()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.toDomainWithChecklistItems() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeTrashedNotes(): Flow<List<Note>> =
        queries.selectTrashed()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.toDomainWithChecklistItems() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun searchActiveNotes(query: String): Flow<List<Note>> =
        observeActiveNotes().filteredBy(query)

    override fun searchArchivedNotes(query: String): Flow<List<Note>> =
        observeArchivedNotes().filteredBy(query)

    override fun searchTrashedNotes(query: String): Flow<List<Note>> =
        observeTrashedNotes().filteredBy(query)

    /**
     * Arama bellekte yapılıyor (bkz. [foldForSearch]): liste ekranı zaten tüm
     * notları maddeleriyle yüklüyor, üzerine ek bir sorgu maliyeti gelmiyor.
     */
    private fun Flow<List<Note>>.filteredBy(query: String): Flow<List<Note>> {
        val folded = query.trim().foldForSearch()
        if (folded.isEmpty()) return this
        return map { notes -> notes.filter { it.matchesSearch(folded) } }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override fun observeActiveNotesByLabel(labelId: String): Flow<List<Note>> =
        labelQueries.selectActiveByLabel(labelId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.toDomainWithChecklistItems() }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    override fun observeLabels(): Flow<List<Label>> =
        labelQueries.selectAllLabels()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { entities -> entities.map { it.toDomainLabel() } }
            .flowOn(ioDispatcher)

    override suspend fun createLabel(
        name: String,
        color: NoteColor,
        icon: LabelIcon,
    ): Label = withContext(ioDispatcher) {
        val label = Label(id = generateId(), name = name.trim(), color = color, icon = icon)
        labelQueries.insertLabel(
            id = label.id,
            name = label.name,
            color = label.color.name,
            icon = label.icon.name,
        )
        label
    }

    override suspend fun deleteLabel(id: String) = withContext(ioDispatcher) {
        labelQueries.transaction {
            labelQueries.deleteCrossRefsForLabel(id)
            labelQueries.deleteLabel(id)
        }
    }

    override suspend fun renameLabel(id: String, name: String) = withContext(ioDispatcher) {
        labelQueries.renameLabel(name = name.trim(), id = id)
        Unit
    }

    override suspend fun updateLabel(
        id: String,
        name: String,
        color: NoteColor,
        icon: LabelIcon,
    ) = withContext(ioDispatcher) {
        labelQueries.updateLabel(
            name = name.trim(),
            color = color.name,
            icon = icon.name,
            id = id,
        )
        Unit
    }

    override suspend fun mergeLabels(sourceId: String, targetId: String) = withContext(ioDispatcher) {
        if (sourceId == targetId) return@withContext
        labelQueries.transaction {
            labelQueries.moveCrossRefsToLabel(targetLabelId = targetId, sourceLabelId = sourceId)
            labelQueries.deleteCrossRefsForLabel(sourceId)
            labelQueries.deleteLabel(sourceId)
        }
    }

    override suspend fun setLabelForNotes(
        noteIds: List<String>,
        labelId: String,
        assigned: Boolean,
    ) = withContext(ioDispatcher) {
        if (noteIds.isEmpty()) return@withContext
        val now = currentTimeMillis()
        labelQueries.transaction {
            noteIds.forEach { noteId ->
                if (assigned) {
                    labelQueries.insertCrossRef(noteId = noteId, labelId = labelId)
                } else {
                    labelQueries.deleteCrossRef(noteId = noteId, labelId = labelId)
                }
                // Liste sorguları yalnızca Note tablosunu dinliyor; bu olmadan
                // etiket değişikliği ekrana yansımıyor.
                queries.touchNote(updatedAt = now, id = noteId)
            }
        }
    }

    override suspend fun emptyTrash() = withContext(ioDispatcher) {
        queries.transaction {
            queries.emptyTrash()
            labelQueries.purgeOrphanCrossRefs()
            queries.purgeOrphanChecklistItems()
        }
    }

    override suspend fun getNoteById(id: String): Note? = withContext(ioDispatcher) {
        val entity = queries.selectById(id).executeAsOneOrNull() ?: return@withContext null
        val items = queries.selectItemsForNote(id).executeAsList().map { it.toDomainItem() }
        entity.toDomainNote(items, labelsForNote(id))
    }

    override suspend fun upsertNote(note: Note) = withContext(ioDispatcher) {
        queries.transaction {
            queries.insertOrReplaceNote(
                id = note.id,
                title = note.title,
                content = note.content,
                color = note.color.name,
                isChecklist = note.isChecklist,
                isPinned = note.isPinned,
                isArchived = note.isArchived,
                isDeleted = note.isDeleted,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                remoteId = note.remoteId,
                syncStatus = note.syncStatus.name,
                reminderAt = note.reminderAt,
            )
            queries.deleteItemsForNote(note.id)
            note.checklistItems.forEach { item ->
                queries.insertOrReplaceItem(
                    id = item.id,
                    noteId = note.id,
                    text = item.text,
                    isChecked = item.isChecked,
                    position = item.position.toLong(),
                )
            }
            labelQueries.deleteCrossRefsForNote(note.id)
            note.labels.forEach { label ->
                labelQueries.insertCrossRef(noteId = note.id, labelId = label.id)
            }
        }
    }

    override suspend fun setPinned(id: String, pinned: Boolean) = withContext(ioDispatcher) {
        queries.setPinned(isPinned = pinned, updatedAt = currentTimeMillis(), id = id)
        Unit
    }

    override suspend fun setArchived(id: String, archived: Boolean) = withContext(ioDispatcher) {
        queries.setArchived(isArchived = archived, updatedAt = currentTimeMillis(), id = id)
        Unit
    }

    override suspend fun moveToTrash(id: String) = withContext(ioDispatcher) {
        queries.moveToTrash(updatedAt = currentTimeMillis(), id = id)
        Unit
    }

    override suspend fun restoreFromTrash(id: String) = withContext(ioDispatcher) {
        queries.restoreFromTrash(updatedAt = currentTimeMillis(), id = id)
        Unit
    }

    override suspend fun deletePermanently(id: String) = withContext(ioDispatcher) {
        queries.transaction {
            labelQueries.deleteCrossRefsForNote(id)
            // Foreign key denetimi açık değil; maddeler elle silinmezse kalıyor.
            queries.deleteItemsForNote(id)
            queries.deleteNotePermanently(id)
        }
    }

    override suspend fun purgeTrashedBefore(timestampMillis: Long) = withContext(ioDispatcher) {
        queries.transaction {
            queries.purgeTrashedBefore(timestampMillis)
            labelQueries.purgeOrphanCrossRefs()
            queries.purgeOrphanChecklistItems()
        }
    }

    override suspend fun getAllNotes(): List<Note> = withContext(ioDispatcher) {
        queries.selectAllNotes().executeAsList().toDomainWithChecklistItems()
    }

    override suspend fun getAllLabels(): List<Label> = withContext(ioDispatcher) {
        labelQueries.selectAllLabels().executeAsList().map { it.toDomainLabel() }
    }

    override suspend fun mergeBackup(
        notes: List<Note>,
        labels: List<Label>,
    ) = withContext(ioDispatcher) {
        // Tek transaction: yarıda kesilirse kısmi bir durum kalmasın.
        queries.transaction {
            // Etiket adı UNIQUE. Yedekteki etiketle aynı adda ama farklı id'li
            // bir etiket zaten varsa yedekteki eklenemez; notlar yedekteki id'ye
            // bağlanırsa var olmayan bir etikete işaret eder ve etiketsiz görünür.
            // Bu yüzden yedekteki id, aynı adlı mevcut etiketin id'sine çevriliyor.
            val idByName = labelQueries.selectAllLabels().executeAsList()
                .associateTo(mutableMapOf()) { it.name to it.id }
            val resolvedLabelIds = mutableMapOf<String, String>()
            labels.forEach { label ->
                val existingId = idByName[label.name]
                if (existingId != null && existingId != label.id) {
                    resolvedLabelIds[label.id] = existingId
                } else {
                    // Aynı id zaten varsa "INSERT OR IGNORE" mevcut kaydı korur.
                    labelQueries.insertLabel(
                        id = label.id,
                        name = label.name,
                        color = label.color.name,
                        icon = label.icon.name,
                    )
                    idByName[label.name] = label.id
                }
            }
            notes.forEach { note ->
                queries.insertOrReplaceNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    color = note.color.name,
                    isChecklist = note.isChecklist,
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    isDeleted = note.isDeleted,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    remoteId = note.remoteId,
                    syncStatus = note.syncStatus.name,
                    reminderAt = note.reminderAt,
                )
                queries.deleteItemsForNote(note.id)
                note.checklistItems.forEach { item ->
                    queries.insertOrReplaceItem(
                        id = item.id,
                        noteId = note.id,
                        text = item.text,
                        isChecked = item.isChecked,
                        position = item.position.toLong(),
                    )
                }
                labelQueries.deleteCrossRefsForNote(note.id)
                note.labels.forEach { label ->
                    labelQueries.insertCrossRef(
                        noteId = note.id,
                        labelId = resolvedLabelIds[label.id] ?: label.id,
                    )
                }
            }
        }
    }

    override suspend fun getNotesWithUpcomingReminders(afterMillis: Long): List<Note> =
        withContext(ioDispatcher) {
            queries.selectUpcomingReminders(afterMillis) { id, title, content, color, isChecklist, isPinned, isArchived, isDeleted, createdAt, updatedAt, remoteId, syncStatus, reminderAt ->
                NoteEntity(id, title, content, color, isChecklist, isPinned, isArchived, isDeleted, createdAt, updatedAt, remoteId, syncStatus, reminderAt)
            }.executeAsList().map { it.toDomainNote() }
        }

    override suspend fun getNotesWithDueReminders(afterMillis: Long, untilMillis: Long): List<Note> =
        withContext(ioDispatcher) {
            queries.selectDueReminders(afterMillis, untilMillis) { id, title, content, color, isChecklist, isPinned, isArchived, isDeleted, createdAt, updatedAt, remoteId, syncStatus, reminderAt ->
                NoteEntity(id, title, content, color, isChecklist, isPinned, isArchived, isDeleted, createdAt, updatedAt, remoteId, syncStatus, reminderAt)
            }.executeAsList().map { it.toDomainNote() }
        }

    /**
     * Liste ekranları için: checklist olmayan notlarda gereksiz sorgu yapmadan,
     * checklist notlarında ise madde önizlemesi gösterebilmek için item'ları
     * eager-load eder. Yerel SQLite için küçük not sayılarında (N+1) kabul edilebilir.
     */
    private fun List<NoteEntity>.toDomainWithChecklistItems(): List<Note> = map { entity ->
        val items = if (entity.isChecklist) {
            queries.selectItemsForNote(entity.id).executeAsList().map { it.toDomainItem() }
        } else {
            emptyList()
        }
        entity.toDomainNote(items, labelsForNote(entity.id))
    }

    private fun labelsForNote(noteId: String): List<Label> =
        labelQueries.selectLabelsForNote(noteId).executeAsList().map {
            Label(
                id = it.id,
                name = it.name,
                color = it.color.toNoteColor(),
                icon = it.icon.toLabelIcon(),
            )
        }

    /**
     * Enum'lar veritabaninda ADIYLA tutuluyor. Tanimadigimiz bir ad gelirse
     * (ileri surumden geri donus, bozuk kayit) cokmek yerine varsayilana duser.
     */
    private fun com.cnsoftware.synapse.db.Label.toDomainLabel() = Label(
        id = id,
        name = name,
        color = color.toNoteColor(),
        icon = icon.toLabelIcon(),
    )

    private fun String.toNoteColor(): NoteColor =
        NoteColor.entries.firstOrNull { it.name == this } ?: NoteColor.DEFAULT

    private fun String.toLabelIcon(): LabelIcon =
        LabelIcon.entries.firstOrNull { it.name == this } ?: LabelIcon.LABEL

    private fun NoteEntity.toDomainNote(
        items: List<ChecklistItem> = emptyList(),
        labels: List<Label> = emptyList(),
    ): Note = Note(
        id = id,
        title = title,
        content = content,
        color = runCatching { NoteColor.valueOf(color) }.getOrDefault(NoteColor.DEFAULT),
        isChecklist = isChecklist,
        checklistItems = items,
        labels = labels,
        isPinned = isPinned,
        isArchived = isArchived,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reminderAt = reminderAt,
        remoteId = remoteId,
        syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.LOCAL_ONLY),
    )

    private fun ChecklistItemEntity.toDomainItem(): ChecklistItem = ChecklistItem(
        id = id,
        text = text,
        isChecked = isChecked,
        position = position.toInt(),
    )
}
