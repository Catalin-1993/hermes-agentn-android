package com.mobilefork.hermesagent.ui.kanban

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalConfiguration
import com.mobilefork.hermesagent.ui.shell.ShellChoiceChip
import com.mobilefork.hermesagent.ui.shell.ShellEntryField
import com.mobilefork.hermesagent.ui.shell.ShellEntrySheet
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import java.text.SimpleDateFormat
import java.util.Calendar
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.mobilefork.hermesagent.ui.theme.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mobilefork.hermesagent.ui.theme.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.data.KanbanBoardSnapshot
import com.mobilefork.hermesagent.data.KanbanBridge
import com.mobilefork.hermesagent.data.KanbanTask
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellPageTitle
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KanbanUiState(
    val loading: Boolean = true,
    val statusFilter: String = "all",
    val board: String = "default",
    val note: String = "",
    val error: String = "",
    val statusMessage: String = "",
    val counts: Map<String, Int> = emptyMap(),
    val tasks: List<KanbanTask> = emptyList(),
    val draftTitle: String = "",
    val draftBody: String = "",
)

class KanbanViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(KanbanUiState())
    val uiState: StateFlow<KanbanUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            val filter = _uiState.value.statusFilter
            val statusArg = filter.takeUnless { it == "all" }
            // Chaquopy can still be booting when the user opens Kanban early.
            var merged = KanbanBoardSnapshot(ok = false, error = "Waiting for Agent runtime…")
            repeat(12) { attempt ->
                val (ensure, snapshot) = withContext(Dispatchers.IO) {
                    val ensured = KanbanBridge.ensureBoard(getApplication())
                    ensured to KanbanBridge.listBoard(getApplication(), status = statusArg)
                }
                merged = if (snapshot.ok) {
                    snapshot.copy(note = ensure.note.ifBlank { snapshot.note })
                } else {
                    snapshot
                }
                if (merged.ok) return@repeat
                val waitingOnPython = merged.error.contains("Python runtime", ignoreCase = true)
                if (!waitingOnPython || attempt == 11) return@repeat
                _uiState.update {
                    it.copy(
                        loading = true,
                        statusMessage = "Waiting for Agent Python runtime…",
                        error = "",
                    )
                }
                kotlinx.coroutines.delay(1_500)
            }
            applySnapshot(merged, statusMessage = if (merged.ok) BOARD_REFRESHED_STATUS else merged.error)
        }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        refresh()
    }

    fun updateDraftTitle(value: String) {
        _uiState.update { it.copy(draftTitle = value) }
    }

    fun updateDraftBody(value: String) {
        _uiState.update { it.copy(draftBody = value) }
    }

    fun createTask() {
        val title = _uiState.value.draftTitle.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            val snapshot = withContext(Dispatchers.IO) {
                KanbanBridge.createTask(
                    getApplication(),
                    title = title,
                    body = _uiState.value.draftBody.trim(),
                )
            }
            if (snapshot.ok) {
                _uiState.update { it.copy(draftTitle = "", draftBody = "") }
            }
            applySnapshot(snapshot, statusMessage = if (snapshot.ok) "Task created" else snapshot.error)
        }
    }

    fun completeTask(taskId: String) {
        mutate { KanbanBridge.completeTask(getApplication(), taskId, summary = "Completed from Android Kanban") }
    }

    fun unblockTask(taskId: String) {
        mutate { KanbanBridge.unblockTask(getApplication(), taskId) }
    }

    fun commentTask(taskId: String, text: String) {
        if (text.isBlank()) return
        mutate { KanbanBridge.commentTask(getApplication(), taskId, text) }
    }

    private fun mutate(block: () -> KanbanBoardSnapshot) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = "") }
            val snapshot = withContext(Dispatchers.IO) { block() }
            applySnapshot(snapshot, statusMessage = if (snapshot.ok) "Updated" else snapshot.error)
        }
    }

    private fun applySnapshot(snapshot: KanbanBoardSnapshot, statusMessage: String) {
        _uiState.update {
            it.copy(
                loading = false,
                board = snapshot.board.ifBlank { it.board },
                note = snapshot.note.ifBlank {
                    "Shared SQLite board. Multi-agent workers still need gateway dispatch."
                },
                error = if (snapshot.ok) "" else snapshot.error.ifBlank { statusMessage },
                statusMessage = statusMessage,
                counts = snapshot.counts,
                // When create/complete returns only error snapshot without tasks, keep prior list.
                tasks = if (snapshot.ok || snapshot.tasks.isNotEmpty()) snapshot.tasks else it.tasks,
            )
        }
    }
}

internal enum class GoalGroup { Tracking, Planned, Done }

