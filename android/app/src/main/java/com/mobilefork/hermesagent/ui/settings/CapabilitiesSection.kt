package com.mobilefork.hermesagent.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.shell.AppSection
import com.mobilefork.hermesagent.ui.shell.ShellChevron
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.shell.ShellRowIcon
import com.mobilefork.hermesagent.data.CapabilitiesBridge
import com.mobilefork.hermesagent.data.CapabilitiesSnapshot
import com.mobilefork.hermesagent.data.SkillState
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.shell.ShellSegmentedTabs
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CapabilitiesUiState(
    val loading: Boolean = true,
    val waitingForAgent: Boolean = false,
    val snapshot: CapabilitiesSnapshot = CapabilitiesSnapshot(ok = false),
    /** The item being changed, so only its row shows progress. */
    val busyKey: String = "",
    val error: String = "",
)

class CapabilitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CapabilitiesUiState())
    val uiState: StateFlow<CapabilitiesUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            // The agent's runtime starts with the app; wait for it rather than showing an error.
            repeat(20) { attempt ->
                val snapshot = withContext(Dispatchers.IO) { CapabilitiesBridge.list(getApplication()) }
                if (snapshot.error != CapabilitiesBridge.PYTHON_NOT_READY || attempt == 19) {
                    apply(snapshot, busyKey = "")
                    return@launch
                }
                _uiState.update { it.copy(waitingForAgent = true) }
                delay(1_500)
            }
        }
    }

    fun setSkill(name: String, enabled: Boolean) = change(name) { CapabilitiesBridge.setSkillEnabled(it, name, enabled) }
    fun installSkill(identifier: String) = change(identifier) { CapabilitiesBridge.installSkill(it, identifier) }
    fun setPlugin(key: String, enabled: Boolean) = change(key) { CapabilitiesBridge.setPluginEnabled(it, key, enabled) }
    fun setToolset(key: String, enabled: Boolean) = change(key) { CapabilitiesBridge.setToolsetEnabled(it, key, enabled) }

    private fun change(key: String, action: (Application) -> CapabilitiesSnapshot) {
        if (_uiState.value.busyKey.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyKey = key, error = "") }
            val snapshot = withContext(Dispatchers.IO) { action(getApplication()) }
            apply(snapshot, busyKey = "")
        }
    }

    private fun apply(snapshot: CapabilitiesSnapshot, busyKey: String) {
        _uiState.update { state ->
            if (snapshot.ok) {
                CapabilitiesUiState(loading = false, snapshot = snapshot, busyKey = busyKey)
            } else {
                // A failed change keeps the last good lists and says what went wrong.
                state.copy(loading = false, waitingForAgent = false, busyKey = busyKey, error = snapshot.error)
            }
        }
    }
}

/**
 * Everything the Hermes agent can use, from the original project: its skills (installed, and the
 * official ones ready to install), its plugins and its tools. Each is one tap away.
 */
