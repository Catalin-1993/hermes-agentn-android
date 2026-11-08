package com.mobilefork.hermesagent.ui.brain

import android.content.Context
import com.example.data.model.WorkspaceProfile
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Keeps the person's projects on the phone. A file that cannot be read, such as one a newer
 * version wrote, is ignored and the projects start from the defaults, never from a crash.
 */
internal class BrainWorkspaceStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "brain/projects.bin")

    @Suppress("UNCHECKED_CAST")
    fun load(): List<WorkspaceProfile>? {
        if (!file.isFile) return null
        val loaded = runCatching {
            ObjectInputStream(file.inputStream().buffered()).use { input ->
                if (input.readInt() != VERSION) null
                else (input.readObject() as List<*>).filterIsInstance<WorkspaceProfile>().takeIf { it.isNotEmpty() }
            }
        }.getOrNull()
        // What cannot be read is set aside, not overwritten, so the person's projects are never lost silently.
        if (loaded == null) file.renameTo(File(file.parentFile, "projects.unreadable.bin"))
        return loaded
    }

    fun save(workspaces: List<WorkspaceProfile>) {
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, "projects.tmp")
            ObjectOutputStream(temporary.outputStream().buffered()).use { output ->
                output.writeInt(VERSION)
                output.writeObject(ArrayList(workspaces))
            }
            if (!temporary.renameTo(file)) {
                file.delete()
                temporary.renameTo(file)
            }
        }
    }

    private companion object {
        const val VERSION = 1
    }
}
