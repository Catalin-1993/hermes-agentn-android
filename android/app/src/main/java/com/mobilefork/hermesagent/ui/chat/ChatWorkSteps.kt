package com.mobilefork.hermesagent.ui.chat

import com.mobilefork.hermesagent.ui.i18n.ShellExtraStrings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.ShellStrings
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface

/** The room kept under the header for the work line, so nothing moves when it appears. */
internal val ChatWorkLineHeight = 36.dp

internal data class ChatWorkStep(
    val id: String,
    /** The tool's own name; blank for the agent's reasoning. */
    val tool: String,
    val detail: String,
    val running: Boolean,
)

/** What the agent did since the person's last message: each tool call joined to its result. */
internal fun workStepsOfLastTurn(messages: List<ChatUiMessage>): List<ChatWorkStep> {
    val turnStart = messages.indexOfLast { it.role == "user" } + 1
    val steps = mutableListOf<ChatWorkStep>()
    for (message in messages.subList(turnStart, messages.size)) {
        val title = message.content.substringBefore('\n').trim()
        val body = message.content.substringAfter('\n', "").trim()
        when (message.eventType) {
            AgentEventType.Thought -> {
                val previous = steps.lastOrNull()
                if (previous != null && previous.tool.isBlank()) {
                    steps[steps.lastIndex] = previous.copy(detail = body)
                } else {
                    steps += ChatWorkStep(message.id, "", body, running = false)
                }
            }
            AgentEventType.ToolCall -> steps += ChatWorkStep(message.id, title, body, running = true)
            AgentEventType.ToolResult, AgentEventType.FileAccess, AgentEventType.ProcessLog -> {
                val name = title.removeSuffix(" result").trim()
                val index = steps.indexOfLast { it.running && it.tool == name }
                    .takeIf { it >= 0 }
                    ?: steps.indexOfLast { it.running }
                if (index >= 0) {
                    val call = steps[index]
                    steps[index] = call.copy(running = false, detail = call.detail.ifBlank { body })
                } else {
                    steps += ChatWorkStep(message.id, name, body, running = false)
                }
            }
            else -> Unit
        }
    }
    return steps
}

private val WORK_STEP_LABELS: List<Pair<String, (ShellExtraStrings) -> String>> = listOf(
    "terminal" to ShellExtraStrings::stepTerminal,
    "mcp_run_in_proot" to ShellExtraStrings::stepTerminal,
    "linux_" to ShellExtraStrings::stepTerminal,
    "file_write" to ShellExtraStrings::stepWriteFile,
    "write_file" to ShellExtraStrings::stepWriteFile,
    "patch" to ShellExtraStrings::stepWriteFile,
    "read_file" to ShellExtraStrings::stepReadFile,
    "file_read" to ShellExtraStrings::stepReadFile,
    "search_files" to ShellExtraStrings::stepReadFile,
    "web_search" to ShellExtraStrings::stepSearchWeb,
    "web_" to ShellExtraStrings::stepBrowse,
    "browser" to ShellExtraStrings::stepBrowse,
    "android_device_diagnostics" to ShellExtraStrings::stepCheckPhone,
    "android_" to ShellExtraStrings::stepUsePhone,
    "open_gui" to ShellExtraStrings::stepUsePhone,
    "Native action" to ShellExtraStrings::stepUsePhone,
    "hy_memory" to ShellExtraStrings::stepMemory,
    "memory" to ShellExtraStrings::stepMemory,
    "skill" to ShellExtraStrings::stepSkills,
    "delegate" to ShellExtraStrings::stepDelegate,
    "schedule_" to ShellExtraStrings::stepSchedule,
    "list_tasks" to ShellExtraStrings::stepSchedule,
    "cancel_task" to ShellExtraStrings::stepSchedule,
)

