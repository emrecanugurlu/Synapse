package com.cnsoftware.synapse.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cnsoftware.synapse.backup.AutoBackupService
import com.cnsoftware.synapse.backup.BackupManager
import com.cnsoftware.synapse.backup.BackupResult
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.prefs.AutoBackupFrequency
import com.cnsoftware.synapse.reminder.ReminderCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Ayarlar ekranının yedekleme kısmı. */
class SettingsViewModel(
    private val backupManager: BackupManager,
    private val autoBackup: AutoBackupService,
    private val settings: AppSettings,
    private val reminders: ReminderCoordinator,
) : ViewModel() {

    /** İşlem sürerken düğmeler kilitlenir; iki kez tetiklenmesin. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Son işlemin sonucu; kullanıcıya gösterildikten sonra temizlenir. */
    private val _result = MutableStateFlow<BackupResult?>(null)
    val result: StateFlow<BackupResult?> = _result.asStateFlow()

    /**
     * Klasör seçili ama erişilemiyor mu?
     *
     * Kullanıcı klasörü silmiş, SD kartı çıkarmış veya izni geri almış
     * olabilir. Bu durumda otomatik yedek sessizce başarısız olur; ayarlar
     * ekranı uyarı gösterip yeniden seçtiriyor.
     */
    private val _folderAccessLost = MutableStateFlow(false)
    val folderAccessLost: StateFlow<Boolean> = _folderAccessLost.asStateFlow()

    init {
        refreshFolderAccess()
    }

    fun refreshFolderAccess() {
        val uri = settings.backupFolderUri.value
        _folderAccessLost.value = uri.isNotBlank() && !autoBackup.hasFolderAccess(uri)
    }

    fun exportBackup() = launchBackup { backupManager.export() }

    fun importBackup() = launchBackup {
        backupManager.import().also { outcome ->
            // Yedekten gelen notların hatırlatıcıları yalnızca veritabanına
            // yazılır; alarmlar burada kurulmazsa cihaz yeniden başlayana kadar çalmaz.
            if (outcome is BackupResult.Success) reminders.rescheduleAll()
        }
    }

    /**
     * Klasör seçtirir; seçim yapılırsa sıklık kapalıysa günlüğe çekilir ve
     * ilk yedek hemen alınır — kullanıcı özelliğin çalıştığını görsün.
     */
    fun chooseBackupFolder() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val uri = autoBackup.chooseFolder()
            if (uri != null) {
                settings.setBackupFolderUri(uri)
                if (settings.autoBackupFrequency.value == AutoBackupFrequency.OFF) {
                    setFrequency(AutoBackupFrequency.DAILY)
                } else {
                    autoBackup.schedule(settings.autoBackupFrequency.value.hours)
                }
                autoBackup.runNow()
            }
            refreshFolderAccess()
            _busy.value = false
        }
    }

    fun setFrequency(frequency: AutoBackupFrequency) {
        settings.setAutoBackupFrequency(frequency)
        // Klasör yoksa iş kurulmuyor: yazacak yer olmadan çalışmak
        // pil harcamaktan başka işe yaramaz.
        val canRun = settings.backupFolderUri.value.isNotBlank()
        autoBackup.schedule(if (canRun) frequency.hours else 0L)
    }

    fun consumeResult() {
        _result.value = null
    }

    private fun launchBackup(block: suspend () -> BackupResult) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            val outcome = block()
            _busy.value = false
            // İptal sessizce geçilsin; kullanıcı zaten vazgeçtiğini biliyor.
            _result.value = outcome.takeIf { it !is BackupResult.Cancelled }
        }
    }
}
