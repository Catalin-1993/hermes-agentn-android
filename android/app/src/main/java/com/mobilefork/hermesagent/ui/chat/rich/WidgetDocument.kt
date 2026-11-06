package com.mobilefork.hermesagent.ui.chat.rich

/** The app's look, handed to a widget as CSS variables so it follows the theme. All `#rrggbb`. */
internal data class WidgetTheme(
    val dark: Boolean,
    val foreground: String,
    val mutedForeground: String,
    val accent: String,
    val border: String,
    val card: String,
    val well: String,
    val wellBorder: String,
)

/** Where a widget's libraries may come from. Same list as the desktop app; everything else is refused. */
internal val WIDGET_CDN_HOSTS = setOf("cdnjs.cloudflare.com", "cdn.jsdelivr.net", "unpkg.com", "esm.sh")

/** Never resolves on the internet, so the document's origin holds no cookies or storage of anyone's. */
internal const val WIDGET_BASE_URL = "https://hermes-widget.invalid/"
internal const val WIDGET_BRIDGE_NAME = "HermesWidgetBridge"

internal const val WIDGET_MIN_HEIGHT_DP = 96
internal const val WIDGET_MAX_HEIGHT_DP = 1200

// The version the desktop app ships, pinned by hash: a changed file on the CDN simply will not run.
private const val MERMAID_URL = "https://cdn.jsdelivr.net/npm/mermaid@11.17.2/dist/mermaid.min.js"
private const val MERMAID_INTEGRITY = "sha384-EOXBFmc3gx5mb+vn0vPvvGqACToJD24hhacX5Yx+8NUUQrHIle/Qi5Bg9o3zKwW2"

/**
 * Builds what a sealed widget frame loads. The policy comes first and a document cannot loosen it:
 * no connections, frames, forms or plugins, and libraries from [allowedHosts] only. An empty set
 * means inline code only.
 */
internal object WidgetDocument {
    fun contentSecurityPolicy(allowedHosts: Set<String>): String {
        val hosts = allowedHosts.sorted().joinToString(" ") { "https://$it" }
        val extra = if (hosts.isEmpty()) "" else " $hosts"
        return listOf(
            "default-src 'none'",
            "script-src 'unsafe-inline'$extra",
            "style-src 'unsafe-inline'$extra",
            "img-src data: blob:$extra",
            "font-src data:$extra",
            "media-src data: blob:",
            "connect-src 'none'",
            "frame-src 'none'",
            "object-src 'none'",
            "form-action 'none'",
            "base-uri 'none'",
        ).joinToString("; ")
    }

    private fun prelude(theme: WidgetTheme, allowedHosts: Set<String>): String {
        val scheme = if (theme.dark) "dark" else "light"
        val policy = contentSecurityPolicy(allowedHosts)
        return "<!doctype html><meta charset=\"utf-8\">" +
            "<meta http-equiv=\"Content-Security-Policy\" content=\"$policy\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<style>:root{color-scheme:$scheme;--foreground:${theme.foreground};" +
            "--muted-foreground:${theme.mutedForeground};--accent:${theme.accent};--border:${theme.border};" +
            "--card:${theme.card};--surface:${theme.card};--well:${theme.well};--well-border:${theme.wellBorder}}" +
            "*,*::before,*::after{box-sizing:border-box}html{-webkit-text-size-adjust:100%}" +
            "html,body{margin:0;padding:0;background:transparent;color:var(--foreground);" +
            "font-family:system-ui,-apple-system,Roboto,sans-serif;font-size:14px;line-height:1.4;" +
            "overflow-wrap:anywhere}" +
            "img,svg,canvas,video{max-width:100%}</style>"
    }

    /** Runs after the markup: tells the app its height and gives the widget its one voice. */
    private val chrome = """
        <script>(function(){
        var b=window.$WIDGET_BRIDGE_NAME,last=0;
        function post(){var e=document.body;if(!e||!b)return;var h=e.scrollHeight,k=e.children;
        for(var i=0;i<k.length;i++){var r=k[i].getBoundingClientRect();
        if(r.width||r.height){h=Math.max(h,Math.ceil(r.bottom+scrollY))}}
        if(Math.abs(h-last)>1){last=h;b.reportHeight(h)}}
        if(typeof ResizeObserver==="function"){var o=new ResizeObserver(post);
        o.observe(document.documentElement);if(document.body)o.observe(document.body)}
        addEventListener("load",function(){post();setTimeout(post,300);setTimeout(post,1200)});post();
        function send(p){if(typeof p!=="string"||!p.trim()||!b)return Promise.resolve({ok:false,error:"invalid"});
        return Promise.resolve(JSON.parse(b.send(p)))}
        window.hermes={send:send,maxLength:500};
        addEventListener("click",function(e){var el=e.target&&e.target.closest?e.target.closest("[data-hermes-send]"):null;
        if(el)send(el.getAttribute("data-hermes-send")||"")},true);
        })()</script>
    """.trimIndent().replace("\n", "")

