package com.mobilefork.hermesagent.voice

import android.content.Context
import com.mobilefork.hermesagent.data.HermesNetworkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** One file of a speech model: where it comes from, and the exact size and SHA-256 it must have. */
internal class SpeechModelFile(val name: String, val url: String, val bytes: Long, val sha256: String)

/**
 * The on-device speech recognizer: NVIDIA Parakeet TDT 0.6B v3 (int8), converted for sherpa-onnx.
 * It recognizes 25 European languages, Romanian among them, and finds the language by itself.
 * Licence: CC BY 4.0, so [CREDIT] stays visible wherever the model is offered.
 */
internal object ParakeetModel {
    const val DIRECTORY = "parakeet-tdt-0.6b-v3-int8"
    const val CREDIT = "Speech model: NVIDIA Parakeet TDT 0.6B v3 (CC BY 4.0), converted by the sherpa-onnx project."

    // A fixed revision, never "main": the files below are exactly the ones whose hashes are listed.
    private const val BASE =
        "https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8/resolve/2bda32ec70b097a55adaa07d9a7173915b43cc78"

    val files: List<SpeechModelFile> = listOf(
        SpeechModelFile("encoder.int8.onnx", "$BASE/encoder.int8.onnx", 652_184_281L, "acfc2b4456377e15d04f0243af540b7fe7c992f8d898d751cf134c3a55fd2247"),
        SpeechModelFile("decoder.int8.onnx", "$BASE/decoder.int8.onnx", 11_845_275L, "179e50c43d1a9de79c8a24149a2f9bac6eb5981823f2a2ed88d655b24248db4e"),
        SpeechModelFile("joiner.int8.onnx", "$BASE/joiner.int8.onnx", 6_355_277L, "3164c13fc2821009440d20fcb5fdc78bff28b4db2f8d0f0b329101719c0948b3"),
        SpeechModelFile("tokens.txt", "$BASE/tokens.txt", 93_939L, "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d"),
    )

    val totalBytes: Long = files.sumOf { it.bytes }
}

enum class SpeechModelPhase { Missing, Downloading, Ready, Error }

data class SpeechModelStatus(
    val phase: SpeechModelPhase = SpeechModelPhase.Missing,
    /** 0 to 100 while downloading. */
    val progress: Int = 0,
    val message: String = "",
)

/**
 * Downloads, checks and removes the speech model. A download carries on from the part already on the
 * phone, every file is checked against its SHA-256 before it counts, and nothing is used until all of
 * them are in place.
 */
object LocalSpeechModelStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val _status = MutableStateFlow(SpeechModelStatus())
    val status: StateFlow<SpeechModelStatus> = _status.asStateFlow()

    const val CREDIT = ParakeetModel.CREDIT
    val sizeMegabytes: Int get() = (ParakeetModel.totalBytes / 1_000_000L).toInt()

    fun directory(context: Context): File =
        File(File(context.applicationContext.filesDir, "voice-models"), ParakeetModel.DIRECTORY)

    /** Sizes only: the hashes were checked when each file was downloaded. */
    fun isReady(context: Context): Boolean {
        val dir = directory(context)
        return ParakeetModel.files.all { File(dir, it.name).let { file -> file.isFile && file.length() == it.bytes } }
    }

    /** Brings the status in line with what is on the phone, unless a download is running. */
    fun refresh(context: Context) {
        if (_status.value.phase == SpeechModelPhase.Downloading) return
        _status.value = if (isReady(context)) {
            SpeechModelStatus(SpeechModelPhase.Ready, 100)
        } else {
            SpeechModelStatus(SpeechModelPhase.Missing)
        }
    }

    fun download(context: Context) {
        val app = context.applicationContext
        if (job?.isActive == true || isReady(app)) {
            refresh(app)
            return
        }
        _status.value = SpeechModelStatus(SpeechModelPhase.Downloading, 0)
        job = scope.launch {
            try {
                fetchAll(app)
                _status.value = SpeechModelStatus(SpeechModelPhase.Ready, 100)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                _status.value = if (isReady(app)) SpeechModelStatus(SpeechModelPhase.Ready, 100) else SpeechModelStatus()
                throw cancelled
            } catch (error: Exception) {
                _status.value = SpeechModelStatus(
                    SpeechModelPhase.Error,
                    message = error.message?.takeIf { it.isNotBlank() } ?: "The download failed.",
                )
            }
        }
    }

    /** Stops a running download; the part already downloaded stays, so the next start continues. */
    fun cancel() {
        job?.cancel()
    }

    fun remove(context: Context) {
        job?.cancel()
        LocalSpeechEngine.unload()
        directory(context.applicationContext).deleteRecursively()
        _status.value = SpeechModelStatus()
    }

    private suspend fun fetchAll(context: Context) {
        val dir = directory(context).apply { mkdirs() }
        val needed = ParakeetModel.files.filter { File(dir, it.name).length() != it.bytes }.sumOf { it.bytes }
        if (dir.usableSpace < needed + 64L * 1024 * 1024) {
            throw IOException("Not enough free space: about ${needed / 1_000_000 + 64} MB are needed.")
        }
        var finished = 0L
        for (file in ParakeetModel.files) {
            val target = File(dir, file.name)
            if (target.length() != file.bytes) {
                target.delete()
                fetchOne(context, file, target, finished)
            }
            finished += file.bytes
        }
    }

    private suspend fun fetchOne(context: Context, file: SpeechModelFile, target: File, finishedBefore: Long) {
        HermesNetworkPolicy.requireExternalNetworkAllowed(context, file.url, actionLabel = "speech model download")
        val part = File(target.parentFile, file.name + ".part")
        var attempt = 0
        while (true) {
            try {
                downloadInto(file, part, finishedBefore)
                break
            } catch (error: IOException) {
                // A dropped connection carries on from the part on disk, a few times, before giving up.
                if (++attempt >= 4 || !currentScopeActive()) throw error
                kotlinx.coroutines.delay(1_500L * attempt)
            }
        }
        if (sha256Of(part) != file.sha256) {
            part.delete()
            throw IOException("${file.name} did not match its checksum, so it was discarded. Try again.")
        }
        if (!part.renameTo(target)) throw IOException("${file.name} could not be saved.")
    }

    private suspend fun currentScopeActive(): Boolean = kotlin.coroutines.coroutineContext[Job]?.isActive != false

    private suspend fun downloadInto(file: SpeechModelFile, part: File, finishedBefore: Long) {
        var offset = if (part.isFile) part.length() else 0L
        if (offset > file.bytes) {
            part.delete()
            offset = 0L
        }
        if (offset == file.bytes) return
        val connection = (URL(file.url).openConnection() as HttpURLConnection).apply {
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
                throw IOException("The server answered $code for ${file.name}.")
            }
            RandomAccessFile(part, "rw").use { out ->
                out.seek(offset)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(256 * 1024)
                    var written = offset
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        written += read
                        kotlin.coroutines.coroutineContext.ensureActive()
                        val percent = (((finishedBefore + written) * 100L) / ParakeetModel.totalBytes).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _status.value = SpeechModelStatus(SpeechModelPhase.Downloading, percent)
                        }
                    }
                }
            }
            if (part.length() != file.bytes) throw IOException("${file.name} stopped before it was complete.")
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256Of(file: File): String {
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
