package com.example.ui.screens.workspace

import androidx.compose.runtime.Composable

/**
 * What a project is and how its agent works: a name and role, what it is for, the instructions, and
 * three simple choices: how hard to think, how freely to act, and which model to prefer.
 */
@Composable
fun WorkspaceInstructionsScreen(
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
    val efforts = listOf("Rapid", "Echilibrat", "Profund")
    val autonomies = listOf("Cautious" to "Precaut", "Balanced" to "Echilibrat", "Autonomous" to "Autonom")
    ProjectEditPage(saveLabel = "Salvează", onSave = onSave, saveTag = "btn_save_instructions") {
        ProjectSection(title = "Despre proiect") {
            ProjectField(formName, onNameChange, label = "Nume", tag = "field_project_name")
            ProjectField(formRole, onRoleChange, label = "Rol", tag = "field_project_role")
            ProjectField(
                formPurpose, onPurposeChange,
                label = "La ce folosește",
                minLines = 2,
                tag = "field_project_purpose",
            )
        }
        ProjectSection(title = "Instrucțiuni", hint = "Cum să lucreze agentul acestui proiect.") {
            ProjectField(
                formInstructions, onInstructionsChange,
                label = "Instrucțiuni",
                minLines = 6,
                maxLines = 20,
                placeholder = "Ex.: Răspunde scurt, în română. Cere o clarificare înainte de a șterge ceva.",
                tag = "field_project_instructions",
            )
        }
        ProjectSection(title = "Mod de lucru") {
            androidx.compose.material3.Text("Efort de gândire", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            ProjectChoice(
                options = efforts,
                selectedIndex = efforts.indexOfFirst { it.equals(formReasoning, ignoreCase = true) },
                onSelect = { onReasoningChange(efforts[it]) },
                tag = "ReasoningTab",
            )
            androidx.compose.material3.Text("Cât de liber acționează", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            ProjectChoice(
                options = autonomies.map { it.second },
                selectedIndex = autonomies.indexOfFirst { (key, label) -> formAutonomy.equals(key, true) || formAutonomy.equals(label, true) },
                onSelect = { onAutonomyChange(autonomies[it].first) },
                tag = "AutonomyTab",
            )
            ProjectField(
                formPreferredModel, onPreferredModelChange,
                label = "Model preferat (opțional)",
                tag = "field_project_model",
            )
        }
    }
}
