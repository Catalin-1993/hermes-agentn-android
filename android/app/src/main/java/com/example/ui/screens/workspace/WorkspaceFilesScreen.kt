package com.example.ui.screens.workspace

import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHigh
import android.widget.Toast
import com.example.ui.components.dock.dockBottomGradientFade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.KnowledgeItem
import com.example.data.model.WorkspaceMemoryScope
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val CardBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color.White else SurfaceCardHigh
private val ItemBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFF1F5F9) else SurfaceCardHigh
private val InputBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFF8FAFC) else SurfaceCardHigh
private val SubtleBorderColor: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0x1A000000) else BorderSubtle
private val EmeraldBorderColor: Color
    @Composable get() = EmeraldPrimary.copy(alpha = 0.2f)

@Composable
fun WorkspaceFilesScreen(
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Sandbox Files, 1: Knowledge Base, 2: Isolation & Memory

    // Dialog state for File Preview & Edit
    var previewFile by remember { mutableStateOf<File?>(null) }
    var previewContent by remember { mutableStateOf("") }
    var isEditingPreview by remember { mutableStateOf(false) }

    // State for creating new sandbox file
    var isCreatingFile by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    // State for adding knowledge item
    var isAddingKnowledge by remember { mutableStateOf(false) }
    var newKnowledgeTitle by remember { mutableStateOf("") }
    var newKnowledgeContent by remember { mutableStateOf("") }
    var newKnowledgeType by remember { mutableStateOf("NOTE") }

    val fileTemplates = listOf(
        "README.md" to "# Proiect Workspace\n\nDescrierea modulului și instrucțiuni de instalare.",
        "schema.sql" to "-- Schemă Bază de Date\nCREATE TABLE IF NOT EXISTS items (\n    id TEXT PRIMARY KEY,\n    name TEXT NOT NULL\n);",
        "config.json" to "{\n  \"version\": \"1.0.0\",\n  \"environment\": \"production\",\n  \"debug\": false\n}",
        "main.py" to "# Script Principal Python\ndef main():\n    print('Execuție inițializată cu succes')\n\nif __name__ == '__main__':\n    main()"
    )

    val memorySuggestions = listOf(
        "Proiect dezvoltat în Kotlin & Jetpack Compose",
        "Bază de date locală Room DB",
        "Răspunsuri tehnice în limba română",
        "Respectă arhitectura MVVM & Clean Code"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tab Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("📁 Sandbox (${workspaceFiles.size})", 0, Icons.Default.FolderOpen),
                Triple("🧠 Cunoștințe (${formKnowledgeList.size})", 1, Icons.Default.Bookmark),
                Triple("🔒 Izolare & Memorie", 2, Icons.Default.Security)
            ).forEach { (title, index, icon) ->
                val isSel = selectedTab == index
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) (if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else EmeraldPrimary.copy(alpha = 0.18f)) else ItemBackground,
                    border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) EmeraldPrimary else SubtleBorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .dockBottomGradientFade(fadeHeight = 185.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            when (selectedTab) {
                // ==================== TAB 0: FIȘIERE SANDBOX ====================
                0 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = BorderStroke(1.dp, EmeraldBorderColor),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                        Text("Fișiere Locale Sandbox", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    }

                                    Button(
                                        onClick = { isCreatingFile = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fișier Nou", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    "Aceste fișiere sunt stocate direct în spațiul izolat al acestui agent. Agentul le poate citi, modifica și crea autonom.",
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Formular Creare Fișier
                    if (isCreatingFile) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = BorderStroke(1.dp, EmeraldPrimary),
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
                                        Text("Creează Fișier Nou", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                                        IconButton(onClick = { isCreatingFile = false }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                                        }
                                    }

                                    // Quick Templates
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Șabloane Rapide:", fontSize = 10.5.sp, color = TextMuted)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(fileTemplates) { (tmplName, tmplContent) ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = ItemBackground,
                                                    border = BorderStroke(0.8.dp, SubtleBorderColor),
                                                    modifier = Modifier.clickable {
                                                        newFileName = tmplName
                                                        newFileContent = tmplContent
                                                    }
                                                ) {
                                                    Text(tmplName, fontSize = 10.5.sp, color = EmeraldAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = newFileName,
                                        onValueChange = { newFileName = it },
                                        label = { Text("Nume fișier (cu extensie)", fontSize = 11.sp) },
                                        placeholder = { Text("ex. README.md, model.py, data.json", color = TextMuted, fontSize = 11.5.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = SubtleBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = InputBackground,
                                            unfocusedContainerColor = InputBackground
                                        )
                                    )

                                    OutlinedTextField(
                                        value = newFileContent,
                                        onValueChange = { newFileContent = it },
                                        label = { Text("Conținut Fișier", fontSize = 11.sp) },
                                        placeholder = { Text("Scrie sau lipește conținutul fișierului...", color = TextMuted, fontSize = 11.5.sp) },
                                        minLines = 5,
                                        maxLines = 10,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = SubtleBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = InputBackground,
                                            unfocusedContainerColor = InputBackground
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                if (newFileName.isNotBlank()) {
                                                    onAddFile(newFileName.trim(), newFileContent)
                                                    newFileName = ""
                                                    newFileContent = ""
                                                    isCreatingFile = false
                                                    Toast.makeText(context, "Fișier adăugat cu succes!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Salvează Fișierul", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Lista Fișierelor
                    if (workspaceFiles.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                                    Text("Niciun fișier stocat în acest proiect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                                    Text("Apasă pe '+ Fișier Nou' sau trimite fișiere în chat pentru a le stoca aici.", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    } else {
                        items(workspaceFiles) { file ->
                            val fileSizeKb = (file.length() / 1024.0)
                            val sizeStr = if (fileSizeKb < 1) "${file.length()} B" else String.format(Locale.getDefault(), "%.1f KB", fileSizeKb)
                            val modDateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

                            val icon = when {
                                file.name.endsWith(".py") || file.name.endsWith(".kt") || file.name.endsWith(".json") || file.name.endsWith(".sql") -> Icons.Default.Code
                                file.name.endsWith(".md") || file.name.endsWith(".txt") -> Icons.Default.Description
                                else -> Icons.Default.FolderOpen
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CardBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EmeraldPrimary.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$sizeStr • Modificat: $modDateStr",
                                            fontSize = 10.5.sp,
                                            color = TextMuted
                                        )
                                    }

                                    // Preview / Edit Button
                                    IconButton(
                                        onClick = {
                                            try {
                                                previewContent = if (file.exists()) file.readText() else ""
                                                previewFile = file
                                                isEditingPreview = false
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Eroare citire: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = "Vizualizează", tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                    }

                                    // Copy Button
                                    IconButton(
                                        onClick = {
                                            try {
                                                val content = if (file.exists()) file.readText() else ""
                                                clipboardManager.setText(AnnotatedString(content))
                                                Toast.makeText(context, "Conținut copiat!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Eroare copiere: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiază", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { onDeleteFile(file) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Șterge", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 1: BAZĂ DE CUNOAȘTERE ====================
                1 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = BorderStroke(1.dp, EmeraldBorderColor),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                        Text("Bază de Cunoștințe & Notițe", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    }

                                    Button(
                                        onClick = { isAddingKnowledge = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Adaugă Notă", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    "Aceste fragmente de cunoștințe sunt injectate automat în contextul cognitiv al agentului la fiecare interogare.",
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Formular Adăugare Cunoștință
                    if (isAddingKnowledge) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = BorderStroke(1.dp, EmeraldPrimary),
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
                                        Text("Fragment Cunoștință Nou", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimary)
                                        IconButton(onClick = { isAddingKnowledge = false }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                                        }
                                    }

                                    // Type Selector (NOTE, CODE, DOC)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("NOTE" to "Notiță", "CODE" to "Cod / Snippet", "DOC" to "Document").forEach { (typeKey, label) ->
                                            val isSel = newKnowledgeType == typeKey
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) EmeraldPrimary.copy(alpha = 0.18f) else ItemBackground,
                                                border = BorderStroke(if (isSel) 1.dp else 0.8.dp, if (isSel) EmeraldPrimary else SubtleBorderColor),
                                                modifier = Modifier.clickable { newKnowledgeType = typeKey }
                                            ) {
                                                Text(
                                                    label,
                                                    fontSize = 11.sp,
                                                    color = if (isSel) EmeraldAccent else TextSecondary,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = newKnowledgeTitle,
                                        onValueChange = { newKnowledgeTitle = it },
                                        label = { Text("Titlu Notă / Concept", fontSize = 11.sp) },
                                        placeholder = { Text("ex. Structură Date API, Constrângeri...", color = TextMuted, fontSize = 11.5.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = SubtleBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = InputBackground,
                                            unfocusedContainerColor = InputBackground
                                        )
                                    )

                                    OutlinedTextField(
                                        value = newKnowledgeContent,
                                        onValueChange = { newKnowledgeContent = it },
                                        label = { Text("Conținut Cunoștință", fontSize = 11.sp) },
                                        placeholder = { Text("Introdu descrierea, instrucțiunile sau snippet-ul...", color = TextMuted, fontSize = 11.5.sp) },
                                        minLines = 4,
                                        maxLines = 8,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = SubtleBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = InputBackground,
                                            unfocusedContainerColor = InputBackground
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                if (newKnowledgeTitle.isNotBlank()) {
                                                    val newItem = KnowledgeItem(
                                                        id = "kn_${UUID.randomUUID().toString().take(8)}",
                                                        title = newKnowledgeTitle.trim(),
                                                        content = newKnowledgeContent.trim(),
                                                        type = newKnowledgeType,
                                                        timestamp = System.currentTimeMillis()
                                                    )
                                                    onKnowledgeListChange(formKnowledgeList + newItem)
                                                    newKnowledgeTitle = ""
                                                    newKnowledgeContent = ""
                                                    isAddingKnowledge = false
                                                    Toast.makeText(context, "Cunoștință adăugată!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Adaugă în Bază", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Lista Elementelor de Cunoaștere
                    if (formKnowledgeList.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                                    Text("Nu există notițe de cunoaștere încă", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                                    Text("Apasă pe '+ Adaugă Notă' pentru a ancora cunoștințe cheie în memoria agentului.", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    } else {
                        items(formKnowledgeList) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CardBackground,
                                border = BorderStroke(1.dp, SubtleBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(EmeraldPrimary.copy(alpha = 0.18f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(item.type, fontSize = 9.sp, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                                            }
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(item.content))
                                                    Toast.makeText(context, "Conținut copiat!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiază", tint = TextMuted, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    onKnowledgeListChange(formKnowledgeList.filter { it.id != item.id })
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Șterge", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    if (item.content.isNotBlank()) {
                                        Text(
                                            text = item.content,
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 2: IZOLARE & MEMORIE ====================
                2 -> {
                    // Regim Izolare Memorie
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                    Text("Regim de Izolare a Memoriei", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                }
                                Text("Alege cum sunt izolate datele și amintirile generate de acest agent:", fontSize = 11.5.sp, color = TextMuted)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        Pair(
                                            WorkspaceMemoryScope.WORKSPACE_ONLY,
                                            Triple("Izolare strictă în proiect", "Memoria și datele rămân strict în acest agent. Zero scurgeri de date către alți agenți.", Icons.Default.Security)
                                        ),
                                        Pair(
                                            WorkspaceMemoryScope.GLOBAL_SHARED,
                                            Triple("Partajare Globală cu Toți Agenții", "Memoria și cunoștințele sunt vizibile și consultabile de toți ceilalți agenți din spațiul Hermes.", Icons.Default.Psychology)
                                        )
                                    ).forEach { (scope, info) ->
                                        val isSelected = formMemoryScope == scope
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.18f) else ItemBackground,
                                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) EmeraldPrimary else SubtleBorderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onMemoryScopeChange(scope) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    info.third,
                                                    contentDescription = null,
                                                    tint = if (isSelected) EmeraldAccent else TextMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(info.first, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = if (isSelected) TextPrimary else TextSecondary)
                                                    Text(info.second, fontSize = 10.5.sp, color = TextMuted)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, if (isSelected) EmeraldPrimary else SubtleBorderColor, CircleShape)
                                                        .background(if (isSelected) EmeraldPrimary else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Memorie Long-Term Persistentă
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                    Text("Context Memorie Long-Term", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                }

                                Text(
                                    "Fapte, decizii tehnice și preferințe memorate permanent în profilul acestui proiect:",
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )

                                // Suggestions
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Sugestii Rapide:", fontSize = 10.5.sp, color = TextMuted)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(memorySuggestions) { sugg ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = ItemBackground,
                                                border = BorderStroke(0.8.dp, SubtleBorderColor),
                                                modifier = Modifier.clickable {
                                                    if (!formExtraMemoryInstructions.contains(sugg)) {
                                                        val updated = if (formExtraMemoryInstructions.isBlank()) sugg else "$formExtraMemoryInstructions\n- $sugg"
                                                        onExtraMemoryInstructionsChange(updated)
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "+ $sugg",
                                                    fontSize = 10.5.sp,
                                                    color = EmeraldAccent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = formExtraMemoryInstructions,
                                    onValueChange = onExtraMemoryInstructionsChange,
                                    placeholder = {
                                        Text(
                                            "Adaugă amintiri cheie:\n- Proiectul folosește Room DB pentru persistență\n- Menține comentarii concise în cod...",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    },
                                    minLines = 5,
                                    maxLines = 10,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = SubtleBorderColor,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = InputBackground,
                                        unfocusedContainerColor = InputBackground
                                    )
                                )
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

    // ==================== DIALOG PREVIEW & EDITARE FIȘIER ====================
    previewFile?.let { currentFile ->
        val isMarkdown = currentFile.name.endsWith(".md", ignoreCase = true) || currentFile.name.endsWith(".markdown", ignoreCase = true)
        var mdPreviewMode by remember(currentFile.name) { mutableStateOf("formatted") }

        Dialog(onDismissRequest = { previewFile = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, EmeraldBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            Text(
                                currentFile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { previewFile = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Închide", tint = TextMuted)
                        }
                    }

                    if (isMarkdown && !isEditingPreview) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = InputBackground,
                                border = BorderStroke(0.8.dp, SubtleBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (mdPreviewMode == "formatted") EmeraldPrimary.copy(alpha = 0.25f) else Color.Transparent,
                                        modifier = Modifier.clickable { mdPreviewMode = "formatted" }
                                    ) {
                                        Text(
                                            text = "Formatat",
                                            fontSize = 11.sp,
                                            fontWeight = if (mdPreviewMode == "formatted") FontWeight.Bold else FontWeight.Normal,
                                            color = if (mdPreviewMode == "formatted") EmeraldAccent else TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (mdPreviewMode == "source") EmeraldPrimary.copy(alpha = 0.25f) else Color.Transparent,
                                        modifier = Modifier.clickable { mdPreviewMode = "source" }
                                    ) {
                                        Text(
                                            text = "Sursă",
                                            fontSize = 11.sp,
                                            fontWeight = if (mdPreviewMode == "source") FontWeight.Bold else FontWeight.Normal,
                                            color = if (mdPreviewMode == "source") EmeraldAccent else TextMuted,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SubtleBorderColor, thickness = 0.8.dp)

                    if (isEditingPreview) {
                        OutlinedTextField(
                            value = previewContent,
                            onValueChange = { previewContent = it },
                            minLines = 8,
                            maxLines = 14,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )
                    } else if (isMarkdown && mdPreviewMode == "formatted") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = InputBackground,
                            border = BorderStroke(0.8.dp, SubtleBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 340.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(12.dp)) {
                                item {
                                    com.example.ui.components.RichContentRenderer(
                                        content = previewContent.ifBlank { "*(Fișier gol)*" },
                                        isStreaming = false
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = InputBackground,
                            border = BorderStroke(0.8.dp, SubtleBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(10.dp)) {
                                item {
                                    Text(
                                        text = previewContent.ifBlank { "(Fișier gol)" },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = if (previewContent.isBlank()) TextMuted else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(previewContent))
                                    Toast.makeText(context, "Conținut copiat!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ItemBackground, contentColor = TextSecondary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiază", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { isEditingPreview = !isEditingPreview },
                                colors = ButtonDefaults.buttonColors(containerColor = ItemBackground, contentColor = EmeraldAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditingPreview) "Previzualizare" else "Editează", fontSize = 11.sp)
                            }
                        }

                        if (isEditingPreview) {
                            Button(
                                onClick = {
                                    try {
                                        currentFile.writeText(previewContent)
                                        isEditingPreview = false
                                        Toast.makeText(context, "Modificări salvate!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Eroare salvare: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Salvează", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
