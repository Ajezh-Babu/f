package com.ajesh.datereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    const val EXTRA_ID = "reminder_id"

    private fun pending(ctx: Context, id: Long): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            ctx, (id % Int.MAX_VALUE).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    fun schedule(ctx: Context, r: Reminder) {
        if (r.done || r.timeMillis <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, r.id)
        if (canScheduleExact(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.timeMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.timeMillis, pi) // fallback (not exact)
        }
    }

    fun cancel(ctx: Context, id: Long) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx, id))
    }

    fun rescheduleAll(ctx: Context) {
        ReminderStore.load(ctx).forEach { schedule(ctx, it) }
    }
}
