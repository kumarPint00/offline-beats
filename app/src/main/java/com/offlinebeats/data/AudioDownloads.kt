package com.offlinebeats.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class DownloadRow(
    val id: Long,
    val name: String,
    val source: String,
    val credit: String,
    val license: String,
    val licenseUrl: String,
    val createdAt: Long
)

class AudioDownloads(private val context: Context) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("download_history_v1", Context.MODE_PRIVATE)

    @Synchronized
    fun history(): List<DownloadRow> {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        val json = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        return (0 until json.length()).mapNotNull { i ->
            val item = json.optJSONObject(i) ?: return@mapNotNull null
            DownloadRow(item.optLong("id"), item.optString("name"), item.optString("source"),
                item.optString("credit"), item.optString("license"),
                item.optString("licenseUrl"), item.optLong("createdAt"))
        }.reversed()
    }

    fun saveCommons(track: CommonsAudio): Long {
        require(LicenseRules.allowed(track.license, track.licenseUrl, "true") ||
            LicenseRules.allowed(track.license, track.licenseUrl, "false")) {
            "This file is not marked with an accepted free license"
        }
        val uri = URI(track.mediaUrl)
        val commonsHost = uri.host == "upload.wikimedia.org" && track.provider == "Wikimedia Commons"
        val archiveHost = uri.host == "archive.org" &&
            uri.path.startsWith("/download/") && track.provider == "Internet Archive"
        require(uri.scheme == "https" && (commonsHost || archiveHost)) {
            "Unsupported audio provider or download host"
        }
        val extension = uri.path.substringAfterLast('.', "ogg").take(6)
        require(extension.lowercase() in setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav")) {
            "Unsupported audio format"
        }
        val filename = safeName(track.name) + "-" + System.currentTimeMillis() + "." + extension
        return enqueue(track.mediaUrl, filename, track.mime,
            DownloadRow(0, track.name, track.sourceUrl, track.creator,
                track.license, track.licenseUrl, System.currentTimeMillis()))
    }

    fun saveDirect(raw: String, rightsConfirmed: Boolean): Long {
        require(rightsConfirmed) { "Confirm that you have permission to download" }
        val info = LinkRules.direct(raw)
        val proposed = info.uri.path.substringAfterLast('/').substringBeforeLast('.')
        val name = safeName(proposed)
        val filename = name + "-" + System.currentTimeMillis() + "." + info.extension
        return enqueue(info.uri.toString(), filename, info.mime,
            DownloadRow(0, name, info.uri.toString(), "User-provided URL",
                "Permission confirmed by user", "", System.currentTimeMillis()))
    }

    @Synchronized
    private fun enqueue(url: String, filename: String, mime: String, entry: DownloadRow): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(entry.name)
            .setDescription("OfflineBeats • audio download")
            .setMimeType(mime)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "OfflineBeats/" + filename)
        val id = manager.enqueue(request)
        val entries = JSONArray(prefs.getString("entries", "[]"))
        entries.put(JSONObject().apply {
            put("id", id); put("name", entry.name); put("source", entry.source)
            put("credit", entry.credit); put("license", entry.license)
            put("licenseUrl", entry.licenseUrl); put("createdAt", entry.createdAt)
        })
        prefs.edit().putString("entries", entries.toString()).apply()
        return id
    }

    fun status(id: Long): String {
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = manager.query(query) ?: return "Unknown"
        cursor.use {
            if (!it.moveToFirst()) return "Missing"
            return when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_PENDING -> "Queued"
                DownloadManager.STATUS_RUNNING -> "Downloading"
                DownloadManager.STATUS_PAUSED -> "Paused"
                DownloadManager.STATUS_SUCCESSFUL -> "Saved"
                DownloadManager.STATUS_FAILED -> "Failed"
                else -> "Unknown"
            }
        }
    }

    fun openWithAudioApp(id: Long) {
        val uri = manager.getUriForDownloadedFile(id)
            ?: error("Download has not completed")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "audio/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open in Samsung Music or another app")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun safeName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9 _.-]"), "_").trim(' ', '.').take(70)
            .ifBlank { "audio" }
}
