package com.cnsoftware.synapse.di

import com.cnsoftware.synapse.backup.AutoBackupService
import com.cnsoftware.synapse.backup.BackupFileService
import com.cnsoftware.synapse.backup.IosAutoBackupService
import com.cnsoftware.synapse.backup.IosBackupFileService
import com.cnsoftware.synapse.db.DatabaseDriverFactory
import com.cnsoftware.synapse.reminder.IosReminderScheduler
import com.cnsoftware.synapse.reminder.ReminderScheduler
import com.cnsoftware.synapse.prefs.IosPreferencesStore
import com.cnsoftware.synapse.prefs.PreferencesStore
import com.cnsoftware.synapse.share.IosShareService
import com.cnsoftware.synapse.share.ShareService
import org.koin.dsl.module

fun platformModule() = module {
    single { DatabaseDriverFactory() }
    single<ReminderScheduler> { IosReminderScheduler() }
    single<ShareService> { IosShareService() }
    single<PreferencesStore> { IosPreferencesStore() }
    single<BackupFileService> { IosBackupFileService() }
    single<AutoBackupService> { IosAutoBackupService() }
}

/** iOS tarafından (Swift) çağrılacak Koin başlatma fonksiyonu. */
fun initKoinIOS() {
    initKoin(platformModule())
}
