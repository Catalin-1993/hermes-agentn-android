package com.mobilefork.hermesagent.ui.brain

import com.mobilefork.hermesagent.HermesApplication
import org.json.JSONObject

/** What the agent's Python tools call into: one JSON object in, one JSON object out. */
object BrainAgentBridge {
    @JvmStatic
    fun libraryJson(argsJson: String): String = run(argsJson) { tools, args -> tools.library(args) }

    @JvmStatic
    fun calendarJson(argsJson: String): String = run(argsJson) { tools, args -> tools.calendar(args) }

    private fun run(argsJson: String, call: (BrainAgentTools, JSONObject) -> JSONObject): String {
        val args = runCatching { JSONObject(argsJson) }.getOrNull()
            ?: return JSONObject().put("error", "The tool's arguments were not valid JSON.").toString()
        return call(BrainAgentTools(HermesApplication.instance.applicationContext), args).toString()
    }
}
