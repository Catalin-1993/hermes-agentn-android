package com.mobilefork.hermesagent.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.IconButton
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.data.HermesSkill
import com.mobilefork.hermesagent.data.SkillsBridge
import com.mobilefork.hermesagent.data.SkillsSnapshot
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SkillsUiState(
    val loading: Boolean = true,
    val error: String = "",
    val note: String = "",
    val toggleSupported: Boolean = false,
    val skills: List<HermesSkill> = emptyList(),
    val statusMessage: String = "",
)

class SkillsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            var snapshot = SkillsSnapshot(ok = false, error = "Waiting for Agent runtime…")
            repeat(12) { attempt ->
                snapshot = withContext(Dispatchers.IO) {
                    SkillsBridge.listSkills(getApplication())
                }
                if (snapshot.ok) return@repeat
                val waitingOnPython = snapshot.error.contains("Python runtime", ignoreCase = true)
                if (!waitingOnPython || attempt == 11) return@repeat
                _uiState.update {
                    it.copy(
                        loading = true,
                        statusMessage = "Waiting for Agent Python runtime…",
                        error = "",
                    )
                }
                kotlinx.coroutines.delay(1_500)
            }
            applySnapshot(
                snapshot,
                statusMessage = if (snapshot.ok) {
                    "Skills refreshed (${snapshot.skills.size})"
                } else {
                    snapshot.error
                },
            )
        }
    }

    fun setEnabled(name: String, enabled: Boolean) {
        if (!_uiState.value.toggleSupported) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            val snapshot = withContext(Dispatchers.IO) {
                SkillsBridge.setSkillEnabled(getApplication(), name, enabled)
            }
            applySnapshot(
                snapshot,
                statusMessage = if (snapshot.ok) {
                    if (enabled) "Enabled $name" else "Disabled $name"
                } else {
                    snapshot.error
                },
            )
        }
    }

    private fun applySnapshot(snapshot: SkillsSnapshot, statusMessage: String) {
        _uiState.update {
            it.copy(
                loading = false,
                error = if (snapshot.ok) "" else snapshot.error.ifBlank { statusMessage },
                note = snapshot.note,
                toggleSupported = snapshot.toggleSupported,
                skills = if (snapshot.ok || snapshot.skills.isNotEmpty()) snapshot.skills else it.skills,
                statusMessage = statusMessage,
            )
        }
    }
}

@Composable
fun SkillsSection(
    modifier: Modifier = Modifier,
    viewModel: SkillsViewModel = viewModel(),
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(uiState.skills, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            uiState.skills
        } else {
            uiState.skills.filter {
                it.name.contains(needle, ignoreCase = true) || it.description.contains(needle, ignoreCase = true)
            }
        }
    }
    val active = uiState.skills.count { it.enabled }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("HermesSkillsSection"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shell.contextSkills, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (uiState.skills.isEmpty()) shell.skillsDescriptionShort
                    else shell.contextSkillsCount(active, uiState.skills.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.mutedText,
                    modifier = Modifier.testTag("SkillsStatus"),
                )
            }
            ShellRoundButton(
                iconRes = R.drawable.ic_action_refresh,
                contentDescription = shell.refresh,
                onClick = viewModel::refresh,
                testTag = "SkillsRefreshButton",
            )
        }
        if (uiState.skills.size > 6) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("SkillsSearch"),
                singleLine = true,
                placeholder = { Text(shell.skillsSearchHint) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                shape = RoundedCornerShape(50),
            )
        }
        if (uiState.note.isNotBlank()) {
            Text(
                uiState.note,
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
                modifier = Modifier.testTag("SkillsNote"),
            )
        }
        if (uiState.error.isNotBlank()) {
            Text(
                uiState.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("SkillsError"),
            )
        }
        if (visible.isEmpty() && !uiState.loading) {
            Text(
                text = if (uiState.skills.isEmpty()) LocalHermesStrings.current.skillsEmpty() else shell.skillsNoMatch,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.mutedText,
            )
        }
        if (visible.isNotEmpty()) {
            ShellGroupCard {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .testTag("SkillsList"),
                ) {
                    itemsIndexed(visible, key = { _, skill -> skill.name }) { index, skill ->
                        SkillRow(
                            skill = skill,
                            toggleSupported = uiState.toggleSupported,
                            enabledControls = !uiState.loading && uiState.toggleSupported,
                            onEnabledChange = { viewModel.setEnabled(skill.name, it) },
                        )
                        if (index != visible.lastIndex) ShellRowDivider(startInset = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillRow(
    skill: HermesSkill,
    toggleSupported: Boolean,
    enabledControls: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("SkillRow-${skill.name}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                skill.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (skill.description.isNotBlank()) {
                Text(
                    skill.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = LocalHermesGlassTokens.current.mutedText,
                )
            }
        }
        if (toggleSupported) {
            Switch(
                modifier = Modifier.testTag("SkillSwitch-${skill.name}"),
                checked = skill.enabled,
                onCheckedChange = onEnabledChange,
                enabled = enabledControls,
            )
        }
    }
}
