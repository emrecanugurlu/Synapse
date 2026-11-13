package com.cnsoftware.synapse.reminder

import com.cnsoftware.synapse.domain.ChecklistItem
import com.cnsoftware.synapse.domain.Note
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.prefs.PreferencesStore
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
    private val preferences: PreferencesStore,
) {

    /** Çöpe atılan ya da kalıcı silinen notların alarmlarını iptal eder. */
    fun cancel(noteIds: Collection<String>) {
        noteIds.forEach(scheduler::cancel)
    }

    /** Çöpten geri alınan notların, zamanı henüz gelmemiş alarmlarını yeniden kurar. */
    suspend fun reschedule(noteIds: Collection<String>) {
        val now = currentTimeMillis()
        noteIds.forEach { id ->
            val note = repository.getNoteById(id) ?: return@forEach
            val reminderAt = note.reminderAt ?: return@forEach
            if (!note.isDeleted && reminderAt > now) schedule(note, reminderAt)
        }
    }

    /**
     * Gelecekteki tüm alarmları yeniden kurar: yedekten geri yüklemede ve
     * tam zamanlı alarm izni değiştiğinde (alarmlar yeni izinle kurulsun diye).
     */
    suspend fun rescheduleAll() {
        val now = currentTimeMillis()
        // Sorgu madde listesini yüklemiyor; liste notlarında bildirim metni
        // boş kalmasın diye her not tam hâliyle yeniden okunuyor.
        reschedule(repository.getNotesWithUpcomingReminders(now).map { it.id })
    }

    /**
     * Uygulama süreci başlarken ve cihaz yeniden açıldığında çağrılır.
     *
     * Yeniden başlatma ve "zorla durdur" sistemdeki alarmları siler. Gelecektekiler
     * yeniden kurulur; cihaz kapalıyken zamanı geçmiş olanlar ise kaybolmasın diye
     * hemen gösterilir.
     */
    suspend fun onStartup() {
        val now = currentTimeMillis()
        val baseline = baseline(now)
        rescheduleAll()
        repository.getNotesWithDueReminders(afterMillis = baseline, untilMillis = now)
            .forEach { due ->
                val reminderAt = due.reminderAt ?: return@forEach
                if (wasFired(due.id, reminderAt)) return@forEach
                val note = repository.getNoteById(due.id) ?: return@forEach
                // Geçmiş bir zamana kurulan alarm hemen çalar ve normal bildirim
                // yolundan geçer; böylece "gösterildi" kaydı da tek yerde tutulur.
                schedule(note, reminderAt)
            }
    }

    /**
     * Bildirim gösterilmeden hemen önce çağrılır. Bu hatırlatıcı daha önce
     * gösterildiyse false döner; aynı alarm iki yoldan (asıl alarm + kaçırılmış
     * diye yeniden kurulan) gelirse kullanıcı iki bildirim görmesin.
     */
    fun claimFiring(noteId: String, reminderAt: Long): Boolean {
        if (wasFired(noteId, reminderAt)) return false
        preferences.putString(firedKey(noteId), reminderAt.toString())
        return true
    }

    private fun wasFired(noteId: String, reminderAt: Long): Boolean =
        preferences.getString(firedKey(noteId))?.toLongOrNull() == reminderAt

    /**
     * "Gösterildi" kaydı bu andan itibaren tutuluyor. Daha eski, zamanı geçmiş
     * hatırlatıcıların çalıp çalmadığı bilinemez; onları kaçırılmış sayıp
     * güncellemeden sonra toplu bildirim yağdırmamak için bu eşik kullanılıyor.
     */
    private fun baseline(now: Long): Long {
        preferences.getString(KEY_BASELINE)?.toLongOrNull()?.let { return it }
        preferences.putString(KEY_BASELINE, now.toString())
        return now
    }

    private fun schedule(note: Note, reminderAt: Long) {
        scheduler.schedule(
            noteId = note.id,
            title = note.title,
            message = reminderMessage(note.isChecklist, note.content, note.checklistItems),
            triggerAtMillis = reminderAt,
        )
    }

    private companion object {
        const val KEY_BASELINE = "reminder_tracking_since"
        fun firedKey(noteId: String) = "reminder_fired_$noteId"
    }
}
