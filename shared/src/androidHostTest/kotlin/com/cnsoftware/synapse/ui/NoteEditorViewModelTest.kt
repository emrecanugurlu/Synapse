package com.cnsoftware.synapse.ui

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cnsoftware.synapse.data.NoteRepositoryImpl
import com.cnsoftware.synapse.db.NoteDatabase
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteColor
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.reminder.ReminderNotice
import com.cnsoftware.synapse.ui.editor.NoteEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var driver: SqlDriver
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var reminderPermissions: FakeReminderPermissions
    private lateinit var shareService: FakeShareService
    private lateinit var settings: AppSettings

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoteDatabase.Schema.create(driver)
        repository = NoteRepositoryImpl(NoteDatabase(driver), dispatcher)
        reminderScheduler = FakeReminderScheduler()
        reminderPermissions = FakeReminderPermissions()
        shareService = FakeShareService()
        settings = AppSettings(FakePreferencesStore())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    private fun editorFor(
        noteId: String?,
        startAsChecklist: Boolean = false,
        newNoteId: String? = null,
    ) = NoteEditorViewModel(
        noteId = noteId,
        repository = repository,
        reminderScheduler = reminderScheduler,
        reminderPermissions = reminderPermissions,
        shareService = shareService,
        settings = settings,
        startAsChecklist = startAsChecklist,
        newNoteId = newNoteId,
    )

    private fun storedNote(id: String, title: String = "Başlık") = Note(
        id = id,
        title = title,
        content = "içerik",
        color = NoteColor.DEFAULT,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun settingReminderWithEverythingAllowedShowsNoNotice() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.setReminder(5_000L)
        advanceUntilIdle()

        assertEquals(1, reminderScheduler.scheduled.size)
        assertEquals(null, viewModel.reminderNotice.value)
    }

    @Test
    fun blockedNotificationsAreReportedAndSettingsCanBeOpened() = runTest(dispatcher) {
        reminderPermissions.notificationsAllowed = false
        // Bildirim kapalıyken dakiklik uyarısı anlamsız; yalnızca ilki gösterilmeli.
        reminderPermissions.exactAlarmsAllowed = false
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.setReminder(5_000L)
        advanceUntilIdle()

        // Alarm yine de kurulur: izin sonradan verilirse çalsın.
        assertEquals(1, reminderScheduler.scheduled.size)
        assertEquals(ReminderNotice.NOTIFICATIONS_BLOCKED, viewModel.reminderNotice.value)

        viewModel.openReminderSettings()
        assertEquals(1, reminderPermissions.openedNotificationSettings)
        assertEquals(null, viewModel.reminderNotice.value)
    }

    @Test
    fun exactAlarmNoticeIsNotRepeatedAfterNotNow() = runTest(dispatcher) {
        reminderPermissions.exactAlarmsAllowed = false
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.setReminder(5_000L)
        advanceUntilIdle()
        assertEquals(ReminderNotice.EXACT_ALARM_OFF, viewModel.reminderNotice.value)

        viewModel.dismissReminderNotice()
        assertEquals(null, viewModel.reminderNotice.value)

        // "Şimdi değil" dendikten sonra her hatırlatıcıda yeniden sorulmaz.
        viewModel.setReminder(6_000L)
        advanceUntilIdle()
        assertEquals(null, viewModel.reminderNotice.value)
    }

    @Test
    fun openingAndClosingWithoutChangesDoesNotTouchTheNote() = runTest(dispatcher) {
        repository.upsertNote(storedNote("n1"))

        val viewModel = editorFor("n1")
        advanceUntilIdle()
        // Yükleme sonrası otomatik kaydetme penceresi de geçsin.
        advanceTimeBy(1_000)
        viewModel.save()
        advanceUntilIdle()

        // "Son düzenleme" değişmemeli; not yalnızca okundu.
        assertEquals(1L, repository.getNoteById("n1")?.updatedAt)
    }

    @Test
    fun reopeningPicksUpChangesMadeOutsideTheEditor() = runTest(dispatcher) {
        repository.upsertNote(storedNote("n1"))
        val label = repository.createLabel("iş")

        // İlk açılış ve çıkış.
        val viewModel = editorFor("n1")
        advanceUntilIdle()
        viewModel.save()
        advanceUntilIdle()

        // Not kapalıyken listeden sabitlendi, etiketlendi ve arşivlendi.
        repository.setPinned("n1", true)
        repository.setLabelForNotes(listOf("n1"), label.id, assigned = true)
        repository.setArchived("n1", true)

        // Aynı ViewModel örneğiyle (önbellekten) yeniden açılıp düzenleniyor.
        viewModel.onOpened()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isArchived)

        viewModel.onTitleChange("Düzenlendi")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.getNoteById("n1")
        assertEquals("Düzenlendi", saved?.title)
        assertEquals(true, saved?.isPinned)
        assertEquals(true, saved?.isArchived)
        assertEquals(listOf("iş"), saved?.labels?.map { it.name })
    }

    @Test
    fun reopeningKeepsUnsavedTextInsteadOfReloading() = runTest(dispatcher) {
        settings.setAutoSaveEnabled(false)
        repository.upsertNote(storedNote("n1"))

        val viewModel = editorFor("n1")
        advanceUntilIdle()
        viewModel.onTitleChange("Yarım kalan")

        // Ekran döndürme: aynı ViewModel ile ekran yeniden kuruluyor.
        viewModel.onOpened()
        advanceUntilIdle()

        assertEquals("Yarım kalan", viewModel.uiState.value.title)
    }

    @Test
    fun discardedChangesDoNotComeBack() = runTest(dispatcher) {
        settings.setAutoSaveEnabled(false)
        repository.upsertNote(storedNote("n1", title = "Asıl"))

        val viewModel = editorFor("n1")
        advanceUntilIdle()
        viewModel.onTitleChange("Vazgeçilecek")
        viewModel.discardChanges()

        assertEquals("Asıl", viewModel.uiState.value.title)
        assertEquals(false, viewModel.hasUnsavedChanges.value)
    }

    @Test
    fun pendingAutosaveDoesNotBringDeletedNoteBack() = runTest(dispatcher) {
        repository.upsertNote(storedNote("n1"))

        val viewModel = editorFor("n1")
        advanceUntilIdle()

        // Otomatik kaydetme beklerken (600ms dolmadan) not siliniyor.
        viewModel.onTitleChange("Son anda yazıldı")
        viewModel.delete()
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals(listOf("n1"), repository.observeTrashedNotes().first().map { it.id })
        assertTrue(repository.observeActiveNotes().first().isEmpty())
    }

    @Test
    fun newNoteUsesGivenIdAndResumesFromItAfterRestore() = runTest(dispatcher) {
        val first = editorFor(noteId = null, newNoteId = "k1")
        advanceUntilIdle()
        first.onTitleChange("Taslak")
        first.save()
        advanceUntilIdle()
        assertEquals("Taslak", repository.getNoteById("k1")?.title)

        // Süreç ölümünden sonra: aynı id ile yeni bir ViewModel kuruluyor.
        val restored = editorFor(noteId = null, newNoteId = "k1")
        advanceUntilIdle()
        assertEquals("Taslak", restored.uiState.value.title)

        restored.onTitleChange("Taslak 2")
        restored.save()
        advanceUntilIdle()

        // İkinci bir kopya oluşmamalı.
        assertEquals(listOf("Taslak 2"), repository.observeActiveNotes().first().map { it.title })
    }

    @Test
    fun newNoteIsPersistedOnSave() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onTitleChange("Merhaba")
        viewModel.save()
        advanceUntilIdle()

        val notes = repository.observeActiveNotes().first()
        assertEquals(listOf("Merhaba"), notes.map { it.title })
    }

    @Test
    fun newNoteCanStartDirectlyAsChecklist() = runTest(dispatcher) {
        val viewModel = editorFor(noteId = null, startAsChecklist = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isChecklist)
        assertEquals(1, state.checklistItems.size)
        assertEquals("", state.checklistItems.single().text)
        // İmleç doğrudan ilk maddede olsun.
        assertEquals(state.checklistItems.single().id, viewModel.pendingFocusItemId.value)
    }

    @Test
    fun blankNoteIsNotPersisted() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertTrue(repository.observeActiveNotes().first().isEmpty())
    }

    @Test
    fun debouncedAutosavePersistsChanges() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onTitleChange("Otomatik")
        // Debounce süresi 600ms; henüz kaydedilmemiş olmalı.
        advanceTimeBy(300)
        assertTrue(repository.observeActiveNotes().first().isEmpty())

        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(listOf("Otomatik"), repository.observeActiveNotes().first().map { it.title })
    }

    @Test
    fun autosaveStopsWhenNothingChanges() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onTitleChange("Tek kayıt")
        advanceTimeBy(1_000)
        advanceUntilIdle()

        val firstSave = viewModel.lastSavedAt.value
        assertTrue(firstSave > 0)

        // Kullanıcı hiçbir şey yapmadan bekliyor: yeni kayıt olmamalı.
        // (Kaydetme sonrası zaman damgasını uiState'e yazmak, otomatik kaydetme
        // akışını yeniden tetikleyip 600ms'de bir tekrarlayan bir döngü yaratıyordu.)
        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertEquals(firstSave, viewModel.lastSavedAt.value)
    }

    @Test
    fun autosaveDisabledMeansNothingIsWrittenUntilSaveIsCalled() = runTest(dispatcher) {
        settings.setAutoSaveEnabled(false)
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onTitleChange("Elle kaydedilecek")
        advanceTimeBy(3_000)
        advanceUntilIdle()

        // Otomatik kaydetme kapalı: hiçbir şey yazılmamalı, ama değişiklik işaretli olmalı.
        assertTrue(repository.observeActiveNotes().first().isEmpty())
        assertTrue(viewModel.hasUnsavedChanges.value)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            listOf("Elle kaydedilecek"),
            repository.observeActiveNotes().first().map { it.title },
        )
        assertEquals(false, viewModel.hasUnsavedChanges.value)
    }

    @Test
    fun editingArchivedNotePreservesArchivedFlag() = runTest(dispatcher) {
        repository.upsertNote(
            Note(
                id = "n1",
                title = "Eski",
                content = "içerik",
                color = NoteColor.DEFAULT,
                isArchived = true,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val viewModel = editorFor("n1")
        advanceUntilIdle()

        viewModel.onTitleChange("Yeni başlık")
        viewModel.save()
        advanceUntilIdle()

        val saved = repository.getNoteById("n1")
        assertEquals("Yeni başlık", saved?.title)
        assertEquals(true, saved?.isArchived)
    }

    @Test
    fun togglingChecklistModeConvertsContentWithoutLosingIt() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onContentChange("süt al\nekmek al\n\nyumurta")

        // Metin -> checklist: her dolu satır bir öğe.
        viewModel.toggleChecklistMode()
        assertEquals(
            listOf("süt al", "ekmek al", "yumurta"),
            viewModel.uiState.value.checklistItems.map { it.text },
        )
        assertEquals("", viewModel.uiState.value.content)

        // Checklist -> metin: her öğe bir satır.
        viewModel.toggleChecklistMode()
        assertEquals("süt al\nekmek al\nyumurta", viewModel.uiState.value.content)
        assertTrue(viewModel.uiState.value.checklistItems.isEmpty())
    }

    @Test
    fun enterInsertsItemAfterCurrentAndRequestsFocus() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onContentChange("bir\niki")
        viewModel.toggleChecklistMode()
        val first = viewModel.uiState.value.checklistItems.first()

        viewModel.addChecklistItemAfter(first.id)

        val items = viewModel.uiState.value.checklistItems
        assertEquals(listOf("bir", "", "iki"), items.map { it.text })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
        assertEquals(items[1].id, viewModel.pendingFocusItemId.value)

        viewModel.onFocusHandled()
        assertEquals(null, viewModel.pendingFocusItemId.value)
    }

    @Test
    fun backspaceOnEmptyItemRemovesItAndFocusesPrevious() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onContentChange("bir\niki")
        viewModel.toggleChecklistMode()
        val (first, second) = viewModel.uiState.value.checklistItems
        viewModel.onChecklistItemTextChange(second.id, "")

        viewModel.onChecklistBackspaceOnEmpty(second.id)

        assertEquals(listOf("bir"), viewModel.uiState.value.checklistItems.map { it.text })
        assertEquals(first.id, viewModel.pendingFocusItemId.value)
    }

    @Test
    fun backspaceOnLastRemainingItemIsIgnored() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.toggleChecklistMode()
        val only = viewModel.uiState.value.checklistItems.single()

        viewModel.onChecklistBackspaceOnEmpty(only.id)

        assertEquals(1, viewModel.uiState.value.checklistItems.size)
    }

    @Test
    fun moveChecklistItemReordersAndReindexes() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onContentChange("bir\niki\nüç")
        viewModel.toggleChecklistMode()
        val first = viewModel.uiState.value.checklistItems.first()

        // İlk öğeyi en alta taşı.
        viewModel.moveChecklistItem(first.id, 2)

        val items = viewModel.uiState.value.checklistItems
        assertEquals(listOf("iki", "üç", "bir"), items.map { it.text })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun checkingItemMovesItToTheEndSoDataMatchesDisplayOrder() = runTest(dispatcher) {
        val viewModel = editorFor(null)
        advanceUntilIdle()

        viewModel.onContentChange("bir\niki\nüç")
        viewModel.toggleChecklistMode()
        val first = viewModel.uiState.value.checklistItems.first()

        viewModel.toggleChecklistItemChecked(first.id)
        assertEquals(listOf("iki", "üç", "bir"), viewModel.uiState.value.checklistItems.map { it.text })

        // İşaret kaldırılınca tamamlanmamışların sonuna döner.
        viewModel.toggleChecklistItemChecked(first.id)
        assertEquals(listOf("iki", "üç", "bir"), viewModel.uiState.value.checklistItems.map { it.text })
        assertTrue(viewModel.uiState.value.checklistItems.none { it.isChecked })
    }

    @Test
    fun deleteMovesNoteToTrashAndCancelsReminder() = runTest(dispatcher) {
        repository.upsertNote(
            Note(
                id = "n1",
                title = "Silinecek",
                content = "",
                color = NoteColor.DEFAULT,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val viewModel = editorFor("n1")
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()

        assertEquals(listOf("n1"), repository.observeTrashedNotes().first().map { it.id })
        assertEquals(listOf("n1"), reminderScheduler.cancelled)
    }
}
