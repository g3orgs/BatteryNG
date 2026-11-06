package de.batteryng.mvp

import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Fetches the latest published GitHub release only when the user requests it. */
internal object GitHubReleases {
    const val PAGE_URL = "https://github.com/g3orgs/BatteryNG/releases"
    private const val API_URL = "https://api.github.com/repos/g3orgs/BatteryNG/releases/latest"

    fun latestVersion(): String {
        val connection = (URL(API_URL).openConnection() as HttpsURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BatteryNG")
        }
        try {
            if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("GitHub returned HTTP ${connection.responseCode}")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(response).getString("tag_name").removePrefix("v")
        } finally {
            connection.disconnect()
        }
    }
}

/** Returns positive when [left] is newer, negative when [right] is newer. */
internal object ReleaseVersions {
    fun compare(left: String, right: String): Int? {
        val leftParts = parse(left) ?: return null
        val rightParts = parse(right) ?: return null
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val difference = (leftParts.getOrNull(index) ?: 0)
                .compareTo(rightParts.getOrNull(index) ?: 0)
            if (difference != 0) return difference
        }
        return 0
    }

    private fun parse(version: String): List<Int>? {
        val normalized = version.removePrefix("v")
        if (normalized.isEmpty()) return null
        return normalized.split('.').map { part ->
            if (part.isEmpty() || !part.all(Char::isDigit)) return null
            part.toIntOrNull() ?: return null
        }
    }
}
