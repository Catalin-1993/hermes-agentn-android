package com.mobilefork.hermesagent.voice

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.IOException

/** Opens the `.tar.bz2` a voice model comes in. Nothing it contains can write outside the target folder. */
internal object VoiceArchive {
    private const val MAX_UNPACKED_BYTES = 2L * 1024 * 1024 * 1024

    /**
     * Unpacks [archive] into [target], leaving out the one top-level folder every official archive has.
     * Links, entries with a different top folder, entries that climb out of [target] and archives that
     * unpack to more than 2 GB are refused.
     */
    fun extract(archive: File, target: File) {
        target.mkdirs()
        val root = target.canonicalFile
        var top: String? = null
        var total = 0L
        archive.inputStream().buffered().use { raw ->
            BZip2CompressorInputStream(raw).use { bzip ->
                TarArchiveInputStream(bzip).use { tar ->
                    while (true) {
                        val entry = tar.nextEntry ?: break
                        if (entry.isSymbolicLink || entry.isLink) throw IOException("The archive contains a link, so it was refused.")
                        val parts = entry.name.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != "." }
                        if (parts.isEmpty()) continue
                        if (parts.any { it == ".." }) throw IOException("The archive tries to leave its folder, so it was refused.")
                        val first = top ?: parts.first().also { top = it }
                        if (parts.first() != first) throw IOException("The archive has an unexpected layout, so it was refused.")
                        val relative = parts.drop(1)
                        if (relative.isEmpty()) continue
                        val out = File(root, relative.joinToString(File.separator)).canonicalFile
                        if (!out.path.startsWith(root.path + File.separator)) {
                            throw IOException("The archive tries to leave its folder, so it was refused.")
                        }
                        if (entry.isDirectory) {
                            out.mkdirs()
                            continue
                        }
                        out.parentFile?.mkdirs()
                        out.outputStream().buffered().use { sink ->
                            val buffer = ByteArray(256 * 1024)
                            while (true) {
                                val read = tar.read(buffer)
                                if (read < 0) break
                                total += read
                                if (total > MAX_UNPACKED_BYTES) throw IOException("The archive is far bigger than expected, so it was refused.")
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }
            }
        }
        if (top == null) throw IOException("The archive was empty.")
    }
}
