package com.cnsoftware.synapse.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

    private val reminders: ReminderCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                reminders.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
