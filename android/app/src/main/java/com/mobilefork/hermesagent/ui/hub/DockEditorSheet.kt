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
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalHubStrings
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

/**
 * Edits the bottom bar: reorder, remove, or add any page. Two to five buttons stay in the bar;
 * a page that belongs to a library template can be added only while that template is on.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DockEditorSheet(
    items: List<DockItem>,
    templates: Map<LibraryTemplate, TemplateState>,
    onChange: (List<DockItem>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        DockEditorContent(items, templates, onChange, onReset, onDismiss)
    }
}

/** What the sheet shows; apart from the sheet so it can be drawn and tested on its own. */
@Composable
internal fun DockEditorContent(
    items: List<DockItem>,
    templates: Map<LibraryTemplate, TemplateState>,
    onChange: (List<DockItem>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    val full = items.size >= DockItem.MAX
    val addable = DockItem.entries.filter { it !in items }
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
                        .testTag("HermesDockRow-${item.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ItemIcon(item)
                    Text(
                        text = item.title(words),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    SmallAction(
                        icon = R.drawable.ic_ui_arrow_left,
                        description = words.moveUp,
                        enabled = index > 0,
                        tag = "HermesDockLeft-${item.key}",
                        onClick = { onChange(items.toMutableList().also { it.add(index - 1, it.removeAt(index)) }) },
                    )
                    SmallAction(
                        icon = R.drawable.ic_ui_arrow_right,
                        description = words.moveDown,
                        enabled = index < items.lastIndex,
                        tag = "HermesDockRight-${item.key}",
                        onClick = { onChange(items.toMutableList().also { it.add(index + 1, it.removeAt(index)) }) },
                    )
                    SmallAction(
                        icon = R.drawable.ic_ui_minus,
                        description = words.remove,
                        enabled = items.size > DockItem.MIN,
                        tint = MaterialTheme.colorScheme.error,
                        tag = "HermesDockRemove-${item.key}",
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
                    val usable = available && !full
                    if (index > 0) ShellRowDivider(startInset = 64.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = usable, role = Role.Button) { onChange(items + item) }
                            .semantics { contentDescription = words.add + " " + item.title(words) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .alpha(if (usable) 1f else 0.5f)
                            .testTag("HermesDockAdd-${item.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ItemIcon(item)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title(words), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            if (!available) {
                                Text(words.dockNeedsActivation, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
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
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            PillButton(words.dockReset, filled = false, modifier = Modifier.weight(1f).testTag("HermesDockReset"), onClick = onReset)
            PillButton(words.dockDone, filled = true, modifier = Modifier.weight(1f).testTag("HermesDockDone"), onClick = onDismiss)
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = LocalHermesGlassTokens.current.mutedText)
}

@Composable
internal fun ItemIcon(item: DockItem, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val glass = LocalHermesGlassTokens.current
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(glass.accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(item.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size / 2))
    }
}

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
