package com.mobilefork.hermesagent.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.BuildConfig
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.ShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellChevron
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.shell.ShellRowIcon
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.OutlinedButton
import com.mobilefork.hermesagent.ui.theme.TextButton
import com.mobilefork.hermesagent.update.AppInstaller
import com.mobilefork.hermesagent.update.AppRelease
import com.mobilefork.hermesagent.update.AppUpdateClient
import com.mobilefork.hermesagent.update.AppUpdateParser
import com.mobilefork.hermesagent.update.InstallStart
import com.mobilefork.hermesagent.update.UpdateCheckResult
import com.mobilefork.hermesagent.update.UpdateError
import com.mobilefork.hermesagent.update.UpdateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Where the app looks for new releases until the user points it somewhere else. */
const val DEFAULT_UPDATE_REPO = "Catalin-1993/hermes-agentn-android"

enum class UpdatePhase { Idle, Checking, UpToDate, Available, Downloading, Ready, NoRelease, Error }

data class AppUpdateUiState(
    val phase: UpdatePhase = UpdatePhase.Idle,
    val release: AppRelease? = null,
    val progress: Float = 0f,
    val error: UpdateError? = null,
    val installNote: InstallStart? = null,
    val repo: String = DEFAULT_UPDATE_REPO,
    val apk: File? = null,
)

/** Checks, downloads and hands over an update, only when the user asks for it. */
class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = AppUpdateClient()
    private val updatesDir = File(application.cacheDir, "updates")
    private val _uiState = MutableStateFlow(
        AppUpdateUiState(repo = prefs.getString(KEY_REPO, DEFAULT_UPDATE_REPO)?.takeIf(AppUpdateParser::isValidRepo) ?: DEFAULT_UPDATE_REPO),
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()
    val currentVersion: String = BuildConfig.VERSION_NAME

    fun check() {
        val phase = _uiState.value.phase
        if (phase == UpdatePhase.Checking || phase == UpdatePhase.Downloading) return
        _uiState.update { it.copy(phase = UpdatePhase.Checking, error = null, installNote = null) }
        viewModelScope.launch {
            val repo = _uiState.value.repo
            val result = runCatching { withContext(Dispatchers.IO) { client.check(repo, currentVersion) } }
            result.onSuccess { found ->
                _uiState.update {
                    when (found) {
                        is UpdateCheckResult.Available -> it.copy(phase = UpdatePhase.Available, release = found.release, apk = null)
                        is UpdateCheckResult.UpToDate -> {
                            updatesDir.deleteRecursively()
                            it.copy(phase = UpdatePhase.UpToDate, release = null, apk = null)
                        }
                        UpdateCheckResult.NoRelease -> it.copy(phase = UpdatePhase.NoRelease, release = null, apk = null)
                    }
                }
            }.onFailure { failed -> fail(failed) }
        }
    }

    fun download() {
        val release = _uiState.value.release ?: return
        if (_uiState.value.phase == UpdatePhase.Downloading) return
        _uiState.update { it.copy(phase = UpdatePhase.Downloading, progress = 0f, error = null, installNote = null) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    client.download(release, updatesDir) { done, total ->
                        if (total > 0) _uiState.update { it.copy(progress = (done.toFloat() / total).coerceIn(0f, 1f)) }
                    }
                }
            }
            result.onSuccess { file ->
                _uiState.update { it.copy(phase = UpdatePhase.Ready, apk = file, progress = 1f) }
            }.onFailure { failed -> fail(failed) }
        }
    }

    fun install(context: Context) {
        val apk = _uiState.value.apk?.takeIf { it.exists() } ?: return
        val started = AppInstaller.install(context, apk)
        _uiState.update { it.copy(installNote = started) }
    }

    /** Saves a new update source. Returns false, and keeps the old one, when it is not owner/repository. */
    fun setRepo(value: String): Boolean {
        val repo = value.trim()
        if (!AppUpdateParser.isValidRepo(repo)) return false
        prefs.edit().putString(KEY_REPO, repo).apply()
        updatesDir.deleteRecursively()
        _uiState.update { AppUpdateUiState(repo = repo) }
        return true
    }

    private fun fail(failure: Throwable) {
        val kind = (failure as? UpdateException)?.kind ?: UpdateError.Http
        _uiState.update { it.copy(phase = UpdatePhase.Error, error = kind) }
    }

    private companion object {
        const val PREFS = "hermes_app_update"
        const val KEY_REPO = "repo"
    }
}

private fun ShellStrings.updateError(kind: UpdateError?): String = when (kind) {
    UpdateError.InvalidSource -> updateErrInvalidSource
    UpdateError.RateLimited -> updateErrRateLimited
    UpdateError.NoConnection -> updateErrNoConnection
    UpdateError.Unreadable -> updateErrUnreadable
    UpdateError.NoChecksum -> updateErrNoChecksum
    UpdateError.ChecksumMismatch -> updateErrMismatch
    UpdateError.NotGitHub -> updateErrNotGitHub
    UpdateError.Interrupted -> updateErrInterrupted
    UpdateError.Insecure -> updateErrInsecure
    UpdateError.Http, null -> updateErrHttp
}

private fun formatMegabytes(bytes: Long): String =
    if (bytes <= 0) "" else "%.1f MB".format(java.util.Locale.US, bytes / 1_048_576.0)

