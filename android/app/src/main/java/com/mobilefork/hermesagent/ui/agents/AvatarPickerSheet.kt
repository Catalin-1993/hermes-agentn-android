package com.mobilefork.hermesagent.ui.agents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AVATAR_INITIALS
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AvatarChoice
import com.mobilefork.hermesagent.data.AvatarPreset
import com.mobilefork.hermesagent.data.avatarChoiceOf
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

private const val AVATAR_COLUMNS = 4

/**
 * Choosing how an agent looks: the eight characters, then a photo, initials, or the automatic
 * look. A tap on a character applies it and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvatarPickerSheet(
    agent: AgentProfile,
    onChoose: (String) -> Unit,
    /** Null where a picture cannot be kept, such as the setup guide. */
    onPickPhoto: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val shell = LocalShellStrings.current
    val current = avatarChoiceOf(agent.avatarPath)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)
                .testTag("AvatarPickerSheet"),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(shell.extra.avatarTitle, style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AvatarPreset.entries.chunked(AVATAR_COLUMNS).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { preset ->
                            PresetTile(
                                preset = preset,
                                selected = (current as? AvatarChoice.Preset)?.preset == preset,
                                onClick = {
                                    onChoose(preset.storedValue)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            ShellGroupCard {
                if (onPickPhoto != null) AvatarOptionRow(
                    label = shell.extra.avatarPhoto,
                    selected = current is AvatarChoice.Photo,
                    testTag = "AgentPickPhoto",
                    onClick = {
                        onDismiss()
                        onPickPhoto()
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, LocalHermesGlassTokens.current.border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (onPickPhoto != null) ShellRowDivider(startInset = 64.dp)
                AvatarOptionRow(
                    label = shell.extra.avatarInitials,
                    selected = current is AvatarChoice.Initials,
                    testTag = "AvatarInitials",
                    onClick = {
                        onChoose(AVATAR_INITIALS)
                        onDismiss()
                    },
                ) {
                    AgentAvatar(agent = agent.copy(avatarPath = AVATAR_INITIALS), size = 36.dp)
                }
                ShellRowDivider(startInset = 64.dp)
                AvatarOptionRow(
                    label = shell.extra.avatarAutomatic,
                    selected = current is AvatarChoice.Generated,
                    testTag = "AvatarAutomatic",
                    onClick = {
                        onChoose("")
                        onDismiss()
                    },
                ) {
                    AgentAvatar(agent = agent.copy(avatarPath = ""), size = 36.dp)
                }
            }
        }
    }
}

@Composable
private fun PresetTile(
    preset: AvatarPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 6.dp)
            .testTag("AvatarPreset_${preset.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(2.5.dp, ring),
            color = androidx.compose.ui.graphics.Color.Transparent,
        ) {
            Box(modifier = Modifier.padding(3.dp)) {
                androidx.compose.foundation.Image(
                    painter = painterResource(preset.drawable),
                    contentDescription = preset.displayName,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape),
                )
            }
        }
        Text(
            preset.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else LocalHermesGlassTokens.current.mutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AvatarOptionRow(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leading()
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Spacer(Modifier.size(18.dp))
        }
    }
}
