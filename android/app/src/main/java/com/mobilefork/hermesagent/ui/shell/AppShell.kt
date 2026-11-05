package com.mobilefork.hermesagent.ui.shell

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.agents.AgentEditorSheet
import androidx.compose.runtime.key
import com.mobilefork.hermesagent.ui.agents.AgentsViewModel
import com.mobilefork.hermesagent.ui.agents.GroupEditorSheet
import com.mobilefork.hermesagent.ui.chat.ConversationHistoryList
import com.mobilefork.hermesagent.ui.ideas.IdeasScreen
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.normalizeUiLanguageTag
import com.mobilefork.hermesagent.ui.i18n.shellStringsFor
import androidx.compose.animation.core.tween

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.mobilefork.hermesagent.ui.theme.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.data.AppSettings
import com.mobilefork.hermesagent.data.AppSettingsPersistenceException
import com.mobilefork.hermesagent.data.ProviderPresets
import com.mobilefork.hermesagent.ui.auth.AuthScreen
import com.mobilefork.hermesagent.ui.auth.AuthViewModel
import com.mobilefork.hermesagent.ui.boot.BootUiState
import com.mobilefork.hermesagent.ui.chat.ChatScreen
import com.mobilefork.hermesagent.ui.chat.ChatViewModel
import com.mobilefork.hermesagent.ui.device.DeviceScreen
import com.mobilefork.hermesagent.ui.device.DeviceViewModel
import com.mobilefork.hermesagent.ui.kanban.KanbanScreen
import com.mobilefork.hermesagent.ui.kanban.KanbanViewModel
import com.mobilefork.hermesagent.ui.portal.NousPortalScreen
import com.mobilefork.hermesagent.ui.portal.NousPortalViewModel
import com.mobilefork.hermesagent.ui.i18n.AppLanguage
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import com.mobilefork.hermesagent.ui.i18n.hermesStringsFor
import com.mobilefork.hermesagent.ui.settings.SettingsScreen
import com.mobilefork.hermesagent.ui.settings.SettingsViewModel
import com.mobilefork.hermesagent.ui.terminal.TerminalScreen
import com.mobilefork.hermesagent.ui.terminal.TerminalViewModel
import com.mobilefork.hermesagent.ui.theme.HermesThemeConfig
import com.mobilefork.hermesagent.ui.theme.HermesTheme
import com.mobilefork.hermesagent.ui.theme.HermesBackdrop
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.normalizeThemeHex
import com.mobilefork.hermesagent.ui.theme.normalizeThemeCardShape

private data class ShellSettingsState(
    val languageTag: String = AppLanguage.ENGLISH.tag,
    val keywordHighlightingEnabled: Boolean = true,
    val themePrimaryHex: String = AppSettings.DEFAULT_THEME_PRIMARY_HEX,
    val themeSecondaryHex: String = AppSettings.DEFAULT_THEME_SECONDARY_HEX,
    val themeBackgroundHex: String = AppSettings.DEFAULT_THEME_BACKGROUND_HEX,
    val themeSurfaceHex: String = AppSettings.DEFAULT_THEME_SURFACE_HEX,
    val themeSurfaceVariantHex: String = AppSettings.DEFAULT_THEME_SURFACE_VARIANT_HEX,
    val themeCardShape: String = "rounded",
    val uiFontScale: Float = 1.0f,
)

private fun loadShellSettingsState(settingsStore: AppSettingsStore): ShellSettingsState {
    val stored = settingsStore.load()
    return ShellSettingsState(
        languageTag = normalizeUiLanguageTag(stored.languageTag),
        keywordHighlightingEnabled = stored.keywordHighlightingEnabled,
        themePrimaryHex = normalizeThemeHex(stored.themePrimaryHex, AppSettings.DEFAULT_THEME_PRIMARY_HEX),
        themeSecondaryHex = normalizeThemeHex(stored.themeSecondaryHex, AppSettings.DEFAULT_THEME_SECONDARY_HEX),
        themeBackgroundHex = normalizeThemeHex(stored.themeBackgroundHex, AppSettings.DEFAULT_THEME_BACKGROUND_HEX),
        themeSurfaceHex = normalizeThemeHex(stored.themeSurfaceHex, AppSettings.DEFAULT_THEME_SURFACE_HEX),
        themeSurfaceVariantHex = normalizeThemeHex(stored.themeSurfaceVariantHex, AppSettings.DEFAULT_THEME_SURFACE_VARIANT_HEX),
        themeCardShape = normalizeShellThemeCardShape(stored.themeCardShape),
        uiFontScale = com.mobilefork.hermesagent.data.AppSettings.normalizeUiFontScale(stored.uiFontScale),
    )
}

