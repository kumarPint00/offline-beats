package com.offlinebeats.data

import java.net.URI

enum class LinkKind { EMPTY, DIRECT_AUDIO, YOUTUBE, WEB_PAGE, INVALID }

data class DirectAudio(val uri: URI, val extension: String, val mime: String)

object LinkRules {
    private val mimes = mapOf(
        "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "aac" to "audio/aac",
        "flac" to "audio/flac", "ogg" to "audio/ogg", "opus" to "audio/ogg",
        "wav" to "audio/wav"
    )
    fun classify(raw: String): LinkKind {
        if (raw.isBlank()) return LinkKind.EMPTY
        val url = try { URI(raw.trim()) } catch (_: Exception) { return LinkKind.INVALID }
        if (url.scheme?.lowercase() != "https" || url.host.isNullOrBlank() ||
            url.userInfo != null) return LinkKind.INVALID
        val host = url.host.lowercase()
        if (host == "youtube.com" || host.endsWith(".youtube.com") ||
            host == "youtu.be" || host == "youtube-nocookie.com" ||
            host.endsWith(".youtube-nocookie.com")) return LinkKind.YOUTUBE
        val extension = url.path.substringAfterLast('.', "").lowercase()
        return if (mimes.containsKey(extension)) LinkKind.DIRECT_AUDIO else LinkKind.WEB_PAGE
    }
    fun direct(raw: String): DirectAudio {
        require(classify(raw) == LinkKind.DIRECT_AUDIO) {
            "Use a direct HTTPS audio URL ending in .mp3, .m4a, .aac, .flac, .ogg, .opus, or .wav"
        }
        val uri = URI(raw.trim())
        val ext = uri.path.substringAfterLast('.').lowercase()
        return DirectAudio(uri, ext, mimes.getValue(ext))
    }
}
