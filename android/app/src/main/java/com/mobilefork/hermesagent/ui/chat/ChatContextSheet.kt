package com.mobilefork.hermesagent.ui.chat

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.ProviderPreset
import com.mobilefork.hermesagent.data.ProviderPresets
import com.mobilefork.hermesagent.data.SecureSecretsStore
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellChevron
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.shell.ShellRowIcon
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.TextButton
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import com.mobilefork.hermesagent.ui.theme.hermesPressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the "+" sheet needs to know about the conversation it was opened from. */
internal data class ChatContextState(
    val agent: AgentProfile,
    val isGroup: Boolean,
    val globalProvider: String,
    val globalModel: String,
    /** Null hides the Skills row: the agent has no device tools, or the edition has none. */
    val skillsActive: Int?,
    val skillsTotal: Int,
    val connectedCount: Int,
    val quickActionsEnabled: Boolean,
    val showCommandsTip: Boolean,
    val isListening: Boolean,
    val research: Boolean = false,
    val thinkingLevel: Int = 0,
    /** Every agent the person made, for inviting into a group. */
    val agents: List<AgentProfile> = emptyList(),
    /** The members of the open group chat; empty in a one-to-one chat. */
    val groupMemberIds: List<String> = emptyList(),
    val libraryPicks: List<LibraryPick> = emptyList(),
)

/** What the library offers to attach: a note's text, or a file already in the app. */
internal data class LibraryPick(val id: String, val title: String, val kind: LibraryPickKind)

internal enum class LibraryPickKind { Note, Document, Image }

internal data class ChatContextActions(
    val onCamera: () -> Unit,
    val onPhotos: () -> Unit,
    val onFiles: () -> Unit = {},
    val onPickFromLibrary: (LibraryPick) -> Unit = {},
    val onSetResearch: (Boolean) -> Unit = {},
    val onSetThinking: (Int) -> Unit = {},
    /** Starts a group with the open agent and these agents, or adds them to the open group. */
    val onInviteAgents: (List<String>) -> Unit = {},
    /** Applies a provider and model to the agent the sheet was opened from. */
    val onPickModel: (provider: String, model: String) -> Unit,
    /** Null for the main agent, which has no "own" model to go back from. */
    val onUseMainModel: (() -> Unit)?,
    val onOpenSkills: () -> Unit,
    /** Opens the page of installed and downloadable AI models. */
    val onOpenModels: () -> Unit = {},
    val onOpenConnectors: () -> Unit,
    val onSignalQuickAction: (SignalIntelligenceQuickAction) -> Unit,
    val onDismiss: () -> Unit,
)

private enum class ContextPage { Home, Model, Quick, Agents, Library, Voice }

/** The label shown for the model an agent will answer with, as "Provider · model". */
internal fun contextModelSummary(provider: String, model: String): String {
    val label = ProviderPresets.find(provider)?.label ?: provider
    return listOf(label, model).filter { it.isNotBlank() }.joinToString(" · ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatContextSheet(
    state: ChatContextState,
    actions: ChatContextActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        ChatContextContent(state, actions)
    }
}

