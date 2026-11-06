package com.mobilefork.hermesagent.ui.shell

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import com.mobilefork.hermesagent.ui.theme.hermesPressScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.ShellStrings
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

internal val ShellPageMargin = 16.dp
internal val ShellTabBarHeight = 64.dp
internal val ShellTabBarGap = 12.dp
/** Room a page leaves at its bottom so the floating tab bar never covers its content. */
internal val ShellTabBarClearance = ShellTabBarHeight + ShellTabBarGap * 2

internal fun shellTabs(): List<AppSection> = listOf(
    AppSection.Hermes,
    AppSection.History,
    AppSection.Ideas,
    AppSection.Kanban,
    AppSection.Apps,
)

/** Everything that used to live in the side menu and is now opened from Apps. */
internal fun shellAppsDestinations(): List<AppSection> = listOf(
    AppSection.Accounts,
    AppSection.Device,
    AppSection.Terminal,
    AppSection.NousPortal,
    AppSection.Settings,
)

internal fun AppSection.shellTitle(shell: ShellStrings): String = when (this) {
    AppSection.Hermes -> shell.tabChat
    AppSection.History -> shell.tabHistory
    AppSection.Ideas -> shell.tabIdeas
    AppSection.Kanban -> shell.tabGoals
    AppSection.Apps -> shell.tabApps
    AppSection.Accounts -> shell.connectedAccountsTitle
    AppSection.Device -> shell.deviceTitle
    AppSection.Terminal -> shell.terminalTitle
    AppSection.NousPortal -> shell.portalTitle
    AppSection.Settings -> shell.settingsTitle
}

private fun AppSection.tabIcon(): Int = when (this) {
    AppSection.History -> R.drawable.ic_tab_history
    AppSection.Ideas -> R.drawable.ic_tab_ideas
    AppSection.Kanban -> R.drawable.ic_tab_goals
    AppSection.Apps -> R.drawable.ic_tab_apps
    else -> R.drawable.ic_tab_chat
}

@Composable
internal fun ShellTabBar(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalHermesGlassTokens.current
    val shell = LocalShellStrings.current
    val barShape = RoundedCornerShape(ShellTabBarHeight / 2)
    val tabs = shellTabs()
    Box(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .height(ShellTabBarHeight)
            .hermesGlassSurface(barShape, shadowRadius = 16.dp)
            .testTag("HermesShellTabBar"),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        ) {
            val tabWidth = maxWidth / tabs.size
            // The selection pill travels between tabs, so a switch reads as one motion.
            val pillOffset by animateDpAsState(
                targetValue = tabWidth * tabs.indexOf(selected).coerceAtLeast(0),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                label = "tabPill",
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(pillOffset.roundToPx(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(glass.tabSelected.copy(alpha = 0.72f)),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selected
                    val label = tab.shellTitle(shell)
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                role = Role.Tab,
                            ) { onSelect(tab) }
                            .semantics {
                                this.selected = isSelected
                                contentDescription = label
                            }
                            .testTag("HermesNav${tab.name}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(tab.tabIcon()),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .hermesPressScale(interaction, pressedScale = 0.86f)
                                .size(25.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShellRoundButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    testTag: String = "",
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .hermesPressScale(interaction, pressedScale = 0.92f)
            .hermesGlassSurface(CircleShape)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Header of a page opened from Apps: a round back button, then the page's title. */
@Composable
internal fun ShellSubpageHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ShellPageMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellRoundButton(
            iconRes = R.drawable.ic_ui_back,
            contentDescription = LocalShellStrings.current.back,
            onClick = onBack,
            testTag = "HermesShellBack",
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing()
    }
}

/** Title of a page reached from the tab bar, with its one action at the end of the row. */
@Composable
internal fun ShellPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing()
    }
}

@Composable
internal fun ShellGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hermesGlassSurface(shape)
            .clip(shape),
    ) { content() }
}

@Composable
internal fun ShellRowDivider(startInset: Dp = 0.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startInset),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** One row of a card or a list: leading piece, title, grey description, trailing piece. */
@Composable
internal fun ShellListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalHermesGlassTokens.current.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun ShellRowIcon(@DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
internal fun ShellChevron() {
    Icon(
        painter = painterResource(R.drawable.ic_ui_chevron_right),
        contentDescription = null,
        tint = LocalHermesGlassTokens.current.mutedText,
        modifier = Modifier.size(20.dp),
    )
}

/** The app's A mark in a circle: the avatar of the chat and of the settings profile. */
@Composable
internal fun ShellAgentAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val glass = LocalHermesGlassTokens.current
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        border = BorderStroke(1.dp, glass.border),
    ) {
        Image(
            painter = painterResource(R.drawable.agent_logo),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    }
}

/** The bottom space a page leaves for the floating tab bar. */
internal val ShellBottomClearance = ShellTabBarClearance + 4.dp

/**
 * Bottom padding for content that must stay above the tab bar and, while typing, above the
 * keyboard: the larger of the two, followed frame by frame in layout with no recomposition.
 */
@Composable
internal fun Modifier.shellAboveBarAndKeyboard(): Modifier = windowInsetsPadding(
    WindowInsets(bottom = ShellBottomClearance)
        .union(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
)

/** A row of two to four options in one glass pill; the chosen one sits on a lighter pill. */
@Composable
internal fun ShellSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "ShellTab",
) {
    val glass = LocalHermesGlassTokens.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hermesGlassSurface(shape, shadowRadius = 8.dp)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val fill by androidx.compose.animation.animateColorAsState(
                targetValue = if (selected) glass.tabSelected else Color.Transparent,
                label = "segmentFill",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(shape)
                    .background(fill)
                    .clickable(onClick = { onSelect(index) })
                    .semantics { this.selected = selected }
                    .testTag("$testTagPrefix$index"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else glass.mutedText,
                    maxLines = 1,
                )
            }
        }
    }
}
