package com.cnsoftware.synapse.reminder

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cnsoftware.synapse.data.NoteRepositoryImpl
import com.cnsoftware.synapse.db.NoteDatabase
import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.ui.FakePreferencesStore
import com.cnsoftware.synapse.ui.FakeReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderCoordinatorTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var driver: SqlDriver
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var preferences: FakePreferencesStore
    private lateinit var coordinator: ReminderCoordinator

    private val hour = 60 * 60 * 1000L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoteDatabase.Schema.create(driver)
        repository = NoteRepositoryImpl(NoteDatabase(driver), dispatcher)
        scheduler = FakeReminderScheduler()
        preferences = FakePreferencesStore()
        coordinator = ReminderCoordinator(repository, scheduler, preferences)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun note(id: String, reminderAt: Long?, isDeleted: Boolean = false) = Note(
        id = id,
        title = "Başlık $id",
        content = "içerik",
        isDeleted = isDeleted,
        createdAt = 1L,
        updatedAt = 1L,
        reminderAt = reminderAt,
    )

    /** "Kayıt şu andan itibaren tutuluyor" eşiğini geçmişe çeker (ilk kurulum çok önceymiş gibi). */
    private fun trackingSince(millis: Long) {
        preferences.putString("reminder_tracking_since", millis.toString())
    }

    @Test
    fun startupSchedulesUpcomingAndShowsMissedOnes() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        trackingSince(now - 10 * hour)
        repository.upsertNote(note("gelecek", reminderAt = now + hour))
        repository.upsertNote(note("kacan", reminderAt = now - hour))
        repository.upsertNote(note("silinmis", reminderAt = now - hour, isDeleted = true))
        repository.upsertNote(note("hatirlaticisiz", reminderAt = null))

        coordinator.onStartup()

        // Kaçırılan, kendi (geçmiş) zamanıyla kurulur: alarm hemen çalar.
        assertEquals(
            setOf("gelecek" to now + hour, "kacan" to now - hour),
            scheduler.scheduled.toSet(),
        )
    }

    @Test
    fun remindersOlderThanTrackingStartAreNotTreatedAsMissed() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        // Eşik yok: bu, güncellemeden sonraki ilk açılış. O ana kadarki geçmiş
        // hatırlatıcıların çalıp çalmadığı bilinmiyor; toplu bildirim atılmamalı.
        repository.upsertNote(note("eski", reminderAt = now - hour))

        coordinator.onStartup()

        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun reminderAlreadyShownIsNotShownAgainOnStartup() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        trackingSince(now - 10 * hour)
        repository.upsertNote(note("n1", reminderAt = now - hour))

        // Alarm zamanında çaldı ve bildirim gösterildi.
        assertTrue(coordinator.claimFiring("n1", now - hour))

        coordinator.onStartup()

        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun sameReminderCanOnlyBeClaimedOnce() {
        assertTrue(coordinator.claimFiring("n1", 1_000L))
        // Aynı alarm ikinci yoldan gelirse bildirim tekrarlanmaz.
        assertFalse(coordinator.claimFiring("n1", 1_000L))
        // Kullanıcı aynı nota yeni bir zaman kurduysa o ayrı bir hatırlatıcıdır.
        assertTrue(coordinator.claimFiring("n1", 2_000L))
    }

    @Test
    fun checklistNoteUsesFirstFilledItemAsMessage() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        val messages = mutableListOf<String>()
        val recording = object : ReminderScheduler {
            override fun schedule(noteId: String, title: String, message: String, triggerAtMillis: Long) {
                messages += message
            }

            override fun cancel(noteId: String) = Unit
        }
        repository.upsertNote(
            note("liste", reminderAt = now + hour).copy(
                content = "",
                isChecklist = true,
                checklistItems = listOf(
                    ChecklistItem(id = "c1", text = "", position = 0),
                    ChecklistItem(id = "c2", text = "süt al", position = 1),
                ),
            ),
        )

        ReminderCoordinator(repository, recording, preferences).rescheduleAll()

        assertEquals(listOf("süt al"), messages)
    }
}
