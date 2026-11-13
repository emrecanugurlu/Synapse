package com.cnsoftware.synapse.backup

/**
 * Otomatik yedeğin platforma bağlı kısmı.
 *
 * Android'de klasör seçimi Storage Access Framework, periyodik iş ise
 * WorkManager demek; ikisi de ortak modülde ifade edilemiyor.
 */
interface AutoBackupService {

    /**
     * Kullanıcıya klasör seçtirir ve o klasöre kalıcı yazma izni alır.
     * @return seçilen klasörün URI'si; iptal edilirse null.
     */
    suspend fun chooseFolder(): String?

    /**
     * Kayıtlı klasör izninin hâlâ geçerli olup olmadığı.
     *
     * Kullanıcı klasörü silebilir, SD kartı çıkarabilir veya sistem
     * ayarlarından izni geri alabilir. Bu durumda yedek sessizce
     * başarısız olmamalı; ayarlar ekranı uyarı gösteriyor.
     */
    fun hasFolderAccess(folderUri: String): Boolean

    /** Sıklığa göre periyodik işi kurar; saat 0 ise planlanmış işi iptal eder. */
    fun schedule(hours: Long)

    /** Hemen bir yedek almayı dener (klasör seçildiğinde ilk yedeği üretmek için). */
    fun runNow()
}
