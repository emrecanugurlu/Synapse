package com.cnsoftware.synapse.ui

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cnsoftware.synapse.data.NoteRepositoryImpl
import com.cnsoftware.synapse.db.NoteDatabase
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.ui.notelist.NoteListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var driver: SqlDriver
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var shareService: FakeShareService
    private lateinit var viewModel: NoteListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoteDatabase.Schema.create(driver)
        repository = NoteRepositoryImpl(NoteDatabase(driver), dispatcher)
        shareService = FakeShareService()
        val preferences = FakePreferencesStore()
        viewModel = NoteListViewModel(
            repository,
            shareService,
            preferences,
            AppSettings(preferences),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    private fun note(id: String, title: String, updatedAt: Long = 1_000L) = Note(
        id = id,
        title = title,
        content = "",
        color = NoteColor.DEFAULT,
        createdAt = 1_000L,
        updatedAt = updatedAt,
    )

    @Test
    fun searchFiltersNotes() = runTest(dispatcher) {
        repository.upsertNote(note("1", "Market listesi"))
        repository.upsertNote(note("2", "Toplantı notları"))

        val collector = launch { viewModel.notes.collect {} }
        advanceUntilIdle()
        assertEquals(2, viewModel.notes.value.size)

        viewModel.onSearchQueryChange("Market")
        advanceUntilIdle()
        assertEquals(listOf("1"), viewModel.notes.value.map { it.id })

        collector.cancel()
    }

    @Test
    fun bulkArchiveWithUndoRestoresNotes() = runTest(dispatcher) {
        repository.upsertNote(note("1", "Bir"))
        repository.upsertNote(note("2", "İki"))

        val collector = launch { viewModel.notes.collect {} }
        advanceUntilIdle()

        viewModel.toggleSelection("1")
        viewModel.toggleSelection("2")
        viewModel.archiveSelected()
        advanceUntilIdle()

        assertTrue(viewModel.selectedIds.value.isEmpty())
        assertTrue(viewModel.notes.value.isEmpty())

        val undo = viewModel.undoEvent.value
        assertNotNull(undo)
        viewModel.performUndo(undo)
        advanceUntilIdle()

        assertEquals(setOf("1", "2"), viewModel.notes.value.map { it.id }.toSet())
        collector.cancel()
    }

    @Test
    fun shareSelectedSendsCombinedTextAndClearsSelection() = runTest(dispatcher) {
        repository.upsertNote(note("1", "Bir"))
        repository.upsertNote(note("2", "İki"))

        val collector = launch { viewModel.notes.collect {} }
        advanceUntilIdle()

        viewModel.toggleSelection("1")
        viewModel.toggleSelection("2")
        viewModel.shareSelected()
        advanceUntilIdle()

        assertEquals(1, shareService.sharedTexts.size)
        assertTrue(shareService.sharedTexts.single().contains("Bir"))
        assertTrue(shareService.sharedTexts.single().contains("İki"))
        assertTrue(viewModel.selectedIds.value.isEmpty())
        collector.cancel()
    }
}
