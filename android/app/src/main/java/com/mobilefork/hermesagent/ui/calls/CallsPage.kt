package com.mobilefork.hermesagent.ui.calls

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.avatarInitials
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.i18n.CallStrings
import com.mobilefork.hermesagent.ui.i18n.LocalCallStrings
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** What the Conversations page needs to offer calls. */
internal class CallsUi(
    /** The calls placed from this app, newest first. */
    val records: List<CallRecord>,
    val onCallAgent: (AgentProfile) -> Unit,
    /** Called after a call to a real person was handed to the phone, so it can be listed. */
    val onPersonCalled: (name: String, number: String) -> Unit,
)

/**
 * The Calls tab, laid out the way a phone's calling app is: a search field, the recent calls, then
 * the people to call (the agents, and the contacts of the phone), with a keypad button for numbers
 * that are in no list. Tapping anyone calls them.
 */
@Composable
internal fun CallsPage(
    agents: List<AgentProfile>,
    calls: CallsUi,
    bottomClearance: Dp,
    modifier: Modifier = Modifier,
) {
    val words = LocalCallStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var keypadOpen by rememberSaveable { mutableStateOf(false) }
    var digits by rememberSaveable { mutableStateOf("") }
    var contactsGranted by remember { mutableStateOf(hasContactsPermission(context)) }
    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    LaunchedEffect(contactsGranted) {
        contacts = if (contactsGranted) withContext(Dispatchers.IO) { loadDeviceContacts(context) } else emptyList()
    }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsGranted = granted
    }

    // A real person is reached through the phone's own calling, which shows its own call screen.
    var pendingPerson by remember { mutableStateOf<Pair<String, String>?>(null) }
    fun dial(name: String, number: String) {
        if (placePhoneCall(context, number)) calls.onPersonCalled(name, dialableNumber(number))
    }
    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingPerson?.let { (name, number) -> dial(name, number) }
        pendingPerson = null
    }
    fun callPerson(name: String, number: String) {
        val allowed = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (allowed) {
            dial(name, number)
        } else {
            pendingPerson = name to number
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    val search = query.trim()
    val shownAgents = remember(agents, search) { agents.filter { search.isEmpty() || it.name.contains(search, ignoreCase = true) } }
    val shownContacts = remember(contacts, search) {
        contacts.filter { search.isEmpty() || it.name.contains(search, ignoreCase = true) || dialableNumber(it.number).contains(dialableNumber(search).ifEmpty { "\u0000" }) }
    }
    // The first contact of each letter carries the letter's heading.
    val letterStarts = remember(shownContacts) {
        shownContacts.distinctBy { contactInitial(it) }.map { it.id }.toSet()
    }
    val typedNumber = dialableNumber(search)
    val searching = search.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("HermesCallsList"),
            contentPadding = PaddingValues(bottom = bottomClearance + if (keypadOpen) KEYPAD_PANEL_HEIGHT else 88.dp),
        ) {
            item(key = "calls-search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(64) },
                    singleLine = true,
                    placeholder = { Text(words.searchHint, color = glass.mutedText) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_ui_search), contentDescription = null, tint = glass.mutedText, modifier = Modifier.size(20.dp))
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("HermesCallsSearch"),
                )
            }
            if (!contactsGranted && !searching) {
                item(key = "calls-contacts-permission") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                            .hermesGlassSurface(RoundedCornerShape(20.dp), shadowRadius = 6.dp)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(words.contactsAllowTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(words.contactsAllowBody, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                        }
                        Text(
                            text = words.contactsAllow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("HermesCallsAllowContacts"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            if (searching && typedNumber.length >= 3) {
                item(key = "calls-number") {
                    CallRow(
                        avatar = { RoundIcon(R.drawable.ic_call_phone) },
                        title = words.callNumber(typedNumber),
                        subtitle = words.kindPhone,
                        trailing = "",
                        description = words.callNumber(typedNumber),
                        tag = "HermesCallNumber",
                        onClick = { callPerson(typedNumber, typedNumber) },
                    )
                }
            }
            if (!searching) {
                item(key = "calls-recents-title") { CallsSectionTitle(words.recents) }
                if (calls.records.isEmpty()) {
                    item(key = "calls-recents-empty") {
                        Text(
                            text = words.emptyRecents,
                            modifier = Modifier.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.mutedText,
                        )
                    }
                }
                items(calls.records.take(RECENTS_SHOWN), key = { "recent-${it.id}" }) { record ->
                    val agent = agents.firstOrNull { it.id == record.target }.takeIf { record.kind == CallKind.Agent }
                    CallRow(
                        avatar = { if (agent != null) AgentAvatar(agent = agent, size = 48.dp) else InitialsAvatar(record.name, 48.dp) },
                        title = record.name,
                        subtitle = recordSubtitle(record, words),
                        subtitleIcon = R.drawable.ic_call_made,
                        trailing = formatCallTime(record.startedAtEpochMs, words),
                        description = words.callAgain(record.name),
                        tag = "HermesCallRecent-${record.id}",
                        onClick = {
                            if (agent != null) calls.onCallAgent(agent)
                            else if (record.kind == CallKind.Person) callPerson(record.name, record.target)
                        },
                    )
                    ShellRowDivider(startInset = 62.dp)
                }
            }
            if (shownAgents.isNotEmpty()) {
                item(key = "calls-agents-title") { CallsSectionTitle(words.agents) }
                items(shownAgents, key = { "call-agent-${it.id}" }) { agent ->
                    CallRow(
                        avatar = { AgentAvatar(agent = agent, size = 48.dp) },
                        title = agent.name,
                        subtitle = agent.title.ifBlank { words.kindAgent },
                        trailing = "",
                        description = words.callAction + " " + agent.name,
                        tag = "HermesCallAgent-${agent.id}",
                        onClick = { calls.onCallAgent(agent) },
                    )
                }
            }
            if (shownContacts.isNotEmpty()) {
                item(key = "calls-contacts-title") { CallsSectionTitle(words.contacts) }
                items(shownContacts, key = { "contact-${it.id}" }) { contact ->
                    val initial = contactInitial(contact)
                    val showLetter = !searching && contact.id in letterStarts
                    Column {
                        if (showLetter) {
                            Text(
                                text = initial.toString(),
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        CallRow(
                            avatar = { InitialsAvatar(contact.name, 48.dp) },
                            title = contact.name,
                            subtitle = contact.number,
                            trailing = "",
                            description = words.callAction + " " + contact.name,
                            tag = "HermesCallContact-${contact.id}",
                            onClick = { callPerson(contact.name, contact.number) },
                        )
                    }
                }
            }
            if (searching && shownAgents.isEmpty() && shownContacts.isEmpty() && typedNumber.length < 3) {
                item(key = "calls-no-match") {
                    Text(
                        text = words.noMatches,
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.mutedText,
                    )
                }
            }
        }

        // The keypad button, where a phone puts it.
        if (!keypadOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = bottomClearance + 12.dp, end = 4.dp)
                    .size(60.dp)
                    .hermesGlassSurface(CircleShape, shadowRadius = 10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(role = Role.Button) { keypadOpen = true }
                    .testTag("HermesCallsKeypadButton"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_call_dialpad),
                    contentDescription = words.keypad,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        AnimatedVisibility(
            visible = keypadOpen,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            val match = remember(digits, contacts, agents) {
                val clean = dialableNumber(digits)
                if (clean.length < 3) null else contacts.firstOrNull { dialableNumber(it.number).endsWith(clean) || dialableNumber(it.number).contains(clean) }?.name
            }
            DialPad(
                digits = digits,
                matchName = match,
                bottomClearance = bottomClearance,
                onDigit = { digits = (digits + it).take(32) },
                onBackspace = { digits = digits.dropLast(1) },
                onClear = { digits = "" },
                onCall = {
                    val number = dialableNumber(digits)
                    if (number.isNotEmpty()) callPerson(match ?: number, number)
                },
                onClose = { keypadOpen = false },
            )
        }
    }
}

private fun contactInitial(contact: DeviceContact): Char =
    contact.name.first().uppercaseChar().let { if (it.isLetter()) it else '#' }

private val KEYPAD_PANEL_HEIGHT = 400.dp
private const val RECENTS_SHOWN = 12

@Composable
private fun CallsSectionTitle(title: String) {
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
private fun CallRow(
    avatar: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: String,
    description: String,
    tag: String,
    onClick: () -> Unit,
    subtitleIcon: Int? = null,
) {
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
            }
            .padding(vertical = 10.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        avatar()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (subtitleIcon != null) {
                        Icon(painterResource(subtitleIcon), contentDescription = null, tint = glass.success, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (trailing.isNotBlank()) {
            Text(text = trailing, style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
        }
    }
}

@Composable
private fun InitialsAvatar(name: String, size: Dp) {
    val glass = LocalHermesGlassTokens.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(glass.accentContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitials(name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RoundIcon(iconRes: Int) {
    val glass = LocalHermesGlassTokens.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(glass.successContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = glass.success, modifier = Modifier.size(22.dp))
    }
}

private fun recordSubtitle(record: CallRecord, words: CallStrings): String {
    val kind = if (record.kind == CallKind.Agent) words.kindAgent else words.kindPhone
    val length = if (record.durationSeconds > 0) words.minutesSeconds(record.durationSeconds / 60, record.durationSeconds % 60) else ""
    return listOf(words.outgoing, kind, length).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatCallTime(epochMs: Long, words: CallStrings): String {
    val then = Calendar.getInstance().apply { timeInMillis = epochMs }
    val now = Calendar.getInstance()
    val sameDay = { a: Calendar, b: Calendar -> a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(then, now) -> DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMs))
        sameDay(then, yesterday) -> words.yesterday
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(epochMs))
    }
}

// ---- the keypad ----

private val KEY_LETTERS = mapOf(
    '1' to "", '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL", '6' to "MNO",
    '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '*' to "", '0' to "+", '#' to "",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialPad(
    digits: String,
    matchName: String?,
    bottomClearance: Dp,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCall: () -> Unit,
    onClose: () -> Unit,
) {
    val words = LocalCallStrings.current
    val glass = LocalHermesGlassTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .hermesGlassSurface(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), shadowRadius = 14.dp)
            // Solid, so the list behind it never shows through the keys.
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(top = 8.dp, bottom = bottomClearance)
            .testTag("HermesDialPad"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 24.dp)
                .clickable(role = Role.Button, onClickLabel = words.closeKeypad, onClick = onClose)
                .semantics { contentDescription = words.closeKeypad }
                .testTag("HermesDialClose"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(glass.mutedText.copy(alpha = 0.5f)))
        }
        Text(
            text = digits.ifEmpty { words.dialHint },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .testTag("HermesDialDisplay"),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (digits.length > 14) 24.sp else 32.sp),
            fontWeight = FontWeight.Normal,
            color = if (digits.isEmpty()) glass.mutedText else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = matchName.orEmpty(),
            modifier = Modifier.height(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                row.forEach { key ->
                    DialKey(
                        key = key,
                        onClick = { onDigit(key) },
                        onLongClick = if (key == '0') ({ onDigit('+') }) else null,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(width = 72.dp, height = 1.dp))
            Spacer(Modifier.size(28.dp))
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 64.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (digits.isEmpty()) glass.success.copy(alpha = 0.45f) else glass.success)
                    .clickable(enabled = digits.isNotEmpty(), role = Role.Button, onClick = onCall)
                    .semantics { contentDescription = words.callAction }
                    .testTag("HermesDialCall"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_call_phone), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.size(28.dp))
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 64.dp)
                    .combinedClickable(enabled = digits.isNotEmpty(), role = Role.Button, onClick = onBackspace, onLongClick = onClear)
                    .semantics { contentDescription = words.backspace }
                    .testTag("HermesDialBackspace"),
                contentAlignment = Alignment.Center,
            ) {
                if (digits.isNotEmpty()) {
                    Icon(painterResource(R.drawable.ic_call_backspace), contentDescription = null, tint = glass.mutedText, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(key: Char, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val glass = LocalHermesGlassTokens.current
    Column(
        modifier = Modifier
            .size(width = 72.dp, height = 64.dp)
            .clip(RoundedCornerShape(50))
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = key.toString() }
            .testTag("HermesDialKey-$key"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = key.toString(), fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = KEY_LETTERS[key].orEmpty().ifEmpty { " " },
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = glass.mutedText,
        )
    }
}