/**
 * The updates area at the bottom of Settings: plain text on the page, no card. Hidden in the Play
 * edition, which updates through Play. One line says where things stand; small text buttons act on it.
 */
@Composable
internal fun AppUpdateCard(modifier: Modifier = Modifier, viewModel: AppUpdateViewModel = viewModel()) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val offline = remember(state.phase) { AppSettingsStore(context).load().offlineAirplaneMode }
    var editingSource by rememberSaveable { mutableStateOf(false) }
    var notesOpen by rememberSaveable { mutableStateOf(false) }
    val busy = state.phase == UpdatePhase.Checking || state.phase == UpdatePhase.Downloading
    val release = state.release
    val attention = state.phase == UpdatePhase.Available || state.phase == UpdatePhase.Ready

    val status = when (state.phase) {
        UpdatePhase.Idle -> if (offline) shell.updateOffline else shell.updateVersion(viewModel.currentVersion)
        UpdatePhase.Checking -> shell.updateChecking
        UpdatePhase.UpToDate -> shell.updateUpToDateLine(viewModel.currentVersion)
        UpdatePhase.Available -> listOf(shell.updateAvailable(release?.version.orEmpty()), formatMegabytes(release?.apkSizeBytes ?: 0))
            .filter { it.isNotBlank() }.joinToString(" · ")
        UpdatePhase.Downloading -> shell.updateDownloading((state.progress * 100).toInt())
        UpdatePhase.Ready -> shell.updateReady
        UpdatePhase.NoRelease -> shell.updateNoRelease
        UpdatePhase.Error -> shell.updateError(state.error)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp).testTag("HermesAppUpdateCard"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = status,
            style = if (attention) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            color = when {
                state.phase == UpdatePhase.Error -> MaterialTheme.colorScheme.error
                attention -> MaterialTheme.colorScheme.primary
                else -> glass.mutedText
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp).testTag("HermesUpdateStatus"),
        )
        if (state.phase == UpdatePhase.Downloading) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.width(160.dp).padding(top = 6.dp).testTag("HermesUpdateProgress"),
            )
        }
        if (release != null && attention) {
            Text(
                text = release.notes.ifBlank { release.tag },
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
                textAlign = TextAlign.Center,
                maxLines = if (notesOpen) 8 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 2.dp)
                    .clickable { notesOpen = !notesOpen }
                    .testTag("HermesUpdateNotes"),
            )
        }
        state.installNote?.takeIf { it != InstallStart.Started }?.let { note ->
            Text(
                text = if (note == InstallStart.NeedsPermission) shell.updateNeedsPermission else shell.updateInstallUnavailable,
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 2.dp).testTag("HermesUpdateInstallNote"),
            )
        }
        val compact = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            when (state.phase) {
                UpdatePhase.Available -> TextButton(
                    onClick = viewModel::download,
                    enabled = !offline,
                    contentPadding = compact,
                    modifier = Modifier.testTag("HermesUpdateDownload"),
                ) { Text(shell.updateGet, style = MaterialTheme.typography.labelLarge) }
                UpdatePhase.Ready -> TextButton(
                    onClick = { viewModel.install(context) },
                    contentPadding = compact,
                    modifier = Modifier.testTag("HermesUpdateInstall"),
                ) { Text(shell.updateInstall, style = MaterialTheme.typography.labelLarge) }
                UpdatePhase.Checking, UpdatePhase.Downloading -> Unit
                else -> TextButton(
                    onClick = viewModel::check,
                    enabled = !offline,
                    contentPadding = compact,
                    modifier = Modifier.testTag("HermesUpdateCheck"),
                ) {
                    Text(
                        text = if (state.phase == UpdatePhase.Error) shell.updateRetry else shell.updateCheckShort,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            val page = release?.pageUrl?.takeIf { it.startsWith("https://github.com/") }
            if (page != null && attention) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                    contentPadding = compact,
                    modifier = Modifier.testTag("HermesUpdateOpenPage"),
                ) { Text(shell.updateOpenPage, style = MaterialTheme.typography.labelLarge) }
            }
            if (!busy && !attention) {
                TextButton(
                    onClick = { editingSource = true },
                    contentPadding = compact,
                    modifier = Modifier.testTag("HermesUpdateSource"),
                ) { Text(shell.updateSource, style = MaterialTheme.typography.labelLarge, color = glass.mutedText) }
            }
        }
    }

    if (editingSource) {
        var draft by rememberSaveable { mutableStateOf(state.repo) }
        val valid = AppUpdateParser.isValidRepo(draft)
        AlertDialog(
            onDismissRequest = { editingSource = false },
            title = { Text(shell.updateSource) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(shell.updateSourceHint) },
                    isError = draft.isNotBlank() && !valid,
                    supportingText = { if (draft.isNotBlank() && !valid) Text(shell.updateErrInvalidSource) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("HermesUpdateSourceField"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (viewModel.setRepo(draft)) editingSource = false
                    },
                    enabled = valid,
                    modifier = Modifier.testTag("HermesUpdateSourceSave"),
                ) { Text(shell.save) }
            },
            dismissButton = { TextButton(onClick = { editingSource = false }) { Text(shell.cancel) } },
        )
    }
}
