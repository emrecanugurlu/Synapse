package com.cnsoftware.synapse.reminder

import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.util.currentTimeMillis

/** Bildirimde gösterilecek kısa metin: düz notta içerik, listede ilk dolu madde. */
internal fun reminderMessage(
    isChecklist: Boolean,
    content: String,
    checklistItems: List<ChecklistItem>,
): String =
    if (isChecklist) {
        checklistItems.firstOrNull { it.text.isNotBlank() }?.text.orEmpty()
    } else {
        content
    }.take(100)

/**
 * Notların durumu editör dışında değiştiğinde alarmları veritabanıyla uyumlu tutar.
 *
 * Alarm, notun kendisinden bağımsız olarak sistemde yaşıyor: not çöpe atılınca
 * kendiliğinden iptal olmuyor, çöpten ya da yedekten geri gelince kendiliğinden
 * kurulmuyor. Bu işi her ViewModel ayrı ayrı yapmak yerine tek yerden yürütüyoruz.
 */
class ReminderCoordinator(
    private val repository: NoteRepository,
    private val scheduler: ReminderScheduler,
) {

    /** Çöpe atılan ya da kalıcı silinen notların alarmlarını iptal eder. */
    fun cancel(noteIds: Collection<String>) {
        noteIds.forEach(scheduler::cancel)
    }

    /** Çöpten geri alınan notların, zamanı henüz gelmemiş alarmlarını yeniden kurar. */
    suspend fun reschedule(noteIds: Collection<String>) {
        val now = currentTimeMillis()
        noteIds.forEach { id ->
            repository.getNoteById(id)?.let { scheduleIfUpcoming(it, now) }
        }
    }

    /** Cihaz yeniden başladığında ve yedekten geri yüklemede: gelecekteki tüm alarmlar. */
    suspend fun rescheduleAll() {
        val now = currentTimeMillis()
        // Sorgu madde listesini yüklemiyor; liste notlarında bildirim metni
        // boş kalmasın diye her not tam hâliyle yeniden okunuyor.
        reschedule(repository.getNotesWithUpcomingReminders(now).map { it.id })
    }

    private fun scheduleIfUpcoming(note: Note, now: Long) {
        val reminderAt = note.reminderAt ?: return
        if (note.isDeleted || reminderAt <= now) return
        scheduler.schedule(
            noteId = note.id,
            title = note.title,
            message = reminderMessage(note.isChecklist, note.content, note.checklistItems),
            triggerAtMillis = reminderAt,
        )
    }
}
