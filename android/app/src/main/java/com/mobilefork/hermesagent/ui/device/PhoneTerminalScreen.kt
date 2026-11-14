package com.mobilefork.hermesagent.ui.device

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellActionItem
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellSegmentedTabs
import com.mobilefork.hermesagent.ui.terminal.TerminalScreen
import com.mobilefork.hermesagent.ui.terminal.TerminalViewModel

/** The phone's controls and its terminal on one page, switched by two tabs. */
@Composable
fun PhoneTerminalScreen(
    startOnTerminal: Boolean,
    extraBottomSpacing: Dp,
    onContextActionsChanged: (List<ShellActionItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shell = LocalShellStrings.current
    var tab by rememberSaveable { mutableIntStateOf(if (startOnTerminal) 1 else 0) }
    Column(modifier = modifier.fillMaxSize()) {
        ShellSegmentedTabs(
            options = listOf(shell.settingsSystem, shell.terminalTitle),
            selectedIndex = tab,
            onSelect = { tab = it },
            testTagPrefix = "HermesPhoneTerminalTab",
            modifier = Modifier.padding(horizontal = ShellPageMargin).padding(top = 8.dp),
        )
        Box(modifier = Modifier.fillMaxSize()) {
            if (tab == 0) {
                DeviceScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel<DeviceViewModel>(),
                    extraBottomSpacing = extraBottomSpacing,
                    onContextActionsChanged = onContextActionsChanged,
                )
            } else {
                androidx.compose.runtime.SideEffect { onContextActionsChanged(emptyList()) }
                TerminalScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel<TerminalViewModel>(),
                    extraBottomSpacing = extraBottomSpacing,
                )
            }
        }
    }
}
