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
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.i18n.LocalChatStrings
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
    /** What the tool answered, once it has; the plan reads its list from here. */
    val result: String = "",
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
                    steps[index] = call.copy(running = false, detail = call.detail.ifBlank { body }, result = body)
                } else {
                    steps += ChatWorkStep(message.id, name, body, running = false)
                }
            }
            else -> Unit
        }
    }
    return steps
}


/** One line of the agent's plan, as it wrote it with its to-do tool. */
internal enum class PlanState { Pending, Active, Done, Cancelled }

internal data class PlanItem(val text: String, val state: PlanState)

internal data class WorkPlan(val items: List<PlanItem>) {
    /** Cancelled items no longer count towards the whole. */
    val total: Int get() = items.count { it.state != PlanState.Cancelled }
    val done: Int get() = items.count { it.state == PlanState.Done }
    val current: PlanItem? get() = items.firstOrNull { it.state == PlanState.Active } ?: items.firstOrNull { it.state == PlanState.Pending }
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** The agent's to-do tool: it keeps the plan, so its calls are drawn as the plan and not as activity. */
internal fun isPlanStep(step: ChatWorkStep): Boolean = step.tool.removeSuffix("_tool").equals("todo_list", ignoreCase = true)

/** The newest plan the agent wrote: its latest answer lists every item, so that is read before the call. */
internal fun workPlanOf(steps: List<ChatWorkStep>): WorkPlan? {
    for (step in steps.asReversed()) {
        if (!isPlanStep(step)) continue
        for (text in listOf(step.result, step.detail)) {
            parsePlan(text)?.let { return it }
        }
    }
    return null
}

internal fun parsePlan(text: String): WorkPlan? {
    val json = text.trim().takeIf { it.startsWith("{") } ?: return null
    val list = runCatching { org.json.JSONObject(json).optJSONArray("todos") }.getOrNull() ?: return null
    val items = buildList {
        for (index in 0 until list.length()) {
            val item = list.optJSONObject(index) ?: continue
            val content = item.optString("content").trim()
            if (content.isBlank()) continue
            val state = when (item.optString("status").lowercase()) {
                "in_progress" -> PlanState.Active
                "completed" -> PlanState.Done
                "cancelled", "canceled" -> PlanState.Cancelled
                else -> PlanState.Pending
            }
            add(PlanItem(content, state))
        }
    }
    return items.takeIf { it.isNotEmpty() }?.let(::WorkPlan)
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
 * What the agent is doing, in one calm line under its picture. With a plan it names the item in progress
 * and how far along the plan is; without one it names the step it is on. Tapping it opens the plan and
 * the steps taken; once the turn is over it settles into a quiet count that opens the same list.
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
    val plan = remember(steps) { workPlanOf(steps) }
    val activity = remember(steps) { steps.filterNot(::isPlanStep) }
    val canExpand = plan != null || activity.isNotEmpty()

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
                        Text(
                            text = if (plan != null) {
                                LocalChatStrings.current.workProgress.format(plan.done, plan.total)
                            } else {
                                shell.extra.stepsCount(activity.size)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.mutedText,
                        )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        WorkLine(
                            shown = paced.shown.filterNot(::isPlanStep),
                            plan = plan,
                            working = isSending,
                            elapsed = elapsed,
                            expanded = expanded,
                            onToggle = if (canExpand) ({ expanded = !expanded }) else null,
                        )
                        AnimatedVisibility(
                            visible = expanded && canExpand,
                            enter = fadeIn(tween(160)) + expandVertically(tween(180)),
                            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
                        ) {
                            WorkDetails(plan = plan, activity = activity, isSending = isSending)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = expanded && canExpand && !paced.live,
            enter = fadeIn(tween(160)) + expandVertically(tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
        ) {
            WorkDetails(plan = plan, activity = activity, isSending = false)
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

/**
 * The live line: one quiet row with what the agent is doing right now, the time so far and a chevron. No
 * card and no steps until it is tapped; then the plan and the steps open under it.
 */
@Composable
private fun WorkLine(
    shown: List<ChatWorkStep>,
    plan: WorkPlan?,
    working: Boolean,
    elapsed: String,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val latest = shown.lastOrNull()
    // What it is doing: the step in hand with its detail, else the plan item, else plain thinking.
    val headline = latest?.let { step ->
        val detail = workStepDetailLine(step.detail).take(40)
        if (detail.isBlank()) workStepLabel(step, shell) else "${workStepLabel(step, shell)}  ${detail}"
    } ?: plan?.current?.text ?: shell.extra.stepThinking
    Row(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("HermesGenerationProgress"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            if (working) PulseDot() else Icon(
                painter = painterResource(R.drawable.ic_ui_check),
                contentDescription = null,
                tint = glass.success,
                modifier = Modifier.size(16.dp),
            )
        }
        androidx.compose.animation.Crossfade(
            targetState = headline,
            animationSpec = tween(220),
            label = "workLine",
            modifier = Modifier.weight(1f, fill = false),
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (elapsed.isNotBlank()) {
            Text(elapsed, style = MaterialTheme.typography.labelMedium, color = glass.mutedText)
        }
        if (onToggle != null) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_chevron_right),
                contentDescription = if (expanded) shell.extra.stepsHide else shell.extra.stepsShow,
                tint = glass.mutedText,
                modifier = Modifier.size(12.dp).rotate(if (expanded) -90f else 90f),
            )
        }
    }
}

/** The plan, with how far it has come, and under it what the agent actually did. */
@Composable
private fun WorkDetails(plan: WorkPlan?, activity: List<ChatWorkStep>, isSending: Boolean) {
    val shell = LocalShellStrings.current
    val words = LocalChatStrings.current
    val glass = LocalHermesGlassTokens.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .padding(top = 6.dp, bottom = 8.dp)
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, glass.mutedText.copy(alpha = 0.22f), shape)
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("HermesWorkSteps"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (plan != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        words.workPlan,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.mutedText,
                    )
                    Text(
                        words.workProgress.format(plan.done, plan.total),
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.mutedText,
                        modifier = Modifier.testTag("HermesWorkPlanProgress"),
                    )
                }
                plan.items.forEach { item -> PlanRow(item) }
            }
        }
        if (activity.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    words.workActivity,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = glass.mutedText,
                )
                activity.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                            when {
                                step.running && isSending -> CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.5.dp)
                                step.tool.isBlank() -> Box(Modifier.size(5.dp).background(glass.mutedText, CircleShape))
                                else -> Icon(
                                    painter = painterResource(R.drawable.ic_ui_check),
                                    contentDescription = null,
                                    tint = glass.success,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                workStepLabel(step, shell),
                                style = MaterialTheme.typography.bodySmall,
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
    }
}

