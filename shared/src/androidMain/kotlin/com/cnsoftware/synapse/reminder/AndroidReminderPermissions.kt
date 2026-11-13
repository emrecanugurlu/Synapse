package com.cnsoftware.synapse.reminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Bildirim izni istemek için Activity köprüsü.
 *
 * İzin diyaloğu `registerForActivityResult` ile Activity kurulurken kaydedilmek
 * zorunda; dosya seçicideki `AndroidFilePicker` ile aynı sebep.
 */
object AndroidNotificationPermission {

    interface Host {
        /** İzin diyaloğunu gösterir; kullanıcı izin verdiyse true döner. */
        suspend fun requestNotificationPermission(): Boolean
    }

    /** MainActivity `onCreate`'te set eder, `onDestroy`'da temizler. */
    var host: Host? = null
}

class AndroidReminderPermissions(private val context: Context) : ReminderPermissions {

    override suspend fun ensureNotificationsAllowed(): Boolean {
        if (notificationsEnabled()) return true
        // Android 12 ve öncesinde çalışma zamanı izni yok; bildirimler
        // kapalıysa bunu yalnızca kullanıcı ayarlardan açabilir.
        if (Build.VERSION.SDK_INT < 33) return false
        val host = AndroidNotificationPermission.host ?: return false
        return host.requestNotificationPermission() && notificationsEnabled()
    }

    private fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())
        }
        launch(intent)
    }

    override fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) return
        launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri()))
    }

    private fun packageUri(): Uri = Uri.fromParts("package", context.packageName, null)

    private fun launch(intent: Intent) {
        // Uygulama bağlamından başlatıldığı için NEW_TASK şart. Bazı cihazlarda
        // ilgili ayar ekranı bulunmayabiliyor; o durumda sessizce vazgeçiyoruz.
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
