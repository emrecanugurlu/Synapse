package com.cnsoftware.synapse.share

import android.content.Context
import android.content.Intent

class AndroidShareService(private val context: Context) : ShareService {

    override fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, null).apply {
            // Application context'ten başlatıldığı için gerekli.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
