package com.cnsoftware.synapse

import android.app.Application
import com.cnsoftware.synapse.di.initKoin
import com.cnsoftware.synapse.di.platformModule

class SynapseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(platformModule(applicationContext))
    }
}
