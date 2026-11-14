@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mobilefork.hermesagent.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.mobilefork.hermesagent.ui.theme.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.shell.ShellChevron
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import com.mobilefork.hermesagent.ui.shell.ShellActionItem
import com.mobilefork.hermesagent.ui.theme.hermesPanelColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
    extraBottomSpacing: Dp = 0.dp,
    onOpenSettings: () -> Unit = {},
    onContextActionsChanged: (List<ShellActionItem>) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalHermesStrings.current
    val scrollState = rememberScrollState()

    LaunchedEffect(strings.language) {
        viewModel.refresh()
    }

    SideEffect {
        val actions = buildList {
            add(
                ShellActionItem(
                    label = strings.refresh.ifBlank { "Refresh" },
                    description = strings.authRefreshDescription(),
                    iconRes = R.drawable.ic_action_refresh,
                    onClick = viewModel::refresh,
                )
            )
            if (uiState.hasPendingRequest) {
                add(
                    ShellActionItem(
                        label = strings.cancelPendingSignIn(),
                        description = strings.authCancelPendingDescription(),
                        iconRes = R.drawable.ic_nav_settings,
                        onClick = viewModel::cancelPendingRequest,
                    )
                )
            }
        }
        onContextActionsChanged(actions)
    }

    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    var openOptionId by rememberSaveable { mutableStateOf<String?>(null) }
    val matching = uiState.options.filter {
        query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) ||
            it.description.contains(query.trim(), ignoreCase = true)
    }
    val connected = matching.filter { it.signedIn }
    val available = matching.filterNot { it.signedIn }
    Surface(modifier = modifier.fillMaxSize(), color = hermesPanelColor()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = ShellPageMargin)
                    .padding(top = 8.dp, bottom = extraBottomSpacing),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountNotices(uiState = uiState, viewModel = viewModel, onOpenSettings = onOpenSettings)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountsSectionTitle(shell.accountsConnected, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) query = ""
                        },
                        modifier = Modifier.testTag("AuthSearchToggle"),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_search),
                            contentDescription = shell.accountsSearch,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                if (searchOpen) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("AuthSearchInput"),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        placeholder = { Text(shell.accountsSearch) },
                    )
                }
                if (connected.isEmpty() && query.isBlank()) {
                    Text(
                        shell.accountsNoneConnected,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.mutedText,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        connected.forEach { option ->
                            Column(
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable { openOptionId = option.id }
                                    .testTag("AuthConnected-${option.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ConnectorLogo(id = option.id, label = option.label)
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                AccountsSectionTitle(
                    shell.accountsAvailable,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (matching.isEmpty()) {
                    Text(
                        shell.accountsNoResults,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.mutedText,
                    )
                }
                available.forEach { option ->
                    ShellListRow(
                        title = option.label,
                        subtitle = option.description,
                        modifier = Modifier.testTag("AuthOption-${option.id}"),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        onClick = { openOptionId = option.id },
                        leading = { ConnectorLogo(id = option.id, label = option.label) },
                        trailing = {
                            Icon(
                                painter = painterResource(R.drawable.ic_ui_plus),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
                ShellRowDivider()
                ShellListRow(
                    title = shell.accountsAdvanced,
                    modifier = Modifier.testTag("AuthAdvancedToggle"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    onClick = { advancedOpen = !advancedOpen },
                    trailing = { ShellChevron() },
                )
                if (advancedOpen) {
                    Text(strings.authIntro, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                OutlinedTextField(
                    value = uiState.corr3xtBaseUrl,
                    onValueChange = viewModel::updateCorr3xtBaseUrl,
                    label = { Text(strings.corr3xtAuthBaseUrl.ifBlank { "Corr3xt auth base URL" }) },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = viewModel::saveCorr3xtBaseUrl) {
                        Text(strings.saveAuthUrl.ifBlank { "Save auth URL" })
                    }
                    Button(onClick = viewModel::refresh) {
                        Text(strings.refresh.ifBlank { "Refresh" })
                    }
                }

                }
            }
        }
    }

    val openOption = uiState.options.firstOrNull { it.id == openOptionId }
    if (openOption != null) {
        ModalBottomSheet(onDismissRequest = { openOptionId = null }) {
            AccountOptionControls(
                uiState = uiState,
                option = openOption,
                viewModel = viewModel,
                onOpenSettings = {
                    openOptionId = null
                    onOpenSettings()
                },
            )
        }
    }
}

@Composable
private fun AccountNotices(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalHermesStrings.current
    if (uiState.globalStatus.isNotBlank()) {
        SelectionContainer {
            Text(
                uiState.globalStatus,
                modifier = Modifier.testTag("AuthGlobalStatus"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (uiState.hasPendingRequest) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(strings.pendingCorr3xtSignIn.ifBlank { "Pending Corr3xt sign-in" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.authWaitingCallbackFor(uiState.pendingMethodLabel),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = viewModel::copyPendingSignInUrl) {
                            Text(strings.copyAuthSignInUrl())
                        }
                        Button(onClick = viewModel::cancelPendingRequest) {
                            Text(strings.cancelPendingSignIn())
                        }
                    }
                }
            }
        }

        if (uiState.apiKeyFallbackMethodId.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(strings.authApiKeyFallbackTitle(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.authApiKeyFallbackDescription(uiState.apiKeyFallbackLabel),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            viewModel.prepareApiKeySetup(uiState.apiKeyFallbackMethodId)
                            onOpenSettings()
                        },
                    ) {
                        Text(strings.setUpApiKeyFor(uiState.apiKeyFallbackLabel))
                    }
                }
            }
        }

}

@Composable
private fun AccountsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** Providers have no logo in the app, so each shows its initial in a rounded tile. */
/** The brand's own mark for a connector, on a quiet tile; a letter only for one with no mark. */
@Composable
internal fun ConnectorLogo(
    id: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val logo = connectorLogoFor(id)
    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.small),
        shape = MaterialTheme.shapes.small,
        color = LocalHermesGlassTokens.current.accentContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (logo == null) {
                Text(
                    text = label.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else if (logo.second) {
                // One-colour marks follow the theme so they stay visible in dark mode.
                Icon(
                    painter = painterResource(logo.first),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(logo.first),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Drawable and whether it is a one-colour mark that takes the theme's text colour. */
private fun connectorLogoFor(id: String): Pair<Int, Boolean>? = when {
    id == "email" -> R.drawable.ic_brand_email to true
    id == "phone" -> R.drawable.ic_app_phone to true
    id == "google" -> R.drawable.ic_brand_google to false
    id == "openrouter" -> R.drawable.ic_brand_openrouter to false
    id == "openai" || id == "codex" || id == "chatgpt" -> R.drawable.ic_brand_openai to true
    id.startsWith("xai") -> R.drawable.ic_brand_xai to true
    id == "nous" -> R.drawable.ic_brand_nous to true
    id == "claude" -> R.drawable.ic_brand_claude to false
    id == "gemini" -> R.drawable.ic_brand_gemini to false
    id == "deepseek" -> R.drawable.ic_brand_deepseek to false
    id == "moonshot" -> R.drawable.ic_brand_kimi to false
    id.startsWith("qwen") -> R.drawable.ic_brand_qwen to false
    id.startsWith("zai") || id == "bigmodel" -> R.drawable.ic_brand_zai to true
    else -> null
}

/**
 * Every way to connect, one row each, for the Capabilities page. Tapping a row opens the same sheet
 * as the Connectors page. [leading] rows (the Nous Portal) come first in the same card.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectorsList(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
    leading: @Composable () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val glass = LocalHermesGlassTokens.current
    LaunchedEffect(Unit) { viewModel.refresh() }
    var openOptionId by rememberSaveable { mutableStateOf<String?>(null) }
    ShellGroupCard(modifier = modifier.testTag("HermesConnectorsList")) {
        leading()
        uiState.options.forEachIndexed { index, option ->
            ShellRowDivider(startInset = 66.dp)
            ShellListRow(
                title = option.label,
                subtitle = option.description,
                subtitleStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("AuthOption-${option.id}"),
                onClick = { openOptionId = option.id },
                leading = { ConnectorLogo(id = option.id, label = option.label) },
                trailing = {
                    Icon(
                        painter = painterResource(if (option.signedIn) R.drawable.ic_ui_check else R.drawable.ic_ui_plus),
                        contentDescription = null,
                        tint = if (option.signedIn) MaterialTheme.colorScheme.primary else glass.mutedText,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        }
    }
    val openOption = uiState.options.firstOrNull { it.id == openOptionId }
    if (openOption != null) {
        ModalBottomSheet(onDismissRequest = { openOptionId = null }) {
            AccountOptionControls(
                uiState = uiState,
                option = openOption,
                viewModel = viewModel,
                onOpenSettings = {
                    openOptionId = null
                    onOpenSettings()
                },
            )
        }
    }
}

@Composable
private fun AccountOptionControls(
    uiState: AuthUiState,
    option: AuthOptionUiState,
    viewModel: AuthViewModel,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalHermesStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Sign-in codes and failures arrive here, so they must stay visible over the page.
        AccountNotices(uiState = uiState, viewModel = viewModel, onOpenSettings = onOpenSettings)
        Text(option.label, style = MaterialTheme.typography.titleLarge)
        Text(option.description, style = MaterialTheme.typography.bodySmall)
        Text(option.status, style = MaterialTheme.typography.bodyMedium)
        if (option.accountHint.isNotBlank()) {
            Text(option.accountHint, style = MaterialTheme.typography.bodySmall)
        }
        if (option.runtimeProvider.isNotBlank()) {
            Text(
                "${strings.hermesProviderPrefix.ifBlank { "Agent provider" }}: ${option.runtimeProvider}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (option.supportsApiKeySetup) {
            OutlinedTextField(
                value = option.credentialInput,
                onValueChange = { viewModel.updateProviderCredentialInput(option.id, it) },
                label = { Text(strings.apiKeyLabel()) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    if (option.credentialInputHelp.isNotBlank()) {
                        Text(option.credentialInputHelp)
                    }
                },
                singleLine = false,
                minLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AuthProviderCredential-${option.id}"),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (option.supportsBrowserSignIn) {
                Button(
                    modifier = Modifier.testTag("AuthSignIn-${option.id}"),
                    onClick = { viewModel.startAuth(option.id) },
                    enabled = option.browserSignInEnabled,
                ) {
                    Text(if (option.signedIn) strings.reconnect.ifBlank { "Reconnect" } else strings.signIn.ifBlank { "Sign in" })
                }
            }
            if (option.supportsApiKeySetup) {
                Button(
                    modifier = Modifier.testTag("AuthProviderSaveCredential-${option.id}"),
                    onClick = { viewModel.saveProviderCredential(option.id) },
                ) {
                    Text(strings.saveLabel())
                }
                Button(
                    onClick = {
                        viewModel.prepareApiKeySetup(option.id)
                        onOpenSettings()
                    },
                ) {
                    Text(
                        if (option.supportsBrowserSignIn) {
                            strings.useApiKeyInSettings()
                        } else {
                            strings.setUpApiKeyFor(option.label)
                        }
                    )
                }
            }
            if (option.providerSetupUrl.isNotBlank()) {
                Button(
                    modifier = Modifier.testTag("AuthProviderOpenSetup-${option.id}"),
                    onClick = { viewModel.openProviderSetupPage(option.id) },
                ) {
                    Text(strings.openProviderKeyPage(option.label))
                }
                Button(
                    modifier = Modifier.testTag("AuthProviderCopySetup-${option.id}"),
                    onClick = { viewModel.copyProviderSetupUrl(option.id) },
                ) {
                    Text(strings.copyProviderSetupUrl())
                }
                Button(
                    modifier = Modifier.testTag("AuthProviderCheckSetup-${option.id}"),
                    onClick = { viewModel.checkProviderSetupPages(option.id) },
                ) {
                    Text(strings.checkProviderSetupUrl())
                }
            }
            if (option.signedIn) {
                Button(onClick = { viewModel.signOut(option.id) }) {
                    Text(strings.signOut.ifBlank { "Sign out" })
                }
            }
        }
    }
}
