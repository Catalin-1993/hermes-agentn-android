package com.mobilefork.hermesagent.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.IconButton
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import com.mobilefork.hermesagent.ui.theme.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.device.HermesHyMemoryBridge
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LocalMemoryEntry(
    val id: String,
    val content: String,
    val hitCount: Int,
    val promoted: Boolean,
)

data class LocalMemoryUiState(
    val loading: Boolean = true,
    val statusLine: String = "",
    val memoryCount: Int = 0,
    val reinforced: Int = 0,
    val promoted: Int = 0,
    val entries: List<LocalMemoryEntry> = emptyList(),
    val error: String = "",
)

class LocalMemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LocalMemoryUiState())
    val uiState: StateFlow<LocalMemoryUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            val app = getApplication<Application>()
            val status = withContext(Dispatchers.IO) {
                JSONObject(HermesHyMemoryBridge.performActionJson(app, "status"))
            }
            val list = withContext(Dispatchers.IO) {
                JSONObject(
                    HermesHyMemoryBridge.performActionJson(
                        app,
                        "list",
                        JSONObject().put("limit", 40),
                    ),
                )
            }
            val entries = ArrayList<LocalMemoryEntry>()
            val array = list.optJSONArray("memories")
                ?: list.optJSONArray("entries")
                ?: list.optJSONArray("items")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    entries += LocalMemoryEntry(
                        id = item.optString("id"),
                        content = item.optString("content").ifBlank { item.optString("text") },
                        hitCount = item.optInt("hit_count", 0),
                        promoted = item.optBoolean("promoted", false) ||
                            item.optString("tier").equals("promoted", ignoreCase = true),
                    )
                }
            }
            val ok = status.optBoolean("success", false)
            _uiState.update {
                it.copy(
                    loading = false,
                    memoryCount = status.optInt("memory_count", entries.size),
                    reinforced = status.optInt("reinforced_memory_count", 0),
                    promoted = status.optInt("promoted_memory_count", 0),
                    statusLine = if (ok) {
                        "hy-memory local companion · ${status.optInt("memory_count", 0)} facts"
                    } else {
                        status.optString("error").ifBlank { "Memory status unavailable" }
                    },
                    entries = entries,
                    error = if (ok) "" else status.optString("error"),
                )
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                HermesHyMemoryBridge.performActionJson(getApplication(), "clear")
            }
            refresh()
        }
    }

    fun add(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                HermesHyMemoryBridge.performActionJson(
                    getApplication(),
                    "retain",
                    JSONObject().put("text", clean),
                )
            }
            refresh()
        }
    }

    fun delete(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                HermesHyMemoryBridge.performActionJson(
                    getApplication(),
                    "delete",
                    JSONObject().put("id", id),
                )
            }
            refresh()
        }
    }
}

@Composable
fun LocalMemorySection(
    modifier: Modifier = Modifier,
    viewModel: LocalMemoryViewModel = viewModel(),
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }
    var adding by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("HermesLocalMemorySection"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shell.memoryTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (uiState.loading && uiState.memoryCount == 0) "…" else shell.memoryCount(uiState.memoryCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.mutedText,
                    modifier = Modifier.testTag("HermesLocalMemoryStatus"),
                )
                Text(shell.memoryDescription, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            }
            ShellRoundButton(
                iconRes = if (adding) R.drawable.ic_action_close else R.drawable.ic_ui_plus,
                contentDescription = shell.memoryAdd,
                onClick = { adding = !adding },
                testTag = "MemoryAddButton",
            )
        }
        if (adding) {
            ShellGroupCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.fillMaxWidth().testTag("MemoryAddInput"),
                        placeholder = { Text(shell.memoryHint) },
                        minLines = 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = {
                            adding = false
                            draft = ""
                        }) { Text(shell.cancel) }
                        Button(
                            onClick = {
                                viewModel.add(draft)
                                draft = ""
                                adding = false
                            },
                            enabled = draft.isNotBlank(),
                            modifier = Modifier.testTag("MemoryAddSave"),
                        ) { Text(shell.save) }
                    }
                }
            }
        }
        if (uiState.error.isNotBlank()) {
            Text(uiState.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (uiState.entries.isEmpty() && !uiState.loading) {
            Text(
                text = shell.memoryEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.mutedText,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        uiState.entries.forEach { entry ->
            MemoryCard(entry = entry, onDelete = { viewModel.delete(entry.id) })
        }
        if (uiState.memoryCount > 0) {
            TextButton(
                onClick = { confirmClear = true },
                enabled = !uiState.loading,
                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("MemoryClearAll"),
            ) { Text(shell.memoryClearAll, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(shell.memoryClearConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearAll()
                }) { Text(shell.memoryClearAll, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(shell.cancel) } },
        )
    }
}

@Composable
private fun MemoryCard(entry: LocalMemoryEntry, onDelete: () -> Unit) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hermesGlassSurface(RoundedCornerShape(20.dp))
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp)
            .testTag("MemoryCard-${entry.id}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (entry.promoted) {
                    Text(
                        text = shell.memoryReinforced,
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.success,
                        modifier = Modifier
                            .background(glass.successContainer, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                if (entry.hitCount > 0) {
                    Text(shell.memoryUsed(entry.hitCount), style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
                }
            }
        }
        IconButton(
            onClick = onDelete,
            enabled = entry.id.isNotBlank(),
            modifier = Modifier.size(40.dp).testTag("MemoryDelete-${entry.id}"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_trash),
                contentDescription = shell.memoryDeleteDescription,
                tint = glass.mutedText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
