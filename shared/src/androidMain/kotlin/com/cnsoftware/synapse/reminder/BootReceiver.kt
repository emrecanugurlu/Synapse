package com.cnsoftware.synapse.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Cihaz yeniden başladığında AlarmManager'daki alarmlar silinir.
 * Bu receiver, veritabanındaki gelecek hatırlatıcıları okuyup alarmları yeniden kurar.
 * Koin, SynapseApplication.onCreate'te (receiver'dan önce) başlatıldığı için inject güvenlidir.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val repository: NoteRepository by inject()
    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val now = currentTimeMillis()
                repository.getNotesWithUpcomingReminders(now).forEach { note ->
                    val reminderAt = note.reminderAt ?: return@forEach
                    scheduler.schedule(
                        noteId = note.id,
                        title = note.title,
                        message = note.content.take(100),
                        triggerAtMillis = reminderAt,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