@Composable
fun CapabilitiesSection(
    modifier: Modifier = Modifier,
    onOpenSection: (AppSection) -> Unit = {},
    viewModel: CapabilitiesViewModel = viewModel(),
) {
    val shell = LocalShellStrings.current
    val extra = shell.extra
    val glass = LocalHermesGlassTokens.current
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val snapshot = state.snapshot

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("HermesCapabilities"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(extra.capBody, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
        ShellSegmentedTabs(
            options = listOf(extra.capSkills, extra.capPlugins, extra.capApps, extra.capTools),
            selectedIndex = tab,
            onSelect = { tab = it },
            testTagPrefix = "HermesCapabilitiesTab",
        )
        if (state.error.isNotBlank()) {
            Text(state.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (state.loading && !snapshot.ok) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    if (state.waitingForAgent) extra.capWaiting else extra.capLoading,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.mutedText,
                )
            }
            return@Column
        }
        when (tab) {
            0 -> SkillsTab(state, query, onQuery = { query = it }, viewModel = viewModel)
            1 -> SwitchList(
                rows = snapshot.plugins.map { CapabilityRow(it.key, it.name, it.description, it.enabled) },
                busyKey = state.busyKey,
                partError = snapshot.partErrors["plugins"],
                empty = extra.capNoPlugins,
                testPrefix = "HermesPlugin",
                onToggle = viewModel::setPlugin,
            )
            2 -> AppsTab(onOpenSection)
            else -> SwitchList(
                rows = snapshot.toolsets.map { CapabilityRow(it.key, it.label.trimStart().dropEmojiPrefix(), it.description, it.enabled) },
                busyKey = state.busyKey,
                partError = snapshot.partErrors["toolsets"],
                empty = extra.capNoTools,
                testPrefix = "HermesToolset",
                onToggle = viewModel::setToolset,
            )
        }
        Text(extra.capNextConversation, style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
    }
}

/** Hermes labels its tools for a terminal, with an emoji first; the app's rows have their own look. */
private fun String.dropEmojiPrefix(): String {
    val firstLetter = indexOfFirst { it.isLetterOrDigit() }
    return if (firstLetter <= 0) this else substring(firstLetter)
}

private data class CapabilityRow(val key: String, val title: String, val body: String, val enabled: Boolean)

@Composable
private fun SkillsTab(
    state: CapabilitiesUiState,
    query: String,
    onQuery: (String) -> Unit,
    viewModel: CapabilitiesViewModel,
) {
    val extra = LocalShellStrings.current.extra
    val glass = LocalHermesGlassTokens.current
    val needle = query.trim()
    val skills = remember(state.snapshot.skills, needle) {
        state.snapshot.skills.filter {
            needle.isEmpty() || it.name.contains(needle, ignoreCase = true) || it.description.contains(needle, ignoreCase = true)
        }
    }
    val installed = skills.filter { it.state != SkillState.Available }
    val available = skills.filter { it.state == SkillState.Available }
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("HermesCapabilitiesSearch"),
        singleLine = true,
        placeholder = { Text(extra.capSearch) },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_ui_search), contentDescription = null, modifier = Modifier.size(20.dp))
        },
        shape = RoundedCornerShape(50),
    )
    state.snapshot.partErrors["skills"]?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (installed.isNotEmpty()) {
        Text(extra.capInstalled(installed.count { it.state == SkillState.Active }), style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
        SwitchList(
            rows = installed.map { CapabilityRow(it.name, it.name, it.description, it.state == SkillState.Active) },
            busyKey = state.busyKey,
            partError = null,
            empty = "",
            testPrefix = "HermesSkill",
            onToggle = viewModel::setSkill,
        )
    }
    if (available.isNotEmpty()) {
        Text(extra.capAvailable(available.size), style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
        ShellGroupCard {
            available.forEachIndexed { index, skill ->
                if (index > 0) ShellRowDivider(startInset = 16.dp)
                CapabilityRowLayout(skill.name, skill.description, "HermesSkillAvailable-${skill.name}") {
                    if (state.busyKey == skill.identifier) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            extra.capInstall,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .clickable(enabled = state.busyKey.isEmpty()) { viewModel.installSkill(skill.identifier) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("HermesSkillInstall-${skill.name}"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchList(
    rows: List<CapabilityRow>,
    busyKey: String,
    partError: String?,
    empty: String,
    testPrefix: String,
    onToggle: (String, Boolean) -> Unit,
) {
    if (partError != null) {
        Text(partError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    if (rows.isEmpty()) {
        if (empty.isNotBlank()) {
            Text(empty, style = MaterialTheme.typography.bodyMedium, color = LocalHermesGlassTokens.current.mutedText)
        }
        return
    }
    ShellGroupCard {
        rows.forEachIndexed { index, row ->
            if (index > 0) ShellRowDivider(startInset = 16.dp)
            CapabilityRowLayout(row.title, row.body, "$testPrefix-${row.key}") {
                if (busyKey == row.key) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Switch(
                        checked = row.enabled,
                        onCheckedChange = { onToggle(row.key, it) },
                        enabled = busyKey.isEmpty(),
                        modifier = Modifier.testTag("$testPrefix-switch-${row.key}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityRowLayout(title: String, body: String, testTag: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (body.isNotBlank()) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalHermesGlassTokens.current.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

/** Where the agent reaches other services: connected accounts and the Nous Portal. */
@Composable
private fun AppsTab(onOpenSection: (AppSection) -> Unit) {
    val shell = LocalShellStrings.current
    ShellGroupCard {
        ShellListRow(
            title = shell.settingsConnectors,
            modifier = Modifier.testTag("HermesSettingsConnectors"),
            onClick = { onOpenSection(AppSection.Accounts) },
            leading = { ShellRowIcon(R.drawable.ic_set_plug) },
            trailing = { ShellChevron() },
        )
        ShellRowDivider(startInset = 54.dp)
        ShellListRow(
            title = shell.portalTitle,
            modifier = Modifier.testTag("HermesSettingsPortal"),
            onClick = { onOpenSection(AppSection.NousPortal) },
            leading = { ShellRowIcon(R.drawable.ic_app_window) },
            trailing = { ShellChevron() },
        )
    }
}
