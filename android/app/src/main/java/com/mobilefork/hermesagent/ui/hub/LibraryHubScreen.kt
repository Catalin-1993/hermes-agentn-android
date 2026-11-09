package com.mobilefork.hermesagent.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.HubStrings
import com.mobilefork.hermesagent.ui.i18n.LocalHubStrings
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellPageTitle
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface

private fun LibraryTemplate.iconRes(): Int = when (this) {
    LibraryTemplate.Notes -> R.drawable.ic_tab_library
    LibraryTemplate.Ideas -> R.drawable.ic_tab_ideas
    LibraryTemplate.Goals -> R.drawable.ic_tab_goals
}

private fun LibraryTemplate.name(words: HubStrings): String = when (this) {
    LibraryTemplate.Notes -> words.templatesNotesName
    LibraryTemplate.Ideas -> words.templatesIdeasName
    LibraryTemplate.Goals -> words.templatesGoalsName
}

private fun LibraryTemplate.body(words: HubStrings, notesCount: Int): String = when (this) {
    LibraryTemplate.Notes -> words.templatesNotesCount(notesCount) + " · " + words.templatesNotesBody
    LibraryTemplate.Ideas -> words.templatesIdeasBody
    LibraryTemplate.Goals -> words.templatesGoalsBody
}

/**
 * The library's front page: a list of templates. Notes & documents is the library itself; Ideas and
 * Goals are pages the person adds here and switches on. A page that is off cannot be opened and is
 * left out of the bottom bar and the shortcuts.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryHubScreen(
    templates: Map<LibraryTemplate, TemplateState>,
    notesCount: Int,
    bottomClearance: Dp,
    onOpen: (LibraryTemplate) -> Unit,
    onSetActive: (LibraryTemplate, Boolean) -> Unit,
    onSetAdded: (LibraryTemplate, Boolean) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    var adding by rememberSaveable { mutableStateOf(false) }
    val inLibrary = LibraryTemplate.entries.filter { templates[it]?.added != false }
    val missing = LibraryTemplate.entries.filter { it !in inLibrary }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShellPageMargin)
            .padding(top = 8.dp, bottom = bottomClearance)
            .testTag("HermesLibraryHub"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShellRoundButton(iconRes = R.drawable.ic_ui_menu, contentDescription = "Menu", onClick = onOpenMenu, testTag = "HermesLibraryMenu")
            ShellPageTitle(title = words.templatesTitle, modifier = Modifier.weight(1f)) {
                ShellRoundButton(
                    iconRes = R.drawable.ic_ui_plus,
                    contentDescription = words.templatesAdd,
                    onClick = { adding = true },
                    testTag = "HermesLibraryAddTemplate",
                )
            }
        }
        Text(words.templatesSection, style = MaterialTheme.typography.titleSmall, color = glass.mutedText)
        inLibrary.forEach { template ->
            val state = templates[template] ?: TemplateState()
            TemplateCard(
                template = template,
                active = state.active,
                notesCount = notesCount,
                onOpen = { onOpen(template) },
                onSetActive = { onSetActive(template, it) },
                onRemove = if (template == LibraryTemplate.Notes) null else ({ onSetAdded(template, false) }),
            )
        }
    }
    if (adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
                    .testTag("HermesTemplateSheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(words.templatesAdd, style = MaterialTheme.typography.titleLarge)
                if (missing.isEmpty()) {
                    Text(words.templatesAllAdded, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
                }
                missing.forEach { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button) {
                                onSetAdded(template, true)
                                onSetActive(template, true)
                                adding = false
                            }
                            .padding(vertical = 8.dp)
                            .testTag("HermesTemplateAdd-${template.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TemplateIcon(template)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.name(words), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(template.body(words, notesCount), style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                        }
                        Icon(painterResource(R.drawable.ic_ui_plus), contentDescription = words.add, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateIcon(template: LibraryTemplate) {
    val glass = LocalHermesGlassTokens.current
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(glass.accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(template.iconRes()), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun TemplateCard(
    template: LibraryTemplate,
    active: Boolean,
    notesCount: Int,
    onOpen: () -> Unit,
    onSetActive: (Boolean) -> Unit,
    onRemove: (() -> Unit)?,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hermesGlassSurface(shape, shadowRadius = 6.dp)
            .clip(shape)
            .clickable(enabled = active, role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = template.name(words) }
            .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp)
            .testTag("HermesTemplate-${template.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.alpha(if (active) 1f else 0.5f)) { TemplateIcon(template) }
        Column(modifier = Modifier.weight(1f).alpha(if (active) 1f else 0.6f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(template.name(words), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (active) template.body(words, notesCount) else words.templatesOffHint,
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
            )
        }
        if (template != LibraryTemplate.Notes) {
            Switch(
                checked = active,
                onCheckedChange = onSetActive,
                modifier = Modifier.testTag("HermesTemplateSwitch-${template.key}"),
            )
        }
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = words.templatesRemove }
                    .testTag("HermesTemplateRemove-${template.key}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_action_close), contentDescription = null, tint = glass.mutedText, modifier = Modifier.size(18.dp))
            }
        }
    }
}
