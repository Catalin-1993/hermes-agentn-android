package com.mobilefork.hermesagent.desktop

import com.mobilefork.hermesagent.api.HermesEndpointUrl
import org.json.JSONObject

/** What a pairing code or a typed address says about the desktop: where it is, its key, and its name. */
data class DesktopPairing(
    val url: String,
    val key: String = "",
    val name: String = "",
)

/**
 * Reads what the person gives to link a desktop: the text of a QR code, a pasted line, or just an address.
 * Accepted forms, so the desktop can show whichever it has:
 *  - JSON: {"url": "...", "key": "...", "name": "..."}
 *  - a link: hermes-pair://host:port?key=...&name=...&tls=1
 *  - a plain address: https://my-mac.tailnet.ts.net or my-mac.tailnet.ts.net
 */
object DesktopPairingCode {
    fun parse(raw: String): DesktopPairing? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        return when {
            text.startsWith("{") -> fromJson(text)
            text.startsWith(PAIR_SCHEME, ignoreCase = true) -> fromPairLink(text)
            else -> fromAddress(text)
        }
    }

    private fun fromJson(text: String): DesktopPairing? {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        val url = json.optString("url").ifBlank { json.optString("address") }
        return build(url, json.optString("key").ifBlank { json.optString("token") }, json.optString("name"))
    }

    private fun fromPairLink(text: String): DesktopPairing? {
        val rest = text.substring(PAIR_SCHEME.length)
        val authority = rest.substringBefore('?').trimEnd('/')
        val query = rest.substringAfter('?', "")
        val values = query.split('&').filter { it.contains('=') }.associate {
            it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }
        val scheme = if (values["tls"] == "0") "http" else "https"
        return build("$scheme://$authority", values["key"].orEmpty(), values["name"].orEmpty())
    }

    private fun fromAddress(text: String): DesktopPairing? =
        if (text.any { it.isWhitespace() }) null else build(text, "", "")

    private fun build(url: String, key: String, name: String): DesktopPairing? {
        if (url.isBlank()) return null
        val normalized = runCatching { HermesEndpointUrl.normalizeBaseUrl(url) }.getOrNull() ?: return null
        // A host with no dot and no port is not an address (a stray word); a desktop needs a real one.
        val authority = normalized.substringAfter("://").substringBefore('/')
        if (authority.isBlank() || authority.any { it.isWhitespace() }) return null
        return DesktopPairing(url = normalized, key = key.trim(), name = name.trim())
    }

    private const val PAIR_SCHEME = "hermes-pair://"
}
