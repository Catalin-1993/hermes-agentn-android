package com.mobilefork.hermesagent.ui.brain

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatSession
import com.example.data.model.UserSettings
import com.example.data.model.WorkspaceProfile
import com.example.data.model.WorkspaceTaskStatus
import com.example.ui.components.NavDrawerContent
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.WorkspaceHubScreen
import com.example.ui.screens.WorkspaceViewMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.hermesPageBackground
import com.example.utils.LocalAppStrings
import com.example.utils.getAppStrings
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentStore
import com.mobilefork.hermesagent.data.AppSettings
import com.mobilefork.hermesagent.data.OnboardingStore
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.chat.ChatConversationSummary
import com.mobilefork.hermesagent.ui.chat.ChatViewModel
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

/** The pages the side menu opens, from the Brain project. */
enum class BrainPage { Projects, Calendar, Library }

/** The Brain pages are drawn in their own look (light or dark to match the app) and their own words. */
@Composable
internal fun BrainTheme(languageTag: String, content: @Composable () -> Unit) {
    val light = LocalHermesGlassTokens.current.lightCanvas
    val strings = remember(languageTag) { getAppStrings(if (languageTag.startsWith("ro")) "ro" else "en") }
    CompositionLocalProvider(LocalAppStrings provides strings) {
        MyApplicationTheme(appTheme = if (light) "Light" else "ChatGPT Dark", content = content)
    }
}

/** The chats the side menu lists, in the shape the Brain menu draws them. Newest first, as the app keeps them. */
internal fun conversationsAsSessions(summaries: List<ChatConversationSummary>): List<ChatSession> {
    val now = System.currentTimeMillis()
    return summaries.mapIndexed { index, summary ->
        // The app keeps the list newest first and shows its time as text, so order stands in for time.
        val at = now - index * 60_000L
        ChatSession(
            id = summary.id,
            title = summary.title.ifBlank { summary.preview },
            createdAt = at,
            lastModified = at,
            updatedAt = at,
            messageCount = summary.messageCount,
            folderName = "Chat",
            workspaceId = projectOfAgent(summary.agentId),
        )
    }
}

/** A project's agent has the project's id, so a chat with an agent belongs to that project. */
internal fun projectOfAgent(agentId: String): String =
    if (agentId == AgentProfile.MAIN_ID) BrainViewModel.MAIN_WORKSPACE_ID else agentId

/**
 * The agent that is this project in chat. The core project is the main agent; any other project
 * becomes an agent with the same id, name, role and instructions the first time it is chatted with.
 * Null when its name is already taken by an agent the person made: they keep that one.
 */
internal fun agentForProject(context: Context, workspace: WorkspaceProfile): String? {
    if (workspace.id == BrainViewModel.MAIN_WORKSPACE_ID) return AgentProfile.MAIN_ID
    val store = AgentStore(context)
    if (store.list().any { it.id == workspace.id }) return workspace.id
    return runCatching {
        store.save(
            AgentProfile(
                id = workspace.id,
                name = workspace.name,
                title = workspace.agentRole,
                description = workspace.purpose,
                persona = workspace.customInstructions,
                toolsEnabled = true,
            ),
        ).id
    }.getOrNull()
}

/** The side menu: every entry from the Brain project except Portfolio, with this app's own avatar. */
@Composable
internal fun BrainDrawer(
    languageTag: String,
    chatViewModel: ChatViewModel,
    brainViewModel: BrainViewModel,
    onOpenPage: (BrainPage, projectId: String?) -> Unit,
    onOpenConversation: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chat by chatViewModel.uiState.collectAsState()
    val workspaces by brainViewModel.workspaces.collectAsState()
    val activeWorkspaceId by brainViewModel.activeWorkspaceId.collectAsState()
    var selectedFolder by rememberSaveable { mutableStateOf("Chat") }
    val sessions = remember(chat.conversationSummaries) { conversationsAsSessions(chat.conversationSummaries) }
    val answers = remember { OnboardingStore(context).answers() }
    val settings = remember(answers.userName) {
        UserSettings().let { base -> if (answers.userName.isBlank()) base else base.copy(userName = answers.userName) }
    }
    BrainTheme(languageTag) {
        NavDrawerContent(
            sessions = sessions,
            currentSessionId = chat.activeConversationId,
            settings = settings,
            selectedFolder = selectedFolder,
            workspaces = workspaces,
            activeWorkspaceId = activeWorkspaceId,
            onSelectWorkspace = brainViewModel::selectWorkspace,
            onManageWorkspaces = { onOpenPage(BrainPage.Projects, null) },
            onOpenLibrary = { onOpenPage(BrainPage.Library, null) },
            onOpenCalendar = { onOpenPage(BrainPage.Calendar, null) },
            onOpenWorkspaceOverview = { id -> onOpenPage(BrainPage.Projects, id) },
            onSelectFolder = { selectedFolder = it },
            onSelectSession = onOpenConversation,
            onNewChat = onNewChat,
            onDeleteSession = chatViewModel::deleteConversation,
            onOpenSettings = onOpenSettings,
            onClose = onClose,
            profileAvatar = {
                // The avatar the person already has: the one they chose for themselves, else the agent's.
                val own = answers.userAvatar
                val shown = if (own.isBlank()) AgentStore(context).main()
                else AgentProfile(id = "you", name = answers.userName.ifBlank { "?" }, avatarPath = own)
                AgentAvatar(agent = shown, size = 50.dp)
            },
        )
    }
}

