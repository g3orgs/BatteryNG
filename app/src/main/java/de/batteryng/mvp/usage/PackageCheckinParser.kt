package de.batteryng.mvp.usage

import java.io.Reader

/** Parses the compact `dumpsys package --checkin packages` UID-to-package mapping. */
internal object PackageCheckinParser {
    data class Result(val packagesByUid: Map<Int, List<String>>, val permissionDenied: Boolean)

    fun parse(reader: Reader): Result {
        val packages = mutableMapOf<Int, MutableList<String>>()
        var currentPackage: String? = null
        var currentAppId: Int? = null
        var permissionDenied = false

        reader.buffered().forEachLine { line ->
            if (line.contains("Permission Denial", ignoreCase = true)) permissionDenied = true
            when {
                line.startsWith("pkg,") -> {
                    val fields = line.split(',', limit = 4)
                    currentPackage = fields.getOrNull(1)?.takeIf { it.isNotBlank() }
                    currentAppId = fields.getOrNull(2)?.toIntOrNull()
                }
                line.startsWith("pkg-usr,") -> {
                    val fields = line.split(',', limit = 4)
                    val userId = fields.getOrNull(1)?.toIntOrNull() ?: return@forEachLine
                    val installed = fields.getOrNull(2)?.startsWith('I') == true
                    val appId = currentAppId ?: return@forEachLine
                    val packageName = currentPackage ?: return@forEachLine
                    if (!installed || userId < 0 || appId < 10_000) return@forEachLine
                    val uid = userId.toLong() * 100_000L + appId
                    if (uid > Int.MAX_VALUE) return@forEachLine
                    packages.getOrPut(uid.toInt()) { mutableListOf() }.add(packageName)
                }
            }
        }
        return Result(
            packagesByUid = packages.mapValues { (_, names) -> names.distinct().sorted() },
            permissionDenied = permissionDenied,
        )
    }
}
