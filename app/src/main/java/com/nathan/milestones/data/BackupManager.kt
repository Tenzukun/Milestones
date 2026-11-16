package com.nathan.milestones.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup = one .zip file:
 *   backup.json      -> all dates + memories
 *   photos/xxx.jpg   -> every memory photo
 */
object BackupManager {

    private const val BACKUP_JSON = "backup.json"
    private const val PHOTO_PREFIX = "photos/"

    /** Saves everything into the .zip at [uri]. Returns (number of dates, number of memories). */
    fun export(context: Context, uri: Uri): Pair<Int, Int> {
        val app = context.applicationContext
        val counters = JSONArray(prefs(app, "milestones").getString("counters", null) ?: "[]")
        val memories = JSONArray(prefs(app, "memories").getString("memories", null) ?: "[]")

        val output = app.contentResolver.openOutputStream(uri) ?: throw IOException("Couldn't open the file")
        ZipOutputStream(output).use { zip ->
            val memoriesForBackup = JSONArray()
            for (i in 0 until memories.length()) {
                val m = JSONObject(memories.getJSONObject(i).toString()) // a copy we can change
                val path = m.optString("photoPath", "")
                val photo = File(path)
                if (path.isNotEmpty() && photo.exists()) {
                    // Put the photo in the zip and point the memory at it
                    zip.putNextEntry(ZipEntry(PHOTO_PREFIX + photo.name))
                    photo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    m.put("photoPath", PHOTO_PREFIX + photo.name)
                } else {
                    m.put("photoPath", "")
                }
                memoriesForBackup.put(m)
            }

            val json = JSONObject().apply {
                put("app", "Milestones")
                put("version", 1)
                put("counters", counters)
                put("memories", memoriesForBackup)
            }
            zip.putNextEntry(ZipEntry(BACKUP_JSON))
            zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return counters.length() to memories.length()
    }

    /**
     * Replaces everything in the app with the backup at [uri].
     * It checks the whole file first, so a broken file can't wipe your current data.
     * Returns (number of dates, number of memories).
     */
    fun restore(context: Context, uri: Uri): Pair<Int, Int> {
        val app = context.applicationContext
        val tempDir = File(app.filesDir, "restore_tmp").apply { deleteRecursively(); mkdirs() }
        try {
            // 1. Unpack into a temporary folder
            var jsonText: String? = null
            val input = app.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open the file")
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == BACKUP_JSON) {
                        jsonText = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (entry.name.startsWith(PHOTO_PREFIX) && !entry.isDirectory) {
                        val safeName = File(entry.name).name // ignores any "../" tricks in the file name
                        File(tempDir, safeName).outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }

            // 2. Check it's really a Milestones backup
            val json = JSONObject(jsonText ?: throw IOException("This isn't a Milestones backup"))
            if (json.optString("app") != "Milestones") throw IOException("This isn't a Milestones backup")
            val counters = json.getJSONArray("counters")
            val memories = json.getJSONArray("memories")

            // 3. Everything looks good: swap in the restored photos
            val photosDir = File(app.filesDir, "memories")
            photosDir.deleteRecursively()
            if (!tempDir.renameTo(photosDir)) throw IOException("Couldn't save the photos")

            // 4. Point each memory at its photo's new location
            for (i in 0 until memories.length()) {
                val m = memories.getJSONObject(i)
                val p = m.optString("photoPath", "")
                m.put(
                    "photoPath",
                    if (p.startsWith(PHOTO_PREFIX)) File(photosDir, File(p).name).absolutePath else ""
                )
            }

            // 5. Save the dates and memories
            prefs(app, "milestones").edit().putString("counters", counters.toString()).apply()
            prefs(app, "memories").edit().putString("memories", memories.toString()).apply()
            return counters.length() to memories.length()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun prefs(context: Context, name: String) =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
}