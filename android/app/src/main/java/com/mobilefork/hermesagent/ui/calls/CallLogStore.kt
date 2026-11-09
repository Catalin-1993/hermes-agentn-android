package com.mobilefork.hermesagent.ui.calls

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Who a call was to: one of the person's agents, or a real person on the phone network. */
enum class CallKind { Agent, Person }

/** One call the person placed from this app. [target] is the agent's id, or the phone number. */
data class CallRecord(
    val id: String,
    val kind: CallKind,
    val target: String,
    val name: String,
    val startedAtEpochMs: Long,
    /** Seconds the call lasted; 0 when it is not known (the phone's own call screen takes over for people). */
    val durationSeconds: Int = 0,
)

/**
 * The calls placed from this app, newest first, kept on the phone. Only the app's own calls are
 * listed; the phone's call log is never read.
 */
class CallLogStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("brain_call_log", Context.MODE_PRIVATE)

    fun list(): List<CallRecord> {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                CallRecord(
                    id = item.optString("id"),
                    kind = if (item.optString("kind") == "person") CallKind.Person else CallKind.Agent,
                    target = item.optString("target"),
                    name = item.optString("name"),
                    startedAtEpochMs = item.optLong("at"),
                    durationSeconds = item.optInt("seconds"),
                ).takeIf { it.id.isNotBlank() && it.target.isNotBlank() }
            }.sortedByDescending { it.startedAtEpochMs }
        }.getOrDefault(emptyList())
    }

    fun add(record: CallRecord) {
        val kept = (listOf(record) + list().filterNot { it.id == record.id }).take(MAX_RECORDS)
        val array = JSONArray()
        kept.forEach { call ->
            array.put(
                JSONObject()
                    .put("id", call.id)
                    .put("kind", if (call.kind == CallKind.Person) "person" else "agent")
                    .put("target", call.target)
                    .put("name", call.name)
                    .put("at", call.startedAtEpochMs)
                    .put("seconds", call.durationSeconds),
            )
        }
        preferences.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "calls"
        const val MAX_RECORDS = 200
    }
}
