package com.mobilefork.hermesagent.ui.onboarding

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mobilefork.hermesagent.BuildConfig
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentPurpose
import com.mobilefork.hermesagent.data.AgentStore
import com.mobilefork.hermesagent.data.AgentTone
import com.mobilefork.hermesagent.data.OnboardingAnswers
import com.mobilefork.hermesagent.data.OnboardingStore
import com.mobilefork.hermesagent.data.ProviderPresets
import com.mobilefork.hermesagent.data.onboardingPersona
import com.mobilefork.hermesagent.data.personaAfterOnboarding
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.agents.AvatarPickerSheet
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.ui.i18n.UiLanguage
import com.mobilefork.hermesagent.ui.settings.SettingsViewModel
import com.mobilefork.hermesagent.ui.shell.ShellChoiceChip
import com.mobilefork.hermesagent.ui.shell.ShellEntryField
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.theme.Button
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

private const val STEP_COUNT = 6
private val ONBOARDING_PROVIDERS = listOf("openrouter", "anthropic", "openai", "deepseek", "moonshot", "alibaba")

/**
 * The first-run guide: welcome, language, the agent's brain, the agent itself, permissions, and
 * the person. Every step can be left for later; nothing is saved until the last one, apart from
 * the language, which the rest of the guide is shown in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    onLanguageChanged: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val shell = LocalShellStrings.current
    val store = remember(context) { OnboardingStore(context) }
    val agentStore = remember(context) { AgentStore(context) }
    val previous = remember(store) { store.answers() }
    val settings by settingsViewModel.uiState.collectAsState()
    LaunchedEffect(settingsViewModel) { settingsViewModel.reload() }

    var step by rememberSaveable { mutableIntStateOf(0) }
    var agentName by rememberSaveable { mutableStateOf(agentStore.main().name) }
    var tone by rememberSaveable { mutableStateOf(previous.tone) }
    var purposes by rememberSaveable { mutableStateOf(previous.purposes.toList()) }
    var userName by rememberSaveable { mutableStateOf(previous.userName) }
    var userAvatar by rememberSaveable { mutableStateOf(previous.userAvatar) }
    var providerChosen by rememberSaveable { mutableStateOf(false) }
    var choosingAvatar by remember { mutableStateOf(false) }

    fun finish() {
        val answers = OnboardingAnswers(userName, userAvatar, tone, purposes.toSet())
        val generated = onboardingPersona(agentName, answers)
        settingsViewModel.updateCustomSystemPrompt(
            personaAfterOnboarding(settings.customSystemPrompt, store.lastGeneratedPersona(), generated),
        )
        settingsViewModel.save()
        runCatching { agentStore.saveMainName(agentName) }
        store.finish(answers, generated)
        onFinished()
    }

    fun skipAll() {
        store.skip()
        onFinished()
    }

    BackHandler(enabled = step > 0) { step -= 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("HermesOnboarding"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(104.dp)) {
                if (step > 0) {
                    QuietAction(shell.extra.obBack, "HermesOnboardingBack") { step -= 1 }
                }
            }
            StepDots(step = step, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(104.dp), contentAlignment = Alignment.CenterEnd) {
                if (step < STEP_COUNT - 1) {
                    QuietAction(shell.extra.obSkip, "HermesOnboardingSkip", onClick = ::skipAll)
                }
            }
        }
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(260)) { if (forward) it / 6 else -it / 6 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(160)))
            },
            label = "onboardingStep",
        ) { shown ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (shown) {
                    0 -> WelcomeStep()
                    1 -> LanguageStep(
                        selected = settings.languageTag,
                        onSelect = { language ->
                            settingsViewModel.selectUiLanguage(language)
                            onLanguageChanged()
                        },
                    )
                    2 -> BrainStep(
                        provider = settings.provider.takeIf { providerChosen }.orEmpty(),
                        apiKey = settings.apiKey,
                        onProvider = { id ->
                            providerChosen = true
                            settingsViewModel.updateProvider(id)
                        },
                        onApiKey = settingsViewModel::updateApiKey,
                        onGetKey = { id, url -> settingsViewModel.openProviderKeyPage(id, url) },
                    )
                    3 -> AgentStep(
                        name = agentName,
                        onName = { agentName = it.take(AgentProfile.MAX_NAME_CHARS) },
                        tone = tone,
                        onTone = { tone = it },
                        purposes = purposes.toSet(),
                        onTogglePurpose = { purpose ->
                            purposes = if (purpose in purposes) purposes - purpose else purposes + purpose
                        },
                    )
                    4 -> PermissionsStep(showPhoneControl = AgentPurpose.Phone in purposes)
                    else -> YouStep(
                        name = userName,
                        onName = { userName = it.take(AgentProfile.MAX_NAME_CHARS) },
                        avatar = userAvatar,
                        onChooseAvatar = { choosingAvatar = true },
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { if (step < STEP_COUNT - 1) step += 1 else finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("HermesOnboardingNext"),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    when (step) {
                        0 -> shell.extra.obStart
                        STEP_COUNT - 1 -> shell.extra.obDone
                        else -> shell.extra.obContinue
                    },
                )
            }
        }
    }
    if (choosingAvatar) {
        AvatarPickerSheet(
            agent = AgentProfile(id = "you", name = userName.ifBlank { "?" }, avatarPath = userAvatar),
            onChoose = { userAvatar = it },
            onPickPhoto = null,
            onDismiss = { choosingAvatar = false },
        )
    }
}

@Composable
private fun QuietAction(label: String, testTag: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag),
        style = MaterialTheme.typography.labelLarge,
        color = LocalHermesGlassTokens.current.mutedText,
        maxLines = 1,
    )
}

@Composable
private fun StepDots(step: Int, modifier: Modifier = Modifier) {
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(STEP_COUNT) { index ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (index == step) 18.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (index <= step) MaterialTheme.colorScheme.primary else glass.border),
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = LocalHermesGlassTokens.current.mutedText)
    }
}

@Composable
private fun ColumnScope.WelcomeStep() {
    val shell = LocalShellStrings.current
    Spacer(Modifier.height(48.dp))
    Image(
        painter = painterResource(R.drawable.agent_logo),
        contentDescription = null,
        modifier = Modifier
            .size(104.dp)
            .clip(CircleShape)
            .align(Alignment.CenterHorizontally),
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(shell.extra.obWelcomeTitle, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(
            shell.extra.obWelcomeBody,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalHermesGlassTokens.current.mutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LanguageStep(selected: String, onSelect: (UiLanguage) -> Unit) {
    val shell = LocalShellStrings.current
    StepTitle(shell.extra.obLanguageTitle, shell.extra.obLanguageBody)
    ShellGroupCard {
        UiLanguage.entries.forEachIndexed { index, language ->
            if (index > 0) ShellRowDivider(startInset = 56.dp)
            SelectRow(
                selected = selected == language.tag,
                testTag = "HermesOnboardingLanguage-${language.tag}",
                onClick = { onSelect(language) },
            ) {
                Text(language.flag, style = MaterialTheme.typography.titleLarge)
                Text(language.nativeLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SelectRow(
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrainStep(
    provider: String,
    apiKey: String,
    onProvider: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onGetKey: (String, String) -> Unit,
) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val presets = remember {
        ONBOARDING_PROVIDERS.mapNotNull(ProviderPresets::find).filter {
            !BuildConfig.HERMES_PLAY_EDITION || com.mobilefork.hermesagent.play.PlayNetworkPolicy.supportsProvider(it.id)
        }
    }
    StepTitle(shell.extra.obBrainTitle, shell.extra.obBrainBody)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            ShellChoiceChip(
                label = preset.label.removeSuffix(" API key"),
                selected = provider == preset.id,
                onClick = { onProvider(preset.id) },
                testTag = "HermesOnboardingProvider-${preset.id}",
            )
        }
    }
    val chosen = presets.firstOrNull { it.id == provider }
    if (chosen != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShellGroupCard {
                BasicTextField(
                    value = apiKey,
                    onValueChange = onApiKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("HermesOnboardingApiKey"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    decorationBox = { inner ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            if (apiKey.isEmpty()) {
                                Text(shell.extra.obBrainKey, style = MaterialTheme.typography.bodyLarge, color = glass.mutedText)
                            }
                            inner()
                        }
                    },
                )
            }
            if (chosen.apiKeyUrl.isNotBlank()) {
                QuietAction(shell.extra.obBrainGetKey, "HermesOnboardingGetKey") { onGetKey(chosen.id, chosen.apiKeyUrl) }
            }
        }
    }
    Text(shell.extra.obBrainLocal, style = MaterialTheme.typography.bodyMedium, color = glass.mutedText)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentStep(
    name: String,
    onName: (String) -> Unit,
    tone: AgentTone?,
    onTone: (AgentTone) -> Unit,
    purposes: Set<AgentPurpose>,
    onTogglePurpose: (AgentPurpose) -> Unit,
) {
    val shell = LocalShellStrings.current
    val extra = shell.extra
    StepTitle(extra.obAgentTitle, extra.obAgentBody)
    FieldLabel(extra.obAgentName)
    ShellGroupCard {
        ShellEntryField(
            value = name,
            onValueChange = onName,
            placeholder = extra.obAgentName,
            testTag = "HermesOnboardingAgentName",
            primary = true,
        )
    }
    Labeled(extra.obTone) {
        AgentTone.entries.forEach { choice ->
            ShellChoiceChip(
                label = when (choice) {
                    AgentTone.Concise -> extra.toneConcise
                    AgentTone.Friendly -> extra.toneFriendly
                    AgentTone.Professional -> extra.toneProfessional
                },
                selected = tone == choice,
                onClick = { onTone(choice) },
                testTag = "HermesOnboardingTone-${choice.name}",
            )
        }
    }
    Labeled(extra.obPurpose) {
        AgentPurpose.entries.forEach { purpose ->
            ShellChoiceChip(
                label = when (purpose) {
                    AgentPurpose.Productivity -> extra.purposeProductivity
                    AgentPurpose.Coding -> extra.purposeCoding
                    AgentPurpose.Phone -> extra.purposePhone
                    AgentPurpose.Research -> extra.purposeResearch
                    AgentPurpose.Learning -> extra.purposeLearning
                },
                selected = purpose in purposes,
                onClick = { onTogglePurpose(purpose) },
                testTag = "HermesOnboardingPurpose-${purpose.name}",
            )
        }
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        label,
        modifier = Modifier.padding(bottom = 0.dp),
        style = MaterialTheme.typography.labelMedium,
        color = LocalHermesGlassTokens.current.mutedText,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalHermesGlassTokens.current.mutedText)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun PermissionsStep(showPhoneControl: Boolean) {
    val context = LocalContext.current
    val shell = LocalShellStrings.current
    val extra = shell.extra
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    var micGranted by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notificationsGranted by remember {
        mutableStateOf(!needsNotificationPermission || granted(Manifest.permission.POST_NOTIFICATIONS))
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsGranted = it
    }
    StepTitle(extra.obPermsTitle, extra.obPermsBody)
    ShellGroupCard {
        PermissionRow(extra.permMic, extra.permMicBody, micGranted, "HermesOnboardingPermMic") {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        if (needsNotificationPermission) {
            ShellRowDivider(startInset = 16.dp)
            PermissionRow(extra.permNotif, extra.permNotifBody, notificationsGranted, "HermesOnboardingPermNotif") {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (showPhoneControl && !BuildConfig.HERMES_PLAY_EDITION) {
            ShellRowDivider(startInset = 16.dp)
            PermissionRow(extra.permPhone, extra.permPhoneBody, granted = false, testTag = "HermesOnboardingPermPhone") {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean, testTag: String, onAllow: () -> Unit) {
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodySmall, color = glass.mutedText)
        }
        if (granted) {
            Text(
                shell.extra.permAllowed,
                style = MaterialTheme.typography.labelLarge,
                color = glass.success,
                modifier = Modifier.testTag("$testTag-granted"),
            )
        } else {
            Text(
                shell.extra.permAllow,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onAllow)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag(testTag),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ColumnScope.YouStep(
    name: String,
    onName: (String) -> Unit,
    avatar: String,
    onChooseAvatar: () -> Unit,
) {
    val extra = LocalShellStrings.current.extra
    StepTitle(extra.obYouTitle, extra.obYouBody)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AgentAvatar(
            agent = AgentProfile(id = "you", name = name.ifBlank { "?" }, avatarPath = avatar),
            size = 88.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onChooseAvatar)
                .testTag("HermesOnboardingAvatar"),
        )
        QuietAction(extra.obChangeAvatar, "HermesOnboardingChangeAvatar", onClick = onChooseAvatar)
    }
    FieldLabel(extra.obYourName)
    ShellGroupCard {
        ShellEntryField(
            value = name,
            onValueChange = onName,
            placeholder = extra.obYourName,
            testTag = "HermesOnboardingUserName",
            primary = true,
        )
    }
}

