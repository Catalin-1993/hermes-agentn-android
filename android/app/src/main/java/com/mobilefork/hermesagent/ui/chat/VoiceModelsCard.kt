package com.mobilefork.hermesagent.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.OutlinedButton
import com.mobilefork.hermesagent.ui.theme.TextButton
import com.mobilefork.hermesagent.voice.LocalSpeechEngine
import com.mobilefork.hermesagent.voice.LocalTtsEngine
import com.mobilefork.hermesagent.voice.LocalTtsRequest
import com.mobilefork.hermesagent.voice.VoiceModelCatalog
import com.mobilefork.hermesagent.voice.VoiceModelPhase
import com.mobilefork.hermesagent.voice.VoiceModelSpec
import com.mobilefork.hermesagent.voice.VoiceModelStatus
import com.mobilefork.hermesagent.voice.VoiceModelStore
import com.mobilefork.hermesagent.voice.VoiceSampleRecorder
import com.mobilefork.hermesagent.voice.VoiceSamples
import java.util.Locale
import kotlin.concurrent.thread

/**
 * The voice manager: every voice and speech detector that can be downloaded to this phone, each with
 * its own Download, Use, Listen and Remove. Several can stay on the phone; one voice reads replies.
 */
@Composable
internal fun VoiceModelsCard(settings: VoiceSettings) {
    val context = LocalContext.current
    val glass = LocalHermesGlassTokens.current
    val statuses by VoiceModelStore.statuses.collectAsState()
    LaunchedEffect(Unit) { VoiceModelStore.refresh(context) }
    var chosenVoice by rememberSaveable { mutableStateOf(settings.localVoiceModel) }
    var chosenDetector by rememberSaveable { mutableStateOf(settings.speechDetector) }
    var speed by remember { mutableFloatStateOf(settings.localVoiceSpeed) }

    Column(modifier = Modifier.fillMaxWidth().testTag("HermesVoiceModels"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Voices on this phone", style = MaterialTheme.typography.titleMedium)
        Text(
            "Download a voice, then choose it. It reads replies aloud without the internet, and the phone's own voice reads when a voice does not speak the language you use.",
            style = MaterialTheme.typography.bodySmall,
            color = glass.mutedText,
        )
        VoiceModelCatalog.voices.forEach { spec ->
            VoiceModelRow(
                spec = spec,
                status = statuses[spec.id] ?: VoiceModelStatus(),
                chosen = chosenVoice == spec.id,
                onChoose = {
                    chosenVoice = spec.id
                    settings.localVoiceModel = spec.id
                },
            ) { VoiceExtras(spec, settings, speed) }
        }
        Column {
            Text("Speaking speed ${"%.1f".format(Locale.ROOT, speed)}x", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    settings.localVoiceSpeed = it
                },
                valueRange = 0.6f..1.6f,
                modifier = Modifier.testTag("HermesVoiceSpeed"),
            )
        }

        Text("Hearing when you speak", style = MaterialTheme.typography.titleMedium)
        Text(
            "A tiny model that notices the moment you start and stop talking, instead of only measuring volume. It is used when you speak to the agent.",
            style = MaterialTheme.typography.bodySmall,
            color = glass.mutedText,
        )
        VoiceModelCatalog.detectors.forEach { spec ->
            VoiceModelRow(
                spec = spec,
                status = statuses[spec.id] ?: VoiceModelStatus(),
                chosen = chosenDetector == spec.id,
                onChoose = {
                    chosenDetector = spec.id
                    settings.speechDetector = spec.id
                },
                onUnchoose = {
                    chosenDetector = ""
                    settings.speechDetector = ""
                },
            ) {}
        }
    }
}

