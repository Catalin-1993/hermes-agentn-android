package com.mobilefork.hermesagent.desktop

import com.mobilefork.hermesagent.api.HermesEndpointUrl
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** The desktop answered with an error status. */
class RemoteHttpException(val code: Int, message: String) : IOException(message)

/** One conversation that lives on the desktop. */
data class RemoteSession(
    val id: String,
    val title: String,
    val preview: String,
    /** Seconds since the epoch; 0 when the desktop did not say. */
    val lastActive: Double,
    val messageCount: Int,
)

/** One message of a desktop conversation, reduced to what the phone draws. */
data class RemoteMessage(val role: String, val content: String)

/** What the desktop reports while it answers, in the order it happens. */
sealed interface RemoteEvent {
    data class RunStarted(val runId: String) : RemoteEvent
    data class Delta(val text: String) : RemoteEvent
    data class Commentary(val text: String) : RemoteEvent
    data class ToolStarted(val name: String, val preview: String) : RemoteEvent
    data class ToolFinished(val name: String, val failed: Boolean) : RemoteEvent
    data class ApprovalRequest(
        val runId: String,
        val command: String,
        val description: String,
        val choices: List<String>,
    ) : RemoteEvent
    data class Completed(val content: String) : RemoteEvent
    /** status is completed, failed or cancelled. */
    data class RunEnded(val status: String) : RemoteEvent
    data class Failed(val message: String) : RemoteEvent
    data object Done : RemoteEvent
}

/** Turns the desktop's server-sent frames (an `event:` name and a JSON `data:` body) into [RemoteEvent]s. */
object RemoteEventParser {
    fun parse(name: String, data: String): RemoteEvent? {
        val json = runCatching { JSONObject(data.ifBlank { "{}" }) }.getOrNull() ?: return null
        return when (name) {
            "run.started" -> RemoteEvent.RunStarted(json.optString("run_id"))
            "assistant.delta" -> json.optString("delta").takeIf { it.isNotEmpty() }?.let { RemoteEvent.Delta(it) }
            "assistant.commentary" -> json.optString("text").takeIf { it.isNotBlank() }?.let { RemoteEvent.Commentary(it) }
            "tool.started" -> RemoteEvent.ToolStarted(json.optString("tool_name"), json.optString("preview"))
            "tool.completed" -> RemoteEvent.ToolFinished(json.optString("tool_name"), failed = false)
            "tool.failed" -> RemoteEvent.ToolFinished(json.optString("tool_name"), failed = true)
            "approval.request" -> RemoteEvent.ApprovalRequest(
                runId = json.optString("run_id"),
                command = json.optString("command"),
                description = json.optString("description"),
                choices = json.optJSONArray("choices").toStrings().ifEmpty { listOf("once", "deny") },
            )
            "assistant.completed" -> RemoteEvent.Completed(json.optString("content"))
            "run.completed", "run.failed", "run.cancelled" -> RemoteEvent.RunEnded(name.substringAfter("run."))
            "error" -> RemoteEvent.Failed(json.optString("message").ifBlank { "The desktop reported an error." })
            "done" -> RemoteEvent.Done
            else -> null
        }
    }

    private fun JSONArray?.toStrings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
}

/**
 * Talks to a linked desktop's gateway. Every call blocks, so callers run it off the main thread; failures
 * are [IOException]s with a message a person can read. Nothing here touches the phone's own agent.
 */
