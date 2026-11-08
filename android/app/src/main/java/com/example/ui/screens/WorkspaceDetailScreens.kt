package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import com.example.ui.components.dock.dockBottomGradientFade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.mobilefork.hermesagent.R
import com.example.ui.components.UserAvatar
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InterAgentMessageEntity
import com.example.data.model.KnowledgeItem
import com.example.data.model.WorkspaceMemoryScope
import com.example.data.model.WorkspaceProfile
import com.example.data.model.WorkspaceTaskEntity
import com.example.data.model.WorkspaceTaskStatus
import com.example.ui.screens.workspace.WorkspaceFilesScreen
import com.example.ui.screens.workspace.WorkspaceInstructionsScreen
import com.example.ui.screens.workspace.WorkspaceToolsScreen
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color.White else Color(0xFF101613)
private val ItemBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFF1F5F9) else Color(0xFF141B18)
private val InputBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFF8FAFC) else Color(0xFF0C110E)
private val SubtleBorderColor: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0x1A000000) else Color(0x18FFFFFF)

// ==========================================
// 1. ECRAN INSTRUCȚIUNI & SETĂRI AGENT
// ==========================================
@Composable
fun WorkspaceInstructionsScreenContent(
    formEmoji: String,
    onEmojiChange: (String) -> Unit,
    formAvatarUri: String?,
    onAvatarUriChange: (String?) -> Unit,
    formName: String,
    onNameChange: (String) -> Unit,
    formRole: String,
    onRoleChange: (String) -> Unit,
    formPurpose: String,
    onPurposeChange: (String) -> Unit,
    formInstructions: String,
    onInstructionsChange: (String) -> Unit,
    formPreferredModel: String,
    onPreferredModelChange: (String) -> Unit,
    formReasoning: String,
    onReasoningChange: (String) -> Unit,
    formAutonomy: String,
    onAutonomyChange: (String) -> Unit,
    formTags: List<String> = listOf("Agent", "Workspace"),
    onTagsChange: (List<String>) -> Unit = {},
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    WorkspaceInstructionsScreen(
        formEmoji = formEmoji,
        onEmojiChange = onEmojiChange,
        formAvatarUri = formAvatarUri,
        onAvatarUriChange = onAvatarUriChange,
        formName = formName,
        onNameChange = onNameChange,
        formRole = formRole,
        onRoleChange = onRoleChange,
        formPurpose = formPurpose,
        onPurposeChange = onPurposeChange,
        formInstructions = formInstructions,
        onInstructionsChange = onInstructionsChange,
        formPreferredModel = formPreferredModel,
        onPreferredModelChange = onPreferredModelChange,
        formReasoning = formReasoning,
        onReasoningChange = onReasoningChange,
        formAutonomy = formAutonomy,
        onAutonomyChange = onAutonomyChange,
        formTags = formTags,
        onTagsChange = onTagsChange,
        onBack = onBack,
        onSave = onSave
    )
}

// ==========================================
// 2. ECRAN UNELTE & EXECUȚIE WORKSPACE
// ==========================================
@Composable
fun WorkspaceToolsScreenContent(
    formActiveTools: List<String>,
    onActiveToolsChange: (List<String>) -> Unit,
    formSwarmEnabled: Boolean,
    onSwarmEnabledChange: (Boolean) -> Unit,
    formSwarmRoles: List<String>,
    onSwarmRolesChange: (List<String>) -> Unit,
    formCanBeConsultedByHermes: Boolean,
    onCanBeConsultedByHermesChange: (Boolean) -> Unit,
    formMaxDelegationDepth: Int,
    onMaxDelegationDepthChange: (Int) -> Unit,
    formAllowedIncoming: List<String> = listOf("agent_hermes"),
    onAllowedIncomingChange: (List<String>) -> Unit = {},
    formAllowedOutgoing: List<String> = emptyList(),
    onAllowedOutgoingChange: (List<String>) -> Unit = {},
    interAgentMessages: List<InterAgentMessageEntity> = emptyList(),
    onTriggerSwarmWorkflow: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    WorkspaceToolsScreen(
        formActiveTools = formActiveTools,
        onActiveToolsChange = onActiveToolsChange,
        formSwarmEnabled = formSwarmEnabled,
        onSwarmEnabledChange = onSwarmEnabledChange,
        formSwarmRoles = formSwarmRoles,
        onSwarmRolesChange = onSwarmRolesChange,
        formCanBeConsultedByHermes = formCanBeConsultedByHermes,
        onCanBeConsultedByHermesChange = onCanBeConsultedByHermesChange,
        formMaxDelegationDepth = formMaxDelegationDepth,
        onMaxDelegationDepthChange = onMaxDelegationDepthChange,
        formAllowedIncoming = formAllowedIncoming,
        onAllowedIncomingChange = onAllowedIncomingChange,
        formAllowedOutgoing = formAllowedOutgoing,
        onAllowedOutgoingChange = onAllowedOutgoingChange,
        interAgentMessages = interAgentMessages,
        onTriggerSwarmWorkflow = onTriggerSwarmWorkflow,
        onBack = onBack,
        onSave = onSave
    )
}

