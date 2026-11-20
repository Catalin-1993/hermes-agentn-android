package com.mobilefork.hermesagent.voice

import android.content.Context
import com.mobilefork.hermesagent.data.HermesNetworkPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

enum class VoiceModelPhase { Missing, Downloading, Unpacking, Ready, Error }

data class VoiceModelStatus(
    val phase: VoiceModelPhase = VoiceModelPhase.Missing,
    /** 0 to 100 while downloading. */
    val progress: Int = 0,
    val message: String = "",
)

/**
 * Downloads, checks, unpacks and removes the voice models, each on its own. A download carries on from
 * the part already on the phone, every file is checked against its SHA-256 before it counts, and a
 * model is only usable once all of it is in place: it is unpacked beside its final folder and moved
 * in with one rename, so a stopped download never leaves a half model that looks ready.
 */
object VoiceModelStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val _statuses = MutableStateFlow<Map<String, VoiceModelStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, VoiceModelStatus>> = _statuses.asStateFlow()

    fun statusOf(id: String): VoiceModelStatus = _statuses.value[id] ?: VoiceModelStatus()

    private fun root(context: Context) = File(context.applicationContext.filesDir, "voice-models")

    fun directory(context: Context, spec: VoiceModelSpec): File = File(root(context), spec.id)

    private fun cache(context: Context, spec: VoiceModelSpec) = File(File(root(context), ".downloads"), spec.id)

    /** A model counts as ready only when the marker written after unpacking names exactly these files. */
    fun isReady(context: Context, spec: VoiceModelSpec): Boolean {
        val marker = File(directory(context, spec), READY_MARKER)
        return marker.isFile && runCatching { marker.readText() }.getOrNull() == fingerprint(spec)
    }

    fun isReady(context: Context, id: String): Boolean = VoiceModelCatalog.find(id)?.let { isReady(context, it) } == true

    private fun fingerprint(spec: VoiceModelSpec) = spec.downloads.joinToString(",") { it.sha256 }

    /** Brings every status in line with what is on the phone, except the ones being downloaded. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        _statuses.update { current ->
            VoiceModelCatalog.all.associate { spec ->
                val running = current[spec.id]?.takeIf { it.phase == VoiceModelPhase.Downloading || it.phase == VoiceModelPhase.Unpacking }
                spec.id to (running ?: if (isReady(app, spec)) VoiceModelStatus(VoiceModelPhase.Ready, 100) else VoiceModelStatus())
            }
        }
    }

    fun download(context: Context, id: String) {
        val app = context.applicationContext
        val spec = VoiceModelCatalog.find(id) ?: return
        if (jobs[id]?.isActive == true) return
        if (isReady(app, spec)) {
            setStatus(id, VoiceModelStatus(VoiceModelPhase.Ready, 100))
            return
        }
        setStatus(id, VoiceModelStatus(VoiceModelPhase.Downloading, 0))
        jobs[id] = scope.launch {
            try {
                install(app, spec)
                setStatus(id, VoiceModelStatus(VoiceModelPhase.Ready, 100))
            } catch (cancelled: CancellationException) {
                setStatus(id, if (isReady(app, spec)) VoiceModelStatus(VoiceModelPhase.Ready, 100) else VoiceModelStatus())
                throw cancelled
            } catch (error: Exception) {
                setStatus(
                    id,
                    VoiceModelStatus(VoiceModelPhase.Error, message = error.message?.takeIf { it.isNotBlank() } ?: "The download failed."),
                )
            }
        }
    }

    /** Stops a running download; the part already downloaded stays, so the next start continues. */
    fun cancel(id: String) {
        jobs[id]?.cancel()
    }

    fun remove(context: Context, id: String) {
        val app = context.applicationContext
        val spec = VoiceModelCatalog.find(id) ?: return
        jobs[id]?.cancel()
        LocalTtsEngine.unload()
        LocalVad.unload()
        directory(app, spec).deleteRecursively()
        cache(app, spec).deleteRecursively()
        setStatus(id, VoiceModelStatus())
    }

    private fun setStatus(id: String, status: VoiceModelStatus) {
        _statuses.update { it + (id to status) }
    }

    private suspend fun install(context: Context, spec: VoiceModelSpec) {
        val downloads = cache(context, spec).apply { mkdirs() }
        val missing = spec.downloads.filter { File(downloads, it.fileName).length() != it.bytes }.sumOf { it.bytes }
        // The archive stays while it is unpacked, and what it unpacks to is a little larger than it.
        val needed = missing + spec.downloadBytes * 3 / 2 + 64L * 1024 * 1024
        if (root(context).apply { mkdirs() }.usableSpace < needed) {
            throw IOException("Not enough free space: about ${needed / 1_000_000} MB are needed.")
        }
        var finished = 0L
        for (item in spec.downloads) {
            HermesNetworkPolicy.requireExternalNetworkAllowed(context, item.url, actionLabel = "voice model download")
            ResumableDownload.fetch(item.url, File(downloads, item.fileName), item.bytes, item.sha256) { inFile ->
                val percent = (((finished + inFile) * 100L) / spec.downloadBytes).toInt().coerceIn(0, 99)
                if (statusOf(spec.id).progress != percent) setStatus(spec.id, VoiceModelStatus(VoiceModelPhase.Downloading, percent))
            }
            finished += item.bytes
        }
        setStatus(spec.id, VoiceModelStatus(VoiceModelPhase.Unpacking, 99))
        val staging = File(root(context), spec.id + ".new")
        staging.deleteRecursively()
        staging.mkdirs()
        try {
            for (item in spec.downloads) {
                val file = File(downloads, item.fileName)
                if (item.extract) VoiceArchive.extract(file, staging) else file.copyTo(File(staging, item.fileName), overwrite = true)
            }
            File(staging, READY_MARKER).writeText(fingerprint(spec))
            val target = directory(context, spec)
            target.deleteRecursively()
            if (!staging.renameTo(target)) throw IOException("The voice could not be saved.")
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
        downloads.deleteRecursively()
    }

    private const val READY_MARKER = ".ready"
}