internal fun workStepLabel(step: ChatWorkStep, shell: ShellStrings): String {
    if (step.tool.isBlank()) return shell.extra.stepThinking
    WORK_STEP_LABELS.firstOrNull { (prefix, _) -> step.tool.startsWith(prefix, ignoreCase = true) }
        ?.let { (_, label) -> return label(shell.extra) }
    return step.tool
        .removeSuffix("_tool")
        .replace('_', ' ')
        .trim()
        .replaceFirstChar { it.uppercase() }
}

/** A step's arguments or result as one quiet line, without JSON punctuation. */
internal fun workStepDetailLine(detail: String): String =
    detail.replace(Regex("""[{}\[\]"]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .take(140)

/**
 * The line under the agent's picture: what it is doing now, for how long, and with a tap, every step
 * of this turn. When the turn ends it stays as a quiet count until the next message.
 */
@Composable
internal fun ChatWorkIndicator(
    isSending: Boolean,
    elapsed: String,
    steps: List<ChatWorkStep>,
    speaker: AgentProfile?,
    turnKey: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(turnKey) { mutableStateOf(false) }
    val visible = isSending || steps.isNotEmpty()
    val current = steps.lastOrNull { it.running } ?: steps.lastOrNull()
    val canExpand = steps.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChatWorkLineHeight),
            contentAlignment = Alignment.Center,
        ) {
            WorkLinePill(
                visible = visible,
                isSending = isSending,
                speaker = speaker,
                current = current,
                stepCount = steps.size,
                elapsed = elapsed,
                expanded = expanded,
                canExpand = canExpand,
                onToggle = { expanded = !expanded },
            )
        }
        AnimatedVisibility(
            visible = expanded && canExpand,
            enter = fadeIn(tween(160)) + expandVertically(tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
        ) {
            WorkStepsPanel(steps = steps, isSending = isSending)
        }
    }
}

@Composable
private fun WorkLinePill(
    visible: Boolean,
    isSending: Boolean,
    speaker: AgentProfile?,
    current: ChatWorkStep?,
    stepCount: Int,
    elapsed: String,
    expanded: Boolean,
    canExpand: Boolean,
    onToggle: () -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(140)),
            ) {
                val pillShape = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .then(if (isSending) Modifier.hermesGlassSurface(pillShape, shadowRadius = 6.dp) else Modifier)
                        .clip(pillShape)
                        .then(if (canExpand) Modifier.clickable(onClick = onToggle) else Modifier)
                        .animateContentSize(tween(durationMillis = 160))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("HermesGenerationProgress"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        speaker != null && isSending -> AgentAvatar(agent = speaker, size = 18.dp)
                        isSending -> CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                    }
                    Text(
                        text = when {
                            speaker != null && isSending -> shell.agentTyping(speaker.name)
                            isSending && current != null -> "${workStepLabel(current, shell)} · $elapsed"
                            isSending -> shell.working(elapsed)
                            else -> shell.extra.stepsCount(stepCount)
                        },
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSending) MaterialTheme.colorScheme.onSurface else glass.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (canExpand) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_chevron_right),
                            contentDescription = if (expanded) shell.extra.stepsHide else shell.extra.stepsShow,
                            tint = glass.mutedText,
                            modifier = Modifier
                                .size(12.dp)
                                .rotate(if (expanded) -90f else 90f),
                        )
                    }
                }
            }
}

@Composable
private fun WorkStepsPanel(steps: List<ChatWorkStep>, isSending: Boolean) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .padding(top = 2.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .hermesGlassSurface(shape, shadowRadius = 10.dp)
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("HermesWorkSteps"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        steps.forEach { step ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    when {
                        step.running && isSending -> CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                        )
                        step.tool.isBlank() -> Box(
                            Modifier
                                .size(6.dp)
                                .background(glass.mutedText, CircleShape),
                        )
                        else -> Icon(
                            painter = painterResource(R.drawable.ic_ui_check),
                            contentDescription = null,
                            tint = glass.success,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        workStepLabel(step, shell),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val detail = workStepDetailLine(step.detail)
                    if (detail.isNotBlank()) {
                        Text(
                            detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
