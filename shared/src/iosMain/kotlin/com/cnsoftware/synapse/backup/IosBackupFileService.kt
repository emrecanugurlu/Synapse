package com.cnsoftware.synapse.backup

/**
 * iOS tarafı henüz bağlanmadı.
 *
 * Doğru uygulama `UIDocumentPickerViewController` ile yapılmalı: dışa aktarımda
 * içerik önce geçici bir dosyaya yazılıp `forExportingURLs`, içe aktarımda
 * `forOpeningContentTypes` ile açılmalı; sonuç delegate üzerinden alınmalı.
 *
 * Burada iptal (`false`/`null`) dönmek, çökmeden sessizce hiçbir şey yapmamak
 * anlamına geliyor: iOS uygulaması henüz yayınlanmadığı için bu kabul
 * edilebilir bir ara durum. Bağlanmadan iOS sürümü yayınlanmamalı.
 */
class IosBackupFileService : BackupFileService {

    override suspend fun writeBackup(suggestedName: String, content: String): Boolean = false

    override suspend fun readBackup(): String? = null
}
