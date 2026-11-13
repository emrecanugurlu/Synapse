package com.cnsoftware.synapse.ui

import com.cnsoftware.synapse.prefs.PreferencesStore
import com.cnsoftware.synapse.reminder.ReminderPermissions
import com.cnsoftware.synapse.reminder.ReminderScheduler
import com.cnsoftware.synapse.share.ShareService

class FakePreferencesStore : PreferencesStore {
    private val strings = mutableMapOf<String, String>()
    private val booleans = mutableMapOf<String, Boolean>()
    private val ints = mutableMapOf<String, Int>()

    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }

    override fun getInt(key: String, default: Int): Int = ints[key] ?: default
    override fun putInt(key: String, value: Int) {
        ints[key] = value
    }
}

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Pair<String, Long>>()
    val cancelled = mutableListOf<String>()

    override fun schedule(noteId: String, title: String, message: String, triggerAtMillis: Long) {
        scheduled += noteId to triggerAtMillis
    }

    override fun cancel(noteId: String) {
        cancelled += noteId
    }
}

class FakeReminderPermissions(
    var notificationsAllowed: Boolean = true,
    var exactAlarmsAllowed: Boolean = true,
) : ReminderPermissions {
    var openedNotificationSettings = 0
    var openedExactAlarmSettings = 0

    override suspend fun ensureNotificationsAllowed(): Boolean = notificationsAllowed
    override fun openNotificationSettings() {
        openedNotificationSettings++
    }

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed
    override fun openExactAlarmSettings() {
        openedExactAlarmSettings++
    }
}

class FakeShareService : ShareService {
    val sharedTexts = mutableListOf<String>()

    override fun shareText(text: String) {
        sharedTexts += text
    }
}
