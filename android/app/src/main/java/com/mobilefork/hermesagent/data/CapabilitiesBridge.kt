package com.mobilefork.hermesagent.data

import android.content.Context
import com.chaquo.python.Python
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class SkillState { Active, Disabled, Available }

data class CapabilitySkill(
    val name: String,
    val description: String,
    val category: String,
    val state: SkillState,
    /** Set for an official Hermes skill not installed yet. */
    val identifier: String = "",
)

data class CapabilityPlugin(val key: String, val name: String, val description: String, val enabled: Boolean)

data class CapabilityToolset(val key: String, val label: String, val description: String, val enabled: Boolean)

data class CapabilitiesSnapshot(
    val ok: Boolean,
    val error: String = "",
    val skills: List<CapabilitySkill> = emptyList(),
    val plugins: List<CapabilityPlugin> = emptyList(),
    val toolsets: List<CapabilityToolset> = emptyList(),
    /** A part that could not be read, with why; the other parts still load. */
    val partErrors: Map<String, String> = emptyMap(),
)

/** The Hermes agent's skills, plugins and tools, read and changed through Hermes' own code. */
object CapabilitiesBridge {
    private fun hermesHome(context: Context) = File(context.applicationContext.filesDir, "hermes-home").absolutePath

    private fun call(method: String, vararg args: Any?): CapabilitiesSnapshot {
        if (!Python.isStarted()) return CapabilitiesSnapshot(ok = false, error = PYTHON_NOT_READY)
        return runCatching {
            parse(
                JSONObject(
                    Python.getInstance().getModule("hermes_android.capabilities_bridge").callAttr(method, *args).toString(),
                ),
            )
        }.getOrElse { CapabilitiesSnapshot(ok = false, error = it.message.orEmpty().ifBlank { it::class.java.simpleName }) }
    }

    fun list(context: Context) = call("list_capabilities", hermesHome(context))

    fun setSkillEnabled(context: Context, name: String, enabled: Boolean) =
        call("set_skill_enabled", name, enabled, hermesHome(context))

    fun installSkill(context: Context, identifier: String) = call("install_skill", identifier, hermesHome(context))

    fun setPluginEnabled(context: Context, key: String, enabled: Boolean) =
        call("set_plugin_enabled", key, enabled, hermesHome(context))

    fun setToolsetEnabled(context: Context, key: String, enabled: Boolean) =
        call("set_toolset_enabled", key, enabled, hermesHome(context))

    const val PYTHON_NOT_READY = "python-not-ready"

    internal fun parse(json: JSONObject): CapabilitiesSnapshot {
        val errors = json.optJSONObject("errors")
        return CapabilitiesSnapshot(
            ok = json.optBoolean("ok", false),
            error = json.optString("error"),
            skills = json.optJSONArray("skills").objects().map {
                CapabilitySkill(
                    name = it.optString("name"),
                    description = it.optString("description"),
                    category = it.optString("category"),
                    state = when (it.optString("state")) {
                        "disabled" -> SkillState.Disabled
                        "available" -> SkillState.Available
                        else -> SkillState.Active
                    },
                    identifier = it.optString("identifier"),
                )
            },
            plugins = json.optJSONArray("plugins").objects().map {
                CapabilityPlugin(it.optString("key"), it.optString("name"), it.optString("description"), it.optBoolean("enabled"))
            },
            toolsets = json.optJSONArray("toolsets").objects().map {
                CapabilityToolset(it.optString("key"), it.optString("label"), it.optString("description"), it.optBoolean("enabled"))
            },
            partErrors = errors?.keys()?.asSequence()?.associateWith { errors.optString(it) }.orEmpty(),
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
