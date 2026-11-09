package com.mobilefork.hermesagent.ui.chat

import com.mobilefork.hermesagent.ui.i18n.ShellExtraStrings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.wrapContentHeight
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

/** Each step stays on screen at least this long, so the work reads as calm motion even when it is quick. */
internal const val WORK_STEP_MIN_MILLIS = 2_000L
private const val WORK_TICK_MILLIS = 80L

/** The steps shown so far in a turn, and whether the work card is still up. */
internal class PacedWork(val shown: List<ChatWorkStep>, val live: Boolean)

/**
 * Lets the steps of a turn appear one at a time, each held for [minShowMillis], however fast the work
 * behind them goes. Steps already finished when a conversation is opened are never replayed.
 */
@Composable
internal fun rememberPacedWork(
    steps: List<ChatWorkStep>,
    isSending: Boolean,
    turnKey: String,
    minShowMillis: Long = WORK_STEP_MIN_MILLIS,
): PacedWork {
    val latest by rememberUpdatedState(steps to isSending)
    var shownCount by remember(turnKey) { mutableIntStateOf(0) }
    var live by remember(turnKey) { mutableStateOf(false) }
    LaunchedEffect(turnKey) {
        var sendingSeen = false
        var placeholder = false
        var changedAt = 0L
        // Time is counted in the loop's own ticks, which is what the coroutine clock moves.
        var now = 0L
        while (true) {
            val (all, sending) = latest
            if (sending) sendingSeen = true
            if (sendingSeen) {
                val held = now - changedAt >= minShowMillis
                when {
                    shownCount < all.size && (held || (shownCount == 0 && !placeholder)) -> {
                        shownCount += 1
                        changedAt = now
                        live = true
                    }
                    shownCount == 0 && !placeholder && sending -> {
                        placeholder = true
                        changedAt = now
                        live = true
                    }
                    live && !sending && shownCount >= all.size && held -> live = false
                }
            }
            delay(WORK_TICK_MILLIS)
            now += WORK_TICK_MILLIS
        }
    }
    return PacedWork(steps.take(shownCount), live)
}

/**
 * The work card under the agent's picture: the last steps of this turn as a short timeline, the one in
 * progress marked. Once the turn is over it settles into a quiet count that opens the full list.
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
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val paced = rememberPacedWork(steps, isSending && speaker == null, turnKey)
    var expanded by rememberSaveable(turnKey) { mutableStateOf(false) }
    val canExpand = steps.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChatWorkLineHeight)
                // The card may grow past the room kept for it; it floats over the page below.
                .wrapContentHeight(align = Alignment.Top, unbounded = true),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (speaker != null && isSending) {
                // In a group the card says who writes.
                Row(
                    modifier = Modifier
                        .hermesGlassSurface(RoundedCornerShape(50), shadowRadius = 6.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("HermesGenerationProgress"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AgentAvatar(agent = speaker, size = 18.dp)
                    Text(shell.agentTyping(speaker.name), style = MaterialTheme.typography.labelMedium)
                    TypingDots(dotSize = 5.dp)
                }
            } else {
                PlainVisibility(
                    visible = !paced.live && canExpand,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(140)),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("HermesGenerationProgress"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(shell.extra.stepsCount(steps.size), style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_chevron_right),
                            contentDescription = if (expanded) shell.extra.stepsHide else shell.extra.stepsShow,
                            tint = glass.mutedText,
                            modifier = Modifier.size(12.dp).rotate(if (expanded) -90f else 90f),
                        )
                    }
                }
                PlainVisibility(
                    visible = paced.live,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(260)) + shrinkVertically(tween(260)),
                ) {
                    WorkTimeline(shown = paced.shown, working = isSending, elapsed = elapsed)
                }
            }
        }
        AnimatedVisibility(
            visible = expanded && canExpand && !paced.live,
            enter = fadeIn(tween(160)) + expandVertically(tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
        ) {
            WorkStepsPanel(steps = steps, isSending = false)
        }
    }
}

/** AnimatedVisibility without the layout scope the call site happens to be in. */
@Composable
private fun PlainVisibility(
    visible: Boolean,
    enter: androidx.compose.animation.EnterTransition,
    exit: androidx.compose.animation.ExitTransition,
    content: @Composable androidx.compose.animation.AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = enter, exit = exit, content = content)
}

/** The last three steps, newest at the bottom: finished ones ticked, the one in progress alive. */
@Composable
private fun WorkTimeline(shown: List<ChatWorkStep>, working: Boolean, elapsed: String) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val shape = RoundedCornerShape(20.dp)
    val rows = shown.takeLast(3)
    Column(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .hermesGlassSurface(shape, shadowRadius = 8.dp)
            .clip(shape)
            .animateContentSize(tween(220))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("HermesGenerationProgress"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (rows.isEmpty()) {
            TimelineRow(
                key = "thinking",
                label = shell.extra.stepThinking,
                detail = "",
                state = TimelineState.Active,
                trailing = elapsed,
                dots = true,
            )
        }
        rows.forEachIndexed { index, step ->
            val last = index == rows.lastIndex
            TimelineRow(
                key = step.id,
                label = workStepLabel(step, shell),
                detail = workStepDetailLine(step.detail).take(44),
                state = if (last && working) TimelineState.Active else TimelineState.Done,
                trailing = if (last && working) elapsed else "",
                dots = false,
            )
        }
    }
}

private enum class TimelineState { Active, Done }

@Composable
private fun TimelineRow(
    key: String,
    label: String,
    detail: String,
    state: TimelineState,
    trailing: String,
    dots: Boolean,
) {
    val glass = LocalHermesGlassTokens.current
    val appear = remember(key) { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = appear,
        enter = fadeIn(tween(240)) + expandVertically(tween(240)),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                when (state) {
                    TimelineState.Active -> PulseDot()
                    TimelineState.Done -> Icon(
                        painter = painterResource(R.drawable.ic_ui_check),
                        contentDescription = null,
                        tint = glass.success,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (state == TimelineState.Active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (state == TimelineState.Active) MaterialTheme.colorScheme.onSurface else glass.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (dots) TypingDots(dotSize = 4.dp)
                }
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing.isNotBlank()) {
                Text(trailing, style = MaterialTheme.typography.labelSmall, color = glass.mutedText)
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
