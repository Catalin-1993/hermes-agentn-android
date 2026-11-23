package com.mobilefork.hermesagent.ui.chat

import com.mobilefork.hermesagent.api.HermesEndpointUrl
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** What the core's judge decided after a turn. */
internal data class GoalDecision(
    val shouldContinue: Boolean,
    /** The message to send next when [shouldContinue]. */
    val prompt: String,
    /** done, continue, blocked, inactive... as the judge named it. */
    val verdict: String,
    /** The judge's own short account, such as "Continuing toward goal (2/12): ...". */
    val message: String,
)

/**
 * The standing-goal loop of the Hermes core, over the local server: the app sets the goal once, sends the
 * agent's reply after each turn, and the core's judge (with its turn budget and quality gates) says whether
 * to go on. Calls block, so they run off the main thread; any failure is an [IOException].
 */
internal class GoalClient(
    baseUrl: String,
    private val apiKey: String?,
    httpClient: OkHttpClient = OkHttpClient(),
) {
    private val base = HermesEndpointUrl.normalizeBaseUrl(baseUrl)
    // The judge calls a model, so a decision can take a while.
    private val client = httpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun set(sessionId: String, goal: String, maxTurns: Int) {
        post(sessionId, JSONObject().put("action", "set").put("goal", goal).put("max_turns", maxTurns))
    }

    fun evaluate(sessionId: String, reply: String): GoalDecision {
        val json = post(sessionId, JSONObject().put("action", "evaluate").put("reply", reply))
        return GoalDecision(
            shouldContinue = json.optBoolean("should_continue", false),
            prompt = json.optString("continuation_prompt").takeIf { it != "null" }.orEmpty(),
            verdict = json.optString("verdict"),
            message = json.optString("message"),
        )
    }

    fun clear(sessionId: String) {
        post(sessionId, JSONObject().put("action", "clear"))
    }

    private fun post(sessionId: String, body: JSONObject): JSONObject {
        val url = "$base/api/goals/${java.net.URLEncoder.encode(sessionId, "UTF-8").replace("+", "%20")}"
        val request = Request.Builder().url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .apply { if (!apiKey.isNullOrBlank()) header("Authorization", "Bearer $apiKey") }
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("The goal judge answered ${response.code}")
            return runCatching { JSONObject(text) }.getOrElse { throw IOException("The goal judge sent something unreadable") }
        }
    }
}
