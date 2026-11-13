package com.cnsoftware.synapse.backup

/**
 * iOS tarafı henüz bağlanmadı.
 *
 * Doğru uygulama: klasör için güvenlik kapsamlı yer imi
 * (`UIDocumentPickerViewController` + `bookmarkData`), periyodik iş için
 * `BGTaskScheduler`. iOS'ta arka plan çalışma zamanı garanti edilmediği için
 * "her gün tam olarak şu saatte" beklentisi Android'deki kadar sağlam olmaz.
 *
 * `hasFolderAccess` false döndüğü sürece ayarlar ekranı otomatik yedeği
 * kurulmamış gösteriyor; sessiz bir başarısızlık oluşmuyor.
 */
class IosAutoBackupService : AutoBackupService {

    override suspend fun chooseFolder(): String? = null

    override fun hasFolderAccess(folderUri: String): Boolean = false

    override fun schedule(hours: Long) = Unit

    override fun runNow() = Unit
}
