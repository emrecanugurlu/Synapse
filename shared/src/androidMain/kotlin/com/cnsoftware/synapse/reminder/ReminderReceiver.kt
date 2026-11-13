package com.cnsoftware.synapse.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cnsoftware.synapse.shared.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Alarm çaldığında bildirimi gösterir.
 * Koin, SynapseApplication.onCreate'te (receiver'dan önce) başlatıldığı için inject güvenlidir.
 */
class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val reminders: ReminderCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
        // Aynı hatırlatıcı iki kez gösterilmesin (asıl alarm + açılışta
        // "kaçırılmış" diye yeniden kurulan). 0: eski sürümün kurduğu alarm,
        // zaman bilgisi taşımıyor; kayıt tutmadan gösteriliyor.
        if (triggerAt != 0L && !reminders.claimFiring(noteId, triggerAt)) return

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
            .ifBlank { context.getString(R.string.reminder_default_title) }
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.reminder_channel_description)
                },
            )
        }

        // Bildirime dokununca ilgili notu aç. Hedef Activity bu modülde değil;
        // sınıfı launcher intent'inden öğrenip ona açıkça yönlendiriyoruz.
        val activity = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component
        val contentIntent = activity?.let {
            PendingIntent.getActivity(
                context,
                noteId.hashCode(),
                Intent(ACTION_OPEN_NOTE)
                    .setComponent(it)
                    .putExtra(EXTRA_NOTE_ID, noteId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(noteId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val EXTRA_NOTE_ID = "noteId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"

        /** Alarmın ait olduğu hatırlatıcı zamanı; çift bildirimi ayıklamak için. */
        const val EXTRA_TRIGGER_AT = "triggerAt"

        /** Bildirime dokunulduğunda MainActivity'ye giden action; manifest'te de tanımlı. */
        const val ACTION_OPEN_NOTE = "com.cnsoftware.synapse.OPEN_NOTE"
    }
}
