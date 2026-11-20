package com.mobilefork.hermesagent.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.data.HermesNetworkPolicy
import com.mobilefork.hermesagent.ui.i18n.LocalChatStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Whether this run of the app is connected to the linked desktop. It starts disconnected: the phone's own
 * agent is what answers until the person says otherwise, and it stays available when the desktop goes away.
 */
object DesktopConnection {
    private val connectedFlow = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = connectedFlow

    /** True once the offer was shown in this run, so the person is asked at most once. */
    @Volatile var offered: Boolean = false

    fun setConnected(value: Boolean) {
        connectedFlow.value = value
    }
}

/**
 * Checks the linked desktop when the app opens and, if it answers, asks once: connect, or stay local.
 * Shows nothing when no desktop is linked, when it does not answer, or when the person already chose.
 */
@Composable
fun DesktopOfferHost() {
    val context = LocalContext.current
    val words = LocalChatStrings.current
    val store = remember { DesktopLinkStore(context) }
    var offerFor by remember { mutableStateOf<DesktopLink?>(null) }

    LaunchedEffect(Unit) {
        val link = store.link() ?: return@LaunchedEffect
        val answer = store.answer()
        if (answer != DesktopOfferAnswer.Ask || DesktopConnection.offered) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            DesktopProbe(networkGuard = { url -> HermesNetworkPolicy.requireExternalNetworkAllowed(context, url, "desktop check") })
                .probe(link.url, store.key())
        }
        if (DesktopOfferPolicy.shouldAsk(link, result, answer, DesktopConnection.offered)) {
            DesktopConnection.offered = true
            offerFor = link
        }
    }

    val link = offerFor ?: return
    AlertDialog(
        onDismissRequest = { offerFor = null },
        title = { Text(words.desktopOfferTitle) },
        text = { Text(words.desktopOfferBody.format(link.name.ifBlank { link.url })) },
        confirmButton = {
            TextButton(
                onClick = {
                    DesktopConnection.setConnected(true)
                    offerFor = null
                },
                modifier = Modifier.testTag("HermesDesktopConnect"),
            ) { Text(words.desktopOfferConnect) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    store.setAnswer(DesktopOfferAnswer.StayLocal)
                    DesktopConnection.setConnected(false)
                    offerFor = null
                },
                modifier = Modifier.testTag("HermesDesktopStayLocal"),
            ) { Text(words.desktopOfferStayLocal) }
        },
    )
}

/** Settings card: link a desktop with an address or a pairing code, check it, or unlink it. */
@Composable
fun DesktopLinkCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val words = LocalChatStrings.current
    val store = remember { DesktopLinkStore(context) }
    val scope = rememberCoroutineScope()
    var linked by remember { mutableStateOf(store.link()) }
    var address by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun describe(result: DesktopProbeResult): String = when (result) {
        is DesktopProbeResult.Reachable -> words.desktopLinkReachable
        DesktopProbeResult.NeedsKey -> words.desktopLinkNeedsKey
        is DesktopProbeResult.Unreachable -> "${words.desktopLinkUnreachable}. ${result.reason}"
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(words.desktopLinkTitle, style = MaterialTheme.typography.titleMedium)
        Text(words.desktopLinkNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val current = linked
        if (current != null) {
            Text(current.name.ifBlank { current.url }, style = MaterialTheme.typography.bodyMedium)
            LaunchedEffect(current.url) {
                busy = true
                status = words.desktopLinkChecking
                val result = withContext(Dispatchers.IO) {
                    DesktopProbe(networkGuard = { HermesNetworkPolicy.requireExternalNetworkAllowed(context, it, "desktop check") })
                        .probe(current.url, store.key())
                }
                status = describe(result)
                busy = false
            }
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("HermesDesktopStatus"))
            }
            TextButton(
                onClick = {
                    store.forget()
                    DesktopConnection.setConnected(false)
                    linked = null
                    status = ""
                },
                modifier = Modifier.testTag("HermesDesktopForget"),
            ) { Text(words.desktopLinkForget) }
        } else {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(words.desktopLinkAddress) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("HermesDesktopAddress"),
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text(words.desktopLinkKey) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("HermesDesktopKey"),
            )
            Row {
                TextButton(
                    enabled = !busy && address.isNotBlank(),
                    onClick = {
                        val pairing = DesktopPairingCode.parse(address)
                        val withKey = pairing?.let { if (key.isNotBlank()) it.copy(key = key.trim()) else it }
                        if (withKey == null) {
                            status = words.desktopLinkInvalid
                        } else scope.launch {
                            busy = true
                            status = words.desktopLinkChecking
                            val result = withContext(Dispatchers.IO) {
                                DesktopProbe(networkGuard = { HermesNetworkPolicy.requireExternalNetworkAllowed(context, it, "desktop check") })
                                    .probe(withKey.url, withKey.key)
                            }
                            status = describe(result)
                            busy = false
                            // A desktop that is off right now is still worth keeping: the link is the person's choice.
                            store.save(withKey)
                            linked = store.link()
                            address = ""
                            key = ""
                        }
                    },
                    modifier = Modifier.testTag("HermesDesktopLink"),
                ) { Text(words.desktopLinkCheck) }
            }
            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
