package com.example.ui.components.avatar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkspaceProfile
import com.example.ui.theme.LocalHermesTheme
import com.example.ui.theme.TextPrimary
import com.example.utils.WorkspaceDisplayUtils

/**
 * StandaloneWorkspaceTopHeaderAvatar - Large 3D character cutout avatar
 * displayed at the top of the Workspace Overview and sub-pages.
 * Matches the home page header: larger size (56dp), standalone cutout without any
 * circular border or background disk ("fără nicio bulină"), with a frosted glass
 * name pill underneath.
 */
@Composable
fun StandaloneWorkspaceTopHeaderAvatar(
    workspace: WorkspaceProfile,
    avatarSize: Dp = 62.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val matchedAvatar = remember(workspace) {
        AvatarProfile.findMatchingAvatarForWorkspace(
            name = workspace.name,
            role = workspace.agentRole,
            purpose = workspace.purpose,
            tags = workspace.tags,
            avatarUri = workspace.avatarUri
        ) ?: if (!workspace.avatarUri.isNullOrBlank()) AvatarProfile.getById(workspace.avatarUri) else null
    }

    val displayInfo = remember(workspace) {
        WorkspaceDisplayUtils.getDisplayInfo(workspace)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ws_top_header_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ws_float"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.offset(y = floatOffset.dp)
    ) {
        // Pure 3D cutout character, NO background, NO circular disc/bulină - matches Home screen!
        if (matchedAvatar != null) {
            InteractiveAvatarView(
                avatarProfile = matchedAvatar,
                activity = AvatarActivity.Idle,
                size = avatarSize,
                showContactShadow = false,
                showSpeechBubble = false
            )
        } else if (!workspace.avatarUri.isNullOrBlank()) {
            val uriStr = workspace.avatarUri.orEmpty()
            val resolvedData: Any = when (uriStr) {
                "avatar_coding", "avatar_marcus_liu" -> com.mobilefork.hermesagent.R.drawable.img_avatar_coding
                "avatar_design", "avatar_leo_vance" -> com.mobilefork.hermesagent.R.drawable.img_avatar_design
                "avatar_writing", "avatar_elena_pop" -> com.mobilefork.hermesagent.R.drawable.img_avatar_writing
                "avatar_data", "avatar_emma_hart" -> com.mobilefork.hermesagent.R.drawable.img_avatar_data
                "avatar_alex_chen", "avatar_panda" -> com.mobilefork.hermesagent.R.drawable.img_avatar_panda
                "avatar_sofia_reyes", "avatar_owl" -> com.mobilefork.hermesagent.R.drawable.img_avatar_owl
                "avatar_daniel_kim", "avatar_doctor" -> com.mobilefork.hermesagent.R.drawable.img_avatar_doctor
                "avatar_olivia_smith", "avatar_financial" -> com.mobilefork.hermesagent.R.drawable.img_avatar_financial
                "avatar_clara_vance", "avatar_lawyer" -> com.mobilefork.hermesagent.R.drawable.img_avatar_lawyer
                "avatar_psychologist" -> com.mobilefork.hermesagent.R.drawable.img_avatar_psychologist
                "avatar_chef" -> com.mobilefork.hermesagent.R.drawable.img_avatar_chef
                "avatar_fox", "avatar_hermes_orchestrator" -> com.mobilefork.hermesagent.R.drawable.img_avatar_fox
                else -> if (uriStr.startsWith("/")) java.io.File(uriStr) else uriStr
            }
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(resolvedData)
                    .crossfade(true)
                    .build(),
                contentDescription = workspace.name,
                modifier = Modifier.size(avatarSize),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = workspace.emoji,
                fontSize = (avatarSize.value * 0.58f).sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Frosted Glass Name Pill (matching Home top bar!)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LocalHermesTheme.current.headerButtonBg,
            border = BorderStroke(1.dp, LocalHermesTheme.current.headerButtonBorder),
            shadowElevation = if (LocalHermesTheme.current.isLight) 6.dp else 3.dp
        ) {
            Text(
                text = displayInfo.profession.ifBlank { workspace.name },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp
                ),
                color = LocalHermesTheme.current.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
    }
}