internal fun normalizeShellThemeCardShape(value: String): String = normalizeThemeCardShape(value)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppShellScreen(
    bootUiState: BootUiState,
    onRetryHermes: () -> Unit,
) {
    var currentSection by rememberSaveable { mutableStateOf(AppSection.Hermes) }
    var currentActions by remember { mutableStateOf<List<ShellActionItem>>(emptyList()) }
    var showActionSheet by rememberSaveable { mutableStateOf(false) }
    // Settings shows its inner page's name in the shell header while one is open.
    var settingsPageTitle by remember { mutableStateOf<String?>(null) }
    var subpageParent by remember { mutableStateOf(AppSection.Apps) }
    // The agent being edited: blank creates a new one, null means the editor is closed.
    var agentEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    // The group being edited: blank creates a new one, null means the editor is closed.
    var groupEditorId by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current.applicationContext
    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    var shellSettings by remember { mutableStateOf(loadShellSettingsState(appSettingsStore)) }
    // Keyed by languageTag so CompositionLocal always refreshes when language changes.
    val strings = remember(shellSettings.languageTag) {
        hermesStringsFor(AppLanguage.fromTag(shellSettings.languageTag))
    }
    val shell = remember(shellSettings.languageTag) { shellStringsFor(shellSettings.languageTag) }

    fun refreshShellSettings() {
        shellSettings = loadShellSettingsState(appSettingsStore)
    }

    fun persistShellSettings(transform: (AppSettings) -> AppSettings): Boolean {
        return try {
            appSettingsStore.update(transform)
            true
        } catch (error: AppSettingsPersistenceException) {
            Toast.makeText(
                context,
                "Unable to save Agent settings: ${error.message ?: error::class.java.simpleName}",
                Toast.LENGTH_LONG,
            ).show()
            false
        }
    }

    // Re-sync shell language/theme when returning to Settings or after external saves.
    LaunchedEffect(currentSection) {
        if (currentSection == AppSection.Settings) {
            refreshShellSettings()
        }
    }

    fun applyProvider(providerId: String): Boolean {
        val preset = ProviderPresets.find(providerId) ?: return false
        if (!persistShellSettings { current ->
            current.copy(
                provider = preset.id,
                baseUrl = preset.baseUrl,
                model = preset.modelHint,
            )
        }) return false
        refreshShellSettings()
        return true
    }

    fun applyModel(modelName: String): Boolean {
        val normalized = modelName.trim()
        if (normalized.isBlank()) return false
        if (!persistShellSettings { current -> current.copy(model = normalized) }) return false
        refreshShellSettings()
        return true
    }

    fun setActions(actions: List<ShellActionItem>) {
        currentActions = actions
        if (actions.isEmpty()) {
            showActionSheet = false
        }
    }

    fun navigateToSection(section: AppSection) {
        refreshShellSettings()
        settingsPageTitle = null
        val appsPages = shellAppsDestinations()
        subpageParent = if (currentSection in appsPages && section in appsPages) currentSection else AppSection.Apps
        if (section != currentSection) {
            showActionSheet = false
        }
        currentSection = section
    }

    val isSubpage = currentSection in shellAppsDestinations()

    fun goBack() {
        // One page opened from another (Settings > Connectors) returns to the page it came from.
        val target = if (isSubpage) subpageParent else AppSection.Hermes
        navigateToSection(target)
        subpageParent = AppSection.Apps
    }

    BackHandler(enabled = currentSection != AppSection.Hermes) { goBack() }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(currentSection) {
        setActions(emptyList())
    }

    HermesTheme(
        config = HermesThemeConfig(
            primaryHex = shellSettings.themePrimaryHex,
            secondaryHex = shellSettings.themeSecondaryHex,
            backgroundHex = shellSettings.themeBackgroundHex,
            surfaceHex = shellSettings.themeSurfaceHex,
            surfaceVariantHex = shellSettings.themeSurfaceVariantHex,
            cardShape = shellSettings.themeCardShape,
            fontScale = shellSettings.uiFontScale,
        ),
    ) {
        CompositionLocalProvider(LocalHermesStrings provides strings, LocalShellStrings provides shell) {
            HermesBackdrop(modifier = Modifier.fillMaxSize()) {
                val pageStates = rememberSaveableStateHolder()
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    containerColor = Color.Transparent,
                ) { innerPadding ->
                    // A plain cross-fade: nothing slides or resizes, so the canvas and the tab
                    // bar stay perfectly still while the page changes.
                    AnimatedContent(
                        targetState = currentSection,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        transitionSpec = {
                            fadeIn(tween(durationMillis = 190, delayMillis = 50)) togetherWith
                                fadeOut(tween(durationMillis = 90))
                        },
                        label = "shellPage",
                    ) { section ->
                        val setPageActions: (List<ShellActionItem>) -> Unit = { actions ->
                            // The page fading out must not overwrite the incoming page's actions.
                            if (section == currentSection) setActions(actions)
                        }
                        pageStates.SaveableStateProvider(section.name) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (section in shellAppsDestinations()) {
                                    ShellSubpageHeader(
                                        title = settingsPageTitle?.takeIf { section == AppSection.Settings }
                                            ?: section.shellTitle(shell),
                                        onBack = { backDispatcher?.onBackPressed() ?: goBack() },
                                        trailing = {
                                            if (currentActions.isNotEmpty() && section == currentSection) {
                                                ShellRoundButton(
                                                    iconRes = R.drawable.ic_ui_more_horiz,
                                                    contentDescription = shell.more,
                                                    onClick = { showActionSheet = true },
                                                    testTag = "HermesShellPageActions",
                                                )
                                            }
                                        },
                                    )
                                }
                                ShellDeferredPage(
                                    immediate = section == AppSection.Hermes,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                ) {
                                when (section) {
                            AppSection.Hermes -> {
                                val authViewModel: AuthViewModel = viewModel()
                                val chatViewModel: ChatViewModel = viewModel()
                                ChatScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = chatViewModel,
                                    keywordHighlightingEnabled = shellSettings.keywordHighlightingEnabled,
                                    aboveTabBar = true,
                                    authViewModel = authViewModel,
                                    onNavigateToSection = ::navigateToSection,
                                    onContextActionsChanged = setPageActions,
                                    onOpenContextActions = { showActionSheet = true },
                                    onOpenAgentProfile = { agent -> agentEditorId = agent.id },
                                    onOpenGroupDetails = { id -> groupEditorId = id },
                                    onApplyProvider = ::applyProvider,
                                    onApplyModel = ::applyModel,
                                )
                            }

                            AppSection.History -> {
                                val chatViewModel: ChatViewModel = viewModel()
                                val agentsViewModel: AgentsViewModel = viewModel()
                                val agents by agentsViewModel.agents.collectAsState()
                                val chatState by chatViewModel.uiState.collectAsState()
                                DisposableEffect(chatViewModel) {
                                    chatViewModel.showHistory()
                                    onDispose { chatViewModel.hideHistory() }
                                }
                                ConversationHistoryList(
                                    summaries = chatState.conversationSummaries,
                                    onOpenConversation = { id ->
                                        chatViewModel.openConversation(id)
                                        navigateToSection(AppSection.Hermes)
                                    },
                                    onStartNew = {
                                        chatViewModel.startNewConversation(AgentProfile.MAIN_ID)
                                        navigateToSection(AppSection.Hermes)
                                    },
                                    agents = agents,
                                    onOpenAgent = { agent ->
                                        chatViewModel.openAgentChat(agent.id)
                                        navigateToSection(AppSection.Hermes)
                                    },
                                    onEditAgent = { agent -> agentEditorId = agent.id },
                                    onNewAgent = { agentEditorId = "" },
                                    onNewGroup = { groupEditorId = "" },
                                    onEditGroup = { group -> groupEditorId = group.id },
                                    onRename = chatViewModel::renameConversation,
                                    onRegenerateTitle = chatViewModel::regenerateConversationTitle,
                                    onDelete = chatViewModel::deleteConversation,
                                    bottomClearance = ShellBottomClearance,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            AppSection.Ideas -> {
                                val chatViewModel: ChatViewModel = viewModel()
                                IdeasScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    bottomClearance = ShellBottomClearance,
                                    onSendPrompt = { prompt ->
                                        val chatNow = chatViewModel.uiState.value
                                        if (chatNow.isSending || chatNow.input.isNotBlank() || chatNow.attachments.isNotEmpty()) {
                                            chatViewModel.setStatus(shell.ideaBlocked)
                                        } else {
                                            chatViewModel.sendQuickPrompt(prompt)
                                        }
                                        navigateToSection(AppSection.Hermes)
                                    },
                                )
                            }

                            AppSection.Apps -> {
                                AppsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    bottomClearance = ShellBottomClearance,
                                    onOpenSection = ::navigateToSection,
                                )
                            }

                            AppSection.Accounts -> {
                                val authViewModel: AuthViewModel = viewModel()
                                AuthScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = authViewModel,
                                    extraBottomSpacing = ShellBottomClearance,
                                    onOpenSettings = { navigateToSection(AppSection.Settings) },
                                    onContextActionsChanged = setPageActions,
                                )
                            }

                            AppSection.NousPortal -> {
                                val portalViewModel: NousPortalViewModel = viewModel()
                                NousPortalScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = portalViewModel,
                                    extraBottomSpacing = ShellBottomClearance,
                                    onContextActionsChanged = setPageActions,
                                )
                            }

                            AppSection.Device -> {
                                val deviceViewModel: DeviceViewModel = viewModel()
                                DeviceScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = deviceViewModel,
                                    extraBottomSpacing = ShellBottomClearance,
                                    onContextActionsChanged = setPageActions,
                                )
                            }

                            AppSection.Kanban -> {
                                val kanbanViewModel: KanbanViewModel = viewModel()
                                KanbanScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = kanbanViewModel,
                                    extraBottomSpacing = ShellBottomClearance,
                                )
                            }

                            AppSection.Terminal -> {
                                val terminalViewModel: TerminalViewModel = viewModel()
                                TerminalScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = terminalViewModel,
                                    extraBottomSpacing = ShellBottomClearance,
                                )
                            }

                            AppSection.Settings -> {
                                val settingsViewModel: SettingsViewModel = viewModel()
                                LaunchedEffect(settingsViewModel) {
                                    settingsViewModel.reload()
                                }
                                SettingsScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    viewModel = settingsViewModel,
                                    extraBottomSpacing = ShellBottomClearance,
                                    onContextActionsChanged = setPageActions,
                                    onSettingsChanged = ::refreshShellSettings,
                                    onOpenSection = ::navigateToSection,
                                    onPageTitleChanged = { title -> if (section == currentSection) settingsPageTitle = title },
                                )
                            }
                        }
                                }
                            }
                        }
                    }
                }
                // List content dissolves before it reaches the translucent bar.
                val glass = LocalHermesGlassTokens.current
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(ShellBottomClearance)
                        .background(
                            Brush.verticalGradient(
                                0f to glass.backdropBottom.copy(alpha = 0f),
                                0.45f to glass.backdropBottom.copy(alpha = 0.9f),
                                1f to glass.backdropBottom,
                            ),
                        ),
                )
                ShellTabBar(
                    selected = if (isSubpage) AppSection.Apps else currentSection,
                    onSelect = ::navigateToSection,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = ShellPageMargin)
                        .padding(bottom = ShellTabBarGap),
                )
            }

            agentEditorId?.let { editingId ->
                val agentsViewModel: AgentsViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                AgentEditorSheet(
                    agentId = editingId,
                    viewModel = agentsViewModel,
                    onDismiss = { agentEditorId = null },
                    onSaved = { agent, isNew ->
                        agentEditorId = null
                        if (isNew && !chatViewModel.uiState.value.isSending) {
                            chatViewModel.openAgentChat(agent.id)
                            navigateToSection(AppSection.Hermes)
                        }
                    },
                )
            }

            groupEditorId?.let { editingId ->
                val agentsViewModel: AgentsViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                val isNew = editingId.isBlank()
                val existing = remember(editingId) { chatViewModel.participantsFor(editingId) }
                val storedIds = remember(editingId) {
                    chatViewModel.uiState.value.conversationSummaries.firstOrNull { it.id == editingId }?.memberIds.orEmpty()
                }
                key(editingId) {
                    GroupEditorSheet(
                        isNew = isNew,
                        initialName = if (isNew) "" else existing.groupName,
                        initialMemberIds = existing.members.map { it.id }.ifEmpty { storedIds },
                        initialAvatarPath = if (isNew) "" else existing.groupAvatarPath,
                        viewModel = agentsViewModel,
                        onDismiss = { groupEditorId = null },
                        onSave = { name, memberIds, avatarPath ->
                            groupEditorId = null
                            if (isNew) {
                                chatViewModel.createGroup(name, memberIds, avatarPath)
                                navigateToSection(AppSection.Hermes)
                            } else {
                                chatViewModel.updateGroup(editingId, name, memberIds, avatarPath)
                            }
                        },
                        onDelete = {
                            groupEditorId = null
                            chatViewModel.deleteConversation(editingId)
                        },
                    )
                }
            }

            if (showActionSheet && currentActions.isNotEmpty()) {
                ContextActionSheet(
                    section = currentSection,
                    actions = currentActions,
                    onDismiss = { showActionSheet = false },
                )
            }
        }
    }
}

