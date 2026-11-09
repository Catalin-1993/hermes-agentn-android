package com.mobilefork.hermesagent.play

import com.mobilefork.hermesagent.ui.theme.TextButton

import androidx.compose.runtime.collectAsState
import com.mobilefork.hermesagent.ui.chat.ConversationHistoryList
import com.mobilefork.hermesagent.ui.shell.ContextActionSheet
import com.mobilefork.hermesagent.ui.theme.HermesBackdrop
import com.mobilefork.hermesagent.ui.shell.ShellActionItem
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.shellStringsFor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.data.AppSettingsStore
import com.mobilefork.hermesagent.data.ProviderPresets
import com.mobilefork.hermesagent.ui.auth.AuthViewModel
import com.mobilefork.hermesagent.ui.chat.ChatScreen
import com.mobilefork.hermesagent.ui.chat.ChatViewModel
import com.mobilefork.hermesagent.ui.i18n.*
import com.mobilefork.hermesagent.ui.settings.SettingsScreen
import com.mobilefork.hermesagent.ui.shell.AppSection
import com.mobilefork.hermesagent.ui.theme.HermesTheme
import com.mobilefork.hermesagent.ui.theme.HermesThemeConfig

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PlayShell(chat: ChatViewModel) {
    val context = LocalContext.current
    val store = remember { AppSettingsStore(context) }
    var settings by remember { mutableStateOf(store.load()) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val strings = hermesStringsFor(AppLanguage.fromTag(settings.languageTag))
    val shell = shellStringsFor(settings.languageTag)
    val auth: AuthViewModel = viewModel()
    val chatState by chat.uiState.collectAsState()
    var chatActions by remember { mutableStateOf<List<ShellActionItem>>(emptyList()) }
    var chatActionsOpen by rememberSaveable { mutableStateOf(false) }
    HermesTheme(HermesThemeConfig(
        primaryHex = settings.themePrimaryHex, secondaryHex = settings.themeSecondaryHex,
        backgroundHex = settings.themeBackgroundHex, surfaceHex = settings.themeSurfaceHex,
        surfaceVariantHex = settings.themeSurfaceVariantHex, cardShape = settings.themeCardShape,
        fontScale = settings.uiFontScale,
    )) {
        CompositionLocalProvider(LocalHermesStrings provides strings, LocalShellStrings provides shell, com.mobilefork.hermesagent.ui.i18n.LocalChatStrings provides com.mobilefork.hermesagent.ui.i18n.chatStringsFor(settings.languageTag)) {
            HermesBackdrop(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { settingsOpen = false; chat.hideHistory() }, Modifier.testTag("PlayChatTab")) {
                            Text(strings.sectionHermes)
                        }
                        TextButton(onClick = { settingsOpen = false; chat.showHistory() }, Modifier.testTag("PlayHistoryTab")) {
                            Text(shell.tabHistory)
                        }
                        TextButton(onClick = { settingsOpen = true; chat.hideHistory() }, Modifier.testTag("PlaySettingsTab")) {
                            Text(strings.sectionSettings)
                        }
                    }
                    Text(strings.playEditionSummary(), Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("PlayEditionIdentity"), style = MaterialTheme.typography.bodySmall)
                    if (settingsOpen) {
                        SettingsScreen(
                            Modifier.weight(1f),
                            onSettingsChanged = { settings = store.load() },
                            showPageHeader = true,
                        )
                    } else if (chatState.isShowingHistory) {
                        ConversationHistoryList(
                            summaries = chatState.conversationSummaries,
                            onOpenConversation = chat::openConversation,
                            onStartNew = {
                                chat.startNewConversation()
                                chat.hideHistory()
                            },
                            onRename = chat::renameConversation,
                            onRegenerateTitle = chat::regenerateConversationTitle,
                            onDelete = chat::deleteConversation,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        ChatScreen(
                            modifier = Modifier.weight(1f), viewModel = chat,
                            keywordHighlightingEnabled = settings.keywordHighlightingEnabled,
                            authViewModel = auth,
                            onContextActionsChanged = { chatActions = it },
                            onOpenContextActions = { chatActionsOpen = true },
                            onNavigateToSection = { section ->
                                when (section) {
                                    AppSection.Hermes -> Unit
                                    AppSection.History -> chat.showHistory()
                                    else -> settingsOpen = true
                                }
                            },
                            onApplyProvider = { provider ->
                                ProviderPresets.find(provider)?.let { preset ->
                                    store.update { it.copy(provider = preset.id, baseUrl = preset.baseUrl, model = preset.modelHint) }
                                    settings = store.load()
                                    true
                                } ?: false
                            },
                            onApplyModel = { model ->
                                store.update { it.copy(model = model) }; settings = store.load(); true
                            },
                        )
                    }
                }
            }
            if (chatActionsOpen && chatActions.isNotEmpty()) {
                ContextActionSheet(
                    section = AppSection.Hermes,
                    actions = chatActions,
                    onDismiss = { chatActionsOpen = false },
                )
            }
        }
    }
}
