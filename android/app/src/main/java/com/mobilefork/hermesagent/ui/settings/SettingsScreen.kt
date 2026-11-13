@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.mobilefork.hermesagent.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.ShellStrings
import com.mobilefork.hermesagent.ui.i18n.UiLanguage
import com.mobilefork.hermesagent.ui.i18n.normalizeUiLanguageTag
import com.mobilefork.hermesagent.ui.i18n.shellStringsFor
import com.mobilefork.hermesagent.ui.shell.AppSection
import com.mobilefork.hermesagent.ui.shell.ShellAgentAvatar
import com.mobilefork.hermesagent.ui.shell.ShellChevron
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.shell.ShellRowIcon
import com.mobilefork.hermesagent.ui.shell.ShellSubpageHeader
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

import com.mobilefork.hermesagent.ui.i18n.playEditionSummary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import com.mobilefork.hermesagent.ui.shell.ShellSegmentedTabs
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.mobilefork.hermesagent.ui.i18n.modelSettingsText
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import com.mobilefork.hermesagent.ui.theme.TextButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.api.HermesEndpointUrl
import com.mobilefork.hermesagent.backend.BackendKind
import com.mobilefork.hermesagent.data.AppSettings
import com.mobilefork.hermesagent.data.ProviderPresets
import com.mobilefork.hermesagent.ui.i18n.AppLanguage
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import com.mobilefork.hermesagent.ui.i18n.hermesStringsFor
import com.mobilefork.hermesagent.ui.i18n.llamaCppAdvancedText
import com.mobilefork.hermesagent.ui.shell.ShellActionItem
import kotlinx.coroutines.launch
import java.util.Locale

enum class SettingsPage(val route: String, val label: String) {
    Overview("/settings", "General"),
    Models("/settings/models", "Models"),
    Theme("/settings/theme", "Theme"),
    Tools("/settings/tools", "Tools"),
    Memory("/settings/memory", "Memory"),
    Privacy("/settings/privacy", "Privacy and safety"),
}

internal val appearanceCardShapes = listOf("rounded", "soft", "square")

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    extraBottomSpacing: Dp = 0.dp,
    onContextActionsChanged: (List<ShellActionItem>) -> Unit = {},
    onSettingsChanged: () -> Unit = {},
    onOpenSection: (AppSection) -> Unit = {},
    onPageTitleChanged: (String?) -> Unit = {},
    showPageHeader: Boolean = false,
    initialPage: SettingsPage? = null,
    onOpenOnboarding: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    // Settings content follows the selected language immediately (source of truth = uiState),
    // so the page retranslates even before shell CompositionLocal catches up.
    val strings = hermesStringsFor(AppLanguage.fromTag(uiState.languageTag))
    val shell = shellStringsFor(uiState.languageTag)
    val selectedPreset = ProviderPresets.find(uiState.provider)
    val selectedProviderLabel = strings.providerDisplayLabel(
        uiState.provider,
        selectedPreset?.label ?: uiState.provider,
    )
    // No page selected is the settings list; a selected page opens on top of it.
    var modelsTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedPageName by rememberSaveable(initialPage) { mutableStateOf(initialPage?.name) }
    val selectedPage = SettingsPage.entries.firstOrNull { it.name == selectedPageName }
    // A page opened directly from elsewhere leaves through the shell, back to where it came from.
    BackHandler(enabled = selectedPage != null && initialPage == null) { selectedPageName = null }
    SideEffect { onPageTitleChanged(selectedPage?.let { settingsPageTitle(it, shell) }) }

    val listState = rememberLazyListState()
    val listScope = rememberCoroutineScope()
    // Selecting a settings destination always exposes its first action, not the old scroll offset.
    LaunchedEffect(selectedPage) {
        listState.scrollToItem(0)
    }

    SideEffect {
        onContextActionsChanged(emptyList())
    }

    // Snapshot existing runtime state once; do not cold-start on every recomposition.
    LaunchedEffect(Unit) {
        viewModel.refreshAgentEndpoint(forceStart = false)
    }

    CompositionLocalProvider(LocalHermesStrings provides strings, LocalShellStrings provides shell) {
    MaterialTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                if (selectedPage == null) {
                    SettingsHome(
                        providerLabel = selectedProviderLabel,
                        modelName = uiState.model,
                        extraBottomSpacing = extraBottomSpacing,
                        onOpenPage = { page -> selectedPageName = page.name },
                        onOpenMemory = { selectedPageName = SettingsPage.Memory.name },
                        onOpenSection = onOpenSection,
                        onOpenOnboarding = onOpenOnboarding,
                    )
                } else Column(
                    modifier = Modifier
                        .widthIn(max = 920.dp)
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showPageHeader && selectedPage != null) {
                        ShellSubpageHeader(
                            title = settingsPageTitle(selectedPage, shell),
                            onBack = { selectedPageName = null },
                        )
                    }
                    if (selectedPage == SettingsPage.Models) {
                        // Pinned above the list so switching tabs never needs a scroll back up.
                        ShellSegmentedTabs(
                            options = listOf(shell.modelsTabCloud, shell.modelsTabLocal, shell.modelsTabAdvanced),
                            selectedIndex = modelsTab,
                            onSelect = { modelsTab = it },
                            testTagPrefix = "HermesModelsTab",
                        )
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("HermesSettingsContentList"),
                        contentPadding = PaddingValues(bottom = extraBottomSpacing),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    if (selectedPage == SettingsPage.Privacy) {
                    item {
                        com.mobilefork.hermesagent.ui.privacy.PrivacySafetyCard(strings)
                    }
                    }
                    if (selectedPage == SettingsPage.Overview) {
                    item {
                        LanguagePickerCard(
                            currentLanguageTag = uiState.languageTag,
                            onSelectLanguage = { language ->
                                viewModel.selectUiLanguage(language)
                                onSettingsChanged()
                            },
                            strings = strings,
                        )
                    }
                    item {
                        AgentPersonaCard(
                            customSystemPrompt = uiState.customSystemPrompt,
                            onPromptChange = viewModel::updateCustomSystemPrompt,
                            onSave = viewModel::saveAgentPersona,
                            onClear = viewModel::clearAgentPersona,
                            strings = strings,
                        )
                    }
                    item {
                        OfflineAirplaneCard(
                            enabled = uiState.offlineAirplaneMode,
                            onChange = viewModel::updateOfflineAirplaneMode,
                            strings = strings,
                        )
                    }
                    item(key = "diagnostic-export") {
                        com.mobilefork.hermesagent.ui.diagnostics.DiagnosticsExportCard()
                    }
                    item {
                        SettingsHelpCard(providerLabel = selectedProviderLabel, strings = strings)
                    }
                    }
                    if (selectedPage == SettingsPage.Theme) {
                    item {
                        AppearanceCard(
                            keywordHighlightingEnabled = uiState.keywordHighlightingEnabled,
                            themePrimaryHex = uiState.themePrimaryHex,
                            themeSecondaryHex = uiState.themeSecondaryHex,
                            themeBackgroundHex = uiState.themeBackgroundHex,
                            themeSurfaceHex = uiState.themeSurfaceHex,
                            themeSurfaceVariantHex = uiState.themeSurfaceVariantHex,
                            themeCardShape = uiState.themeCardShape,
                            uiFontScale = uiState.uiFontScale,
                            onKeywordHighlightingChange = viewModel::updateKeywordHighlighting,
                            onPrimaryHexChange = viewModel::updateThemePrimaryHex,
                            onSecondaryHexChange = viewModel::updateThemeSecondaryHex,
                            onBackgroundHexChange = viewModel::updateThemeBackgroundHex,
                            onSurfaceHexChange = viewModel::updateThemeSurfaceHex,
                            onSurfaceVariantHexChange = viewModel::updateThemeSurfaceVariantHex,
                            onCardShapeChange = viewModel::updateThemeCardShape,
                            onUiFontScaleChange = viewModel::updateUiFontScale,
                            onApplyPreset = viewModel::applyThemePreset,
                            onSaveAppearance = {
                                viewModel.saveAppearance()
                                onSettingsChanged()
                            },
                            strings = strings,
                        )
                    }
                    }
                    if (selectedPage == SettingsPage.Models) {
                    if (modelsTab == 0) {
                    item(key = "model-provider") {
                        RemoteFallbackCard(
                            providerId = uiState.provider,
                            providerLabel = selectedProviderLabel,
                            baseUrl = uiState.baseUrl,
                            model = uiState.model,
                            apiKey = uiState.apiKey,
                            status = uiState.status,
                            onSelectProvider = viewModel::updateProvider,
                            onBaseUrlChange = viewModel::updateBaseUrl,
                            onModelChange = viewModel::updateModel,
                            onApiKeyChange = viewModel::updateApiKey,
                            onOpenProviderKeyPage = viewModel::openProviderKeyPage,
                            onCopyProviderKeyPage = viewModel::copyProviderKeyPage,
                            onCheckProviderKeyPage = viewModel::checkProviderKeyPage,
                            onImportProviderCredential = viewModel::importSavedProviderCredential,
                            onSave = viewModel::save,
                            strings = strings,
                        )
                    }
                    }
                    if (modelsTab == 1) {
                    item(key = "model-picker") {
                        LocalModelDownloadsSection(
                            dataSaverMode = uiState.dataSaverMode,
                            offlineAirplaneMode = uiState.offlineAirplaneMode,
                            onDataSaverModeChange = viewModel::updateDataSaverMode,
                            selectedBackend = uiState.onDeviceBackend,
                            onRuntimeFlavorSelected = viewModel::syncOnDeviceBackendWithRuntimeFlavor,
                            onRequiredLlamaCppRuntimeLane = viewModel::syncPersistedRequiredLlamaCppRuntimeLane,
                            onCompletedDownloadReady = viewModel::startAcceptedLocalRuntimeHandoff,
                        )
                    }
                    item(key = "model-runtime") {
                        SettingsDisclosure(
                            sectionId = "ModelSettings-runtime",
                            title = modelSettingsText(strings.language, "runtime"),
                            summary = modelSettingsText(strings.language, "runtime_help"),
                        ) {
                            OnDeviceInferenceCard(
                                onDeviceBackend = uiState.onDeviceBackend,
                                speculativeDecodingMode = uiState.liteRtLmSpeculativeDecodingMode,
                                onSelectBackend = viewModel::updateOnDeviceBackend,
                                onSelectSpeculativeDecodingMode = viewModel::updateLiteRtLmSpeculativeDecodingMode,
                                onStartRuntime = { runtimeFlavor ->
                                    viewModel.startLocalRuntimeForFlavor(runtimeFlavor)
                                },
                                summary = uiState.onDeviceSummary,
                                strings = strings,
                            )
                        }
                    }
                    item(key = "model-advanced") {
                        SettingsDisclosure(
                            sectionId = "ModelSettings-advanced",
                            title = modelSettingsText(strings.language, "advanced"),
                            summary = modelSettingsText(strings.language, "advanced_help"),
                        ) {
                            LlamaCppAdvancedCard(
                                runtimeLane = uiState.llamaCppRuntimeLane,
                                cacheTypeK = uiState.llamaCppCacheTypeK,
                                cacheTypeV = uiState.llamaCppCacheTypeV,
                                flashAttention = uiState.llamaCppFlashAttention,
                                additionalArguments = uiState.llamaCppAdditionalArguments,
                                onRuntimeLaneChange = viewModel::updateLlamaCppRuntimeLane,
                                onCacheTypeKChange = viewModel::updateLlamaCppCacheTypeK,
                                onCacheTypeVChange = viewModel::updateLlamaCppCacheTypeV,
                                onFlashAttentionChange = viewModel::updateLlamaCppFlashAttention,
                                onAdditionalArgumentsChange = viewModel::updateLlamaCppAdditionalArguments,
                                onApplyAndRestart = viewModel::applyLlamaCppAdvancedSettings,
                                onDangerousOneShotStart = viewModel::tryLlamaCppDespiteRamWarning,
                                language = strings.language,
                            )
                        }
                    }
                    }
                    if (modelsTab == 2) {
                    item(key = "model-generation") {
                        SettingsDisclosure(
                            sectionId = "ModelSettings-generation",
                            title = modelSettingsText(strings.language, "generation"),
                            summary = modelSettingsText(strings.language, "generation_help"),
                        ) {
                            ModelGenerationConfigCard(
                                maxTokens = uiState.localModelMaxTokens,
                                contextTokens = uiState.localModelContextTokens,
                                cpuThreads = uiState.localModelCpuThreads,
                                topK = uiState.localModelTopK,
                                topP = uiState.localModelTopP,
                                temperature = uiState.localModelTemperature,
                                accelerator = uiState.localModelAccelerator,
                                toolMode = uiState.localModelToolMode,
                                apiGenerationKnobsEnabled = uiState.apiGenerationKnobsEnabled,
                                customSystemPrompt = uiState.customSystemPrompt,
                                onMaxTokensChange = viewModel::updateLocalModelMaxTokens,
                                onContextTokensChange = viewModel::updateLocalModelContextTokens,
                                onCpuThreadsChange = viewModel::updateLocalModelCpuThreads,
                                onTopKChange = viewModel::updateLocalModelTopK,
                                onTopPChange = viewModel::updateLocalModelTopP,
                                onTemperatureChange = viewModel::updateLocalModelTemperature,
                                onAcceleratorChange = viewModel::updateLocalModelAccelerator,
                                onToolModeChange = viewModel::updateLocalModelToolMode,
                                onApiGenerationKnobsEnabledChange = viewModel::updateApiGenerationKnobsEnabled,
                                onPromptChange = viewModel::updateCustomSystemPrompt,
                                onSave = viewModel::saveModelGenerationConfig,
                                onClearPrompt = viewModel::clearAgentPersona,
                            )
                        }
                    }
                    item(key = "model-diagnostic-export") {
                        com.mobilefork.hermesagent.ui.diagnostics.DiagnosticsExportCard()
                    }
                    if (!com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) item {
                        SettingsDisclosure(
                            sectionId = "ModelSettings-sharing",
                            title = modelSettingsText(strings.language, "sharing"),
                            summary = modelSettingsText(strings.language, "sharing_help"),
                        ) {
                        AgentEndpointCard(
                            loopbackUrl = uiState.agentLoopbackUrl,
                            lanUrl = uiState.agentLanUrl,
                            apiKey = uiState.agentApiKey,
                            modelName = uiState.agentModelName,
                            started = uiState.agentEndpointStarted,
                            onRefresh = { viewModel.refreshAgentEndpoint(forceStart = true) },
                            strings = strings,
                        )
                        }
                    }
                    }
                    }
                    if (selectedPage == SettingsPage.Tools && !com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
                    item {
                        CapabilitiesSection()
                    }
                    item {
                        SettingsDisclosure(
                            sectionId = "ToolSettings-mcp",
                            title = "MCP",
                            summary = shell.toolsMcpSummary,
                        ) {
                            McpSettingsSection(selectedProviderId = uiState.provider)
                        }
                    }
                    item {
                        SettingsDisclosure(
                            sectionId = "ToolSettings-routing",
                            title = shell.toolsRoutingTitle,
                            summary = shell.toolsRoutingSummary,
                        ) {
                            ToolProfileCard()
                        }
                    }
                    }
                    if (selectedPage == SettingsPage.Memory && !com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
                    item {
                        LocalMemorySection()
                    }
                    }
                    if (uiState.status.isNotBlank()) {
                        item {
                            SelectionContainer { Text(uiState.status, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                        }
                    }
                    }
                }
            }
        }
    }
    }
}

