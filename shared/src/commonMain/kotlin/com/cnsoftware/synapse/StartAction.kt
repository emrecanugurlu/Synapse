package com.cnsoftware.synapse

/**
 * Uygulama dışından gelen açılış isteği (launcher kısayolu, paylaş menüsü).
 *
 * Platform katmanı intent'i buna çevirir; `App` bunu bir kez tüketip ilgili
 * ekranı açar. Ortak modülde duruyor ki iOS tarafı da (Share Extension,
 * Home Screen Quick Action) aynı sözleşmeyi kullanabilsin.
 */
sealed interface StartAction {
    /** Doğrudan boş bir not editörü aç. */
    data object NewNote : StartAction

    /** Doğrudan boş bir liste editörü aç. */
    data object NewChecklist : StartAction

    /** Başka bir uygulamadan paylaşılan metinle yeni not aç. */
    data class ShareText(val title: String, val text: String) : StartAction
}
