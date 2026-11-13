package com.example.ui.screens.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.KnowledgeItem
import com.example.data.model.WorkspaceMemoryScope
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import java.io.File
import java.util.UUID

/**
 * What a project keeps: its files, the notes it knows, and how far its memory reaches. Files and notes
 * are added from a small form; a row's cross removes it.
 */
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
    var addingFile by remember { mutableStateOf(false) }
    var addingNote by remember { mutableStateOf(false) }
    val scopes = listOf(WorkspaceMemoryScope.WORKSPACE_ONLY to "Project only", WorkspaceMemoryScope.GLOBAL_SHARED to "Shared")
    ProjectEditPage(saveLabel = "Save", onSave = onSave, saveTag = "btn_save_files") {
        ProjectSection(title = "Files", hint = "Files the agent can read and write in this project.") {
            if (workspaceFiles.isEmpty()) EmptyLine("No files yet.")
            workspaceFiles.forEach { file ->
                ItemRow(
                    title = file.name,
                    subtitle = readableSize(file.length()),
                    onRemove = { onDeleteFile(file) },
                    tag = "project_file_${file.name}",
                )
            }
            AddRow("Add a file", { addingFile = true }, "btn_add_file")
        }
        ProjectSection(title = "What it knows", hint = "Notes and facts the project remembers.") {
            if (formKnowledgeList.isEmpty()) EmptyLine("Nothing added yet.")
            formKnowledgeList.forEach { item ->
                ItemRow(
                    title = item.title,
                    subtitle = item.content.replace('\n', ' ').take(90),
                    onRemove = { onKnowledgeListChange(formKnowledgeList.filter { it.id != item.id }) },
                    tag = "project_knowledge_${item.id}",
                )
            }
            AddRow("Add a note", { addingNote = true }, "btn_add_knowledge")
        }
        ProjectSection(title = "Memory", hint = "Who can see what this project remembers.") {
            ProjectChoice(
                options = scopes.map { it.second },
                selectedIndex = scopes.indexOfFirst { it.first == formMemoryScope },
                onSelect = { onMemoryScopeChange(scopes[it].first) },
                tag = "MemoryScopeTab",
            )
            ProjectField(
                formExtraMemoryInstructions, onExtraMemoryInstructionsChange,
                label = "Anything else to remember (optional)",
                minLines = 3,
                tag = "field_memory_extra",
            )
        }
    }
    if (addingFile) {
        TwoFieldDialog(
            title = "New file",
            firstLabel = "Name (e.g. notes.md)",
            secondLabel = "Content",
            onDismiss = { addingFile = false },
            onConfirm = { name, content ->
                onAddFile(name.trim(), content)
                addingFile = false
            },
            tag = "dialog_add_file",
        )
    }
    if (addingNote) {
        TwoFieldDialog(
            title = "New note",
            firstLabel = "Title",
            secondLabel = "Text",
            onDismiss = { addingNote = false },
            onConfirm = { title, content ->
                onKnowledgeListChange(
                    formKnowledgeList + KnowledgeItem(
                        id = "kn_${UUID.randomUUID().toString().take(8)}",
                        title = title.trim(),
                        content = content.trim(),
                        type = "TEXT",
                        timestamp = System.currentTimeMillis(),
                    ),
                )
                addingNote = false
            },
            tag = "dialog_add_knowledge",
        )
    }
}

private fun readableSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "${bytes / 1_024} KB"
    else -> "%.1f MB".format(bytes / 1_048_576.0)
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalHermesGlassTokens.current.mutedText)
}

@Composable
private fun ItemRow(title: String, subtitle: String, onRemove: () -> Unit, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalHermesGlassTokens.current.mutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove $title",
            tint = LocalHermesGlassTokens.current.mutedText,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable(onClick = onRemove).padding(6.dp),
        )
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TwoFieldDialog(
    title: String,
    firstLabel: String,
    secondLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    tag: String,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag(tag)) {
                ProjectField(first, { first = it }, label = firstLabel)
                ProjectField(second, { second = it }, label = secondLabel, minLines = 4)
            }
        },
        confirmButton = {
            TextButton(enabled = first.isNotBlank(), onClick = { onConfirm(first, second) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