/** What the sheet shows; apart from the sheet so it can be drawn and tested on its own. */
@Composable
internal fun ChatContextContent(
    state: ChatContextState,
    actions: ChatContextActions,
) {
    val shell = LocalShellStrings.current
    var page by rememberSaveable { mutableStateOf(ContextPage.Home) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
            .testTag("HermesChatContextSheet"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (page) {
            ContextPage.Home -> ContextHome(
                state = state,
                actions = actions,
                onOpenModel = { page = ContextPage.Model },
                onOpenQuick = { page = ContextPage.Quick },
                onOpenAgents = { page = ContextPage.Agents },
                onOpenLibrary = { page = ContextPage.Library },
                onOpenVoice = { page = ContextPage.Voice },
            )
            ContextPage.Voice -> ContextVoice(onBack = { page = ContextPage.Home })
            ContextPage.Agents -> ContextAgents(
                state = state,
                actions = actions,
                onBack = { page = ContextPage.Home },
            )
            ContextPage.Library -> ContextLibrary(
                state = state,
                actions = actions,
                onBack = { page = ContextPage.Home },
            )
            ContextPage.Quick -> ContextQuickActions(
                state = state,
                actions = actions,
                onBack = { page = ContextPage.Home },
            )
            ContextPage.Model -> ContextModelPicker(
                state = state,
                actions = actions,
                title = shell.modelPickerTitle,
                onBack = { page = ContextPage.Home },
            )
        }
    }
}

@Composable
private fun ContextHome(
    state: ChatContextState,
    actions: ChatContextActions,
    onOpenModel: () -> Unit,
    onOpenQuick: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenVoice: () -> Unit = {},
) {
    val shell = LocalShellStrings.current
    val words = com.mobilefork.hermesagent.ui.i18n.LocalChatStrings.current
    // Camera, photos, files and the library: one row, so the four fit side by side.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ContextTile(
            iconRes = R.drawable.ic_action_camera,
            label = shell.tileCamera,
            modifier = Modifier.weight(1f).testTag("HermesChatCameraButton"),
            onClick = {
                actions.onDismiss()
                actions.onCamera()
            },
        )
        ContextTile(
            iconRes = R.drawable.ic_action_image,
            label = shell.tilePhotos,
            modifier = Modifier.weight(1f).testTag("HermesChatAttachImageButton"),
            onClick = {
                actions.onDismiss()
                actions.onPhotos()
            },
        )
        ContextTile(
            iconRes = R.drawable.ic_action_file,
            label = words.tileFiles,
            modifier = Modifier.weight(1f).testTag("HermesChatAttachFileButton"),
            onClick = {
                actions.onDismiss()
                actions.onFiles()
            },
        )
        ContextTile(
            iconRes = R.drawable.ic_tab_library,
            label = words.tileLibrary,
            modifier = Modifier.weight(1f).testTag("HermesChatAttachLibraryButton"),
            onClick = onOpenLibrary,
        )
    }
    // Plain rows with one line each: no frame, no explanations.
    val rowPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 11.dp)
    Column(modifier = Modifier.testTag("HermesChatComposerActions")) {
        if (!state.isGroup) {
            ShellListRow(
                title = shell.contextModel,
                modifier = Modifier.testTag("HermesChatModelRow"),
                contentPadding = rowPadding,
                onClick = onOpenModel,
                leading = { ShellRowIcon(R.drawable.ic_set_model) },
                trailing = { ShellChevron() },
            )
        }
        if (!state.isGroup && state.skillsActive != null) {
            ShellListRow(
                title = shell.extra.capTitle,
                modifier = Modifier.testTag("HermesChatSkillsRow"),
                contentPadding = rowPadding,
                onClick = {
                    actions.onDismiss()
                    actions.onOpenSkills()
                },
                leading = { ShellRowIcon(R.drawable.ic_set_bolt) },
                trailing = { ShellChevron() },
            )
        }
        ShellListRow(
            title = words.researchTitle,
            modifier = Modifier.testTag("HermesChatResearchRow"),
            contentPadding = rowPadding,
            onClick = { actions.onSetResearch(!state.research) },
            leading = { ShellRowIcon(R.drawable.ic_action_research) },
            trailing = {
                androidx.compose.material3.Switch(
                    checked = state.research,
                    onCheckedChange = actions.onSetResearch,
                    modifier = Modifier.testTag("HermesChatResearchSwitch"),
                )
            },
        )
        ShellListRow(
            title = words.voiceTitle,
            modifier = Modifier.testTag("HermesChatVoiceRow"),
            contentPadding = rowPadding,
            onClick = onOpenVoice,
            leading = { ShellRowIcon(R.drawable.ic_action_speaker) },
            trailing = { ShellChevron() },
        )
        ShellListRow(
            title = words.agentsTitle,
            modifier = Modifier.testTag("HermesChatAgentsRow"),
            contentPadding = rowPadding,
            onClick = onOpenAgents,
            leading = { ShellRowIcon(R.drawable.ic_app_people) },
            trailing = { ShellChevron() },
        )
        if (!com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
            ShellListRow(
                title = shell.contextQuickActions,
                modifier = Modifier.testTag("HermesChatQuickActionsRow"),
                contentPadding = rowPadding,
                onClick = onOpenQuick,
                leading = { ShellRowIcon(R.drawable.ic_set_sliders) },
                trailing = { ShellChevron() },
            )
        }
    }
    ThinkingBar(level = state.thinkingLevel, onChange = actions.onSetThinking)
}

