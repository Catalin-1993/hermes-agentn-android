package com.mobilefork.hermesagent.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.mobilefork.hermesagent.ui.theme.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mobilefork.hermesagent.ui.theme.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.agents.GroupAvatar
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mobilefork.hermesagent.ui.calls.CallsPage
import com.mobilefork.hermesagent.ui.calls.CallsUi
import com.mobilefork.hermesagent.ui.i18n.LocalCallStrings
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellSegmentedTabs
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellPageTitle
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ConversationHistoryList(
    summaries: List<ChatConversationSummary>,
    onOpenConversation: (String) -> Unit,
    onStartNew: () -> Unit,
    onRename: (String, String) -> Unit,
    onRegenerateTitle: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 0.dp,
    agents: List<AgentProfile> = emptyList(),
    onOpenAgent: (AgentProfile) -> Unit = {},
    onEditAgent: (AgentProfile) -> Unit = {},
    onNewAgent: (() -> Unit)? = null,
    onNewGroup: (() -> Unit)? = null,
    onEditGroup: (ChatConversationSummary) -> Unit = {},
    /** When given, the page is split into Groups, Agents, Recent and Calls under a bar. */
    calls: CallsUi? = null,
) {
    val shell = LocalShellStrings.current
    val callWords = LocalCallStrings.current
    // 0 groups, 1 agents, 2 recent, 3 calls; null keeps every section on one page.
    var tab by rememberSaveable { mutableIntStateOf(2) }
    val section: Int? = if (calls != null) tab else null
    val glass = LocalHermesGlassTokens.current
    var menuId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var titleDraft by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ShellPageMargin)
            .padding(top = 8.dp),
    ) {
        ShellPageTitle(title = shell.historyTitle) {
            var newMenuOpen by remember { mutableStateOf(false) }
            Box {
                ShellRoundButton(
                    iconRes = R.drawable.ic_ui_plus,
                    contentDescription = shell.newChat,
                    onClick = { if (onNewAgent != null) newMenuOpen = true else onStartNew() },
                    testTag = "HermesHistoryNewChat",
                )
                DropdownMenu(expanded = newMenuOpen, onDismissRequest = { newMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(shell.newChat) },
                        modifier = Modifier.testTag("HermesNewChatItem"),
                        onClick = { newMenuOpen = false; onStartNew() },
                    )
                    DropdownMenuItem(
                        text = { Text(shell.newAgent) },
                        modifier = Modifier.testTag("HermesNewAgentItem"),
                        onClick = { newMenuOpen = false; onNewAgent?.invoke() },
                    )
                    if (onNewGroup != null) {
                        DropdownMenuItem(
                            text = { Text(shell.newGroup) },
                            // A group needs two agents, and the main agent is only the first.
                            enabled = agents.size >= GROUP_CHAT_MIN_MEMBERS,
                            modifier = Modifier.testTag("HermesNewGroupItem"),
                            onClick = { newMenuOpen = false; onNewGroup() },
                        )
                    }
                }
            }
        }
        val groups = summaries.filter { it.isGroup }
        val chats = summaries.filterNot { it.isGroup }
        if (calls != null) {
            ShellSegmentedTabs(
                options = listOf(shell.groupsSection, shell.agentsSection, shell.recentSection, callWords.tabCalls),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                testTagPrefix = "HermesConversationsTab",
            )
        }
        if (calls != null && tab == 3) {
            CallsPage(agents = agents, calls = calls, bottomClearance = bottomClearance, modifier = Modifier.fillMaxSize())
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomClearance),
        ) {
            if (section == 0 && groups.isEmpty()) {
                item(key = "groups-empty") { EmptyTabText(callWords.emptyGroups) }
            }
            if ((section == null || section == 0) && groups.isNotEmpty()) {
                if (section == null) item(key = "groups-title") { ConversationsSectionTitle(shell.groupsSection) }
                itemsIndexed(groups, key = { _, group -> "group-${group.id}" }) { index, group ->
                    if (index > 0) ShellRowDivider(startInset = 62.dp)
                    GroupRow(
                        group = group,
                        members = group.memberIds.mapNotNull { id -> agents.firstOrNull { it.id == id } },
                        onOpen = { onOpenConversation(group.id) },
                        onEdit = { onEditGroup(group) },
                    )
                }
            }
            if (section == 1 && agents.isEmpty()) {
                item(key = "agents-empty") { EmptyTabText(callWords.emptyAgents) }
            }
            if ((section == null || section == 1) && agents.isNotEmpty()) {
                if (section == null) item(key = "agents-title") { ConversationsSectionTitle(shell.agentsSection) }
                itemsIndexed(agents, key = { _, agent -> "agent-${agent.id}" }) { index, agent ->
                    if (index > 0) ShellRowDivider(startInset = 62.dp)
                    AgentRow(
                        agent = agent,
                        preview = chats.firstOrNull { it.agentId == agent.id }?.preview.orEmpty(),
                        onOpen = { onOpenAgent(agent) },
                        onEdit = { onEditAgent(agent) },
                    )
                }
                if (section == null) item(key = "recent-title") { ConversationsSectionTitle(shell.recentSection) }
            }
            if ((section == null || section == 2) && chats.isEmpty()) {
                item(key = "conversations-empty") {
                    Text(
                        text = shell.historyEmpty,
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.mutedText,
                    )
                }
            }
            itemsIndexed(if (section == null || section == 2) chats else emptyList(), key = { _, summary -> summary.id }) { index, summary ->
                    if (index > 0) ShellRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("HermesHistoryRow-${summary.id}")
                            .combinedClickable(
                                onClick = { onOpenConversation(summary.id) },
                                onLongClickLabel = shell.more,
                                onLongClick = { menuId = summary.id },
                            )
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (agents.isNotEmpty()) {
                            AgentAvatar(
                                agent = agents.firstOrNull { it.id == summary.agentId } ?: AgentProfile.Main,
                                size = 36.dp,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = if (summary.isDefaultTitle) shell.newChat else summary.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (summary.preview.isNotBlank()) {
                                Text(
                                    text = summary.preview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.mutedText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = "${summary.updatedLabel} · ${shell.messageCount(summary.messageCount)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.mutedText,
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuId = summary.id },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("HermesHistoryActions-${summary.id}"),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_ui_more_vert),
                                    contentDescription = shell.more,
                                    tint = glass.mutedText,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            DropdownMenu(expanded = menuId == summary.id, onDismissRequest = { menuId = null }) {
                                DropdownMenuItem(
                                    text = { Text(shell.rename) },
                                    onClick = { menuId = null; renameId = summary.id; titleDraft = summary.title },
                                )
                                DropdownMenuItem(
                                    text = { Text(shell.regenerateTitle) },
                                    onClick = { menuId = null; onRegenerateTitle(summary.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text(shell.delete) },
                                    onClick = { menuId = null; deleteId = summary.id },
                                )
                            }
                        }
                    }
                }
        }
    }
    if (renameId != null) AlertDialog(
        onDismissRequest = { renameId = null },
        title = { Text(shell.rename) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = titleDraft, onValueChange = { titleDraft = it.take(192) },
                    singleLine = true, label = { Text(shell.titleField) },
                    modifier = Modifier.testTag("HermesHistoryTitleInput"))
                Text(shell.localOnlyNote)
            }
        },
        confirmButton = { TextButton(enabled = titleDraft.isNotBlank(), onClick = {
            renameId?.let { onRename(it, titleDraft) }; renameId = null
        }) { Text(shell.save) } },
        dismissButton = { TextButton(onClick = { renameId = null }) { Text(shell.cancel) } },
    )
    if (deleteId != null) AlertDialog(
        onDismissRequest = { deleteId = null },
        title = { Text(shell.delete) },
        text = { Text(shell.deleteConfirm) },
        confirmButton = { TextButton(onClick = { deleteId?.let(onDelete); deleteId = null }) {
            Text(shell.delete)
        } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text(shell.cancel) } },
    )
}

