package com.cnsoftware.synapse.share

/**
 * Sistem paylaşım menüsünü açar.
 * Android: Intent.ACTION_SEND, iOS: UIActivityViewController.
 */
interface ShareService {
    fun shareText(text: String)
}
