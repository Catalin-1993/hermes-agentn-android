package com.example.a2ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class A2uiAction(val name: String = "action")
data class A2uiSubmission(
    val displayLabel: String = "",
    val wireJson: String = "{}",
    val action: A2uiAction = A2uiAction()
)

data class A2uiSurfaceModel(
    val surfaceId: String,
    val title: String = "",
    val catalogId: String = "standard",
    val content: String = "",
    val rawJson: String = "{}"
)

class SurfaceStoreHolder {
    private val _surfaces = MutableStateFlow<Map<String, A2uiSurfaceModel>>(emptyMap())
    val surfaces: StateFlow<Map<String, A2uiSurfaceModel>> = _surfaces.asStateFlow()
    val catalogRegistry: Map<String, Any> = mapOf("standard" to Any())

    fun updateSurface(surfaceId: String, surface: A2uiSurfaceModel) {
        val map = _surfaces.value.toMutableMap()
        map[surfaceId] = surface
        _surfaces.value = map
    }
}

object HermesA2ui {
    private var outboundSink: ((A2uiSubmission) -> Unit)? = null
    val store = SurfaceStoreHolder()

    fun setOutboundSink(sink: (A2uiSubmission) -> Unit) {
        outboundSink = sink
    }

    fun submit(submission: A2uiSubmission) {
        outboundSink?.invoke(submission)
    }

    fun handle(request: Any) {
        // Handle interactive request
    }

    fun stripFrames(text: String): String {
        return text
            .replace(Regex("""```a2ui[\s\S]*?```"""), "")
            .replace(Regex("""<a2ui>[\s\S]*?</a2ui>"""), "")
            .trim()
    }

    fun containsFrame(text: String): Boolean {
        return text.contains("```a2ui") || text.contains("<a2ui>") || text.contains(""""surfaceId"""")
    }

    fun decodeSurfaceIds(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun ensureSurfacesForMessage(content: String, existingIds: List<String>): List<String> {
        val regex = Regex(""""surfaceId"\s*:\s*"([^"]+)"""")
        val found = regex.findAll(content).map { it.groupValues[1] }.distinct().toList()
        found.forEach { id ->
            if (!store.surfaces.value.containsKey(id)) {
                store.updateSurface(id, A2uiSurfaceModel(surfaceId = id, title = "A2UI Interfață", content = content))
            }
        }
        return (existingIds + found).distinct()
    }
}
