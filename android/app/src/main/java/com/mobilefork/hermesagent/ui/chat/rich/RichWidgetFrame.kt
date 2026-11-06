package com.mobilefork.hermesagent.ui.chat.rich

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobilefork.hermesagent.BuildConfig
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.HermesNetworkPolicy
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** Keeps a scrolled-away widget's height, so coming back to it never moves the conversation. */
private val widgetHeights = LruCache<String, Int>(64)

@Composable
internal fun rememberWidgetTheme(): WidgetTheme {
    val glass = LocalHermesGlassTokens.current
    val scheme = MaterialTheme.colorScheme
    return remember(glass, scheme) {
        WidgetTheme(
            dark = scheme.surface.luminance() < 0.4f,
            foreground = scheme.onSurface.toCssHex(),
            mutedForeground = glass.mutedText.toCssHex(),
            accent = scheme.primary.toCssHex(),
            border = scheme.onSurface.copy(alpha = 0.14f).compositeOver(glass.bubbleAgent).toCssHex(),
            card = glass.bubbleAgent.toCssHex(),
            well = scheme.onSurface.copy(alpha = 0.05f).compositeOver(glass.bubbleAgent).toCssHex(),
            wellBorder = scheme.onSurface.copy(alpha = 0.09f).compositeOver(glass.bubbleAgent).toCssHex(),
        )
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}

private fun Color.toCssHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

/** Hosts this build may load libraries from: none on the Play edition. */
private fun widgetHosts(): Set<String> = if (BuildConfig.HERMES_PLAY_EDITION) emptySet() else WIDGET_CDN_HOSTS

/**
 * A reply's HTML, diagram or drawing, live in the chat. Sits as tall as its content, so the chat
 * keeps its one scroll: vertical drags move the conversation, sideways drags reach the widget.
 */
@Composable
internal fun RichWidgetFrame(
    language: RichLanguage,
    code: String,
    onSendPrompt: (String) -> Unit,
    onRenderFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = rememberWidgetTheme()
    val cacheKey = remember(code, language) { "${language.fence}:${code.hashCode()}:${code.length}" }
    var heightDp by remember(cacheKey) { mutableIntStateOf(widgetHeights.get(cacheKey) ?: 0) }
    val shownHeight by animateDpAsState(
        targetValue = if (heightDp > 0) heightDp.dp else 140.dp,
        animationSpec = tween(durationMillis = 180),
        label = "widgetHeight",
    )
    val send by rememberUpdatedState(onSendPrompt)
    val failed by rememberUpdatedState(onRenderFailed)
    key(cacheKey, theme) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(shownHeight)
                .testTag("HermesWidgetFrame"),
            factory = { context ->
                ChatDragKeepingFrame(context).apply {
                    val web = createWidgetWebView(
                        context = context,
                        document = WidgetDocument.forBlock(language, code, theme, widgetHosts()),
                        onHeight = { reported ->
                            val clamped = WidgetDocument.clampHeight(reported)
                            widgetHeights.put(cacheKey, clamped)
                            heightDp = clamped
                        },
                        onSend = { prompt -> send(prompt) },
                        onFailed = { failed() },
                    )
                    if (web == null) failed() else addView(web, matchParent())
                }
            },
            onRelease = { frame -> frame.releaseWebViews() },
        )
    }
}

/** The same widget filling the screen, where it can scroll and zoom. */
@Composable
internal fun RichWidgetFullScreen(
    language: RichLanguage,
    code: String,
    onSendPrompt: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = rememberWidgetTheme()
    val shell = LocalShellStrings.current
    val send by rememberUpdatedState(onSendPrompt)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .testTag("HermesWidgetFullScreen"),
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                factory = { context ->
                    FrameLayout(context).apply {
                        createWidgetWebView(
                            context = context,
                            document = WidgetDocument.forBlock(language, code, theme, widgetHosts()),
                            onHeight = {},
                            onSend = { prompt ->
                                onDismiss()
                                send(prompt)
                            },
                            onFailed = onDismiss,
                            zoomable = true,
                        )?.let { addView(it, matchParent()) }
                    }
                },
                onRelease = { frame -> frame.releaseWebViews() },
            )
            ShellRoundButton(
                iconRes = R.drawable.ic_action_close,
                contentDescription = shell.extra.widgetClose,
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                testTag = "HermesWidgetFullScreenClose",
            )
        }
    }
}

