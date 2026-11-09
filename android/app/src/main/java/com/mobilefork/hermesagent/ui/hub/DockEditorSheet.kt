package com.mobilefork.hermesagent.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalHubStrings
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

/**
 * Edits the bottom bar: reorder, remove, or add a page of the app, a project, or a page or folder of
 * the library. Two to five buttons stay in the bar; a page that belongs to a library template can be
 * added only while that template is on.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DockEditorSheet(
    items: List<DockEntry>,
    templates: Map<LibraryTemplate, TemplateState>,
    projects: List<DockEntry>,
    pages: List<DockEntry>,
    onChange: (List<DockEntry>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        DockEditorContent(items, templates, projects, pages, onChange, onReset, onDismiss)
    }
}

/** What the sheet shows; apart from the sheet so it can be drawn and tested on its own. */
@Composable
internal fun DockEditorContent(
    items: List<DockEntry>,
    templates: Map<LibraryTemplate, TemplateState>,
    projects: List<DockEntry>,
    pages: List<DockEntry>,
    onChange: (List<DockEntry>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    val full = items.size >= DockItem.MAX
    val barKeys = items.map { it.key }.toSet()
    val addable = DockItem.entries.map(DockEntry::of).filter { it.key !in barKeys }
    val addableProjects = projects.filter { it.key !in barKeys }
    val addablePages = pages.filter { it.key !in barKeys }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
            .testTag("HermesDockEditor"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(words.dockEditTitle, style = MaterialTheme.typography.titleLarge)
            Text(words.dockEditHint, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
        }
        SheetLabel(words.dockInBar)
        ShellGroupCard(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                if (index > 0) ShellRowDivider(startInset = 64.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("HermesDockRow-${rowKey(item.key)}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EntryIcon(item)
                    Text(
                        text = item.title(words),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SmallAction(
                        icon = R.drawable.ic_ui_arrow_left,
                        description = words.moveUp,
                        enabled = index > 0,
                        tag = "HermesDockLeft-${rowKey(item.key)}",
                        onClick = { onChange(items.toMutableList().also { it.add(index - 1, it.removeAt(index)) }) },
                    )
                    SmallAction(
                        icon = R.drawable.ic_ui_arrow_right,
                        description = words.moveDown,
                        enabled = index < items.lastIndex,
                        tag = "HermesDockRight-${rowKey(item.key)}",
                        onClick = { onChange(items.toMutableList().also { it.add(index + 1, it.removeAt(index)) }) },
                    )
                    SmallAction(
                        icon = R.drawable.ic_ui_minus,
                        description = words.remove,
                        enabled = items.size > DockItem.MIN,
                        tint = MaterialTheme.colorScheme.error,
                        tag = "HermesDockRemove-${rowKey(item.key)}",
                        onClick = { onChange(items - item) },
                    )
                }
            }
        }
        if (addable.isNotEmpty()) {
            SheetLabel(words.dockAvailable)
            if (full) {
                Text(words.dockFull, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            }
            ShellGroupCard(modifier = Modifier.fillMaxWidth()) {
                addable.forEachIndexed { index, item ->
                    val available = item.isAvailable(templates)
                    if (index > 0) ShellRowDivider(startInset = 64.dp)
                    AddRow(
                        entry = item,
                        usable = available && !full,
                        note = if (available) "" else words.dockNeedsActivation,
                        addLabel = words.add,
                        onAdd = { onChange(items + item) },
                    )
                }
            }
        }
        if (addableProjects.isNotEmpty()) {
            SheetLabel(words.dockProjectList)
            PickList(addableProjects, usable = !full, addLabel = words.add, onAdd = { onChange(items + it) })
        }
        if (addablePages.isNotEmpty()) {
            SheetLabel(words.dockLibraryPages)
            PickList(addablePages, usable = !full, addLabel = words.add, onAdd = { onChange(items + it) })
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton(words.dockReset, filled = false, modifier = Modifier.weight(1f).testTag("HermesDockReset"), onClick = onReset)
            PillButton(words.dockDone, filled = true, modifier = Modifier.weight(1f).testTag("HermesDockDone"), onClick = onDismiss)
        }
    }
}

/** A key as part of a test tag: the page names stay as they were, ids lose their colon. */
private fun rowKey(key: String): String = key.replace(':', '-')

@Composable
private fun AddRow(
    entry: DockEntry,
    usable: Boolean,
    note: String,
    addLabel: String,
    onAdd: () -> Unit,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = usable, role = Role.Button, onClick = onAdd)
            .semantics { contentDescription = addLabel + " " + entry.title(words) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .alpha(if (usable) 1f else 0.5f)
            .testTag("HermesDockAdd-${rowKey(entry.key)}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EntryIcon(entry)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title(words),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.isNotBlank()) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            }
        }
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(glass.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_ui_plus), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PickList(entries: List<DockEntry>, usable: Boolean, addLabel: String, onAdd: (DockEntry) -> Unit) {
    ShellGroupCard(modifier = Modifier.fillMaxWidth()) {
        entries.take(40).forEachIndexed { index, entry ->
            if (index > 0) ShellRowDivider(startInset = 64.dp)
            AddRow(entry = entry, usable = usable, note = "", addLabel = addLabel, onAdd = { onAdd(entry) })
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = LocalHermesGlassTokens.current.mutedText)
}

@Composable
internal fun EntryIcon(entry: DockEntry, size: Dp = 40.dp) {
    val glass = LocalHermesGlassTokens.current
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(glass.accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(entry.iconRes()), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size / 2))
    }
}

@Composable
internal fun ItemIcon(item: DockItem, size: Dp = 40.dp) = EntryIcon(DockEntry.of(item), size)

@Composable
private fun SmallAction(
    icon: Int,
    description: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.28f)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun PillButton(text: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val glass = LocalHermesGlassTokens.current
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (filled) MaterialTheme.colorScheme.primary else glass.tabSelected)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
