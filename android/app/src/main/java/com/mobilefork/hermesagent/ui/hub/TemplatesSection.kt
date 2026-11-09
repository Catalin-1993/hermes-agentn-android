package com.mobilefork.hermesagent.ui.hub

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.HubStrings
import com.mobilefork.hermesagent.ui.i18n.LocalHubStrings

private fun LibraryTemplate.iconRes(): Int = when (this) {
    LibraryTemplate.Notes -> R.drawable.ic_tab_library
    LibraryTemplate.Ideas -> R.drawable.ic_tab_ideas
    LibraryTemplate.Goals -> R.drawable.ic_tab_goals
}

private fun LibraryTemplate.label(words: HubStrings): String = when (this) {
    LibraryTemplate.Notes -> words.templatesNotesName
    LibraryTemplate.Ideas -> words.templatesIdeasName
    LibraryTemplate.Goals -> words.templatesGoalsName
}

/** The templates that are pages of their own; Notes is this library itself. */
private val PAGE_TEMPLATES = listOf(LibraryTemplate.Ideas, LibraryTemplate.Goals)

/**
 * The last section of the library, after Favorite and Private. Its title opens a list downward with
 * every template that is in the library, each a page like any other; "+" shows the ones that can still
 * be added. A template that is off stays listed but dimmed, and cannot be opened.
 */
@Composable
internal fun LibraryTemplatesSection(
    templates: Map<LibraryTemplate, TemplateState>,
    onOpen: (LibraryTemplate) -> Unit,
    onSetActive: (LibraryTemplate, Boolean) -> Unit,
    onSetAdded: (LibraryTemplate, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = LocalHubStrings.current
    var open by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val inLibrary = PAGE_TEMPLATES.filter { templates[it]?.added != false }
    val missing = PAGE_TEMPLATES.filter { it !in inLibrary }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(220))
            .testTag("HermesTemplatesSection"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button) { open = !open }
                .semantics { contentDescription = words.templatesSection }
                .padding(vertical = 4.dp)
                .testTag("HermesTemplatesHeader"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_chevron_right),
                    contentDescription = null,
                    tint = muted,
                    modifier = Modifier.size(16.dp).rotate(if (open) 90f else 0f),
                )
                Text(
                    text = words.templatesSection,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = muted,
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) {
                        open = true
                        adding = !adding
                    }
                    .semantics { contentDescription = words.templatesAdd }
                    .testTag("HermesTemplatesAdd"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_ui_plus), contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
            }
        }
        if (open) {
            if (inLibrary.isEmpty() && !adding) {
                Text(
                    text = words.templatesEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 8.dp),
                )
            }
            inLibrary.forEach { template ->
                val active = templates[template]?.active != false
                TemplateRow(
                    template = template,
                    active = active,
                    onOpen = { onOpen(template) },
                    onSetActive = { onSetActive(template, it) },
                    onRemove = { onSetAdded(template, false) },
                )
            }
            if (adding) {
                Text(
                    text = words.templatesAdd,
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                    modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
                )
                if (missing.isEmpty()) {
                    Text(words.templatesAllAdded, style = MaterialTheme.typography.bodySmall, color = muted.copy(alpha = 0.6f), modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
                }
                missing.forEach { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) {
                                onSetAdded(template, true)
                                onSetActive(template, true)
                                adding = false
                            }
                            .padding(start = 6.dp, end = 6.dp)
                            .testTag("HermesTemplateAdd-${template.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(template.iconRes()), contentDescription = null, tint = muted, modifier = Modifier.size(20.dp))
                        Text(template.label(words), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Icon(painterResource(R.drawable.ic_ui_plus), contentDescription = words.add, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: LibraryTemplate,
    active: Boolean,
    onOpen: () -> Unit,
    onSetActive: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val words = LocalHubStrings.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var menu by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = active, role = Role.Button, onClick = onOpen)
            .padding(start = 6.dp, end = 2.dp)
            .alpha(if (active) 1f else 0.5f)
            .testTag("HermesTemplate-${template.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(template.iconRes()), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Text(
            text = template.label(words),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { menu = true }
                    .semantics { contentDescription = words.templatesRemove }
                    .testTag("HermesTemplateMenu-${template.key}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_ui_more_horiz), contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (active) words.templatesDeactivate else words.templatesActivate) },
                    modifier = Modifier.testTag("HermesTemplateToggle-${template.key}"),
                    onClick = {
                        menu = false
                        onSetActive(!active)
                    },
                )
                DropdownMenuItem(
                    text = { Text(words.templatesRemove) },
                    modifier = Modifier.testTag("HermesTemplateRemove-${template.key}"),
                    onClick = {
                        menu = false
                        onRemove()
                    },
                )
            }
        }
    }
}
