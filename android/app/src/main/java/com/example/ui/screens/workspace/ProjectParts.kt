package com.example.ui.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.ui.shell.ShellBottomClearance
import com.mobilefork.hermesagent.ui.shell.ShellSegmentedTabs
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface

/**
 * The frame of a project's editing pages: a scrolling column of sections and, under it, one Save
 * button above the tab bar. The page's own header, with its back button, comes from the project screen.
 */
@Composable
internal fun ProjectEditPage(
    saveLabel: String,
    onSave: () -> Unit,
    saveTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = ShellBottomClearance + 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onSave)
                    .testTag(saveTag),
                contentAlignment = Alignment.Center,
            ) {
                Text(saveLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** A titled group: a small heading, an optional hint, and a card holding the fields. */
@Composable
internal fun ProjectSection(
    title: String,
    hint: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalHermesGlassTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hermesGlassSurface(RoundedCornerShape(22.dp), shadowRadius = 6.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun ProjectField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = if (minLines > 1) 12 else 1,
    placeholder: String = "",
    tag: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (tag.isNotBlank()) Modifier.testTag(tag) else Modifier),
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder, color = LocalHermesGlassTokens.current.mutedText) }),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = minLines == 1 && maxLines == 1,
        shape = RoundedCornerShape(16.dp),
    )
}

/** Two to four options in one row, the chosen one lit. */
@Composable
internal fun ProjectChoice(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tag: String,
) {
    ShellSegmentedTabs(
        options = options,
        selectedIndex = selectedIndex.coerceAtLeast(0),
        onSelect = onSelect,
        testTagPrefix = tag,
    )
}

@Composable
internal fun ProjectSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String,
) {
    val glass = LocalHermesGlassTokens.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = glass.mutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
