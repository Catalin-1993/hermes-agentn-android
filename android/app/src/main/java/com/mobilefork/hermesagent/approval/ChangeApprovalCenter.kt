package com.mobilefork.hermesagent.approval

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A change the agent wants to make, as the approval card shows it. */
data class PendingChange(
    val id: String,
    val tool: String,
    /** create, update, delete, write or run: decides the card's accent. */
    val kind: String,
    val title: String,
    val target: String,
    val note: String,
    /** What will be saved, in the order the agent gave it. */
    val fields: List<Pair<String, String>>,
    /** The fields the person may change before accepting. */
    val editable: Set<String>,
    /** Fields shown shortened; their full text is what runs. */
    val truncated: Set<String>,
)

/**
 * Where the agent's changes wait for the person. The agent's call blocks in [request] until the
 * card is answered, so nothing is saved, changed or deleted before Accept. No answer within
 * [TIMEOUT_MS] counts as Reject.
 */
object ChangeApprovalCenter {
    const val TIMEOUT_MS = 10 * 60 * 1000L

    private class Waiting(val change: PendingChange) {
        val latch = CountDownLatch(1)
        @Volatile var answer: JSONObject? = null
    }

    private val lock = Any()
    private val waiting = mutableListOf<Waiting>()
    private val _pending = MutableStateFlow<List<PendingChange>>(emptyList())

    /** Oldest first: the card shows the first one. */
    val pending: StateFlow<List<PendingChange>> = _pending.asStateFlow()

    /** Blocks the calling (agent) thread until the person answers; returns the decision as JSON. */
    fun request(proposalJson: String, timeoutMs: Long = TIMEOUT_MS): String {
        val change = parse(proposalJson) ?: return rejection("The change could not be read.").toString()
        // The person may have already said how much to ask: Full lets everything through, Auto lets
        // notes, events and memory through. Every other case waits for the card.
        if (ChangePermissions.allowsWithoutAsking(ChangePermissions.current(), change.tool, change.kind)) {
            return JSONObject().put("decision", "accept").put("arguments", JSONObject()).toString()
        }
        val entry = Waiting(change)
        synchronized(lock) {
            waiting += entry
            publish()
        }
        val answered = try {
            entry.latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        synchronized(lock) {
            waiting -= entry
            publish()
        }
        val answer = entry.answer
        return (if (answered && answer != null) answer else rejection("No answer came in time.")).toString()
    }

    /** Accept, with the person's edits to editable fields only. Unchanged fields are not sent back. */
    fun accept(id: String, edits: Map<String, String> = emptyMap()) {
        val change = find(id) ?: return
        val arguments = JSONObject()
        edits.forEach { (key, value) ->
            val original = change.fields.firstOrNull { it.first == key }?.second
            if (key in change.editable && key !in change.truncated && value != original) arguments.put(key, value)
        }
        answer(id, JSONObject().put("decision", "accept").put("arguments", arguments))
    }

    fun reject(id: String) = answer(id, rejection(""))

    /** After the person widens the permission, cards already waiting that it now covers are accepted. */
    internal fun releaseAllowed() {
        val mode = ChangePermissions.current()
        val covered = synchronized(lock) {
            waiting.filter { ChangePermissions.allowsWithoutAsking(mode, it.change.tool, it.change.kind) }.map { it.change.id }
        }
        covered.forEach { accept(it) }
    }

    private fun find(id: String): PendingChange? = synchronized(lock) { waiting.firstOrNull { it.change.id == id }?.change }

    private fun answer(id: String, decision: JSONObject) {
        val entry = synchronized(lock) { waiting.firstOrNull { it.change.id == id } } ?: return
        entry.answer = decision
        entry.latch.countDown()
    }

    private fun publish() {
        _pending.value = waiting.map { it.change }
    }

    private fun rejection(reason: String): JSONObject = JSONObject().put("decision", "reject").put("reason", reason)

    internal fun parse(proposalJson: String): PendingChange? {
        val json = runCatching { JSONObject(proposalJson) }.getOrNull() ?: return null
        val fieldsJson = json.optJSONObject("fields") ?: JSONObject()
        val fields = fieldsJson.keys().asSequence().map { it to fieldsJson.optString(it) }.toList()
        return PendingChange(
            id = UUID.randomUUID().toString(),
            tool = json.optString("tool"),
            kind = json.optString("kind").ifBlank { "update" },
            title = json.optString("title").ifBlank { "Approve a change" },
            target = json.optString("target"),
            note = json.optString("note"),
            fields = fields,
            editable = json.optJSONArray("editable").strings(),
            truncated = json.optJSONArray("truncated").strings(),
        )
    }

    private fun JSONArray?.strings(): Set<String> =
        if (this == null) emptySet() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }.toSet()
}

/** What the agent's Python gate (hermes_android/change_approval.py) calls into. */
object ChangeApprovalBridge {
    @JvmStatic
    fun requestJson(proposalJson: String): String = ChangeApprovalCenter.request(proposalJson)
}
