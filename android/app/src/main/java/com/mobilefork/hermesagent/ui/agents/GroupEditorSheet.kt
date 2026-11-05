package com.mobilefork.hermesagent.ui.agents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.chat.GROUP_CHAT_MAX_MEMBERS
import com.mobilefork.hermesagent.ui.chat.GROUP_CHAT_MIN_MEMBERS
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.OutlinedButton
import com.mobilefork.hermesagent.ui.theme.TextButton
import java.util.UUID

/** [isNew] creates a group; otherwise the sheet edits the group described by the other values. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupEditorSheet(
    isNew: Boolean,
    initialName: String,
    initialMemberIds: List<String>,
    initialAvatarPath: String,
    viewModel: AgentsViewModel,
    onDismiss: () -> Unit,
    onSave: (name: String, memberIds: List<String>, avatarPath: String) -> Unit,
    onDelete: () -> Unit,
) {
    val shell = LocalShellStrings.current
    var name by rememberSaveable { mutableStateOf(initialName) }
    var memberIds by rememberSaveable { mutableStateOf(initialMemberIds) }
    var avatarPath by rememberSaveable { mutableStateOf(initialAvatarPath) }
    var errorText by rememberSaveable { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val avatarKey = rememberSaveable { "group-${UUID.randomUUID()}" }
    val sheetOpen = remember { booleanArrayOf(true) }
    DisposableEffect(Unit) { onDispose { sheetOpen[0] = false } }
    fun discardUnsavedPicture(path: String) {
        if (path != initialAvatarPath) viewModel.discardAvatar(path)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing = true
            errorText = ""
            viewModel.importAvatar(avatarKey, uri) { path ->
                importing = false
                when {
                    path == null -> errorText = shell.agentPhotoFailed
                    !sheetOpen[0] -> viewModel.discardAvatar(path)
                    else -> {
                        discardUnsavedPicture(avatarPath)
                        avatarPath = path
                    }
                }
            }
        }
    }
    val close = {
        discardUnsavedPicture(avatarPath)
        onDismiss()
    }
    val agents by viewModel.agents.collectAsState()
    ModalBottomSheet(onDismissRequest = close) {
        GroupEditorForm(
            isNew = isNew,
            name = name,
            memberIds = memberIds,
            avatarPath = avatarPath,
            agents = agents,
            errorText = errorText,
            saveEnabled = !importing,
            onNameChange = { name = it },
            onMemberIdsChange = { memberIds = it },
            onPickPhoto = { picker.launch(arrayOf("image/*")) },
            onRemovePhoto = {
                discardUnsavedPicture(avatarPath)
                avatarPath = ""
            },
            onSave = {
                if (initialAvatarPath.isNotBlank() && initialAvatarPath != avatarPath) {
                    viewModel.discardAvatar(initialAvatarPath)
                }
                onSave(name.trim(), memberIds, avatarPath)
            },
            onDelete = { confirmDelete = true },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(shell.deleteGroup) },
            text = { Text(shell.deleteGroupConfirm(name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        discardUnsavedPicture(avatarPath)
                        onDelete()
                    },
                    modifier = Modifier.testTag("GroupDeleteConfirm"),
                ) { Text(shell.delete) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(shell.cancel) } },
        )
    }
}

@Composable
internal fun GroupEditorForm(
    isNew: Boolean,
    name: String,
    memberIds: List<String>,
    avatarPath: String,
    agents: List<AgentProfile>,
    errorText: String,
    saveEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onMemberIdsChange: (List<String>) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val selected = agents.filter { it.id in memberIds }
    val countValid = selected.size in GROUP_CHAT_MIN_MEMBERS..GROUP_CHAT_MAX_MEMBERS
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .testTag("GroupEditor"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            GroupAvatar(members = selected, avatarPath = avatarPath, size = 88.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onPickPhoto, modifier = Modifier.testTag("GroupPickPhoto")) {
                    Text(shell.agentChoosePhoto)
                }
                if (avatarPath.isNotBlank()) {
                    TextButton(onClick = onRemovePhoto) { Text(shell.groupUseMemberAvatars) }
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it.take(AgentProfile.MAX_TITLE_CHARS)) },
            modifier = Modifier.fillMaxWidth().testTag("GroupNameInput"),
            singleLine = true,
            label = { Text(shell.groupNameField) },
        )
        Text(shell.groupMembersField, style = MaterialTheme.typography.titleMedium)
        Text(
            shell.groupMembersHint(GROUP_CHAT_MIN_MEMBERS, GROUP_CHAT_MAX_MEMBERS),
            style = MaterialTheme.typography.bodySmall,
            color = glass.mutedText,
        )
        agents.forEach { agent ->
            val isSelected = agent.id in memberIds
            val canAdd = selected.size < GROUP_CHAT_MAX_MEMBERS
            val toggle = {
                onMemberIdsChange(if (isSelected) memberIds - agent.id else memberIds + agent.id)
            }
            ShellListRow(
                title = agent.name,
                subtitle = agent.title,
                modifier = Modifier.testTag("GroupMember-${agent.id}"),
                contentPadding = PaddingValues(vertical = 6.dp),
                onClick = if (isSelected || canAdd) toggle else null,
                leading = { AgentAvatar(agent = agent, size = 40.dp) },
                trailing = {
                    Checkbox(
                        checked = isSelected,
                        enabled = isSelected || canAdd,
                        onCheckedChange = { toggle() },
                    )
                },
            )
        }
        if (errorText.isNotBlank()) {
            Text(errorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSave,
            enabled = saveEnabled && name.isNotBlank() && countValid,
            modifier = Modifier.fillMaxWidth().testTag("GroupSave"),
        ) {
            Text(if (isNew) shell.groupCreate else shell.save)
        }
        if (!isNew) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().testTag("GroupDelete"),
            ) {
                Text(shell.deleteGroup, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}
