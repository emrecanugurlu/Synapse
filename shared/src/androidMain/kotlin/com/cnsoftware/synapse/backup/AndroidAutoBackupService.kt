package com.cnsoftware.synapse.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Klasör seçimi için Activity köprüsü.
 *
 * `ACTION_OPEN_DOCUMENT_TREE` sonucu Activity'de alınmak zorunda; dosya
 * seçicideki [AndroidFilePicker] ile aynı sebep.
 */
object AndroidFolderPicker {

    interface Host {
        /** Klasör seçtirir ve kalıcı izni alır; iptal edilirse null döner. */
        suspend fun chooseFolder(): Uri?
    }

    var host: Host? = null
}

internal const val AUTO_BACKUP_WORK = "synapse-auto-backup"

class AndroidAutoBackupService(private val context: Context) : AutoBackupService {

    override suspend fun chooseFolder(): String? =
        AndroidFolderPicker.host?.chooseFolder()?.toString()

    override fun hasFolderAccess(folderUri: String): Boolean {
        if (folderUri.isBlank()) return false
        val uri = runCatching { Uri.parse(folderUri) }.getOrNull() ?: return false

        // İzin listede duruyor mu?
        val granted = context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        if (!granted) return false

        // İzin duruyor ama klasör silinmiş olabilir; gerçekten yazılabilir mi?
        val folder = DocumentFile.fromTreeUri(context, uri)
        return folder != null && folder.exists() && folder.canWrite()
    }

    override fun schedule(hours: Long) {
        val workManager = WorkManager.getInstance(context)
        if (hours <= 0) {
            workManager.cancelUniqueWork(AUTO_BACKUP_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(hours, TimeUnit.HOURS).build()
        // UPDATE: sıklık değiştiğinde mevcut iş yeni aralıkla güncellensin,
        // KEEP olsaydı eski aralık sonsuza kadar kalırdı.
        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun runNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$AUTO_BACKUP_WORK-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AutoBackupWorker>().build(),
        )
    }
}

/**
 * Seçilen klasöre JSON yazar ve eski yedekleri budar.
 *
 * `Intent.FLAG_GRANT_*` bayrakları yalnızca izni ALIRKEN gerekli; yazma
 * sırasında kalıcı izin zaten `contentResolver`'da kayıtlı.
 */
internal object BackupFolderWriter {

    /** Klasörde tutulacak en fazla yedek sayısı. */
    private const val KEEP_COUNT = 5

    private const val PREFIX = "synapse-"
    private const val SUFFIX = ".json"

    fun write(context: Context, folderUri: String, fileName: String, content: String): Boolean {
        val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) ?: return false
        if (!folder.exists() || !folder.canWrite()) return false

        // Aynı adlı dosya varsa Android "(1)" ekleyerek yenisini yaratır;
        // üzerine yazmak için önce mevcudu siliyoruz.
        folder.findFile(fileName)?.delete()

        val file = folder.createFile("application/json", fileName) ?: return false
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
            output.write(content.encodeToByteArray())
        } ?: return false

        prune(folder)
        return true
    }

    /**
     * Yalnızca bu uygulamanın ürettiği dosyalar budanıyor.
     * Kullanıcı aynı klasörde başka şeyler tutuyor olabilir; ada bakmadan
     * silmek onların verisini yok ederdi.
     */
    private fun prune(folder: DocumentFile) {
        val backups = folder.listFiles()
            .filter { it.isFile && it.name?.startsWith(PREFIX) == true && it.name?.endsWith(SUFFIX) == true }
            .sortedByDescending { it.lastModified() }
        backups.drop(KEEP_COUNT).forEach { it.delete() }
    }
}

/** İzin alınırken kullanılan bayraklar; Activity tarafından da kullanılıyor. */
internal const val FOLDER_PERMISSION_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
