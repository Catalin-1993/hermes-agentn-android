package com.mobilefork.hermesagent.ui.privacy

import android.app.ActivityManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import com.mobilefork.hermesagent.ui.theme.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.mobilefork.hermesagent.ui.i18n.*

@Composable
fun LocalDataDeletionControl(strings: HermesStrings) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Text(
            strings.let { LocalPrivacyText.accountScope(it.language) },
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens.current.mutedText,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        com.mobilefork.hermesagent.ui.shell.ShellGroupCard {
            com.mobilefork.hermesagent.ui.shell.ShellListRow(
                title = LocalPrivacyText.delete(strings.language),
                titleStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.testTag("DeleteLocalAppData"),
                onClick = { confirm = true },
                leading = {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(com.mobilefork.hermesagent.R.drawable.ic_action_trash),
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }
        if (denied) Text(LocalPrivacyText.deletionDenied(strings.language))
    }
    if (confirm) AlertDialog(
        containerColor = opaquePrivacyDialogColor(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.testTag("DeleteLocalDataConfirmation"),
        onDismissRequest = { confirm = false },
        title = { Text(LocalPrivacyText.delete(strings.language)) },
        text = { Text(LocalPrivacyText.warning(strings.language)) },
        confirmButton = {
            TextButton(modifier = Modifier.testTag("ConfirmDeleteLocalAppData"), onClick = {
                confirm = false
                // No caller-controlled package or path: Android can clear only this application's own data.
                val accepted = runCatching {
                    context.getSystemService(ActivityManager::class.java).clearApplicationUserData()
                }.getOrDefault(false)
                denied = !accepted
            }) { Text(LocalPrivacyText.delete(strings.language)) }
        },
        dismissButton = {
            TextButton(modifier = Modifier.testTag("CancelDeleteLocalAppData"), onClick = { confirm = false }) {
                Text(strings.privacyText(PrivacyText.CANCEL))
            }
        },
    )
}
