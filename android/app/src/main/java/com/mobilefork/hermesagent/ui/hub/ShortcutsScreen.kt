package com.mobilefork.hermesagent.ui.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.CalendarEventItem
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.chat.ChatConversationSummary
import com.mobilefork.hermesagent.ui.i18n.HubStrings
import com.mobilefork.hermesagent.ui.i18n.LocalHubStrings
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellPageTitle
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val COLUMNS = 3

/** The apps worth pinning, when they are on the phone. */
internal val PinnableApps = listOf(
    "WhatsApp" to "com.whatsapp",
    "Instagram" to "com.instagram.android",
    "Facebook" to "com.facebook.katana",
    "Messenger" to "com.facebook.orca",
    "Telegram" to "org.telegram.messenger",
    "Gmail" to "com.google.android.gm",
    "YouTube" to "com.google.android.youtube",
    "TikTok" to "com.zhiliaoapp.musically",
    "X" to "com.twitter.android",
    "LinkedIn" to "com.linkedin.android",
    "Maps" to "com.google.android.apps.maps",
    "Chrome" to "com.android.chrome",
)

/** Opens another app by its package name; false when it is not installed. */
internal fun launchApp(context: Context, packageName: String): Boolean {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
    return runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}

/** Opens the store page of an app that is not installed. */
internal fun openStorePage(context: Context, packageName: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * The shortcuts page: today's plan, and tiles for the pages, agents, chats and apps the person
 * pinned. Tiles are added from a sheet and rearranged or removed in edit mode.
 */
@Composable
internal fun ShortcutsScreen(
    shortcuts: List<Shortcut>,
    templates: Map<LibraryTemplate, TemplateState>,
    agents: List<AgentProfile>,
    conversations: List<ChatConversationSummary>,
    events: List<CalendarEventItem>,
    bottomClearance: Dp,
    onOpen: (Shortcut) -> Unit,
    onChange: (List<Shortcut>) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    var editing by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val visible = shortcuts.filter { shortcut ->
        when (shortcut.kind) {
            ShortcutKind.Page -> DockItem.fromKey(shortcut.target)?.isAvailable(templates) == true
            ShortcutKind.Agent -> agents.any { it.id == shortcut.target }
            else -> true
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShellPageMargin)
            .padding(top = 8.dp, bottom = bottomClearance)
            .testTag("HermesShortcutsScreen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ShellPageTitle(title = words.shortcutsTitle) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShellRoundButton(
                    iconRes = if (editing) R.drawable.ic_ui_check else R.drawable.ic_ui_edit,
                    contentDescription = if (editing) words.shortcutsDone else words.shortcutsEdit,
                    onClick = { editing = !editing },
                    testTag = "HermesShortcutsEdit",
                )
                ShellRoundButton(
                    iconRes = R.drawable.ic_ui_plus,
                    contentDescription = words.shortcutsAdd,
                    onClick = { adding = true },
                    testTag = "HermesShortcutsAdd",
                )
            }
        }
        TodayCard(events = events, onClick = onOpenCalendar)
        if (visible.isEmpty()) {
            Text(
                text = words.shortcutsEmpty,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = glass.mutedText,
                textAlign = TextAlign.Center,
            )
        }
        visible.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { shortcut ->
                    val index = shortcuts.indexOfFirst { it.id == shortcut.id }
                    ShortcutTile(
                        shortcut = shortcut,
                        agents = agents,
                        conversations = conversations,
                        editing = editing,
                        canMoveBack = index > 0,
                        canMoveForward = index in 0 until shortcuts.lastIndex,
                        onOpen = { onOpen(shortcut) },
                        onRemove = { onChange(shortcuts.filterNot { it.id == shortcut.id }) },
                        onMove = { by ->
                            if (index >= 0) onChange(shortcuts.toMutableList().also { it.add(index + by, it.removeAt(index)) })
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (adding) {
        AddShortcutSheet(
            templates = templates,
            agents = agents,
            conversations = conversations,
            existing = shortcuts,
            onAdd = { onChange(shortcuts + it); adding = false },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun TodayCard(events: List<CalendarEventItem>, onClick: () -> Unit) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }
    val upcoming = remember(events, today) {
        events.filter { it.dateString == today }.sortedBy { it.time.ifBlank { "00:00" } }.take(3)
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hermesGlassSurface(shape, shadowRadius = 8.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag("HermesShortcutsToday"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_tab_calendar), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(words.shortcutsToday, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (upcoming.isEmpty()) {
            Text(words.shortcutsNoEvents, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
        }
        upcoming.forEach { event ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = event.time.ifBlank { words.shortcutsAllDay },
                    modifier = Modifier.size(width = 78.dp, height = 22.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    agents: List<AgentProfile>,
    conversations: List<ChatConversationSummary>,
    editing: Boolean,
    canMoveBack: Boolean,
    canMoveForward: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(24.dp)
    val label = when (shortcut.kind) {
        ShortcutKind.Page -> DockItem.fromKey(shortcut.target)?.title(words).orEmpty()
        ShortcutKind.Agent -> agents.firstOrNull { it.id == shortcut.target }?.name ?: shortcut.label
        ShortcutKind.Chat -> conversations.firstOrNull { it.id == shortcut.target }?.title?.ifBlank { shortcut.label } ?: shortcut.label
        ShortcutKind.App -> shortcut.label
    }
    Box(modifier = modifier.height(112.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hermesGlassSurface(shape, shadowRadius = 6.dp)
                .clip(shape)
                .clickable(enabled = !editing, role = Role.Button, onClick = onOpen)
                .semantics { contentDescription = label }
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .testTag("HermesShortcut-${shortcut.id}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (shortcut.kind) {
                ShortcutKind.Page -> DockItem.fromKey(shortcut.target)?.let { ItemIcon(it, size = 52.dp) }
                ShortcutKind.Agent -> agents.firstOrNull { it.id == shortcut.target }?.let { AgentAvatar(agent = it, size = 52.dp) }
                ShortcutKind.Chat -> Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(glass.accentContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_tab_chat), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
                ShortcutKind.App -> AppIcon(context, shortcut.target, 52.dp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (editing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = words.remove + " " + label }
                    .testTag("HermesShortcutRemove-${shortcut.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_ui_minus), contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
            }
            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)) {
                MoveButton(R.drawable.ic_ui_arrow_left, words.moveUp, canMoveBack, "HermesShortcutLeft-${shortcut.id}") { onMove(-1) }
                MoveButton(R.drawable.ic_ui_arrow_right, words.moveDown, canMoveForward, "HermesShortcutRight-${shortcut.id}") { onMove(1) }
            }
        }
    }
}

@Composable
private fun MoveButton(icon: Int, description: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .alpha(if (enabled) 1f else 0.25f)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
    }
}

/** The app's own icon from the phone, or a plain mark when it cannot be read. */
@Composable
internal fun AppIcon(context: Context, packageName: String, size: Dp) {
    val bitmap = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(128, 128).asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4)))
    } else {
        Box(
            modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(LocalHermesGlassTokens.current.accentContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_tab_apps), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size / 2))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddShortcutSheet(
    templates: Map<LibraryTemplate, TemplateState>,
    agents: List<AgentProfile>,
    conversations: List<ChatConversationSummary>,
    existing: List<Shortcut>,
    onAdd: (Shortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        AddShortcutContent(templates, agents, conversations, existing, onAdd)
    }
}

/** What the sheet shows; apart from the sheet so it can be drawn and tested on its own. */
@Composable
internal fun AddShortcutContent(
    templates: Map<LibraryTemplate, TemplateState>,
    agents: List<AgentProfile>,
    conversations: List<ChatConversationSummary>,
    existing: List<Shortcut>,
    onAdd: (Shortcut) -> Unit,
) {
    val words = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    val installed = remember { PinnableApps.filter { (_, pkg) -> context.packageManager.getLaunchIntentForPackage(pkg) != null } }
    fun pinned(kind: ShortcutKind, target: String) = existing.any { it.kind == kind && it.target == target }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
            .testTag("HermesShortcutSheet"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(words.shortcutsAdd, style = MaterialTheme.typography.titleLarge)
        SheetSection(words.shortcutsPages)
        DockItem.entries.filter { it != DockItem.Shortcuts && it.isAvailable(templates) }.forEach { item ->
            PickRow(
                title = item.title(words),
                done = pinned(ShortcutKind.Page, item.key),
                tag = "HermesShortcutPick-${item.key}",
                leading = { ItemIcon(item) },
                onClick = { onAdd(Shortcut(UUID.randomUUID().toString(), ShortcutKind.Page, item.key, "")) },
            )
        }
        SheetSection(words.shortcutsAgents)
        agents.forEach { agent ->
            PickRow(
                title = agent.name,
                done = pinned(ShortcutKind.Agent, agent.id),
                tag = "HermesShortcutPickAgent-${agent.id}",
                leading = { AgentAvatar(agent = agent, size = 40.dp) },
                onClick = { onAdd(Shortcut(UUID.randomUUID().toString(), ShortcutKind.Agent, agent.id, agent.name)) },
            )
        }
        val chats = conversations.filterNot { it.isGroup }.take(8)
        if (chats.isNotEmpty()) {
            SheetSection(words.shortcutsChats)
            chats.forEach { chat ->
                val title = chat.title.ifBlank { chat.preview }.ifBlank { words.dockChat }
                PickRow(
                    title = title,
                    done = pinned(ShortcutKind.Chat, chat.id),
                    tag = "HermesShortcutPickChat-${chat.id}",
                    leading = {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(glass.accentContainer), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_tab_chat), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                    },
                    onClick = { onAdd(Shortcut(UUID.randomUUID().toString(), ShortcutKind.Chat, chat.id, title)) },
                )
            }
        }
        SheetSection(words.shortcutsApps)
        if (installed.isEmpty()) Text(words.shortcutsNoApps, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
        installed.forEach { (name, pkg) ->
            PickRow(
                title = name,
                done = pinned(ShortcutKind.App, pkg),
                tag = "HermesShortcutPickApp-$pkg",
                leading = { AppIcon(context, pkg, 40.dp) },
                onClick = { onAdd(Shortcut(UUID.randomUUID().toString(), ShortcutKind.App, pkg, name)) },
            )
        }
    }
}

@Composable
private fun SheetSection(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.titleSmall,
        color = LocalHermesGlassTokens.current.mutedText,
    )
}

@Composable
private fun PickRow(title: String, done: Boolean, tag: String, leading: @Composable () -> Unit, onClick: () -> Unit) {
    val words: HubStrings = LocalHubStrings.current
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !done, role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading()
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (done) {
            Text(words.shortcutsAlready, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
        } else {
            Icon(painterResource(R.drawable.ic_ui_plus), contentDescription = words.add, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}