/** A page opened from the side menu, over the app's pages and under its tab bar. */
@Composable
internal fun BrainPageHost(
    page: BrainPage,
    projectId: String?,
    languageTag: String,
    chatViewModel: ChatViewModel,
    brainViewModel: BrainViewModel,
    onOpenDrawer: () -> Unit,
    onClose: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettingsTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chat by chatViewModel.uiState.collectAsState()
    BrainTheme(languageTag) {
        Box(modifier = modifier.fillMaxSize().hermesPageBackground()) {
            when (page) {
                BrainPage.Calendar -> {
                    val events by brainViewModel.calendarEvents.collectAsState()
                    CalendarScreen(
                        eventsList = events,
                        onAddEvent = brainViewModel::addCalendarEvent,
                        onUpdateEvent = brainViewModel::updateCalendarEvent,
                        onDeleteEvent = brainViewModel::deleteCalendarEvent,
                        onOpenDrawer = onOpenDrawer,
                        onBack = onClose,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                BrainPage.Library -> {
                    val items by brainViewModel.libraryItems.collectAsState()
                    LibraryScreen(
                        libraryItems = items,
                        onAddLibraryItem = brainViewModel::addLibraryItem,
                        onUpdateLibraryItem = brainViewModel::updateLibraryItem,
                        onDeleteLibraryItem = brainViewModel::deleteLibraryItem,
                        // Asking the agent about a note continues in the app's own chat.
                        onUsePromptInChat = { prompt ->
                            chatViewModel.updateInput(prompt)
                            onOpenChat()
                        },
                        onNavigateToChat = onOpenChat,
                        onOpenDrawer = onOpenDrawer,
                        onSendMessage = { prompt ->
                            onOpenChat()
                            (prompt ?: chat.input).takeIf { it.isNotBlank() }?.let(chatViewModel::sendQuickPrompt)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                BrainPage.Projects -> {
                    val workspaces by brainViewModel.workspaces.collectAsState()
                    val activeId by brainViewModel.activeWorkspaceId.collectAsState()
                    val tasks by brainViewModel.tasks.collectAsState()
                    val messages by brainViewModel.interAgentMessages.collectAsState()
                    val sessions = remember(chat.conversationSummaries) { conversationsAsSessions(chat.conversationSummaries) }
                    val files = remember(workspaces) { brainViewModel.attachedFiles(workspaces) }

                    fun chatWith(workspaceId: String, prompt: String?) {
                        val workspace = workspaces.firstOrNull { it.id == workspaceId }
                        val agentId = workspace?.let { agentForProject(context, it) } ?: AgentProfile.MAIN_ID
                        brainViewModel.selectWorkspace(workspaceId)
                        chatViewModel.openAgentChat(agentId)
                        onOpenChat()
                        prompt?.takeIf { it.isNotBlank() }?.let(chatViewModel::sendQuickPrompt)
                    }

                    WorkspaceHubScreen(
                        workspaces = workspaces,
                        activeWorkspaceId = activeId,
                        tasks = tasks,
                        interAgentMessages = messages,
                        workspaceFiles = files,
                        sessions = sessions,
                        initialSelectedWorkspaceId = projectId,
                        initialViewMode = if (projectId != null) WorkspaceViewMode.OVERVIEW else WorkspaceViewMode.LIST,
                        onSelectWorkspace = brainViewModel::selectWorkspace,
                        onSaveWorkspace = brainViewModel::saveWorkspace,
                        onDeleteWorkspace = brainViewModel::deleteWorkspace,
                        onStartChatInWorkspace = ::chatWith,
                        onStartVoiceInWorkspace = { id -> chatWith(id, null) },
                        onOpenModelSkills = onOpenSettingsTools,
                        onSelectSession = { id ->
                            chatViewModel.openConversation(id)
                            onOpenChat()
                        },
                        onCreateTask = brainViewModel::createTask,
                        onUpdateTaskStatus = { id, status, summary, _ -> brainViewModel.updateTaskStatus(id, status, summary) },
                        onDeleteTask = brainViewModel::deleteTask,
                        // Running a task or a workflow asks the project's agent in chat, which is where it works.
                        onRunTaskAutonomously = { task, workspace ->
                            brainViewModel.updateTaskStatus(task.id, WorkspaceTaskStatus.IN_PROGRESS, "")
                            chatWith(workspace.id, taskPrompt(task.title, task.instructions))
                        },
                        onRunSwarmWorkflow = { workspace, prompt -> chatWith(workspace.id, prompt) },
                        onBack = onClose,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private fun taskPrompt(title: String, instructions: String): String =
    listOf(title.trim(), instructions.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
