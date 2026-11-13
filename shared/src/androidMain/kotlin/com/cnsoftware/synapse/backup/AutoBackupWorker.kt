package com.cnsoftware.synapse.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.prefs.AppSettings
import com.cnsoftware.synapse.util.currentTimeMillis
import com.cnsoftware.synapse.util.formatBackupTimestamp
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Arka planda yedek üretir.
 *
 * Elle yedekten farkı, kullanıcıya dosya sormaması: hedef klasör bir kez
 * seçilmiş ve izni kalıcı. Bu yüzden [BackupManager] yerine doğrudan
 * repository'den okuyup klasöre yazıyor.
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: NoteRepository by inject()
    private val settings: AppSettings by inject()
    private val autoBackup: AutoBackupService by inject()

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun doWork(): Result {
        val folderUri = settings.backupFolderUri.value
        // Klasör seçilmemiş ya da izin kaybolmuş: tekrar denemek düzeltmez,
        // kullanıcının ayarlardan yeniden klasör seçmesi gerekiyor.
        if (!autoBackup.hasFolderAccess(folderUri)) return Result.failure()

        return try {
            val notes = repository.getAllNotes()
            val labels = repository.getAllLabels()
            val now = currentTimeMillis()

            val payload = BackupFile(
                exportedAt = now,
                labels = labels.map { it.toBackup() },
                notes = notes.map { it.toBackup() },
            )

            val ok = BackupFolderWriter.write(
                context = applicationContext,
                folderUri = folderUri,
                fileName = "synapse-${formatBackupTimestamp(now)}.json",
                content = json.encodeToString(payload),
            )

            if (ok) {
                settings.setLastBackupAt(now)
                Result.success()
            } else {
                // Geçici bir sorun olabilir (depolama dolu, SD kart takılı değil).
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
