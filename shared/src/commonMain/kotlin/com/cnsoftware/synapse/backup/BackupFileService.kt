package com.cnsoftware.synapse.backup

/**
 * Yedek dosyasını kullanıcının seçtiği konuma yazar / oradan okur.
 *
 * Uygulama kendi klasörüne yazmıyor: yedeğin amacı uygulama silinse bile
 * ayakta kalmak. Bu yüzden konumu her zaman kullanıcı seçiyor
 * (Android: Storage Access Framework, iOS: Files).
 */
interface BackupFileService {

    /**
     * Kaydetme yeri sorup [content] içeriğini yazar.
     * @return kullanıcı iptal ettiyse false.
     */
    suspend fun writeBackup(suggestedName: String, content: String): Boolean

    /**
     * Dosya seçtirip içeriğini okur.
     * @return kullanıcı iptal ettiyse null.
     */
    suspend fun readBackup(): String?
}
