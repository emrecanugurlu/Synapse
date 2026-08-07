package com.cnsoftware.synapse.domain

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<Note>>
    fun observeArchivedNotes(): Flow<List<Note>>
    fun observeTrashedNotes(): Flow<List<Note>>
    fun searchActiveNotes(query: String): Flow<List<Note>>
    fun searchArchivedNotes(query: String): Flow<List<Note>>
    fun searchTrashedNotes(query: String): Flow<List<Note>>
    fun observeActiveNotesByLabel(labelId: String): Flow<List<Note>>

    fun observeLabels(): Flow<List<Label>>
    suspend fun createLabel(
        name: String,
        color: NoteColor = NoteColor.DEFAULT,
        icon: LabelIcon = LabelIcon.LABEL,
    ): Label
    suspend fun deleteLabel(id: String)
    suspend fun renameLabel(id: String, name: String)

    /** Ad, renk ve ikonu birlikte günceller. */
    suspend fun updateLabel(id: String, name: String, color: NoteColor, icon: LabelIcon)

    /** Kaynak etiketin tüm notlarını hedefe taşır ve kaynağı siler. */
    suspend fun mergeLabels(sourceId: String, targetId: String)

    /** Toplu etiketleme: verilen notlara etiketi ekler ([assigned] true) veya kaldırır. */
    suspend fun setLabelForNotes(noteIds: List<String>, labelId: String, assigned: Boolean)

    /** Çöp kutusundaki tüm notları kalıcı olarak siler. */
    suspend fun emptyTrash()

    suspend fun getNoteById(id: String): Note?
    suspend fun upsertNote(note: Note)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun moveToTrash(id: String)
    suspend fun restoreFromTrash(id: String)
    suspend fun deletePermanently(id: String)

    /** Verilen zaman damgasından önce çöpe atılmış notları kalıcı olarak siler. */
    suspend fun purgeTrashedBefore(timestampMillis: Long)

    /** Zamanı henüz gelmemiş hatırlatıcısı olan notlar (boot sonrası alarmları yeniden kurmak için). */
    suspend fun getNotesWithUpcomingReminders(afterMillis: Long): List<Note>
}
