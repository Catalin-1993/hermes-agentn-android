package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatSession
import com.example.data.model.UserSettings
import com.example.data.model.WorkspaceProfile
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDrawer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LocalHermesTheme
import com.example.ui.theme.hermesSettingsBackground
import com.example.utils.LocalAppStrings
import java.util.Locale

@Composable
private fun DashedCircleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFEAB308)
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.5f, 3.5f), 0f)
            )
        )
    }
}

@Composable
private fun TaskTopicIcon(
    title: String,
    modifier: Modifier = Modifier
) {
    val lower = remember(title) { title.lowercase(Locale.ROOT) }

    when {
        // Gold/Amber Dashed Ring for GitHub, Supabase, OpenRouter, Manus, Credit, API, Integration, Connector, Webhook
        lower.contains("github") || lower.contains("supabase") || lower.contains("openrouter") ||
                lower.contains("manus") || lower.contains("suprabase") || lower.contains("credit") ||
                lower.contains("api") || lower.contains("connector") || lower.contains("webhook") ||
                lower.contains("integr") -> {
            DashedCircleIcon(modifier = modifier, color = Color(0xFFEAB308))
        }

        // Cloud / Cloud Queue / Server / Testing
        lower.contains("cloud") || lower.contains("server") || lower.contains("testing") ||
                lower.contains("host") || lower.contains("deploy") -> {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }

        // Document with code/gear / App Creation / Config / Installation
        lower.contains("cum să") || lower.contains("creez") || lower.contains("aplicați") ||
                lower.contains("app") || lower.contains("code") || lower.contains("testez") ||
                lower.contains("build") || lower.contains("dev") -> {
            Icon(
                imageVector = Icons.Default.IntegrationInstructions,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }

        // Document Search / Resolve Conflict / Fix / Issue / Search / File
        lower.contains("rezolv") || lower.contains("conflict") || lower.contains("instal") ||
                lower.contains("document") || lower.contains("pdf") || lower.contains("fix") ||
                lower.contains("eroar") || lower.contains("problem") -> {
            Icon(
                imageVector = Icons.Default.FindInPage,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }

        // People / Candidates / Meeting / HR / Team
        lower.contains("întâlnire") || lower.contains("candidat") || lower.contains("echip") ||
                lower.contains("recrut") || lower.contains("people") || lower.contains("meeting") ||
                lower.contains("interviu") -> {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }

        // VPN / Language / Pi-hole / Network / Translate
        lower.contains("vpn") || lower.contains("pi-hole") || lower.contains("translat") ||
                lower.contains("limb") || lower.contains("securit") || lower.contains("rețeal") -> {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }

        // Finance / Portfolio / Money
        lower.contains("finan") || lower.contains("bani") || lower.contains("buget") ||
                lower.contains("invest") || lower.contains("tranzac") -> {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }

        // Default fallback icon
        else -> {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = TextSecondary,
                modifier = modifier.size(16.dp)
            )
        }
    }
}

enum class TaskSortOrder {
    RECENT,
    ALPHABETICAL,
    PINNED
}

@Composable
fun NavDrawerContent(
    sessions: List<ChatSession>,
    currentSessionId: String?,
    settings: UserSettings,
    selectedFolder: String,
    workspaces: List<WorkspaceProfile> = emptyList(),
    activeWorkspaceId: String = "ws_main",
    onSelectWorkspace: (String) -> Unit = {},
    onManageWorkspaces: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenWorkspaceOverview: ((String) -> Unit)? = null,
    onSelectFolder: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: (() -> Unit)? = null,
    /** The picture in the profile row, drawn by the app so it is the avatar the person already has. */
    profileAvatar: @Composable () -> Unit,
) {
    val strings = LocalAppStrings.current
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isChatListExpanded by remember { mutableStateOf(false) }
    var isLibraryExpanded by remember { mutableStateOf(false) }

    var taskSortOrder by remember { mutableStateOf(TaskSortOrder.RECENT) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Only show sessions where an actual conversation took place
    val conversationSessions = remember(sessions) {
        sessions.filter { it.title.isNotBlank() }
    }

    val filteredSessions = remember(conversationSessions, searchQuery, taskSortOrder) {
        val list = if (searchQuery.isBlank()) {
            conversationSessions
        } else {
            conversationSessions.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        when (taskSortOrder) {
            TaskSortOrder.RECENT -> list.sortedByDescending { it.updatedAt }
            TaskSortOrder.ALPHABETICAL -> list.sortedBy { it.title.lowercase(Locale.ROOT) }
            TaskSortOrder.PINNED -> list.sortedByDescending { it.pinned }
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .hermesSettingsBackground()
            .testTag("nav_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Top title "Hermes" (Spacious & Prominent) + Search button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.5.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = TextPrimary
                )

                IconButton(
                    onClick = { isSearchOpen = !isSearchOpen },
                    modifier = Modifier.size(48.dp).testTag("drawer_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = strings.navSearchPlaceholder,
                        tint = TextPrimary,
                        modifier = Modifier.size(27.5.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isSearchOpen) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.navSearchConversations, color = TextMuted, fontSize = 16.5.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("drawer_search_input")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Content Area for Drawer items (No green background highlights)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Section 1: Space (opens Workspace Hub / Agents page directly)
                item {
                    val isSpaceActive = selectedFolder == "Spațiu"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Transparent)
                            .clickable {
                                onSelectFolder("Spațiu")
                                onManageWorkspaces()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("nav_item_workspaces"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Workspaces,
                            contentDescription = strings.navSpace,
                            tint = TextSecondary,
                            modifier = Modifier.size(25.5.dp)
                        )
                        Text(
                            text = strings.navSpace,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.6.sp,
                                fontWeight = if (isSpaceActive) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = TextPrimary
                        )
                    }
                }

                // Section 2: Calendar (between Projects and Library)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isCalendarActive = selectedFolder == "Calendar"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Transparent)
                            .clickable {
                                onSelectFolder("Calendar")
                                onOpenCalendar()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("nav_item_calendar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = strings.calendar,
                            tint = TextSecondary,
                            modifier = Modifier.size(25.5.dp)
                        )
                        Text(
                            text = strings.calendar,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.6.sp,
                                fontWeight = if (isCalendarActive) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = TextPrimary
                        )
                    }
                }

                // Section 4: Library (Bibliotecă, closed by default)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isLibraryActive = selectedFolder == "Bibliotecă"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Transparent)
                            .testTag("nav_item_biblioteca"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left area (Icon + "Library" title): opens Library screen directly
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectFolder("Bibliotecă")
                                    onOpenLibrary()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = strings.navLibrary,
                                tint = TextSecondary,
                                modifier = Modifier.size(25.5.dp)
                            )
                            Text(
                                text = strings.navLibrary,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.6.sp,
                                    fontWeight = if (isLibraryActive) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = TextPrimary
                            )
                        }

                        // Right area: Arrow button with generous touch target to toggle expand/collapse
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isLibraryExpanded = !isLibraryExpanded
                                }
                                .testTag("toggle_expand_library"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLibraryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isLibraryExpanded) strings.navCollapseLibrary else strings.navExpandLibrary,
                                tint = TextMuted,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }

                // Expanded Library items (No green background)
                if (isLibraryExpanded) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, end = 4.dp, top = 2.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .clickable { onOpenLibrary() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.5.dp)
                                )
                                Text(
                                    text = strings.navAllDocuments,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = TextSecondary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Transparent)
                                    .clickable { onOpenLibrary() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(19.5.dp)
                                )
                                Text(
                                    text = strings.navNotesMarkdown,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Section 5: Task / Task-uri (moved to the bottom as requested)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    val isTaskActive = selectedFolder == "Task" || selectedFolder == "Task-uri" || selectedFolder == "Chat"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Transparent)
                            .clickable {
                                onSelectFolder("Task")
                                isChatListExpanded = !isChatListExpanded
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("nav_item_chat"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = strings.navTask,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isTaskActive) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.6.sp
                                ),
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = if (isChatListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = strings.navExpandChat,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { isSortMenuExpanded = !isSortMenuExpanded },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Criterii sortare",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false },
                                modifier = Modifier.background(SurfaceCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Recente", color = TextPrimary) },
                                    onClick = {
                                        taskSortOrder = TaskSortOrder.RECENT
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Alfabetic (A-Z)", color = TextPrimary) },
                                    onClick = {
                                        taskSortOrder = TaskSortOrder.ALPHABETICAL
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fixate (Pinned)", color = TextPrimary) },
                                    onClick = {
                                        taskSortOrder = TaskSortOrder.PINNED
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Expanded Task Sessions (with topic-connected icons and maximum 60% text width constraint)
                if (isChatListExpanded) {
                    if (filteredSessions.isEmpty()) {
                        item {
                            Text(
                                text = "Niciun task anterior",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = TextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, top = 6.dp, bottom = 6.dp)
                            )
                        }
                    } else {
                        itemsIndexed(filteredSessions, key = { index, session -> "${session.id}_$index" }) { index, session ->
                            val isSelected = session.id == currentSessionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1E2621) else Color.Transparent)
                                    .clickable { onSelectSession(session.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subtle Topic Icon (matches topic / screenshot)
                                TaskTopicIcon(
                                    title = session.title,
                                    modifier = Modifier.padding(end = 12.dp)
                                )

                                // Task Title constrained to 60% of available width
                                Text(
                                    text = session.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        letterSpacing = 0.1.sp
                                    ),
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    modifier = Modifier.fillMaxWidth(0.60f)
                                )

                                // Empty space occupying the remainder
                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = strings.delete,
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating + Chat pill button on the right (above user profile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onNewChat,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("drawer_new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.navNewChat,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.navNewChat,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // User Profile Row: 15% larger avatar, name and settings icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag("drawer_profile_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    profileAvatar()

                    Column {
                        Text(
                            text = settings.userName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.5.sp
                            ),
                            color = TextPrimary
                        )
                        if (settings.userRole.isNotBlank()) {
                            Text(
                                text = settings.userRole,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(46.dp).testTag("drawer_settings_icon")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.navSettings,
                        tint = TextSecondary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

