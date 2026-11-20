package com.mobilefork.hermesagent.voice

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class VoiceArchiveTest {
    @get:Rule val folder = TemporaryFolder()

    private fun archive(build: TarArchiveOutputStream.() -> Unit): File {
        val file = folder.newFile()
        TarArchiveOutputStream(BZip2CompressorOutputStream(file.outputStream())).use { tar ->
            tar.build()
        }
        return file
    }

    private fun TarArchiveOutputStream.file(name: String, text: String) {
        val bytes = text.toByteArray()
        putArchiveEntry(TarArchiveEntry(name).apply { size = bytes.size.toLong() })
        write(bytes)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.directory(name: String) {
        putArchiveEntry(TarArchiveEntry("$name/"))
        closeArchiveEntry()
    }

    private fun refused(block: () -> Unit) {
        try {
            block()
            fail("the archive should have been refused")
        } catch (expected: IOException) {
            // refused, as it should be
        }
    }

    @Test
    fun theTopFolderIsDroppedAndTheFilesLandInTheTarget() {
        val target = folder.newFolder("model")
        VoiceArchive.extract(
            archive {
                directory("voice-model")
                file("voice-model/model.onnx", "weights")
                file("voice-model/espeak-ng-data/en_dict", "words")
            },
            target,
        )
        assertEquals("weights", File(target, "model.onnx").readText())
        assertEquals("words", File(target, "espeak-ng-data/en_dict").readText())
        assertFalse(File(target, "voice-model").exists())
    }

    @Test
    fun anEntryThatClimbsOutOfTheTargetIsRefusedAndWritesNothing() {
        val target = folder.newFolder("model")
        val outside = File(target.parentFile, "evil.txt")
        refused {
            VoiceArchive.extract(
                archive {
                    file("voice-model/ok.txt", "fine")
                    file("voice-model/../../evil.txt", "bad")
                },
                target,
            )
        }
        assertFalse(outside.exists())
    }

    @Test
    fun linksAreRefused() {
        val target = folder.newFolder("model")
        refused {
            VoiceArchive.extract(
                archive {
                    val link = TarArchiveEntry("voice-model/link", TarArchiveEntry.LF_SYMLINK).apply { linkName = "/etc/passwd" }
                    putArchiveEntry(link)
                    closeArchiveEntry()
                },
                target,
            )
        }
    }

    @Test
    fun anArchiveWithTwoTopFoldersIsRefused() {
        val target = folder.newFolder("model")
        refused {
            VoiceArchive.extract(
                archive {
                    file("one/a.txt", "a")
                    file("two/b.txt", "b")
                },
                target,
            )
        }
    }

    @Test
    fun anEmptyArchiveIsRefused() {
        refused { VoiceArchive.extract(archive { }, folder.newFolder("model")) }
    }
}