/** How hard the agent should think: drag along the bar, from the provider's own default to the most. */
@Composable
private fun ThinkingBar(level: Int, onChange: (Int) -> Unit) {
    val words = com.mobilefork.hermesagent.ui.i18n.LocalChatStrings.current
    val glass = LocalHermesGlassTokens.current
    val levels = words.thinkingLevels
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("HermesChatThinkingCard"),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_action_brain),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = words.thinkingTitle,
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = levels.getOrElse(level) { levels.first() },
                style = MaterialTheme.typography.labelLarge,
                color = glass.mutedText,
                modifier = Modifier.testTag("HermesChatThinkingValue"),
            )
        }
        androidx.compose.material3.Slider(
            value = level.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(0, levels.lastIndex)) },
            valueRange = 0f..levels.lastIndex.toFloat(),
            steps = (levels.size - 2).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().testTag("HermesChatThinkingSlider"),
        )
    }
}

@Composable
private fun ContextAgents(
    state: ChatContextState,
    actions: ChatContextActions,
    onBack: () -> Unit,
) {
    val words = com.mobilefork.hermesagent.ui.i18n.LocalChatStrings.current
    val glass = LocalHermesGlassTokens.current
    // The agents already in the conversation are not offered again.
    val open = remember(state.agents, state.groupMemberIds, state.agent) {
        val inChat = if (state.isGroup) state.groupMemberIds.toSet() else setOf(state.agent.id)
        state.agents.filter { it.id !in inChat }
    }
    var picked by rememberSaveable { mutableStateOf(setOf<String>()) }
    ContextPageHeader(title = words.agentsTitle, note = words.agentsPick, onBack = onBack)
    if (open.isEmpty()) {
        Text(words.agentsNone, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
    } else {
        ShellGroupCard(modifier = Modifier.testTag("HermesChatAgentList")) {
            open.forEachIndexed { index, agent ->
                if (index > 0) ShellRowDivider(startInset = 62.dp)
                val on = agent.id in picked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { picked = if (on) picked - agent.id else picked + agent.id }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("HermesChatInvite-${agent.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    com.mobilefork.hermesagent.ui.agents.AgentAvatar(agent = agent, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(agent.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        if (agent.title.isNotBlank()) {
                            Text(agent.title, style = MaterialTheme.typography.bodySmall, color = glass.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    androidx.compose.material3.Checkbox(checked = on, onCheckedChange = { picked = if (it) picked + agent.id else picked - agent.id })
                }
            }
        }
        Button(
            onClick = {
                actions.onInviteAgents(picked.toList())
                actions.onDismiss()
            },
            enabled = picked.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("HermesChatInviteApply"),
        ) { Text(if (state.isGroup) words.agentsAddToGroup else words.agentsStartGroup) }
    }
}

@Composable
private fun ContextLibrary(
    state: ChatContextState,
    actions: ChatContextActions,
    onBack: () -> Unit,
) {
    val words = com.mobilefork.hermesagent.ui.i18n.LocalChatStrings.current
    val glass = LocalHermesGlassTokens.current
    ContextPageHeader(title = words.libraryPickTitle, note = "", onBack = onBack)
    if (state.libraryPicks.isEmpty()) {
        Text(words.libraryEmpty, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
    } else {
        ShellGroupCard(modifier = Modifier.testTag("HermesChatLibraryList")) {
            state.libraryPicks.take(60).forEachIndexed { index, pick ->
                if (index > 0) ShellRowDivider(startInset = 54.dp)
                ShellListRow(
                    title = pick.title,
                    subtitle = when (pick.kind) {
                        LibraryPickKind.Note -> words.libraryKindNote
                        LibraryPickKind.Document -> words.libraryKindDocument
                        LibraryPickKind.Image -> words.libraryKindImage
                    },
                    modifier = Modifier.testTag("HermesChatLibraryPick-${pick.id}"),
                    onClick = {
                        actions.onDismiss()
                        actions.onPickFromLibrary(pick)
                    },
                    leading = {
                        ShellRowIcon(if (pick.kind == LibraryPickKind.Image) R.drawable.ic_action_image else R.drawable.ic_action_file)
                    },
                )
            }
        }
    }
}

@Composable
private fun ContextQuickActions(
    state: ChatContextState,
    actions: ChatContextActions,
    onBack: () -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    ContextPageHeader(title = shell.contextQuickActions, note = "", onBack = onBack)
    SignalIntelligenceQuickActionGrid(
        compact = true,
        enabled = state.quickActionsEnabled,
        onSignalQuickAction = { action ->
            actions.onDismiss()
            actions.onSignalQuickAction(action)
        },
    )
    if (state.showCommandsTip) {
        val strings = com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings.current
        QuietMetaText(text = strings.chatCommandsTip(state.isListening), color = glass.mutedText)
    }
}

@Composable
private fun ContextPageHeader(title: String, note: String, onBack: () -> Unit) {
    val shell = LocalShellStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ShellRoundButton(
            iconRes = R.drawable.ic_ui_back,
            contentDescription = shell.back,
            onClick = onBack,
            size = 40.dp,
            testTag = "HermesChatContextBack",
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (note.isNotBlank()) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = LocalHermesGlassTokens.current.mutedText)
            }
        }
    }
}