@Composable
private fun EmptyTabText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalHermesGlassTokens.current.mutedText,
    )
}

@Composable
private fun ConversationsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .padding(top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupRow(
    group: ChatConversationSummary,
    members: List<AgentProfile>,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val glass = LocalHermesGlassTokens.current
    val shell = LocalShellStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("HermesGroupRow-${group.id}")
            .combinedClickable(onClick = onOpen, onLongClickLabel = shell.editGroup, onLongClick = onEdit)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupAvatar(members = members, avatarPath = group.groupAvatarPath, size = 48.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (group.needsUser) {
                Text(
                    text = shell.groupNeedsYou,
                    modifier = Modifier
                        .background(glass.accentContainer, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("HermesGroupNeedsYou-${group.id}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = group.preview.ifBlank { shell.groupMemberCount(members.size) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .size(32.dp)
                .testTag("HermesGroupActions-${group.id}"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_more_vert),
                contentDescription = shell.editGroup,
                tint = glass.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgentRow(
    agent: AgentProfile,
    preview: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val glass = LocalHermesGlassTokens.current
    val shell = LocalShellStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("HermesAgentRow-${agent.id}")
            .combinedClickable(
                onClick = onOpen,
                onLongClickLabel = shell.agentProfile,
                onLongClick = onEdit,
            )
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentAvatar(agent = agent, size = 48.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = agent.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val line = preview.ifBlank { agent.title.ifBlank { agent.description } }
            if (line.isNotBlank()) {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .size(32.dp)
                .testTag("HermesAgentActions-${agent.id}"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_more_vert),
                contentDescription = shell.agentProfile,
                tint = glass.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