// ==========================================
// 3. ECRAN FIȘIERE & CUNOAȘTERE WORKSPACE
// ==========================================
@Composable
fun WorkspaceFilesScreenContent(
    formMemoryScope: WorkspaceMemoryScope,
    onMemoryScopeChange: (WorkspaceMemoryScope) -> Unit,
    formExtraMemoryInstructions: String,
    onExtraMemoryInstructionsChange: (String) -> Unit,
    formKnowledgeList: List<KnowledgeItem>,
    onKnowledgeListChange: (List<KnowledgeItem>) -> Unit,
    workspaceFiles: List<File>,
    onAddFile: (name: String, content: String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    WorkspaceFilesScreen(
        formMemoryScope = formMemoryScope,
        onMemoryScopeChange = onMemoryScopeChange,
        formExtraMemoryInstructions = formExtraMemoryInstructions,
        onExtraMemoryInstructionsChange = onExtraMemoryInstructionsChange,
        formKnowledgeList = formKnowledgeList,
        onKnowledgeListChange = onKnowledgeListChange,
        workspaceFiles = workspaceFiles,
        onAddFile = onAddFile,
        onDeleteFile = onDeleteFile,
        onBack = onBack,
        onSave = onSave
    )
}

// ==========================================
// 4. ECRAN TASK-URI WORKSPACE
// ==========================================
@Composable
fun WorkspaceTasksScreenContent(
    workspace: WorkspaceProfile,
    tasks: List<WorkspaceTaskEntity>,
    onTriggerCreateTask: () -> Unit,
    onRunTaskAutonomously: (task: WorkspaceTaskEntity, workspace: WorkspaceProfile) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("TOATE") }

    val filteredTasks = when (selectedFilter) {
        "IN_PROGRESS" -> tasks.filter { it.status == WorkspaceTaskStatus.IN_PROGRESS }
        "PENDING" -> tasks.filter { it.status == WorkspaceTaskStatus.PENDING }
        "COMPLETED" -> tasks.filter { it.status == WorkspaceTaskStatus.COMPLETED }
        else -> tasks
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .dockBottomGradientFade(fadeHeight = 185.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sarcini & Execuție Autonomă", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                                Text("${tasks.size} task-uri totale înregistrate", fontSize = 11.sp, color = TextMuted)
                            }

                            Button(
                                onClick = onTriggerCreateTask,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Task Nou", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("TOATE" to "Toate", "IN_PROGRESS" to "În Lucru", "PENDING" to "În Așteptare", "COMPLETED" to "Finalizate").forEach { (filterKey, label) ->
                                val isSel = selectedFilter == filterKey
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ItemBackground)
                                        .clickable { selectedFilter = filterKey }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(label, fontSize = 11.sp, color = if (isSel) TextPrimary else TextMuted)
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) EmeraldPrimary else Color(0xFF26302B))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Text("Niciun task găsit.", color = TextMuted, fontSize = 13.sp)
                            Text("Apasă '+ Task Nou' pentru a delega o sarcină sub-agentului.", color = TextMuted.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(filteredTasks, key = { index, task -> "${task.id}_$index" }) { index, task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    task.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )

                                val (statusBg, statusFg, statusTxt) = when (task.status) {
                                    WorkspaceTaskStatus.PENDING -> Triple(Color(0xFF2E2210), Color(0xFFFBBF24), "În Așteptare")
                                    WorkspaceTaskStatus.IN_PROGRESS -> Triple(Color(0xFF10283A), Color(0xFF60A5FA), "În Lucru")
                                    WorkspaceTaskStatus.RUNNING -> Triple(Color(0xFF10283A), Color(0xFF60A5FA), "În Rulare")
                                    WorkspaceTaskStatus.COMPLETED -> Triple(Color(0xFF103322), EmeraldAccent, "Finalizat")
                                    WorkspaceTaskStatus.FAILED -> Triple(Color(0xFF3B1616), Color(0xFFF87171), "Eșuat")
                                    else -> Triple(Color(0xFF2E2210), Color(0xFFFBBF24), "În Așteptare")
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(statusTxt, fontSize = 10.sp, color = statusFg, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (task.instructions.isNotBlank()) {
                                Text(task.instructions, fontSize = 11.sp, color = TextSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Rol: ${task.assignedSubRole}", fontSize = 10.5.sp, color = TextMuted)
                                Text(
                                    SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(task.createdAt)),
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }

                            if (task.resultSummary.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ItemBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                            Text("Rezumat Execuție & Artefacte:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                        }
                                        Text(task.resultSummary, fontSize = 11.sp, color = TextPrimary)
                                        if (task.producedArtifacts.isNotBlank()) {
                                            Text("📁 Artefact: ${task.producedArtifacts}", fontSize = 10.5.sp, color = Color(0xFF93C5FD), fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onDeleteTask(task.id.toString()) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Șterge task", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }

                                if (task.status == WorkspaceTaskStatus.PENDING || task.status == WorkspaceTaskStatus.FAILED) {
                                    Button(
                                        onClick = { onRunTaskAutonomously(task, workspace) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF13362B),
                                            contentColor = EmeraldAccent
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Execută Acum Autonom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (task.status == WorkspaceTaskStatus.IN_PROGRESS) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF3B82F6)
                                        )
                                        Text("Sub-agentul execută uneltele...", fontSize = 11.sp, color = Color(0xFF93C5FD))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
            ) {
                Text("Înapoi", fontSize = 13.sp)
            }

            Button(
                onClick = onTriggerCreateTask,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Deleagă Task", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ==========================================
// Șabloane Structurate pentru Workspace
// ==========================================
data class WorkspaceTemplate(
    val title: String,
    val emoji: String,
    val role: String,
    val purpose: String,
    val instructions: String,
    val model: String,
    val avatarUri: String?
)

// ==========================================
// 5. ECRAN CREARE WORKSPACE NOU
// ==========================================
@Composable
fun WorkspaceCreateScreenContent(
    formEmoji: String,
    onEmojiChange: (String) -> Unit,
    formAvatarUri: String?,
    onAvatarUriChange: (String?) -> Unit,
    formName: String,
    onNameChange: (String) -> Unit,
    formRole: String,
    onRoleChange: (String) -> Unit,
    formPurpose: String,
    onPurposeChange: (String) -> Unit,
    formInstructions: String,
    onInstructionsChange: (String) -> Unit,
    formPreferredModel: String,
    onPreferredModelChange: (String) -> Unit,
    formReasoning: String,
    onReasoningChange: (String) -> Unit,
    onSelectTemplate: (title: String, emoji: String, role: String, purpose: String, instructions: String, model: String, avatarUri: String?) -> Unit,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    val templates = listOf(
        WorkspaceTemplate(
            title = "Arhitectură & Cod",
            emoji = "💻",
            role = "Senior Full-Stack Architect",
            purpose = "Dezvoltare software Kotlin/Android",
            instructions = "Oferă cod curat, modular, fără cod mort.",
            model = "Hermes 3 (Nous Research)",
            avatarUri = "avatar_marcus_liu"
        ),
        WorkspaceTemplate(
            title = "Cercetare & Date",
            emoji = "🔬",
            role = "Document Analyst",
            purpose = "Sinteză de documente și cercetare",
            instructions = "Sintetizează documentele și extrage corelații cheie.",
            model = "Gemini 2.5 Flash",
            avatarUri = "avatar_sofia_reyes"
        ),
        WorkspaceTemplate(
            title = "Consiliere Juridică",
            emoji = "⚖️",
            role = "Compliance & Regulatory Counsel",
            purpose = "Analiză contracte, conformitate și protecție legală",
            instructions = "Adoptă o terminologie juridică clară și precisă.",
            model = "Hermes 3 (Nous Research)",
            avatarUri = "avatar_lawyer"
        ),
        WorkspaceTemplate(
            title = "Consultanță Financiară",
            emoji = "📈",
            role = "Senior Financial Strategist",
            purpose = "Planificare financiară, ROI și bugete",
            instructions = "Calculează cu rigoare matematică și prudență fiscală.",
            model = "Hermes 3 (Nous Research)",
            avatarUri = "avatar_financial"
        ),
        WorkspaceTemplate(
            title = "Hermes Universal",
            emoji = "⚡",
            role = "Universal Agent",
            purpose = "Asistență generală",
            instructions = "Răspunde structurat, precis și adaptat contextului.",
            model = "Hermes 3 (Nous Research)",
            avatarUri = null
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .dockBottomGradientFade(fadeHeight = 185.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Șabloane Rapide:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(templates) { template ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ItemBackground)
                                    .clickable {
                                        onSelectTemplate(
                                            template.title,
                                            template.emoji,
                                            template.role,
                                            template.purpose,
                                            template.instructions,
                                            template.model,
                                            template.avatarUri
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(template.emoji, fontSize = 14.sp)
                                Text(template.title, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Configurare Workspace", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)

                        // --- PREMIUM PORTRAIT AVATAR SELECTOR (FEȚE REALE) ---
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Selectează Portret Profesional Agent (Față Reală):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val avatars = listOf(
                                    Triple("avatar_daniel_kim", "Daniel Kim", "Sentiment Analyst"),
                                    Triple("avatar_sofia_reyes", "Sofia Reyes", "Macro Analyst"),
                                    Triple("avatar_marcus_liu", "Marcus Liu", "Technical Analyst"),
                                    Triple("avatar_emma_hart", "Emma Hart", "Data Scientist"),
                                    Triple("avatar_alex_chen", "Alex Chen", "Web Developer"),
                                    Triple("avatar_olivia_smith", "Olivia Smith", "Business Advisor"),
                                    Triple("avatar_leo_vance", "Leo Vance", "Creative Designer"),
                                    Triple("avatar_clara_vance", "Clara Vance", "Legal Counsel"),
                                    Triple("avatar_elena_pop", "Elena Pop", "Content Editor"),
                                    Triple("img_easy_avatar", "Default", "Universal")
                                )
                                items(avatars) { (id, name, desc) ->
                                    val isSelected = formAvatarUri == id || (id == "img_easy_avatar" && formAvatarUri == null)
                                    val drawableId = when (id) {
                                        "avatar_daniel_kim" -> R.drawable.avatar_daniel_kim
                                        "avatar_sofia_reyes" -> R.drawable.avatar_sofia_reyes
                                        "avatar_marcus_liu" -> R.drawable.avatar_marcus_liu
                                        "avatar_emma_hart" -> R.drawable.avatar_emma_hart
                                        "avatar_alex_chen" -> R.drawable.avatar_alex_chen
                                        "avatar_olivia_smith" -> R.drawable.avatar_olivia_smith
                                        "avatar_leo_vance" -> R.drawable.avatar_leo_vance
                                        "avatar_clara_vance" -> R.drawable.avatar_clara_vance
                                        "avatar_elena_pop" -> R.drawable.avatar_elena_pop
                                        else -> R.drawable.img_easy_avatar
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) (if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF132B20)) else ItemBackground
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) EmeraldPrimary else SubtleBorderColor
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clickable {
                                                if (id == "img_easy_avatar") {
                                                    onAvatarUriChange(null)
                                                } else {
                                                    onAvatarUriChange(id)
                                                    // Auto-populate blank/default fields
                                                    if (formName.isBlank() || formName == "New Agent" || formName == "Workspace Nou") {
                                                        onNameChange(name)
                                                    }
                                                    if (formRole.isBlank()) {
                                                        onRoleChange(desc)
                                                    }
                                                }
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = drawableId),
                                                contentDescription = name,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.dp,
                                                        color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = name.split(" ").first(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) TextPrimary else TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = desc.split(" ").first(),
                                                fontSize = 8.sp,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = formEmoji,
                                onValueChange = { if (it.length <= 2) onEmojiChange(it) },
                                label = { Text("Emoji", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.width(80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SubtleBorderColor,
                                    unfocusedBorderColor = SubtleBorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground
                                )
                            )

                            OutlinedTextField(
                                value = formName,
                                onValueChange = onNameChange,
                                label = { Text("Nume Workspace", fontSize = 11.sp) },
                                placeholder = { Text("ex. Dezvoltare Software", color = TextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SubtleBorderColor,
                                    unfocusedBorderColor = SubtleBorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground
                                )
                            )
                        }

                        OutlinedTextField(
                            value = formRole,
                            onValueChange = onRoleChange,
                            label = { Text("Rol / Persona Sub-Agent", fontSize = 11.sp) },
                            placeholder = { Text("ex. Senior Full-Stack Architect", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SubtleBorderColor,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )

                        OutlinedTextField(
                            value = formPurpose,
                            onValueChange = onPurposeChange,
                            label = { Text("Scop & Obiective Specializate", fontSize = 11.sp) },
                            placeholder = { Text("Descrie pe scurt ce realizează acest workspace...", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SubtleBorderColor,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )

                        OutlinedTextField(
                            value = formInstructions,
                            onValueChange = onInstructionsChange,
                            label = { Text("Instrucțiuni de Sistem (System Prompt)", fontSize = 11.sp) },
                            placeholder = { Text("Reguli de comportament, constrângeri de cod, ton de comunicare...", color = TextMuted, fontSize = 12.sp) },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SubtleBorderColor,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )

                        Text("Model AI:", fontSize = 11.5.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Hermes 3", "Gemini 2.5", "Claude 3.5", "GPT-4o").forEach { modelKey ->
                                val isSel = formPreferredModel.contains(modelKey)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ItemBackground)
                                        .clickable { onPreferredModelChange(modelKey) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(modelKey, fontSize = 11.sp, color = if (isSel) TextPrimary else TextMuted)
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) EmeraldPrimary else Color(0xFF26302B))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 86.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
            ) {
                Text("Anulează", fontSize = 13.sp)
            }

            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Creează Workspace", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