class RemoteGatewayClient(
    baseUrl: String,
    private val key: String,
    httpClient: OkHttpClient = OkHttpClient(),
    private val networkGuard: (String) -> Unit = {},
) {
    private val base = HermesEndpointUrl.normalizeBaseUrl(baseUrl)
    private val client = httpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    /** A turn can think for minutes; only the silence between frames (the desktop sends keep-alives) ends it. */
    private val streamClient = client.newBuilder().readTimeout(120, TimeUnit.SECONDS).build()

    fun listSessions(limit: Int = 50): List<RemoteSession> {
        val json = getJson("/api/sessions?limit=$limit")
        val rows = json.optJSONArray("data") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("id").ifBlank { return@mapNotNull null }
            RemoteSession(
                id = id,
                title = row.optString("title"),
                preview = row.optString("preview"),
                lastActive = row.optDouble("last_active", 0.0).takeIf { !it.isNaN() } ?: 0.0,
                messageCount = row.optInt("message_count", 0),
            )
        }
    }

    /** Makes sure the desktop has a conversation with this id (the phone's conversation id), creating it if not. */
    fun ensureSession(id: String, title: String = "") {
        try {
            getJson("/api/sessions/${encode(id)}")
        } catch (error: RemoteHttpException) {
            if (error.code != 404) throw error
            createSession(title, id)
        }
    }

    /** Starts a new desktop conversation and returns its id. */
    fun createSession(title: String = "", id: String = ""): String {
        val body = JSONObject().apply {
            if (title.isNotBlank()) put("title", title)
            if (id.isNotBlank()) put("id", id)
        }
        val json = postJson("/api/sessions", body)
        return json.optString("id").ifBlank { json.optJSONObject("session")?.optString("id").orEmpty() }
            .ifBlank { throw IOException("The desktop did not return a conversation id.") }
    }

    fun messages(sessionId: String): List<RemoteMessage> {
        val json = getJson("/api/sessions/${encode(sessionId)}/messages")
        val rows = json.optJSONArray("data") ?: JSONArray()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val role = row.optString("role")
            if (role != "user" && role != "assistant") return@mapNotNull null
            val content = textOf(row.opt("content"))
            if (content.isBlank()) null else RemoteMessage(role, content)
        }
    }

    /**
     * Sends one message and reports what the desktop does, until it is done. Blocks. [onCall] receives the
     * call so the caller can cancel it, which is how the phone drops a stream it no longer wants.
     */
    fun chat(sessionId: String, text: String, onCall: (Call) -> Unit = {}, onEvent: (RemoteEvent) -> Unit) {
        val url = "$base/api/sessions/${encode(sessionId)}/chat/stream"
        networkGuard(url)
        val body = JSONObject().put("message", text).toString().toRequestBody(JSON)
        val call = streamClient.newCall(authorized(Request.Builder().url(url).post(body)).header("Accept", "text/event-stream").build())
        onCall(call)
        call.execute().use { response ->
            if (!response.isSuccessful) throw RemoteHttpException(response.code, failureMessage(response.code, response.body?.string().orEmpty()))
            val reader = response.body?.charStream()?.buffered() ?: throw IOException("The desktop sent nothing.")
            readFrames(reader) { name, data -> RemoteEventParser.parse(name, data)?.let(onEvent) }
        }
    }

    fun answerApproval(runId: String, choice: String) {
        postJson("/v1/runs/${encode(runId)}/approval", JSONObject().put("choice", choice))
    }

    fun stop(runId: String) {
        postJson("/v1/runs/${encode(runId)}/stop", JSONObject())
    }

    private fun readFrames(reader: BufferedReader, onFrame: (String, String) -> Unit) {
        var name = "message"
        val data = StringBuilder()
        while (true) {
            val line = reader.readLine() ?: break
            when {
                line.isEmpty() -> {
                    if (data.isNotEmpty()) onFrame(name, data.toString())
                    name = "message"
                    data.setLength(0)
                }
                line.startsWith(":") -> Unit // keep-alive comment
                line.startsWith("event:") -> name = line.substring(6).trim()
                line.startsWith("data:") -> {
                    if (data.isNotEmpty()) data.append('\n')
                    data.append(line.substring(5).removePrefix(" "))
                }
            }
        }
        if (data.isNotEmpty()) onFrame(name, data.toString())
    }

    private fun getJson(path: String): JSONObject {
        val url = base + path
        networkGuard(url)
        client.newCall(authorized(Request.Builder().url(url).get()).build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw RemoteHttpException(response.code, failureMessage(response.code, text))
            return runCatching { JSONObject(text) }.getOrElse { throw IOException("The desktop sent something unreadable.") }
        }
    }

    private fun postJson(path: String, body: JSONObject): JSONObject {
        val url = base + path
        networkGuard(url)
        val request = authorized(Request.Builder().url(url).post(body.toString().toRequestBody(JSON))).build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw RemoteHttpException(response.code, failureMessage(response.code, text))
            return runCatching { JSONObject(text.ifBlank { "{}" }) }.getOrDefault(JSONObject())
        }
    }

    private fun authorized(builder: Request.Builder): Request.Builder =
        builder.apply { if (key.isNotBlank()) header("Authorization", "Bearer $key") }

    private fun failureMessage(code: Int, body: String): String = when (code) {
        401, 403 -> "The desktop did not accept the key."
        404 -> "The desktop does not have that conversation."
        409 -> "The desktop is not running that turn any more."
        429 -> "The desktop is busy with other turns. Try again in a moment."
        else -> {
            val detail = runCatching { JSONObject(body).optJSONObject("error")?.optString("message").orEmpty() }.getOrDefault("")
            "The desktop answered $code${if (detail.isNotBlank()) ": $detail" else "."}"
        }
    }

    private fun textOf(content: Any?): String = when (content) {
        is String -> content
        is JSONArray -> (0 until content.length()).joinToString("") { index ->
            content.optJSONObject(index)?.optString("text").orEmpty()
        }
        else -> ""
    }

    private fun encode(segment: String): String = java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
