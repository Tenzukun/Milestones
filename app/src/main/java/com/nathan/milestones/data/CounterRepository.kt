package com.nathan.milestones.data

import android.content.Context
import com.nathan.milestones.model.CountMode
import com.nathan.milestones.model.CountUnit
import com.nathan.milestones.model.Counter
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Saves counters on the phone using SharedPreferences (stored as JSON).
 * Also remembers which counter each home screen widget shows.
 */
class CounterRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("milestones", Context.MODE_PRIVATE)

    fun getAll(): List<Counter> {
        val raw = prefs.getString(KEY_COUNTERS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            runCatching { fromJson(array.getJSONObject(i)) }.getOrNull()
        }
    }

    fun get(id: String): Counter? = getAll().firstOrNull { it.id == id }

    /** Adds a new counter or replaces the one with the same id. */
    fun save(counter: Counter) {
        val list = getAll().toMutableList()
        val index = list.indexOfFirst { it.id == counter.id }
        if (index >= 0) list[index] = counter else list.add(counter)
        write(list)
    }

    fun delete(id: String) {
        write(getAll().filterNot { it.id == id })
    }

    fun setWidgetCounter(widgetId: Int, counterId: String) {
        prefs.edit().putString(widgetKey(widgetId), counterId).apply()
    }

    fun getWidgetCounter(widgetId: Int): Counter? =
        prefs.getString(widgetKey(widgetId), null)?.let { get(it) }

    fun removeWidget(widgetId: Int) {
        prefs.edit().remove(widgetKey(widgetId)).apply()
    }

    private fun write(list: List<Counter>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_COUNTERS, array.toString()).apply()
    }

    private fun toJson(c: Counter) = JSONObject().apply {
        put("id", c.id)
        put("title", c.title)
        put("start", c.startDate.toString()) // ISO format, e.g. 2026-09-13
        put("mode", c.mode.name)
        put("unit", c.unit.name)
        put("customMessage", c.customMessage)
        put("notify", c.notify)
        put("notifyDayBefore", c.notifyDayBefore)
        put("emoji", c.emoji)
        put("colorIndex", c.colorIndex)
    }

    private fun fromJson(o: JSONObject) = Counter(
        id = o.getString("id"),
        title = o.getString("title"),
        startDate = LocalDate.parse(o.getString("start")),
        mode = CountMode.valueOf(o.getString("mode")),
        unit = CountUnit.valueOf(o.getString("unit")),
        // opt... = use a default value if the field is missing (older saved dates)
        customMessage = o.optString("customMessage", ""),
        notify = o.optBoolean("notify", true),
        notifyDayBefore = o.optBoolean("notifyDayBefore", false),
        emoji = o.optString("emoji", "❤️"),
        colorIndex = o.optInt("colorIndex", 0)
    )

    /** Used so each daily notification check only runs once per day. */
    fun getLastNotifyCheck(): String? = prefs.getString(KEY_LAST_NOTIFY, null)

    fun setLastNotifyCheck(date: String) {
        prefs.edit().putString(KEY_LAST_NOTIFY, date).apply()
    }

    private fun widgetKey(widgetId: Int) = "widget_$widgetId"

    companion object {
        private const val KEY_COUNTERS = "counters"
        private const val KEY_LAST_NOTIFY = "last_notify_check"
    }
}