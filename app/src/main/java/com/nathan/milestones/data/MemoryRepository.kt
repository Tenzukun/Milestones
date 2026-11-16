package com.nathan.milestones.data

import android.content.Context
import android.net.Uri
import com.nathan.milestones.model.Memory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Saves memories as JSON (like CounterRepository) and keeps their photos
 * in the app's private folder, so they stay even if you delete them from your gallery.
 */
class MemoryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("memories", Context.MODE_PRIVATE)

    fun getAll(): List<Memory> {
        val raw = prefs.getString(KEY_MEMORIES, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            runCatching { fromJson(array.getJSONObject(i)) }.getOrNull()
        }
    }

    /** All memories for one date, newest first. */
    fun getFor(counterId: String): List<Memory> =
        getAll().filter { it.counterId == counterId }.sortedByDescending { it.date }

    /** Adds or replaces a memory. If its photo changed, the old photo file is deleted. */
    fun save(memory: Memory) {
        val list = getAll().toMutableList()
        val index = list.indexOfFirst { it.id == memory.id }
        if (index >= 0) {
            val oldPhoto = list[index].photoPath
            if (oldPhoto != null && oldPhoto != memory.photoPath) File(oldPhoto).delete()
            list[index] = memory
        } else {
            list.add(memory)
        }
        write(list)
    }

    fun delete(memory: Memory) {
        memory.photoPath?.let { File(it).delete() }
        write(getAll().filterNot { it.id == memory.id })
    }

    /** Used when a whole date is deleted: removes its memories and their photos. */
    fun deleteAllFor(counterId: String) {
        val all = getAll()
        all.filter { it.counterId == counterId }.forEach { m -> m.photoPath?.let { File(it).delete() } }
        write(all.filterNot { it.counterId == counterId })
    }

    /**
     * Copies a photo picked from the gallery into the app's private storage.
     * Returns the new file path, or null if it couldn't be copied.
     */
    fun copyPhoto(uri: Uri, memoryId: String): String? {
        return try {
            val dir = File(appContext.filesDir, "memories").apply { mkdirs() }
            val file = File(dir, "$memoryId-${System.currentTimeMillis()}.jpg")
            val input = appContext.contentResolver.openInputStream(uri) ?: return null
            input.use { i -> file.outputStream().use { o -> i.copyTo(o) } }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun write(list: List<Memory>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_MEMORIES, array.toString()).apply()
    }

    private fun toJson(m: Memory) = JSONObject().apply {
        put("id", m.id)
        put("counterId", m.counterId)
        put("date", m.date.toString())
        put("title", m.title)
        put("note", m.note)
        put("photoPath", m.photoPath ?: "")
    }

    private fun fromJson(o: JSONObject) = Memory(
        id = o.getString("id"),
        counterId = o.getString("counterId"),
        date = LocalDate.parse(o.getString("date")),
        title = o.getString("title"),
        note = o.optString("note", ""),
        photoPath = o.optString("photoPath", "").ifEmpty { null }
    )

    companion object {
        private const val KEY_MEMORIES = "memories"
    }
}