@Composable
private fun ContextTile(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .height(80.dp)
            .hermesPressScale(interaction)
            .hermesGlassSurface(shape)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalHermesGlassTokens.current.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ContextModelPicker(
    state: ChatContextState,
    actions: ChatContextActions,
    title: String,
    onBack: () -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = LocalContext.current
    val agent = state.agent
    // Other agents can only use providers the chat client talks to directly, as in the agent editor.
    val providers = remember(agent.isMain) {
        ProviderPresets.defaults.filter {
            (agent.isMain || usesDirectOpenAiCompatibleTransport(it.id)) &&
                (!com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION ||
                    com.mobilefork.hermesagent.play.PlayNetworkPolicy.supportsProvider(it.id))
        }
    }
    val ownModel = agent.provider.isNotBlank()
    var provider by rememberSaveable { mutableStateOf(if (ownModel) agent.provider else state.globalProvider) }
    var model by rememberSaveable { mutableStateOf(if (ownModel) agent.model else state.globalModel) }
    var keyReady by remember(provider) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(provider) {
        keyReady = withContext(Dispatchers.IO) {
            runCatching { SecureSecretsStore(context).loadApiKey(provider).isNotBlank() }.getOrDefault(false)
        }
    }
    val preset = ProviderPresets.find(provider)

    ContextPageHeader(
        title = title,
        note = if (agent.isMain) shell.contextModelGlobalNote else shell.contextModelAgentNote(agent.name),
        onBack = onBack,
    )
    Text(
        text = shell.modelProviderLabel,
        style = MaterialTheme.typography.labelLarge,
        color = glass.mutedText,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("HermesChatProviderChips"),
    ) {
        items(providers, key = { it.id }) { item ->
            ProviderChip(
                preset = item,
                selected = item.id == provider,
                onClick = {
                    if (item.id != provider) {
                        provider = item.id
                        model = item.modelHint
                    }
                },
            )
        }
    }
    OutlinedTextField(
        value = model,
        onValueChange = { model = it },
        modifier = Modifier.fillMaxWidth().testTag("HermesChatModelInput"),
        singleLine = true,
        label = { Text(shell.modelNameField) },
        shape = RoundedCornerShape(16.dp),
    )
    val hint = preset?.modelHint.orEmpty()
    if (hint.isNotBlank() && hint != model.trim()) {
        Row(
            modifier = Modifier
                .clickable { model = hint }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("${shell.modelSuggested}:", style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    when (keyReady) {
        true -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_check),
                contentDescription = null,
                tint = glass.success,
                modifier = Modifier.size(18.dp),
            )
            Text(shell.modelKeyReady, style = MaterialTheme.typography.bodySmall, color = glass.success)
        }
        false -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(shell.modelKeyMissing, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            TextButton(
                onClick = {
                    actions.onDismiss()
                    actions.onOpenConnectors()
                },
                modifier = Modifier.testTag("HermesChatModelAddKey"),
            ) { Text(shell.modelAddKey) }
        }
        null -> Unit
    }
    Button(
        onClick = {
            actions.onPickModel(provider, model.trim())
            actions.onDismiss()
        },
        enabled = model.isNotBlank() && provider.isNotBlank(),
        modifier = Modifier.fillMaxWidth().testTag("HermesChatModelApply"),
    ) { Text(shell.modelUseThis) }
    if (actions.onUseMainModel != null && ownModel) {
        TextButton(
            onClick = {
                actions.onUseMainModel.invoke()
                actions.onDismiss()
            },
            modifier = Modifier.fillMaxWidth().testTag("HermesChatModelUseMain"),
        ) { Text(shell.modelUseMain, textAlign = TextAlign.Center) }
    }
    // The models that run on this phone live on their own page, one tap from here.
    TextButton(
        onClick = {
            actions.onDismiss()
            actions.onOpenModels()
        },
        modifier = Modifier.fillMaxWidth().testTag("HermesChatLocalModelsRow"),
    ) { Text(com.mobilefork.hermesagent.ui.i18n.LocalChatStrings.current.modelsTitle, textAlign = TextAlign.Center) }
}

@Composable
internal fun ProviderChip(
    preset: ProviderPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val glass = LocalHermesGlassTokens.current
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) glass.tabSelected else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.testTag("HermesChatProvider_${preset.id}"),
    ) {
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}


