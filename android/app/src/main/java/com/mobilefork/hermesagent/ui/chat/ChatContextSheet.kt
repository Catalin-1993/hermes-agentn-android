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
)

internal data class ChatContextActions(
    val onCamera: () -> Unit,
    val onPhotos: () -> Unit,
    /** Applies a provider and model to the agent the sheet was opened from. */
    val onPickModel: (provider: String, model: String) -> Unit,
    /** Null for the main agent, which has no "own" model to go back from. */
    val onUseMainModel: (() -> Unit)?,
    val onOpenSkills: () -> Unit,
    val onOpenConnectors: () -> Unit,
    val onSignalQuickAction: (SignalIntelligenceQuickAction) -> Unit,
    val onDismiss: () -> Unit,
)

private enum class ContextPage { Home, Model, Quick }

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
    val shell = LocalShellStrings.current
    var page by rememberSaveable { mutableStateOf(ContextPage.Home) }
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
                .testTag("HermesChatContextSheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (page) {
                ContextPage.Home -> ContextHome(
                    state = state,
                    actions = actions,
                    onOpenModel = { page = ContextPage.Model },
                    onOpenQuick = { page = ContextPage.Quick },
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
}

@Composable
private fun ContextHome(
    state: ChatContextState,
    actions: ChatContextActions,
    onOpenModel: () -> Unit,
    onOpenQuick: () -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
    }
    val hasModelRow = !state.isGroup
    val hasSkillsRow = !state.isGroup && state.skillsActive != null
    ShellGroupCard(modifier = Modifier.testTag("HermesChatComposerActions")) {
        if (hasModelRow) {
            val own = state.agent.provider.isNotBlank()
            val provider = if (own) state.agent.provider else state.globalProvider
            val model = if (own) state.agent.model else state.globalModel
            ShellListRow(
                title = shell.contextModel,
                subtitle = contextModelSummary(provider, model),
                modifier = Modifier.testTag("HermesChatModelRow"),
                onClick = onOpenModel,
                leading = { ShellRowIcon(R.drawable.ic_set_model) },
                trailing = { ShellChevron() },
            )
            ShellRowDivider(startInset = 54.dp)
        }
        if (hasSkillsRow) {
            ShellListRow(
                title = shell.contextSkills,
                subtitle = if (state.skillsTotal > 0) shell.contextSkillsCount(state.skillsActive ?: 0, state.skillsTotal) else "",
                modifier = Modifier.testTag("HermesChatSkillsRow"),
                onClick = {
                    actions.onDismiss()
                    actions.onOpenSkills()
                },
                leading = { ShellRowIcon(R.drawable.ic_set_bolt) },
                trailing = { ShellChevron() },
            )
            ShellRowDivider(startInset = 54.dp)
        }
        ShellListRow(
            title = shell.contextConnectors,
            subtitle = shell.contextConnectorsCount(state.connectedCount),
            modifier = Modifier.testTag("HermesChatConnectorsRow"),
            onClick = {
                actions.onDismiss()
                actions.onOpenConnectors()
            },
            leading = { ShellRowIcon(R.drawable.ic_set_plug) },
            trailing = { ShellChevron() },
        )
        if (!com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
            ShellRowDivider(startInset = 54.dp)
            ShellListRow(
                title = shell.contextQuickActions,
                modifier = Modifier.testTag("HermesChatQuickActionsRow"),
                onClick = onOpenQuick,
                leading = { ShellRowIcon(R.drawable.ic_set_sliders) },
                trailing = { ShellChevron() },
            )
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
            .height(88.dp)
            .hermesPressScale(interaction)
            .hermesGlassSurface(shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = LocalHermesGlassTokens.current.mutedText,
            modifier = Modifier.padding(top = 8.dp),
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
