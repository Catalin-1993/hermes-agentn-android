package com.mobilefork.hermesagent.ui.chat

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import com.mobilefork.hermesagent.ui.theme.HermesGlassTokens
import com.mobilefork.hermesagent.ui.theme.backdropColorAt
import com.mobilefork.hermesagent.ui.chat.rich.RichBlockCard
import com.mobilefork.hermesagent.ui.chat.rich.RichSegment
import com.mobilefork.hermesagent.ui.chat.rich.parseRichSegments
import com.mobilefork.hermesagent.ui.chat.rich.textWithoutRichBlocks
import kotlin.math.pow
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.semantics.Role
import kotlin.math.ceil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import com.mobilefork.hermesagent.ui.shell.shellAboveBarAndKeyboard
import com.mobilefork.hermesagent.ui.theme.hermesGlassSurface
import com.mobilefork.hermesagent.ui.theme.hermesPressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import com.mobilefork.hermesagent.ui.i18n.LocalShellStrings
import com.mobilefork.hermesagent.data.AgentProfile
import com.mobilefork.hermesagent.data.AgentStore
import androidx.compose.foundation.horizontalScroll
import com.mobilefork.hermesagent.ui.agents.AgentAvatar
import com.mobilefork.hermesagent.ui.agents.GroupAvatar
import com.mobilefork.hermesagent.ui.shell.ShellGroupCard
import com.mobilefork.hermesagent.ui.shell.ShellListRow
import com.mobilefork.hermesagent.ui.shell.ShellPageMargin
import com.mobilefork.hermesagent.ui.shell.ShellRoundButton
import com.mobilefork.hermesagent.ui.shell.ShellRowDivider
import com.mobilefork.hermesagent.ui.shell.ShellRowIcon
import com.mobilefork.hermesagent.ui.theme.LocalHermesGlassTokens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import com.mobilefork.hermesagent.ui.theme.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.mobilefork.hermesagent.ui.theme.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.mobilefork.hermesagent.ui.theme.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.api.assistantDisplayText
import com.mobilefork.hermesagent.ui.auth.AuthViewModel
import com.mobilefork.hermesagent.ui.i18n.LocalHermesStrings
import com.mobilefork.hermesagent.ui.i18n.playEditionSummary
import com.mobilefork.hermesagent.ui.privacy.AiContentReportDialog
import com.mobilefork.hermesagent.ui.shell.AppSection
import com.mobilefork.hermesagent.ui.shell.ShellActionItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

