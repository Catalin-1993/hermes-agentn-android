package com.mobilefork.hermesagent.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.mobilefork.hermesagent.backend.BackendKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * One agent in the contact list. The main agent is the app's existing agent: it has no stored
 * record, uses the app's settings as they are, and always exists.
 */
data class AgentProfile(
    val id: String,
    val name: String,
    val title: String = "",
    val description: String = "",
    val persona: String = "",
    /** A direct provider this agent always uses; blank means the same route as the main agent. */
    val provider: String = "",
    val model: String = "",
    val toolsEnabled: Boolean = false,
    val avatarPath: String = "",
    val createdAtEpochMs: Long = 0L,
) {
    val isMain: Boolean get() = id == MAIN_ID

    /**
     * Whether this agent may reach device tools. A pinned provider answers over its plain chat
     * transport, which has no tools, so the switch only ever applies to the main route.
     */
    val usesDeviceTools: Boolean get() = isMain || (toolsEnabled && provider.isBlank())

    companion object {
        const val MAIN_ID = "main"
        const val MAX_NAME_CHARS = 40
        const val MAX_TITLE_CHARS = 60
        const val MAX_DESCRIPTION_CHARS = 200
        // The prompt reaching the model is cut at 900 characters, name and role included.
        const val MAX_PERSONA_CHARS = 500
        /** Words a group chat already gives a meaning after @, so they cannot be names. */
        val RESERVED_MENTIONS = setOf("everyone", "all", "toți", "toti", "toate", "alle", "user")

        val Main = AgentProfile(id = MAIN_ID, name = "Agent", toolsEnabled = true)
    }
}

/** The settings a request made for this agent runs with. The main agent's are unchanged. */
fun AgentProfile.effectiveSettings(global: AppSettings): AppSettings {
    if (isMain) return global
    val withPersona = global.copy(customSystemPrompt = AppSettings.normalizeCustomSystemPrompt(identityPrompt()))
    if (provider.isBlank()) return withPersona
    return withPersona.copy(
        provider = provider,
        baseUrl = "",
        model = model,
        onDeviceBackend = BackendKind.NONE.persistedValue,
    )
}

private fun AgentProfile.identityPrompt(): String = buildString {
    append("You are ").append(name.trim()).append('.')
    if (title.isNotBlank()) append(" Your role: ").append(title.trim()).append('.')
    if (description.isNotBlank()) append(' ').append(description.trim())
    if (persona.isNotBlank()) append("\n\n").append(persona.trim())
}

class AgentStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun list(): List<AgentProfile> = listOf(AgentProfile.Main) + readCustom().sortedBy { it.createdAtEpochMs }

    /** An agent removed after a conversation began hands that conversation to the main agent. */
    @Synchronized
    fun get(id: String?): AgentProfile =
        if (id.isNullOrBlank() || id == AgentProfile.MAIN_ID) AgentProfile.Main
        else readCustom().firstOrNull { it.id == id } ?: AgentProfile.Main

    /** The agents still present among these ids, in the same order. */
    @Synchronized
    fun existing(ids: List<String>): List<AgentProfile> {
        val agents = list().associateBy { it.id }
        return ids.distinct().mapNotNull { agents[it] }
    }

    /** Mentions address agents by name, so a name can belong to only one of them. */
    @Synchronized
    fun isNameAvailable(name: String, excludingId: String? = null): Boolean {
        val wanted = name.trim().lowercase()
        return wanted.isNotBlank() && wanted !in AgentProfile.RESERVED_MENTIONS && list().none { it.id != excludingId && it.name.trim().lowercase() == wanted }
    }

    @Synchronized
    fun save(agent: AgentProfile): AgentProfile {
        require(!agent.isMain) { "The main agent is configured through the app settings" }
        val normalized = agent.copy(
            id = agent.id.ifBlank { UUID.randomUUID().toString() },
            name = agent.name.trim().take(AgentProfile.MAX_NAME_CHARS),
            title = agent.title.trim().take(AgentProfile.MAX_TITLE_CHARS),
            description = agent.description.trim().take(AgentProfile.MAX_DESCRIPTION_CHARS),
            persona = AppSettings.normalizeCustomSystemPrompt(agent.persona).take(AgentProfile.MAX_PERSONA_CHARS),
            provider = agent.provider.trim().lowercase(),
            model = if (agent.provider.isBlank()) "" else agent.model.trim(),
            toolsEnabled = agent.toolsEnabled && agent.provider.isBlank(),
            createdAtEpochMs = agent.createdAtEpochMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
        require(normalized.name.isNotBlank()) { "An agent needs a name" }
        require(isNameAvailable(normalized.name, normalized.id)) { "Another agent already has this name" }
        write(readCustom().filterNot { it.id == normalized.id } + normalized)
        return normalized
    }

    @Synchronized
    fun delete(id: String) {
        val agent = readCustom().firstOrNull { it.id == id } ?: return
        write(readCustom().filterNot { it.id == id })
        if (agent.avatarPath.isNotBlank()) runCatching { File(agent.avatarPath).delete() }
    }

    /** Copies a picked image into app storage as a small square, so it outlives the picker's grant. */
    fun importAvatar(agentId: String, source: Uri): String {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to read this image" }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= AVATAR_SIZE_PX) sample *= 2
        val decoded = resolver.openInputStream(source).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Unable to read this image")
        val upright = runCatching {
            val degrees = resolver.openInputStream(source).use { stream ->
                if (stream == null) {
                    0
                } else {
                    when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            }
            if (degrees == 0) {
                decoded
            } else {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
            }
        }.getOrDefault(decoded)
        val side = minOf(upright.width, upright.height)
        val square = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, AVATAR_SIZE_PX, AVATAR_SIZE_PX, true)
        val directory = File(appContext.filesDir, AVATAR_DIRECTORY).apply { mkdirs() }
        // A fresh name each time, so a screen showing the previous picture reloads the new one.
        val file = File(directory, "$agentId-${System.currentTimeMillis()}.jpg")
        try {
            file.outputStream().use { output ->
                require(scaled.compress(Bitmap.CompressFormat.JPEG, 90, output)) { "Unable to save this image" }
            }
        } catch (error: Exception) {
            file.delete()
            throw error
        }
        return file.absolutePath
    }

    private fun readCustom(): List<AgentProfile> {
        val raw = preferences.getString(KEY_AGENTS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val name = item.optString("name")
                if (id.isBlank() || name.isBlank() || id == AgentProfile.MAIN_ID) continue
                add(
                    AgentProfile(
                        id = id,
                        name = name,
                        title = item.optString("title"),
                        description = item.optString("description"),
                        persona = item.optString("persona"),
                        provider = item.optString("provider"),
                        model = item.optString("model"),
                        toolsEnabled = item.optBoolean("toolsEnabled", false),
                        avatarPath = item.optString("avatarPath"),
                        createdAtEpochMs = item.optLong("createdAtEpochMs"),
                    ),
                )
            }
        }
    }

    private fun write(agents: List<AgentProfile>) {
        val array = JSONArray()
        agents.forEach { agent ->
            array.put(
                JSONObject()
                    .put("id", agent.id)
                    .put("name", agent.name)
                    .put("title", agent.title)
                    .put("description", agent.description)
                    .put("persona", agent.persona)
                    .put("provider", agent.provider)
                    .put("model", agent.model)
                    .put("toolsEnabled", agent.toolsEnabled)
                    .put("avatarPath", agent.avatarPath)
                    .put("createdAtEpochMs", agent.createdAtEpochMs),
            )
        }
        check(preferences.edit().putString(KEY_AGENTS, array.toString()).commit()) { "Unable to save the agent" }
        revisionFlow.update { it + 1 }
    }

    companion object {
        private const val PREFS_NAME = "hermes_android_agents"
        private const val KEY_AGENTS = "agents"
        private const val AVATAR_DIRECTORY = "agent-avatars"
        private const val AVATAR_SIZE_PX = 256
        private val revisionFlow = MutableStateFlow(0L)

        /** Changes whenever an agent is saved or removed, for screens showing agent details. */
        val revision: StateFlow<Long> = revisionFlow.asStateFlow()
    }
}
