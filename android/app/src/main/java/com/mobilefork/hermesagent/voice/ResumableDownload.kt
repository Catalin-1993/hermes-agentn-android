package com.mobilefork.hermesagent.voice

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Downloads one file that carries on from the part already on the phone, then checks its SHA-256. */
internal object ResumableDownload {
    /**
     * Fetches [url] into [target] unless it is already there with the right size. [onBytes] reports how
     * many bytes of this file are in place. A dropped connection is retried from the part on disk.
     */
    suspend fun fetch(url: String, target: File, bytes: Long, sha256: String, onBytes: (Long) -> Unit) {
        if (target.isFile && target.length() == bytes) {
            onBytes(bytes)
            return
        }
        target.delete()
        val part = File(target.parentFile, target.name + ".part")
        var attempt = 0
        while (true) {
            try {
                download(url, part, bytes, onBytes)
                break
            } catch (error: IOException) {
                if (++attempt >= 4 || coroutineContext[Job]?.isActive == false) throw error
                delay(1_500L * attempt)
            }
        }
        if (sha256Of(part) != sha256) {
            part.delete()
            throw IOException("${target.name} did not match its checksum, so it was discarded. Try again.")
        }
        if (!part.renameTo(target)) throw IOException("${target.name} could not be saved.")
    }

    private suspend fun download(url: String, part: File, bytes: Long, onBytes: (Long) -> Unit) {
        var offset = if (part.isFile) part.length() else 0L
        if (offset > bytes) {
            part.delete()
            offset = 0L
        }
        if (offset == bytes) {
            onBytes(bytes)
            return
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
        }
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK && offset > 0) {
                // The server ignored the range: start the file again.
                part.delete()
                offset = 0L
            } else if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("The server answered $code for ${part.name.removeSuffix(".part")}.")
            }
            part.parentFile?.mkdirs()
            RandomAccessFile(part, "rw").use { out ->
                out.seek(offset)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(256 * 1024)
                    var written = offset
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read
                        coroutineContext.ensureActive()
                        onBytes(written)
                    }
                }
            }
            if (part.length() != bytes) throw IOException("${part.name.removeSuffix(".part")} stopped before it was complete.")
        } finally {
            connection.disconnect()
        }
    }

    fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
