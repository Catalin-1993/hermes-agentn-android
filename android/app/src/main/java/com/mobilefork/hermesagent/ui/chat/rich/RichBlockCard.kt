package com.mobilefork.hermesagent.ui.chat.rich

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

private val RichCardShape = RoundedCornerShape(24.dp)

/**
 * One drawn block of a reply, in the same card as the agent's bubbles. While it is still being
 * written it shows that it is drawing; a half-written program never runs. Whatever cannot be drawn
 * shows its source, so nothing the agent wrote is ever lost.
 */
@Composable
internal fun RichBlockCard(
    block: RichSegment.Block,
    onSendPrompt: (String) -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalHermesGlassTokens.current
    val shell = LocalShellStrings.current
    val blockKey = remember(block.code, block.language) { "${block.language.fence}:${block.code.hashCode()}" }
    var showCode by rememberSaveable(blockKey) { mutableStateOf(false) }
    var renderFailed by rememberSaveable(blockKey) { mutableStateOf(false) }
    var fullScreen by rememberSaveable(blockKey) { mutableStateOf(false) }
    val chart = remember(block.code, block.language, block.closed) {
        if (block.closed && block.language == RichLanguage.Chart) parseChartSpec(block.code) else null
    }
    val cannotDraw = renderFailed || chart is ChartParse.Refused
    val drawsInFrame = block.closed && block.language != RichLanguage.Chart

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(durationMillis = 160))
            .testTag("HermesRichBlock_${block.language.fence}"),
        color = glass.bubbleAgent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RichCardShape,
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when {
                !block.closed -> DrawingPlaceholder()
                showCode || cannotDraw -> {
                    if (cannotDraw) {
                        Text(
                            shell.extra.richCouldNotDraw,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.mutedText,
                        )
                    }
                    RichCodeView(block.code)
                }
                chart is ChartParse.Ok -> RichChart(chart.spec)
                drawsInFrame -> RichWidgetFrame(
                    language = block.language,
                    code = block.code,
                    onSendPrompt = onSendPrompt,
                    onRenderFailed = { renderFailed = true },
                )
            }
            if (block.closed) {
                // Pulled out by its own padding, so the words line up with the content above.
                Row(
                    modifier = Modifier.offset(x = (-8).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!cannotDraw) {
                        RichFooterAction(
                            label = if (showCode) shell.extra.richHideCode else shell.extra.richShowCode,
                            testTag = "HermesRichShowCode",
                            onClick = { showCode = !showCode },
                        )
                    }
                    if (drawsInFrame && !cannotDraw && !showCode) {
                        RichFooterAction(
                            label = shell.extra.richFullScreen,
                            testTag = "HermesRichFullScreen",
                            onClick = { fullScreen = true },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_more_horiz),
                        contentDescription = shell.messageActions,
                        tint = glass.mutedText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenActions)
                            .padding(8.dp)
                            .size(18.dp)
                            .testTag("HermesRichBlockActions"),
                    )
                }
            }
        }
    }
    if (fullScreen) {
        RichWidgetFullScreen(
            language = block.language,
            code = block.code,
            onSendPrompt = onSendPrompt,
            onDismiss = { fullScreen = false },
        )
    }
}

@Composable
private fun RichFooterAction(label: String, testTag: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(testTag),
        style = MaterialTheme.typography.labelMedium,
        color = LocalHermesGlassTokens.current.mutedText,
    )
}

@Composable
private fun DrawingPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(bottom = 8.dp)
            .testTag("HermesRichDrawing"),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        Text(
            LocalShellStrings.current.extra.richDrawing,
            style = MaterialTheme.typography.labelMedium,
            color = LocalHermesGlassTokens.current.mutedText,
        )
    }
}

@Composable
private fun RichCodeView(code: String) {
    SelectionContainer {
        Text(
            text = code,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
                .testTag("HermesRichCode"),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}
