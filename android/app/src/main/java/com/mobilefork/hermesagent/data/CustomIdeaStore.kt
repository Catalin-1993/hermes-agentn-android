package com.mobilefork.hermesagent.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** An idea the person wrote themselves: a name, and what to ask the agent when they tap it. */
data class CustomIdea(
    val id: String,
    val title: String,
    val prompt: String,
)

/** The person's own ideas, newest first, kept on this phone. */
class CustomIdeaStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<CustomIdea> {
        val array = runCatching { JSONArray(prefs.getString(KEY_IDEAS, "[]").orEmpty()) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id")
            val title = item.optString("title")
            if (id.isBlank() || title.isBlank()) return@mapNotNull null
            CustomIdea(id, title, item.optString("prompt").ifBlank { title })
        }
    }

    /** Adds a new idea, or replaces the one with this [id]. A blank title saves nothing. */
    fun save(id: String?, title: String, prompt: String): List<CustomIdea> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return load()
        val idea = CustomIdea(
            id = id ?: UUID.randomUUID().toString(),
            title = cleanTitle,
            prompt = prompt.trim().ifBlank { cleanTitle },
        )
        val current = load()
        val updated = if (current.any { it.id == idea.id }) {
            current.map { if (it.id == idea.id) idea else it }
        } else {
            listOf(idea) + current
        }
        write(updated)
        return updated
    }

    fun delete(id: String): List<CustomIdea> {
        val updated = load().filterNot { it.id == id }
        write(updated)
        return updated
    }

    private fun write(ideas: List<CustomIdea>) {
        val array = JSONArray()
        ideas.forEach { idea ->
            array.put(JSONObject().put("id", idea.id).put("title", idea.title).put("prompt", idea.prompt))
        }
        prefs.edit().putString(KEY_IDEAS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "hermes_custom_ideas"
        const val KEY_IDEAS = "ideas"
    }
}
