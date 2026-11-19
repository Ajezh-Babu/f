package com.ajesh.datereminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderStore {
    private const val PREFS = "reminders"
    private const val KEY = "list"

    @Synchronized
    fun load(ctx: Context): List<Reminder> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Reminder(o.getLong("id"), o.getString("title"), o.getLong("time"), o.optBoolean("done"))
        }.sortedBy { it.timeMillis }
    }

    @Synchronized
    private fun save(ctx: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title)
                .put("time", it.timeMillis).put("done", it.done))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
        ReminderWidget.refresh(ctx)
    }

    fun add(ctx: Context, title: String, timeMillis: Long): Reminder {
        val r = Reminder(System.currentTimeMillis(), title, timeMillis)
        save(ctx, load(ctx) + r)
        AlarmScheduler.schedule(ctx, r)
        return r
    }

    fun setDone(ctx: Context, id: Long, done: Boolean) {
        val list = load(ctx).map { if (it.id == id) it.copy(done = done) else it }
        save(ctx, list)
        list.firstOrNull { it.id == id }?.let {
            if (done) AlarmScheduler.cancel(ctx, it.id) else AlarmScheduler.schedule(ctx, it)
        }
    }

    fun delete(ctx: Context, id: Long) {
        AlarmScheduler.cancel(ctx, id)
        save(ctx, load(ctx).filter { it.id != id })
    }

    fun find(ctx: Context, id: Long): Reminder? = load(ctx).firstOrNull { it.id == id }
}
