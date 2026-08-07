package com.cnsoftware.synapse.reminder

/**
 * Nota bağlı yerel bildirim zamanlayıcısı.
 * Android: AlarmManager + BroadcastReceiver, iOS: UNUserNotificationCenter.
 */
interface ReminderScheduler {
    fun schedule(noteId: String, title: String, message: String, triggerAtMillis: Long)
    fun cancel(noteId: String)
}
