package com.mobilefork.hermesagent.desktop

import com.mobilefork.hermesagent.approval.ChangeApprovalCenter
import java.io.IOException
import okhttp3.Call
import org.json.JSONObject

/** What one desktop turn reports back to the chat, in the order it happens. Called off the main thread. */
interface DesktopTurnListener {
    fun onDelta(text: String)
    fun onToolStarted(name: String, preview: String)
    fun onToolFinished(name: String, failed: Boolean)
    fun onCompleted(content: String)
    fun onFailed(message: String)
}

/**
 * One message answered by the agent on the linked desktop. The conversation keeps the phone's own id on
 * the desktop too, so continuing it from either side is the same conversation.
 *
 * A command the desktop's agent wants to run is shown on the phone's existing approval card and answered
 * only by the person, whatever the phone's permission level is: the phone never lets a desktop command
 * through on its own.
 */
class DesktopTurn(
    private val client: RemoteGatewayClient,
    /** Asks the person; true means run it. Blocks until they answer, so it runs on its own thread. */
    private val askPerson: (command: String, description: String) -> Boolean =
        { command, description -> askOnApprovalCard(command, description) },
) {
    @Volatile private var call: Call? = null
    @Volatile private var runId: String = ""
    @Volatile private var cancelled = false

    /** Stops listening and tells the desktop to stop the turn. Safe to call from any thread. */
    fun cancel() {
        cancelled = true
        call?.cancel()
        val id = runId
        if (id.isNotBlank()) Thread { runCatching { client.stop(id) } }.start()
    }

    /** Blocks until the turn ends. */
    fun run(sessionId: String, text: String, listener: DesktopTurnListener) {
        var completed: String? = null
        var failure: String? = null
        try {
            client.ensureSession(sessionId)
            client.chat(sessionId, text, onCall = { call = it }) { event ->
                when (event) {
                    is RemoteEvent.RunStarted -> runId = event.runId
                    is RemoteEvent.Delta -> listener.onDelta(event.text)
                    is RemoteEvent.ToolStarted -> listener.onToolStarted(event.name, event.preview)
                    is RemoteEvent.ToolFinished -> listener.onToolFinished(event.name, event.failed)
                    is RemoteEvent.ApprovalRequest -> answerInBackground(event)
                    is RemoteEvent.Completed -> completed = event.content
                    is RemoteEvent.Failed -> failure = event.message
                    is RemoteEvent.RunEnded, is RemoteEvent.Commentary, RemoteEvent.Done -> Unit
                }
            }
        } catch (error: IOException) {
            if (!cancelled) failure = error.message ?: "The desktop could not be reached."
        }
        if (cancelled) return
        val finalText = completed
        val problem = failure
        when {
            finalText != null -> listener.onCompleted(finalText)
            else -> listener.onFailed(problem ?: "The desktop ended the turn without an answer.")
        }
    }

    private fun answerInBackground(request: RemoteEvent.ApprovalRequest) {
        Thread {
            val allowed = runCatching { askPerson(request.command, request.description) }.getOrDefault(false)
            val choice = if (allowed && "once" in request.choices) "once" else "deny"
            runCatching { client.answerApproval(request.runId, choice) }
        }.start()
    }

    private companion object {
        fun askOnApprovalCard(command: String, description: String): Boolean {
            val proposal = JSONObject()
                .put("tool", "desktop")
                .put("kind", "run")
                .put("title", "Your desktop wants to run a command")
                .put("target", "Desktop")
                .put("note", description)
                .put("fields", JSONObject().put("command", command))
            val answer = runCatching { JSONObject(ChangeApprovalCenter.request(proposal.toString())) }.getOrNull()
            return answer?.optString("decision") == "accept"
        }
    }
}
