package com.example.ui.screens

import androidx.activity.compose.BackHandler
import com.example.ui.components.dock.dockBottomGradientFade
import com.example.utils.LocalAppStrings
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.BackgroundSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.hermesPageBackground
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCopy as DuplicateIcon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.filled.UploadFile
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.example.data.storage.LibraryStorageManager
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.shadow
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.ui.components.ChatBottomBar
import com.example.data.model.LibraryItem
import com.example.data.model.UserProfile
import com.example.data.model.NotionBlock
import com.example.data.model.NotionBlockType
import com.example.data.model.ChatMessage
import com.example.data.model.AiSkill
import com.example.data.model.AttachedFile
import com.example.data.model.SpaceItem
import com.example.data.model.ChatSession
import com.example.utils.LocalAppStrings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import java.util.UUID

enum class LibraryTab {
    NOTITE,
    DOCUMENTE,
    IMAGINI,
    VIDEO,
    TRASH
}

// Notion Gradient Cover Palettes
val notionCoverGradients = listOf(
    listOf(Color(0xFF2B5876), Color(0xFF4E4376)), // Slate Dusk
    listOf(Color(0xFF00B4DB), Color(0xFF0083B0)), // Ocean Teal
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)), // Emerald Minimal
    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // Royal Purple
    listOf(Color(0xFFF2994A), Color(0xFFF2C94C)), // Warm Amber
    listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))  // Charcoal Mono
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryItems: List<LibraryItem>,
    onAddLibraryItem: (LibraryItem) -> Unit,
    onUpdateLibraryItem: (LibraryItem) -> Unit,
    onDeleteLibraryItem: (String) -> Unit,
    onUsePromptInChat: (String) -> Unit,
    onNavigateToChat: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    userProfile: UserProfile = UserProfile(),
    // Trash parameters
    trashSessions: List<ChatSession> = emptyList(),
    onRestoreChat: (String) -> Unit = {},
    onPermanentDelete: (String) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    // Agent Chat parameters for bottom sheet
    messages: List<ChatMessage> = emptyList(),
    spaces: List<SpaceItem> = emptyList(),
    isGenerating: Boolean = false,
    inputText: String = "",
    onInputTextChange: (String) -> Unit = {},
    onSendMessage: (userPrompt: String?) -> Unit = {},
    onStopGeneration: () -> Unit = {},
    onRegenerate: (ChatMessage) -> Unit = {},
    onEditUserMessage: (messageId: String, newText: String) -> Unit = { _, _ -> },
    onSaveToLibrary: (messageId: String, category: String) -> Unit = { _, _ -> },
    onSaveToSpace: (messageId: String, spaceId: String) -> Unit = { _, _ -> },
    onBranchChat: (ChatMessage) -> Unit = {},
    speakingMessageId: String? = null,
    onSpeakMessage: (ChatMessage) -> Unit = {},
    activeSkill: AiSkill? = null,
    activeAttachment: AttachedFile? = null,
    isVoiceRecording: Boolean = false,
    onOpenAttachmentPicker: () -> Unit = {},
    onOpenSkillPicker: () -> Unit = {},
    onToggleRecording: () -> Unit = {},
    /** Drawn as the last section of the notes tree, after Favorite and Private. */
    templatesSection: (@Composable () -> Unit)? = null,
    /** A page to open as soon as the library shows, for a shortcut that points at one. */
    initialItemId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(LibraryTab.NOTITE) }
    var defaultAddFileCategory by remember { mutableStateOf("Documente") }

    var isChatSheetOpen by remember { mutableStateOf(false) }
    var isFullScreenChat by remember { mutableStateOf(false) }
    var chatSheetHeightDp by remember { mutableStateOf(340.dp) }

    var isNotionModalOpen by remember { mutableStateOf(false) }
    var creationTargetParentId by remember { mutableStateOf<String?>(null) }
    var creationTargetSection by remember { mutableStateOf("Privat") }
    var expandedFolderIds by remember { mutableStateOf(emptySet<String>()) }

    var itemToEdit by remember { mutableStateOf<LibraryItem?>(null) }
    var itemToView by remember { mutableStateOf<LibraryItem?>(null) }

    var isFileModalOpen by remember { mutableStateOf(false) }
    var itemToDeleteId by remember { mutableStateOf<String?>(null) }
    var isAskAiDialogOpen by remember { mutableStateOf(false) }
    var activeSelectedItem by remember { mutableStateOf<LibraryItem?>(null) }
    var horizontalDragAccumulator by remember { mutableFloatStateOf(0f) }
    var openedInitialItem by remember(initialItemId) { mutableStateOf(initialItemId == null) }
    LaunchedEffect(initialItemId, libraryItems) {
        if (!openedInitialItem) {
            libraryItems.firstOrNull { it.id == initialItemId }?.let { item ->
                openedInitialItem = true
                expandedFolderIds = expandedFolderIds + item.id
                activeSelectedItem = item
                itemToEdit = item
                creationTargetParentId = item.parentId
                creationTargetSection = item.section
                isNotionModalOpen = true
            }
        }
    }

    BackHandler {
        when {
            itemToView != null -> itemToView = null
            itemToEdit != null -> itemToEdit = null
            isNotionModalOpen -> isNotionModalOpen = false
            isFileModalOpen -> isFileModalOpen = false
            itemToDeleteId != null -> itemToDeleteId = null
            isAskAiDialogOpen -> isAskAiDialogOpen = false
            isChatSheetOpen -> isChatSheetOpen = false
            isSearchActive || searchQuery.isNotEmpty() -> {
                isSearchActive = false
                searchQuery = ""
            }
            else -> onNavigateToChat()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAccumulator = 0f },
                    onDragEnd = {
                        // Swipe left-to-right (drag right): open navigation drawer
                        if (horizontalDragAccumulator > 70f) {
                            onOpenDrawer()
                        }
                        horizontalDragAccumulator = 0f
                    },
                    onDragCancel = { horizontalDragAccumulator = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragAccumulator += dragAmount
                    }
                )
            }
            .hermesPageBackground(),
        color = Color.Transparent
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("library_screen_container")
        ) {
        val density = LocalDensity.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // -----------------------------------------------------------------
            // TOP SEARCH FIELD (Expands at the top ABOVE navigation buttons)
            // -----------------------------------------------------------------
            AnimatedVisibility(
                visible = isSearchActive || searchQuery.isNotEmpty(),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Caută în fișiere, notițe sau istoric...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty() || isSearchActive) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Închide Căutarea",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // TOP NAVIGATION BAR (4 Category Tabs + Search Button on Single Row)
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Segmented Category Navigation Bar (Notițe, Documente, Imagini, Video)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. Notițe
                        val isNotiteActive = selectedTab == LibraryTab.NOTITE
                        Surface(
                            onClick = { selectedTab = LibraryTab.NOTITE },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isNotiteActive) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = strings.notes,
                                    tint = if (isNotiteActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.notes,
                                    fontSize = 13.sp,
                                    fontWeight = if (isNotiteActive) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isNotiteActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 2. Documente
                        val isDocumenteActive = selectedTab == LibraryTab.DOCUMENTE
                        Surface(
                            onClick = { selectedTab = LibraryTab.DOCUMENTE },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDocumenteActive) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = strings.documents,
                                    tint = if (isDocumenteActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.documents,
                                    fontSize = 13.sp,
                                    fontWeight = if (isDocumenteActive) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isDocumenteActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 3. Imagini
                        val isImaginiActive = selectedTab == LibraryTab.IMAGINI
                        Surface(
                            onClick = { selectedTab = LibraryTab.IMAGINI },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isImaginiActive) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = strings.images,
                                    tint = if (isImaginiActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.images,
                                    fontSize = 13.sp,
                                    fontWeight = if (isImaginiActive) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isImaginiActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 4. Video
                        val isVideoActive = selectedTab == LibraryTab.VIDEO
                        Surface(
                            onClick = { selectedTab = LibraryTab.VIDEO },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isVideoActive) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = strings.video,
                                    tint = if (isVideoActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.video,
                                    fontSize = 13.sp,
                                    fontWeight = if (isVideoActive) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isVideoActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 5. Coș de gunoi
                        val isTrashActive = selectedTab == LibraryTab.TRASH
                        Surface(
                            onClick = { selectedTab = LibraryTab.TRASH },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isTrashActive) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.trash,
                                    tint = if (isTrashActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.trash,
                                    fontSize = 13.sp,
                                    fontWeight = if (isTrashActive) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isTrashActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Search Toggle Button (Lupa de Căutare din Partea Dreaptă)
                Surface(
                    onClick = { isSearchActive = !isSearchActive },
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSearchActive || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isSearchActive || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Caută",
                            tint = if (isSearchActive || searchQuery.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Workspace Content View (Notițe, Documente, Imagini, Video) with professional bottom fade under dock
            Box(
                modifier = Modifier
                    .weight(1f)
                    .dockBottomGradientFade(fadeHeight = 185.dp)
            ) {
                when (selectedTab) {
                    LibraryTab.NOTITE -> {
                        NotionWorkspaceTreeView(
                            libraryItems = libraryItems,
                            searchQuery = searchQuery,
                            expandedFolderIds = expandedFolderIds,
                            onToggleFolder = { folderId ->
                                expandedFolderIds = if (expandedFolderIds.contains(folderId)) {
                                    expandedFolderIds - folderId
                                } else {
                                    expandedFolderIds + folderId
                                }
                            },
                            onView = { item ->
                                activeSelectedItem = item
                                itemToEdit = item
                                creationTargetParentId = item.parentId
                                creationTargetSection = item.section
                                isNotionModalOpen = true
                            },
                            onEdit = { item ->
                                activeSelectedItem = item
                                itemToEdit = item
                                creationTargetParentId = item.parentId
                                creationTargetSection = item.section
                                isNotionModalOpen = true
                            },
                            onDelete = { itemToDeleteId = it },
                            onToggleFavorite = { item ->
                                val newFav = !item.isFavorite
                                val updated = item.copy(
                                    isFavorite = newFav,
                                    section = if (newFav) "Favorite" else "Privat",
                                    lastModified = System.currentTimeMillis()
                                )
                                onUpdateLibraryItem(updated)
                                android.widget.Toast.makeText(
                                    context,
                                    if (newFav) "Adăugat la Favorite" else "Eliminat din Favorite",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            onCreateChildPage = { parentItem ->
                                activeSelectedItem = parentItem
                                creationTargetParentId = parentItem.id
                                creationTargetSection = parentItem.section
                                itemToEdit = null
                                isNotionModalOpen = true
                            },
                            onCreateSectionPage = { sectionName ->
                                creationTargetParentId = null
                                creationTargetSection = sectionName
                                itemToEdit = null
                                isNotionModalOpen = true
                            },
                            onSendToChat = { onUsePromptInChat(it) },
                            templatesSection = templatesSection,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    LibraryTab.DOCUMENTE -> {
                        val documentItems = remember(libraryItems, searchQuery) {
                            libraryItems.filter { item ->
                                (item.category == "Documente" || item.category == "PDF-uri" || item.category == "PDF" || item.category == "DOC" || item.category == "Fisiere" || item.category == "Altceva" || item.fileSize > 0) &&
                                        item.category != "Notițe" && item.category != "Imagini" && item.category != "Imagine" && item.category != "Foto" && item.category != "Video" && item.category != "Videoclipuri" &&
                                        (searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.snippet.contains(searchQuery, ignoreCase = true) || item.fullContent.contains(searchQuery, ignoreCase = true))
                            }
                        }

                        if (documentItems.isEmpty()) {
                            CategoryEmptyStateView(
                                icon = Icons.Default.Folder,
                                title = strings.noDocuments,
                                description = strings.noDocuments,
                                buttonText = strings.addDocument,
                                onAddClick = {
                                    defaultAddFileCategory = "Documente"
                                    isFileModalOpen = true
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                            ) {
                                items(documentItems) { item ->
                                    MinimalDocumentCard(
                                        item = item,
                                        onView = {
                                            activeSelectedItem = item
                                            itemToView = item
                                        },
                                        onEdit = {
                                            activeSelectedItem = item
                                            itemToEdit = item
                                        },
                                        onDelete = { itemToDeleteId = item.id }
                                    )
                                }
                            }
                        }
                    }
                    LibraryTab.IMAGINI -> {
                        val imageItems = remember(libraryItems, searchQuery) {
                            libraryItems.filter { item ->
                                val media = item.mediaUrl.orEmpty()
                                (item.category == "Imagini" || item.category == "Imagine" || item.category == "Foto" || (media.isNotBlank() && (media.endsWith(".png") || media.endsWith(".jpg") || media.endsWith(".jpeg") || media.contains("image")))) &&
                                        (searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.snippet.contains(searchQuery, ignoreCase = true) || item.fullContent.contains(searchQuery, ignoreCase = true))
                            }
                        }

                        if (imageItems.isEmpty()) {
                            CategoryEmptyStateView(
                                icon = Icons.Default.Image,
                                title = strings.noImages,
                                description = strings.noImages,
                                buttonText = strings.addImage,
                                onAddClick = {
                                    defaultAddFileCategory = "Imagini"
                                    isFileModalOpen = true
                                }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                            ) {
                                items(imageItems) { item ->
                                    ImageGalleryCard(
                                        item = item,
                                        onView = {
                                            activeSelectedItem = item
                                            itemToView = item
                                        },
                                        onEdit = {
                                            activeSelectedItem = item
                                            itemToEdit = item
                                        },
                                        onDelete = { itemToDeleteId = item.id }
                                    )
                                }
                            }
                        }
                    }
                    LibraryTab.VIDEO -> {
                        val videoItems = remember(libraryItems, searchQuery) {
                            libraryItems.filter { item ->
                                (item.category == "Video" || item.category == "Videoclipuri" || item.category == "VIDEO") &&
                                        (searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.snippet.contains(searchQuery, ignoreCase = true) || item.fullContent.contains(searchQuery, ignoreCase = true))
                            }
                        }

                        if (videoItems.isEmpty()) {
                            CategoryEmptyStateView(
                                icon = Icons.Default.Movie,
                                title = strings.noVideos,
                                description = strings.noVideos,
                                buttonText = strings.addVideo,
                                onAddClick = {
                                    defaultAddFileCategory = "Videoclipuri"
                                    isFileModalOpen = true
                                }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                            ) {
                                items(videoItems) { item ->
                                    VideoGalleryCard(
                                        item = item,
                                        onView = {
                                            activeSelectedItem = item
                                            itemToView = item
                                        },
                                        onEdit = {
                                            activeSelectedItem = item
                                            itemToEdit = item
                                        },
                                        onDelete = { itemToDeleteId = item.id }
                                    )
                                }
                            }
                        }
                    }
                    LibraryTab.TRASH -> {
                        TrashScreen(
                            trashSessions = trashSessions,
                            onRestoreChat = onRestoreChat,
                            onPermanentDelete = onPermanentDelete,
                            onEmptyTrash = onEmptyTrash,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // BOTTOM FLOATING ACTION BAR (Exact same position above the dock bar)
        // ---------------------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 102.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val vibrantGreen = MaterialTheme.colorScheme.primary

            // Chat Agent Button
            Surface(
                onClick = { isChatSheetOpen = true },
                shape = RoundedCornerShape(12.dp),
                color = vibrantGreen,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.height(44.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ask AI",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Creation Button dynamically matching selected tab
            if (selectedTab != LibraryTab.TRASH) {
                Surface(
                    onClick = {
                        when (selectedTab) {
                            LibraryTab.NOTITE -> {
                                itemToEdit = null
                                creationTargetParentId = null
                                creationTargetSection = "Privat"
                                isNotionModalOpen = true
                            }
                            LibraryTab.DOCUMENTE -> {
                                defaultAddFileCategory = "Documente"
                                isFileModalOpen = true
                            }
                            LibraryTab.IMAGINI -> {
                                defaultAddFileCategory = "Imagini"
                                isFileModalOpen = true
                            }
                            LibraryTab.VIDEO -> {
                                defaultAddFileCategory = "Videoclipuri"
                                isFileModalOpen = true
                            }
                            LibraryTab.TRASH -> {}
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = vibrantGreen,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("create_library_item_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (selectedTab) {
                                LibraryTab.NOTITE -> Icons.Default.Edit
                                LibraryTab.DOCUMENTE -> Icons.Default.Folder
                                LibraryTab.IMAGINI -> Icons.Default.Image
                                LibraryTab.VIDEO -> Icons.Default.Movie
                                LibraryTab.TRASH -> Icons.Default.Delete
                            },
                            contentDescription = "Adaugă element",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------------------
        // AGENT CHAT BOTTOM SHEET (Positioned at bottom, height adjusted smoothly via drag gesture)
        // -----------------------------------------------------------------------------
        AnimatedVisibility(
            visible = isChatSheetOpen,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
        ) {
            val currentSheetHeight = if (isFullScreenChat) maxHeight else chatSheetHeightDp.coerceAtMost(maxHeight)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentSheetHeight)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val dragAmountDp = with(density) { dragAmount.toDp() }
                            val minHeight = 140.dp
                            val maxHeightAvailable = maxHeight - 10.dp
                            chatSheetHeightDp = (chatSheetHeightDp - dragAmountDp).coerceIn(minHeight, maxHeightAvailable)
                        }
                    },
                shape = RoundedCornerShape(
                    topStart = if (isFullScreenChat) 0.dp else 24.dp,
                    topEnd = if (isFullScreenChat) 0.dp else 24.dp
                ),
                color = Color(0xFF0F0F10),
                shadowElevation = 16.dp,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2C30))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F0F10))
                ) {
                    // Header: Selected Folder / Item Indicator + Fullscreen Toggle + Close
                    // Top half solid neutral dark, bottom half slightly transparent with smooth vertical gradient
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF141416),
                                        Color(0xFF141416).copy(alpha = 0.65f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Indicator showing the selected folder / item / section / tab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E22))
                                .border(1.dp, Color(0xFF33333C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            val contextItem = activeSelectedItem ?: itemToEdit ?: itemToView
                            val indicatorIcon = when {
                                contextItem != null -> {
                                    when (contextItem.category) {
                                        "Documente", "PDF-uri", "PDF", "DOC", "Fisiere" -> Icons.Default.Folder
                                        "Imagini", "Imagine", "Foto" -> Icons.Default.Image
                                        "Video", "Videoclipuri", "VIDEO" -> Icons.Default.Movie
                                        else -> Icons.Default.Description
                                    }
                                }
                                selectedTab == LibraryTab.NOTITE -> Icons.Default.FolderOpen
                                selectedTab == LibraryTab.DOCUMENTE -> Icons.Default.Folder
                                selectedTab == LibraryTab.IMAGINI -> Icons.Default.Image
                                selectedTab == LibraryTab.VIDEO -> Icons.Default.Movie
                                selectedTab == LibraryTab.TRASH -> Icons.Default.Delete
                                else -> Icons.Default.Folder
                            }
                            val indicatorLabel = when {
                                contextItem != null && contextItem.title.isNotBlank() -> contextItem.title
                                selectedTab == LibraryTab.NOTITE -> "Notițe"
                                selectedTab == LibraryTab.DOCUMENTE -> "Documente"
                                selectedTab == LibraryTab.IMAGINI -> "Imagini"
                                selectedTab == LibraryTab.VIDEO -> "Video"
                                selectedTab == LibraryTab.TRASH -> "Coș de gunoi"
                                else -> "Librărie"
                            }

                            Icon(
                                imageVector = indicatorIcon,
                                contentDescription = null,
                                tint = Color(0xFFE2E8F0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = indicatorLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Full Screen Toggle Button
                            IconButton(
                                onClick = { isFullScreenChat = !isFullScreenChat },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullScreenChat) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullScreenChat) "Micșorează ecranul" else "Ecran complet (Full Screen)",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Close Button
                            IconButton(
                                onClick = { isChatSheetOpen = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Închide Chat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2C2C30).copy(alpha = 0.5f))

                    // Messages Stream (Uses exact same ChatGptMessageItem components)
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (messages.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cum te pot ajuta în Librărie?",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                items(messages) { message ->
                                    ChatGptMessageItem(
                                        message = message,
                                        spaces = spaces,
                                        isSpeaking = message.id.toString() == speakingMessageId,
                                        onSpeakClick = { onSpeakMessage(message) },
                                        onSaveToLibrary = { category -> onSaveToLibrary(message.id.toString(), category) },
                                        onSaveToSpace = { spaceId -> onSaveToSpace(message.id.toString(), spaceId) },
                                        onBranchChat = onBranchChat,
                                        onRegenerate = onRegenerate,
                                        onEditUserMessage = onEditUserMessage
                                    )
                                }
                            }
                        }
                    }

                    // Unified Chat Bottom Bar at the Bottom of Chat Sheet
                    ChatBottomBar(
                        inputText = inputText,
                        onInputTextChange = onInputTextChange,
                        onSendMessage = { prompt ->
                            onSendMessage(prompt.ifBlank { null })
                        },
                        placeholderText = "Întreabă Hermes...",
                        isLoading = isGenerating,
                        onStopGeneration = onStopGeneration,
                        onOpenAttachments = onOpenAttachmentPicker,
                        onOpenModelSkills = onOpenSkillPicker,
                        onOpenVoice = onToggleRecording
                    )
                }
            }
        }

        // Notion Page Editor Modal (Create / Edit)
        if (isNotionModalOpen || itemToEdit != null) {
            NotionNoteEditorModal(
                existingNote = itemToEdit,
                onDismiss = {
                    isNotionModalOpen = false
                    itemToEdit = null
                },
                onSave = { updatedOrNewItem ->
                    val parentIdToExpand = creationTargetParentId
                    val itemToSave = if (itemToEdit != null) {
                        updatedOrNewItem
                    } else {
                        updatedOrNewItem.copy(
                            parentId = creationTargetParentId,
                            section = creationTargetSection,
                            isFavorite = (creationTargetSection == "Favorite")
                        )
                    }
                    if (itemToEdit != null) {
                        onUpdateLibraryItem(itemToSave)
                    } else {
                        onAddLibraryItem(itemToSave)
                    }
                    if (parentIdToExpand != null) {
                        expandedFolderIds = expandedFolderIds + parentIdToExpand
                    }
                    isNotionModalOpen = false
                    itemToEdit = null
                    creationTargetParentId = null
                }
            )
        }

        // File Resource Modal Upload
        if (isFileModalOpen) {
            AddFileResourceModal(
                initialCategory = defaultAddFileCategory,
                onDismiss = { isFileModalOpen = false },
                onSave = { newItem ->
                    onAddLibraryItem(newItem)
                    isFileModalOpen = false
                }
            )
        }

        // View Item Details Modal (Notes, Images, PDFs)
        itemToView?.let { item ->
            ViewLibraryItemModal(
                item = item,
                onDismiss = { itemToView = null },
                onEditNotion = {
                    itemToView = null
                    itemToEdit = item
                },
                onSendToChat = { content ->
                    onUsePromptInChat(content)
                    itemToView = null
                },
                onToggleCheckbox = { blockId, isChecked ->
                    val updatedBlocks = item.blocks.map { block ->
                        if (block.id == blockId) block.copy(isChecked = isChecked) else block
                    }
                    val updatedItem = item.copy(blocks = updatedBlocks)
                    onUpdateLibraryItem(updatedItem)
                    itemToView = updatedItem
                },
                onToggleExpand = { blockId ->
                    val updatedBlocks = item.blocks.map { block ->
                        if (block.id == blockId) block.copy(isExpanded = !block.isExpanded) else block
                    }
                    val updatedItem = item.copy(blocks = updatedBlocks)
                    onUpdateLibraryItem(updatedItem)
                    itemToView = updatedItem
                }
            )
        }

        // Delete Dialog Confirmation
        itemToDeleteId?.let { itemId ->
            AlertDialog(
                onDismissRequest = { itemToDeleteId = null },
                title = { Text(strings.delete) },
                text = { Text(strings.deleteItemConfirm) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteLibraryItem(itemId)
                            itemToDeleteId = null
                        }
                    ) {
                        Text(strings.delete, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDeleteId = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        // Ask AI Dialog
        if (isAskAiDialogOpen) {
            AskAiDialog(
                onDismiss = { isAskAiDialogOpen = false },
                onSubmitPrompt = { prompt ->
                    isAskAiDialogOpen = false
                    onUsePromptInChat(prompt)
                }
            )
        }
        }
    }
}

@Composable
private fun AskAiDialog(
    onDismiss: () -> Unit,
    onSubmitPrompt: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var promptText by remember { mutableStateOf("") }
    val suggestions = listOf(
        strings.sprintPriorities,
        strings.brainstormAi,
        strings.quickDataAnalysis,
        strings.quickProjectIdeas
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.askAi, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.askAiAboutDoc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text(strings.typeMessage, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = strings.quickSuggestions,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestions) { sugg ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                promptText = sugg
                            }
                        ) {
                            Text(
                                text = sugg,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        onSubmitPrompt(promptText)
                    }
                },
                enabled = promptText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.send, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Helper composable to render Notion B&W Vector Icon or Emoji
@Composable
fun NotionPageIconDisplay(
    iconString: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    iconSize: Dp = 18.dp
) {
    if (iconString.startsWith("icon:")) {
        val iconName = iconString.removePrefix("icon:")
        val vector = when (iconName) {
            "Folder" -> Icons.Default.Folder
            "Book" -> Icons.Default.Book
            "Code" -> Icons.Default.Code
            "Terminal" -> Icons.Default.Terminal
            "Storage" -> Icons.Default.Storage
            "Lightbulb" -> Icons.Default.Lightbulb
            "AutoAwesome" -> Icons.Default.AutoAwesome
            "Work" -> Icons.Default.Work
            "CheckCircle" -> Icons.Default.CheckCircle
            "Star" -> Icons.Default.Star
            "Bookmark" -> Icons.Default.Bookmark
            "Tag" -> Icons.Default.Tag
            "Key" -> Icons.Default.Key
            "Shield" -> Icons.Default.Shield
            "Lock" -> Icons.Default.Lock
            "Rocket" -> Icons.Default.Rocket
            "AttachFile" -> Icons.Default.AttachFile
            "Link" -> Icons.Default.Link
            "Palette" -> Icons.Default.Palette
            "TableChart" -> Icons.Default.TableChart
            "FormatQuote" -> Icons.Default.FormatQuote
            else -> Icons.Default.Description
        }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = Color(0xFFC4C4C4),
                modifier = Modifier.size(iconSize)
            )
        }
    } else if (iconString.isBlank() || iconString == "📄" || iconString == "📝") {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFFC4C4C4),
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconString,
                fontSize = (iconSize.value).sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// NOTION-STYLE PAGE CREATION & EDITING MODAL
// -----------------------------------------------------------------------------
@Composable
fun NotionNoteEditorModal(
    existingNote: LibraryItem?,
    onDismiss: () -> Unit,
    onSave: (LibraryItem) -> Unit
) {
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var hasCover by remember {
        mutableStateOf(
            existingNote != null && (existingNote.coverGradientIndex > 0 || !existingNote.mediaUrl.isNullOrBlank() || !existingNote.customCoverUri.isNullOrBlank())
        )
    }
    var coverIndex by remember {
        mutableIntStateOf(if (existingNote != null && existingNote.coverGradientIndex > 0) existingNote.coverGradientIndex else 0)
    }
    var customCoverUri by remember {
        mutableStateOf<String?>(existingNote?.mediaUrl ?: existingNote?.customCoverUri)
    }
    var showCoverPickerGallery by remember { mutableStateOf(false) }
    var showCoverControls by remember { mutableStateOf(false) }
    var coverControlsTrigger by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showCoverControls, coverControlsTrigger) {
        if (showCoverControls) {
            delay(2500)
            showCoverControls = false
        }
    }

    var hasIcon by remember {
        mutableStateOf(
            existingNote != null && !existingNote.iconEmoji.isNullOrBlank() && existingNote.iconEmoji != "icon:Description"
        )
    }
    var selectedEmoji by remember {
        mutableStateOf(existingNote?.iconEmoji?.ifBlank { "icon:Description" } ?: "icon:Description")
    }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedIconTab by remember { mutableIntStateOf(0) } // 0 = Notion B&W Icons, 1 = Emoji

    var hasTags by remember { mutableStateOf(!existingNote?.tags.isNullOrEmpty()) }
    var tagsInput by remember { mutableStateOf(existingNote?.tags?.joinToString(", ") ?: "") }
    var isFavoriteState by remember {
        mutableStateOf(existingNote?.isFavorite == true || existingNote?.section.equals("Favorite", ignoreCase = true))
    }
    
    var activeBlockForPlusMenu by remember { mutableStateOf<Int?>(null) }
    var activeBlockForRowMenu by remember { mutableStateOf<Int?>(null) }
    var focusedBlockIndex by remember { mutableIntStateOf(0) }
    var editingSubPageBlockIndex by remember { mutableStateOf<Int?>(null) }
    var draggingBlockIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Custom Cover Gallery Picker launcher
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customCoverUri = it.toString()
            hasCover = true
        }
    }

    val blocksState = remember {
        mutableStateListOf<NotionBlock>().apply {
            if (existingNote != null && existingNote.blocks.isNotEmpty()) {
                addAll(existingNote.blocks)
            } else {
                // Starts minimal & clean - single empty paragraph block
                add(NotionBlock(type = NotionBlockType.PARAGRAPH, content = ""))
            }
        }
    }

    val notionBwIcons = listOf(
        "Description" to "Notă",
        "Folder" to "Dosar",
        "Book" to "Carte",
        "Code" to "Cod",
        "Terminal" to "Terminal",
        "Storage" to "Bază Date",
        "Lightbulb" to "Idee",
        "AutoAwesome" to "AI / Smart",
        "Work" to "Proiect",
        "CheckCircle" to "Sarcini",
        "Star" to "Favorit",
        "Bookmark" to "Bookmark",
        "Tag" to "Tag",
        "Key" to "Cheie",
        "Shield" to "Securitate",
        "Lock" to "Privat",
        "Rocket" to "Lansare",
        "AttachFile" to "Fișier",
        "Link" to "Link",
        "Palette" to "Design",
        "TableChart" to "Tabel",
        "FormatQuote" to "Citat"
    )

    val emojiCategories = listOf(
        "Populari" to listOf("📝", "🚀", "💡", "🎨", "📊", "📄", "💻", "📌", "⚙️", "🌟", "📚", "🎯", "⚡", "🧠", "💼", "🔥", "🔮", "✨"),
        "Idei & Tech" to listOf("🤖", "🔍", "📱", "🛠️", "🔒", "🌐", "🧪", "📈", "📐", "⚙️", "🖥️"),
        "Documente" to listOf("📑", "📂", "🖊️", "📅", "🗂️", "📌", "🏷️", "💼")
    )

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val saveAndDismiss = {
        focusManager.clearFocus()
        keyboardController?.hide()
        
        val hasAnyText = title.isNotBlank() || blocksState.any { it.content.isNotBlank() }
        if (hasAnyText) {
            val finalTitle = title.ifBlank {
                blocksState.firstOrNull { it.content.isNotBlank() }?.content?.lines()?.firstOrNull { it.isNotBlank() }?.take(40) ?: "Notă nesalvată"
            }
            val parsedTags = if (hasTags) {
                tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else emptyList()

            val snippetText = blocksState.firstOrNull { it.content.isNotBlank() }?.content ?: finalTitle
            val newItem = LibraryItem(
                id = existingNote?.id ?: ("l-" + UUID.randomUUID().toString().take(6)),
                title = finalTitle,
                category = "Notițe",
                snippet = if (snippetText.length > 70) snippetText.take(70) + "..." else snippetText,
                fullContent = blocksState.joinToString("\n") { it.content },
                dateAdded = existingNote?.dateAdded ?: System.currentTimeMillis(),
                iconEmoji = if (hasIcon) selectedEmoji else "icon:Description",
                coverGradientIndex = if (hasCover && customCoverUri == null) coverIndex else 0,
                customCoverUri = if (hasCover) customCoverUri else null,
                mediaUrl = if (hasCover) customCoverUri else null,
                tags = parsedTags,
                blocks = blocksState.toList(),
                isRecent = true,
                isFavorite = isFavoriteState,
                section = if (isFavoriteState) "Favorite" else (existingNote?.section?.takeIf { it != "Favorite" && it != "Workspace" } ?: "Privat"),
                parentId = existingNote?.parentId,
                lastModified = System.currentTimeMillis()
            )
            onSave(newItem)
        } else {
            onDismiss()
        }
    }

    BackHandler(onBack = saveAndDismiss)

    Dialog(
        onDismissRequest = saveAndDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            color = Color(0xFF191919) // Pure sleek Notion dark theme canvas
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Minimal Header Bar (Back / Auto-save button, page title, Favorite toggle button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(onClick = saveAndDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Înapoi",
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (title.isNotBlank()) title else "Pagină nouă",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCCCCCC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { isFavoriteState = !isFavoriteState },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavoriteState) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isFavoriteState) "Elimină din Favorite" else "Adaugă la Favorite",
                            tint = if (isFavoriteState) Color(0xFFFBBF24) else Color(0xFFCCCCCC),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Scrollable Notion Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. COVER BANNER (Custom Photo or Gradient)
                    if (hasCover) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .then(
                                    if (customCoverUri == null) {
                                        Modifier.background(Brush.horizontalGradient(notionCoverGradients[coverIndex % notionCoverGradients.size]))
                                    } else {
                                        Modifier.background(Color(0xFF1E1E1E))
                                    }
                                )
                        ) {
                            if (!customCoverUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = customCoverUri,
                                    contentDescription = "Copertă personalizată",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Sleek, compact translucent toolbar capsule
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.78f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Open Gallery Modal button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.clickable { showCoverPickerGallery = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Galerie", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Upload custom photo from phone button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.12f),
                                        modifier = Modifier.clickable { coverPickerLauncher.launch("image/*") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Telefon", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    // Switch gradient button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.12f),
                                        modifier = Modifier.clickable {
                                            customCoverUri = null
                                            coverIndex = (coverIndex + 1) % notionCoverGradients.size
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gradient", fontSize = 11.sp, color = Color.White)
                                        }
                                    }

                                    // Remove cover button
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Red.copy(alpha = 0.25f),
                                        modifier = Modifier.clickable {
                                            hasCover = false
                                            customCoverUri = null
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Elimină", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        // 2. PROGRESSIVE DISCLOSURE OPTION BUTTONS (Cover / Icon / Tag)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            if (!hasCover) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            showCoverPickerGallery = true
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFF777777), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adaugă copertă", fontSize = 12.sp, color = Color(0xFF888888))
                                }
                            }

                            if (!hasIcon) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            hasIcon = true
                                            showEmojiPicker = true
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.AddReaction, contentDescription = null, tint = Color(0xFF777777), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adaugă iconiță", fontSize = 12.sp, color = Color(0xFF888888))
                                }
                            }

                            if (!hasTags) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { hasTags = true }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Color(0xFF777777), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adaugă tag-uri", fontSize = 12.sp, color = Color(0xFF888888))
                                }
                            }
                        }

                        // 3. ICON DISPLAY (if enabled)
                        if (hasIcon) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showEmojiPicker = !showEmojiPicker }
                                    .padding(4.dp)
                            ) {
                                NotionPageIconDisplay(
                                    iconString = selectedEmoji,
                                    size = 44.dp,
                                    iconSize = 26.dp
                                )
                            }
                        }

                        // RICH NOTION ICON / EMOJI PICKER POPUP
                        AnimatedVisibility(visible = showEmojiPicker) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF222222),
                                border = BorderStroke(1.dp, Color(0xFF333333)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = selectedIconTab == 0,
                                                onClick = { selectedIconTab = 0 },
                                                label = { Text("Notion B&W", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF383838),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                            FilterChip(
                                                selected = selectedIconTab == 1,
                                                onClick = { selectedIconTab = 1 },
                                                label = { Text("Emoji", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF383838),
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }

                                        TextButton(onClick = {
                                            hasIcon = false
                                            showEmojiPicker = false
                                        }) {
                                            Text("Elimină", fontSize = 11.sp, color = Color(0xFFFF6B6B))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (selectedIconTab == 0) {
                                        // Notion Minimalist Black & White Icons Grid
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(notionBwIcons) { (iconKey, label) ->
                                                val fullKey = "icon:$iconKey"
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (selectedEmoji == fullKey) Color(0xFF3B3B3B) else Color.Transparent)
                                                        .clickable {
                                                            selectedEmoji = fullKey
                                                            showEmojiPicker = false
                                                        }
                                                        .padding(6.dp)
                                                ) {
                                                    NotionPageIconDisplay(
                                                        iconString = fullKey,
                                                        size = 36.dp,
                                                        iconSize = 20.dp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(label, fontSize = 9.sp, color = Color(0xFFAAAAAA), maxLines = 1)
                                                }
                                            }
                                        }
                                    } else {
                                        // Emojis list
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(emojiCategories[0].second) { emoji ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(if (selectedEmoji == emoji) Color(0xFF383838) else Color.Transparent)
                                                        .clickable {
                                                            selectedEmoji = emoji
                                                            showEmojiPicker = false
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = emoji, fontSize = 22.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. TITLE INPUT FIELD (Notion Style)
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = TextStyle(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFFFF),
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(Color(0xFF2E7D32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = "Fără titlu",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF444444)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // 5. TAGS INPUT (if enabled)
                        if (hasTags) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                BasicTextField(
                                    value = tagsInput,
                                    onValueChange = { tagsInput = it },
                                    textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFFBBBBBB)),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (tagsInput.isEmpty()) {
                                                Text("Introduceți tag-uri separate prin virgulă...", fontSize = 13.sp, color = Color(0xFF555555))
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { hasTags = false },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Șterge tag-uri", tint = Color(0xFF666666), modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 6. BLOCK-BASED EDITOR LIST
                        blocksState.forEachIndexed { index, block ->
                            NotionBlockEditorRow(
                                block = block,
                                index = index,
                                isFocused = (index == focusedBlockIndex),
                                isDragging = (draggingBlockIndex == index),
                                dragOffsetY = if (draggingBlockIndex == index) dragOffsetY else 0f,
                                onFocus = { focusedBlockIndex = index },
                                onStartDrag = {
                                    draggingBlockIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragDelta = { deltaY ->
                                    dragOffsetY += deltaY
                                    val threshold = 45f
                                    val currentIdx = draggingBlockIndex ?: index
                                    if (dragOffsetY > threshold && currentIdx < blocksState.size - 1) {
                                        val temp = blocksState[currentIdx]
                                        blocksState[currentIdx] = blocksState[currentIdx + 1]
                                        blocksState[currentIdx + 1] = temp
                                        draggingBlockIndex = currentIdx + 1
                                        focusedBlockIndex = currentIdx + 1
                                        dragOffsetY = 0f
                                    } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                        val temp = blocksState[currentIdx]
                                        blocksState[currentIdx] = blocksState[currentIdx - 1]
                                        blocksState[currentIdx - 1] = temp
                                        draggingBlockIndex = currentIdx - 1
                                        focusedBlockIndex = currentIdx - 1
                                        dragOffsetY = 0f
                                    }
                                },
                                onEndDrag = {
                                    draggingBlockIndex = null
                                    dragOffsetY = 0f
                                },
                                onOpenSubPage = {
                                    editingSubPageBlockIndex = index
                                },
                                onAddNewBlockBelow = { idx, type, initialContent ->
                                    val newIdx = idx + 1
                                    blocksState.add(newIdx, NotionBlock(type = type, content = initialContent))
                                    focusedBlockIndex = newIdx
                                },
                                isOnlyBlock = (blocksState.size == 1),
                                onUpdateBlock = { updated -> blocksState[index] = updated },
                                onOpenPlusMenu = { activeBlockForPlusMenu = index },
                                onOpenRowMenu = { activeBlockForRowMenu = index }
                            )
                        }

                        // Clean Bottom Clickable Canvas area to naturally append paragraph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clickable {
                                    val newIdx = blocksState.size
                                    blocksState.add(NotionBlock(type = NotionBlockType.PARAGRAPH, content = ""))
                                    focusedBlockIndex = newIdx
                                }
                        )
                    }
                }
            }
        }
    }

    // PLUS (+) NOTION BLOCK SELECTOR (1:1 Notion Mobile Popover)
    activeBlockForPlusMenu?.let { targetIndex ->
        val notionPlusItems = remember {
            listOf(
                NotionPlusMenuItem("New Page", icon = Icons.Default.Description, type = NotionBlockType.PAGE),
                NotionPlusMenuItem("Text", customText = "T", type = NotionBlockType.PARAGRAPH),
                NotionPlusMenuItem("To-do list", icon = Icons.Default.CheckBox, type = NotionBlockType.CHECKLIST),
                NotionPlusMenuItem("Heading 1", customText = "H₁", type = NotionBlockType.HEADING_1),
                NotionPlusMenuItem("Heading 2", customText = "H₂", type = NotionBlockType.HEADING_2),
                NotionPlusMenuItem("Bulleted List", icon = Icons.AutoMirrored.Filled.List, type = NotionBlockType.BULLET_LIST),
                NotionPlusMenuItem("Numbered List", icon = Icons.Default.FormatListNumbered, type = NotionBlockType.NUMBERED_LIST),
                NotionPlusMenuItem("Image", icon = Icons.Default.Image, type = NotionBlockType.PARAGRAPH),
                NotionPlusMenuItem("File", icon = Icons.Default.UploadFile, type = NotionBlockType.LINK_BOOKMARK),
                NotionPlusMenuItem("Code", icon = Icons.Default.Code, type = NotionBlockType.CODE),
                NotionPlusMenuItem("Quote", icon = Icons.Default.FormatQuote, type = NotionBlockType.QUOTE),
                NotionPlusMenuItem("Heading 3", customText = "H₃", type = NotionBlockType.HEADING_3),
                NotionPlusMenuItem("Table", icon = Icons.Default.TableChart, type = NotionBlockType.TABLE),
                NotionPlusMenuItem("Toggle list", icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, type = NotionBlockType.TOGGLE),
                NotionPlusMenuItem("Callout", icon = Icons.Default.Lightbulb, type = NotionBlockType.CALLOUT),
                NotionPlusMenuItem("Divider", icon = Icons.Default.HorizontalRule, type = NotionBlockType.DIVIDER)
            )
        }

        Dialog(
            onDismissRequest = { activeBlockForPlusMenu = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { activeBlockForPlusMenu = null },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF222225),
                    border = BorderStroke(1.dp, Color(0xFF333338)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .width(250.dp)
                        .heightIn(max = 490.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Prevents dismiss on card click */ }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(notionPlusItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newIdx = targetIndex + 1
                                        val newBlock = when (item.type) {
                                            NotionBlockType.PAGE -> NotionBlock(
                                                type = NotionBlockType.PAGE,
                                                content = "",
                                                subPageIcon = "icon:Description",
                                                subPageCoverGradientIndex = 0,
                                                subPageCustomCoverUri = null,
                                                subPageTags = emptyList(),
                                                subPageBlocks = listOf(
                                                    NotionBlock(type = NotionBlockType.PARAGRAPH, content = "")
                                                )
                                            )
                                            NotionBlockType.TABLE -> NotionBlock(
                                                type = NotionBlockType.TABLE,
                                                tableRows = listOf(
                                                    listOf("Coloană 1", "Coloană 2"),
                                                    listOf("Element 1", "Element 2")
                                                )
                                            )
                                            else -> NotionBlock(type = item.type, content = "")
                                        }
                                        blocksState.add(newIdx, newBlock)
                                        focusedBlockIndex = newIdx
                                        activeBlockForPlusMenu = null
                                        if (item.type == NotionBlockType.PAGE) {
                                            editingSubPageBlockIndex = newIdx
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.customText != null) {
                                        Text(
                                            text = item.customText,
                                            fontSize = if (item.customText.length > 1) 15.sp else 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB5B5BC)
                                        )
                                    } else if (item.icon != null) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = Color(0xFFB5B5BC),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFEAEAEA)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 6-DOTS ROW ACTIONS DIALOG (Move Up/Down, Duplicate, Delete)
    activeBlockForRowMenu?.let { targetIndex ->
        Dialog(onDismissRequest = { activeBlockForRowMenu = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF222222),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Opțiuni Rând Notion", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    RowMenuItem(
                        icon = Icons.Default.ArrowUpward,
                        label = "Mută rândul mai sus",
                        enabled = targetIndex > 0,
                        onClick = {
                            if (targetIndex > 0) {
                                val temp = blocksState[targetIndex]
                                blocksState[targetIndex] = blocksState[targetIndex - 1]
                                blocksState[targetIndex - 1] = temp
                            }
                            activeBlockForRowMenu = null
                        }
                    )

                    RowMenuItem(
                        icon = Icons.Default.ArrowDownward,
                        label = "Mută rândul mai jos",
                        enabled = targetIndex < blocksState.size - 1,
                        onClick = {
                            if (targetIndex < blocksState.size - 1) {
                                val temp = blocksState[targetIndex]
                                blocksState[targetIndex] = blocksState[targetIndex + 1]
                                blocksState[targetIndex + 1] = temp
                            }
                            activeBlockForRowMenu = null
                        }
                    )

                    RowMenuItem(
                        icon = Icons.Default.ContentCopy,
                        label = "Duplică rândul",
                        enabled = true,
                        onClick = {
                            val newBlock = blocksState[targetIndex].copy(id = UUID.randomUUID().toString())
                            blocksState.add(targetIndex + 1, newBlock)
                            activeBlockForRowMenu = null
                        }
                    )

                    RowMenuItem(
                        icon = Icons.Default.Delete,
                        label = "Șterge rândul",
                        textColor = Color(0xFFFF6B6B),
                        enabled = blocksState.size > 1,
                        onClick = {
                            if (blocksState.size > 1) {
                                blocksState.removeAt(targetIndex)
                            }
                            activeBlockForRowMenu = null
                        }
                    )
                }
            }
        }
    }

    if (showCoverPickerGallery) {
        CoverPickerGalleryDialog(
            onDismiss = { showCoverPickerGallery = false },
            onSelectImageCover = { imageUrl ->
                customCoverUri = imageUrl
                hasCover = true
                showCoverPickerGallery = false
            },
            onSelectGradientCover = { gradIdx ->
                customCoverUri = null
                coverIndex = gradIdx
                hasCover = true
                showCoverPickerGallery = false
            },
            onUploadFromDevice = {
                showCoverPickerGallery = false
                coverPickerLauncher.launch("image/*")
            },
            onRemoveCover = {
                hasCover = false
                customCoverUri = null
                showCoverPickerGallery = false
            }
        )
    }

    // NESTED SUB-PAGE NOTION EDITOR MODAL
    editingSubPageBlockIndex?.let { subIdx ->
        val subBlock = blocksState.getOrNull(subIdx)
        if (subBlock != null && subBlock.type == NotionBlockType.PAGE) {
            val isBrandNewSubPage = subBlock.content.isBlank() && 
                    (subBlock.subPageBlocks.isEmpty() || (subBlock.subPageBlocks.size == 1 && subBlock.subPageBlocks[0].content.isBlank())) &&
                    subBlock.subPageCoverGradientIndex <= 0 && 
                    subBlock.subPageCustomCoverUri == null && 
                    subBlock.subPageTags.isEmpty()

            val virtualSubItem = remember(subBlock.id, subBlock.content, subBlock.subPageBlocks.size) {
                if (isBrandNewSubPage) {
                    null // Opens clean 1:1 Notion New Page modal with placeholder "Fără titlu" and clean action bar
                } else {
                    LibraryItem(
                        id = subBlock.subPageId ?: ("sub-" + subBlock.id.take(8)),
                        title = subBlock.content,
                        category = "Notițe",
                        snippet = subBlock.subPageBlocks.firstOrNull { it.content.isNotBlank() }?.content ?: "",
                        fullContent = subBlock.subPageBlocks.joinToString("\n") { it.content },
                        dateAdded = System.currentTimeMillis(),
                        iconEmoji = subBlock.subPageIcon,
                        coverGradientIndex = subBlock.subPageCoverGradientIndex,
                        mediaUrl = subBlock.subPageCustomCoverUri,
                        tags = subBlock.subPageTags,
                        blocks = if (subBlock.subPageBlocks.isNotEmpty()) subBlock.subPageBlocks else listOf(NotionBlock(type = NotionBlockType.PARAGRAPH, content = ""))
                    )
                }
            }

            NotionNoteEditorModal(
                existingNote = virtualSubItem,
                onDismiss = { editingSubPageBlockIndex = null },
                onSave = { savedSubPage ->
                    blocksState[subIdx] = subBlock.copy(
                        content = savedSubPage.title.ifBlank { "Pagină fără titlu" },
                        subPageIcon = if (savedSubPage.iconEmoji.isNotBlank()) savedSubPage.iconEmoji else "icon:Description",
                        subPageCoverGradientIndex = savedSubPage.coverGradientIndex,
                        subPageCustomCoverUri = savedSubPage.mediaUrl,
                        subPageTags = savedSubPage.tags,
                        subPageBlocks = savedSubPage.blocks
                    )
                    editingSubPageBlockIndex = null
                }
            )
        }
    }
}

