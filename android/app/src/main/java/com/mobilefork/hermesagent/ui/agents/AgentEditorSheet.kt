package com.mobilefork.hermesagent.ui.agents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.ProviderPresets
import com.mobilefork.hermesagent.data.SecureSecretsStore
import com.mobilefork.hermesagent.ui.chat.usesDirectOpenAiCompatibleTransport
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellChevron
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.OutlinedButton
import com.mobilefork.hermesagent.ui.theme.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** The fields the editor edits, separate from saving so the form can be shown on its own. */
internal data class AgentDraft(
    val id: String,
    val name: String = "",
    val title: String = "",
    val description: String = "",
    val persona: String = "",
    val provider: String = "",
    val model: String = "",
    val toolsEnabled: Boolean = false,
    val avatarPath: String = "",
    val createdAtEpochMs: Long = 0L,
) {
    fun toProfile() = AgentProfile(
        id = id,
        name = name,
        title = title,
        description = description,
        persona = persona,
        provider = provider,
        model = model,
        toolsEnabled = toolsEnabled,
        avatarPath = avatarPath,
        createdAtEpochMs = createdAtEpochMs,
    )
}

private fun AgentProfile.toDraft() = AgentDraft(
    id, name, title, description, persona, provider, model, toolsEnabled, avatarPath, createdAtEpochMs,
)

