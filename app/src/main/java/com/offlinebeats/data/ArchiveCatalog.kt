package com.offlinebeats.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Internet Archive audio with a Creative Commons license declared in item metadata.
 * Declared licensing is not independently verified; always show source and terms.
 */
class ArchiveCatalog {
    fun search(text: String): List<CommonsAudio> {
        val query = text.trim().take(80)
        require(query.length >= 2) { "Enter at least 2 characters" }
        // Quote as a literal phrase; avoid injecting advanced search operators.
        val literal = query.replace(Regex("[^\\p{L}\\p{N} _.-]"), " ").trim()
        if (literal.length < 2) return emptyList()
        val expression = "mediatype:audio AND (title:(\"$literal\") OR creator:(\"$literal\"))"
        val url = "https://archive.org/advancedsearch.php?q=" + encode(expression) +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator" +
            "&fl%5B%5D=licenseurl&rows=25&output=json"
        val docs = json(url).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
        val songs = mutableListOf<CommonsAudio>()
        for (i in 0 until docs.length()) {
            val item = docs.optJSONObject(i) ?: continue
            val id = item.optString("identifier")
            if (!Regex("[A-Za-z0-9_-]{4,100}").matches(id)) continue
            val licenseUrl = first(item.opt("licenseurl"))
            val license = licenseFromUrl(licenseUrl) ?: continue
            val metadata = try {
                json("https://archive.org/metadata/" + id)
            } catch (_: Exception) { continue }
            val files = metadata.optJSONArray("files") ?: continue
            val creator = first(item.opt("creator")).ifBlank { "Creator not supplied" }
            val title = first(item.opt("title")).ifBlank { id }
            // Only downloadable original audio files, not thumbnails or derivatives.
            val audio = firstOriginal(files) ?: continue
            val filename = audio.optString("name")
            val extension = filename.substringAfterLast('.', "").lowercase()
            val mime = when (extension) {
                "mp3" -> "audio/mpeg"
                "ogg", "opus" -> "audio/ogg"
                "flac" -> "audio/flac"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "aac" -> "audio/aac"
                else -> continue
            }
            songs.add(CommonsAudio(
                id = id.hashCode().toLong(),
                name = title.take(120),
                creator = creator.take(180),
                mediaUrl = "https://archive.org/download/" + id + "/" + encodePath(filename),
                sourceUrl = "https://archive.org/details/" + id,
                mime = mime,
                license = license,
                licenseUrl = licenseUrl,
                provider = "Internet Archive"
            ))
            if (songs.size >= 15) break
        }
        return songs
    }

    private fun firstOriginal(files: JSONArray): JSONObject? {
        for (i in 0 until files.length()) {
            val f = files.optJSONObject(i) ?: continue
            if (!f.optString("source").equals("original", ignoreCase = true)) continue
            val ext = f.optString("name").substringAfterLast('.', "").lowercase()
            if (ext in setOf("mp3", "flac", "ogg", "opus", "m4a", "aac", "wav")) return f
        }
        return null
    }

    private fun licenseFromUrl(raw: String): String? {
        val license = raw.trimEnd('/').lowercase()
        val match = Regex("^https?://creativecommons\\.org/licenses/(by|by-sa)/(1\\.0|2\\.0|2\\.5|3\\.0|4\\.0)$").matchEntire(license)
        if (match != null) return "CC " + match.groupValues[1].uppercase() + " " + match.groupValues[2]
        if (Regex("^https?://creativecommons\\.org/publicdomain/zero/1\\.0$").matches(license)) return "CC0"
        return null
    }

    private fun first(v: Any?): String = when (v) {
        is String -> v
        is JSONArray -> v.optString(0)
        else -> ""
    }

    private fun json(rawUrl: String): JSONObject {
        val connection = URL(rawUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "OfflineBeats/0.3 (Android; https://github.com/kumarPint00/offline-beats)")
        try {
            require(connection.responseCode in 200..299) { "Internet Archive returned HTTP " + connection.responseCode }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    private fun encode(v: String) = URLEncoder.encode(v, "UTF-8")
    private fun encodePath(filename: String): String =
        filename.split('/').joinToString("/") { encode(it).replace("+", "%20") }
}
