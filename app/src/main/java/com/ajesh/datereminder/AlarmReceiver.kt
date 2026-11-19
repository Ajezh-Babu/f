package com.ajesh.datereminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1)
        val r = ReminderStore.find(ctx, id) ?: return
        if (r.done) return
        createChannel(ctx)

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(r.title)
            .setContentText("Reminder")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        val allowed = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(ctx).notify(id.toInt(), n)
        ReminderWidget.refresh(ctx)
    }

    companion object {
        const val CHANNEL = "reminders"
        fun createChannel(ctx: Context) {
            val ch = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Date and time reminders"
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
