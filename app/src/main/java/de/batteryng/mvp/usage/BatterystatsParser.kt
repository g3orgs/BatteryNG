package de.batteryng.mvp.usage

import java.io.Reader

/** Reads only the on-battery component totals from `dumpsys batterystats --usage --accumulated`. */
internal object BatterystatsParser {
    private val uidLine = Regex("^\\s*UID\\s+([^:]+):\\s+([0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)")
    private val component = Regex("(?:^|\\s)([A-Za-z_][A-Za-z0-9_]*)=([0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)")
    private val appUid = Regex("u([0-9]+)a([0-9]+)")
    private val rawUid = Regex("[0-9]+")

    data class Result(
        val byUidMah: Map<Int, Double>,
        val foundUsage: Boolean,
        val permissionDenied: Boolean,
    )

    fun parse(reader: Reader): Result {
        val result = LinkedHashMap<Int, Double>()
        var foundUsage = false
        var currentUid: Int? = null
        var permissionDenied = false

        reader.buffered().forEachLine { line ->
            if (line.contains("Permission Denial", ignoreCase = true)) permissionDenied = true
            if (line.contains("Estimated power use (mAh):")) {
                foundUsage = true
                currentUid = null
                return@forEachLine
            }
            if (!foundUsage) return@forEachLine

            val header = uidLine.find(line)
            if (header != null) {
                currentUid = decodeAppUid(header.groupValues[1])
                if (currentUid != null) result.putIfAbsent(currentUid!!, 0.0)
                return@forEachLine
            }

            val uid = currentUid ?: return@forEachLine
            val trimmed = line.trimStart()
            if (!trimmed.startsWith("(on battery,")) return@forEachLine

            // Tokens such as cpu:fg=... are sub-components of cpu=... and must not be added.
            val totalMah = component.findAll(trimmed).sumOf { match ->
                match.groupValues[2].toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            }
            result[uid] = (result[uid] ?: 0.0) + totalMah
        }
        return Result(result, foundUsage, permissionDenied)
    }

    /** Android's formatUid: u10a150 is user 10, appId 10150. */
    private fun decodeAppUid(token: String): Int? {
        appUid.matchEntire(token)?.let { match ->
            val userId = match.groupValues[1].toIntOrNull() ?: return null
            val appNumber = match.groupValues[2].toIntOrNull() ?: return null
            val uid = userId.toLong() * 100_000L + 10_000L + appNumber
            return uid.toInt().takeIf { uid <= Int.MAX_VALUE }
        }
        return rawUid.matchEntire(token)?.value?.toIntOrNull()?.takeIf { it % 100_000 >= 10_000 }
    }
}