@Composable
private fun VoiceModelRow(
    spec: VoiceModelSpec,
    status: VoiceModelStatus,
    chosen: Boolean,
    onChoose: () -> Unit,
    onUnchoose: (() -> Unit)? = null,
    extras: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val glass = LocalHermesGlassTokens.current
    ShellGroupCard(modifier = Modifier.testTag("HermesVoiceModel_${spec.id}")) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(spec.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Text("${spec.downloadMegabytes} MB", style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
            }
            if (spec.isVoice) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    spec.languages.sorted().take(6).forEach { Chip(it.uppercase(Locale.ROOT)) }
                    if (spec.languages.size > 6) Chip("+${spec.languages.size - 6}")
                    if (spec.canClone) Chip("Clones a voice")
                }
            }
            Text(spec.summary, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            when (status.phase) {
                VoiceModelPhase.Ready -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onUnchoose == null) {
                            RadioButton(selected = chosen, onClick = onChoose, modifier = Modifier.testTag("HermesVoiceUse_${spec.id}"))
                            Text(if (chosen) "In use" else "Use this voice", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Switch(
                                checked = chosen,
                                onCheckedChange = { if (it) onChoose() else onUnchoose() },
                                modifier = Modifier.testTag("HermesVoiceUse_${spec.id}"),
                            )
                            Text(if (chosen) "  In use" else "  Use it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    extras()
                    TextButton(
                        onClick = { VoiceModelStore.remove(context, spec.id) },
                        modifier = Modifier.testTag("HermesVoiceRemove_${spec.id}"),
                    ) { Text("Remove from phone") }
                }
                VoiceModelPhase.Downloading -> {
                    LinearProgressIndicator(progress = { status.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${status.progress}%", style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                    TextButton(onClick = { VoiceModelStore.cancel(spec.id) }, modifier = Modifier.testTag("HermesVoiceCancel_${spec.id}")) {
                        Text("Cancel")
                    }
                }
                VoiceModelPhase.Unpacking -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Unpacking…", style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                }
                else -> {
                    if (status.phase == VoiceModelPhase.Error && status.message.isNotBlank()) {
                        Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { VoiceModelStore.download(context, spec.id) },
                        modifier = Modifier.fillMaxWidth().testTag("HermesVoiceDownload_${spec.id}"),
                    ) { Text(if (status.phase == VoiceModelPhase.Error) "Try again" else "Download ${spec.downloadMegabytes} MB") }
                }
            }
            Text(spec.credit, style = MaterialTheme.typography.labelSmall, color = glass.mutedText)
        }
    }
}

@Composable
private fun Chip(text: String) {
    val glass = LocalHermesGlassTokens.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(glass.accentContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
    )
}

