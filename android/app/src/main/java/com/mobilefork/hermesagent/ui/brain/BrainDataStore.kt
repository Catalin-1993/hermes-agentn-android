package com.mobilefork.hermesagent.ui.brain

import android.content.Context
import com.example.data.model.CalendarEventItem
import com.example.data.model.LibraryItem
import com.example.data.storage.CalendarStorageManager
import com.example.data.storage.LibraryStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * The one place the library and the calendar are read and changed, for the pages on screen and for
 * the agent alike. Every change reads the saved list, applies itself and writes it back under one
 * lock, so what the agent adds is never lost to a screen holding an older copy, and the other way
 * round. [revision] moves after each change so open pages reload.
 */
object BrainDataStore {
    private val lock = Any()
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()

    fun library(context: Context): List<LibraryItem> = synchronized(lock) { loadLibrary(context) }

    fun calendar(context: Context): List<CalendarEventItem> = synchronized(lock) { loadCalendar(context) }

    /** Runs [change] on the saved library and saves the result. A change that throws saves nothing. */
    fun <T> withLibrary(context: Context, change: (MutableList<LibraryItem>) -> T): T = synchronized(lock) {
        val list = loadLibrary(context).toMutableList()
        val result = change(list)
        LibraryStorageManager.saveLibraryIndex(context, list)
        _revision.value += 1
        result
    }

    fun <T> withCalendar(context: Context, change: (MutableList<CalendarEventItem>) -> T): T = synchronized(lock) {
        val list = loadCalendar(context).toMutableList()
        val result = change(list)
        CalendarStorageManager.saveCalendarEvents(context, list)
        _revision.value += 1
        result
    }

    /**
     * Adds a note that is also a real file, so other apps can open it. Making the file puts its own
     * entry in the saved library; that entry is replaced by [note], which carries the page's content,
     * so the note is listed once. The file is named after the title, and the library writes a new
     * file over an old one of the same name, so a second note with a title already used gets a
     * numbered file name instead of silently replacing the first note's text.
     */
    fun addNote(context: Context, note: LibraryItem): LibraryItem = synchronized(lock) {
        val taken = loadLibrary(context)
            .mapNotNull { item -> item.filePath?.takeIf { it.isNotBlank() }?.let { File(it).nameWithoutExtension.lowercase() } }
            .toSet()
        var fileTitle = note.title
        var number = 2
        while (fileTitle.lowercase() in taken) fileTitle = "${note.title} (${number++})"
        val file = runCatching {
            LibraryStorageManager.createPhysicalNoteItem(
                context,
                fileTitle,
                note.fullContent.ifBlank { note.blocks.joinToString("\n\n") { it.content } },
                note.category,
                note.tags,
                note.parentId,
            )
        }.getOrNull()
        val stored = if (file == null) note else note.copy(
            filePath = file.filePath,
            originalFileName = file.originalFileName,
            fileSize = file.fileSize,
            fileExtension = file.fileExtension,
            mimeType = file.mimeType,
        )
        val list = loadLibrary(context).toMutableList()
        if (file != null) list.removeAll { it.id == file.id }
        list.add(0, stored)
        LibraryStorageManager.saveLibraryIndex(context, list)
        _revision.value += 1
        stored
    }

    private fun loadLibrary(context: Context): List<LibraryItem> =
        runCatching { LibraryStorageManager.loadLibraryIndex(context) }.getOrDefault(emptyList())

    private fun loadCalendar(context: Context): List<CalendarEventItem> =
        runCatching { CalendarStorageManager.loadCalendarEvents(context) }.getOrDefault(emptyList())
}
