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
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_EXACT_ALARM_PERMISSION_CHANGED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    // Gelecektekileri kur, cihaz kapalıyken kaçanları göster.
                    reminders.onStartup()
                } else {
                    // Kullanıcı tam zamanlı alarm iznini verdi: yaklaşık kurulmuş
                    // alarmlar tam zamanlı olarak yeniden kurulsun.
                    reminders.rescheduleAll()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (API 31+).
        // Sabit eski sürümlerde tanımlı olmadığı için değeri doğrudan yazılıyor.
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