/** Choosing one of a voice's speakers, listening to it, and for cloning voices the recording they copy. */
@Composable
private fun VoiceExtras(spec: VoiceModelSpec, settings: VoiceSettings, speed: Float) {
    val context = LocalContext.current
    val glass = LocalHermesGlassTokens.current
    var voice by remember(spec.id) { mutableIntStateOf(settings.localVoice(spec.id).coerceIn(0, spec.voiceNames.lastIndex)) }
    var speaking by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf("") }
    val languageTag = remember { AppSettingsStore(context).load().languageTag.ifBlank { Locale.getDefault().toLanguageTag() } }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (spec.voiceNames.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        voice = (voice - 1 + spec.voiceNames.size) % spec.voiceNames.size
                        settings.setLocalVoice(spec.id, voice)
                    },
                    modifier = Modifier.testTag("HermesVoicePrev_${spec.id}"),
                ) { Text("‹") }
                Text(
                    "${spec.voiceNames[voice]}  (${voice + 1} of ${spec.voiceNames.size})",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = {
                        voice = (voice + 1) % spec.voiceNames.size
                        settings.setLocalVoice(spec.id, voice)
                    },
                    modifier = Modifier.testTag("HermesVoiceNext_${spec.id}"),
                ) { Text("›") }
            }
        }
        // Bumped whenever the recording or its words change, so Listen follows what is there now.
        var revision by remember { mutableIntStateOf(0) }
        if (spec.canClone) CloneRecording(spec, onChanged = { revision += 1 })
        val reference = remember(spec.id, revision) { VoiceSamples.referenceFor(context, spec) }
        OutlinedButton(
            onClick = {
                if (speaking) {
                    LocalTtsEngine.stop()
                    speaking = false
                } else {
                    problem = ""
                    val tag = if (spec.speaks(languageTag)) languageTag else "en"
                    val current = if (spec.canClone) VoiceSamples.referenceFor(context, spec) else null
                    speaking = true
                    LocalTtsEngine.speak(
                        context,
                        LocalTtsRequest(
                            spec = spec,
                            text = sampleSentence(tag),
                            languageTag = tag,
                            voice = settings.localVoice(spec.id),
                            speed = speed,
                            reference = current,
                        ),
                    ) { failed, message ->
                        speaking = false
                        if (failed) problem = message.ifBlank { "The voice could not speak." }
                    }
                }
            },
            enabled = !spec.canClone || reference != null,
            modifier = Modifier.fillMaxWidth().testTag("HermesVoiceListen_${spec.id}"),
        ) { Text(if (speaking) "Stop" else "Listen") }
        if (spec.canClone && reference == null) {
            Text(
                if (spec.needsReferenceText) "Record your voice and type what you said, then listen." else "Record your voice, then listen.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
            )
        }
        if (problem.isNotBlank()) Text(problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/** Recording the voice a cloning model copies, and the words said in it when the model needs them. */
@Composable
private fun CloneRecording(spec: VoiceModelSpec, onChanged: () -> Unit) {
    val context = LocalContext.current
    val glass = LocalHermesGlassTokens.current
    var hasOwn by remember { mutableStateOf(VoiceSamples.hasOwn(context)) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf(VoiceSamples.ownText(context)) }
    var working by remember { mutableStateOf(false) }
    val recorder = remember {
        VoiceSampleRecorder(
            onTick = { seconds = it },
            onDone = { samples ->
                recording = false
                if (samples == null) {
                    message = "The microphone could not start."
                } else if (samples.size < VoiceSamples.MIN_SECONDS * 16_000) {
                    message = "That was too short. Speak for at least ${VoiceSamples.MIN_SECONDS.toInt()} seconds."
                } else {
                    VoiceSamples.saveOwn(context, samples, 16_000)
                    hasOwn = true
                    onChanged()
                    message = "Saved ${samples.size / 16_000} seconds of your voice."
                }
            },
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recording = true
            seconds = 0
            recorder.start()
        } else {
            message = "The microphone is needed to record your voice."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            when {
                hasOwn -> "Copying: your recording"
                spec.needsReferenceText -> "Copying: nothing yet"
                else -> "Copying: the demo voice that comes with the model"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (recording) {
                        recorder.stop()
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        message = ""
                        recording = true
                        seconds = 0
                        recorder.start()
                    } else {
                        permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f).testTag("HermesVoiceRecord"),
            ) { Text(if (recording) "Stop (${seconds}s)" else if (hasOwn) "Record again" else "Record my voice") }
            if (hasOwn && !recording) {
                OutlinedButton(
                    onClick = {
                        VoiceSamples.removeOwn(context)
                        hasOwn = false
                        transcript = ""
                        message = ""
                        onChanged()
                    },
                    modifier = Modifier.testTag("HermesVoiceRecordingDelete"),
                ) { Text("Delete") }
            }
        }
        Text(
            "Speak clearly for 8 to ${VoiceSamples.MAX_SECONDS} seconds, in a quiet room, in the language the voice will speak.",
            style = MaterialTheme.typography.bodySmall,
            color = glass.mutedText,
        )
        if (spec.needsReferenceText && hasOwn) {
            OutlinedTextField(
                value = transcript,
                onValueChange = {
                    transcript = it
                    VoiceSamples.saveOwnText(context, it)
                    onChanged()
                },
                label = { Text("The words you said, exactly") },
                modifier = Modifier.fillMaxWidth().testTag("HermesVoiceTranscript"),
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
            )
            if (LocalSpeechEngine.isAvailable(context)) {
                OutlinedButton(
                    onClick = {
                        working = true
                        thread(name = "hermes-voice-transcript") {
                            val text = runCatching {
                                VoiceSamples.ownSamples(context)?.let { LocalSpeechEngine.transcribe(context, it) }.orEmpty()
                            }.getOrDefault("")
                            VoiceSamples.saveOwnText(context, text)
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                transcript = text
                                onChanged()
                                working = false
                                if (text.isBlank()) message = "The speech model could not hear any words. Type them instead."
                            }
                        }
                    },
                    enabled = !working,
                    modifier = Modifier.fillMaxWidth().testTag("HermesVoiceTranscribe"),
                ) { Text(if (working) "Listening…" else "Fill in from the recording") }
            }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
    }
}

private fun sampleSentence(languageTag: String): String = when (languageTag.substringBefore('-').lowercase()) {
    "ro" -> "Bună ziua! Mă numesc Brain și sunt asistentul tău personal. Astăzi calendarul tău arată o întâlnire la ora zece."
    "fr" -> "Bonjour ! Je m'appelle Brain, votre assistant personnel. Aujourd'hui, votre agenda indique une réunion à dix heures."
    "de" -> "Guten Tag! Ich bin Brain, dein persönlicher Assistent. Heute zeigt dein Kalender einen Termin um zehn Uhr."
    "es" -> "¡Hola! Soy Brain, tu asistente personal. Hoy tu calendario muestra una reunión a las diez."
    "it" -> "Ciao! Sono Brain, il tuo assistente personale. Oggi il tuo calendario mostra una riunione alle dieci."
    else -> "Hello! I am Brain, your personal assistant. Today your calendar shows a meeting at ten o'clock."
}