/** Picks the voice that reads replies: the phone's own, or ElevenLabs with a key. */
@Composable
private fun ContextVoice(onBack: () -> Unit) {
    val words = com.mobilefork.hermesagent.ui.i18n.LocalChatStrings.current
    val context = LocalContext.current
    val settings = remember { VoiceSettings(context) }
    var provider by rememberSaveable { mutableStateOf(settings.provider) }
    var key by rememberSaveable { mutableStateOf("") }
    var voice by rememberSaveable { mutableStateOf(settings.elevenLabsVoiceId) }
    val hasKey = remember { settings.elevenLabsKey.isNotBlank() }
    ContextPageHeader(title = words.voiceTitle, note = words.voiceNote, onBack = onBack)
    com.mobilefork.hermesagent.ui.shell.ShellSegmentedTabs(
        options = listOf(words.voiceSystem, "ElevenLabs"),
        selectedIndex = if (provider == VoiceSettings.ELEVENLABS) 1 else 0,
        onSelect = {
            provider = if (it == 1) VoiceSettings.ELEVENLABS else VoiceSettings.SYSTEM
            settings.provider = provider
        },
        testTagPrefix = "HermesVoiceProvider",
    )
    if (provider == VoiceSettings.ELEVENLABS) {
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            modifier = Modifier.fillMaxWidth().testTag("HermesVoiceKey"),
            singleLine = true,
            label = { Text(if (hasKey) words.voiceKeySaved else "API key") },
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = voice,
            onValueChange = { voice = it },
            modifier = Modifier.fillMaxWidth().testTag("HermesVoiceId"),
            singleLine = true,
            label = { Text(words.voiceId) },
            shape = RoundedCornerShape(16.dp),
        )
        Button(
            onClick = {
                if (key.isNotBlank()) settings.elevenLabsKey = key
                settings.elevenLabsVoiceId = voice
                onBack()
            },
            modifier = Modifier.fillMaxWidth().testTag("HermesVoiceSave"),
        ) { Text(words.voiceSave) }
    }
}