internal const val CHAT_BOTTOM_ANCHOR_KEY = "HermesChatBottomAnchor"
private const val CHAT_FOLLOW_RATE = 0.22f
private const val CHAT_FOLLOW_SETTLE_FRAMES = 45
private const val CHAT_REVEAL_RATE = 0.09f

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    keywordHighlightingEnabled: Boolean,
    aboveTabBar: Boolean = false,
    authViewModel: AuthViewModel,
    onNavigateToSection: (AppSection) -> Unit,
    onContextActionsChanged: (List<ShellActionItem>) -> Unit = {},
    onOpenContextActions: (() -> Unit)? = null,
    onOpenAgentProfile: ((AgentProfile) -> Unit)? = null,
    onOpenGroupDetails: ((String) -> Unit)? = null,
    onApplyProvider: (String) -> Boolean,
    onApplyModel: (String) -> Boolean,
    onOpenSettingsPage: (com.mobilefork.hermesagent.ui.settings.SettingsPage) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val remoteConsentTarget by viewModel.remoteConsentTarget.collectAsState()
    remoteConsentTarget?.let { target ->
        com.mobilefork.hermesagent.ui.privacy.RemoteProcessingConsentDialog(
            target, viewModel::acceptRemoteProcessingConsent, viewModel::declineRemoteProcessingConsent,
        )
    }
    var generationElapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(uiState.isSending) {
        if (!uiState.isSending) {
            generationElapsedSeconds = 0L
            return@LaunchedEffect
        }
        val startedAt = SystemClock.elapsedRealtime()
        while (true) {
            generationElapsedSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1_000L)
                .coerceAtLeast(0L)
            delay(1_000L)
        }
    }
    val strings = LocalHermesStrings.current
    val shell = LocalShellStrings.current
    val agentsRevision by AgentStore.revision.collectAsState()
    val participants = remember(uiState.activeConversationId, agentsRevision, uiState.conversationSummaries) {
        viewModel.participantsFor(uiState.activeConversationId)
    }
    val activeAgent = participants.agent
    val activeSpeaker = participants.agentsById[uiState.activeSpeakerId]
    val visibleMessages = remember(uiState.messages, uiState.showIntermediateSteps, participants.isGroup) {
        when {
            // A member's turn appears with its first words; until then the indicator shows who writes.
            participants.isGroup -> uiState.messages.filter { it.role == "user" || it.content.isNotBlank() }
            uiState.showIntermediateSteps -> uiState.messages
            else -> uiState.messages.filter { it.role == "user" || it.eventType == AgentEventType.FinalAnswer }
        }
    }
    val workSteps = remember(uiState.messages, participants.isGroup) {
        if (participants.isGroup) emptyList() else workStepsOfLastTurn(uiState.messages)
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard = remember(focusManager, keyboardController) {
        {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }
    // Follows a reply only until the reader scrolls back through it themselves.
    var followReply by rememberSaveable { mutableStateOf(true) }
    // User drag/fling on the transcript must dismiss the IME so scrolling near the composer
    // never re-opens or resizes the keyboard (imeNestedScroll was the main cause).
    val dismissKeyboardOnScroll = remember(dismissKeyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Any vertical nested scroll (user drag near composer/list) hides IME.
                // source filter avoided for BOM compatibility (Drag/Fling vs UserInput).
                if (available.y != 0f) {
                    dismissKeyboard()
                }
                if (available.y > 0f) {
                    followReply = false
                }
                return Offset.Zero
            }
        }
    }
    val listState = rememberLazyListState()
    val isNearBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total <= 0) {
                true
            } else {
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf true
                lastVisible >= (total - 2).coerceAtLeast(0)
            }
        }
    }
    val scrollScope = rememberCoroutineScope()
    val latestMessageFingerprint = uiState.messages.lastOrNull()?.let { message ->
        "${message.id}:${message.role}:${message.content.length}:${uiState.messages.size}"
    }.orEmpty()
    val showScrollToBottom by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible < totalItems - 2
        }
    }
    var ttsController by remember(context) { mutableStateOf<HermesTtsController?>(null) }
    var composerActionMenuOpen by rememberSaveable { mutableStateOf(false) }
    var voiceDisclosureOpen by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        onDispose {
            ttsController?.shutdown()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.setListening(false)
        if (result.resultCode != Activity.RESULT_OK) {
            viewModel.setStatus(strings.voiceInputCanceled())
            return@rememberLauncherForActivityResult
        }
        val transcript = SpeechInputController.extractBestResult(result.data)
        if (transcript.isNullOrBlank()) {
            viewModel.setStatus(strings.noSpeechCaptured())
        } else {
            viewModel.applyVoiceInput(transcript)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setListening(true)
            runCatching {
                speechLauncher.launch(SpeechInputController.buildIntent())
            }.getOrElse {
                viewModel.setListening(false)
                viewModel.setStatus(strings.voiceRecognitionUnavailable())
            }
        } else {
            viewModel.setListening(false)
            viewModel.setStatus(strings.microphonePermissionRequired())
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.attachImage(uri.toString())
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) {
            viewModel.setStatus(strings.cameraCaptureCanceled())
        } else {
            runCatching {
                persistCameraPreview(context, bitmap)
            }.onSuccess { uri ->
                viewModel.attachImage(uri.toString())
            }.onFailure { error ->
                viewModel.setStatus(strings.cameraAttachFailed(error.message ?: "unknown error"))
            }
        }
    }

    fun speak(text: String): Boolean {
        val controller = ttsController ?: HermesTtsController(context).also { ttsController = it }
        val worked = controller.speak(text)
        if (!worked) {
            viewModel.setStatus(strings.speechPlaybackNotReady())
        }
        return worked
    }

    fun launchConsentedVoiceInput() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.setListening(true)
            try {
                speechLauncher.launch(SpeechInputController.buildIntent())
            } catch (_: ActivityNotFoundException) {
                viewModel.setListening(false)
                viewModel.setStatus(strings.voiceRecognitionUnavailable())
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun startVoiceInput() { voiceDisclosureOpen = true }
    if (voiceDisclosureOpen) {
        com.mobilefork.hermesagent.ui.privacy.VoiceInputDisclosureDialog(
            onAccept = { voiceDisclosureOpen = false; launchConsentedVoiceInput() },
            onDecline = { voiceDisclosureOpen = false },
        )
    }

    fun applyProvider(providerId: String): Boolean {
        return onApplyProvider(providerId)
    }

    fun applyModel(modelName: String): Boolean {
        return onApplyModel(modelName)
    }

    fun startAuthMethod(methodId: String): Boolean {
        if (com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
            onNavigateToSection(AppSection.Settings)
            return false
        }
        val supported = setOf("openrouter", "openai", "codex", "chatgpt", "claude", "gemini", "qwen", "qwen-coding-plan", "qwen-oauth", "zai", "google", "email", "phone")
        if (methodId !in supported) return false
        return authViewModel.startAuth(methodId)
    }

    val shellActions = remember(shell, uiState.messages, uiState.showIntermediateSteps, participants.isGroup) {
        buildList {
            if (participants.isGroup) {
                // A group keeps one conversation, and its members use no tools: no steps to show.
                if (onOpenGroupDetails != null) {
                    add(
                        ShellActionItem(
                            label = shell.editGroup,
                            iconRes = R.drawable.ic_app_people,
                            onClick = { onOpenGroupDetails(uiState.activeConversationId) },
                        ),
                    )
                }
            } else {
                add(
                    ShellActionItem(
                        label = shell.newChat,
                        iconRes = R.drawable.ic_ui_plus,
                        onClick = { viewModel.startNewConversation() },
                    ),
                )
                add(
                    ShellActionItem(
                        label = if (uiState.showIntermediateSteps) shell.hideSteps else shell.showSteps,
                        iconRes = R.drawable.ic_tab_history,
                        onClick = viewModel::toggleIntermediateSteps,
                    ),
                )
            }
            addAll(listOf(
            ShellActionItem(
                label = shell.speakLastReply,
                iconRes = R.drawable.ic_action_speaker,
                onClick = { speak(viewModel.latestAssistantReply()) },
            ),
            ShellActionItem(
                label = shell.clearConversation,
                iconRes = R.drawable.ic_action_close,
                onClick = viewModel::clearCurrentConversation,
            ),
            ))
        }
    }

    SideEffect {
        onContextActionsChanged(shellActions)
    }

    // A new question rises to the top of the transcript and its answer grows underneath it.
    // The empty tail below the answer is what makes that position reachable; it shrinks by
    // exactly what the answer gains, so the question never moves until the answer is taller
    // than the screen. From there the transcript follows the newest line.
    var anchorMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var anchorTailPx by rememberSaveable { mutableIntStateOf(0) }
    var seenUserMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    var seenConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    val lastUserMessageId = visibleMessages.lastOrNull { it.role == "user" }?.id
    LaunchedEffect(uiState.activeConversationId, lastUserMessageId) {
        val sameConversation = seenConversationId == uiState.activeConversationId
        val isNewQuestion = lastUserMessageId != null &&
            lastUserMessageId != seenUserMessageId &&
            (sameConversation || uiState.isSending)
        seenConversationId = uiState.activeConversationId
        seenUserMessageId = lastUserMessageId
        if (isNewQuestion) {
            followReply = true
            anchorMessageId = lastUserMessageId
            anchorTailPx = snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
            withFrameNanos { }
            val index = visibleMessages.indexOfFirst { it.id == lastUserMessageId }
            if (index >= 0) listState.animateScrollToItem(index)
        } else if (!sameConversation) {
            anchorMessageId = null
            anchorTailPx = 0
            if (visibleMessages.isNotEmpty()) listState.scrollToItem(visibleMessages.size)
        }
    }
    LaunchedEffect(anchorMessageId) {
        val anchor = anchorMessageId ?: return@LaunchedEffect
        snapshotFlow { chatAnchorTailPx(listState.layoutInfo, anchor) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { anchorTailPx = it }
    }
    val followStep = rememberUpdatedState(followReply)
    LaunchedEffect(uiState.isSending, anchorMessageId) {
        // Keeps running a moment past the end: the text reveal finishes after the last token.
        var settledFrames = 0
        while (uiState.isSending || settledFrames < CHAT_FOLLOW_SETTLE_FRAMES) {
            withFrameNanos { }
            val overflow = if (followStep.value && anchorTailPx == 0) chatReplyOverflowPx(listState.layoutInfo) else 0f
            if (overflow > 0.5f) {
                settledFrames = 0
                listState.scrollBy(overflow * CHAT_FOLLOW_RATE)
            } else {
                settledFrames++
            }
        }
    }
    LaunchedEffect(latestMessageFingerprint) {
        // Replies with no question above them, such as command feedback, stay in view.
        if (anchorMessageId == null && visibleMessages.isNotEmpty() && isNearBottom) {
            listState.animateScrollToItem(visibleMessages.size)
        }
    }

    LaunchedEffect(uiState.isSending) {
        if (uiState.isSending) {
            dismissKeyboard()
        }
    }

    fun copyMessage(message: ChatUiMessage) {
        val text = messageClipboardText(message, strings.attachmentFallback())
        if (text.isNotBlank()) {
            copyTextToClipboard(context, strings.messageClipboardLabel(), text)
            viewModel.setStatus("Message copied")
        }
    }

    fun handleSend() {
        val input = uiState.input.trim()
        if (input.isEmpty() && uiState.attachments.isEmpty()) return
        dismissKeyboard()
        if (input.isEmpty()) {
            viewModel.sendMessage()
            return
        }
        val commandResult = ChatCommandRouter.execute(
            rawInput = input,
            host = ChatCommandHost(
                openHistory = { onNavigateToSection(AppSection.History) },
                newConversation = viewModel::startNewConversation,
                clearConversation = viewModel::clearCurrentConversation,
                navigateToSection = onNavigateToSection,
                applyProvider = ::applyProvider,
                applyModel = ::applyModel,
                startAuthMethod = ::startAuthMethod,
                speakLastReply = { speak(viewModel.latestAssistantReply()) },
            ),
            strings = strings,
        )
        if (commandResult.handled) {
            viewModel.consumeCommandResult(input, commandResult.feedback)
        } else {
            viewModel.sendMessage()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        BoxWithConstraints(
            // Do not use imeNestedScroll here — it makes dragging near the composer
            // open/resize the keyboard while scrolling the chat transcript.
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val tinyVerticalViewport = maxHeight < 360.dp
            val tinyHorizontalViewport = maxWidth < 260.dp
            val tinyRuntimeViewport = tinyVerticalViewport || tinyHorizontalViewport
            val imeVisible = WindowInsets.isImeVisible
            val density = LocalDensity.current
            // The transcript runs the whole height of the page. The header and the composer float
            // over it, and it fades out under them, so these three measurements place both.
            var pageTopInRoot by remember { mutableFloatStateOf(0f) }
            var windowHeightPx by remember { mutableFloatStateOf(0f) }
            var headerBottomInRoot by remember { mutableFloatStateOf(0f) }
            var composerTopInRoot by remember { mutableFloatStateOf(0f) }
            var composerBottomInRoot by remember { mutableFloatStateOf(0f) }
            val pageHeightPx = constraints.maxHeight.toFloat()
            val topContentPadding = with(density) { (headerBottomInRoot - pageTopInRoot).coerceAtLeast(0f).toDp() } +
                ChatWorkLineHeight
            val bottomContentPadding = if (composerTopInRoot > 0f) {
                with(density) { (pageHeightPx - (composerTopInRoot - pageTopInRoot)).coerceAtLeast(0f).toDp() } + 8.dp
            } else {
                CHAT_COMPOSER_FALLBACK_CLEARANCE
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 960.dp)
                    .padding(horizontal = if (tinyRuntimeViewport) 4.dp else ShellPageMargin)
                    .onGloballyPositioned { coordinates ->
                        pageTopInRoot = coordinates.positionInRoot().y
                        windowHeightPx = coordinates.findRootCoordinates().size.height.toFloat()
                    },
            ) {
                if (uiState.messages.isEmpty()) {
                    EmptyChatHint(
                        body = shell.groupEmptyBody.takeIf { participants.isGroup },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topContentPadding, bottom = bottomContentPadding)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { dismissKeyboard() })
                            },
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Nested scroll only — avoid parent detectTapGestures here so
                            // SelectionContainer long-press / message action hits stay reliable.
                            .nestedScroll(dismissKeyboardOnScroll),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .chatEdgeFade(
                                    tokens = LocalHermesGlassTokens.current,
                                    pageTopInWindow = { pageTopInRoot },
                                    windowHeight = { windowHeightPx },
                                    headerBottom = { headerBottomInRoot - pageTopInRoot },
                                    composerTop = { composerTopInRoot - pageTopInRoot },
                                    composerBottom = { composerBottomInRoot - pageTopInRoot },
                                )
                                .testTag("HermesChatMessageList"),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
                        ) {
                            itemsIndexed(visibleMessages, key = { _, message -> message.id }) { index, message ->
                                val previous = visibleMessages.getOrNull(index - 1)
                                Column(
                                    // New messages fade in. They never slide: the anchor owns all movement.
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(durationMillis = 220),
                                        placementSpec = null,
                                        fadeOutSpec = tween(durationMillis = 120),
                                    ),
                                ) {
                                    if (shouldShowChatTimeSeparator(previous?.createdAtEpochMs, message.createdAtEpochMs)) {
                                        ChatTimeSeparator(message.createdAtEpochMs)
                                    }
                                    if (message.role != "user" && message.eventType != AgentEventType.FinalAnswer) {
                                        AgentEventCard(message = message)
                                    } else {
                                        ChatBubble(
                                            message = message,
                                            groupSender = message.senderAgentId
                                                .takeIf { participants.isGroup && message.role != "user" }
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let { id ->
                                                    participants.agentsById[id] ?: AgentProfile(id = id, name = shell.agentRemoved)
                                                },
                                            keywordHighlightingEnabled = keywordHighlightingEnabled,
                                            onSpeak = { speak(textWithoutRichBlocks(message.content)) },
                                            onSendPrompt = viewModel::sendQuickPrompt,
                                            onCopy = { copyMessage(message) },
                                            onEdit = { viewModel.stageMessageEdit(message.id) },
                                            onResend = { viewModel.resendMessage(message.id) },
                                        )
                                    }
                                }
                            }
                            item(key = CHAT_BOTTOM_ANCHOR_KEY) {
                                Spacer(
                                    modifier = Modifier
                                        .height(with(density) { anchorTailPx.toDp() }.coerceAtLeast(1.dp))
                                        .testTag(CHAT_BOTTOM_ANCHOR_KEY),
                                )
                            }
                        }
                        if (showScrollToBottom) {
                            ShellRoundButton(
                                iconRes = R.drawable.ic_ui_send,
                                contentDescription = shell.scrollToBottom,
                                onClick = {
                                    dismissKeyboard()
                                    followReply = true
                                    scrollScope.launch {
                                        listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 4.dp, bottom = bottomContentPadding)
                                    .rotate(180f),
                                size = 38.dp,
                                testTag = "HermesChatScrollToBottom",
                            )
                        }
                    }
                }
                Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                headerBottomInRoot = coordinates.positionInRoot().y + coordinates.size.height
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                ChatTopBar(
                    agent = activeAgent,
                    participants = participants,
                    compact = tinyVerticalViewport,
                    onOpenProfile = if (participants.isGroup) {
                        onOpenGroupDetails?.let { open -> { open(uiState.activeConversationId) } }
                    } else {
                        onOpenAgentProfile?.let { open -> { open(activeAgent) } }
                    },
                    onOpenActions = if (onOpenContextActions != null) {
                        {
                            dismissKeyboard()
                            onContextActionsChanged(shellActions)
                            onOpenContextActions()
                        }
                    } else {
                        null
                    },
                )
                if (!tinyRuntimeViewport) {
                    ChatReadinessStrip(modifier = Modifier.fillMaxWidth())
                }
                if (uiState.error.isNotBlank()) {
                    StatusBanner(text = uiState.error, isError = true)
                }
                    }
                    ChatWorkIndicator(
                        isSending = uiState.isSending,
                        elapsed = formatGenerationElapsed(generationElapsedSeconds),
                        steps = workSteps,
                        speaker = activeSpeaker,
                        turnKey = lastUserMessageId.orEmpty(),
                    )
                }
                ChatComposer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .then(if (aboveTabBar) Modifier.shellAboveBarAndKeyboard() else Modifier.imePadding())
                        .padding(bottom = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            composerTopInRoot = coordinates.positionInRoot().y
                            composerBottomInRoot = composerTopInRoot + coordinates.size.height
                        },
                    input = uiState.input,
                    attachments = uiState.attachments,
                    statusText = if (shouldShowComposerStatus(tinyRuntimeViewport, imeVisible)) uiState.status else "",
                    isSending = uiState.isSending,
                    isListening = uiState.isListening,
                    onInputChange = viewModel::updateInput,
                    contextOpen = composerActionMenuOpen,
                    onOpenContext = { composerActionMenuOpen = true },
                    onRemoveAttachment = viewModel::removeAttachment,
                    onMic = ::startVoiceInput,
                    onSend = ::handleSend,
                    onStop = viewModel::stopCurrentTask,
                    mentionCandidates = if (participants.isGroup) participants.members else emptyList(),
                )
            }
        }
    }
    if (composerActionMenuOpen) {
        val skillsViewModel: com.mobilefork.hermesagent.ui.settings.SkillsViewModel = viewModel()
        val skillsState by skillsViewModel.uiState.collectAsState()
        val authState by authViewModel.uiState.collectAsState()
        val globalSettings = remember(composerActionMenuOpen) {
            com.mobilefork.hermesagent.data.AppSettingsStore(context).load()
        }
        LaunchedEffect(Unit) {
            if (!com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) skillsViewModel.refresh()
        }
        val hasSkills = !com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION && activeAgent.usesDeviceTools
        ChatContextSheet(
            state = ChatContextState(
                agent = activeAgent,
                isGroup = participants.isGroup,
                globalProvider = globalSettings.provider,
                globalModel = globalSettings.model,
                skillsActive = if (hasSkills) skillsState.skills.count { it.enabled } else null,
                skillsTotal = skillsState.skills.size,
                connectedCount = authState.options.count { it.signedIn },
                quickActionsEnabled = !uiState.isSending && uiState.input.isBlank() && uiState.attachments.isEmpty(),
                showCommandsTip = !com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION,
                isListening = uiState.isListening,
            ),
            actions = ChatContextActions(
                onCamera = { cameraLauncher.launch(null) },
                onPhotos = { imageLauncher.launch(arrayOf("image/*")) },
                onPickModel = { provider, model ->
                    if (activeAgent.isMain) {
                        if (onApplyProvider(provider)) onApplyModel(model)
                    } else {
                        viewModel.setAgentModel(activeAgent.id, provider, model)
                    }
                },
                onUseMainModel = if (activeAgent.isMain) null else {
                    { viewModel.setAgentModel(activeAgent.id, "", "") }
                },
                onOpenSkills = { onOpenSettingsPage(com.mobilefork.hermesagent.ui.settings.SettingsPage.Tools) },
                onOpenConnectors = { onNavigateToSection(AppSection.Accounts) },
                onSignalQuickAction = { action -> viewModel.sendQuickPrompt(action.prompt) },
                onDismiss = { composerActionMenuOpen = false },
            ),
        )
    }
}

