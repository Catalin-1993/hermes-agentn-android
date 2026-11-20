package com.mobilefork.hermesagent.desktop

import com.mobilefork.hermesagent.api.HermesEndpointUrl
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/** What asking a desktop gateway "are you there?" found. */
sealed interface DesktopProbeResult {
    /** A Hermes gateway answered and accepted the key (or needs none). */
    data class Reachable(val platform: String) : DesktopProbeResult

    /** A gateway answered, but it wants a key the phone does not have or has wrong. */
    data object NeedsKey : DesktopProbeResult

    /** Nothing answered, or what answered is not a Hermes gateway. */
    data class Unreachable(val reason: String) : DesktopProbeResult
}

/**
 * Checks, quickly, whether a desktop gateway can be reached from the phone. It never throws: a closed lid,
 * another Wi-Fi or a wrong address are ordinary answers, not errors.
 */
class DesktopProbe(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .build(),
    private val networkGuard: (String) -> Unit = {},
) {
    fun probe(url: String, key: String = ""): DesktopProbeResult {
        val base = runCatching { HermesEndpointUrl.normalizeBaseUrl(url) }
            .getOrElse { return DesktopProbeResult.Unreachable("That is not an address.") }
        return try {
            val healthUrl = HermesEndpointUrl.healthUrl(base)
            networkGuard(healthUrl)
            val platform = client.newCall(Request.Builder().url(healthUrl).get().build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return DesktopProbeResult.Unreachable("It answered ${response.code}; this does not look like a Hermes desktop.")
                }
                val body = response.body?.string().orEmpty()
                val json = runCatching { org.json.JSONObject(body) }.getOrNull()
                    ?: return DesktopProbeResult.Unreachable("It answered, but not like a Hermes desktop.")
                if (json.optString("status").isBlank()) {
                    return DesktopProbeResult.Unreachable("It answered, but not like a Hermes desktop.")
                }
                json.optString("platform")
            }
            // The health route is open; the key is checked on a route that needs it.
            val modelsUrl = HermesEndpointUrl.modelsUrl(base)
            val request = Request.Builder().url(modelsUrl).get().apply {
                if (key.isNotBlank()) header("Authorization", "Bearer $key")
            }.build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 401 || response.code == 403 -> DesktopProbeResult.NeedsKey
                    response.isSuccessful -> DesktopProbeResult.Reachable(platform)
                    else -> DesktopProbeResult.Unreachable("It answered ${response.code}.")
                }
            }
        } catch (error: Exception) {
            DesktopProbeResult.Unreachable(error.message?.takeIf { it.isNotBlank() } ?: "No answer.")
        }
    }
}
