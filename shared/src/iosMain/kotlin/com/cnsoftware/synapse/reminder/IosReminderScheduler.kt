package com.cnsoftware.synapse.reminder

import com.cnsoftware.synapse.util.currentTimeMillis
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosReminderScheduler : ReminderScheduler {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(noteId: String, title: String, message: String, triggerAtMillis: Long) {
        val intervalSeconds = (triggerAtMillis - currentTimeMillis()) / 1000.0
        if (intervalSeconds <= 0) return

        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions
            val content = UNMutableNotificationContent().apply {
                setTitle(title.ifBlank { "Not hatırlatıcısı" })
                setBody(message)
                setSound(UNNotificationSound.defaultSound)
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = intervalSeconds,
                repeats = false,
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = noteId,
                content = content,
                trigger = trigger,
            )
            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    override fun cancel(noteId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(noteId))
    }
}