internal fun goalGroupOf(status: String): GoalGroup = when (status) {
    "running", "ready", "blocked" -> GoalGroup.Tracking
    "done", "archived" -> GoalGroup.Done
    else -> GoalGroup.Planned
}

private const val COLLAPSED_GOALS = 3
private const val BOARD_REFRESHED_STATUS = "Board refreshed"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KanbanScreen(
    modifier: Modifier = Modifier,
    extraBottomSpacing: Dp = 0.dp,
    viewModel: KanbanViewModel = viewModel(),
) {
    val strings = LocalHermesStrings.current
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val uiState by viewModel.uiState.collectAsState()
    var newGoalOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val grouped = uiState.tasks.groupBy { goalGroupOf(it.status) }
    val sections = listOf(
        Triple(GoalGroup.Tracking, shell.goalsTracking, glass.success to glass.successContainer),
        Triple(GoalGroup.Planned, shell.goalsPlanned, MaterialTheme.colorScheme.primary to glass.accentContainer),
        Triple(GoalGroup.Done, shell.goalsDone, glass.mutedText to MaterialTheme.colorScheme.surfaceVariant),
    ).filter { (group, _, _) -> grouped[group].orEmpty().isNotEmpty() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ShellPageMargin)
            .padding(top = 8.dp)
            .testTag("HermesKanbanScreen"),
    ) {
        ShellPageTitle(title = shell.goalsTitle) {
            ShellRoundButton(
                iconRes = R.drawable.ic_ui_plus,
                contentDescription = shell.newGoal,
                onClick = { newGoalOpen = true },
                testTag = "HermesKanbanNewButton",
            )
        }
        if (uiState.error.isNotBlank()) {
            Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.kanbanRuntimeText(uiState.error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                if (uiState.note.isNotBlank()) {
                    Text(
                        strings.kanbanRuntimeText(uiState.note),
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.mutedText,
                    )
                }
                TextButton(
                    onClick = viewModel::refresh,
                    enabled = !uiState.loading,
                    modifier = Modifier.testTag("HermesKanbanRetry"),
                ) { Text(shell.retry) }
            }
        } else if (uiState.statusMessage.isNotBlank() && uiState.statusMessage != BOARD_REFRESHED_STATUS) {
            Text(
                strings.kanbanRuntimeText(uiState.statusMessage),
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = glass.mutedText,
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("HermesKanbanTaskList"),
            contentPadding = PaddingValues(top = 8.dp, bottom = extraBottomSpacing),
        ) {
            if (uiState.tasks.isEmpty()) {
                item {
                    if (uiState.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            shell.goalsEmpty,
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.mutedText,
                        )
                    }
                }
            }
            sections.forEachIndexed { index, (group, title, colors) ->
                item(key = "goals-section-$group") {
                    GoalSection(
                        title = title,
                        accent = colors.first,
                        halo = colors.second,
                        tasks = grouped[group].orEmpty(),
                        enabled = !uiState.loading,
                        showDivider = index > 0,
                        onComplete = viewModel::completeTask,
                        onUnblock = viewModel::unblockTask,
                        onComment = viewModel::commentTask,
                    )
                }
            }
        }
    }

    if (newGoalOpen) {
        var deadline by rememberSaveable { mutableStateOf(GoalDeadline.None) }
        val locale = LocalConfiguration.current.locales[0]
        ShellEntrySheet(
            title = shell.newGoal,
            onDismiss = { newGoalOpen = false },
            testTag = "HermesKanbanNewGoalSheet",
        ) {
            ShellGroupCard {
                ShellEntryField(
                    value = uiState.draftTitle,
                    onValueChange = viewModel::updateDraftTitle,
                    placeholder = shell.extra.goalNamePrompt,
                    testTag = "HermesKanbanTitleInput",
                    primary = true,
                    autoFocus = true,
                )
                ShellRowDivider(startInset = 16.dp)
                ShellEntryField(
                    value = uiState.draftBody,
                    onValueChange = viewModel::updateDraftBody,
                    placeholder = shell.extra.goalDetailsPrompt,
                    testTag = "HermesKanbanBodyInput",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    shell.extra.goalDeadlineTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.mutedText,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GoalDeadline.entries.forEach { choice ->
                        ShellChoiceChip(
                            label = when (choice) {
                                GoalDeadline.None -> shell.extra.goalDeadlineNone
                                GoalDeadline.Today -> shell.extra.goalDeadlineToday
                                GoalDeadline.Week -> shell.extra.goalDeadlineWeek
                                GoalDeadline.Month -> shell.extra.goalDeadlineMonth
                            },
                            selected = deadline == choice,
                            onClick = { deadline = choice },
                            testTag = "HermesGoalDeadline_${choice.name}",
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val line = goalDeadlineDate(deadline, Calendar.getInstance())?.let { day ->
                        shell.extra.goalDeadlineLine(SimpleDateFormat("d MMM yyyy", locale).format(day.time))
                    }
                    viewModel.updateDraftBody(goalBodyWithDeadline(uiState.draftBody, line))
                    viewModel.createTask()
                    newGoalOpen = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("HermesKanbanCreateButton"),
                enabled = !uiState.loading && uiState.draftTitle.isNotBlank(),
            ) {
                Text(shell.createGoal)
            }
        }
    }
}

internal enum class GoalDeadline { None, Today, Week, Month }

/** The last day a choice allows: this Sunday for a week, the month's last day for a month. */
internal fun goalDeadlineDate(choice: GoalDeadline, today: Calendar): Calendar? {
    val day = today.clone() as Calendar
    return when (choice) {
        GoalDeadline.None -> null
        GoalDeadline.Today -> day
        GoalDeadline.Week -> day.apply {
            val daysToSunday = (Calendar.SUNDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
            add(Calendar.DAY_OF_MONTH, daysToSunday)
        }
        GoalDeadline.Month -> day.apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
    }
}

internal fun goalBodyWithDeadline(details: String, deadlineLine: String?): String =
    listOfNotNull(details.trim().takeIf { it.isNotBlank() }, deadlineLine).joinToString("\n")

@Composable
private fun GoalSection(
    title: String,
    accent: Color,
    halo: Color,
    tasks: List<KanbanTask>,
    enabled: Boolean,
    showDivider: Boolean,
    onComplete: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onComment: (String, String) -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val visible = if (expanded) tasks else tasks.take(COLLAPSED_GOALS)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            ShellRowDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(halo, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accent, CircleShape),
                )
            }
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
        }
        visible.forEach { task ->
            GoalRow(
                task = task,
                enabled = enabled,
                onComplete = { onComplete(task.id) },
                onUnblock = { onUnblock(task.id) },
                onComment = { onComment(task.id, it) },
            )
        }
        if (tasks.size > COLLAPSED_GOALS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_dots_grid),
                    contentDescription = null,
                    tint = glass.mutedText,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = if (expanded) shell.showLess else shell.showMore(tasks.size - COLLAPSED_GOALS),
                    style = MaterialTheme.typography.bodyLarge,
                    color = glass.mutedText,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GoalRow(
    task: KanbanTask,
    enabled: Boolean,
    onComplete: () -> Unit,
    onUnblock: () -> Unit,
    onComment: (String) -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val isDone = goalGroupOf(task.status) == GoalGroup.Done
    var menuOpen by rememberSaveable(task.id) { mutableStateOf(false) }
    var commentOpen by rememberSaveable(task.id) { mutableStateOf(false) }
    var commentDraft by rememberSaveable(task.id) { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("HermesKanbanTask_${task.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val boxShape = RoundedCornerShape(7.dp)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(boxShape)
                .background(if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(2.dp, if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, boxShape)
                .clickable(enabled = enabled && !isDone, role = Role.Checkbox, onClick = onComplete)
                .semantics {
                    contentDescription = shell.completeGoal
                    selected = isDone
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    task.body.isBlank() -> shell.goalStatus(task.status)
                    task.status == "blocked" -> "${shell.goalStatus(task.status)} · ${task.body.trim()}"
                    else -> task.body.trim()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = glass.mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_more_vert),
                    contentDescription = shell.more,
                    tint = glass.mutedText,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            listOf(shell.goalStatus(task.status), task.id, task.assignee)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    enabled = false,
                    onClick = {},
                )
                if (!isDone) {
                    DropdownMenuItem(
                        text = { Text(shell.completeGoal) },
                        enabled = enabled,
                        onClick = { menuOpen = false; onComplete() },
                    )
                }
                if (task.status == "blocked") {
                    DropdownMenuItem(
                        text = { Text(shell.unblockGoal) },
                        enabled = enabled,
                        onClick = { menuOpen = false; onUnblock() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(shell.commentGoal) },
                    onClick = { menuOpen = false; commentOpen = true },
                )
            }
        }
    }
    if (commentOpen) {
        AlertDialog(
            onDismissRequest = { commentOpen = false },
            title = { Text(shell.commentGoal) },
            text = {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = enabled && commentDraft.isNotBlank(),
                    onClick = {
                        onComment(commentDraft)
                        commentDraft = ""
                        commentOpen = false
                    },
                ) { Text(shell.addComment) }
            },
            dismissButton = {
                TextButton(onClick = { commentOpen = false }) { Text(shell.cancel) }
            },
        )
    }
}
