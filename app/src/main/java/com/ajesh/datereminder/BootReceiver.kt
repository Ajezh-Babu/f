package com.ajesh.datereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms every alarm after reboot, time/timezone change, or when exact-alarm permission is granted. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(ctx)
        ReminderWidget.refresh(ctx)
    }
}
