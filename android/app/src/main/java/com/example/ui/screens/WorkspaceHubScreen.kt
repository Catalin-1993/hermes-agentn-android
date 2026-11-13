package com.example.ui.screens

import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHigh
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.components.dock.dockBottomGradientFade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Close
import com.example.ui.components.AppleGlassBackButton
import com.example.ui.components.ChatBottomBar
import com.example.ui.components.avatar.AvatarProfile
import com.example.ui.components.avatar.StandaloneWorkspaceTopHeaderAvatar
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatSession
import com.example.data.model.InterAgentMessageEntity
import com.example.data.model.KnowledgeItem
import com.example.data.model.WorkspaceMemoryScope
import com.example.data.model.WorkspaceProfile
import com.example.data.model.WorkspaceTaskEntity
import com.example.data.model.WorkspaceTaskStatus
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LocalHermesTheme
import com.example.ui.theme.hermesPageBackground
import com.example.utils.LocalAppStrings
import com.example.utils.WorkspaceDisplayUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush


// Adaptive aesthetic matching professional standards (Muse Light & Dark Hermes)
private val ScreenBackground: Color
    @Composable
    get() = if (LocalHermesTheme.current.isLight) LocalHermesTheme.current.backgroundCanvas else SurfaceCardHigh

private val CardBackground: Color
    @Composable
    get() = if (LocalHermesTheme.current.isLight) LocalHermesTheme.current.surfaceCard else SurfaceCardHigh

private val CardBorderColor: Color
    @Composable
    get() = if (LocalHermesTheme.current.isLight) LocalHermesTheme.current.borderSubtle else BorderSubtle

private val InputBackground: Color
    @Composable
    get() = if (LocalHermesTheme.current.isLight) LocalHermesTheme.current.surfaceInput else SurfaceCardHigh

private val HighlightSurface: Color
    @Composable
    get() = if (LocalHermesTheme.current.isLight) LocalHermesTheme.current.surfaceCardHigh else EmeraldPrimary.copy(alpha = 0.18f)

private data class AvailableToolDef(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val tag: String
)

private val ALL_AVAILABLE_TOOLS = listOf(
    AvailableToolDef("Canva Studio", "Canva Studio / Canvas UI", "Generate interfaces, artefacts, visual canvas and diagrams", Icons.Default.Palette, "CANVAS"),
    AvailableToolDef("Terminal Shell", "Terminal Shell / CLI", "Run bash and termux commands, builds and debugging", Icons.Default.Terminal, "EXEC"),
    AvailableToolDef("Web Search", "Web Search & Scrape", "Live web search and page parsing", Icons.Default.Language, "WEB"),
    AvailableToolDef("File Manager", "Sandbox File Manager", "Read, write and manage the project's local files", Icons.Default.Folder, "FS"),
    AvailableToolDef("Python", "Python 3 Interpreter", "Run Python scripts, data analysis and calculations", Icons.Default.Code, "PYTHON"),
    AvailableToolDef("MCP Connectors", "Model Context Protocol", "Interogare conectori externi (Supabase, GitHub, Notion)", Icons.Default.Hub, "MCP"),
    AvailableToolDef("Git Repositories", "Git & Versiuni Oficiale", "Updates from the NousResearch repository and Git branches", Icons.Default.Public, "GIT")
)

enum class WorkspaceViewMode {
    LIST,
    OVERVIEW,
    INSTRUCTIONS,
    FILES,
    TOOLS,
    TASKS,
    CREATE
}

/**
 * 3D-effect silver metallic folder icon matching user reference screenshot.
 */
@Composable
fun WorkspaceSilverFolderIcon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 46.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        TextSecondary,
                        TextMuted,
                        SurfaceCardHigh
                    )
                )
            )
            .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(TextPrimary, BorderSubtle))),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = Color(0xFFF3F7F5),
            modifier = Modifier.size(size * 0.62f)
        )
    }
}

