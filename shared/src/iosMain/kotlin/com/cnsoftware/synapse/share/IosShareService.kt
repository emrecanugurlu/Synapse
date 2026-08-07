package com.cnsoftware.synapse.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosShareService : ShareService {

    override fun shareText(text: String) {
        dispatch_async(dispatch_get_main_queue()) {
            val rootViewController =
                UIApplication.sharedApplication.keyWindow?.rootViewController ?: return@dispatch_async
            val activityController = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null,
            )
            // En üstteki sunulan controller'ı bul (editör tam ekran modal olabilir).
            var presenter = rootViewController
            while (presenter.presentedViewController != null) {
                presenter = presenter.presentedViewController!!
            }
            presenter.presentViewController(activityController, animated = true, completion = null)
        }
    }
}
