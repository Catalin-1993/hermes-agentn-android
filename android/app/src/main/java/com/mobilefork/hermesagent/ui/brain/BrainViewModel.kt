package com.mobilefork.hermesagent.ui.brain

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CalendarEventItem
import com.example.data.model.InterAgentMessageEntity
import com.example.data.model.LibraryItem
import com.example.data.model.WorkspaceProfile
import com.example.data.model.WorkspaceTaskEntity
import com.example.data.model.WorkspaceTaskStatus
import com.example.data.storage.LibraryStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** The library (notes and documents), calendar and projects, kept on this phone. */
class BrainViewModel(application: Application) : AndroidViewModel(application) {
    private val workspaceStore = BrainWorkspaceStore(application)
    private val database by lazy { AppDatabase.getDatabase(application) }

    private val _libraryItems = MutableStateFlow<List<LibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<LibraryItem>> = _libraryItems.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEventItem>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEventItem>> = _calendarEvents.asStateFlow()

    private val _workspaces = MutableStateFlow(workspaceStore.load() ?: defaultBrainWorkspaces())
    val workspaces: StateFlow<List<WorkspaceProfile>> = _workspaces.asStateFlow()

    private val _activeWorkspaceId = MutableStateFlow(MAIN_WORKSPACE_ID)
    val activeWorkspaceId: StateFlow<String> = _activeWorkspaceId.asStateFlow()

    val tasks: StateFlow<List<WorkspaceTaskEntity>> by lazy {
        database.workspaceTaskDao.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val interAgentMessages: StateFlow<List<InterAgentMessageEntity>> by lazy {
        _activeWorkspaceId
            .flatMapLatest { id -> database.interAgentMessageDao.getMessagesForWorkspace(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    init {
        // Collecting also delivers the current value, which is the first load; after that, every
        // change by a page or by the agent reloads what is on screen.
        viewModelScope.launch(Dispatchers.IO) {
            BrainDataStore.revision.collect {
                _libraryItems.value = BrainDataStore.library(application)
                _calendarEvents.value = BrainDataStore.calendar(application)
            }
        }
    }

    // ---------------------------------------------------------------- library

    fun addLibraryItem(item: LibraryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.filePath.isNullOrBlank()) {
                BrainDataStore.addNote(getApplication(), item)
            } else {
                BrainDataStore.withLibrary(getApplication()) { it.add(0, item) }
            }
        }
    }

    fun updateLibraryItem(item: LibraryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!item.filePath.isNullOrBlank()) {
                runCatching {
                    File(item.filePath!!).writeText(item.fullContent.ifBlank { item.blocks.joinToString("\n\n") { it.content } })
                }
            }
            BrainDataStore.withLibrary(getApplication()) { list ->
                val index = list.indexOfFirst { it.id == item.id }
                if (index >= 0) list[index] = item
            }
        }
    }

    fun deleteLibraryItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            BrainDataStore.withLibrary(getApplication()) { list -> list.removeAll { it.id == id } }
        }
    }

    fun importLibraryFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = runCatching { LibraryStorageManager.importFileToLibrary(getApplication(), uri) }.getOrNull() ?: return@launch
            BrainDataStore.withLibrary(getApplication()) { list ->
                list.removeAll { it.id == item.id }
                list.add(0, item)
            }
        }
    }

    // ---------------------------------------------------------------- calendar

    fun addCalendarEvent(event: CalendarEventItem) = changeEvents { it.add(event) }

    fun updateCalendarEvent(event: CalendarEventItem) = changeEvents { list ->
        val index = list.indexOfFirst { it.id == event.id }
        if (index >= 0) list[index] = event
    }

    fun deleteCalendarEvent(id: String) = changeEvents { list -> list.removeAll { it.id == id } }

    private fun changeEvents(change: (MutableList<CalendarEventItem>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { BrainDataStore.withCalendar(getApplication(), change) }
    }

    // ---------------------------------------------------------------- projects

    fun selectWorkspace(id: String) {
        _activeWorkspaceId.value = id
    }

    fun saveWorkspace(workspace: WorkspaceProfile) {
        val current = _workspaces.value
        val updated = if (current.any { it.id == workspace.id }) {
            current.map { if (it.id == workspace.id) workspace else it }
        } else {
            current + workspace
        }
        persistWorkspaces(updated)
    }

    fun deleteWorkspace(id: String) {
        val remaining = _workspaces.value.filter { it.id != id }
        if (remaining.isEmpty()) return
        if (_activeWorkspaceId.value == id) _activeWorkspaceId.value = remaining.first().id
        persistWorkspaces(remaining)
    }

    private fun persistWorkspaces(updated: List<WorkspaceProfile>) {
        _workspaces.value = updated
        viewModelScope.launch(Dispatchers.IO) { workspaceStore.save(updated) }
    }

    /** Files attached to a project, as the project's Files page lists them. */
    fun attachedFiles(workspaces: List<WorkspaceProfile>): List<File> =
        workspaces.flatMap { it.attachedFiles }.map(::File).filter { it.isFile }.distinct()

    fun createTask(workspaceId: String, title: String, instructions: String, role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.workspaceTaskDao.insertTask(
                WorkspaceTaskEntity(
                    workspaceId = workspaceId,
                    title = title,
                    instructions = instructions,
                    assignedSubRole = role,
                ),
            )
        }
    }

    fun updateTaskStatus(taskId: Long, status: WorkspaceTaskStatus, summary: String) {
        viewModelScope.launch(Dispatchers.IO) { database.workspaceTaskDao.updateTaskStatus(taskId, status, summary) }
    }

    fun deleteTask(taskId: String) {
        val id = taskId.toLongOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) { database.workspaceTaskDao.deleteTaskById(id) }
    }

    companion object {
        /** The core project, which is the main agent. */
        const val MAIN_WORKSPACE_ID = "agent_hermes"
    }
}
