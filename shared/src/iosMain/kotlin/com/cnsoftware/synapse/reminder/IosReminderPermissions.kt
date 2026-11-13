package com.cnsoftware.synapse.reminder

/**
 * iOS'ta ayrı bir adım gerekmiyor: bildirim izni [IosReminderScheduler] içinde,
 * hatırlatıcı kurulurken isteniyor ve "tam zamanlı alarm" diye ayrı bir izin yok.
 */
class IosReminderPermissions : ReminderPermissions {

    override suspend fun ensureNotificationsAllowed(): Boolean = true

    override fun openNotificationSettings() = Unit

    override fun canScheduleExactAlarms(): Boolean = true

    override fun openExactAlarmSettings() = Unit
}
