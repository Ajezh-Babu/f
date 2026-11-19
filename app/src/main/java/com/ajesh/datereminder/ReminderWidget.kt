package com.ajesh.datereminder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    companion object {
        private val fmt = DateTimeFormatter.ofPattern("d MMM, h:mm a")
        private val rows = intArrayOf(R.id.r1, R.id.r2, R.id.r3, R.id.r4, R.id.r5)

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            mgr.getAppWidgetIds(ComponentName(ctx, ReminderWidget::class.java))
                .forEach { update(ctx, mgr, it) }
        }

        private fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_reminders)
            val now = System.currentTimeMillis()
            val upcoming = ReminderStore.load(ctx).filter { !it.done && it.timeMillis >= now - 60_000 }

            rows.forEachIndexed { i, rid ->
                val r = upcoming.getOrNull(i)
                val text = when {
                    r != null -> "${Instant.ofEpochMilli(r.timeMillis).atZone(ZoneId.systemDefault()).format(fmt)}  •  ${r.title}"
                    i == 0 -> "No upcoming reminders"
                    else -> ""
                }
                v.setTextViewText(rid, text)
            }

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val addIntent = Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_ADD, true)
            v.setOnClickPendingIntent(R.id.widget_add, PendingIntent.getActivity(ctx, 1, addIntent, flags))
            v.setOnClickPendingIntent(R.id.widget_root,
                PendingIntent.getActivity(ctx, 2, Intent(ctx, MainActivity::class.java), flags))
            mgr.updateAppWidget(id, v)
        }
    }
}
