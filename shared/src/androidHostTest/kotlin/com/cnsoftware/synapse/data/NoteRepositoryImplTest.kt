package com.cnsoftware.synapse.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cnsoftware.synapse.db.NoteDatabase
import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepositoryImplTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var driver: SqlDriver
    private lateinit var repository: NoteRepositoryImpl

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoteDatabase.Schema.create(driver)
        repository = NoteRepositoryImpl(NoteDatabase(driver), dispatcher)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun note(
        id: String,
        title: String = "Başlık $id",
        content: String = "İçerik $id",
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        isDeleted: Boolean = false,
        isChecklist: Boolean = false,
        checklistItems: List<ChecklistItem> = emptyList(),
        updatedAt: Long = 1_000L,
        reminderAt: Long? = null,
    ) = Note(
        id = id,
        title = title,
        content = content,
        color = NoteColor.DEFAULT,
        isChecklist = isChecklist,
        checklistItems = checklistItems,
        isPinned = isPinned,
        isArchived = isArchived,
        isDeleted = isDeleted,
        createdAt = 1_000L,
        updatedAt = updatedAt,
        reminderAt = reminderAt,
    )

    @Test
    fun upsertAndReadBackRoundtrip() = runTest(dispatcher) {
        val label = repository.createLabel("iş")
        repository.upsertNote(
            note(
                id = "n1",
                isChecklist = true,
                checklistItems = listOf(
                    ChecklistItem(id = "c1", text = "süt al", isChecked = true, position = 0),
                    ChecklistItem(id = "c2", text = "ekmek al", isChecked = false, position = 1),
                ),
            ).copy(labels = listOf(label)),
        )

        val loaded = repository.getNoteById("n1")
        assertEquals("Başlık n1", loaded?.title)
        assertEquals(listOf("süt al", "ekmek al"), loaded?.checklistItems?.map { it.text })
        assertEquals(listOf("iş"), loaded?.labels?.map { it.name })
    }

    @Test
    fun activeNotesExcludeArchivedAndTrashedAndPinnedComesFirst() = runTest(dispatcher) {
        repository.upsertNote(note("eski", updatedAt = 1))
        repository.upsertNote(note("yeni", updatedAt = 2))
        repository.upsertNote(note("sabit", isPinned = true, updatedAt = 1))
        repository.upsertNote(note("arsiv", isArchived = true))
        repository.upsertNote(note("cop", isDeleted = true))

        val active = repository.observeActiveNotes().first()
        assertEquals(listOf("sabit", "yeni", "eski"), active.map { it.id })
    }

    @Test
    fun archiveTrashRestoreTransitions() = runTest(dispatcher) {
        repository.upsertNote(note("n1"))

        repository.setArchived("n1", true)
        assertEquals(listOf("n1"), repository.observeArchivedNotes().first().map { it.id })
        assertTrue(repository.observeActiveNotes().first().isEmpty())

        repository.moveToTrash("n1")
        assertEquals(listOf("n1"), repository.observeTrashedNotes().first().map { it.id })

        repository.restoreFromTrash("n1")
        assertTrue(repository.observeTrashedNotes().first().isEmpty())
        // Arşiv bayrağı çöpe gidip dönünce korunmalı.
        assertEquals(listOf("n1"), repository.observeArchivedNotes().first().map { it.id })

        repository.deletePermanently("n1")
        assertNull(repository.getNoteById("n1"))
    }

    @Test
    fun searchMatchesTitleAndContentInEachScope() = runTest(dispatcher) {
        repository.upsertNote(note("aktif", title = "Market listesi"))
        repository.upsertNote(note("aktif2", content = "markete uğra"))
        repository.upsertNote(note("arsivli", title = "Market notu", isArchived = true))
        repository.upsertNote(note("copte", title = "Market eski", isDeleted = true))

        assertEquals(
            setOf("aktif", "aktif2"),
            repository.searchActiveNotes("arket").first().map { it.id }.toSet(),
        )
        assertEquals(listOf("arsivli"), repository.searchArchivedNotes("Market").first().map { it.id })
        assertEquals(listOf("copte"), repository.searchTrashedNotes("Market").first().map { it.id })
    }

    @Test
    fun purgeRemovesOnlyOldTrashedNotes() = runTest(dispatcher) {
        repository.upsertNote(note("eskiCop", isDeleted = true, updatedAt = 100))
        repository.upsertNote(note("yeniCop", isDeleted = true, updatedAt = 900))
        repository.upsertNote(note("aktif", updatedAt = 100))

        repository.purgeTrashedBefore(500)

        assertEquals(listOf("yeniCop"), repository.observeTrashedNotes().first().map { it.id })
        assertEquals(listOf("aktif"), repository.observeActiveNotes().first().map { it.id })
    }

    @Test
    fun upcomingRemindersExcludePastAndDeleted() = runTest(dispatcher) {
        repository.upsertNote(note("gelecek", reminderAt = 2_000))
        repository.upsertNote(note("gecmis", reminderAt = 500))
        repository.upsertNote(note("remindersiz"))
        repository.upsertNote(note("silinmis", reminderAt = 3_000, isDeleted = true))

        val upcoming = repository.getNotesWithUpcomingReminders(afterMillis = 1_000)
        assertEquals(listOf("gelecek"), upcoming.map { it.id })
    }

    @Test
    fun emptyTrashRemovesAllTrashedNotesOnly() = runTest(dispatcher) {
        repository.upsertNote(note("cop1", isDeleted = true))
        repository.upsertNote(note("cop2", isDeleted = true))
        repository.upsertNote(note("aktif"))

        repository.emptyTrash()

        assertTrue(repository.observeTrashedNotes().first().isEmpty())
        assertEquals(listOf("aktif"), repository.observeActiveNotes().first().map { it.id })
    }

    @Test
    fun renameLabelUpdatesNameOnNotes() = runTest(dispatcher) {
        val label = repository.createLabel("is")
        repository.upsertNote(note("n1").copy(labels = listOf(label)))

        repository.renameLabel(label.id, "iş")

        assertEquals(listOf("iş"), repository.observeLabels().first().map { it.name })
        assertEquals(listOf("iş"), repository.getNoteById("n1")!!.labels.map { it.name })
    }

    @Test
    fun mergeLabelsMovesNotesAndDeletesSource() = runTest(dispatcher) {
        val source = repository.createLabel("işler")
        val target = repository.createLabel("iş")
        repository.upsertNote(note("n1").copy(labels = listOf(source)))
        // Zaten her iki etikete de sahip olan not çift kayıt üretmemeli.
        repository.upsertNote(note("n2").copy(labels = listOf(source, target)))

        repository.mergeLabels(sourceId = source.id, targetId = target.id)

        assertEquals(listOf("iş"), repository.observeLabels().first().map { it.name })
        assertEquals(listOf("iş"), repository.getNoteById("n1")!!.labels.map { it.name })
        assertEquals(listOf("iş"), repository.getNoteById("n2")!!.labels.map { it.name })
        assertEquals(
            setOf("n1", "n2"),
            repository.observeActiveNotesByLabel(target.id).first().map { it.id }.toSet(),
        )
    }

    @Test
    fun labelFilterAndDeletion() = runTest(dispatcher) {
        val work = repository.createLabel("iş")
        val home = repository.createLabel("ev")
        repository.upsertNote(note("n1").copy(labels = listOf(work)))
        repository.upsertNote(note("n2").copy(labels = listOf(home)))

        assertEquals(setOf("ev", "iş"), repository.observeLabels().first().map { it.name }.toSet())
        assertEquals(listOf("n1"), repository.observeActiveNotesByLabel(work.id).first().map { it.id })

        repository.deleteLabel(work.id)
        assertEquals(listOf("ev"), repository.observeLabels().first().map { it.name })
        assertTrue(repository.getNoteById("n1")!!.labels.isEmpty())
    }
}
