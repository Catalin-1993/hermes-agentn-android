package com.example.ui.screens.workspace

import androidx.compose.foundation.BorderStroke
import com.example.ui.components.dock.dockBottomGradientFade
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InterAgentMessageEntity
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
private val EmeraldBorderColor = Color(0x3310B981)

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: String
)

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
    var newSwarmRoleInput by remember { mutableStateOf("") }
    var isAddingSwarmRole by remember { mutableStateOf(false) }

    var newCallerInput by remember { mutableStateOf("") }
    var isAddingCaller by remember { mutableStateOf(false) }

    val availableTools = listOf(
        ToolDefinition("Canva Studio", "Canva Studio & Canvas UI", "Generare interfețe interactive, reprezentări grafice, artefacte și canvas", Icons.Default.Palette, "Design & UI"),
        ToolDefinition("Terminal Shell", "Terminal Shell (Bash & CLI)", "Execuție comenzi de sistem, compilare și procese în consolă", Icons.Default.Terminal, "Execuție"),
        ToolDefinition("Web Search", "Web Search & Căutare Live", "Interogare web în timp real, extragere date și documentație externă", Icons.Default.Search, "Cercetare"),
        ToolDefinition("File Manager", "File Manager & Sandbox", "Creare, citire, modificare și ștergere fișiere în workspace", Icons.Default.FolderOpen, "Fișiere"),
        ToolDefinition("Python", "Python Code Interpreter", "Rulare algoritmi, calcule matematice și procesare de date", Icons.Default.Code, "Execuție"),
        ToolDefinition("MCP Connectors", "Model Context Protocol (MCP)", "Conectori standardizați pentru GitHub, SQL, API-uri externe", Icons.Default.Hub, "Integrări"),
        ToolDefinition("Git Repositories", "Control Versiuni Git", "Clonare repo-uri, creare branch-uri, verificare diff-uri și commit-uri", Icons.Default.Build, "Inginerie"),
        ToolDefinition("Calculator & Tabele", "Calculator & Analiză Tabele", "Evaluare expresii numerice și transformare structurată CSV/JSON", Icons.Default.AutoAwesome, "Analiză"),
        ToolDefinition("Sintetizare Documente", "Sintetizare Documente Mari", "Indexare rapidă, căutare semantică și rezumare documente voluminoase", Icons.Default.Description, "Documente"),
        ToolDefinition("Browser WebView Automation", "Agent WebView Browser", "Navigare autonomă pe web, inspecție DOM, interacțiune automată cu pagini", Icons.Default.Language, "Web & Browser"),
        ToolDefinition("Google Workspace", "Google Workspace (Drive/Docs/Sheets)", "Sincronizare și creare documente, foi de calcul și prezentări Google", Icons.Default.Cloud, "Productivitate"),
        ToolDefinition("Calendar & Agendă", "Google & Outlook Calendar", "Gestiune ședințe, agendă zilnică, memento-uri și orare", Icons.Default.DateRange, "Organizare"),
        ToolDefinition("Notion & Cunoștințe", "Notion Workspace & Docs", "Acces baze de date Notion, pagini de wiki, documentație și task-uri", Icons.Default.Description, "Cunoștințe"),
        ToolDefinition("Database & SQL / Supabase", "Baze de Date & Supabase", "Interogare PostgreSQL, generare scheme SQL și gestionare tabele", Icons.Default.Storage, "Date & Backend"),
        ToolDefinition("Hugging Face & Modele ML", "Hugging Face Hub", "Acces modele AI open-source, greutăți GGUF, tokenizatoare și dataset-uri", Icons.Default.Psychology, "AI & ML"),
        ToolDefinition("E-Commerce & Plăți", "Shopify & Finanțe Online", "Gestiune produse, comenzi, inventare și integrări gateway plăți", Icons.Default.ShoppingCart, "Comerț"),
        ToolDefinition("Comunicare (Slack, Discord, Telegram)", "Rețele de Comunicare", "Trimitere alerte de echipă, mesaje în canale și notificări bot", Icons.Default.ChatBubble, "Comunicare"),
        ToolDefinition("API & Webhook Plugins", "API Webhooks & Plugin Connectors", "Conectori pentru endpoint-uri webhook, servicii REST și plugin-uri externe", Icons.Default.Extension, "Plugin-uri"),
        ToolDefinition("GitHub & Cloud Connectors", "GitHub & Cloud Provider Connectors", "Sincronizare repo-uri, pull requests, issue tracking și automate CI/CD", Icons.Default.DeviceHub, "Conectori")
    )

    val defaultRoles = listOf("Arhitect", "Executant", "Reviewer", "Researcher", "Tester", "SecOps")

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
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            // Header Card Rezumat & Preseturi
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, EmeraldBorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF142B21))
                                    .border(1.5.dp, EmeraldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(24.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Centru Unelte & Multi-Agent", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text(
                                    "${formActiveTools.size} active din ${availableTools.size} unelte • Swarm: ${if (formSwarmEnabled) "Activat" else "Dezactivat"}",
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ItemBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onActiveToolsChange(listOf("Terminal Shell", "Web Search", "File Manager", "Python"))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡ Esențiale (4)", fontSize = 11.sp, color = EmeraldAccent, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ItemBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onActiveToolsChange(availableTools.map { it.id })
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌟 Activează Toate", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ItemBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onActiveToolsChange(emptyList())
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dezactivează", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 1: Conectori & Arsenal Integrat
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            Text("Conectori & Arsenal Integrat", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextPrimary)
                        }
                        Text("Conectori autorizați, unelte de sistem, plugin-uri și medii de execuție:", fontSize = 11.5.sp, color = TextMuted)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableTools.forEach { tool ->
                                val isEnabled = formActiveTools.contains(tool.id)
                                val isLightTheme = com.example.ui.theme.LocalHermesTheme.current.isLight
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isEnabled) (if (isLightTheme) Color(0xFFECFDF5) else Color(0xFF13281E)) else ItemBackground,
                                    border = BorderStroke(
                                        width = if (isEnabled) 1.dp else 0.8.dp,
                                        color = if (isEnabled) EmeraldBorderColor else SubtleBorderColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isEnabled) {
                                                onActiveToolsChange(formActiveTools.filter { it != tool.id })
                                            } else {
                                                onActiveToolsChange(formActiveTools + tool.id)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isEnabled) EmeraldPrimary.copy(alpha = 0.2f) else (if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF1C2420))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                tool.icon,
                                                contentDescription = null,
                                                tint = if (isEnabled) EmeraldAccent else TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = tool.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp,
                                                    color = if (isEnabled) TextPrimary else TextSecondary
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF222B26))
                                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(tool.category, fontSize = 8.5.sp, color = TextMuted)
                                                }
                                            }
                                            Text(tool.description, fontSize = 10.5.sp, color = TextMuted, maxLines = 2)
                                        }

                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (!formActiveTools.contains(tool.id)) onActiveToolsChange(formActiveTools + tool.id)
                                                } else {
                                                    onActiveToolsChange(formActiveTools.filter { it != tool.id })
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = EmeraldPrimary,
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = if (isLightTheme) Color(0xFFCBD5E1) else Color(0xFF242C28)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 2: Orchestrator Swarm (Echipă Multi-Agent)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                Text("Orchestrator Swarm (Multi-Agent)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Switch(
                                checked = formSwarmEnabled,
                                onCheckedChange = onSwarmEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Color(0xFF242C28)
                                )
                            )
                        }

                        Text(
                            "Activează cooperarea autonomă între agenți specializați. Agentul poate delega sub-sarcini către alți coechipieri din rețea.",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )

                        if (formSwarmEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Roluri În Echipă (Sub-Agenți):", fontSize = 11.5.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                    if (!isAddingSwarmRole) {
                                        Text(
                                            "+ Adaugă Rol",
                                            fontSize = 11.sp,
                                            color = EmeraldAccent,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { isAddingSwarmRole = true }
                                        )
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    formSwarmRoles.forEach { role ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF16251E),
                                            border = BorderStroke(0.8.dp, EmeraldBorderColor)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(role, fontSize = 11.sp, color = EmeraldAccent)
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Șterge rol",
                                                    tint = TextMuted,
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable {
                                                            onSwarmRolesChange(formSwarmRoles.filter { it != role })
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isAddingSwarmRole) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newSwarmRoleInput,
                                            onValueChange = { newSwarmRoleInput = it },
                                            placeholder = { Text("ex. Data Engineer...", fontSize = 11.sp, color = TextMuted) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EmeraldPrimary,
                                                unfocusedBorderColor = SubtleBorderColor,
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedContainerColor = InputBackground,
                                                unfocusedContainerColor = InputBackground
                                            )
                                        )
                                        Button(
                                            onClick = {
                                                if (newSwarmRoleInput.isNotBlank()) {
                                                    val clean = newSwarmRoleInput.trim()
                                                    if (!formSwarmRoles.contains(clean)) {
                                                        onSwarmRolesChange(formSwarmRoles + clean)
                                                    }
                                                    newSwarmRoleInput = ""
                                                    isAddingSwarmRole = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Adaugă", fontSize = 11.sp)
                                        }
                                        IconButton(onClick = { isAddingSwarmRole = false; newSwarmRoleInput = "" }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // Trigger Swarm Execution Card
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF13281E),
                                    border = BorderStroke(1.dp, EmeraldBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Workflow Colectiv Swarm", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimary)
                                            Text("Pornește execuția sincronizată prin planificare, execuție și review.", fontSize = 10.5.sp, color = TextMuted)
                                        }

                                        Button(
                                            onClick = onTriggerSwarmWorkflow,
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rulează", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 3: Guvernanță A2A & Delegare Specializată
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            Text("Guvernanță A2A & Delegare Specializată", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }

                        // Consultare Hermes Orchestrator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Permite Consultarea de către Hermes Orchestrator", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                                Text("Hermes poate apela acest agent pentru sarcini din domeniul său de expertiză", fontSize = 10.5.sp, color = TextMuted)
                            }
                            Switch(
                                checked = formCanBeConsultedByHermes,
                                onCheckedChange = onCanBeConsultedByHermesChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Color(0xFF242C28)
                                )
                            )
                        }

                        HorizontalDivider(color = SubtleBorderColor, thickness = 0.8.dp)

                        // Nivel Maxim de Delegare (Max Depth)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Adâncime Maximă Delegare (Max Delegation Depth):", fontSize = 11.5.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    1 to "1 Nivel (Direct)",
                                    2 to "2 Niveluri (Standard)",
                                    3 to "3 Niveluri (Adânc)"
                                ).forEach { (depth, label) ->
                                    val isSel = formMaxDelegationDepth == depth
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) (if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF162B21)) else ItemBackground,
                                        border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) EmeraldPrimary else SubtleBorderColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onMaxDelegationDepthChange(depth) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("$depth", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isSel) EmeraldAccent else TextSecondary)
                                            Text(label, fontSize = 9.sp, color = TextMuted, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SubtleBorderColor, thickness = 0.8.dp)

                        // Apelanți Permiși
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Canale & Agenți Autorizați (Incoming):", fontSize = 11.5.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                if (!isAddingCaller) {
                                    Text(
                                        "+ Canal",
                                        fontSize = 11.sp,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { isAddingCaller = true }
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                formAllowedIncoming.forEach { caller ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF16251E),
                                        border = BorderStroke(0.8.dp, EmeraldBorderColor)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(caller, fontSize = 11.sp, color = EmeraldAccent)
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Șterge caller",
                                                tint = TextMuted,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable {
                                                        onAllowedIncomingChange(formAllowedIncoming.filter { it != caller })
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAddingCaller) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCallerInput,
                                        onValueChange = { newCallerInput = it },
                                        placeholder = { Text("ex. agent_hermes, ws_research...", fontSize = 11.sp, color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = SubtleBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = InputBackground,
                                            unfocusedContainerColor = InputBackground
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (newCallerInput.isNotBlank()) {
                                                val clean = newCallerInput.trim()
                                                if (!formAllowedIncoming.contains(clean)) {
                                                    onAllowedIncomingChange(formAllowedIncoming + clean)
                                                }
                                                newCallerInput = ""
                                                isAddingCaller = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("OK", fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { isAddingCaller = false; newCallerInput = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 4: Jurnal Comunicații Inter-Agent (Live A2A Audit)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Hub, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                Text("Jurnal Comunicații Inter-Agent", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Text("${interAgentMessages.size} mesaje", fontSize = 11.sp, color = TextMuted)
                        }

                        if (interAgentMessages.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ItemBackground,
                                border = BorderStroke(0.8.dp, SubtleBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Hub, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
                                    Text("Niciun schimb A2A înregistrat recent", fontSize = 11.5.sp, color = TextSecondary)
                                    Text("Mesajele de cooperare între agenți vor apărea automat aici în timp real.", fontSize = 10.5.sp, color = TextMuted)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                interAgentMessages.takeLast(5).reversed().forEach { msg ->
                                    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestamp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ItemBackground,
                                        border = BorderStroke(0.8.dp, SubtleBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("${msg.fromAgent} ➔ ${msg.toAgent}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EmeraldAccent)
                                                Text(dateStr, fontSize = 10.sp, color = TextMuted)
                                            }
                                            Text(msg.content.ifBlank { msg.payloadJson }, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Bar
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
                Text("Înapoi", fontSize = 13.5.sp)
            }

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salvează Modificările", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}