    /** A widget or page: the policy and theme before it, the chrome right before its closing body tag. */
    fun forHtml(code: String, theme: WidgetTheme, allowedHosts: Set<String>): String {
        val bodyClose = Regex("</body\\s*>", RegexOption.IGNORE_CASE).find(code)
        val framed = if (bodyClose != null) {
            code.substring(0, bodyClose.range.first) + chrome + code.substring(bodyClose.range.first)
        } else {
            code + chrome
        }
        return prelude(theme, allowedHosts) + framed
    }

    fun forSvg(code: String, theme: WidgetTheme): String =
        prelude(theme, emptySet()) + "<style>svg{display:block;width:100%;height:auto}</style>" + code + chrome

    /** Asks the bridge to fall back to the source when the library cannot load or the source is wrong. */
    fun forMermaid(code: String, theme: WidgetTheme, allowedHosts: Set<String>): String {
        val source = code.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val config = """{startOnLoad:false,securityLevel:'strict',theme:'base',themeVariables:{
            primaryColor:'${theme.well}',primaryBorderColor:'${theme.accent}',primaryTextColor:'${theme.foreground}',
            secondaryColor:'${theme.card}',tertiaryColor:'${theme.card}',lineColor:'${theme.mutedForeground}',
            textColor:'${theme.foreground}',mainBkg:'${theme.well}',nodeBorder:'${theme.accent}',
            fontFamily:'system-ui,Roboto,sans-serif',fontSize:'14px'}}"""
            .replace("\n", "")
            .replace(Regex("\\s{2,}"), "")
        val failed = "window.$WIDGET_BRIDGE_NAME&&window.$WIDGET_BRIDGE_NAME.renderFailed()"
        val script = "<script src=\"$MERMAID_URL\" integrity=\"$MERMAID_INTEGRITY\" crossorigin=\"anonymous\" onerror=\"$failed\"></script>" +
            "<script>window.mermaid&&(mermaid.initialize($config)," +
            "mermaid.run({nodes:[document.getElementById('d')]}).catch(function(){$failed}))</script>"
        return prelude(theme, allowedHosts) +
            "<style>#d{display:flex;justify-content:center}#d svg{height:auto}</style>" +
            "<div id=\"d\">$source</div>" + script + chrome
    }

    fun forBlock(language: RichLanguage, code: String, theme: WidgetTheme, allowedHosts: Set<String>): String =
        when (language) {
            RichLanguage.Mermaid -> forMermaid(code, theme, allowedHosts)
            RichLanguage.Svg -> forSvg(code, theme)
            else -> forHtml(code, theme, allowedHosts)
        }

    /** Whether a request from inside the frame may go out: https, and only to a listed host. */
    fun isRequestAllowed(url: String, allowedHosts: Set<String>): Boolean {
        val match = Regex("^https://([^/:?#]+)(?::443)?(?:[/?#]|$)", RegexOption.IGNORE_CASE).find(url) ?: return false
        return match.groupValues[1].lowercase() in allowedHosts
    }

    fun clampHeight(reported: Int): Int = reported.coerceIn(WIDGET_MIN_HEIGHT_DP, WIDGET_MAX_HEIGHT_DP)
}

/**
 * A widget speaks for the person only right after they touched it, one prompt at a time and at human
 * speed. A script alone cannot put words in the chat, so a widget a tool result talked the model into
 * writing cannot start the agent by itself.
 */
internal object WidgetIntentGate {
    const val MAX_LENGTH = 500
    const val TOUCH_WINDOW_MS = 3_000L
    const val THROTTLE_MS = 1_000L

    fun evaluate(prompt: String, nowMs: Long, lastTouchMs: Long, lastAcceptedMs: Long): String? = when {
        prompt.isBlank() -> "invalid"
        prompt.length > MAX_LENGTH -> "too_long"
        lastTouchMs <= 0L || nowMs - lastTouchMs > TOUCH_WINDOW_MS -> "no_touch"
        lastAcceptedMs > 0L && nowMs - lastAcceptedMs < THROTTLE_MS -> "throttled"
        else -> null
    }
}