private val baseLlamaCppCacheTypes = listOf(
    "default",
    "f32",
    "f16",
    "bf16",
    "q8_0",
    "q4_0",
    "q4_1",
    "iq4_nl",
    "q5_0",
    "q5_1",
)

private val turboQuantCacheTypes = baseLlamaCppCacheTypes + listOf("turbo2", "turbo3", "turbo4")

@Composable
private fun LlamaCppAdvancedCard(
    runtimeLane: String,
    cacheTypeK: String,
    cacheTypeV: String,
    flashAttention: String,
    additionalArguments: List<String>,
    onRuntimeLaneChange: (String) -> Unit,
    onCacheTypeKChange: (String) -> Unit,
    onCacheTypeVChange: (String) -> Unit,
    onFlashAttentionChange: (String) -> Unit,
    onAdditionalArgumentsChange: (List<String>) -> Unit,
    onApplyAndRestart: () -> Unit,
    onDangerousOneShotStart: () -> Unit,
    language: AppLanguage,
) {
    val normalizedLane = AppSettings.normalizeLlamaCppRuntimeLane(runtimeLane)
    val normalizedK = AppSettings.normalizeLlamaCppCacheType(cacheTypeK)
    val normalizedV = AppSettings.normalizeLlamaCppCacheType(cacheTypeV)
    val normalizedFlash = AppSettings.normalizeLlamaCppFlashAttention(flashAttention)
    val cacheTypes = if (normalizedLane == "turboquant") turboQuantCacheTypes else baseLlamaCppCacheTypes
    val validationKey = llamaCppAdvancedValidationKey(
        lane = normalizedLane,
        cacheTypeK = normalizedK,
        cacheTypeV = normalizedV,
        flashAttention = normalizedFlash,
        additionalArguments = additionalArguments,
    )
    var showDangerConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showDangerConfirmation) {
        AlertDialog(
            modifier = Modifier.testTag("LlamaCppDangerousRamDialog"),
            onDismissRequest = { showDangerConfirmation = false },
            title = { Text(llamaCppAdvancedText(language, "danger_dialog_title")) },
            text = { Text(llamaCppAdvancedText(language, "danger_dialog_body")) },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag("LlamaCppDangerousRamCancel"),
                    onClick = { showDangerConfirmation = false },
                ) {
                    Text(llamaCppAdvancedText(language, "cancel"))
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("LlamaCppDangerousRamConfirm"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDangerConfirmation = false
                        onDangerousOneShotStart()
                    },
                ) {
                    Text(llamaCppAdvancedText(language, "confirm"))
                }
            },
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("LlamaCppAdvancedCard"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(llamaCppAdvancedText(language, "title"), style = MaterialTheme.typography.titleMedium)
            Text(llamaCppAdvancedText(language, "description"), style = MaterialTheme.typography.bodySmall)

            Text(llamaCppAdvancedText(language, "lane"), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LlamaCppChoiceButton(
                    value = "stable",
                    label = llamaCppAdvancedText(language, "stable"),
                    selectedValue = normalizedLane,
                    testTagPrefix = "LlamaCppRuntimeLane",
                    description = llamaCppAdvancedText(language, "lane"),
                    onSelect = onRuntimeLaneChange,
                )
                LlamaCppChoiceButton(
                    value = "turboquant",
                    label = llamaCppAdvancedText(language, "experimental"),
                    selectedValue = normalizedLane,
                    testTagPrefix = "LlamaCppRuntimeLane",
                    description = llamaCppAdvancedText(language, "lane"),
                    onSelect = onRuntimeLaneChange,
                )
            }
            Text(
                llamaCppAdvancedText(
                    language,
                    if (normalizedLane == "turboquant") "experimental_description" else "stable_description",
                ),
                style = MaterialTheme.typography.bodySmall,
            )

            LlamaCppCacheTypeSelector(
                title = llamaCppAdvancedText(language, "cache_k"),
                value = normalizedK,
                choices = cacheTypes,
                testTagPrefix = "LlamaCppCacheK",
                language = language,
                onSelect = onCacheTypeKChange,
            )
            LlamaCppCacheTypeSelector(
                title = llamaCppAdvancedText(language, "cache_v"),
                value = normalizedV,
                choices = cacheTypes,
                testTagPrefix = "LlamaCppCacheV",
                language = language,
                onSelect = onCacheTypeVChange,
            )
            Text(llamaCppAdvancedText(language, "q5_explanation"), style = MaterialTheme.typography.bodySmall)

            Text(llamaCppAdvancedText(language, "flash_attention"), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("default", "auto", "on", "off").forEach { value ->
                    LlamaCppChoiceButton(
                        value = value,
                        label = llamaCppAdvancedText(language, value),
                        selectedValue = normalizedFlash,
                        testTagPrefix = "LlamaCppFlashAttention",
                        description = llamaCppAdvancedText(language, "flash_attention"),
                        onSelect = onFlashAttentionChange,
                    )
                }
            }
            Text(llamaCppAdvancedText(language, "turbo_requirement"), style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = additionalArguments.joinToString("\n"),
                onValueChange = { value ->
                    onAdditionalArgumentsChange(llamaCppArgumentLines(value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("LlamaCppAdditionalArguments")
                    .semantics {
                        contentDescription = llamaCppAdvancedText(language, "additional_arguments")
                    },
                label = { Text(llamaCppAdvancedText(language, "additional_arguments")) },
                placeholder = { Text(llamaCppAdvancedText(language, "arguments_placeholder")) },
                minLines = 3,
                maxLines = 8,
            )
            Text(llamaCppAdvancedText(language, "arguments_description"), style = MaterialTheme.typography.bodySmall)

            Text(llamaCppAdvancedText(language, "effective"), style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${llamaCppAdvancedText(language, "lane")}: $normalizedLane · " +
                    "K=$normalizedK · V=$normalizedV · " +
                    "${llamaCppAdvancedText(language, "flash_attention")}: $normalizedFlash · " +
                    "${llamaCppAdvancedText(language, "additional_arguments")}: ${additionalArguments.size}",
                modifier = Modifier.testTag("LlamaCppEffectiveArgumentsSummary"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            validationKey?.let { key ->
                Text(
                    text = llamaCppAdvancedText(language, key),
                    modifier = Modifier
                        .testTag("LlamaCppAdvancedValidationError")
                        .semantics { contentDescription = llamaCppAdvancedText(language, key) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ApplyLlamaCppAdvancedSettingsButton"),
                onClick = onApplyAndRestart,
                enabled = validationKey == null,
            ) {
                Text(llamaCppAdvancedText(language, "apply_restart"))
            }

            Text(
                llamaCppAdvancedText(language, "danger_title"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                llamaCppAdvancedText(language, "danger_description"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("TryLlamaCppDespiteRamWarningButton")
                    .semantics {
                        contentDescription = llamaCppAdvancedText(language, "danger_button")
                    },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { showDangerConfirmation = true },
                enabled = validationKey == null,
            ) {
                Text(llamaCppAdvancedText(language, "danger_button"))
            }
        }
    }
}

internal fun llamaCppArgumentLines(value: String): List<String> {
    if (value.isEmpty()) return emptyList()
    return value.replace("\r\n", "\n").replace('\r', '\n').split('\n')
}

@Composable
private fun LlamaCppCacheTypeSelector(
    title: String,
    value: String,
    choices: List<String>,
    testTagPrefix: String,
    language: AppLanguage,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title $value" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                LlamaCppChoiceButton(
                    value = choice,
                    label = if (choice == "default") {
                        llamaCppAdvancedText(language, "default")
                    } else {
                        choice
                    },
                    selectedValue = value,
                    testTagPrefix = testTagPrefix,
                    description = title,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun LlamaCppChoiceButton(
    value: String,
    label: String,
    selectedValue: String,
    testTagPrefix: String,
    description: String,
    onSelect: (String) -> Unit,
) {
    val isSelected = selectedValue == value
    Button(
        modifier = Modifier
            .testTag("$testTagPrefix-$value")
            .semantics {
                selected = isSelected
                contentDescription = "$description: $label"
            },
        onClick = { onSelect(value) },
        enabled = !isSelected,
    ) {
        Text(if (isSelected) "✓ $label" else label)
    }
}

internal fun settingsPageTitle(page: SettingsPage, shell: ShellStrings): String = when (page) {
    SettingsPage.Overview -> shell.settingsPersonalization
    SettingsPage.Models -> shell.settingsModels
    SettingsPage.Theme -> shell.settingsAppearance
    SettingsPage.Tools -> shell.extra.capTitle
    SettingsPage.Memory -> shell.settingsMemory
    SettingsPage.Privacy -> shell.settingsAccess
}

/** The small heading over each group of settings. */
@Composable
private fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 6.dp, top = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = LocalHermesGlassTokens.current.mutedText,
    )
}

@Composable
private fun SettingsHome(
    providerLabel: String,
    modelName: String,
    extraBottomSpacing: Dp,
    onOpenPage: (SettingsPage) -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSection: (AppSection) -> Unit,
    onOpenOnboarding: () -> Unit = {},
) {
    val shell = LocalShellStrings.current
    val playEdition = com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION
    val rowTitleStyle = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.6.sp)
    @Composable
    fun SettingsRow(
        title: String,
        iconRes: Int,
        tag: String,
        showChevron: Boolean = false,
        onClick: () -> Unit,
    ) {
        ShellListRow(
            title = title.uppercase(),
            titleStyle = rowTitleStyle,
            modifier = Modifier.testTag(tag),
            onClick = onClick,
            leading = { ShellRowIcon(iconRes) },
            trailing = if (showChevron) ({ ShellChevron() }) else null,
        )
    }
    Column(
        modifier = Modifier
            .widthIn(max = 920.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ShellPageMargin)
            .padding(top = 8.dp, bottom = extraBottomSpacing)
            .testTag("HermesSettingsHome"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShellGroupCard {
            ShellListRow(
                title = shell.agentName,
                subtitle = listOf(providerLabel, modelName).filter { it.isNotBlank() }
                    .joinToString(" · ").ifBlank { shell.noModelSelected },
                leading = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val revision by com.mobilefork.hermesagent.data.AgentStore.revision.collectAsState()
                    val main = remember(revision) { com.mobilefork.hermesagent.data.AgentStore(context).main() }
                    com.mobilefork.hermesagent.ui.agents.AgentAvatar(agent = main, size = 48.dp)
                },
            )
        }
        // Every setting of the app is here, under the heading it belongs to.
        SettingsGroupTitle(shell.extra.setGroupAgent)
        ShellGroupCard {
            SettingsRow(shell.settingsPersonalization, R.drawable.ic_set_sliders, "HermesSettingsPage_Overview") {
                onOpenPage(SettingsPage.Overview)
            }
            ShellRowDivider(startInset = 54.dp)
            SettingsRow(shell.settingsModels, R.drawable.ic_set_globe, "HermesSettingsPage_Models", showChevron = true) {
                onOpenPage(SettingsPage.Models)
            }
            // Play has no skills, memory tools, connected accounts, device, terminal or portal page.
            if (!playEdition) {
                ShellRowDivider(startInset = 54.dp)
                SettingsRow(shell.extra.capTitle, R.drawable.ic_set_bolt, "HermesSettingsPage_Tools") {
                    onOpenPage(SettingsPage.Tools)
                }
                ShellRowDivider(startInset = 54.dp)
                SettingsRow(shell.settingsMemory, R.drawable.ic_set_memory, "HermesSettingsMemory") {
                    onOpenMemory()
                }
            }
        }
        if (!playEdition) {
            SettingsGroupTitle(shell.extra.setGroupConnections)
            ShellGroupCard {
                SettingsRow(shell.settingsConnectors, R.drawable.ic_set_plug, "HermesSettingsConnectors") {
                    onOpenSection(AppSection.Accounts)
                }
                ShellRowDivider(startInset = 54.dp)
                SettingsRow(shell.portalTitle, R.drawable.ic_app_window, "HermesSettingsPortal") {
                    onOpenSection(AppSection.NousPortal)
                }
            }
            SettingsGroupTitle(shell.extra.setGroupDevice)
            ShellGroupCard {
                SettingsRow(shell.settingsSystem, R.drawable.ic_app_phone, "HermesSettingsSystem") {
                    onOpenSection(AppSection.Device)
                }
                ShellRowDivider(startInset = 54.dp)
                SettingsRow(shell.terminalTitle, R.drawable.ic_app_terminal, "HermesSettingsTerminal") {
                    onOpenSection(AppSection.Terminal)
                }
            }
        }
        SettingsGroupTitle(shell.extra.setGroupLook)
        ShellGroupCard {
            SettingsRow(shell.settingsAppearance, R.drawable.ic_set_palette, "HermesSettingsPage_Theme") {
                onOpenPage(SettingsPage.Theme)
            }
        }
        SettingsGroupTitle(shell.extra.setGroupPrivacy)
        ShellGroupCard {
            SettingsRow(shell.settingsAccess, R.drawable.ic_set_shield, "HermesSettingsPage_Privacy") {
                onOpenPage(SettingsPage.Privacy)
            }
        }
        SettingsGroupTitle(shell.extra.setGroupHelp)
        ShellGroupCard {
            SettingsRow(shell.extra.settingsOnboarding, R.drawable.ic_tab_ideas, "HermesSettingsOnboarding") {
                onOpenOnboarding()
            }
        }
        // The Play edition updates through Google Play, so it has no updater of its own.
        if (!playEdition) {
            // The updater's first line is the version, so the plain version text is not repeated.
            AppUpdateCard()
        } else {
            Text(
                text = shell.version(com.mobilefork.hermesagent.BuildConfig.VERSION_NAME),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = LocalHermesGlassTokens.current.mutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private enum class ModelConfigTab {
    ModelConfigs,
    SystemPrompt,
    ToolGuidance,
}

@Composable
private fun ModelGenerationConfigCard(
    maxTokens: Int,
    contextTokens: Int,
    cpuThreads: Int,
    topK: Int,
    topP: Float,
    temperature: Float,
    accelerator: String,
    toolMode: String,
    apiGenerationKnobsEnabled: Boolean,
    customSystemPrompt: String,
    onMaxTokensChange: (Int) -> Unit,
    onContextTokensChange: (Int) -> Unit,
    onCpuThreadsChange: (Int) -> Unit,
    onTopKChange: (Int) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onAcceleratorChange: (String) -> Unit,
    onToolModeChange: (String) -> Unit,
    onApiGenerationKnobsEnabledChange: (Boolean) -> Unit,
    onPromptChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearPrompt: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(ModelConfigTab.ModelConfigs) }
    val strings = LocalHermesStrings.current
    val language = strings.language
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(settingsGenerationText(language, "configurations"), style = MaterialTheme.typography.titleMedium)
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ModelConfigTab.entries.filter {
                    !com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION || it != ModelConfigTab.ToolGuidance
                }.forEach { tab ->
                    Tab(
                        modifier = Modifier.testTag("LocalModelConfigTab-${tab.name}"),
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(modelConfigTabLabel(language, tab)) },
                    )
                }
            }
            when (selectedTab) {
                ModelConfigTab.ModelConfigs -> {
                    GenerationSwitchRow(
                        title = settingsGenerationText(language, "api_knobs_title"),
                        description = settingsGenerationText(language, "api_knobs_description"),
                        checked = apiGenerationKnobsEnabled,
                        onCheckedChange = onApiGenerationKnobsEnabledChange,
                    )
                    Text(settingsGenerationText(language, "context_budget"), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0, 2048, 4096, 8192, 16384, 32768, 65536).forEach { budget ->
                            TextButton(onClick = { onContextTokensChange(budget) }, modifier = Modifier.testTag("LocalModelContext$budget")) {
                                Text((if (budget == 0) settingsGenerationText(language, "default") else "${budget / 1024}K") + if (budget == contextTokens) " ✓" else "")
                            }
                        }
                    }
                    GenerationIntegerRow(settingsGenerationText(language, "cpu_threads"), if (cpuThreads == 0) settingsGenerationText(language, "default") else cpuThreads.toString(), cpuThreads, 0, 0, 12, 1, onCpuThreadsChange, "LocalModelCpuThreads")
                    GenerationIntegerRow(
                        title = settingsGenerationText(language, "max_tokens"),
                        valueLabel = maxTokensLabel(maxTokens, language),
                        value = maxTokens,
                        defaultValue = AppSettings.DEFAULT_LOCAL_MODEL_MAX_TOKENS,
                        minValue = AppSettings.DEFAULT_LOCAL_MODEL_MAX_TOKENS,
                        maxValue = AppSettings.MAX_LOCAL_MODEL_MAX_TOKENS,
                        step = 256,
                        onValueChange = onMaxTokensChange,
                        testTagPrefix = "LocalModelMaxTokens",
                    )
                    GenerationIntegerRow(
                        title = strings.topKLabel(),
                        valueLabel = topK.toString(),
                        value = topK,
                        defaultValue = AppSettings.DEFAULT_LOCAL_MODEL_TOP_K,
                        minValue = AppSettings.MIN_LOCAL_MODEL_TOP_K,
                        maxValue = AppSettings.MAX_LOCAL_MODEL_TOP_K,
                        step = 1,
                        onValueChange = onTopKChange,
                        testTagPrefix = "LocalModelTopK",
                    )
                    GenerationSliderRow(
                        title = strings.topPLabel(),
                        valueLabel = formatGenerationDecimal(topP),
                        value = topP,
                        valueRange = AppSettings.MIN_LOCAL_MODEL_TOP_P..AppSettings.MAX_LOCAL_MODEL_TOP_P,
                        onValueChange = onTopPChange,
                        testTag = "LocalModelTopP",
                    )
                    GenerationSliderRow(
                        title = strings.temperatureLabel(),
                        valueLabel = formatGenerationDecimal(temperature),
                        value = temperature,
                        valueRange = AppSettings.MIN_LOCAL_MODEL_TEMPERATURE..AppSettings.MAX_LOCAL_MODEL_TEMPERATURE,
                        onValueChange = onTemperatureChange,
                        testTag = "LocalModelTemperature",
                    )
                    Text(settingsGenerationText(language, "accelerator"), style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        acceleratorChoices(language).forEach { choice ->
                            Button(
                                modifier = Modifier.testTag("LocalModelAccelerator-${choice.value}"),
                                onClick = { onAcceleratorChange(choice.value) },
                                enabled = accelerator != choice.value,
                            ) {
                                Text(choice.label)
                            }
                        }
                    }
                    Text(
                        settingsGenerationText(language, "accelerator_description"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ModelConfigTab.SystemPrompt -> {
                    OutlinedTextField(
                        value = customSystemPrompt,
                        onValueChange = onPromptChange,
                        label = { Text(settingsGenerationText(language, "system_prompt")) },
                        placeholder = { Text(settingsGenerationText(language, "system_prompt_placeholder")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("LocalModelSystemPrompt"),
                        minLines = 3,
                        maxLines = 8,
                    )
                    Text(
                        "${customSystemPrompt.length} / ${AppSettings.MAX_CUSTOM_SYSTEM_PROMPT_CHARS}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        modifier = Modifier.testTag("ClearLocalModelSystemPromptButton"),
                        onClick = onClearPrompt,
                        enabled = customSystemPrompt.isNotBlank(),
                    ) {
                        Text(settingsGenerationText(language, "clear_prompt"))
                    }
                }
                ModelConfigTab.ToolGuidance -> {
                    Text(
                        settingsGenerationText(language, "tool_guidance_description"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        toolGuidanceChoices(language).forEach { choice ->
                            Button(
                                modifier = Modifier.testTag("LocalModelToolMode-${choice.value}"),
                                onClick = { onToolModeChange(choice.value) },
                                enabled = toolMode != choice.value,
                            ) {
                                Text(choice.label)
                            }
                        }
                    }
                    Text(
                        toolGuidanceChoices(language)
                            .firstOrNull { it.value == toolMode }
                            ?.description
                            .orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SaveLocalModelGenerationConfigButton"),
                onClick = onSave,
            ) {
                Text(settingsGenerationText(language, "save_model_configuration"))
            }
        }
    }
}

@Composable
private fun GenerationSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GenerationIntegerRow(
    title: String,
    valueLabel: String,
    value: Int,
    defaultValue: Int,
    minValue: Int,
    maxValue: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    testTagPrefix: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title $valueLabel" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(valueLabel, style = MaterialTheme.typography.titleSmall)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.testTag("${testTagPrefix}Decrease"),
                onClick = { onValueChange(decrementGenerationValue(value, defaultValue, minValue, step)) },
                enabled = value != defaultValue && value > minValue,
            ) {
                Text("-")
            }
            Button(
                modifier = Modifier.testTag("${testTagPrefix}Default"),
                onClick = { onValueChange(defaultValue) },
                enabled = value != defaultValue,
            ) {
                Text(settingsGenerationText(LocalHermesStrings.current.language, "default"))
            }
            Button(
                modifier = Modifier.testTag("${testTagPrefix}Increase"),
                onClick = { onValueChange(incrementGenerationValue(value, defaultValue, maxValue, step)) },
                enabled = value < maxValue,
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun GenerationSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title $valueLabel" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(valueLabel, style = MaterialTheme.typography.titleSmall)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}

private data class AcceleratorChoice(
    val value: String,
    val label: String,
)

private fun acceleratorChoices(language: AppLanguage): List<AcceleratorChoice> = listOf(
    AcceleratorChoice("auto", settingsGenerationText(language, "auto")),
    AcceleratorChoice("cpu", "CPU"),
    AcceleratorChoice("gpu", "GPU"),
)

private fun maxTokensLabel(value: Int, language: AppLanguage): String {
    return if (value <= 0) settingsGenerationText(language, "default") else value.toString()
}

private fun modelConfigTabLabel(language: AppLanguage, tab: ModelConfigTab): String {
    return when (tab) {
        ModelConfigTab.ModelConfigs -> settingsGenerationText(language, "model_configs")
        ModelConfigTab.SystemPrompt -> settingsGenerationText(language, "system_prompt")
        ModelConfigTab.ToolGuidance -> settingsGenerationText(language, "tool_guidance")
    }
}

private data class ToolGuidanceChoice(val value: String, val label: String, val description: String)

private fun toolGuidanceChoices(language: AppLanguage): List<ToolGuidanceChoice> = listOf(
    ToolGuidanceChoice(
        "small",
        settingsGenerationText(language, "tool_mode_small"),
        settingsGenerationText(language, "tool_mode_small_description"),
    ),
    ToolGuidanceChoice(
        "general",
        settingsGenerationText(language, "tool_mode_general"),
        settingsGenerationText(language, "tool_mode_general_description"),
    ),
    ToolGuidanceChoice(
        "large",
        settingsGenerationText(language, "tool_mode_large"),
        settingsGenerationText(language, "tool_mode_large_description"),
    ),
)

internal fun settingsGenerationText(language: AppLanguage, key: String): String {
    return when (key) {
        "context_budget" -> when (language) {
            AppLanguage.CHINESE -> "上下文预算（模型和 RAM 可能限制；Turbo3 仅用于实验性 GGUF）"
            AppLanguage.SPANISH -> "Contexto solicitado (limitado por modelo/RAM; Turbo3 solo GGUF experimental)"
            AppLanguage.GERMAN -> "Kontextbudget (Modell/RAM begrenzen; Turbo3 nur experimentelles GGUF)"
            AppLanguage.PORTUGUESE -> "Contexto solicitado (modelo/RAM podem limitar; Turbo3 só GGUF experimental)"
            AppLanguage.FRENCH -> "Budget de contexte (limité par modèle/RAM ; Turbo3 pour GGUF expérimental)"
            AppLanguage.ENGLISH -> "Context budget (model/RAM may limit it; Turbo3 is experimental GGUF only)"
        }
        "cpu_threads" -> when (language) {
            AppLanguage.CHINESE -> "GGUF CPU 线程"
            AppLanguage.SPANISH -> "Hilos CPU para GGUF"
            AppLanguage.GERMAN -> "GGUF-CPU-Threads"
            AppLanguage.PORTUGUESE -> "Threads CPU para GGUF"
            AppLanguage.FRENCH -> "Threads CPU GGUF"
            AppLanguage.ENGLISH -> "GGUF CPU threads"
        }

        "configurations" -> when (language) {
            AppLanguage.CHINESE -> "配置"
            AppLanguage.SPANISH -> "Configuraciones"
            AppLanguage.GERMAN -> "Konfigurationen"
            AppLanguage.PORTUGUESE -> "Configurações"
            AppLanguage.FRENCH -> "Configurations"
            AppLanguage.ENGLISH -> "Configurations"
        }
        "model_configs" -> when (language) {
            AppLanguage.CHINESE -> "模型配置"
            AppLanguage.SPANISH -> "Config. modelo"
            AppLanguage.GERMAN -> "Modellkonfig."
            AppLanguage.PORTUGUESE -> "Config. modelo"
            AppLanguage.FRENCH -> "Config. modèle"
            AppLanguage.ENGLISH -> "Model configs"
        }
        "system_prompt" -> when (language) {
            AppLanguage.CHINESE -> "系统提示词"
            AppLanguage.SPANISH -> "Prompt del sistema"
            AppLanguage.GERMAN -> "Systemprompt"
            AppLanguage.PORTUGUESE -> "Prompt do sistema"
            AppLanguage.FRENCH -> "Prompt système"
            AppLanguage.ENGLISH -> "System prompt"
        }
        "tool_guidance" -> when (language) {
            AppLanguage.CHINESE -> "工具指导"
            AppLanguage.SPANISH -> "Guía de herramientas"
            AppLanguage.GERMAN -> "Werkzeugführung"
            AppLanguage.PORTUGUESE -> "Guia de ferramentas"
            AppLanguage.FRENCH -> "Guide des outils"
            AppLanguage.ENGLISH -> "Tool guidance"
        }
        "tool_guidance_description" -> when (language) {
            AppLanguage.CHINESE -> "选择本地模型在每次请求前接收多少经过整理的工具名称和参数结构。"
            AppLanguage.SPANISH -> "Elige cuántos nombres y esquemas de herramientas seleccionados recibe el modelo local antes de cada solicitud."
            AppLanguage.GERMAN -> "Wähle, wie viele kuratierte Werkzeugnamen und Schemas das lokale Modell vor jeder Anfrage erhält."
            AppLanguage.PORTUGUESE -> "Escolha quantos nomes e formatos de ferramentas selecionados o modelo local recebe antes de cada solicitação."
            AppLanguage.FRENCH -> "Choisissez combien de noms et schémas d’outils sélectionnés le modèle local reçoit avant chaque requête."
            AppLanguage.ENGLISH -> "Choose how many curated tool names and argument shapes the local model receives before every request."
        }
        "tool_mode_small" -> when (language) {
            AppLanguage.CHINESE -> "小型模型"
            AppLanguage.SPANISH -> "Modelo pequeño"
            AppLanguage.GERMAN -> "Kleines Modell"
            AppLanguage.PORTUGUESE -> "Modelo pequeno"
            AppLanguage.FRENCH -> "Petit modèle"
            AppLanguage.ENGLISH -> "Small model"
        }
        "tool_mode_general" -> when (language) {
            AppLanguage.CHINESE -> "通用模型"
            AppLanguage.SPANISH -> "Modelo general"
            AppLanguage.GERMAN -> "Allgemeines Modell"
            AppLanguage.PORTUGUESE -> "Modelo geral"
            AppLanguage.FRENCH -> "Modèle général"
            AppLanguage.ENGLISH -> "General model"
        }
        "tool_mode_large" -> when (language) {
            AppLanguage.CHINESE -> "大型模型"
            AppLanguage.SPANISH -> "Modelo grande"
            AppLanguage.GERMAN -> "Großes Modell"
            AppLanguage.PORTUGUESE -> "Modelo grande"
            AppLanguage.FRENCH -> "Grand modèle"
            AppLanguage.ENGLISH -> "Large model"
        }
        "tool_mode_small_description" -> when (language) {
            AppLanguage.CHINESE -> "发送四个核心工具结构，并加入请求明确需要的专用工具。上下文最小。"
            AppLanguage.SPANISH -> "Envía cuatro esquemas básicos y añade herramientas especializadas inferidas de la solicitud. Contexto mínimo."
            AppLanguage.GERMAN -> "Sendet vier Kernschemas plus aus der Anfrage abgeleitete Spezialwerkzeuge. Kleinster Kontext."
            AppLanguage.PORTUGUESE -> "Envia quatro formatos principais e adiciona ferramentas especializadas inferidas do pedido. Contexto mínimo."
            AppLanguage.FRENCH -> "Envoie quatre schémas essentiels et les outils spécialisés déduits de la demande. Contexte minimal."
            AppLanguage.ENGLISH -> "Sends four core schemas plus specialized tools inferred from the request. Smallest context."
        }
        "tool_mode_general_description" -> when (language) {
            AppLanguage.CHINESE -> "推荐。始终发送常用文件、终端、Linux、Android、自动化、诊断和记忆工具的精简结构。"
            AppLanguage.SPANISH -> "Recomendado. Siempre envía esquemas compactos para archivos, terminal, Linux, Android, automatización, diagnóstico y memoria."
            AppLanguage.GERMAN -> "Empfohlen. Sendet immer kompakte Schemas für Dateien, Terminal, Linux, Android, Automatisierung, Diagnose und Speicher."
            AppLanguage.PORTUGUESE -> "Recomendado. Sempre envia formatos compactos para arquivos, terminal, Linux, Android, automação, diagnóstico e memória."
            AppLanguage.FRENCH -> "Recommandé. Envoie toujours des schémas compacts pour fichiers, terminal, Linux, Android, automatisation, diagnostic et mémoire."
            AppLanguage.ENGLISH -> "Recommended. Always sends compact file, terminal, Linux, Android, automation, diagnostics, and memory schemas."
        }
        "tool_mode_large_description" -> when (language) {
            AppLanguage.CHINESE -> "发送完整的紧凑工具目录。适合上下文较大的模型，但占用更多 token。"
            AppLanguage.SPANISH -> "Envía el catálogo compacto completo. Adecuado para modelos con más contexto, pero usa más tokens."
            AppLanguage.GERMAN -> "Sendet den vollständigen kompakten Werkzeugkatalog. Für größere Kontexte, benötigt aber mehr Tokens."
            AppLanguage.PORTUGUESE -> "Envia o catálogo compacto completo. Para modelos com contexto maior, mas usa mais tokens."
            AppLanguage.FRENCH -> "Envoie le catalogue compact complet. Pour les modèles à grand contexte, avec davantage de jetons."
            AppLanguage.ENGLISH -> "Sends the complete compact catalog. Best for larger-context models, but uses more tokens."
        }
        "api_knobs_title" -> when (language) {
            AppLanguage.CHINESE -> "对 API 模型使用生成参数"
            AppLanguage.SPANISH -> "Usar controles de generación para API"
            AppLanguage.GERMAN -> "Generierungsregler für API-Modelle"
            AppLanguage.PORTUGUESE -> "Usar controles de geração para APIs"
            AppLanguage.FRENCH -> "Utiliser les réglages de génération API"
            AppLanguage.ENGLISH -> "Use generation knobs for API models"
        }
        "api_knobs_description" -> when (language) {
            AppLanguage.CHINESE -> "关闭时，提供商模型会保持现有默认值。"
            AppLanguage.SPANISH -> "Desactivado mantiene los modelos del proveedor con sus valores predeterminados."
            AppLanguage.GERMAN -> "Aus lässt Anbieter-Modelle bei ihren vorhandenen Standardwerten."
            AppLanguage.PORTUGUESE -> "Desativado mantém os modelos do provedor nos padrões atuais."
            AppLanguage.FRENCH -> "Désactivé garde les modèles fournisseur sur leurs valeurs par défaut."
            AppLanguage.ENGLISH -> "Off keeps provider models on their existing defaults."
        }
        "max_tokens" -> when (language) {
            AppLanguage.CHINESE -> "最大 token 数"
            AppLanguage.SPANISH -> "Tokens máximos"
            AppLanguage.GERMAN -> "Max. Tokens"
            AppLanguage.PORTUGUESE -> "Tokens máximos"
            AppLanguage.FRENCH -> "Tokens max."
            AppLanguage.ENGLISH -> "Max tokens"
        }
        "accelerator" -> when (language) {
            AppLanguage.CHINESE -> "加速器"
            AppLanguage.SPANISH -> "Acelerador"
            AppLanguage.GERMAN -> "Beschleuniger"
            AppLanguage.PORTUGUESE -> "Acelerador"
            AppLanguage.FRENCH -> "Accélérateur"
            AppLanguage.ENGLISH -> "Accelerator"
        }
        "accelerator_description" -> when (language) {
            AppLanguage.CHINESE -> "自动会使用 Agent 运行时默认值。可选择经过实现的 CPU 或 GPU 路径；Agent 尚未实现独立 NPU 后端。"
            AppLanguage.SPANISH -> "Auto mantiene el valor predeterminado. Elige las rutas CPU o GPU implementadas; Agent aún no implementa un backend NPU independiente."
            AppLanguage.GERMAN -> "Auto nutzt den Runtime-Standard. Wähle die implementierten CPU- oder GPU-Pfade; Agent hat noch kein separates NPU-Backend."
            AppLanguage.PORTUGUESE -> "Auto mantém o padrão do runtime. Escolha as rotas CPU ou GPU implementadas; o Agent ainda não implementa um backend NPU separado."
            AppLanguage.FRENCH -> "Auto garde le réglage du runtime. Choisissez les chemins CPU ou GPU implémentés ; Agent n’a pas encore de backend NPU distinct."
            AppLanguage.ENGLISH -> "Auto keeps the runtime default. Choose the implemented CPU or GPU paths; Agent does not yet implement a separate NPU backend."
        }
        "system_prompt_placeholder" -> when (language) {
            AppLanguage.CHINESE -> "Agent 回复的可选指令。"
            AppLanguage.SPANISH -> "Instrucciones opcionales para las respuestas de Agent."
            AppLanguage.GERMAN -> "Optionale Anweisungen für Agent-Antworten."
            AppLanguage.PORTUGUESE -> "Instruções opcionais para respostas do Agent."
            AppLanguage.FRENCH -> "Instructions facultatives pour les réponses de Agent."
            AppLanguage.ENGLISH -> "Optional instructions for Agent replies."
        }
        "clear_prompt" -> when (language) {
            AppLanguage.CHINESE -> "清空提示词"
            AppLanguage.SPANISH -> "Borrar prompt"
            AppLanguage.GERMAN -> "Prompt löschen"
            AppLanguage.PORTUGUESE -> "Limpar prompt"
            AppLanguage.FRENCH -> "Effacer le prompt"
            AppLanguage.ENGLISH -> "Clear prompt"
        }
        "save_model_configuration" -> when (language) {
            AppLanguage.CHINESE -> "保存模型配置"
            AppLanguage.SPANISH -> "Guardar configuración del modelo"
            AppLanguage.GERMAN -> "Modellkonfiguration speichern"
            AppLanguage.PORTUGUESE -> "Salvar configuração do modelo"
            AppLanguage.FRENCH -> "Enregistrer la configuration modèle"
            AppLanguage.ENGLISH -> "Save model configuration"
        }
        "default" -> when (language) {
            AppLanguage.CHINESE -> "默认"
            AppLanguage.SPANISH -> "Predeterminado"
            AppLanguage.GERMAN -> "Standard"
            AppLanguage.PORTUGUESE -> "Padrão"
            AppLanguage.FRENCH -> "Par défaut"
            AppLanguage.ENGLISH -> "Default"
        }
        "auto" -> when (language) {
            AppLanguage.CHINESE -> "自动"
            AppLanguage.SPANISH -> "Auto"
            AppLanguage.GERMAN -> "Auto"
            AppLanguage.PORTUGUESE -> "Auto"
            AppLanguage.FRENCH -> "Auto"
            AppLanguage.ENGLISH -> "Auto"
        }
        else -> key
    }
}

private fun incrementGenerationValue(value: Int, defaultValue: Int, maxValue: Int, step: Int): Int {
    val base = if (value == defaultValue) 0 else value
    return (base + step).coerceAtMost(maxValue)
}

private fun decrementGenerationValue(value: Int, defaultValue: Int, minValue: Int, step: Int): Int {
    val next = (value - step).coerceAtLeast(minValue)
    return if (next <= 0) defaultValue else next
}

private fun formatGenerationDecimal(value: Float): String {
    return String.format(Locale.US, "%.2f", value)
}

@Composable
private fun AgentPersonaCard(
    customSystemPrompt: String,
    onPromptChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = shell.personaTitle.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = glass.mutedText,
            modifier = Modifier.padding(start = 4.dp),
        )
        ShellGroupCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(shell.personaBody, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
                OutlinedTextField(
                    value = customSystemPrompt,
                    onValueChange = onPromptChange,
                    label = { Text(strings.customSystemPromptLabel()) },
                    placeholder = { Text(strings.customSystemPromptPlaceholder()) },
                    modifier = Modifier.fillMaxWidth().testTag("AgentPersonaPrompt"),
                    minLines = 3,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                )
                Text(
                    strings.characterCount(customSystemPrompt.length, AppSettings.MAX_CUSTOM_SYSTEM_PROMPT_CHARS),
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.mutedText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(
                        modifier = Modifier.testTag("ClearAgentPersonaButton"),
                        onClick = onClear,
                        enabled = customSystemPrompt.isNotBlank(),
                    ) { Text(strings.clearLabel()) }
                    Button(modifier = Modifier.testTag("SaveAgentPersonaButton"), onClick = onSave) {
                        Text(strings.savePersonaLabel())
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    keywordHighlightingEnabled: Boolean,
    themePrimaryHex: String,
    themeSecondaryHex: String,
    themeBackgroundHex: String,
    themeSurfaceHex: String,
    themeSurfaceVariantHex: String,
    themeCardShape: String,
    uiFontScale: Float,
    onKeywordHighlightingChange: (Boolean) -> Unit,
    onPrimaryHexChange: (String) -> Unit,
    onSecondaryHexChange: (String) -> Unit,
    onBackgroundHexChange: (String) -> Unit,
    onSurfaceHexChange: (String) -> Unit,
    onSurfaceVariantHexChange: (String) -> Unit,
    onCardShapeChange: (String) -> Unit,
    onUiFontScaleChange: (Float) -> Unit,
    onApplyPreset: (AppearanceThemePreset) -> Unit,
    onSaveAppearance: () -> Unit,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    Column(
        modifier = Modifier.fillMaxWidth().testTag("HermesAppearanceCard"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = shell.appearanceThemeSection.uppercase(),
            modifier = Modifier.padding(start = 4.dp).testTag("HermesAppearanceCardTop"),
            style = MaterialTheme.typography.labelMedium,
            color = glass.mutedText,
        )
        ShellGroupCard {
            appearanceThemePresets.forEachIndexed { index, preset ->
                val active = preset.primaryHex.equals(themePrimaryHex, ignoreCase = true) &&
                    preset.backgroundHex.equals(themeBackgroundHex, ignoreCase = true)
                ShellListRow(
                    title = strings.appearancePresetLabel(preset.id, preset.label),
                    modifier = Modifier
                        .testTag("AppearancePreset-${preset.id}")
                        .semantics { selected = active },
                    onClick = {
                        onApplyPreset(preset)
                        onSaveAppearance()
                    },
                    leading = { ShellRowIcon(R.drawable.ic_set_palette) },
                    trailing = if (active) ({ AppearanceCheck() }) else null,
                )
                if (index != appearanceThemePresets.lastIndex) ShellRowDivider(startInset = 54.dp)
            }
        }

        Text(
            text = shell.appearanceAccentSection.uppercase(),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = glass.mutedText,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            appearanceAccentChoices.forEach { hex ->
                val active = hex.equals(themePrimaryHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(parseAppearanceColor(hex))
                        .then(
                            if (active) Modifier.border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .border(5.dp, parseAppearanceColor(hex), CircleShape)
                            else Modifier,
                        )
                        .clickable {
                            onPrimaryHexChange(hex)
                            onSaveAppearance()
                        }
                        .semantics { selected = active }
                        .testTag("AppearanceAccent-$hex"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) AppearanceCheck(tint = Color.White)
                }
            }
        }

        Text(
            text = shell.appearanceTextSection.uppercase(),
            modifier = Modifier.padding(start = 4.dp, top = 8.dp).testTag("UiFontScaleValueLabel"),
            style = MaterialTheme.typography.labelMedium,
            color = glass.mutedText,
        )
        ShellGroupCard {
            val sizes = listOf(
                0.85f to shell.sizeSmall,
                1.0f to shell.sizeNormal,
                1.15f to shell.sizeLarge,
                1.3f to shell.sizeExtraLarge,
            )
            sizes.forEachIndexed { index, (scale, label) ->
                val active = kotlin.math.abs(uiFontScale - scale) < 0.03f
                ShellListRow(
                    title = label,
                    subtitle = shell.sizePercent((scale * 100).toInt()),
                    modifier = Modifier.testTag("UiFontScale-${(scale * 100).toInt()}").semantics { selected = active },
                    onClick = {
                        onUiFontScaleChange(scale)
                        onSaveAppearance()
                    },
                    leading = { ShellRowIcon(R.drawable.ic_set_sliders) },
                    trailing = if (active) ({ AppearanceCheck() }) else null,
                )
                if (index != sizes.lastIndex) ShellRowDivider(startInset = 54.dp)
            }
        }

        ShellGroupCard(modifier = Modifier.padding(top = 8.dp)) {
            ShellListRow(
                title = strings.keywordHighlightingTitle(),
                subtitle = strings.keywordHighlightingDescription(),
                trailing = {
                    Switch(
                        checked = keywordHighlightingEnabled,
                        onCheckedChange = {
                            onKeywordHighlightingChange(it)
                            onSaveAppearance()
                        },
                        modifier = Modifier.semantics { contentDescription = strings.keywordHighlightingTitle() },
                    )
                },
            )
        }

        SettingsDisclosure(
            sectionId = "Appearance-advanced",
            title = shell.appearanceAdvanced,
            summary = strings.appearanceDescription(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = themePrimaryHex,
                    onValueChange = onPrimaryHexChange,
                    label = { Text(strings.accentHexLabel()) },
                    modifier = Modifier.fillMaxWidth().testTag("AppearancePrimaryHexField"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = themeSecondaryHex,
                    onValueChange = onSecondaryHexChange,
                    label = { Text(strings.secondaryAccentHexLabel()) },
                    modifier = Modifier.fillMaxWidth().testTag("AppearanceSecondaryHexField"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = themeBackgroundHex,
                    onValueChange = onBackgroundHexChange,
                    label = { Text(strings.backgroundHexLabel()) },
                    modifier = Modifier.fillMaxWidth().testTag("AppearanceBackgroundHexField"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = themeSurfaceHex,
                    onValueChange = onSurfaceHexChange,
                    label = { Text(strings.composerSurfaceHexLabel()) },
                    modifier = Modifier.fillMaxWidth().testTag("AppearanceSurfaceHexField"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = themeSurfaceVariantHex,
                    onValueChange = onSurfaceVariantHexChange,
                    label = { Text(strings.assistantPanelHexLabel()) },
                    modifier = Modifier.fillMaxWidth().testTag("AppearanceSurfaceVariantHexField"),
                    singleLine = true,
                )
                Text(strings.cardsAndBoxesTitle(), style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    appearanceCardShapes.forEach { shape ->
                        Button(
                            modifier = Modifier
                                .testTag("CardShape-$shape")
                                .semantics { selected = themeCardShape == shape },
                            onClick = { onCardShapeChange(shape) },
                            enabled = themeCardShape != shape,
                        ) {
                            Text(strings.cardShapeLabel(shape))
                        }
                    }
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("SaveAppearanceButton"),
                    onClick = onSaveAppearance,
                ) {
                    Text(strings.saveAppearanceLabel())
                }
            }
        }
    }
}

private val appearanceAccentChoices = listOf("#34A774", "#0066D6", "#F4A83A", "#E5484D", "#8E6CEF")

private fun parseAppearanceColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

@Composable
private fun AppearanceCheck(tint: Color = LocalHermesGlassTokens.current.success) {
    Icon(
        painter = painterResource(R.drawable.ic_ui_check),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun OfflineAirplaneCard(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    ShellGroupCard {
        ShellListRow(
            title = strings.offlineAirplaneModeTitle(),
            subtitle = strings.offlineAirplaneModeDescription(),
            modifier = Modifier.testTag("OfflineAirplaneRow"),
            onClick = { onChange(!enabled) },
            trailing = {
                Switch(
                    checked = enabled,
                    onCheckedChange = onChange,
                    modifier = Modifier.semantics { contentDescription = strings.offlineAirplaneModeTitle() },
                )
            },
        )
    }
}

@Composable
private fun SettingsHelpCard(
    providerLabel: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    SettingsDisclosure(
        sectionId = "General-about",
        title = shell.aboutTitle,
        summary = shell.aboutSummary,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.forkDisclosure(), style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
            Text(
                if (com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) strings.playEditionSummary() else strings.settingsHelpStart,
                style = MaterialTheme.typography.bodyMedium,
            )
            // Accounts keeps app sign-in separate from provider key setup.
            if (!com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
                Text(strings.settingsHelpAccounts, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val popularProviderIds = listOf("openrouter", "anthropic", "openai", "deepseek", "moonshot", "alibaba")

@Composable
private fun RemoteFallbackCard(
    providerId: String,
    providerLabel: String,
    baseUrl: String,
    model: String,
    apiKey: String,
    status: String,
    onSelectProvider: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onOpenProviderKeyPage: (String, String) -> Unit,
    onCopyProviderKeyPage: (String, String) -> Unit,
    onCheckProviderKeyPage: (String, String) -> Unit,
    onImportProviderCredential: () -> Unit,
    onSave: () -> Unit,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val play = com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION
    // The provider the app answers with right now; the rows below edit a draft until Save.
    val saved = remember(status) { com.mobilefork.hermesagent.data.AppSettingsStore(context).load() }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var othersOpen by rememberSaveable { mutableStateOf(false) }
    val allowed = remember(play) {
        ProviderPresets.androidSettingsDefaults.filter {
            !play || com.mobilefork.hermesagent.play.PlayNetworkPolicy.supportsProvider(it.id)
        }
    }
    val popular = remember(allowed) { popularProviderIds.mapNotNull { id -> allowed.firstOrNull { it.id == id } } }
    val others = remember(allowed) { allowed.filter { it.id !in popularProviderIds && it.id != "custom" } }
    val custom = allowed.firstOrNull { it.id == "custom" }

    @Composable
    fun ProviderRow(preset: com.mobilefork.hermesagent.data.ProviderPreset) {
        val isActive = preset.id == saved.provider
        val isOpen = expandedId == preset.id
        ShellListRow(
            title = strings.providerDisplayLabel(preset.id, preset.label),
            subtitle = if (isActive) saved.model.ifBlank { preset.modelHint.ifBlank { shell.noModelSelected } }
            else if (preset.id == "custom") shell.modelsCustomSub else preset.modelHint,
            modifier = Modifier.testTag("HermesProviderRow-${preset.id}").semantics { selected = isOpen },
            onClick = {
                if (isOpen) {
                    expandedId = null
                } else {
                    expandedId = preset.id
                    if (preset.id != providerId) onSelectProvider(preset.id)
                }
            },
            leading = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(com.mobilefork.hermesagent.ui.theme.hermesAvatarPalette[
                            allowed.indexOfFirst { it.id == preset.id }.coerceAtLeast(0) %
                                com.mobilefork.hermesagent.ui.theme.hermesAvatarPalette.size
                        ]),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (if (preset.id == "custom") "+" else preset.label.take(1).uppercase()),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                }
            },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isActive) {
                        Text(
                            text = shell.modelsActiveBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.success,
                            modifier = Modifier
                                .background(glass.successContainer, RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_ui_chevron_right),
                        contentDescription = null,
                        tint = glass.mutedText,
                        modifier = Modifier.size(20.dp).rotate(if (isOpen) 90f else 0f),
                    )
                }
            },
        )
        if (isOpen && preset.id == providerId) {
            ProviderEditor(
                preset = preset,
                baseUrl = baseUrl,
                model = model,
                apiKey = apiKey,
                status = status,
                play = play,
                onBaseUrlChange = onBaseUrlChange,
                onModelChange = onModelChange,
                onApiKeyChange = onApiKeyChange,
                onOpenProviderKeyPage = onOpenProviderKeyPage,
                onCopyProviderKeyPage = onCopyProviderKeyPage,
                onCheckProviderKeyPage = onCheckProviderKeyPage,
                onImportProviderCredential = onImportProviderCredential,
                onSave = onSave,
                strings = strings,
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("HermesRemoteProviderCard"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = com.mobilefork.hermesagent.ui.i18n.PlaySettingsText.remoteProviderHelp(strings, play),
            style = MaterialTheme.typography.bodySmall,
            color = glass.mutedText,
            modifier = Modifier.padding(horizontal = 4.dp).testTag("HermesRemoteProviderHelp"),
        )
        Text(
            text = shell.modelsPopular.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = glass.mutedText,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        ShellGroupCard {
            popular.forEachIndexed { index, preset ->
                ProviderRow(preset)
                if (index != popular.lastIndex) ShellRowDivider(startInset = 70.dp)
            }
        }
        if (others.isNotEmpty()) {
            Text(
                text = shell.modelsOthers.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = glass.mutedText,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
            ShellGroupCard {
                ShellListRow(
                    title = shell.modelsOthersCount(others.size),
                    modifier = Modifier.testTag("HermesProviderOthersToggle"),
                    onClick = { othersOpen = !othersOpen },
                    trailing = {
                        Icon(
                            painter = painterResource(R.drawable.ic_ui_chevron_right),
                            contentDescription = null,
                            tint = glass.mutedText,
                            modifier = Modifier.size(20.dp).rotate(if (othersOpen) 90f else 0f),
                        )
                    },
                )
                if (othersOpen) {
                    others.forEach { preset ->
                        ShellRowDivider(startInset = 70.dp)
                        ProviderRow(preset)
                    }
                }
            }
        }
        if (custom != null) {
            Text(
                text = shell.modelsCustomRow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = glass.mutedText,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
            ShellGroupCard { ProviderRow(custom) }
        }
    }
}

@Composable
private fun ProviderEditor(
    preset: com.mobilefork.hermesagent.data.ProviderPreset,
    baseUrl: String,
    model: String,
    apiKey: String,
    status: String,
    play: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onOpenProviderKeyPage: (String, String) -> Unit,
    onCopyProviderKeyPage: (String, String) -> Unit,
    onCheckProviderKeyPage: (String, String) -> Unit,
    onImportProviderCredential: () -> Unit,
    onSave: () -> Unit,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val isCustom = preset.id == "custom"
    val providerLabel = strings.providerDisplayLabel(preset.id, preset.label)
    val customEndpointPreview = if (isCustom) {
        runCatching { HermesEndpointUrl.chatCompletionsUrl(baseUrl) }.getOrNull()
    } else {
        null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
            .padding(16.dp)
            .testTag("HermesProviderEditor-${preset.id}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isCustom) {
            Text(shell.modelsCustomNote, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text(strings.baseUrlLabel()) },
                placeholder = { Text("https://example.com/v1") },
                modifier = Modifier.fillMaxWidth().testTag("HermesBaseUrlField"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Text(strings.customEndpointConnectionHint(), style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
            customEndpointPreview?.let { preview ->
                Text(
                    text = strings.customEndpointPreview(preview),
                    modifier = Modifier.testTag("HermesEndpointDebugPreview"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text(strings.modelLabel()) },
            modifier = Modifier.fillMaxWidth().testTag("HermesModelField"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        preset.modelHint.takeIf { it.isNotBlank() && it != model.trim() }?.let { modelHint ->
            Row(
                modifier = Modifier.clickable { onModelChange(modelHint) }.padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("${shell.modelSuggested}:", style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
                Text(modelHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text(strings.apiKeyLabel()) },
            modifier = Modifier.fillMaxWidth().testTag("HermesApiKeyField"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Text(
            text = if (apiKey.isNotBlank()) shell.modelsKeySaved else shell.modelsNoKey,
            style = MaterialTheme.typography.labelMedium,
            color = if (apiKey.isNotBlank()) glass.success else glass.mutedText,
            modifier = Modifier.testTag("HermesRemoteKeyState"),
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("HermesRemoteSave")) {
            Text(shell.modelsSaveUse)
        }
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
        }
        val keyUrl = preset.apiKeyUrl.takeIf { it.isNotBlank() && !play }
        if (keyUrl != null || !play) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (keyUrl != null) {
                    OutlinedButton(onClick = { onOpenProviderKeyPage(preset.id, keyUrl) }) {
                        Text(strings.openProviderKeyPage(providerLabel))
                    }
                    OutlinedButton(onClick = { onCopyProviderKeyPage(preset.id, keyUrl) }) {
                        Text(strings.copyProviderSetupUrl())
                    }
                    OutlinedButton(onClick = { onCheckProviderKeyPage(preset.id, keyUrl) }) {
                        Text(strings.checkProviderSetupUrl())
                    }
                }
                if (!play) {
                    OutlinedButton(onClick = onImportProviderCredential) {
                        Text(strings.importSavedProviderCredential())
                    }
                }
            }
        }
        Text(
            strings.providerCredentialInputHelp(ProviderPresets.apiKeyEnvVars(preset.id)),
            style = MaterialTheme.typography.bodySmall,
            color = glass.mutedText,
        )
    }
}

@Composable
private fun OnDeviceInferenceCard(
    onDeviceBackend: String,
    speculativeDecodingMode: String,
    onSelectBackend: (String) -> Unit,
    onSelectSpeculativeDecodingMode: (String) -> Unit,
    onStartRuntime: (String) -> Unit,
    summary: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(strings.onDeviceInferenceTitle.ifBlank { "On-device inference" }, style = MaterialTheme.typography.titleMedium)
            Text(strings.onDeviceInferenceDescription, style = MaterialTheme.typography.bodySmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onSelectBackend(BackendKind.LLAMA_CPP.persistedValue)
                        onStartRuntime("GGUF")
                    },
                    enabled = onDeviceBackend != BackendKind.LLAMA_CPP.persistedValue,
                ) {
                    Text(strings.llamaCppLabel.ifBlank { "llama.cpp (GGUF)" })
                }
                Button(
                    onClick = {
                        onSelectBackend(BackendKind.LITERT_LM.persistedValue)
                        onStartRuntime("LiteRT-LM")
                    },
                    enabled = onDeviceBackend != BackendKind.LITERT_LM.persistedValue,
                ) {
                    Text(strings.liteRtLmLabel.ifBlank { "LiteRT-LM" })
                }
                Button(
                    onClick = { onSelectBackend(BackendKind.NONE.persistedValue) },
                    enabled = onDeviceBackend != BackendKind.NONE.persistedValue,
                ) {
                    Text(strings.remoteOnly())
                }
            }
            Text(com.mobilefork.hermesagent.ui.i18n.PlaySettingsText.localModelHelp(
                strings, com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("HermesLocalModelHelp"))
            Text(strings.liteRtLmDescription, style = MaterialTheme.typography.bodySmall)
            Text(strings.gemma4MtpTitle(), style = MaterialTheme.typography.titleSmall)
            Text(strings.gemma4MtpDescription(), style = MaterialTheme.typography.bodySmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                speculativeDecodingChoices().forEach { choice ->
                    Button(
                        modifier = Modifier.testTag("LiteRtLmMtpMode-${choice.value}"),
                        onClick = { onSelectSpeculativeDecodingMode(choice.value) },
                        enabled = speculativeDecodingMode != choice.value,
                    ) {
                        Text(choice.label(strings))
                    }
                }
            }
            SelectionContainer {
                Text(localizedOnDeviceSummary(summary, strings), style = MaterialTheme.typography.bodySmall)
            }
            LocalModelDiagnosticsControls()
        }
    }
}

private data class SpeculativeDecodingChoice(
    val value: String,
    val label: (com.mobilefork.hermesagent.ui.i18n.HermesStrings) -> String,
)

private fun speculativeDecodingChoices(): List<SpeculativeDecodingChoice> = listOf(
    SpeculativeDecodingChoice("auto") { it.gemma4MtpAutoLabel() },
    SpeculativeDecodingChoice("enabled") { it.gemma4MtpEnabledLabel() },
    SpeculativeDecodingChoice("disabled") { it.gemma4MtpDisabledLabel() },
)

private fun localizedOnDeviceSummary(
    summary: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
): String {
    val trimmed = summary.trim()
    return when {
        trimmed.isBlank() -> strings.noCompatibleLocalModel
        trimmed == "Remote provider mode" -> strings.remoteProviderMode()
        trimmed == "Checking preferred local model…" -> strings.checkingPreferredLocalModel()
        trimmed.startsWith("No preferred local model") -> strings.noCompatibleLocalModel
        trimmed.startsWith("Preferred local model:") ->
            "${strings.preferredLocalModel}: ${trimmed.substringAfter(':').trim()}"
        else -> trimmed
    }
}

@Composable
private fun LanguagePickerCard(
    currentLanguageTag: String,
    onSelectLanguage: (UiLanguage) -> Unit,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val selected = UiLanguage.entries.firstOrNull { it.tag == normalizeUiLanguageTag(currentLanguageTag) }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("SettingsLanguagePicker"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = shell.languageTitle.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = glass.mutedText,
            modifier = Modifier.padding(start = 4.dp),
        )
        ShellGroupCard {
            UiLanguage.entries.forEachIndexed { index, language ->
                val active = language == selected
                ShellListRow(
                    title = language.nativeLabel,
                    modifier = Modifier
                        .testTag("SettingsLanguage-${language.tag}")
                        .semantics {
                            this.selected = active
                            contentDescription = if (active) strings.selectedLanguageDescription(language.nativeLabel)
                            else strings.switchLanguageDescription(language.nativeLabel)
                        },
                    onClick = { if (!active) onSelectLanguage(language) },
                    leading = { Text(language.flag, style = MaterialTheme.typography.headlineSmall) },
                    trailing = if (active) ({ AppearanceCheck() }) else null,
                )
                if (index != UiLanguage.entries.lastIndex) ShellRowDivider(startInset = 62.dp)
            }
        }
        if (selected != null) {
            // Kept for screen readers and tests: the language in use, as written in that language.
            Text(
                text = "${selected.flag} ${selected.nativeLabel}",
                modifier = Modifier.size(0.dp).testTag("SettingsLanguageCurrent"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
