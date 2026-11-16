package com.nathan.milestones.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A newer version found on GitHub. */
data class UpdateInfo(
    val version: String,     // "1.2"
    val notes: String,       // the release description
    val downloadUrl: String  // the .apk file (or the release page if no .apk is attached)
)

/**
 * Checks GitHub Releases for a newer version of the app.
 *
 * How it works: GitHub has a free public address that returns the newest release
 * of a repo as JSON. We compare its tag (e.g. "v1.2") with the installed version.
 *
 * If the repo is private, offline, or has no releases, this quietly returns null
 * (no popup), so making the repo private later won't break anything.
 */
object UpdateChecker {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/Tenzukun/Milestones/releases/latest"

    /** Must run in the background (it uses the internet). Returns null if already up to date. */
    fun check(currentVersion: String): UpdateInfo? {
        return try {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                // 404 = repo private or no releases yet
                if (connection.responseCode != 200) return null
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })

                val latest = json.getString("tag_name").removePrefix("v") // "v1.2" -> "1.2"
                if (!isNewer(latest, currentVersion)) return null

                // Find the attached .apk file
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            apkUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                }
                UpdateInfo(
                    version = latest,
                    notes = json.optString("body", ""),
                    downloadUrl = apkUrl ?: json.getString("html_url")
                )
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null // no internet, timeout, etc: just skip the check
        }
    }

    /**
     * Compares versions number by number: "1.10" is newer than "1.9", "1.2" is newer than "1.1.5".
     * Anything after a "-" is ignored, so "1.1-debug" counts as "1.1".
     */
    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore("-").split(".").map { it.trim().toIntOrNull() ?: 0 }
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}