@Composable
private fun RowMenuItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) textColor else Color(0xFF555555),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (enabled) textColor else Color(0xFF555555),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class NotionPlusMenuItem(
    val title: String,
    val icon: ImageVector? = null,
    val customText: String? = null,
    val type: NotionBlockType
)

// Notion Block Editor Row (Text input, Table grid, Checkbox, Subpage, etc + Right side buttons)
@Composable
private fun NotionBlockEditorRow(
    block: NotionBlock,
    index: Int = 0,
    isFocused: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    onFocus: () -> Unit = {},
    onStartDrag: () -> Unit = {},
    onDragDelta: (Float) -> Unit = {},
    onEndDrag: () -> Unit = {},
    onOpenSubPage: () -> Unit = {},
    onAddNewBlockBelow: (index: Int, type: NotionBlockType, initialContent: String) -> Unit = { _, _, _ -> },
    isOnlyBlock: Boolean = false,
    onUpdateBlock: (NotionBlock) -> Unit,
    onOpenPlusMenu: () -> Unit,
    onOpenRowMenu: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (isFocused && block.type != NotionBlockType.PAGE) {
            kotlinx.coroutines.delay(60)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val rowElevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "rowElevation")
    val rowScale by animateFloatAsState(if (isDragging) 1.015f else 1f, label = "rowScale")

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDragging) Color(0xFF282836) else Color.Transparent,
        border = if (isDragging) BorderStroke(1.dp, Color(0xFF555577)) else null,
        shadowElevation = rowElevation,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
                if (isDragging) {
                    translationY = dragOffsetY
                }
            }
            .padding(vertical = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isDragging) 6.dp else 0.dp, vertical = if (isDragging) 4.dp else 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFocus() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Editable Content Area
                Box(modifier = Modifier.weight(1f)) {
                    when (block.type) {
                        NotionBlockType.PAGE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onOpenSubPage() }
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NotionPageIconDisplay(
                                    iconString = block.subPageIcon.ifBlank { "icon:Description" },
                                    size = 22.dp,
                                    iconSize = 18.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = block.content.ifBlank { "Pagină fără titlu" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Deschide sub-pagina",
                                    tint = Color(0xFF888899),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        NotionBlockType.TABLE -> {
                    // Notion Interactive Table Block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        val currentRows = if (block.tableRows.isNotEmpty()) block.tableRows else listOf(
                            listOf("Coloană 1", "Coloană 2"),
                            listOf("Element 1", "Element 2")
                        )

                        currentRows.forEachIndexed { rIdx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEachIndexed { cIdx, cellValue ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (rIdx == 0) Color(0xFF2D2D2D) else Color(0xFF1E1E1E),
                                        border = BorderStroke(0.5.dp, Color(0xFF444444)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        BasicTextField(
                                            value = cellValue,
                                            onValueChange = { newCell ->
                                                val newRows = currentRows.mapIndexed { r, rList ->
                                                    if (r == rIdx) {
                                                        rList.mapIndexed { c, valStr -> if (c == cIdx) newCell else valStr }
                                                    } else rList
                                                }
                                                onUpdateBlock(block.copy(tableRows = newRows))
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            cursorBrush = SolidColor(Color.White),
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .onFocusChanged { if (it.isFocused) onFocus() }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    val colCount = currentRows.firstOrNull()?.size ?: 2
                                    val newRow = List(colCount) { "Celulă nouă" }
                                    onUpdateBlock(block.copy(tableRows = currentRows + listOf(newRow)))
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+ Adaugă rând", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            TextButton(
                                onClick = {
                                    val newRows = currentRows.mapIndexed { idx, rList ->
                                        rList + if (idx == 0) "Antet nou" else "Celulă"
                                    }
                                    onUpdateBlock(block.copy(tableRows = newRows))
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+ Adaugă coloană", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                NotionBlockType.CHECKLIST -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = block.isChecked == true,
                            onCheckedChange = { onUpdateBlock(block.copy(isChecked = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = block.content,
                            onValueChange = { newText ->
                                if (newText.contains("\n")) {
                                    val parts = newText.split("\n", limit = 2)
                                    onUpdateBlock(block.copy(content = parts[0]))
                                    onAddNewBlockBelow(index, NotionBlockType.CHECKLIST, parts.getOrElse(1) { "" })
                                } else {
                                    onUpdateBlock(block.copy(content = newText))
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = if (block.isChecked == true) Color(0xFF666666) else Color(0xFFDDDDDD),
                                textDecoration = if (block.isChecked == true) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) onFocus() },
                            decorationBox = { inner ->
                                Box {
                                    if ((isFocused || isOnlyBlock || index == 0) && block.content.isEmpty()) {
                                        Text("Sarcină / To-do...", fontSize = 14.sp, color = Color(0xFF555555))
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
                NotionBlockType.HEADING_1 -> {
                    BasicTextField(
                        value = block.content,
                        onValueChange = { newText ->
                            if (newText.contains("\n")) {
                                val parts = newText.split("\n", limit = 2)
                                onUpdateBlock(block.copy(content = parts[0]))
                                onAddNewBlockBelow(index, NotionBlockType.PARAGRAPH, parts.getOrElse(1) { "" })
                            } else {
                                onUpdateBlock(block.copy(content = newText))
                            }
                        },
                        textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocus() },
                        decorationBox = { inner ->
                            Box {
                                if (block.content.isEmpty()) Text("Titlu 1", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                                inner()
                            }
                        }
                    )
                }
                NotionBlockType.HEADING_2 -> {
                    BasicTextField(
                        value = block.content,
                        onValueChange = { newText ->
                            if (newText.contains("\n")) {
                                val parts = newText.split("\n", limit = 2)
                                onUpdateBlock(block.copy(content = parts[0]))
                                onAddNewBlockBelow(index, NotionBlockType.PARAGRAPH, parts.getOrElse(1) { "" })
                            } else {
                                onUpdateBlock(block.copy(content = newText))
                            }
                        },
                        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEEEEEE)),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocus() },
                        decorationBox = { inner ->
                            Box {
                                if (block.content.isEmpty()) Text("Titlu 2", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                                inner()
                            }
                        }
                    )
                }
                NotionBlockType.HEADING_3 -> {
                    BasicTextField(
                        value = block.content,
                        onValueChange = { newText ->
                            if (newText.contains("\n")) {
                                val parts = newText.split("\n", limit = 2)
                                onUpdateBlock(block.copy(content = parts[0]))
                                onAddNewBlockBelow(index, NotionBlockType.PARAGRAPH, parts.getOrElse(1) { "" })
                            } else {
                                onUpdateBlock(block.copy(content = newText))
                            }
                        },
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDDDDDD)),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocus() },
                        decorationBox = { inner ->
                            Box {
                                if (block.content.isEmpty()) Text("Titlu 3", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                                inner()
                            }
                        }
                    )
                }
                NotionBlockType.BULLET_LIST -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = block.content,
                            onValueChange = { newText ->
                                if (newText.contains("\n")) {
                                    val parts = newText.split("\n", limit = 2)
                                    onUpdateBlock(block.copy(content = parts[0]))
                                    onAddNewBlockBelow(index, NotionBlockType.BULLET_LIST, parts.getOrElse(1) { "" })
                                } else {
                                    onUpdateBlock(block.copy(content = newText))
                                }
                            },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) onFocus() },
                            decorationBox = { inner ->
                                Box {
                                    if (block.content.isEmpty()) Text("Listă...", fontSize = 14.sp, color = Color(0xFF555555))
                                    inner()
                                }
                            }
                        )
                    }
                }
                NotionBlockType.NUMBERED_LIST -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("${index + 1}. ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicTextField(
                            value = block.content,
                            onValueChange = { newText ->
                                if (newText.contains("\n")) {
                                    val parts = newText.split("\n", limit = 2)
                                    onUpdateBlock(block.copy(content = parts[0]))
                                    onAddNewBlockBelow(index, NotionBlockType.NUMBERED_LIST, parts.getOrElse(1) { "" })
                                } else {
                                    onUpdateBlock(block.copy(content = newText))
                                }
                            },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD)),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) onFocus() },
                            decorationBox = { inner ->
                                Box {
                                    if (block.content.isEmpty()) Text("Listă numerotată...", fontSize = 14.sp, color = Color(0xFF555555))
                                    inner()
                                }
                            }
                        )
                    }
                }
                NotionBlockType.TOGGLE -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateBlock(block.copy(isExpanded = !block.isExpanded)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (block.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            BasicTextField(
                                value = block.content,
                                onValueChange = { newText ->
                                    if (newText.contains("\n")) {
                                        val parts = newText.split("\n", limit = 2)
                                        onUpdateBlock(block.copy(content = parts[0]))
                                        onAddNewBlockBelow(index, NotionBlockType.TOGGLE, parts.getOrElse(1) { "" })
                                    } else {
                                        onUpdateBlock(block.copy(content = newText))
                                    }
                                },
                                textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { if (it.isFocused) onFocus() },
                                decorationBox = { inner ->
                                    Box {
                                        if (block.content.isEmpty()) Text("Meniu pliabil / Toggle...", fontSize = 14.sp, color = Color(0xFF555555))
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                }
                NotionBlockType.LINK_BOOKMARK -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2638),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = block.content,
                                onValueChange = { newText ->
                                    if (newText.contains("\n")) {
                                        val parts = newText.split("\n", limit = 2)
                                        onUpdateBlock(block.copy(content = parts[0]))
                                        onAddNewBlockBelow(index, NotionBlockType.PARAGRAPH, parts.getOrElse(1) { "" })
                                    } else {
                                        onUpdateBlock(block.copy(content = newText))
                                    }
                                },
                                textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF93C5FD)),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { if (it.isFocused) onFocus() },
                                decorationBox = { inner ->
                                    Box {
                                        if (block.content.isEmpty()) Text("Introduceți un link web sau bookmark...", fontSize = 13.sp, color = Color(0xFF475569))
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                }
                NotionBlockType.CODE -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF111111),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = block.content,
                            onValueChange = { newText ->
                                onUpdateBlock(block.copy(content = newText))
                            },
                            textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF80CBC4)),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .padding(10.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) onFocus() },
                            decorationBox = { inner ->
                                Box {
                                    if (block.content.isEmpty()) Text("// Scrie cod aici...", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF444444))
                                    inner()
                                }
                            }
                        )
                    }
                }
                NotionBlockType.QUOTE -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = block.content,
                            onValueChange = { newText ->
                                if (newText.contains("\n")) {
                                    val parts = newText.split("\n", limit = 2)
                                    onUpdateBlock(block.copy(content = parts[0]))
                                    onAddNewBlockBelow(index, NotionBlockType.QUOTE, parts.getOrElse(1) { "" })
                                } else {
                                    onUpdateBlock(block.copy(content = newText))
                                }
                            },
                            textStyle = TextStyle(fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Color(0xFFCCCCCC)),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) onFocus() },
                            decorationBox = { inner ->
                                Box {
                                    if (block.content.isEmpty()) Text("Citat...", fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Color(0xFF555555))
                                    inner()
                                }
                            }
                        )
                    }
                }
                NotionBlockType.CALLOUT -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF252525),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(block.calloutEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = block.content,
                                onValueChange = { newText ->
                                    if (newText.contains("\n")) {
                                        val parts = newText.split("\n", limit = 2)
                                        onUpdateBlock(block.copy(content = parts[0]))
                                        onAddNewBlockBelow(index, NotionBlockType.PARAGRAPH, parts.getOrElse(1) { "" })
                                    } else {
                                        onUpdateBlock(block.copy(content = newText))
                                    }
                                },
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { if (it.isFocused) onFocus() },
                                decorationBox = { inner ->
                                    Box {
                                        if (block.content.isEmpty()) Text("Notă de evidențiat...", fontSize = 13.sp, color = Color(0xFF666666))
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                }
                NotionBlockType.DIVIDER -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF333333))
                }
                else -> {
                    // PARAGRAPH / DEFAULT - Single row focus placeholder
                    BasicTextField(
                        value = block.content,
                        onValueChange = { newText ->
                            if (newText.contains("\n")) {
                                val parts = newText.split("\n", limit = 2)
                                onUpdateBlock(block.copy(content = parts[0]))
                                onAddNewBlockBelow(index, NotionBlockType.PARAGRAPH, parts.getOrElse(1) { "" })
                            } else {
                                onUpdateBlock(block.copy(content = newText))
                            }
                        },
                        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFFDDDDDD), lineHeight = 20.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocus() },
                        decorationBox = { inner ->
                            Box {
                                if ((isFocused || isOnlyBlock) && block.content.isEmpty()) {
                                    Text("Scrie ceva sau apasă '+' pentru opțiuni...", fontSize = 14.sp, color = Color(0xFF444444))
                                }
                                inner()
                            }
                        }
                    )
                }
            }
        }

        // Right side Notion Control Buttons (plus & 3-dots with long-press drag)
        if (isFocused || isOnlyBlock || isDragging) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                // Plus (+) button
                IconButton(
                    onClick = onOpenPlusMenu,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adaugă bloc",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(21.dp)
                    )
                }

                // 3-Dots button (Long press to drag and reorder, Click for menu)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
                    border = if (isDragging) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.y)
                                },
                                onDragEnd = { onEndDrag() },
                                onDragCancel = { onEndDrag() }
                            )
                        }
                        .clickable { onOpenRowMenu() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDragging) Icons.Default.DragHandle else Icons.Default.MoreVert,
                            contentDescription = "Meniu opțiuni sau ține apăsat lung pentru reordonare",
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else Color(0xFF888888),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        } else {
            // Keep consistent right spacing so text doesn't shift when changing active row
            Spacer(modifier = Modifier.width(74.dp))
        }
    }
}
}
}

