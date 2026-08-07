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

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
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

        // Bildirime dokununca uygulamayı aç.
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                noteId.hashCode(),
                it,
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
    }
}