/** A plan item: done is a ticked circle, the one in progress a ring that breathes, the rest an empty ring. */
@Composable
private fun PlanRow(item: PlanItem) {
    val glass = LocalHermesGlassTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 2.dp).size(16.dp), contentAlignment = Alignment.Center) {
            when (item.state) {
                PlanState.Done -> Icon(
                    painter = painterResource(R.drawable.ic_ui_check),
                    contentDescription = null,
                    tint = glass.success,
                    modifier = Modifier.size(16.dp),
                )
                PlanState.Active -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                else -> {
                    // An empty ring drawn as dashes: not started.
                    val ring = glass.mutedText.copy(alpha = if (item.state == PlanState.Cancelled) 0.3f else 0.6f)
                    Box(
                        Modifier
                            .size(14.dp)
                            .drawBehind {
                                drawCircle(
                                    color = ring,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 4f)),
                                    ),
                                )
                            },
                    )
                }
            }
        }
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (item.state == PlanState.Active) FontWeight.SemiBold else FontWeight.Normal,
            color = when (item.state) {
                PlanState.Active -> MaterialTheme.colorScheme.onSurface
                PlanState.Done -> glass.mutedText
                PlanState.Cancelled -> glass.mutedText.copy(alpha = 0.6f)
                PlanState.Pending -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            },
            textDecoration = if (item.state == PlanState.Cancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
    }
}
