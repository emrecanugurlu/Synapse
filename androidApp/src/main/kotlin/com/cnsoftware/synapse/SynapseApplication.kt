package com.cnsoftware.synapse

import android.app.Application
import com.cnsoftware.synapse.di.initKoin
import com.cnsoftware.synapse.di.platformModule
import com.cnsoftware.synapse.reminder.ReminderCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SynapseApplication : Application(), KoinComponent {

    private val reminders: ReminderCoordinator by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin(platformModule(applicationContext))

        // Uygulama "zorla durdur" ile kapatıldıysa sistem alarmları silmiştir ve
        // bunun için bir yayın gelmez; her süreç başlangıcında yeniden kuruyoruz.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            // Hatırlatıcılar yardımcı bir özellik; buradaki bir hata
            // uygulamanın açılışını düşürmemeli.
            runCatching { reminders.onStartup() }
        }
    }
}
