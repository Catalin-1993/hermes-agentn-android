package com.mobilefork.hermesagent.update

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** One published build that is newer than the installed one. */
data class AppRelease(
    val tag: String,
    val version: String,
    val notes: String,
    val pageUrl: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    /** Lowercase hex SHA-256 the release publishes for the APK; null when it publishes none. */
    val sha256: String?,
)

sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Available(val release: AppRelease) : UpdateCheckResult
    /** The repository has no release with an installable APK yet. */
    data object NoRelease : UpdateCheckResult
}

enum class UpdateError { InvalidSource, RateLimited, NoConnection, Http, Unreadable, NoChecksum, ChecksumMismatch, NotGitHub, Interrupted, Insecure }

class UpdateException(val kind: UpdateError, message: String, cause: Throwable? = null) : IOException(message, cause)

/** Reading GitHub's release JSON and comparing versions. No network, so it is tested on its own. */
object AppUpdateParser {
    private val repoPattern = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    private val sha256Pattern = Regex("^[0-9a-f]{64}$")

    fun isValidRepo(value: String): Boolean = repoPattern.matches(value.trim())

    /** Numbers of a version, so `v0.13.161` and `0.13.161-lab` both read as 0, 13, 161. */
    fun versionParts(version: String): List<Int> =
        Regex("\\d+").findAll(version.trim().removePrefix("v").substringBefore('-').substringBefore('+'))
            .map { it.value.toIntOrNull() ?: 0 }
            .toList()

    /** Negative when [a] is older than [b], zero when equal, positive when newer. */
    fun compareVersions(a: String, b: String): Int {
        val left = versionParts(a)
        val right = versionParts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    /**
     * Picks the APK people install: the universal release build. Play, benchmark and debug builds
     * are never offered, and an APK needs a published checksum file or digest next to it.
     */
    fun chooseApk(assets: List<ReleaseAsset>): ReleaseAsset? {
        val candidates = assets.filter { asset ->
            val name = asset.name.lowercase()
            name.endsWith(".apk") && listOf("play", "benchmark", "debug", "unsigned").none { it in name }
        }
        return candidates.firstOrNull { "universal" in it.name.lowercase() } ?: candidates.firstOrNull()
    }

    data class ReleaseAsset(val name: String, val url: String, val size: Long, val digest: String?)

    fun parseAssets(root: JSONObject): List<ReleaseAsset> {
        val array = root.optJSONArray("assets") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name")
            val url = item.optString("browser_download_url")
            if (name.isBlank() || url.isBlank()) return@mapNotNull null
            ReleaseAsset(name, url, item.optLong("size", 0L), item.optString("digest").takeIf { it.isNotBlank() })
        }
    }

    /** The hex hash from GitHub's `sha256:<hex>` digest, or from a `<hex>  <file>` checksum file. */
    fun parseSha256(text: String?): String? {
        val raw = text?.trim().orEmpty().removePrefix("sha256:")
        val token = raw.split(Regex("\\s+")).firstOrNull()?.lowercase().orEmpty()
        return token.takeIf { sha256Pattern.matches(it) }
    }

    fun parseRelease(root: JSONObject, checksumFor: (ReleaseAsset) -> String?): AppRelease? {
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
        val tag = root.optString("tag_name").ifBlank { return null }
        val apk = chooseApk(parseAssets(root)) ?: return null
        val sha = parseSha256(apk.digest) ?: parseSha256(checksumFor(apk))
        return AppRelease(
            tag = tag,
            version = tag.removePrefix("v"),
            notes = root.optString("body").trim(),
            pageUrl = root.optString("html_url"),
            apkName = apk.name,
            apkUrl = apk.url,
            apkSizeBytes = apk.size,
            sha256 = sha,
        )
    }
}

/** Looks for a newer release on GitHub and downloads it after checking its hash. */
class AppUpdateClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val apiBase: String = "https://api.github.com",
    /** Where an APK may be downloaded from. Only tests change these two. */
    private val allowedDownloadHost: String = "github.com",
    private val requireHttps: Boolean = true,
) {
    fun check(repo: String, currentVersion: String): UpdateCheckResult {
        if (!AppUpdateParser.isValidRepo(repo)) throw UpdateException(UpdateError.InvalidSource, "Update source must look like owner/repository.")
        val body = get("$apiBase/repos/${repo.trim()}/releases/latest", allowMissing = true)
            ?: return UpdateCheckResult.NoRelease
        val root = try {
            JSONObject(body)
        } catch (e: org.json.JSONException) {
            throw UpdateException(UpdateError.Unreadable, "The release list could not be read.", e)
        }
        val release = AppUpdateParser.parseRelease(root) { asset ->
            val checksumAsset = AppUpdateParser.parseAssets(root).firstOrNull { it.name == asset.name + ".sha256" }
            checksumAsset?.let { runCatching { get(it.url, allowMissing = true) }.getOrNull() }
        } ?: return UpdateCheckResult.NoRelease
        return if (AppUpdateParser.compareVersions(release.version, currentVersion) > 0) {
            UpdateCheckResult.Available(release)
        } else {
            UpdateCheckResult.UpToDate(currentVersion)
        }
    }

    /**
     * Saves the APK into [directory] and returns it once its SHA-256 matches the published one.
     * A release without a published hash is refused, because the installer cannot tell a swapped
     * file from the real one.
     */
    fun download(release: AppRelease, directory: File, onProgress: (Long, Long) -> Unit): File {
        val expected = release.sha256 ?: throw UpdateException(UpdateError.NoChecksum, "This release publishes no checksum, so it will not be installed.")
        val url = release.apkUrl.toHttpsGitHubUrl()
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() }
        val target = File(directory, release.apkName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateException(UpdateError.Http, "Download failed (HTTP ${response.code}).")
            if (requireHttps && response.request.url.scheme != "https") throw UpdateException(UpdateError.Insecure, "The download was redirected to an insecure address.")
            val body = response.body ?: throw UpdateException(UpdateError.Http, "The download was empty.")
            val total = body.contentLength().takeIf { it > 0 } ?: release.apkSizeBytes
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            try {
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            done += read
                            onProgress(done, total)
                        }
                    }
                }
            } catch (e: IOException) {
                target.delete()
                throw UpdateException(UpdateError.Interrupted, "The download was interrupted.", e)
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != expected) {
                target.delete()
                throw UpdateException(UpdateError.ChecksumMismatch, "The downloaded file does not match its published checksum, so it was discarded.")
            }
        }
        return target
    }

    private fun get(url: String, allowMissing: Boolean): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()
        try {
            http.newCall(request).execute().use { response ->
                if (response.code == 404 && allowMissing) return null
                if (response.code == 403 || response.code == 429) {
                    throw UpdateException(UpdateError.RateLimited, "GitHub is limiting requests right now. Try again later.")
                }
                if (!response.isSuccessful) throw UpdateException(UpdateError.Http, "The update check failed (HTTP ${response.code}).")
                return response.body?.string()
            }
        } catch (e: UpdateException) {
            throw e
        } catch (e: IOException) {
            throw UpdateException(UpdateError.NoConnection, "No connection to GitHub.", e)
        }
    }

    private fun String.toHttpsGitHubUrl(): String {
        val parsed = toHttpUrlOrNull()
        if (parsed == null || (requireHttps && parsed.scheme != "https") || parsed.host != allowedDownloadHost) {
            throw UpdateException(UpdateError.NotGitHub, "The release points to an address that is not on GitHub.")
        }
        return this
    }

    private companion object {
        const val USER_AGENT = "AgentAndroid-Updater"
    }
}