private fun matchParent() = FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT,
)

private fun ViewGroup.releaseWebViews() {
    for (index in 0 until childCount) {
        (getChildAt(index) as? WebView)?.run {
            stopLoading()
            webChromeClient = null
            removeJavascriptInterface(WIDGET_BRIDGE_NAME)
            destroy()
        }
    }
    removeAllViews()
}

/**
 * WebView asks its parent to stop stealing touches whenever it likes. Ignoring that leaves the chat
 * in charge of vertical drags, while a sideways drag, which the chat never claims, stays with the widget.
 */
private class ChatDragKeepingFrame(context: Context) : FrameLayout(context) {
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}

/**
 * The sealed frame: scripts run, but with no file or content access, no storage, no pop-ups, no
 * permissions, and no way out except a link the person taps and the library hosts on the list.
 * Null when the device has no working WebView.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun createWidgetWebView(
    context: Context,
    document: String,
    onHeight: (Int) -> Unit,
    onSend: (String) -> Unit,
    onFailed: () -> Unit,
    zoomable: Boolean = false,
): WebView? {
    val web = runCatching { WebView(context) }.getOrNull() ?: return null
    val appContext = context.applicationContext
    val hosts = widgetHosts()
    val main = Handler(Looper.getMainLooper())
    var lastTouchMs = 0L
    var lastAcceptedMs = 0L
    web.setOnTouchListener { _, _ ->
        lastTouchMs = SystemClock.elapsedRealtime()
        false
    }
    web.setBackgroundColor(AndroidColor.TRANSPARENT)
    web.isVerticalScrollBarEnabled = zoomable
    web.isHorizontalScrollBarEnabled = false
    web.overScrollMode = WebView.OVER_SCROLL_NEVER
    web.settings.apply {
        javaScriptEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        domStorageEnabled = false
        databaseEnabled = false
        setGeolocationEnabled(false)
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        mediaPlaybackRequiresUserGesture = true
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        saveFormData = false
        textZoom = 100
        useWideViewPort = true
        loadWithOverviewMode = false
        setSupportZoom(zoomable)
        builtInZoomControls = zoomable
        displayZoomControls = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
    web.addJavascriptInterface(
        object {
            @JavascriptInterface
            fun reportHeight(height: Int) {
                main.post { onHeight(height) }
            }

            @JavascriptInterface
            fun renderFailed() {
                main.post(onFailed)
            }

            @JavascriptInterface
            fun send(prompt: String): String {
                val now = SystemClock.elapsedRealtime()
                val rejected = WidgetIntentGate.evaluate(prompt, now, lastTouchMs, lastAcceptedMs)
                if (rejected != null) return JSONObject().put("ok", false).put("error", rejected).toString()
                lastAcceptedMs = now
                main.post { onSend(prompt.trim()) }
                return JSONObject().put("ok", true).toString()
            }
        },
        WIDGET_BRIDGE_NAME,
    )
    web.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (request.isForMainFrame && request.hasGesture() && url.scheme == "https") {
                runCatching {
                    appContext.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            return true
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()
            val allowed = WidgetDocument.isRequestAllowed(url, hosts) &&
                !HermesNetworkPolicy.isExternalNetworkBlocked(appContext, url)
            return if (allowed) null else blockedResponse()
        }

        // Without this a crashed renderer takes the whole app down with it.
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            onFailed()
            return true
        }
    }
    web.webChromeClient = object : WebChromeClient() {
        override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
            result.cancel()
            return true
        }

        override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
            result.cancel()
            return true
        }

        override fun onJsPrompt(
            view: WebView,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult,
        ): Boolean {
            result.cancel()
            return true
        }
    }
    web.loadDataWithBaseURL(WIDGET_BASE_URL, document, "text/html", "utf-8", null)
    return web
}

private fun blockedResponse() = WebResourceResponse(
    "text/plain",
    "utf-8",
    403,
    "Blocked",
    emptyMap(),
    ByteArrayInputStream(ByteArray(0)),
)