/** [agentId] blank creates a new agent; the main agent edits only its instructions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgentEditorSheet(
    agentId: String,
    viewModel: AgentsViewModel,
    onDismiss: () -> Unit,
    onSaved: (AgentProfile, Boolean) -> Unit,
) {
    val isNew = agentId.isBlank()
    val isMain = agentId == AgentProfile.MAIN_ID
    val original = remember(agentId) { viewModel.agents.value.firstOrNull { it.id == agentId } }
    val newId = rememberSaveable(agentId) { UUID.randomUUID().toString() }
    var draft by remember(agentId) {
        mutableStateOf(
            when {
                isMain -> AgentProfile.Main.toDraft().copy(persona = viewModel.mainPersona())
                original != null -> original.toDraft()
                else -> AgentDraft(id = newId)
            },
        )
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val shell = LocalShellStrings.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importAvatar(draft.id, uri) { path ->
                if (path != null) {
                    if (draft.avatarPath != original?.avatarPath) viewModel.discardAvatar(draft.avatarPath)
                    draft = draft.copy(avatarPath = path)
                }
            }
        }
    }
    val close = {
        // A picture picked here and not saved must not stay behind in app storage.
        if (draft.avatarPath != original?.avatarPath) viewModel.discardAvatar(draft.avatarPath)
        onDismiss()
    }
    ModalBottomSheet(onDismissRequest = close) {
        AgentEditorForm(
            draft = draft,
            isMain = isMain,
            isNew = isNew,
            nameAvailable = isMain || viewModel.isNameAvailable(draft.name, draft.id),
            onChange = { draft = it },
            onPickPhoto = { picker.launch(arrayOf("image/*")) },
            onDelete = { confirmDelete = true },
            onSave = {
                if (isMain) {
                    viewModel.saveMainPersona(draft.persona)
                    onDismiss()
                } else {
                    viewModel.save(draft.toProfile()).onSuccess { saved ->
                        original?.avatarPath?.takeIf { it != saved.avatarPath }?.let(viewModel::discardAvatar)
                        onSaved(saved, isNew)
                    }
                }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(shell.deleteAgent) },
            text = { Text(shell.deleteAgentConfirm(draft.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete(draft.id)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("AgentDeleteConfirm"),
                ) { Text(shell.delete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(shell.cancel) } },
        )
    }
}

@Composable
internal fun AgentEditorForm(
    draft: AgentDraft,
    isMain: Boolean,
    isNew: Boolean,
    nameAvailable: Boolean,
    onChange: (AgentDraft) -> Unit,
    onPickPhoto: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val shell = LocalShellStrings.current
    var advancedOpen by rememberSaveable { mutableStateOf(draft.provider.isNotBlank() || draft.toolsEnabled) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .testTag("AgentEditor"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AgentAvatar(agent = draft.toProfile(), size = 88.dp)
            if (!isMain) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onPickPhoto, modifier = Modifier.testTag("AgentPickPhoto")) {
                        Text(shell.agentChoosePhoto)
                    }
                    if (draft.avatarPath.isNotBlank()) {
                        TextButton(onClick = { onChange(draft.copy(avatarPath = "")) }) {
                            Text(shell.agentUseGeneratedFace)
                        }
                    }
                }
            }
        }
        if (isMain) {
            Text(shell.agentName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it.take(AgentProfile.MAX_NAME_CHARS))) },
                modifier = Modifier.fillMaxWidth().testTag("AgentNameInput"),
                singleLine = true,
                label = { Text(shell.agentNameField) },
                isError = !nameAvailable && draft.name.isNotBlank(),
                supportingText = {
                    if (!nameAvailable && draft.name.isNotBlank()) Text(shell.agentNameTaken)
                },
            )
            OutlinedTextField(
                value = draft.title,
                onValueChange = { onChange(draft.copy(title = it.take(AgentProfile.MAX_TITLE_CHARS))) },
                modifier = Modifier.fillMaxWidth().testTag("AgentTitleInput"),
                singleLine = true,
                label = { Text(shell.agentRoleField) },
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = { onChange(draft.copy(description = it.take(AgentProfile.MAX_DESCRIPTION_CHARS))) },
                modifier = Modifier.fillMaxWidth().testTag("AgentDescriptionInput"),
                minLines = 2,
                maxLines = 3,
                label = { Text(shell.agentDescriptionField) },
            )
        }
        OutlinedTextField(
            value = draft.persona,
            onValueChange = { onChange(draft.copy(persona = it)) },
            modifier = Modifier.fillMaxWidth().testTag("AgentPersonaInput"),
            minLines = 3,
            maxLines = 8,
            label = { Text(shell.agentInstructionsField) },
        )
        if (!isMain) {
            ShellRowDivider()
            ShellListRow(
                title = shell.agentAdvanced,
                modifier = Modifier.testTag("AgentAdvancedToggle"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                onClick = { advancedOpen = !advancedOpen },
                trailing = { ShellChevron() },
            )
            if (advancedOpen) {
                AgentAdvancedFields(draft = draft, onChange = onChange)
            }
        }
        Button(
            onClick = onSave,
            enabled = isMain || (draft.name.isNotBlank() && nameAvailable),
            modifier = Modifier.fillMaxWidth().testTag("AgentSave"),
        ) {
            Text(if (isNew) shell.agentCreate else shell.save)
        }
        if (!isMain && !isNew) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().testTag("AgentDelete"),
            ) {
                Text(shell.deleteAgent, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AgentAdvancedFields(
    draft: AgentDraft,
    onChange: (AgentDraft) -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    val providers = remember {
        ProviderPresets.defaults.filter { usesDirectOpenAiCompatibleTransport(it.id) }
    }
    val selected = providers.firstOrNull { it.id == draft.provider }
    var menuOpen by remember { mutableStateOf(false) }
    // Reading the encrypted key store is slow and can fail, so it stays off the main thread.
    var keyMissing by remember(draft.provider) { mutableStateOf(false) }
    LaunchedEffect(draft.provider) {
        if (draft.provider.isBlank()) return@LaunchedEffect
        keyMissing = withContext(Dispatchers.IO) {
            runCatching { SecureSecretsStore(context).loadApiKey(draft.provider).isBlank() }.getOrDefault(false)
        }
    }
    Box {
        ShellListRow(
            title = shell.agentProviderField,
            subtitle = selected?.label ?: shell.agentSameAsMain,
            modifier = Modifier.testTag("AgentProviderPicker"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            onClick = { menuOpen = true },
            trailing = { ShellChevron() },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(shell.agentSameAsMain) },
                onClick = {
                    menuOpen = false
                    onChange(draft.copy(provider = "", model = ""))
                },
            )
            providers.forEach { provider ->
                DropdownMenuItem(
                    text = { Text(provider.label) },
                    onClick = {
                        menuOpen = false
                        onChange(draft.copy(provider = provider.id, model = provider.modelHint, toolsEnabled = false))
                    },
                )
            }
        }
    }
    if (keyMissing && selected != null) {
        Text(
            shell.agentKeyMissing(selected.label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (draft.provider.isNotBlank()) {
        OutlinedTextField(
            value = draft.model,
            onValueChange = { onChange(draft.copy(model = it)) },
            modifier = Modifier.fillMaxWidth().testTag("AgentModelInput"),
            singleLine = true,
            label = { Text(shell.agentModelField) },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(shell.agentToolsTitle, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (draft.provider.isNotBlank()) shell.agentToolsPinnedNote else shell.agentToolsBody,
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
            )
        }
        Switch(
            checked = draft.toolsEnabled && draft.provider.isBlank(),
            enabled = draft.provider.isBlank(),
            onCheckedChange = { onChange(draft.copy(toolsEnabled = it)) },
            modifier = Modifier.testTag("AgentToolsSwitch"),
        )
    }
}
