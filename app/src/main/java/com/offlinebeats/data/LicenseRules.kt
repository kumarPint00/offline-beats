package com.offlinebeats.data

import java.net.URI

/** Conservative allow-list: explicitly free Commons licenses only. */
object LicenseRules {
    fun allowed(labelRaw: String, urlRaw: String, copyrighted: String): Boolean {
        val label = labelRaw.trim().lowercase()
        if (label == "public domain" || label == "public domain mark") {
            return copyrighted.equals("false", ignoreCase = true)
        }
        val url = try { URI(urlRaw.trim()) } catch (_: Exception) { return false }
        if (url.scheme != "https" && url.scheme != "http") return false
        if (!url.host.equals("creativecommons.org", ignoreCase = true)) return false
        val path = url.path.trimEnd('/').lowercase()
        return Regex("^/publicdomain/zero/1\\.0$").matches(path) ||
            Regex("^/licenses/(by|by-sa)/(1\\.0|2\\.0|2\\.5|3\\.0|4\\.0)$").matches(path)
    }
}
