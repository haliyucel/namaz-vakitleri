package com.namazvakti.app.update

object UpdateVersionComparator {

    fun parseVersionParts(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        if (cleaned.isBlank()) return emptyList()
        return cleaned.split(".").map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote = parseVersionParts(remoteTag)
        val current = parseVersionParts(currentVersion)
        if (remote.isEmpty() || current.isEmpty()) return false
        val maxLen = maxOf(remote.size, current.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    fun normalizeDisplayTag(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("v", ignoreCase = true)) trimmed else "v$trimmed"
    }
}
