package com.namazvakti.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class RemoteReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String,
    val apkSizeBytes: Long
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: RemoteReleaseInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String, val latestTag: String?) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

data class UpdateUiState(
    val availableRelease: RemoteReleaseInfo? = null,
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedApkPath: String? = null,
    val awaitingInstallPermission: Boolean = false
) {
    val progressFraction: Float?
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else null
}

object GitHubUpdateManager {
    const val DEVELOPER_NAME = "Hasan Ali Yücel"
    const val GITHUB_OWNER = "haliyucel"
    const val GITHUB_REPO = "namaz-vakitleri"
    const val GITHUB_REPO_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = openConnection(LATEST_RELEASE_API, acceptJson = true)
            try {
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    return@withContext UpdateCheckResult.UpToDate(currentVersion, null)
                }
                if (code !in 200..299) {
                    return@withContext UpdateCheckResult.Error(
                        "Güncelleme sunucusu yanıt vermedi (HTTP $code)."
                    )
                }
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                if (tagName.isBlank()) {
                    return@withContext UpdateCheckResult.UpToDate(currentVersion, null)
                }

                if (!UpdateVersionComparator.isNewer(tagName, currentVersion)) {
                    return@withContext UpdateCheckResult.UpToDate(
                        currentVersion = currentVersion,
                        latestTag = UpdateVersionComparator.normalizeDisplayTag(tagName)
                    )
                }

                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                var apkName = "NamazVakitleri-$tagName.apk"
                var apkSize = 0L
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "").takeIf { it.isNotBlank() }
                            apkName = name
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                val release = RemoteReleaseInfo(
                    tagName = UpdateVersionComparator.normalizeDisplayTag(tagName),
                    title = json.optString("name", "").ifBlank { "Yeni Sürüm $tagName" },
                    releaseNotes = json.optString("body", "").trim(),
                    htmlUrl = json.optString("html_url", "$GITHUB_REPO_URL/releases/latest"),
                    apkDownloadUrl = apkUrl,
                    apkFileName = apkName,
                    apkSizeBytes = apkSize
                )
                UpdateCheckResult.UpdateAvailable(release)
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            UpdateCheckResult.Error("Güncelleme kontrol edilemedi. İnternet bağlantınızı kontrol edin.")
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: RemoteReleaseInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val downloadUrl = release.apkDownloadUrl
            ?: return@withContext Result.failure(IllegalStateException("Bu sürümde APK dosyası bulunamadı."))

        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { it.delete() }

            val safeName = release.apkFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val tempFile = File(updatesDir, "$safeName.part")
            val finalFile = File(updatesDir, safeName)

            val conn = openConnectionWithRedirects(downloadUrl, acceptJson = false)
            try {
                if (conn.responseCode !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException("APK indirilemedi (HTTP ${conn.responseCode}).")
                    )
                }

                val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: release.apkSizeBytes
                var downloaded = 0L
                val buffer = ByteArray(16 * 1024)

                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        var read = input.read(buffer)
                        var lastReport = 0L
                        while (read != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 120L) {
                                lastReport = now
                                onProgress(downloaded, totalBytes)
                            }
                            read = input.read(buffer)
                        }
                        output.flush()
                    }
                }
                onProgress(downloaded, totalBytes.coerceAtLeast(downloaded))

                if (tempFile.length() <= 0L) {
                    tempFile.delete()
                    return@withContext Result.failure(IllegalStateException("İndirilen APK dosyası boş."))
                }

                if (finalFile.exists()) finalFile.delete()
                if (!tempFile.renameTo(finalFile)) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
                Result.success(finalFile)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("İndirme hatası: ${e.localizedMessage ?: "Bilinmeyen hata"}"))
        }
    }

    fun canInstallPackages(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun createInstallPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )

    fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openConnection(urlStr: String, acceptJson: Boolean): HttpURLConnection {
        return (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "NamazVakitleri-Android")
            if (acceptJson) {
                setRequestProperty("Accept", "application/vnd.github+json")
            }
        }
    }

    private fun openConnectionWithRedirects(startUrl: String, acceptJson: Boolean): HttpURLConnection {
        var currentUrl = startUrl
        repeat(6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 12_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "NamazVakitleri-Android")
                if (acceptJson) {
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
            }
            val code = conn.responseCode
            if (code in setOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Yönlendirme adresi alınamadı.")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Çok fazla yönlendirme oluştu.")
    }
}
