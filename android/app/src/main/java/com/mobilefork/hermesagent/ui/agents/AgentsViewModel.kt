package com.mobilefork.hermesagent.ui.agents

import com.mobilefork.hermesagent.data.isAvatarFile
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentStore
import com.mobilefork.hermesagent.data.AppSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AgentsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AgentStore(application)

    val agents: StateFlow<List<AgentProfile>> = AgentStore.revision
        .map { store.list() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.list())

    fun isNameAvailable(name: String, agentId: String): Boolean = store.isNameAvailable(name, agentId)

    fun mainPersona(): String = AppSettingsStore(getApplication<Application>()).load().customSystemPrompt

    fun saveMainPersona(persona: String): Result<Unit> = runCatching {
        AppSettingsStore(getApplication<Application>()).update { it.copy(customSystemPrompt = persona) }
        Unit
    }

    fun importAvatar(agentId: String, source: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { runCatching { store.importAvatar(agentId, source) }.getOrNull() }
            onResult(path)
        }
    }

    /** Removes a picture picked in the editor that was then replaced or never saved. */
    fun discardAvatar(path: String) {
        // A character, initials or the automatic look are words, not files.
        if (!isAvatarFile(path)) return
        viewModelScope.launch(Dispatchers.IO) { runCatching { File(path).delete() } }
    }

    fun save(agent: AgentProfile): Result<AgentProfile> = runCatching { store.save(agent) }

    fun saveMainAvatar(avatar: String): Result<Unit> = runCatching { store.saveMainAvatar(avatar) }

    fun delete(agentId: String): Result<Unit> = runCatching { store.delete(agentId) }
}
