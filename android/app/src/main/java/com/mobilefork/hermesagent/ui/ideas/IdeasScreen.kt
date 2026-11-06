package com.mobilefork.hermesagent.ui.ideas

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.CustomIdea
import com.mobilefork.hermesagent.data.CustomIdeaStore
import com.mobilefork.hermesagent.ui.shell.ShellEntryField
import com.mobilefork.hermesagent.ui.shell.ShellEntrySheet
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefork.hermesagent.BuildConfig
import com.mobilefork.hermesagent.ui.chat.SIGNAL_INTELLIGENCE_QUICK_ACTIONS
import com.mobilefork.hermesagent.ui.i18n.IdeaItem
import com.mobilefork.hermesagent.ui.i18n.IdeaSection
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellPageTitle
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

@Composable
internal fun IdeasScreen(
    bottomClearance: Dp,
    onSendPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shell = LocalShellStrings.current
    val strings = LocalHermesStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    val ideaStore = remember(context) { CustomIdeaStore(context) }
    var ownIdeas by remember(ideaStore) { mutableStateOf(ideaStore.load()) }
    var entryOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftTitle by rememberSaveable { mutableStateOf("") }
    var draftPrompt by rememberSaveable { mutableStateOf("") }
    fun openEntry(idea: CustomIdea?) {
        editingId = idea?.id
        draftTitle = idea?.title.orEmpty()
        draftPrompt = idea?.prompt.orEmpty()
        entryOpen = true
    }
    val sections = if (BuildConfig.HERMES_PLAY_EDITION) {
        shell.ideaSections
    } else {
        shell.ideaSections + IdeaSection(
            title = shell.diagnosticsIdeas,
            items = SIGNAL_INTELLIGENCE_QUICK_ACTIONS.map { action ->
                IdeaItem(
                    id = action.id,
                    emoji = "📡",
                    title = strings.signalQuickActionLabel(action.id, action.label),
                    body = shell.diagnosticsIdeaBody,
                    prompt = action.prompt,
                )
            },
        )
    }
    LazyColumn(
        modifier = modifier
            .padding(horizontal = ShellPageMargin)
            .testTag("HermesIdeasScreen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomClearance),
    ) {
        item(key = "ideas-title") {
            ShellPageTitle(title = shell.ideasTitle) {
                ShellRoundButton(
                    iconRes = R.drawable.ic_ui_plus,
                    contentDescription = shell.extra.ideaNew,
                    onClick = { openEntry(null) },
                    testTag = "HermesIdeasNewButton",
                )
            }
        }
        if (ownIdeas.isNotEmpty()) {
            item(key = "ideas-mine-title") {
                Text(
                    text = shell.extra.ideasMine,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            items(ownIdeas, key = { "own-${it.id}" }) { idea ->
                IdeaRow(
                    idea = IdeaItem(id = idea.id, emoji = "💡", title = idea.title, body = idea.prompt, prompt = idea.prompt),
                    onClick = { onSendPrompt(idea.prompt) },
                    trailing = {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            Icon(
                                painter = painterResource(R.drawable.ic_ui_more_horiz),
                                contentDescription = shell.more,
                                tint = glass.mutedText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { menuOpen = true }
                                    .padding(8.dp)
                                    .size(20.dp)
                                    .testTag("HermesIdeaMenu_${idea.id}"),
                            )
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(shell.extra.ideaEdit) },
                                    modifier = Modifier.testTag("HermesIdeaEdit_${idea.id}"),
                                    onClick = {
                                        menuOpen = false
                                        openEntry(idea)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(shell.delete) },
                                    modifier = Modifier.testTag("HermesIdeaDelete_${idea.id}"),
                                    onClick = {
                                        menuOpen = false
                                        ownIdeas = ideaStore.delete(idea.id)
                                    },
                                )
                            }
                        }
                    },
                )
                if (idea != ownIdeas.last()) ShellRowDivider(startInset = 72.dp)
            }
            item(key = "ideas-mine-end") {
                ShellRowDivider()
                Spacer(Modifier.height(16.dp))
            }
        }
        sections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) {
                item(key = "ideas-section-${section.title}") {
                    ShellRowDivider()
                    Text(
                        text = section.title,
                        modifier = Modifier
                            .padding(top = 24.dp, bottom = 4.dp)
                            .semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            items(section.items, key = { it.id }) { idea ->
                IdeaRow(idea = idea, onClick = { onSendPrompt(idea.prompt) })
                if (idea != section.items.last()) {
                    ShellRowDivider(startInset = 72.dp)
                }
            }
        }
    }
    if (entryOpen) {
        ShellEntrySheet(
            title = if (editingId != null) shell.extra.ideaEdit else shell.extra.ideaNew,
            onDismiss = { entryOpen = false },
            testTag = "HermesIdeaSheet",
        ) {
            ShellGroupCard {
                ShellEntryField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    placeholder = shell.extra.ideaNamePrompt,
                    testTag = "HermesIdeaTitleInput",
                    primary = true,
                    autoFocus = true,
                )
                ShellRowDivider(startInset = 16.dp)
                ShellEntryField(
                    value = draftPrompt,
                    onValueChange = { draftPrompt = it },
                    placeholder = shell.extra.ideaPromptPrompt,
                    testTag = "HermesIdeaPromptInput",
                )
            }
            Button(
                onClick = {
                    ownIdeas = ideaStore.save(editingId, draftTitle, draftPrompt)
                    entryOpen = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("HermesIdeaSave"),
                enabled = draftTitle.isNotBlank(),
            ) {
                Text(shell.extra.ideaSave)
            }
        }
    }
}

@Composable
private fun IdeaRow(
    idea: IdeaItem,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag("HermesIdea_${idea.id}"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Text(text = idea.emoji, fontSize = 40.sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = idea.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = idea.body,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalHermesGlassTokens.current.mutedText,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}
