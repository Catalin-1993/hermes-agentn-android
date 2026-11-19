package com.mobilefork.hermesagent.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.OutlinedButton
import com.mobilefork.hermesagent.ui.theme.TextButton
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface

/** Shows the oldest change waiting for the person, over whatever screen is open. */
@Composable
fun ChangeApprovalHost() {
    val pending by ChangeApprovalCenter.pending.collectAsState()
    val change = pending.firstOrNull() ?: return
    key(change.id) {
        ChangeApprovalDialog(change = change, waitingAfter = pending.size - 1)
    }
}

@Composable
private fun ChangeApprovalDialog(change: PendingChange, waitingAfter: Int) {
    // Back and a tap outside do nothing: the agent waits for an explicit Accept or Reject.
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ChangeApprovalCard(
                change = change,
                waitingAfter = waitingAfter,
                onAccept = { edits -> ChangeApprovalCenter.accept(change.id, edits) },
                onReject = { ChangeApprovalCenter.reject(change.id) },
                modifier = Modifier.widthIn(max = 560.dp),
            )
        }
    }
}

@Composable
internal fun ChangeApprovalCard(
    change: PendingChange,
    waitingAfter: Int,
    onAccept: (Map<String, String>) -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalHermesGlassTokens.current
    val scheme = MaterialTheme.colorScheme
    val destructive = change.kind == "delete"
    val accent = if (destructive) scheme.error else scheme.primary
    var editing by remember { mutableStateOf(false) }
    val drafts = remember { mutableStateMapOf<String, String>().apply { change.fields.forEach { (k, v) -> put(k, v) } } }
    val canEdit = change.editable.any { key -> change.fields.any { it.first == key } }
    val shape = RoundedCornerShape(28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .hermesGlassSurface(shape, shadowRadius = 24.dp)
            .clip(shape)
            .padding(20.dp)
            .testTag("HermesChangeApprovalCard"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(12.dp).background(accent, CircleShape))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "The agent asks before changing anything",
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.mutedText,
                )
                Text(
                    text = change.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
            }
        }

        if (change.target.isNotBlank()) {
            Text(
                text = change.target,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            change.fields.filter { it.first !in HIDDEN_FIELDS }.forEach { (key, value) ->
                val editableHere = editing && key in change.editable
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = fieldLabel(key) + if (key in change.truncated) " (shortened here)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.mutedText,
                    )
                    if (editableHere) {
                        OutlinedTextField(
                            value = drafts[key].orEmpty(),
                            onValueChange = { drafts[key] = it },
                            singleLine = key !in MULTILINE_FIELDS,
                            minLines = if (key in MULTILINE_FIELDS) 3 else 1,
                            modifier = Modifier.fillMaxWidth().testTag("HermesChangeApprovalField_$key"),
                        )
                    } else {
                        Text(
                            text = drafts[key] ?: value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            maxLines = if (key in MULTILINE_FIELDS) 12 else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(scheme.onSurface.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }

        if (change.note.isNotBlank()) {
            Text(text = change.note, style = MaterialTheme.typography.bodySmall, color = accent)
        }
        if (waitingAfter > 0) {
            Text(
                text = if (waitingAfter == 1) "1 more change is waiting" else "$waitingAfter more changes are waiting",
                style = MaterialTheme.typography.bodySmall,
                color = glass.mutedText,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onReject,
                colors = ButtonDefaults.textButtonColors(contentColor = scheme.error),
                modifier = Modifier.testTag("HermesChangeApprovalReject"),
            ) { Text("Reject") }
            if (canEdit) {
                OutlinedButton(
                    onClick = {
                        if (editing) change.fields.forEach { (k, v) -> drafts[k] = v }
                        editing = !editing
                    },
                    modifier = Modifier.testTag("HermesChangeApprovalEdit"),
                ) { Text(if (editing) "Undo edits" else "Edit") }
            }
            Button(
                onClick = { onAccept(drafts.toMap()) },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                modifier = Modifier.testTag("HermesChangeApprovalAccept"),
            ) { Text(if (editing) "Save and accept" else if (destructive) "Delete" else "Accept") }
        }
    }
}

private val HIDDEN_FIELDS = setOf("action", "id", "mode")
private val MULTILINE_FIELDS = setOf("content", "description", "file_content", "new_string", "patch", "command")

private fun fieldLabel(key: String): String = when (key) {
    "title" -> "Title"
    "content", "file_content" -> "Content"
    "date" -> "Date"
    "start_time" -> "Starts"
    "end_time" -> "Ends"
    "location" -> "Location"
    "description" -> "Description"
    "tags" -> "Tags"
    "path", "relative_path" -> "File"
    "command" -> "Command"
    "old_string", "old_text" -> "Replaces"
    "new_string", "new_text" -> "With"
    "target" -> "Where"
    else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
