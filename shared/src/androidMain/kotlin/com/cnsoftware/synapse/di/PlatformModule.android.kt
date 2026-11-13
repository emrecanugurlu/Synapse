package com.cnsoftware.synapse.di

import android.content.Context
import com.cnsoftware.synapse.backup.AndroidAutoBackupService
import com.cnsoftware.synapse.backup.AndroidBackupFileService
import com.cnsoftware.synapse.backup.AutoBackupService
import com.cnsoftware.synapse.backup.BackupFileService
import com.cnsoftware.synapse.db.DatabaseDriverFactory
import com.cnsoftware.synapse.reminder.AndroidReminderScheduler
import com.cnsoftware.synapse.reminder.ReminderScheduler
import com.cnsoftware.synapse.prefs.AndroidPreferencesStore
import com.cnsoftware.synapse.prefs.PreferencesStore
import com.cnsoftware.synapse.share.AndroidShareService
import com.cnsoftware.synapse.share.ShareService
import org.koin.dsl.module

fun platformModule(context: Context) = module {
    single { DatabaseDriverFactory(context) }
    single<ReminderScheduler> { AndroidReminderScheduler(context) }
    single<ShareService> { AndroidShareService(context) }
    single<PreferencesStore> { AndroidPreferencesStore(context) }
    single<BackupFileService> { AndroidBackupFileService(context) }
    single<AutoBackupService> { AndroidAutoBackupService(context) }
}
