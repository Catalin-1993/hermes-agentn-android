package com.example.ui.screens.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InterAgentMessageEntity
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: String
)

private val AvailableTools = listOf(
        ToolDefinition("Canva Studio", "Canva Studio & Canvas UI", "Generate interactive interfaces, graphics and canvas artefacts", Icons.Default.Palette, "Design & UI"),
        ToolDefinition("Terminal Shell", "Terminal Shell (Bash & CLI)", "Run system commands, builds and processes in a console", Icons.Default.Terminal, "Execuție"),
        ToolDefinition("Web Search", "Web Search", "Live web queries and external documentation", Icons.Default.Search, "Cercetare"),
        ToolDefinition("File Manager", "File Manager & Sandbox", "Create, read, edit and delete files in the project", Icons.Default.FolderOpen, "Fișiere"),
        ToolDefinition("Python", "Python Code Interpreter", "Run algorithms, maths and data processing", Icons.Default.Code, "Execuție"),
        ToolDefinition("MCP Connectors", "Model Context Protocol (MCP)", "Standard connectors for GitHub, SQL and external APIs", Icons.Default.Hub, "Integrări"),
        ToolDefinition("Git Repositories", "Git Version Control", "Clone repos, branch, check diffs and commit", Icons.Default.Build, "Inginerie"),
        ToolDefinition("Calculator & Tabele", "Calculator & Tables", "Evaluate expressions and transform CSV/JSON", Icons.Default.AutoAwesome, "Analiză"),
        ToolDefinition("Sintetizare Documente", "Document Summaries", "Index, search and summarise large documents", Icons.Default.Description, "Documente"),
        ToolDefinition("Browser WebView Automation", "WebView Browser Agent", "Browse the web, inspect pages and interact with them", Icons.Default.Language, "Web & Browser"),
        ToolDefinition("Google Workspace", "Google Workspace", "Create and sync Drive, Docs and Sheets files", Icons.Default.Cloud, "Productivitate"),
        ToolDefinition("Calendar & Agendă", "Calendar & Agenda", "Meetings, daily agenda and reminders", Icons.Default.DateRange, "Organizare"),
        ToolDefinition("Notion & Cunoștințe", "Notion & Knowledge", "Notion databases, wiki pages and tasks", Icons.Default.Description, "Cunoștințe"),
        ToolDefinition("Database & SQL / Supabase", "Databases & Supabase", "Query PostgreSQL and manage tables", Icons.Default.Storage, "Date & Backend"),
        ToolDefinition("Hugging Face & Modele ML", "Hugging Face Hub", "Open-source models, datasets and tokenizers", Icons.Default.Psychology, "AI & ML"),
        ToolDefinition("E-Commerce & Plăți", "E-Commerce & Payments", "Products, orders, inventory and payment gateways", Icons.Default.ShoppingCart, "Comerț"),
        ToolDefinition("Comunicare (Slack, Discord, Telegram)", "Messaging (Slack, Discord, Telegram)", "Send team alerts and channel messages", Icons.Default.ChatBubble, "Comunicare"),
        ToolDefinition("API & Webhook Plugins", "API & Webhook Plugins", "Webhook endpoints, REST services and plugins", Icons.Default.Extension, "Plugin-uri"),
        ToolDefinition("GitHub & Cloud Connectors", "GitHub & Cloud", "Sync repos, pull requests, issues and CI/CD", Icons.Default.DeviceHub, "Conectori")
    )


private val DefaultRoles = listOf("Arhitect", "Executant", "Reviewer", "Researcher", "Tester", "SecOps")

/**
 * What a project's agent may use and who it works with: a searchable list of tools with a switch each,
 * then a few simple settings for working together with other agents.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceToolsScreen(
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
    var query by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("") }
    val glass = LocalHermesGlassTokens.current
    val shown = remember(query) {
        AvailableTools.filter { query.isBlank() || it.name.contains(query, true) || it.description.contains(query, true) || it.category.contains(query, true) }
    }
    ProjectEditPage(saveLabel = "Save", onSave = onSave, saveTag = "btn_save_tools") {
        ProjectSection(
            title = "Tools (${formActiveTools.size} of ${AvailableTools.size} on)",
            hint = "What the agent may use in this project.",
        ) {
            ProjectField(query, { query = it }, label = "Search tools", tag = "field_tool_search")
            shown.forEach { tool ->
                val on = tool.id in formActiveTools
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("tool_row_${tool.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (on) 0.18f else 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = if (on) MaterialTheme.colorScheme.primary else glass.mutedText, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(tool.description, style = MaterialTheme.typography.bodySmall, color = glass.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Switch(
                        checked = on,
                        onCheckedChange = { checked -> onActiveToolsChange(if (checked) formActiveTools + tool.id else formActiveTools - tool.id) },
                        modifier = Modifier.testTag("tool_switch_${tool.id}"),
                    )
                }
            }
        }
        ProjectSection(title = "Collaboration", hint = "Working together with other agents.") {
            ProjectSwitchRow(
                title = "Agent team",
                subtitle = "The project can share work with other agents.",
                checked = formSwarmEnabled,
                onChange = onSwarmEnabledChange,
                tag = "switch_swarm",
            )
            ProjectSwitchRow(
                title = "Can be consulted by Brain",
                subtitle = "The main agent can ask it when needed.",
                checked = formCanBeConsultedByHermes,
                onChange = onCanBeConsultedByHermesChange,
                tag = "switch_consulted",
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Delegation depth", style = MaterialTheme.typography.bodyLarge)
                    Text("How many levels of agents can work for each other.", style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                }
                Stepper(
                    value = formMaxDelegationDepth,
                    onChange = onMaxDelegationDepthChange,
                    range = 1..5,
                )
            }
            if (formSwarmEnabled) {
                Text("Team roles", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (DefaultRoles + formSwarmRoles).distinct().forEach { role ->
                        val on = role in formSwarmRoles
                        Chip(role, on) { onSwarmRolesChange(if (on) formSwarmRoles - role else formSwarmRoles + role) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectField(newRole, { newRole = it }, label = "New role", modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add role",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(enabled = newRole.isNotBlank()) {
                            onSwarmRolesChange((formSwarmRoles + newRole.trim()).distinct())
                            newRole = ""
                        }.padding(8.dp),
                    )
                }
                PermissionChips("Who can write to it", formAllowedIncoming, onAllowedIncomingChange)
                PermissionChips("Who it can write to", formAllowedOutgoing, onAllowedOutgoingChange)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(onClick = onTriggerSwarmWorkflow)
                        .padding(vertical = 12.dp)
                        .testTag("btn_swarm_workflow"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Start a team workflow", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit, range: IntRange) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton("−", enabled = value > range.first) { onChange(value - 1) }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 6.dp))
        StepButton("+", enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.14f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.primary else LocalHermesGlassTokens.current.mutedText)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
    }
}

/** A list of agent names with a cross each and a field to add one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PermissionChips(title: String, values: List<String>, onChange: (List<String>) -> Unit) {
    var draft by remember { mutableStateOf("") }
    Text(title, style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .clickable { onChange(values - value) }
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.Close, contentDescription = "Remove $value", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProjectField(draft, { draft = it }, label = "Agent id", modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(enabled = draft.isNotBlank()) {
                onChange((values + draft.trim()).distinct())
                draft = ""
            }.padding(8.dp),
        )
    }
}