/**
 * Gives the tap and the cross-fade their first frame before a heavy page composes, so a tab
 * switch answers at once. The frame is empty: no placeholder text flashes by.
 */
@Composable
private fun ShellDeferredPage(
    immediate: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var ready by remember { mutableStateOf(immediate) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        ready = true
    }
    Box(modifier = modifier) {
        if (ready) content()
    }
}

@Composable
private fun HermesSetupScreen(
    uiState: BootUiState,
    onRetry: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenPortal: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenSettings: () -> Unit,
    onContextActionsChanged: (List<ShellActionItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalHermesStrings.current
    LaunchedEffect(uiState.ready, uiState.error, uiState.status) {
        onContextActionsChanged(
            listOf(
                ShellActionItem(
                    label = strings.accounts.ifBlank { "Accounts" },
                    description = strings.accountsActionDescription(),
                    iconRes = R.drawable.ic_nav_accounts,
                    onClick = onOpenAccounts,
                ),
                ShellActionItem(
                    label = strings.settings.ifBlank { "Settings" },
                    description = strings.settingsActionDescription(),
                    iconRes = R.drawable.ic_nav_settings,
                    onClick = onOpenSettings,
                ),
                // label = "Provider Portal"
                ShellActionItem(
                    label = strings.portalTitle.ifBlank { "Provider Portal" },
                    description = strings.portalActionDescription(),
                    iconRes = R.drawable.ic_nav_portal,
                    onClick = onOpenPortal,
                ),
                // label = "Device"
                ShellActionItem(
                    label = strings.sectionDevice.ifBlank { "Device" },
                    description = strings.deviceActionDescription(),
                    iconRes = R.drawable.ic_nav_device,
                    onClick = onOpenDevice,
                ),
            )
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.agent_logo),
            contentDescription = strings.hermesLogoDescription,
            modifier = Modifier.size(72.dp),
        )
        Text(strings.bootStatusText(uiState.status), style = MaterialTheme.typography.headlineSmall)
        if (uiState.baseUrl.isNotBlank()) {
            Text(uiState.baseUrl, style = MaterialTheme.typography.bodySmall)
        }
        if (uiState.probeResult.isNotBlank()) {
            Text(uiState.probeResult, style = MaterialTheme.typography.bodySmall)
        }
        if (uiState.error.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
        Button(onClick = onRetry) {
            Text(strings.retryHermes())
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(strings.gettingStartedTitle(), style = MaterialTheme.typography.titleMedium)
                Text(strings.gettingStartedStep(1))
                Text(strings.gettingStartedStep(2))
                Text(strings.gettingStartedStep(3))
                Text(strings.gettingStartedStep(4))
            }
        }
    }
}
