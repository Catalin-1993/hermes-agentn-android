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
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
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
) {
    val shell = LocalShellStrings.current
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
            ShellRoundButton(
                iconRes = R.drawable.ic_ui_plus,
                contentDescription = shell.newChat,
                onClick = onStartNew,
                testTag = "HermesHistoryNewChat",
            )
        }
        if (summaries.isEmpty()) {
            Text(
                text = shell.historyEmpty,
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.mutedText,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomClearance),
            ) {
                itemsIndexed(summaries, key = { _, summary -> summary.id }) { index, summary ->
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
                        verticalAlignment = Alignment.Top,
                    ) {
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
