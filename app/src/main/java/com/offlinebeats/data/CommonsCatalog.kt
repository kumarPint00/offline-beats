package com.offlinebeats.data

import android.text.Html
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CommonsAudio(
    val id: Long,
    val name: String,
    val creator: String,
    val mediaUrl: String,
    val sourceUrl: String,
    val mime: String,
    val license: String,
    val licenseUrl: String,
    val provider: String = "Wikimedia Commons"
)

class CommonsCatalog {
    fun search(text: String): List<CommonsAudio> {
        val q = text.trim().take(80)
        require(q.length >= 2) { "Enter at least 2 characters" }
        val query = URLEncoder.encode(q + " filetype:audio", "UTF-8")
        val api = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&formatversion=2&generator=search&gsrsearch=" + query +
            "&gsrnamespace=6&gsrlimit=75&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&iilimit=1"
        val conn = URL(api).openConnection() as HttpURLConnection
        conn.connectTimeout = 12000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "OfflineBeats/0.3 (Android; https://github.com/kumarPint00/offline-beats)")
        try {
            require(conn.responseCode in 200..299) { "Catalog HTTP " + conn.responseCode }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val pages = json.optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()
            val songs = mutableListOf<CommonsAudio>()
            for (i in 0 until pages.length()) {
                val page = pages.optJSONObject(i) ?: continue
                val image = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val media = image.optString("url")
                val mime = image.optString("mime")
                if (!media.startsWith("https://upload.wikimedia.org/") ||
                    !(mime.startsWith("audio/") || mime == "application/ogg")) continue
                val ext = image.optJSONObject("extmetadata") ?: continue
                fun meta(name: String): String = ext.optJSONObject(name)?.optString("value").orEmpty()
                val license = meta("LicenseShortName")
                val licenseUrl = meta("LicenseUrl")
                if (!LicenseRules.allowed(license, licenseUrl, meta("Copyrighted"))) continue
                val creator = Html.fromHtml(meta("Artist"), Html.FROM_HTML_MODE_LEGACY).toString()
                    .replace(Regex("\\s+"), " ").trim().take(160).ifBlank { "Creator unspecified" }
                songs.add(CommonsAudio(
                    id = page.optLong("pageid"),
                    name = page.optString("title").removePrefix("File:").substringBeforeLast('.'),
                    creator = creator,
                    mediaUrl = media,
                    sourceUrl = image.optString("descriptionurl").takeIf { it.startsWith("https://") }
                        ?: "https://commons.wikimedia.org/",
                    mime = mime,
                    license = license,
                    licenseUrl = licenseUrl
                ))
            }
            return songs.distinctBy { it.id }
        } finally {
            conn.disconnect()
        }
    }
}