private data class PresetCoverItem(
    val title: String,
    val category: String,
    val imageUrl: String
)

@Composable
private fun CoverPickerGalleryDialog(
    onDismiss: () -> Unit,
    onSelectImageCover: (String) -> Unit,
    onSelectGradientCover: (Int) -> Unit,
    onUploadFromDevice: () -> Unit,
    onRemoveCover: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Toate") }

    val presetCovers = remember {
        listOf(
            PresetCoverItem("Munte & Ceață", "Natură", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&q=80"),
            PresetCoverItem("Pădure de Munte", "Natură", "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1200&q=80"),
            PresetCoverItem("Apus pe Plajă", "Natură", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1200&q=80"),
            PresetCoverItem("Lac de Smarald", "Natură", "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1200&q=80"),
            PresetCoverItem("Grădină Zen", "Natură", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=1200&q=80"),
            
            PresetCoverItem("Forme Geometrice", "Abstract", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80"),
            PresetCoverItem("Valuri 3D Neon", "Abstract", "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=1200&q=80"),
            PresetCoverItem("Marmură Aurie", "Abstract", "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=1200&q=80"),
            
            PresetCoverItem("Oraș Neon Cyber", "Apus & Spațiu", "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=1200&q=80"),
            PresetCoverItem("Galaxie Cosmică", "Apus & Spațiu", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1200&q=80"),
            PresetCoverItem("Apus Cald Orange", "Apus & Spațiu", "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?w=1200&q=80"),
            
            PresetCoverItem("Arhitectură Minimală", "Minimalist", "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=1200&q=80"),
            PresetCoverItem("Birou Muncă Zen", "Minimalist", "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=1200&q=80")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A20),
            border = BorderStroke(1.dp, Color(0xFF30303D)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 510.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header Compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alege Copertă", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Închide", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Bar (Upload + Remove)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Upload button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { onUploadFromDevice() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Din telefon", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Remove Cover button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Red.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { onRemoveCover() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Elimină", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color(0xFFFF6B6B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                val categories = listOf("Toate", "Natură", "Abstract", "Apus & Spațiu", "Minimalist", "Gradiente")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = (selectedCategory == cat)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF262632),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFBBBBCC),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Grid Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategory != "Gradiente") {
                        val filteredImages = presetCovers.filter { item ->
                            selectedCategory == "Toate" || item.category == selectedCategory
                        }

                        val rows = filteredImages.chunked(3)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF252530),
                                        border = BorderStroke(0.5.dp, Color(0xFF383848)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(68.dp)
                                            .clickable { onSelectImageCover(item.imageUrl) }
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = item.imageUrl,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                                            startY = 15f
                                                        )
                                                    )
                                            )
                                            Text(
                                                text = item.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (selectedCategory == "Toate" || selectedCategory == "Gradiente") {
                        if (selectedCategory == "Toate") {
                            Text("GRADIENTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888899), letterSpacing = 1.sp)
                        }

                        val gradRows = notionCoverGradients.indices.chunked(3)
                        gradRows.forEach { gradRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                gradRow.forEach { gIdx ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable { onSelectGradientCover(gIdx) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Brush.horizontalGradient(notionCoverGradients[gIdx])),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Gradient #${gIdx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                                repeat(3 - gradRow.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MinimalDocumentCard(
    item: LibraryItem,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, badgeColor) = remember(item.category, item.title) {
                when {
                    item.category.contains("PDF", ignoreCase = true) || item.title.endsWith(".pdf", ignoreCase = true) ->
                        Icons.Default.PictureAsPdf to Color(0xFFE53935)
                    item.category.contains("DOC", ignoreCase = true) || item.title.endsWith(".doc", ignoreCase = true) || item.title.endsWith(".docx", ignoreCase = true) ->
                        Icons.Default.Description to Color(0xFF1E88E5)
                    item.category.contains("Imagin", ignoreCase = true) || item.title.endsWith(".png", ignoreCase = true) || item.title.endsWith(".jpg", ignoreCase = true) ->
                        Icons.Default.Image to Color(0xFF43A047)
                    item.category.contains("Video", ignoreCase = true) || item.title.endsWith(".mp4", ignoreCase = true) ->
                        Icons.Default.Movie to Color(0xFF8E24AA)
                    else -> Icons.Default.Folder to Color(0xFFFB8C00)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.fileSize > 0) {
                        Text(
                            text = formatBytes(item.fileSize),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = item.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opțiuni",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Deschide", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onView()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Editează", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Șterge", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageGalleryCard(
    item: LibraryItem,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.mediaUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF2E7D32),
                                    Color(0xFF1565C0),
                                    Color(0xFF6A1B9A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Deschide imaginea", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onView()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Editează", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Șterge", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGalleryCard(
    item: LibraryItem,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.mediaUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
                            )
                        )
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Redă Video",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.fileSize > 0) {
                            Text(
                                text = formatBytes(item.fileSize),
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Redă / Deschide", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onView()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Editează", fontSize = 13.sp) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Șterge", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Modal for adding File Resources (Images, Videos, PDFs, Docs, Sheets, etc.)
@Composable
private fun AddFileResourceModal(
    initialCategory: String = "Documente",
    onDismiss: () -> Unit,
    onSave: (LibraryItem) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var snippet by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("0 B") }
    var fileSizeLong by remember { mutableStateOf(0L) }
    var tagsText by remember { mutableStateOf(if (initialCategory == "Imagini") "Foto, Media" else if (initialCategory == "Videoclipuri") "Video, Clip" else "Document, Resursă") }
    var localFilePath by remember { mutableStateOf<String?>(null) }
    var fileExtension by remember { mutableStateOf("") }
    var mimeType by remember { mutableStateOf("") }
    var originalFileName by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("📄") }
    var importedSuccessfully by remember { mutableStateOf(false) }

    // Launcher for ANY file format (PDF, DOCX, XLSX, TXT, CSV, MP4, etc.)
    val anyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val item = LibraryStorageManager.importFileFromUri(context, it, forcedCategory = if (category != "Documente") category else null)
            if (item != null) {
                title = item.title
                category = item.category
                snippet = item.snippet
                fileSizeLong = item.fileSize
                fileSize = formatBytes(item.fileSize)
                mediaUrl = item.mediaUrl ?: ""
                localFilePath = item.filePath
                fileExtension = item.fileExtension
                mimeType = item.mimeType
                originalFileName = item.originalFileName
                iconEmoji = item.iconEmoji
                tagsText = item.tags.joinToString(", ")
                importedSuccessfully = true
            }
        }
    }

    // Launcher for Photos & Videos
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val item = LibraryStorageManager.importFileFromUri(context, it)
            if (item != null) {
                title = item.title
                category = item.category
                snippet = item.snippet
                fileSizeLong = item.fileSize
                fileSize = formatBytes(item.fileSize)
                mediaUrl = item.mediaUrl ?: ""
                localFilePath = item.filePath
                fileExtension = item.fileExtension
                mimeType = item.mimeType
                originalFileName = item.originalFileName
                iconEmoji = item.iconEmoji
                tagsText = item.tags.joinToString(", ")
                importedSuccessfully = true
            }
        }
    }

    val saveAndClose = {
        if (title.isNotBlank()) {
            val parsedTags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val itemToSave = LibraryItem(
                id = "l-" + UUID.randomUUID().toString().take(6),
                title = title,
                category = category,
                snippet = snippet.ifBlank { "Fișier stocat în librăria locală a dispozitivului" },
                fullContent = snippet,
                dateAdded = System.currentTimeMillis(),
                mediaUrl = mediaUrl.ifBlank { null },
                filePath = localFilePath ?: "",
                fileExtension = fileExtension,
                mimeType = mimeType,
                originalFileName = originalFileName,
                fileSize = fileSizeLong,
                tags = parsedTags,
                pageCount = if (category == "PDF-uri" || fileExtension == "pdf") 1 else 1,
                iconEmoji = iconEmoji.ifBlank {
                    when (category) {
                        "Imagini" -> "🖼️"
                        "Videoclipuri" -> "🎥"
                        "PDF-uri" -> "📄"
                        "Foi de calcul" -> "📊"
                        "Documente" -> "📑"
                        else -> "📁"
                    }
                },
                lastModified = System.currentTimeMillis()
            )
            onSave(itemToSave)
        } else {
            onDismiss()
        }
    }

    BackHandler(onBack = saveAndClose)

    val categories = listOf("Documente", "Foi de calcul", "PDF-uri", "Imagini", "Videoclipuri", "Media")

    Dialog(onDismissRequest = saveAndClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(iconEmoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adaugă Fișier în Librărie", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = saveAndClose, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Închide")
                    }
                }

                // Primary Action: Pick from device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { anyFileLauncher.launch("*/*") }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alege orice Fișier",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "PDF, DOCX, XLSX, etc.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { mediaLauncher.launch("image/*") }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Galerie Foto / Video",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "JPG, PNG, MP4 etc.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (importedSuccessfully && localFilePath != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1B2A20),
                        border = BorderStroke(1.dp, Color(0xFF2E593E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Fișier importat în memoria dispozitivului",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE0E0E0)
                                )
                                Text(
                                    text = "$originalFileName • $fileSize • $fileExtension",
                                    fontSize = 10.sp,
                                    color = Color(0xFFAAAAAA)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nume Fișier / Titlu") },
                    placeholder = { Text("ex: Raport_Financiar_2026.xlsx") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categorie Fișier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = snippet,
                    onValueChange = { snippet = it },
                    label = { Text("Descriere / Note Conținut") },
                    placeholder = { Text("Sumar sau detalii despre fișier...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Etichete / Tags (separate prin virgulă)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if ((category == "Imagini" || category == "Videoclipuri") && localFilePath == null) {
                    OutlinedTextField(
                        value = mediaUrl,
                        onValueChange = { mediaUrl = it },
                        label = { Text("URL Media / Imagine") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Anulează") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = saveAndClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Salvează în Librărie", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ENHANCED DISPLAY & READER VIEWER (IMAGES, PDFS & DOCUMENTS)
// -----------------------------------------------------------------------------
@Composable
private fun ViewLibraryItemModal(
    item: LibraryItem,
    onDismiss: () -> Unit,
    onEditNotion: () -> Unit,
    onSendToChat: (String) -> Unit,
    onToggleCheckbox: (blockId: String, isChecked: Boolean) -> Unit,
    onToggleExpand: (blockId: String) -> Unit
) {
    if (item.category == "Notițe") {
        LaunchedEffect(item.id) {
            onEditNotion()
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            when (item.category) {
                "Imagini" -> ViewImageDetail(
                    item = item,
                    onDismiss = onDismiss,
                    onSendToChat = onSendToChat
                )
                "PDF-uri", "Documente" -> ViewPdfDocumentDetail(
                    item = item,
                    onDismiss = onDismiss,
                    onSendToChat = onSendToChat
                )
                else -> ViewGenericItemDetail(
                    item = item,
                    onDismiss = onDismiss,
                    onSendToChat = onSendToChat
                )
            }
        }
    }
}

// 2. High-Res Interactive Image Viewer
@Composable
private fun ViewImageDetail(
    item: LibraryItem,
    onDismiss: () -> Unit,
    onSendToChat: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Image Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "${item.fileSize ?: "2.8 MB"} • ${item.fileExtension.uppercase().ifBlank { "IMG" }} • Adăugat ${item.dateAdded}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Row {
                if (item.filePath != null) {
                    IconButton(onClick = { LibraryStorageManager.shareFile(context, item) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Partajează", tint = Color.White)
                    }
                    IconButton(onClick = { LibraryStorageManager.openFileWithNativeApp(context, item) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Deschide", tint = Color.White)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Închide", tint = Color.White)
                }
            }
        }

        // Full Screen Centered Image Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val modelToLoad: Any? = item.filePath ?: item.mediaUrl
            if (modelToLoad != null) {
                AsyncImage(
                    model = modelToLoad,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color(0xFF222222), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Fără previzualizare URL imagine", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        // Image Metadata & AI Actions Bar
        Surface(
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (item.snippet.isNotBlank()) {
                    Text(text = item.snippet, fontSize = 13.sp, color = Color(0xFFDDDDDD))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Tags
                if (item.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.forEach { tag ->
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF333333)) {
                                Text("#$tag", fontSize = 10.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val copyTarget = item.filePath ?: item.mediaUrl ?: item.title
                            clipboardManager.setText(AnnotatedString(copyTarget))
                            isCopied = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCopied) "Copiat!" else "Copiază Path", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val prompt = "Analizează această imagine din librărie (${item.title}): ${item.mediaUrl ?: item.snippet}. Descrie detaliile UI și conținutul vizual."
                            onSendToChat(prompt)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analizează Viziune cu AI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// 3. Interactive PDF & Document Reader View
@Composable
private fun ViewPdfDocumentDetail(
    item: LibraryItem,
    onDismiss: () -> Unit,
    onSendToChat: (String) -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(1) }
    var selectedReaderTab by remember { mutableIntStateOf(0) } // 0 = Mod Pagini Document, 1 = Mod Text Transcript / OCR
    var documentSearchQuery by remember { mutableStateOf("") }
    val totalPages = item.pageCount.coerceAtLeast(1)

    val pagesContent = remember(item) {
        if (item.pdfPagesText.isNotEmpty()) {
            item.pdfPagesText
        } else {
            listOf(
                "PAGINA 1: DOCUMENT ${item.title}\n\n${item.fullContent.ifBlank { item.snippet }}\n\nSpecificații Tehnice și cerințe salvate în memoria dispozitivului.",
                "PAGINA 2: MODULUL DE ARHITECTURĂ ŞI BAZĂ DE DATE\n\nFișierul original (${item.originalFileName.ifBlank { item.title }}) este stocat local.",
                "PAGINA 3: CONCLUZII ŞI GHID DE ACCES\n\nPoate fi deschis oricând cu orice vizualizator nativ instalat pe telefon."
            )
        }
    }

    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Document Top Header Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.category == "PDF-uri" || item.fileExtension == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(text = item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "${item.category} • ${item.fileSize ?: "3.4 MB"} • ${item.fileExtension.uppercase().ifBlank { "DOC" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row {
                        if (item.filePath != null) {
                            IconButton(onClick = { LibraryStorageManager.shareFile(context, item) }, modifier = Modifier.size(30.dp)) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Partajează")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { LibraryStorageManager.openFileWithNativeApp(context, item) }, modifier = Modifier.size(30.dp)) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Deschide fișier")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Închide")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reader Mode Switch Tabs
                TabRow(
                    selectedTabIndex = selectedReaderTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedReaderTab == 0,
                        onClick = { selectedReaderTab = 0 },
                        text = { Text("Mod Document", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedReaderTab == 1,
                        onClick = { selectedReaderTab = 1 },
                        text = { Text("Text Extras (OCR)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Reader Body
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (selectedReaderTab == 0) {
                // Paginated PDF Viewer Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pagina $currentPage din $totalPages",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior", modifier = Modifier.size(16.dp))
                        }

                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Următor", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Page Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(totalPages) { pageIdx ->
                        val pageNum = pageIdx + 1
                        FilterChip(
                            selected = currentPage == pageNum,
                            onClick = { currentPage = pageNum },
                            label = { Text("P. $pageNum", fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rendered PDF Page Document Canvas
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (item.filePath != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { LibraryStorageManager.openFileWithNativeApp(context, item) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Apasă aici pentru a deschide în aplicația nativă", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Text(
                            text = pagesContent.getOrElse(currentPage - 1) { pagesContent[0] },
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // OCR Full Transcript View with Search
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.FindInPage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = documentSearchQuery,
                            onValueChange = { documentSearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (documentSearchQuery.isEmpty()) Text("Caută în textul documentului...", fontSize = 12.sp, color = Color.Gray)
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val fullTranscript = pagesContent.joinToString("\n\n---\n\n")
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullTranscript,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFD4D4D4),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val activeText = pagesContent.getOrElse(currentPage - 1) { pagesContent[0] }
                        clipboardManager.setText(AnnotatedString(activeText))
                        isCopied = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCopied) "Copiat!" else "Copiază Pagina", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val prompt = "Efectuează un rezumat detaliat și extrage ideile cheie din fișierul PDF '${item.title}':\n\n${pagesContent.joinToString("\n")}"
                        onSendToChat(prompt)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generază Rezumat AI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Generic Fallback Detail Viewer
@Composable
private fun ViewGenericItemDetail(
    item: LibraryItem,
    onDismiss: () -> Unit,
    onSendToChat: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.iconEmoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (item.fileSize > 0) {
                        Text("${item.fileSize} • ${item.category}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            Row {
                if (item.filePath != null) {
                    IconButton(onClick = { LibraryStorageManager.shareFile(context, item) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Partajează")
                    }
                    IconButton(onClick = { LibraryStorageManager.openFileWithNativeApp(context, item) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Deschide fișier")
                    }
                }
                IconButton(onClick = onDismiss) { Icon(imageVector = Icons.Default.Close, contentDescription = "Închide") }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (item.filePath != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { LibraryStorageManager.openFileWithNativeApp(context, item) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deschide fișierul original (${item.fileExtension.uppercase().ifBlank { "FIȘIER" }})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text(
                    text = item.fullContent.ifBlank { item.snippet },
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { onSendToChat(item.fullContent.ifBlank { item.snippet }) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Trimite în Chat", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// -----------------------------------------------------------------------------
// NOTION WORKSPACE SIDEBAR / TREE VIEW COMPOSABLES (MATCHING NOTION SCREENSHOT)
// -----------------------------------------------------------------------------
@Composable
private fun NotionWorkspaceTreeView(
    libraryItems: List<LibraryItem>,
    searchQuery: String,
    expandedFolderIds: Set<String>,
    onToggleFolder: (String) -> Unit,
    onView: (LibraryItem) -> Unit,
    onEdit: (LibraryItem) -> Unit,
    onDelete: (String) -> Unit,
    onToggleFavorite: (LibraryItem) -> Unit,
    onCreateChildPage: (LibraryItem) -> Unit,
    onCreateSectionPage: (String) -> Unit,
    onSendToChat: (String) -> Unit,
    templatesSection: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val filteredItems = remember(libraryItems, searchQuery) {
        if (searchQuery.isBlank()) {
            libraryItems
        } else {
            libraryItems.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.snippet.contains(searchQuery, ignoreCase = true) ||
                it.fullContent.contains(searchQuery, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    val recents = remember(filteredItems) {
        val marked = filteredItems.filter { it.isRecent }
        if (marked.isNotEmpty()) marked else filteredItems
    }
    val favorites = remember(filteredItems) {
        filteredItems.filter { (it.isFavorite || it.section.equals("Favorite", ignoreCase = true)) && it.parentId == null }
    }
    val privateItems = remember(filteredItems) {
        filteredItems.filter { !(it.isFavorite || it.section.equals("Favorite", ignoreCase = true)) && it.parentId == null }
    }

    if (libraryItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Nicio pagină în Notițe",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Apasă pe butonul de mai jos pentru a crea prima ta pagină Notion.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onCreateSectionPage("Privat") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Creează Pagină Nouă", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Recents Square Cards Section (1:1 aspect ratio square cards matching Notion)
            if (recents.isNotEmpty() && searchQuery.isBlank()) {
                item(key = "recents_square_row_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "Recents",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(recents, key = { index, item -> "recent_sq_${item.id}_$index" }) { index, recentItem ->
                                Surface(
                                    onClick = { onEdit(recentItem) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E1E22),
                                    border = BorderStroke(1.dp, Color(0xFF33333C)),
                                    modifier = Modifier.size(104.dp) // 1:1 Square
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // 1. Top Half: Exact Cover (Image or Gradient)
                                        val customCover = recentItem.customCoverUri?.takeIf { it.isNotBlank() }
                                            ?: recentItem.mediaUrl?.takeIf { it.isNotBlank() }
                                        if (customCover != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                            ) {
                                                AsyncImage(
                                                    model = customCover,
                                                    contentDescription = "Copertă ${recentItem.title}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        } else {
                                            val coverGrad = notionCoverGradients.getOrElse(recentItem.coverGradientIndex % notionCoverGradients.size) { notionCoverGradients[0] }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .background(Brush.horizontalGradient(coverGrad))
                                            )
                                        }

                                        // 2. Bottom Half: Normal Page Title
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .align(Alignment.BottomCenter)
                                                .background(Color(0xFF1E1E22))
                                                .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
                                            contentAlignment = Alignment.TopStart
                                        ) {
                                            Text(
                                                text = recentItem.title.ifBlank { "Fără titlu" },
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 14.sp
                                            )
                                        }

                                        // 3. Middle-Left: Page Icon (Icon or Emoji)
                                        Box(
                                            modifier = Modifier
                                                .offset(x = 8.dp, y = 34.dp)
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1E1E22))
                                                .border(1.dp, Color(0xFF353540), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            NotionPageIconDisplay(
                                                iconString = recentItem.iconEmoji.ifBlank { "icon:Description" },
                                                size = 20.dp,
                                                iconSize = 15.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 1. Favorite Section
            item(key = "favourites_header") {
                Spacer(modifier = Modifier.height(10.dp))
                NotionTreeSectionHeader(
                    title = "Favorite",
                    onAdd = { onCreateSectionPage("Favorite") }
                )
            }

            if (favorites.isEmpty() && searchQuery.isBlank()) {
                item(key = "favourites_empty_hint") {
                    Text(
                        text = "Nicio pagină favorită. Adaugă pagini la favorite din meniul (•••).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 8.dp)
                    )
                }
            } else {
                itemsIndexed(favorites, key = { index, item -> "fav_${item.id}_$index" }) { index, item ->
                    NotionTreeItemHierarchy(
                        item = item,
                        allItems = libraryItems,
                        level = 0,
                        expandedFolderIds = expandedFolderIds,
                        onToggleFolder = onToggleFolder,
                        onView = onView,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onToggleFavorite = onToggleFavorite,
                        onCreateChildPage = onCreateChildPage,
                        onSendToChat = onSendToChat
                    )
                }
            }

            // 2. Private Section (All non-favorite pages, whether user or agent created)
            item(key = "private_header") {
                Spacer(modifier = Modifier.height(14.dp))
                NotionTreeSectionHeader(
                    title = "Private",
                    onAdd = { onCreateSectionPage("Privat") }
                )
            }

            if (privateItems.isEmpty() && searchQuery.isBlank()) {
                item(key = "private_empty_hint") {
                    Text(
                        text = "Nicio pagină privată. Apasă pe (+) pentru a crea o pagină.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 8.dp)
                    )
                }
            } else {
                itemsIndexed(privateItems, key = { index, item -> "priv_${item.id}_$index" }) { index, item ->
                    NotionTreeItemHierarchy(
                        item = item,
                        allItems = libraryItems,
                        level = 0,
                        expandedFolderIds = expandedFolderIds,
                        onToggleFolder = onToggleFolder,
                        onView = onView,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onToggleFavorite = onToggleFavorite,
                        onCreateChildPage = onCreateChildPage,
                        onSendToChat = onSendToChat
                    )
                }
            }

            // 3. Templates: the last section, opened downward, one row per template like any page.
            if (templatesSection != null && searchQuery.isBlank()) {
                item(key = "templates_section") {
                    Spacer(modifier = Modifier.height(14.dp))
                    templatesSection()
                }
            }
        }
    }
}

@Composable
private fun NotionTreeSectionHeader(
    title: String,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            onClick = onAdd,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adaugă pagină în $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NotionTreeItemHierarchy(
    item: LibraryItem,
    allItems: List<LibraryItem>,
    level: Int,
    expandedFolderIds: Set<String>,
    onToggleFolder: (String) -> Unit,
    onView: (LibraryItem) -> Unit,
    onEdit: (LibraryItem) -> Unit,
    onDelete: (String) -> Unit,
    onToggleFavorite: (LibraryItem) -> Unit,
    onCreateChildPage: (LibraryItem) -> Unit,
    onSendToChat: (String) -> Unit
) {
    // 1. Direct children from allItems where parentId == item.id
    val directChildren = remember(allItems, item.id) {
        allItems.filter { it.parentId == item.id }
    }

    // 2. Embedded subpages from item.blocks with type == PAGE
    val embeddedSubPages = remember(item.blocks, item.id) {
        item.blocks.filter { it.type == NotionBlockType.PAGE }.map { subBlock ->
            val subId = subBlock.subPageId ?: ("subpage_${item.id}_${subBlock.id}")
            LibraryItem(
                id = subId,
                title = subBlock.content.ifBlank { "Pagină fără titlu" },
                category = "Notițe",
                snippet = subBlock.subPageBlocks.firstOrNull { it.content.isNotBlank() }?.content ?: "Sub-pagină",
                fullContent = subBlock.subPageBlocks.joinToString("\n") { it.content },
                dateAdded = item.dateAdded,
                iconEmoji = subBlock.subPageIcon.ifBlank { "icon:Description" },
                coverGradientIndex = subBlock.subPageCoverGradientIndex,
                mediaUrl = subBlock.subPageCustomCoverUri,
                tags = subBlock.subPageTags,
                blocks = if (subBlock.subPageBlocks.isNotEmpty()) subBlock.subPageBlocks else listOf(NotionBlock(type = NotionBlockType.PARAGRAPH, content = "")),
                parentId = item.id,
                section = item.section
            )
        }
    }

    // Combine avoiding duplicate IDs
    val allChildren = remember(directChildren, embeddedSubPages) {
        val directIds = directChildren.map { it.id }.toSet()
        directChildren + embeddedSubPages.filter { it.id !in directIds }
    }
    val hasChildren = allChildren.isNotEmpty()
    val isExpanded = expandedFolderIds.contains(item.id)
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 18).dp)
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onEdit(item) }
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand / Collapse Chevron (Vârful din stânga paginii)
            IconButton(
                onClick = {
                    if (hasChildren) {
                        onToggleFolder(item.id)
                    } else {
                        onCreateChildPage(item)
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = when {
                        hasChildren && isExpanded -> Icons.Default.KeyboardArrowDown
                        hasChildren && !isExpanded -> Icons.Default.ChevronRight
                        else -> Icons.Default.ChevronRight
                    },
                    contentDescription = if (isExpanded) "Restrânge paginile interioare" else "Deschide paginile interioare",
                    tint = if (hasChildren) {
                        if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Notion Page Icon (Vector Icon or Emoji)
            NotionPageIconDisplay(
                iconString = item.iconEmoji,
                size = 20.dp,
                iconSize = 16.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Page Title
            Text(
                text = item.title.ifBlank { "Fără titlu" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Menu button (...)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Opțiuni",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editează", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onEdit(item)
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Adaugă Sub-pagină", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onCreateChildPage(item)
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Trimite în Chat", fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            val content = item.title + "\n" + item.fullContent
                            onSendToChat(content)
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    val isFav = item.isFavorite || item.section.equals("Favorite", ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isFav) "Elimină din Favorite" else "Adaugă la Favorite",
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite(item)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null,
                                tint = if (isFav) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Șterge", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete(item.id)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Quick Add Sub-Page Button (+)
            IconButton(
                onClick = { onCreateChildPage(item) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adaugă sub-pagină",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Render children downwards if expanded (deschide în jos lista cu toate paginile din interior)
        AnimatedVisibility(
            visible = hasChildren && isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            ) {
                allChildren.forEach { child ->
                    NotionTreeItemHierarchy(
                        item = child,
                        allItems = allItems,
                        level = level + 1,
                        expandedFolderIds = expandedFolderIds,
                        onToggleFolder = onToggleFolder,
                        onView = onView,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onToggleFavorite = onToggleFavorite,
                        onCreateChildPage = onCreateChildPage,
                        onSendToChat = onSendToChat
                    )
                }
            }
        }
    }
}

@Composable
fun TrashScreen(
    trashSessions: List<ChatSession>,
    onRestoreChat: (String) -> Unit,
    onPermanentDelete: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Coș de gunoi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (trashSessions.isNotEmpty()) {
                TextButton(onClick = onEmptyTrash) {
                    Text("Golește coșul", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (trashSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Coșul de gunoi este gol",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trashSessions) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = { onRestoreChat(session.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restaurează",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onPermanentDelete(session.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Șterge definitiv",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatGptMessageItem(
    message: ChatMessage,
    spaces: List<SpaceItem> = emptyList(),
    isSpeaking: Boolean = false,
    onSpeakClick: () -> Unit = {},
    onSaveToLibrary: (category: String) -> Unit = {},
    onSaveToSpace: (spaceId: String) -> Unit = {},
    onBranchChat: (ChatMessage) -> Unit = {},
    onRegenerate: (ChatMessage) -> Unit = {},
    onEditUserMessage: (messageId: String, newText: String) -> Unit = { _, _ -> }
) {
    val isUser = message.sender == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return String.format(java.util.Locale.US, "%.1f %s", size, units[unitIndex])
}
