package com.mobilefork.hermesagent.ui.teach

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import com.mobilefork.hermesagent.ui.i18n.LocalChatStrings
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A browser the person uses to show the agent a task once. While recording, the clicks and the text typed
 * (never a password) are written down as steps; when finished they go to the agent, which learns them as a skill.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun TeachBrowserHost(onClose: () -> Unit, onTeach: (String) -> Unit) {
    val words = LocalChatStrings.current
    var steps by remember { mutableStateOf(emptyList<TeachStep>()) }
    var recording by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val recordingFlag = remember { AtomicBoolean(false) }
    val main = remember { Handler(Looper.getMainLooper()) }

    fun load(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        val url = when {
            text.contains(' ') || !text.contains('.') -> "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(text, "UTF-8")
            Regex("^[A-Za-z][A-Za-z0-9+.-]*://").containsMatchIn(text) -> text
            else -> "https://$text"
        }
        webView?.loadUrl(url)
    }

    BackHandler(enabled = true) {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("HermesTeachBrowser"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f).testTag("HermesTeachAddress"),
                singleLine = true,
                label = { Text(words.teachAddress) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { load(address) }),
            )
            TextButton(onClick = onClose, modifier = Modifier.testTag("HermesTeachClose")) { Text("✕") }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun event(json: String) {
                                    if (!recordingFlag.get()) return
                                    val step = parseTeachEvent(json) ?: return
                                    main.post { steps = steps.plusStep(step) }
                                }
                            },
                            "HermesTeach",
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                address = url
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                view.evaluateJavascript(RECORDER_JS, null)
                                if (recordingFlag.get() && url.startsWith("http")) steps = steps.plusStep(TeachStep.Open(url))
                            }
                        }
                        webView = this
                    }
                },
                onRelease = { it.destroy(); webView = null },
            )
            if (steps.isEmpty() && !recording && address.isBlank()) {
                Text(
                    words.teachHint,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    recording = !recording
                    recordingFlag.set(recording)
                    // Starting here also counts the page that is already open as the first step.
                    val current = webView?.url
                    if (recording && steps.isEmpty() && current != null && current.startsWith("http")) {
                        steps = steps.plusStep(TeachStep.Open(current))
                    }
                },
                modifier = Modifier.testTag("HermesTeachRecord"),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (recording) Color(0xFFE5453B) else MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Text(
                    if (recording) words.teachStopRecording else words.teachRecord,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = if (recording) words.teachRecording else "",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = words.teachSteps.format(steps.size),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.testTag("HermesTeachStepCount"),
            )
            Button(
                onClick = { onTeach(buildTeachPrompt(steps)) },
                enabled = steps.isNotEmpty() && !recording,
                modifier = Modifier.testTag("HermesTeachFinish"),
            ) { Text(words.teachFinish) }
        }
    }
}

/** Runs inside each page: reports clicks and typed values to the app. Password fields are never reported. */
private val RECORDER_JS = """
(function () {
  if (window.__hermesTeach) return;
  window.__hermesTeach = true;
  function css(e) {
    if (!e || e.nodeType !== 1) return '';
    if (e.id) return '#' + (window.CSS && CSS.escape ? CSS.escape(e.id) : e.id);
    var n = e.getAttribute('name');
    if (n) return e.tagName.toLowerCase() + '[name="' + n + '"]';
    var a = e.getAttribute('aria-label');
    if (a) return e.tagName.toLowerCase() + '[aria-label="' + a + '"]';
    var parts = [];
    while (e && e.nodeType === 1 && parts.length < 4) {
      var i = 1, s = e;
      while ((s = s.previousElementSibling)) { if (s.tagName === e.tagName) i++; }
      parts.unshift(e.tagName.toLowerCase() + ':nth-of-type(' + i + ')');
      e = e.parentElement;
    }
    return parts.join(' > ');
  }
  function field(e) {
    return ((e.getAttribute('aria-label') || e.getAttribute('placeholder') || e.name || e.id || '') + '').trim().slice(0, 80);
  }
  function label(e) {
    var tag = (e.tagName || '').toLowerCase();
    if (tag === 'input' || tag === 'textarea' || tag === 'select') return field(e);
    return ((e.innerText || e.getAttribute('aria-label') || '') + '').trim().slice(0, 80);
  }
  document.addEventListener('click', function (ev) {
    var t = (ev.target.closest && ev.target.closest('a,button,input,select,textarea,[role=button],[role=link],[onclick]')) || ev.target;
    HermesTeach.event(JSON.stringify({ type: 'click', selector: css(t), text: label(t) }));
  }, true);
  document.addEventListener('change', function (ev) {
    var t = ev.target;
    if (!t || !t.tagName) return;
    var v = (t.type === 'password') ? '[hidden]' : String(t.value || '').slice(0, 200);
    HermesTeach.event(JSON.stringify({ type: 'input', selector: css(t), text: field(t), value: v }));
  }, true);
})();
"""
