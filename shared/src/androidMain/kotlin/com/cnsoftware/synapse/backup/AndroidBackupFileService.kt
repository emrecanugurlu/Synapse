package com.cnsoftware.synapse.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activity'ye bağımlı olan kısım için köprü.
 *
 * Storage Access Framework çağrıları `registerForActivityResult` ile Activity
 * oluşturulurken kaydedilmek zorunda; bu yüzden sonuç bekleyen kısım
 * MainActivity'de yaşıyor ve buraya kendini tanıtıyor. Shared modül yalnızca
 * bu arayüzü görüyor.
 */
object AndroidFilePicker {

    interface Host {
        /** Kaydetme diyaloğunu açar; iptal edilirse null döner. */
        suspend fun createDocument(suggestedName: String): Uri?

        /** Açma diyaloğunu açar; iptal edilirse null döner. */
        suspend fun openDocument(): Uri?
    }

    /** MainActivity `onCreate`'te set eder, `onDestroy`'da temizler. */
    var host: Host? = null
}

class AndroidBackupFileService(private val context: Context) : BackupFileService {

    override suspend fun writeBackup(suggestedName: String, content: String): Boolean {
        val uri = AndroidFilePicker.host?.createDocument(suggestedName) ?: return false
        withContext(Dispatchers.IO) {
            // "wt": truncate. Varsayılan "w" mevcut dosyanın üzerine yazarken
            // eski içerik daha uzunsa kuyruğu bırakıyor ve JSON bozuluyor.
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(content.encodeToByteArray())
            } ?: error("Yedek dosyası yazılamadı")
        }
        return true
    }

    override suspend fun readBackup(): String? {
        val uri = AndroidFilePicker.host?.openDocument() ?: return null
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: error("Yedek dosyası okunamadı")
        }
    }
}