@Composable
private fun ChatTopBar(
    agent: AgentProfile,
    participants: ChatConversationParticipants,
    compact: Boolean,
    onOpenProfile: (() -> Unit)?,
    onOpenActions: (() -> Unit)?,
) {
    val shell = LocalShellStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("HermesChatTopBar"),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (onOpenProfile != null) {
                        Modifier.clickable(onClickLabel = shell.agentProfile, onClick = onOpenProfile)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!compact) {
                if (participants.isGroup) {
                    GroupAvatar(members = participants.members, avatarPath = participants.groupAvatarPath, size = 44.dp)
                } else {
                    AgentAvatar(agent = agent, size = 44.dp)
                }
            }
            Text(
                text = if (participants.isGroup) participants.groupName else agent.name,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (participants.isGroup && !compact) {
                Text(
                    text = shell.groupMemberCount(participants.members.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalHermesGlassTokens.current.mutedText,
                )
            }
        }
        if (onOpenActions != null) {
            ShellRoundButton(
                iconRes = R.drawable.ic_ui_more_horiz,
                contentDescription = shell.more,
                onClick = onOpenActions,
                modifier = Modifier.align(Alignment.TopEnd),
                testTag = "HermesChatPageActions",
            )
        }
    }
}

@Composable
private fun StatusBanner(text: String, isError: Boolean = false) {
    val context = LocalContext.current
    val strings = LocalHermesStrings.current
    val displayText = if (isError) text else strings.chatStatusText(text)
    val endpointStatus = isEndpointStatusText(displayText)
    val indicatorColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, LocalHermesGlassTokens.current.border),
        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (endpointStatus) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(indicatorColor),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (endpointStatus) {
                    Text(
                        text = strings.endpointStatusIndicatorLabel(),
                        color = indicatorColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                SelectionContainer {
                Text(
                    text = displayText,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
                }
                if (isError) {
                    TextButton(onClick = { copyTextToClipboard(context, strings.messageClipboardLabel(), displayText) },
                        modifier = Modifier.testTag("CopyRuntimeError")) {
                        Text(strings.copyMessageLabel())
                    }
                }
                if (endpointStatus && isError) {
                    Text(
                        text = strings.endpointStatusTroubleshootingHint(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun isEndpointStatusText(text: String): Boolean {
    val lower = text.lowercase()
    return listOf("endpoint", "stream", "sse", "http", "non-stream", "[done]", "base url", "model name").any { token ->
        lower.contains(token)
    } || (lower.contains("hermes is") && lower.contains(" via "))
}

@Composable
private fun EmptyChatHint(
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    val shell = LocalShellStrings.current
    val strings = LocalHermesStrings.current
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .testTag("HermesEmptyChat"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = shell.emptyChatTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body ?: if (com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) {
                strings.playEditionSummary()
            } else {
                shell.emptyChatBody
            },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalHermesGlassTokens.current.mutedText,
            textAlign = TextAlign.Center,
        )
    }
}

private const val CHAT_TIME_SEPARATOR_GAP_MS = 15 * 60_000L

private val CHAT_COMPOSER_FALLBACK_CLEARANCE = 160.dp
private const val CHAT_FADE_STOPS = 48
private const val CHAT_FADE_TOP_BELOW_HEADER_DP = 40f
private const val CHAT_FADE_TOP_ABOVE_HEADER_DP = 18f
private const val CHAT_FADE_TOP_CURVE = 1.6f
private const val CHAT_FADE_BOTTOM_ABOVE_COMPOSER_DP = 24f
private const val CHAT_FADE_BOTTOM_BELOW_COMPOSER_DP = 76f
private const val CHAT_FADE_BOTTOM_CURVE = 2.4f

internal fun shouldShowChatTimeSeparator(previousEpochMs: Long?, currentEpochMs: Long): Boolean =
    previousEpochMs == null || currentEpochMs - previousEpochMs >= CHAT_TIME_SEPARATOR_GAP_MS

@Composable
private fun ChatTimeSeparator(epochMs: Long) {
    val shell = LocalShellStrings.current
    val label = remember(epochMs, shell.today) {
        if (DateUtils.isToday(epochMs)) {
            "${shell.today} ${DateFormat.format("HH:mm", epochMs)}"
        } else {
            DateFormat.format("d MMM HH:mm", epochMs).toString()
        }
    }
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        style = MaterialTheme.typography.labelMedium,
        color = LocalHermesGlassTokens.current.mutedText,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ChatBubble(
    message: ChatUiMessage,
    groupSender: AgentProfile? = null,
    keywordHighlightingEnabled: Boolean,
    onSpeak: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onResend: () -> Unit,
) {
    val isUser = message.role == "user"
    val segments = remember(message.content, isUser) {
        if (isUser) listOf(RichSegment.Text(message.content)) else parseRichSegments(message.content)
    }
    val lastTextIndex = segments.indexOfLast { it is RichSegment.Text }
    val glass = LocalHermesGlassTokens.current
    val shell = LocalShellStrings.current
    val contentColor = MaterialTheme.colorScheme.onSurface
    var actionsOpen by rememberSaveable(message.id) { mutableStateOf(false) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val avatarGutter = if (groupSender != null) 38.dp else 0.dp
        val bubbleMaxWidth = when {
            maxWidth >= 760.dp -> 640.dp
            isUser -> maxWidth * 0.84f - avatarGutter
            // The agent's replies reach as far right as they start on the left.
            else -> maxWidth - avatarGutter
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            if (groupSender != null) {
                AgentAvatar(
                    agent = groupSender,
                    size = 30.dp,
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (groupSender != null) {
                Text(
                    text = groupSender.name,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.mutedText,
                )
            }
            Box {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                segments.forEachIndexed { index, segment ->
                    when (segment) {
                        is RichSegment.Block -> RichBlockCard(
                            block = segment,
                            onSendPrompt = onSendPrompt,
                            onOpenActions = { actionsOpen = true },
                            modifier = Modifier.widthIn(max = bubbleMaxWidth),
                        )
                        is RichSegment.Text -> Surface(
                            modifier = Modifier
                                .widthIn(max = bubbleMaxWidth)
                                .then(if (isUser) Modifier else Modifier.animateContentSize(tween(durationMillis = 140)))
                                .clip(RoundedCornerShape(24.dp))
                                // A tap opens the actions; a long press stays text selection.
                                .clickable(onClickLabel = shell.messageActions) { actionsOpen = true }
                                .testTag("HermesMessageActionsButton"),
                            color = if (isUser) glass.bubbleUser else glass.bubbleAgent,
                            contentColor = contentColor,
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HighlightedMessageText(
                                    text = segment.text.ifBlank { "…" },
                                    assistantText = message.role == "assistant",
                                    color = contentColor,
                                    keywordHighlightingEnabled = keywordHighlightingEnabled,
                                )
                                if (index == lastTextIndex) {
                                    AttachmentPreviewColumn(attachments = message.attachments, contentColor = contentColor)
                                    if (!isUser && hasToolActivity(message.content)) {
                                        CompactActivityRow(content = message.content, contentColor = contentColor)
                                    }
                                }
                            }
                        }
                    }
                }
                }
                ChatMessageActionMenu(
                    message = message,
                    expanded = actionsOpen,
                    onDismiss = { actionsOpen = false },
                    onCopy = onCopy,
                    onEdit = if (isUser) onEdit else null,
                    onResend = if (isUser) onResend else null,
                    onSpeak = if (!isUser && message.content.isNotBlank()) onSpeak else null,
                )
            }
            }
        }
    }
}

@Composable
private fun ChatMessageActionMenu(
    message: ChatUiMessage,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onResend: (() -> Unit)? = null,
    onSpeak: (() -> Unit)? = null,
) {
    val shell = LocalShellStrings.current
    var reportOpen by remember(message.id) { mutableStateOf(false) }
    if (reportOpen) AiContentReportDialog(message.content) { reportOpen = false }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(shell.copyMessage) },
            onClick = {
                onDismiss()
                onCopy()
            },
        )
        if (onEdit != null) {
            DropdownMenuItem(
                text = { Text(shell.editMessage) },
                onClick = {
                    onDismiss()
                    onEdit()
                },
            )
        }
        if (onResend != null) {
            DropdownMenuItem(
                text = { Text(shell.resendMessage) },
                onClick = {
                    onDismiss()
                    onResend()
                },
            )
        }
        if (onSpeak != null) {
            DropdownMenuItem(
                text = { Text(shell.speakReply) },
                onClick = {
                    onDismiss()
                    onSpeak()
                },
            )
        }
        if (message.role != "user" && message.content.isNotBlank()) {
            DropdownMenuItem(
                text = { Text(shell.reportContent) },
                modifier = Modifier.testTag("AiReportAction"),
                onClick = { onDismiss(); reportOpen = true },
            )
        }
    }
}

@Composable
private fun AgentEventCard(message: ChatUiMessage) {
    val type = message.eventType ?: return
    val strings = LocalHermesStrings.current
    var expanded by rememberSaveable(message.id) { mutableStateOf(false) }
    val title = message.content.substringBefore('\n').ifBlank { type.name }
    val details = message.content.substringAfter('\n', "").trim()
    val typeLabel = strings.eventTypeLabel(type.persistedRole)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (message.timelineDepth * 12).dp)
            .testTag("HermesAgentEvent_${type.persistedRole}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .testTag("HermesAgentEventMessage_${message.id}")
                .clickable { expanded = !expanded }
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$typeLabel · $title",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(if (expanded) "−" else "+", style = MaterialTheme.typography.labelMedium)
            }
            if (expanded && details.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuietMetaText(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color.copy(alpha = 0.64f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HighlightedMessageText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    keywordHighlightingEnabled: Boolean,
    assistantText: Boolean = false,
) {
    val strings = LocalHermesStrings.current
    val displayText = remember(text, strings.language, assistantText) {
        sanitizeChatDisplayText(if (assistantText) assistantDisplayText(text) else text, strings)
    }
    val revealedText = rememberRevealedText(displayText)
    // While text is arriving, plain text keeps each frame cheap; highlights land when it settles.
    if (!keywordHighlightingEnabled || text.isBlank() || revealedText.length < displayText.length) {
        SelectionContainer {
            Text(text = revealedText, color = color, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val pattern = remember {
        Regex(
            pattern = """(?i)(/help|/history|/provider|/signin|camera|file attachment|image upload|voice input|native app commands?|skills?|tool calls?|agent actions?)""",
        )
    }
    val highlighted = buildAnnotatedString {
        var cursor = 0
        pattern.findAll(displayText).forEach { match ->
            if (match.range.first > cursor) {
                append(displayText.substring(cursor, match.range.first))
            }
            val start = length
            append(match.value)
            addStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    fontWeight = FontWeight.SemiBold,
                ),
                start,
                length,
            )
            cursor = match.range.last + 1
        }
        if (cursor < displayText.length) {
            append(displayText.substring(cursor))
        }
    }
    SelectionContainer {
        Text(text = highlighted, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

private val TRAILING_MENTION = Regex("""(?:^|\s)@([\p{L}\p{N}_ -]*)$""")

/** What follows a just-typed @ at the end of the text, or null when the person isn't typing one. */
internal fun trailingMentionQuery(input: String): String? =
    TRAILING_MENTION.find(input)?.groupValues?.get(1)

internal fun withCompletedMention(input: String, name: String): String {
    val match = TRAILING_MENTION.find(input) ?: return input
    val at = input.lastIndexOf('@', input.lastIndex)
    return if (at < match.range.first) input else input.substring(0, at) + "@" + name + " "
}

/**
 * Tokens arrive in uneven bursts; the screen shows them as a steady flow. Text already there
 * when this first composes (history, a scrolled-back message) appears whole. Only growth is
 * revealed, a little every frame, faster the further behind the screen has fallen.
 */
@Composable
private fun rememberRevealedText(target: String): String {
    var shownLength by remember { mutableIntStateOf(target.length) }
    LaunchedEffect(target) {
        if (shownLength > target.length) shownLength = target.length
        while (shownLength < target.length) {
            withFrameNanos { }
            shownLength = chatNextRevealLength(shownLength, target.length)
        }
    }
    return if (shownLength >= target.length) target else target.substring(0, shownLength)
}

internal fun chatNextRevealLength(shown: Int, target: Int): Int {
    val backlog = target - shown
    if (backlog <= 0) return target
    return (shown + maxOf(1, ceil(backlog * CHAT_REVEAL_RATE).toInt())).coerceAtMost(target)
}

/** Tail needed for the anchor to reach the top of the clear area, or null while the anchor is off screen. */
internal fun chatAnchorTailPx(info: LazyListLayoutInfo, anchorKey: String): Int? {
    val anchor = info.visibleItemsInfo.firstOrNull { it.key == anchorKey } ?: return null
    val lastContent = info.visibleItemsInfo.lastOrNull { it.key != CHAT_BOTTOM_ANCHOR_KEY } ?: return null
    val contentHeight = lastContent.offset + lastContent.size - anchor.offset
    val clearHeight = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
    return (clearHeight - contentHeight).coerceAtLeast(0)
}

/** How far the newest line sits below the visible part of the transcript. */
internal fun chatReplyOverflowPx(info: LazyListLayoutInfo): Float {
    val lastContent = info.visibleItemsInfo.lastOrNull { it.key != CHAT_BOTTOM_ANCHOR_KEY } ?: return 0f
    if (lastContent.index < info.totalItemsCount - 2) return info.viewportSize.height / 4f
    return (lastContent.offset + lastContent.size - (info.viewportEndOffset - info.afterContentPadding)).toFloat()
}

private fun smoothStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/**
 * How visible the transcript is at height [y]. Fully clear in the reading area; from just below the
 * header up, and from just above the composer down, it thins out along one smooth curve and is gone
 * before the screen ends. Text stays faintly readable between the two bars at the bottom.
 */
internal fun chatEdgeAlpha(
    y: Float,
    headerBottom: Float,
    composerTop: Float,
    composerBottom: Float,
    density: Float,
): Float {
    val topClear = headerBottom + CHAT_FADE_TOP_BELOW_HEADER_DP * density
    val topGone = (headerBottom - CHAT_FADE_TOP_ABOVE_HEADER_DP * density).coerceAtLeast(0f)
    val top = smoothStep((y - topGone) / (topClear - topGone).coerceAtLeast(1f)).pow(CHAT_FADE_TOP_CURVE)
    val bottomClear = composerTop - CHAT_FADE_BOTTOM_ABOVE_COMPOSER_DP * density
    val bottomGone = composerBottom + CHAT_FADE_BOTTOM_BELOW_COMPOSER_DP * density
    val bottom = smoothStep((bottomGone - y) / (bottomGone - bottomClear).coerceAtLeast(1f)).pow(CHAT_FADE_BOTTOM_CURVE)
    return minOf(top, bottom)
}

/**
 * The veil over the transcript: at each height, the page's own canvas colour, as opaque as the text
 * should be faint there. It paints the theme's colours over the text instead of cutting the text out
 * of a separate layer, which showed black wherever that layer stayed opaque.
 */
internal fun chatEdgeVeilStops(
    height: Float,
    headerBottom: Float,
    composerTop: Float,
    composerBottom: Float,
    density: Float,
    pageTopInWindow: Float,
    windowHeight: Float,
    tokens: HermesGlassTokens,
): Array<Pair<Float, Color>> = Array(CHAT_FADE_STOPS + 1) { index ->
    val at = index / CHAT_FADE_STOPS.toFloat()
    val y = at * height
    val canvas = tokens.backdropColorAt((pageTopInWindow + y) / windowHeight.coerceAtLeast(1f))
    at to canvas.copy(alpha = 1f - chatEdgeAlpha(y, headerBottom, composerTop, composerBottom, density))
}

/** Only redrawn when a bar moves, not when text scrolls under it. */
private fun Modifier.chatEdgeFade(
    tokens: HermesGlassTokens,
    pageTopInWindow: () -> Float,
    windowHeight: () -> Float,
    headerBottom: () -> Float,
    composerTop: () -> Float,
    composerBottom: () -> Float,
): Modifier = drawWithCache {
    val height = size.height.coerceAtLeast(1f)
    val veil = Brush.verticalGradient(
        colorStops = chatEdgeVeilStops(
            height = height,
            headerBottom = headerBottom(),
            composerTop = composerTop().takeIf { it > 0f } ?: height,
            composerBottom = composerBottom().takeIf { it > 0f } ?: height,
            density = density,
            pageTopInWindow = pageTopInWindow(),
            windowHeight = windowHeight().takeIf { it > 0f } ?: height,
            tokens = tokens,
        ),
        startY = 0f,
        endY = height,
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush = veil)
    }
}

private fun messageClipboardText(message: ChatUiMessage, attachmentFallback: String): String {
    val attachmentText = message.attachments
        .joinToString(separator = "\n") { attachment ->
            attachment.displayName.ifBlank { attachment.mimeType }.ifBlank { attachmentFallback }
        }
    return listOf(message.content, attachmentText)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n")
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) {
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

internal fun sanitizeChatDisplayText(
    text: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings? = null,
): String {
    if (text.isBlank()) return text
    val normalized = formatXmlToolCallsForDisplay(text.replace("\r\n", "\n"), strings)
    val cleanedLines = mutableListOf<String>()
    var insideCodeFence = false
    normalized.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            insideCodeFence = !insideCodeFence
            return@forEach
        }
        if (insideCodeFence) {
            cleanedLines.add(line)
            return@forEach
        }
        expandCollapsedMarkdownRows(line).forEach { expandedLine ->
            val markdownTableCells = markdownTableCells(expandedLine)
            if (markdownTableCells.isNotEmpty()) {
                if (markdownTableCells.all(::isMarkdownTableSeparatorCell)) {
                    return@forEach
                }
                cleanedLines.add(markdownTableCells.joinToString("  "))
            } else {
                cleanedLines.add(cleanMarkdownInlineMarkers(expandedLine))
            }
        }
    }
    return cleanedLines.joinToString("\n").trimEnd()
}

internal fun formatXmlToolCallsForDisplay(
    text: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings? = null,
): String {
    if ('<' !in text || '>' !in text) {
        return text
    }
    return formatNamedXmlToolCalls(
        XML_TOOL_CALL_BLOCK_REGEX.replace(text) { match ->
            val attributes = match.value.substringBefore(">")
            val body = match.groups[1]?.value.orEmpty().trim()
            val name = XML_TOOL_NAME_ATTRIBUTE_REGEX.find(attributes)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.ifBlank { null }
                ?: XML_TOOL_JSON_NAME_REGEX.find(body)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim()
                    ?.ifBlank { null }
                ?: "tool"
            xmlToolCallDisplayBlock(name = name, arguments = body, strings = strings)
        },
        strings = strings,
    )
}

internal fun shouldShowComposerStatus(tinyRuntimeViewport: Boolean, imeVisible: Boolean): Boolean {
    return !tinyRuntimeViewport && !imeVisible
}

internal fun formatGenerationElapsed(elapsedSeconds: Long): String {
    val safeSeconds = elapsedSeconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60L
    val seconds = safeSeconds % 60L
    return if (minutes == 0L) {
        "${seconds}s"
    } else {
        "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}

private fun formatNamedXmlToolCalls(
    text: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings?,
): String {
    return XML_NAMED_TOOL_CALL_BLOCK_REGEX.replace(text) { match ->
        val name = match.groups[1]?.value.orEmpty().trim().ifBlank { "tool" }
        val body = match.groups[2]?.value.orEmpty().trim()
        xmlToolCallDisplayBlock(name = name, arguments = body, strings = strings)
    }
}

private fun xmlToolCallDisplayBlock(
    name: String,
    arguments: String,
    strings: com.mobilefork.hermesagent.ui.i18n.HermesStrings?,
): String {
    val cleanedName = name.trim().ifBlank { "tool" }
    val cleanedArguments = arguments.trim()
    val toolCallLabel = strings?.toolCallLabel() ?: "Tool call"
    val argumentsLabel = strings?.argumentsLabel() ?: "Arguments"
    return if (cleanedArguments.isBlank()) {
        "$toolCallLabel: $cleanedName"
    } else {
        "$toolCallLabel: $cleanedName\n$argumentsLabel: $cleanedArguments"
    }
}

private val XML_TOOL_CALL_BLOCK_REGEX = Regex(
    pattern = """(?is)<tool_call(?:\s+[^>]*)?>(.*?)</tool_call>""",
)

private val XML_NAMED_TOOL_CALL_BLOCK_REGEX = Regex(
    pattern = """(?is)<(terminal_tool|mcp_send_terminal_input|linux_sandbox_tool|mcp_run_in_proot|file_write_tool|android_device_diagnostics_tool|android_automation_tool|android_ui_tool|hy_memory_tool|hymemory_tool|hindsight_memory_tool|memory_tool|memory_search|memory_add|memory_delete|memory_list)(?:\s+[^>]*)?>(.*?)</\1>""",
)

private val XML_TOOL_NAME_ATTRIBUTE_REGEX = Regex(
    pattern = """(?i)\b(?:name|tool|function)=["']([^"']+)["']""",
)

private val XML_TOOL_JSON_NAME_REGEX = Regex(
    pattern = """["](?:name|tool|function)["]\s*:\s*["]([^"]+)["]""",
)

private fun expandCollapsedMarkdownRows(line: String): List<String> {
    if (line.count { it == '|' } < 2) {
        return listOf(line)
    }
    return line
        .replace(Regex("""\s*\|\|\s*"""), "\n| ")
        .lines()
}

private fun markdownTableCells(line: String): List<String> {
    val cleaned = cleanMarkdownInlineMarkers(line.trim())
    if (cleaned.count { it == '|' } < 2) {
        return emptyList()
    }
    return cleaned.trim('|')
        .split('|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun cleanMarkdownInlineMarkers(text: String): String {
    return text
        .replace(Regex("""\\\(([^\\\n]+)\\\)"""), "$1")
        .replace(Regex("""\\\[([^\\\n]+)\\\]"""), "$1")
        .replace(Regex("""\$\$([^$\n]+)\$\$"""), "$1")
        .replace(Regex("""(?<!\w)\$([^$\n]+)\$(?!\w)"""), "$1")
        .replace(Regex("""\*\*([^*\n]+)\*\*"""), "$1")
        .replace(Regex("""__([^_\n]+)__"""), "$1")
        .replace(Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)"""), "$1")
        .replace(Regex("""`([^`\n]+)`"""), "$1")
}

private fun isMarkdownTableSeparatorCell(cell: String): Boolean {
    return cell.replace(" ", "").matches(Regex(""":?-{3,}:?"""))
}

@Composable
private fun AttachmentPreviewColumn(
    attachments: List<ChatAttachment>,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    var selectedAttachment by remember { mutableStateOf<ChatAttachment?>(null) }
    if (attachments.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            AttachmentPreview(
                attachment = attachment,
                contentColor = contentColor,
                onOpen = { selectedAttachment = attachment },
            )
        }
    }
    selectedAttachment?.let { attachment ->
        FullscreenAttachmentDialog(attachment = attachment, onDismiss = { selectedAttachment = null })
    }
}

@Composable
private fun AttachmentPreview(
    attachment: ChatAttachment,
    contentColor: androidx.compose.ui.graphics.Color,
    onOpen: () -> Unit,
) {
    val image = rememberAttachmentBitmap(attachment)
    val strings = LocalHermesStrings.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .heightIn(min = 112.dp, max = 280.dp)
                .clickable(onClick = onOpen)
                .testTag("HermesChatImagePreview"),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = attachment.displayName,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(attachment.displayName, color = contentColor, style = MaterialTheme.typography.bodySmall)
                    QuietMetaText(text = attachment.mimeType.ifBlank { strings.genericAttachmentLabel() }, color = contentColor)
                }
            }
        }
    }
}

@Composable
private fun FullscreenAttachmentDialog(
    attachment: ChatAttachment,
    onDismiss: () -> Unit,
) {
    val image = rememberAttachmentBitmap(attachment)
    val strings = LocalHermesStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = attachment.displayName,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_action_close),
                            contentDescription = strings.removeAttachment(),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = attachment.displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(strings.attachmentPreviewUnavailable(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun rememberAttachmentBitmap(attachment: ChatAttachment): ImageBitmap? {
    val context = LocalContext.current
    return remember(attachment.uri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(attachment.uri))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

private fun persistCameraPreview(context: Context, bitmap: Bitmap): Uri {
    val directory = File(context.cacheDir, "hermes-camera").apply { mkdirs() }
    val file = File(directory, "camera-${System.currentTimeMillis()}.jpg")
    file.outputStream().use { output ->
        require(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
            "Unable to encode camera image"
        }
    }
    return Uri.fromFile(file)
}

@Composable
private fun CompactActivityRow(
    content: String,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    var expanded by rememberSaveable(content.take(64)) { mutableStateOf(false) }
    val strings = LocalHermesStrings.current
    val diagnosticCards = remember(content) { extractDiagnosticCards(content) }
    val visibleDiagnosticCards = diagnosticCardsForActivityPreview(diagnosticCards, expanded)
    val hiddenDiagnosticCardCount = hiddenDiagnosticCardCountForActivityPreview(diagnosticCards, expanded)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.activityToolContext(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(if (expanded) strings.hideLabel() else strings.detailsLabel(), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.72f))
            }
            visibleDiagnosticCards.forEach { card ->
                DiagnosticSummaryCard(
                    card = card,
                    expanded = expanded,
                    contentColor = contentColor,
                )
            }
            if (hiddenDiagnosticCardCount > 0) {
                Text(
                    text = strings.moreCards(hiddenDiagnosticCardCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.62f),
                )
            }
            if (expanded) {
                Text(
                    text = content.take(360),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun DiagnosticSummaryCard(
    card: DiagnosticCardSummary,
    expanded: Boolean,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = card.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.rowCount > 0) {
                Text(
                    text = "${card.rowCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.62f),
                )
            }
        }
        Text(
            text = card.body,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.78f),
            maxLines = if (expanded) 3 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded && card.rows.isNotEmpty()) {
            DiagnosticMiniGraph(card = card, contentColor = contentColor)
        }
    }
}

@Composable
private fun DiagnosticMiniGraph(
    card: DiagnosticCardSummary,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        card.rows.take(8).forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.84f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = row.valueLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(contentColor.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(row.fraction.coerceIn(0.05f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)),
                    )
                }
                Text(
                    text = row.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun hasToolActivity(content: String): Boolean {
    val lower = content.lowercase()
    return "tool" in lower ||
        "terminal_tool" in lower ||
        "android_system_tool" in lower ||
        "file_write_tool" in lower ||
        "\"cards\"" in lower ||
        "wifi_scan" in lower ||
        "bluetooth_scan" in lower ||
        "radio_signal_status" in lower ||
        "sensor_snapshot" in lower
}

@Composable
private fun ChatComposer(
    modifier: Modifier = Modifier,
    input: String,
    attachments: List<ChatAttachment>,
    statusText: String,
    isSending: Boolean,
    isListening: Boolean,
    onInputChange: (String) -> Unit,
    contextOpen: Boolean,
    onOpenContext: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onMic: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    mentionCandidates: List<AgentProfile> = emptyList(),
) {
    val strings = LocalHermesStrings.current
    val shell = LocalShellStrings.current
    val glass = LocalHermesGlassTokens.current
    val canSend = !isSending && (input.isNotBlank() || attachments.isNotEmpty())
    Column(
        modifier = modifier.testTag("HermesChatComposerFrame"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (statusText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .hermesGlassSurface(RoundedCornerShape(50), shadowRadius = 4.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                QuietMetaText(
                    text = strings.chatStatusText(statusText),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (attachments.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("HermesChatAttachments"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                attachments.forEach { attachment ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(attachment.displayName, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { onRemoveAttachment(attachment.uri) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_action_close),
                                    contentDescription = strings.removeAttachment(),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        val mentionQuery = remember(input) { trailingMentionQuery(input) }
        val mentionMatches = remember(mentionQuery, mentionCandidates) {
            mentionQuery?.let { query ->
                mentionCandidates.filter { it.name.startsWith(query, ignoreCase = true) }
            }.orEmpty()
        }
        if (mentionMatches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("HermesMentionSuggestions"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                mentionMatches.forEach { agent ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(glass.tabSelected)
                            .clickable { onInputChange(withCompletedMention(input, agent.name)) }
                            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("HermesMention-${agent.id}"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AgentAvatar(agent = agent, size = 26.dp)
                        Text(agent.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
        val pillShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .hermesGlassSurface(pillShape, shadowRadius = 14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .testTag("HermesChatComposerRow"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onOpenContext,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("HermesChatMoreInputActionsButton"),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ui_plus),
                        contentDescription = shell.addAttachment,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (contextOpen) 45f else 0f),
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    enabled = !isSending,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 24.dp, max = 112.dp)
                        .testTag("HermesChatInput"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSend()
                            }
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (input.isEmpty()) {
                                Text(
                                    text = shell.messageHint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = glass.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                when {
                    isSending -> ComposerCircleButton(
                        iconRes = R.drawable.ic_ui_stop,
                        contentDescription = shell.stop,
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        testTag = "HermesStopAgentButton",
                        onClick = onStop,
                    )
                    canSend -> ComposerCircleButton(
                        iconRes = R.drawable.ic_ui_send,
                        contentDescription = shell.send,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        testTag = "HermesChatSendButton",
                        onClick = onSend,
                    )
                    else -> IconButton(
                        onClick = onMic,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("HermesChatMicButton"),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ui_mic),
                            contentDescription = shell.voiceInput,
                            tint = if (isListening) MaterialTheme.colorScheme.primary else glass.mutedText,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerCircleButton(
    iconRes: Int,
    contentDescription: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .hermesPressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = containerColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun SignalIntelligenceQuickActionGrid(
    enabled: Boolean,
    compact: Boolean = false,
    onSignalQuickAction: (SignalIntelligenceQuickAction) -> Unit,
) {
    if (com.mobilefork.hermesagent.BuildConfig.HERMES_PLAY_EDITION) return
    val buttonColors = if (compact) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    } else {
        ButtonDefaults.buttonColors()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("HermesSignalQuickActions"),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        val strings = LocalHermesStrings.current
        Text(
            text = strings.signalIntelligence(),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val visibleActions = if (compact) {
            SIGNAL_INTELLIGENCE_QUICK_ACTIONS
        } else {
            SIGNAL_INTELLIGENCE_QUICK_ACTIONS.take(4)
        }
        visibleActions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowActions.forEach { action ->
                    Button(
                        onClick = { onSignalQuickAction(action) },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (compact) 34.dp else 40.dp)
                            .testTag("HermesSignalQuickAction_${action.id}"),
                        shape = MaterialTheme.shapes.small,
                        colors = buttonColors,
                        contentPadding = PaddingValues(
                            horizontal = if (compact) 6.dp else 8.dp,
                            vertical = if (compact) 4.dp else 6.dp,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = action.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                        )
                        Text(
                            text = " ${strings.signalQuickActionLabel(action.id, action.label)}",
                            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
