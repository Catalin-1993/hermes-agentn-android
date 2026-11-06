package com.mobilefork.hermesagent.update

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class AppUpdateTest {
    private val server = MockWebServer()
    private lateinit var dir: File

    @Before fun setUp() {
        server.start()
        dir = Files.createTempDirectory("update-test").toFile()
    }

    @After fun tearDown() {
        server.shutdown()
        dir.deleteRecursively()
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun client() = AppUpdateClient(
        apiBase = server.url("/").toString().trimEnd('/'),
        allowedDownloadHost = server.hostName,
        requireHttps = false,
    )

    private fun releaseJson(tag: String, apkUrl: String, digest: String?, extra: String = "") = """
        {"tag_name":"$tag","body":"Notes","html_url":"https://github.com/o/r/releases/tag/$tag","draft":false,"prerelease":false,
         "assets":[
           {"name":"app-playRelease.apk","browser_download_url":"https://github.com/o/r/play.apk","size":1},
           {"name":"app-universal-release.apk","browser_download_url":"$apkUrl","size":4${digest?.let { ""","digest":"sha256:$it"""" } ?: ""}}
           $extra
         ]}
    """.trimIndent()

    @Test fun versionsCompareNumberByNumber() {
        assertTrue(AppUpdateParser.compareVersions("v0.13.162", "0.13.161") > 0)
        assertTrue(AppUpdateParser.compareVersions("0.13.9", "0.13.10") < 0)
        assertEquals(0, AppUpdateParser.compareVersions("0.13.161-lab", "v0.13.161"))
        assertTrue(AppUpdateParser.compareVersions("1.0", "0.99.99") > 0)
    }

    @Test fun repoNamesAreChecked() {
        assertTrue(AppUpdateParser.isValidRepo("Catalin-1993/hermes-agentn-android"))
        assertFalse(AppUpdateParser.isValidRepo("https://github.com/a/b"))
        assertFalse(AppUpdateParser.isValidRepo("a/b/c"))
        assertFalse(AppUpdateParser.isValidRepo(""))
    }

    @Test fun onlyTheUniversalReleaseApkIsOffered() {
        val root = JSONObject(releaseJson("v1.0.0", "https://github.com/o/r/u.apk", sha("x".toByteArray())))
        val apk = AppUpdateParser.chooseApk(AppUpdateParser.parseAssets(root))
        assertEquals("app-universal-release.apk", apk?.name)
        assertNull(AppUpdateParser.chooseApk(emptyList()))
    }

    @Test fun checksumsComeFromTheDigestOrAChecksumFile() {
        val hex = "a".repeat(64)
        assertEquals(hex, AppUpdateParser.parseSha256("sha256:$hex"))
        assertEquals(hex, AppUpdateParser.parseSha256("$hex  app-release.apk\n"))
        assertNull(AppUpdateParser.parseSha256("nothing"))
        assertNull(AppUpdateParser.parseSha256(null))
    }

    @Test fun drafts_and_prereleases_are_ignored() {
        val draft = JSONObject(releaseJson("v9.0.0", "https://github.com/o/r/u.apk", null)).put("draft", true)
        assertNull(AppUpdateParser.parseRelease(draft) { null })
    }

    @Test fun aNewerReleaseIsReportedAndAnOlderOneIsNot() {
        val digest = sha("data".toByteArray())
        server.enqueue(MockResponse().setBody(releaseJson("v0.13.162", "https://github.com/o/r/u.apk", digest)))
        val newer = client().check("o/r", "0.13.161")
        assertTrue(newer is UpdateCheckResult.Available)
        assertEquals("0.13.162", (newer as UpdateCheckResult.Available).release.version)
        assertEquals(digest, newer.release.sha256)

        server.enqueue(MockResponse().setBody(releaseJson("v0.13.161", "https://github.com/o/r/u.apk", digest)))
        assertTrue(client().check("o/r", "0.13.161") is UpdateCheckResult.UpToDate)
    }

    @Test fun aMissingRepositoryOrReleaseMeansNoRelease() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(UpdateCheckResult.NoRelease, client().check("o/r", "0.13.161"))
    }

    @Test fun rateLimitingGetsAReadableError() {
        server.enqueue(MockResponse().setResponseCode(403))
        try {
            client().check("o/r", "0.13.161")
            fail()
        } catch (e: UpdateException) {
            assertTrue(e.message!!.contains("limiting"))
        }
    }

    @Test fun aChecksumFileNextToTheApkIsUsedWhenThereIsNoDigest() {
        val hex = sha("data".toByteArray())
        val sumUrl = server.url("/o/r/sum").toString()
        val extra = ",{\"name\":\"app-universal-release.apk.sha256\",\"browser_download_url\":\"$sumUrl\",\"size\":10}"
        server.enqueue(MockResponse().setBody(releaseJson("v0.13.162", "https://github.com/o/r/u.apk", null, extra)))
        server.enqueue(MockResponse().setBody("$hex  app-universal-release.apk"))
        val result = client().check("o/r", "0.13.161") as UpdateCheckResult.Available
        assertEquals(hex, result.release.sha256)
    }

    private fun release(url: String, sha256: String?) = AppRelease(
        tag = "v2", version = "2", notes = "", pageUrl = "", apkName = "app.apk",
        apkUrl = url, apkSizeBytes = 4, sha256 = sha256,
    )

    @Test fun aMatchingDownloadIsKeptAndReportsProgress() {
        val bytes = "data".toByteArray()
        server.enqueue(MockResponse().setBody(Buffer().write(bytes)))
        var last = 0L
        val file = client().download(release(server.url("/app.apk").toString(), sha(bytes)), dir) { done, _ -> last = done }
        assertTrue(file.exists())
        assertEquals(4L, last)
    }

    @Test fun aDownloadThatDoesNotMatchItsChecksumIsDeleted() {
        server.enqueue(MockResponse().setBody("tampered"))
        try {
            client().download(release(server.url("/app.apk").toString(), sha("data".toByteArray())), dir) { _, _ -> }
            fail()
        } catch (e: UpdateException) {
            assertTrue(dir.listFiles().isNullOrEmpty())
        }
    }

    @Test fun aReleaseWithoutAChecksumIsRefusedBeforeAnythingIsDownloaded() {
        try {
            client().download(release(server.url("/app.apk").toString(), null), dir) { _, _ -> }
            fail()
        } catch (e: UpdateException) {
            assertEquals(0, server.requestCount)
        }
    }

    @Test fun onlyGitHubDownloadsAreAccepted() {
        val strict = AppUpdateClient()
        try {
            strict.download(release("https://evil.example.com/app.apk", "a".repeat(64)), dir) { _, _ -> }
            fail()
        } catch (e: UpdateException) {
            assertTrue(e.message!!.contains("not on GitHub"))
        }
    }
}