/**
 * Clean, high-fidelity Workspace Overview page matching the reference screenshot.
 * Displays the silver folder icon, workspace name in serif typography, instructions card,
 * files card, active tasks list, recent activity, and chat bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceOverviewContent(
    workspace: WorkspaceProfile,
    tasks: List<WorkspaceTaskEntity>,
    sessions: List<ChatSession>,
    workspaceFiles: List<java.io.File>,
    enableFloatingDock: Boolean = true,
    onOpenInstructions: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenTasks: () -> Unit,
    onCreateTask: () -> Unit,
    onRunTaskAutonomously: (task: WorkspaceTaskEntity, workspace: WorkspaceProfile) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenVoice: () -> Unit = {},
    onOpenAttachments: () -> Unit = {},
    onOpenModelSkills: () -> Unit = {},
    onSelectSession: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var chatInputText by remember { mutableStateOf("") }

    val imeBottomDp: Dp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBarsBottomDp: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockRestingOffset: Dp = if (enableFloatingDock) navBarsBottomDp + 78.dp else navBarsBottomDp + 8.dp
    val animatedInputOffset: Dp = maxOf(imeBottomDp, dockRestingOffset)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .dockBottomGradientFade(fadeHeight = 185.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = animatedInputOffset + 76.dp)
        ) {
            // Name (matching user request: text remains here below the top bar, avatar moved to top bar)
            Text(
                text = workspace.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = TextPrimary,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )

            // Subtitle / Purpose description
            val descriptionText = if (workspace.purpose.isNotBlank() && workspace.purpose != "Specialised work and tasks") {
                workspace.purpose
            } else {
                "No description provided."
            }
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = TextSecondary
                ),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // The Three Cards (Instructions, Files, Tools) - compact to fit on one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Instructions Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenInstructions() }
                        .testTag("card_instructions")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Instructions",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Instructions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (workspace.customInstructions.isNotBlank()) "Configurat" else "Add",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Files Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenFiles() }
                        .testTag("card_files")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Files",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val totalFiles = workspace.knowledgeItems.size + workspaceFiles.size
                        Text(
                            text = "$totalFiles files",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tools Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenTools() }
                        .testTag("card_tools")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Tools",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val activeToolsCount = workspace.activeTools.size
                        Text(
                            text = "$activeToolsCount active",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ==================== TASK SECTION (above Recent Activity) ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Task",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenTasks() }
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                )

                IconButton(
                    onClick = onCreateTask,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_add_task_overview")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add task",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val activeTasks = tasks.filter { it.status != WorkspaceTaskStatus.COMPLETED }
            if (activeTasks.isEmpty()) {
                Text(
                    text = "Niciun task activ",
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeTasks.forEach { task ->
                        val (statusColor, statusText) = when (task.status) {
                            WorkspaceTaskStatus.PENDING -> Color(0xFFEAB308) to "Pending"
                            WorkspaceTaskStatus.IN_PROGRESS -> Color(0xFF3B82F6) to "În lucru"
                            WorkspaceTaskStatus.RUNNING -> Color(0xFF3B82F6) to "În rulare"
                            WorkspaceTaskStatus.COMPLETED -> EmeraldAccent to "Finalizat"
                            WorkspaceTaskStatus.FAILED -> Color(0xFFEF4444) to "Failed"
                            else -> Color(0xFFEAB308) to "Pending"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardBackground,
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTasks() }
                                .testTag("active_task_${task.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            statusText,
                                            fontSize = 10.sp,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = task.title,
                                            fontSize = 13.5.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (task.instructions.isNotBlank()) {
                                            Text(
                                                text = task.instructions,
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (task.status == WorkspaceTaskStatus.PENDING || task.status == WorkspaceTaskStatus.FAILED) {
                                    IconButton(
                                        onClick = { onRunTaskAutonomously(task, workspace) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Run",
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else if (task.status == WorkspaceTaskStatus.IN_PROGRESS) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== RECENT ACTIVITY SECTION ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent activity",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "${sessions.size} chat${if (sessions.size == 1) "" else "s"}",
                        fontSize = 12.sp,
                        color = EmeraldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                val isDraft = workspace.id.startsWith("ws_draft_") || workspace.customInstructions.isBlank() || workspace.name == "New Agent"
                if (isDraft) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.09f)),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("draft_workspace_guided_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("✨", fontSize = 16.sp)
                                Text(
                                    text = "Set up the agent by chatting",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                            Text(
                                text = "Write to the agent in the chat bar below. Say what role it should have, which tools to use (Web Search, Terminal, Python, etc.) or the instructions you want. The agent will use workspace_tool to set itself up.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Idei rapide:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            val suggestions = listOf(
                                "Set yourself up as a Senior Android Architect with Terminal and File Manager",
                                "You are a research and documents analyst. Turn on Web Search",
                                "I want a business and financial strategy assistant"
                            )
                            suggestions.forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CardBackground,
                                    border = BorderStroke(1.dp, CardBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            chatInputText = suggestion
                                        }
                                        .testTag("draft_suggestion_${suggestion.take(10)}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            fontSize = 11.5.sp,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No activity yet. Start a chat below.",
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessions.take(10).forEach { session ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardBackground,
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSession(session.id) }
                                .testTag("recent_session_${session.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.13f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = session.title,
                                            fontSize = 13.5.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val formattedTime = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(session.updatedAt))
                                        val modelLabel = "Llama-3.2"
                                        Text(
                                            text = "$formattedTime · $modelLabel",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Deschide",
                                    tint = TextMuted.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dedicated Bottom Chat Input Bar - stays above floating dock and rides seamlessly with keyboard
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = animatedInputOffset)
        ) {
            ChatBottomBar(
                inputText = chatInputText,
                onInputTextChange = { chatInputText = it },
                onSendMessage = { prompt ->
                    chatInputText = ""
                    onSendMessage(prompt)
                },
                placeholderText = "Type to ask...",
                onOpenAttachments = onOpenAttachments,
                onOpenModelSkills = onOpenModelSkills,
                onOpenVoice = onOpenVoice
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceHubScreen(
    workspaces: List<WorkspaceProfile>,
    activeWorkspaceId: String,
    tasks: List<WorkspaceTaskEntity> = emptyList(),
    interAgentMessages: List<InterAgentMessageEntity> = emptyList(),
    workspaceFiles: List<java.io.File> = emptyList(),
    sessions: List<ChatSession> = emptyList(),
    initialSelectedWorkspaceId: String? = null,
    initialViewMode: WorkspaceViewMode = WorkspaceViewMode.LIST,
    enableFloatingDock: Boolean = true,
    onSelectWorkspace: (String) -> Unit,
    onSaveWorkspace: (WorkspaceProfile) -> Unit,
    onDeleteWorkspace: (String) -> Unit,
    onStartChatInWorkspace: (workspaceId: String, initialPrompt: String?) -> Unit,
    onStartVoiceInWorkspace: (workspaceId: String) -> Unit = {},
    onOpenAttachments: () -> Unit = {},
    onOpenModelSkills: () -> Unit = {},
    onSelectSession: (String) -> Unit = {},
    onCreateTask: (workspaceId: String, title: String, instructions: String, role: String) -> Unit = { _, _, _, _ -> },
    onUpdateTaskStatus: (taskId: Long, status: WorkspaceTaskStatus, summary: String, workspaceId: String) -> Unit = { _, _, _, _ -> },
    onDeleteTask: (taskId: String) -> Unit = {},
    onRunTaskAutonomously: (task: WorkspaceTaskEntity, workspace: WorkspaceProfile) -> Unit = { _, _ -> },
    onRunSwarmWorkflow: (workspace: WorkspaceProfile, prompt: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewMode by remember(initialViewMode) { mutableStateOf(initialViewMode) }
    var selectedWorkspace by remember(initialSelectedWorkspaceId) {
        mutableStateOf(
            if (initialSelectedWorkspaceId != null) {
                workspaces.find { it.id == initialSelectedWorkspaceId }
            } else null
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Editor Form State (Used in EDITOR & CREATE)
    // 0: General, 1: Unelte, 2: Memory, 3: Task-uri, 4: Swarmer
    var editorTab by remember { mutableIntStateOf(0) }
    var formName by remember { mutableStateOf("") }
    var formEmoji by remember { mutableStateOf("⚡") }
    var formAvatarUri by remember { mutableStateOf<String?>(null) }
    var formRole by remember { mutableStateOf("") }
    var formPurpose by remember { mutableStateOf("") }
    var formInstructions by remember { mutableStateOf("") }
    var formExtraMemoryInstructions by remember { mutableStateOf("") }
    var formPreferredModel by remember { mutableStateOf("Hermes 3 (Nous Research)") }
    var formTags by remember { mutableStateOf(listOf("Agent", "Workspace")) }
    var formMemoryScope by remember { mutableStateOf(WorkspaceMemoryScope.WORKSPACE_ONLY) }
    var formKnowledgeList by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }
    var formSwarmEnabled by remember { mutableStateOf(true) }
    var formSwarmRoles by remember { mutableStateOf(listOf("Arhitect", "Executant", "Reviewer")) }
    var formReasoning by remember { mutableStateOf("Echilibrat") }
    var formAutonomy by remember { mutableStateOf("Balanced") }
    var formActiveTools by remember { mutableStateOf(listOf("Terminal Shell", "Web Search", "File Manager", "Python")) }
    var formAllowedIncoming by remember { mutableStateOf(listOf("agent_hermes")) }
    var formAllowedOutgoing by remember { mutableStateOf(listOf<String>()) }
    var formCanBeConsultedByHermes by remember { mutableStateOf(true) }
    var formMaxDelegationDepth by remember { mutableIntStateOf(2) }

    // Sub-form for adding Knowledge Item
    var isAddingKnowledgeInline by remember { mutableStateOf(false) }
    var newKnowledgeTitle by remember { mutableStateOf("") }
    var newKnowledgeContent by remember { mutableStateOf("") }

    // Sub-form for delegating a new Task
    var isCreatingTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskInstructions by remember { mutableStateOf("") }
    var newTaskAssignedRole by remember { mutableStateOf("Executant") }

    // Swarm Workflow Execution Prompt Dialog
    var isRunningSwarmDialog by remember { mutableStateOf(false) }
    var swarmTaskPrompt by remember { mutableStateOf("") }

    fun openWorkspaceOverview(ws: WorkspaceProfile) {
        selectedWorkspace = ws
        formName = ws.name
        formEmoji = ws.emoji
        formAvatarUri = ws.avatarUri
        formRole = ws.agentRole
        formPurpose = ws.purpose
        formInstructions = ws.customInstructions
        formExtraMemoryInstructions = ws.extraMemoryInstructions
        formPreferredModel = ws.preferredModel
        formTags = ws.tags
        formMemoryScope = ws.memoryScope
        formKnowledgeList = ws.knowledgeItems
        formSwarmEnabled = ws.swarmEnabled
        formSwarmRoles = ws.swarmRoles
        formReasoning = ws.reasoningEffort
        formAutonomy = ws.autonomyLevel
        formActiveTools = ws.activeTools
        formAllowedIncoming = ws.allowedIncomingCallers
        formAllowedOutgoing = ws.allowedOutgoingTargets
        formCanBeConsultedByHermes = ws.canBeConsultedByHermes
        formMaxDelegationDepth = ws.maxDelegationDepth
        isAddingKnowledgeInline = false
        viewMode = WorkspaceViewMode.OVERVIEW
    }

    fun openWorkspaceInstructions(ws: WorkspaceProfile) {
        openWorkspaceOverview(ws)
        viewMode = WorkspaceViewMode.INSTRUCTIONS
    }

    fun openWorkspaceFiles(ws: WorkspaceProfile) {
        openWorkspaceOverview(ws)
        viewMode = WorkspaceViewMode.FILES
    }

    fun openWorkspaceTools(ws: WorkspaceProfile) {
        openWorkspaceOverview(ws)
        viewMode = WorkspaceViewMode.TOOLS
    }

    fun openWorkspaceTasks(ws: WorkspaceProfile) {
        openWorkspaceOverview(ws)
        viewMode = WorkspaceViewMode.TASKS
    }

    LaunchedEffect(initialSelectedWorkspaceId, initialViewMode, workspaces) {
        val ws = if (initialSelectedWorkspaceId != null) {
            workspaces.find { it.id == initialSelectedWorkspaceId } ?: workspaces.firstOrNull()
        } else if (initialViewMode != WorkspaceViewMode.LIST) {
            workspaces.find { it.id == activeWorkspaceId } ?: workspaces.firstOrNull()
        } else null

        if (ws != null) {
            when (initialViewMode) {
                WorkspaceViewMode.FILES -> openWorkspaceFiles(ws)
                WorkspaceViewMode.TOOLS -> openWorkspaceTools(ws)
                WorkspaceViewMode.INSTRUCTIONS -> openWorkspaceInstructions(ws)
                WorkspaceViewMode.TASKS -> openWorkspaceTasks(ws)
                WorkspaceViewMode.OVERVIEW -> openWorkspaceOverview(ws)
                else -> {
                    openWorkspaceOverview(ws)
                    viewMode = initialViewMode
                }
            }
        }
    }

    fun startCreate() {
        val draftId = "ws_draft_${UUID.randomUUID().toString().take(8)}"
        val draftWorkspace = WorkspaceProfile(
            id = draftId,
            name = "New Agent",
            emoji = "✨",
            agentRole = "Setup specialist",
            purpose = "Set up this agent by chatting, or choose its role, tools and files.",
            customInstructions = "You are a new agent being set up. Greet the user, ask what role they want you to have, which tools to enable and which files or knowledge to configure, and call workspace_tool to save yourself automatically.",
            memoryScope = WorkspaceMemoryScope.WORKSPACE_ONLY,
            knowledgeItems = emptyList(),
            swarmEnabled = true,
            swarmRoles = listOf("Specialist"),
            reasoningEffort = "Echilibrat",
            autonomyLevel = "Balanced",
            activeTools = listOf("File Manager", "Web Search"),
            extraMemoryInstructions = "",
            attachedFiles = emptyList(),
            tags = listOf("Draft", "Agent Nou"),
            allowedIncomingCallers = listOf("agent_hermes", "user", "ws_main"),
            allowedOutgoingTargets = listOf("agent_hermes"),
            canBeConsultedByHermes = true,
            maxDelegationDepth = 2,
            isEnabled = true
        )
        onSaveWorkspace(draftWorkspace)
        onSelectWorkspace(draftWorkspace.id)
        openWorkspaceOverview(draftWorkspace)
    }

    fun saveCurrentWorkspace(): WorkspaceProfile? {
        if (formName.isBlank()) {
            Toast.makeText(context, "Enter a name for the project", Toast.LENGTH_SHORT).show()
            return null
        }
        val targetId = selectedWorkspace?.id ?: "ws_${UUID.randomUUID().toString().take(8)}"
        val updated = WorkspaceProfile(
            id = targetId,
            name = formName.trim(),
            emoji = formEmoji.ifBlank { "⚡" },
            avatarUri = formAvatarUri,
            agentRole = formRole.ifBlank { "General Assistant" },
            purpose = formPurpose.ifBlank { "Specialised work and tasks" },
            customInstructions = formInstructions.trim(),
            extraMemoryInstructions = formExtraMemoryInstructions.trim(),
            preferredModel = formPreferredModel,
            tags = formTags,
            memoryScope = formMemoryScope,
            knowledgeItems = formKnowledgeList,
            swarmEnabled = formSwarmEnabled,
            swarmRoles = formSwarmRoles,
            reasoningEffort = formReasoning,
            autonomyLevel = formAutonomy,
            activeTools = formActiveTools,
            allowedIncomingCallers = formAllowedIncoming,
            allowedOutgoingTargets = formAllowedOutgoing,
            canBeConsultedByHermes = formCanBeConsultedByHermes,
            maxDelegationDepth = formMaxDelegationDepth
        )
        onSaveWorkspace(updated)
        selectedWorkspace = updated
        Toast.makeText(context, "Project '${updated.name}' saved!", Toast.LENGTH_SHORT).show()
        return updated
    }

    val filteredWorkspaces = if (searchQuery.isBlank()) {
        workspaces
    } else {
        workspaces.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.agentRole.contains(searchQuery, ignoreCase = true)
        }
    }

    // Filter tasks for current workspace
    val currentWorkspaceId = selectedWorkspace?.id ?: activeWorkspaceId
    val currentTasks = tasks.filter { it.workspaceId == currentWorkspaceId }
    val currentMessages = interAgentMessages

    BackHandler {
        when {
            showDeleteConfirmDialog -> showDeleteConfirmDialog = false
            isCreatingTaskDialog -> isCreatingTaskDialog = false
            isRunningSwarmDialog -> isRunningSwarmDialog = false
            isAddingKnowledgeInline -> isAddingKnowledgeInline = false
            isSearchExpanded -> {
                isSearchExpanded = false
                searchQuery = ""
            }
            viewMode == WorkspaceViewMode.CREATE -> {
                viewMode = WorkspaceViewMode.LIST
            }
            viewMode == WorkspaceViewMode.INSTRUCTIONS ||
            viewMode == WorkspaceViewMode.FILES ||
            viewMode == WorkspaceViewMode.TOOLS ||
            viewMode == WorkspaceViewMode.TASKS -> {
                viewMode = if (selectedWorkspace != null) WorkspaceViewMode.OVERVIEW else WorkspaceViewMode.LIST
            }
            viewMode == WorkspaceViewMode.OVERVIEW -> {
                viewMode = WorkspaceViewMode.LIST
            }
            viewMode == WorkspaceViewMode.LIST -> {
                onBack()
            }
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .hermesPageBackground()
            .testTag("workspace_hub_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Navigation Bar
            if (viewMode == WorkspaceViewMode.LIST && isSearchExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search projects...", color = TextMuted, fontSize = 13.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        searchQuery = ""
                                    } else {
                                        isSearchExpanded = false
                                    }
                                },
                                modifier = Modifier.size(28.dp).testTag("close_workspace_search_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close search", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldAccent,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workspace_search_input")
                    )
                }
            } else {
                if (viewMode == WorkspaceViewMode.OVERVIEW) {
                    // Top Bar for OVERVIEW: Back on left, Avatar icon in the center, Delete on right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppleGlassBackButton(
                            onClick = { viewMode = WorkspaceViewMode.LIST },
                            testTag = "btn_back_workspace_top"
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (selectedWorkspace != null && selectedWorkspace?.id != "agent_hermes") {
                            Surface(
                                shape = CircleShape,
                                color = LocalHermesTheme.current.headerButtonBg,
                                border = BorderStroke(1.dp, LocalHermesTheme.current.headerButtonBorder),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable { showDeleteConfirmDialog = true }
                                    .testTag("delete_workspace_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Șterge Workspace",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(42.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            if (viewMode != WorkspaceViewMode.LIST) {
                                AppleGlassBackButton(
                                    onClick = {
                                        when (viewMode) {
                                            WorkspaceViewMode.INSTRUCTIONS,
                                            WorkspaceViewMode.FILES,
                                            WorkspaceViewMode.TOOLS,
                                            WorkspaceViewMode.TASKS -> viewMode = WorkspaceViewMode.OVERVIEW
                                            WorkspaceViewMode.CREATE -> viewMode = WorkspaceViewMode.LIST
                                            else -> onBack()
                                        }
                                    },
                                    testTag = "btn_back_workspace_top"
                                )
                            }

                            Column(
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                val titleText = when (viewMode) {
                                    WorkspaceViewMode.LIST -> "Projects"
                                    WorkspaceViewMode.INSTRUCTIONS -> "Instructions"
                                    WorkspaceViewMode.FILES -> "Files and memory"
                                    WorkspaceViewMode.TOOLS -> "Tools"
                                    WorkspaceViewMode.TASKS -> "Tasks"
                                    WorkspaceViewMode.CREATE -> "New project"
                                    else -> ""
                                }
                                if (titleText.isNotBlank()) {
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            letterSpacing = 0.3.sp
                                        ),
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                                when (viewMode) {
                                    WorkspaceViewMode.LIST -> {
                                        Text(
                                            text = "Your agents and projects",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMuted
                                        )
                                    }
                                    WorkspaceViewMode.INSTRUCTIONS -> {
                                        Text(
                                            text = "How the agent works",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMuted
                                        )
                                    }
                                    WorkspaceViewMode.FILES -> {
                                        Text(
                                            text = "What the project keeps",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMuted
                                        )
                                    }
                                    WorkspaceViewMode.TOOLS -> {
                                        Text(
                                            text = "What it can use and who it works with",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMuted
                                        )
                                    }
                                    WorkspaceViewMode.TASKS -> {
                                        Text(
                                            text = "Tasks given to the agent",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMuted
                                        )
                                    }
                                    WorkspaceViewMode.CREATE -> {
                                        Text(
                                            text = "Set up a dedicated workspace",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = TextMuted
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }

                        if (viewMode == WorkspaceViewMode.LIST) {
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("open_workspace_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search projects",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (viewMode) {
                WorkspaceViewMode.LIST -> {
                    // ==================== LIST VIEW ====================
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .dockBottomGradientFade(fadeHeight = 185.dp)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            if (filteredWorkspaces.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "No project found for \"$searchQuery\"" else "No projects yet",
                                            color = TextMuted,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                            }

                            itemsIndexed(filteredWorkspaces, key = { index, ws -> "${ws.id}_$index" }) { index, ws ->
                                val isWsActive = ws.id == activeWorkspaceId
                                val displayInfo = remember(ws.name, ws.agentRole, ws.id) {
                                    WorkspaceDisplayUtils.getDisplayInfo(ws)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            openWorkspaceOverview(ws)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                        .testTag("workspace_item_${ws.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PremiumWorkspaceAvatar(
                                            avatarUri = ws.avatarUri,
                                            emoji = ws.emoji,
                                            name = ws.name,
                                            isActive = isWsActive,
                                            avatarSize = 46.dp,
                                            workspace = ws
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = displayInfo.profession,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = if (isWsActive) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 15.5.sp
                                                ),
                                                color = if (isWsActive) EmeraldAccent else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (displayInfo.customName != null) {
                                                Text(
                                                    text = displayInfo.customName,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.sp,
                                                        color = TextSecondary,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    if (isWsActive) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldAccent)
                                                .testTag("active_dot_${ws.id}")
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom-Right Floating Create Workspace Button (elevated to exact same height as Calendar & Library)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = EmeraldPrimary,
                            contentColor = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(end = 18.dp, bottom = 102.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { startCreate() }
                                .testTag("create_workspace_fab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Workspace Nou",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Project",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                WorkspaceViewMode.OVERVIEW -> {
                    val currentWs = (selectedWorkspace?.let { sel -> workspaces.find { it.id == sel.id } } ?: selectedWorkspace) ?: workspaces.firstOrNull()
                    if (currentWs != null) {
                        val wsFilteredSessions = remember(sessions, currentWs) {
                            sessions.filter { sess ->
                                sess.folderName == currentWs.name ||
                                sess.folderName == currentWs.id ||
                                sess.id == currentWs.id ||
                                (currentWs.id == "ws_main" && (sess.folderName == "Chat" || sess.folderName == "Space" || sess.folderName == "Principal" || sess.folderName == "General"))
                            }.sortedByDescending { it.updatedAt }
                        }

                        WorkspaceOverviewContent(
                            workspace = currentWs,
                            tasks = tasks.filter { it.workspaceId == currentWs.id },
                            sessions = wsFilteredSessions,
                            workspaceFiles = workspaceFiles,
                            enableFloatingDock = enableFloatingDock,
                            onOpenInstructions = {
                                openWorkspaceInstructions(currentWs)
                            },
                            onOpenFiles = {
                                openWorkspaceFiles(currentWs)
                            },
                            onOpenTools = {
                                openWorkspaceTools(currentWs)
                            },
                            onOpenTasks = {
                                openWorkspaceTasks(currentWs)
                            },
                            onCreateTask = {
                                selectedWorkspace = currentWs
                                isCreatingTaskDialog = true
                            },
                            onRunTaskAutonomously = { task, ws ->
                                onRunTaskAutonomously(task, ws)
                            },
                            onSendMessage = { prompt ->
                                onSelectWorkspace(currentWs.id)
                                onStartChatInWorkspace(currentWs.id, prompt)
                            },
                            onOpenVoice = {
                                onSelectWorkspace(currentWs.id)
                                onStartVoiceInWorkspace(currentWs.id)
                            },
                            onOpenAttachments = onOpenAttachments,
                            onOpenModelSkills = onOpenModelSkills,
                            onSelectSession = { sessId ->
                                onSelectWorkspace(currentWs.id)
                                onSelectSession(sessId)
                            }
                        )
                    }
                }

                WorkspaceViewMode.INSTRUCTIONS -> {
                    val currentWs = selectedWorkspace ?: workspaces.firstOrNull()
                    if (currentWs != null) {
                        WorkspaceInstructionsScreenContent(
                            formEmoji = formEmoji,
                            onEmojiChange = { formEmoji = it },
                            formAvatarUri = formAvatarUri,
                            onAvatarUriChange = { formAvatarUri = it },
                            formName = formName,
                            onNameChange = { formName = it },
                            formRole = formRole,
                            onRoleChange = { formRole = it },
                            formPurpose = formPurpose,
                            onPurposeChange = { formPurpose = it },
                            formInstructions = formInstructions,
                            onInstructionsChange = { formInstructions = it },
                            formPreferredModel = formPreferredModel,
                            onPreferredModelChange = { formPreferredModel = it },
                            formReasoning = formReasoning,
                            onReasoningChange = { formReasoning = it },
                            formAutonomy = formAutonomy,
                            onAutonomyChange = { formAutonomy = it },
                            formTags = formTags,
                            onTagsChange = { formTags = it },
                            onBack = { viewMode = WorkspaceViewMode.OVERVIEW },
                            onSave = {
                                val saved = saveCurrentWorkspace()
                                if (saved != null) {
                                    selectedWorkspace = saved
                                    viewMode = WorkspaceViewMode.OVERVIEW
                                }
                            }
                        )
                    }
                }

                WorkspaceViewMode.TOOLS -> {
                    val currentWs = selectedWorkspace ?: workspaces.firstOrNull()
                    if (currentWs != null) {
                        WorkspaceToolsScreenContent(
                            formActiveTools = formActiveTools,
                            onActiveToolsChange = { formActiveTools = it },
                            formSwarmEnabled = formSwarmEnabled,
                            onSwarmEnabledChange = { formSwarmEnabled = it },
                            formSwarmRoles = formSwarmRoles,
                            onSwarmRolesChange = { formSwarmRoles = it },
                            formCanBeConsultedByHermes = formCanBeConsultedByHermes,
                            onCanBeConsultedByHermesChange = { formCanBeConsultedByHermes = it },
                            formMaxDelegationDepth = formMaxDelegationDepth,
                            onMaxDelegationDepthChange = { formMaxDelegationDepth = it },
                            formAllowedIncoming = formAllowedIncoming,
                            onAllowedIncomingChange = { formAllowedIncoming = it },
                            formAllowedOutgoing = formAllowedOutgoing,
                            onAllowedOutgoingChange = { formAllowedOutgoing = it },
                            interAgentMessages = currentMessages,
                            onTriggerSwarmWorkflow = { isRunningSwarmDialog = true },
                            onBack = { viewMode = WorkspaceViewMode.OVERVIEW },
                            onSave = {
                                val saved = saveCurrentWorkspace()
                                if (saved != null) {
                                    selectedWorkspace = saved
                                    viewMode = WorkspaceViewMode.OVERVIEW
                                }
                            }
                        )
                    }
                }

                WorkspaceViewMode.FILES -> {
                    val currentWs = selectedWorkspace ?: workspaces.firstOrNull()
                    if (currentWs != null) {
                        WorkspaceFilesScreenContent(
                            formMemoryScope = formMemoryScope,
                            onMemoryScopeChange = { formMemoryScope = it },
                            formExtraMemoryInstructions = formExtraMemoryInstructions,
                            onExtraMemoryInstructionsChange = { formExtraMemoryInstructions = it },
                            formKnowledgeList = formKnowledgeList,
                            onKnowledgeListChange = { formKnowledgeList = it },
                            workspaceFiles = workspaceFiles,
                            onAddFile = { fileName, content ->
                                try {
                                    val wsDir = java.io.File(context.filesDir, "workspaces/${currentWs.id}")
                                    if (!wsDir.exists()) wsDir.mkdirs()
                                    val newFile = java.io.File(wsDir, fileName)
                                    newFile.writeText(content)
                                    val updatedFiles = currentWs.attachedFiles + newFile.absolutePath
                                    val updatedWs = currentWs.copy(attachedFiles = updatedFiles.distinct())
                                    onSaveWorkspace(updatedWs)
                                    selectedWorkspace = updatedWs
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not save the file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteFile = { file ->
                                try {
                                    if (file.exists()) file.delete()
                                    val updatedFiles = currentWs.attachedFiles.filter { it != file.absolutePath }
                                    val updatedWs = currentWs.copy(attachedFiles = updatedFiles)
                                    onSaveWorkspace(updatedWs)
                                    selectedWorkspace = updatedWs
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not delete the file: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onBack = { viewMode = WorkspaceViewMode.OVERVIEW },
                            onSave = {
                                val saved = saveCurrentWorkspace()
                                if (saved != null) {
                                    selectedWorkspace = saved
                                    viewMode = WorkspaceViewMode.OVERVIEW
                                }
                            }
                        )
                    }
                }

                WorkspaceViewMode.TASKS -> {
                    val currentWs = selectedWorkspace ?: workspaces.firstOrNull()
                    if (currentWs != null) {
                        WorkspaceTasksScreenContent(
                            workspace = currentWs,
                            tasks = tasks.filter { it.workspaceId == currentWs.id },
                            onTriggerCreateTask = {
                                isCreatingTaskDialog = true
                            },
                            onRunTaskAutonomously = { task, ws ->
                                onRunTaskAutonomously(task, ws)
                            },
                            onDeleteTask = { taskId ->
                                onDeleteTask(taskId)
                            },
                            onBack = { viewMode = WorkspaceViewMode.OVERVIEW }
                        )
                    }
                }

                WorkspaceViewMode.CREATE -> {
                    WorkspaceCreateScreenContent(
                        formEmoji = formEmoji,
                        onEmojiChange = { formEmoji = it },
                        formAvatarUri = formAvatarUri,
                        onAvatarUriChange = { formAvatarUri = it },
                        formName = formName,
                        onNameChange = { formName = it },
                        formRole = formRole,
                        onRoleChange = { formRole = it },
                        formPurpose = formPurpose,
                        onPurposeChange = { formPurpose = it },
                        formInstructions = formInstructions,
                        onInstructionsChange = { formInstructions = it },
                        formPreferredModel = formPreferredModel,
                        onPreferredModelChange = { formPreferredModel = it },
                        formReasoning = formReasoning,
                        onReasoningChange = { formReasoning = it },
                        onSelectTemplate = { title, emoji, role, purpose, instructions, model, avatarUri ->
                            formName = title
                            formEmoji = emoji
                            formRole = role
                            formPurpose = purpose
                            formInstructions = instructions
                            formPreferredModel = model
                            formAvatarUri = avatarUri
                        },
                        onBack = { viewMode = WorkspaceViewMode.LIST },
                        onCreate = {
                            val saved = saveCurrentWorkspace()
                            if (saved != null) {
                                selectedWorkspace = saved
                                viewMode = WorkspaceViewMode.OVERVIEW
                            }
                        }
                    )
                }
            }
        }
    }

    // ==================== DIALOG: DELEGATE TASK ====================
    if (isCreatingTaskDialog) {
        val targetWsId = selectedWorkspace?.id ?: activeWorkspaceId
        AlertDialog(
            onDismissRequest = { isCreatingTaskDialog = false },
            title = { Text("Delegate task to the sub-agent", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Give the project's sub-agent a specific task.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task title", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Code architecture review and optimisations", color = TextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderSubtle,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCardHigh,
                            unfocusedContainerColor = SurfaceCardHigh
                        )
                    )

                    OutlinedTextField(
                        value = newTaskInstructions,
                        onValueChange = { newTaskInstructions = it },
                        label = { Text("Detailed instructions", fontSize = 11.sp) },
                        placeholder = { Text("Describe the steps and tools needed...", color = TextMuted, fontSize = 11.sp) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderSubtle,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCardHigh,
                            unfocusedContainerColor = SurfaceCardHigh
                        )
                    )

                    Text("Rol Sub-Agent Alocat:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Arhitect", "Executant", "Reviewer").forEach { role ->
                            val isSel = newTaskAssignedRole == role
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceCardHigh)
                                    .clickable { newTaskAssignedRole = role }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(role, fontSize = 11.sp, color = if (isSel) TextPrimary else TextMuted)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) EmeraldPrimary else SurfaceCardHigh)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onCreateTask(targetWsId, newTaskTitle.trim(), newTaskInstructions.trim(), newTaskAssignedRole)
                            isCreatingTaskDialog = false
                            newTaskTitle = ""
                            newTaskInstructions = ""
                            Toast.makeText(context, "Task delegat cu succes!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delegate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { isCreatingTaskDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceCardHigh,
            shape = RoundedCornerShape(14.dp)
        )
    }

    // ==================== DIALOG: RUN SWARM WORKFLOW ====================
    if (isRunningSwarmDialog) {
        val currentWs = selectedWorkspace ?: workspaces.find { it.id == activeWorkspaceId }
        AlertDialog(
            onDismissRequest = { isRunningSwarmDialog = false },
            title = { Text("Lansare Workflow Swarm Multi-Agent", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Run the sub-agent team (${formSwarmRoles.joinToString(", ")}) in turn to deliver a complete solution.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = swarmTaskPrompt,
                        onValueChange = { swarmTaskPrompt = it },
                        label = { Text("Obiectivul Principal Swarm", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Build a secure sign-in module and review the code", color = TextMuted, fontSize = 11.sp) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderSubtle,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceCardHigh,
                            unfocusedContainerColor = SurfaceCardHigh
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (swarmTaskPrompt.isNotBlank() && currentWs != null) {
                            onRunSwarmWorkflow(currentWs, swarmTaskPrompt.trim())
                            isRunningSwarmDialog = false
                            swarmTaskPrompt = ""
                            Toast.makeText(context, "Swarm workflow started in the background!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Start swarm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { isRunningSwarmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceCardHigh,
            shape = RoundedCornerShape(14.dp)
        )
    }

    // ==================== DIALOG: DELETE WORKSPACE ====================
    if (showDeleteConfirmDialog && selectedWorkspace != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete the project?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete '${selectedWorkspace!!.name}'? This cannot be undone.",
                    color = TextSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = selectedWorkspace!!.id
                        onDeleteWorkspace(idToDelete)
                        showDeleteConfirmDialog = false
                        viewMode = WorkspaceViewMode.LIST
                        Toast.makeText(context, "Project deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Șterge", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceCardHigh,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

/**
 * The mark of a project in a list: a folder in a soft circle. Projects have no pictures; a dot says
 * which one is the active one.
 */
@Composable
fun PremiumWorkspaceAvatar(
    avatarUri: String?,
    emoji: String,
    name: String,
    isActive: Boolean,
    avatarSize: Dp = 56.dp,
    modifier: Modifier = Modifier,
    workspace: WorkspaceProfile? = null
) {
    Box(
        modifier = modifier.size(avatarSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = name,
                tint = EmeraldPrimary,
                modifier = Modifier.size(avatarSize * 0.5f)
            )
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
                    .border(2.dp, SurfaceCard, CircleShape)
            )
        }
    }
}

