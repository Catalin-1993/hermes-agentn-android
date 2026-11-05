package com.mobilefork.hermesagent.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings

@Composable
internal fun AppsScreen(
    bottomClearance: Dp,
    onOpenSection: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shell = LocalShellStrings.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShellPageMargin)
            .padding(top = 8.dp, bottom = bottomClearance)
            .testTag("HermesAppsScreen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShellPageTitle(title = shell.appsTitle)
        ShellGroupCard(modifier = Modifier.fillMaxWidth()) {
            val rows = listOf(
                AppRow(AppSection.Accounts, R.drawable.ic_app_people, shell.connectedAccountsTitle, shell.connectedAccountsBody),
                AppRow(AppSection.Device, R.drawable.ic_app_phone, shell.deviceTitle, shell.deviceBody),
                AppRow(AppSection.Terminal, R.drawable.ic_app_terminal, shell.terminalTitle, shell.terminalBody),
                AppRow(AppSection.NousPortal, R.drawable.ic_app_window, shell.portalTitle, shell.portalBody),
                AppRow(AppSection.Settings, R.drawable.ic_set_sliders, shell.settingsTitle, shell.settingsBody),
            )
            rows.forEachIndexed { index, row ->
                if (index > 0) ShellRowDivider(startInset = 54.dp)
                ShellListRow(
                    title = row.title,
                    subtitle = row.body,
                    modifier = Modifier.testTag("HermesNav${row.section.name}"),
                    onClick = { onOpenSection(row.section) },
                    leading = { ShellRowIcon(row.iconRes) },
                    trailing = { ShellChevron() },
                )
            }
        }
    }
}

private data class AppRow(
    val section: AppSection,
    val iconRes: Int,
    val title: String,
    val body: String,
)
