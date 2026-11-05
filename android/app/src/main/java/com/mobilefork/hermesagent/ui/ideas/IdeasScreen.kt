package com.mobilefork.hermesagent.ui.ideas

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
        item(key = "ideas-title") { ShellPageTitle(title = shell.ideasTitle) }
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
}

@Composable
private fun IdeaRow(
    idea: IdeaItem,
    onClick: () -> Unit,
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
    }
}
