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
    val efforts = listOf("Fast", "Balanced", "Deep")
    val autonomies = listOf("Cautious" to "Careful", "Balanced" to "Balanced", "Autonomous" to "Autonomous")
    ProjectEditPage(saveLabel = "Save", onSave = onSave, saveTag = "btn_save_instructions") {
        ProjectSection(title = "About the project") {
            ProjectField(formName, onNameChange, label = "Name", tag = "field_project_name")
            ProjectField(formRole, onRoleChange, label = "Role", tag = "field_project_role")
            ProjectField(
                formPurpose, onPurposeChange,
                label = "What it is for",
                minLines = 2,
                tag = "field_project_purpose",
            )
        }
        ProjectSection(title = "Instructions", hint = "How this project's agent should work.") {
            ProjectField(
                formInstructions, onInstructionsChange,
                label = "Instructions",
                minLines = 6,
                maxLines = 20,
                placeholder = "E.g. Answer briefly. Ask before deleting anything.",
                tag = "field_project_instructions",
            )
        }
        ProjectSection(title = "How it works") {
            androidx.compose.material3.Text("Thinking effort", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            ProjectChoice(
                options = efforts,
                selectedIndex = efforts.indexOfFirst { it.equals(formReasoning, ignoreCase = true) },
                onSelect = { onReasoningChange(efforts[it]) },
                tag = "ReasoningTab",
            )
            androidx.compose.material3.Text("How freely it acts", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            ProjectChoice(
                options = autonomies.map { it.second },
                selectedIndex = autonomies.indexOfFirst { (key, label) -> formAutonomy.equals(key, true) || formAutonomy.equals(label, true) },
                onSelect = { onAutonomyChange(autonomies[it].first) },
                tag = "AutonomyTab",
            )
            ProjectField(
                formPreferredModel, onPreferredModelChange,
                label = "Preferred model (optional)",
                tag = "field_project_model",
            )
        }
    }
}
