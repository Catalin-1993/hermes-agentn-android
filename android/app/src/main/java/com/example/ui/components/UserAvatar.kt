package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobilefork.hermesagent.R
import com.example.ui.components.avatar.AvatarProfile
import com.example.ui.theme.AvatarVGBg
import com.example.ui.theme.EmeraldAccent
import java.io.File
import java.io.FileOutputStream

@Composable
fun UserAvatar(
    userName: String,
    avatarUri: String? = null,
    size: Dp = 48.dp,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = Color(0x6610B981),
    isClickable: Boolean = false,
    showEditBadge: Boolean = false,
    onAvatarSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && onAvatarSelected != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val avatarFile = File(context.filesDir, "user_avatar_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(avatarFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                onAvatarSelected(avatarFile.absolutePath)
            } catch (_: Exception) {
                onAvatarSelected(uri.toString())
            }
        }
    }

    val initials = userName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifEmpty { "VG" }

    val clickModifier = if (onAvatarSelected != null) {
        Modifier.clickable { imagePickerLauncher.launch("image/*") }
    } else Modifier

    val matchedPreset = if (!avatarUri.isNullOrBlank()) {
        AvatarProfile.ALL_AVATARS.find {
            it.id == avatarUri ||
                it.id.replace("avatar_", "") == avatarUri.replace("avatar_", "").replace("img_mascot_", "")
        }
    } else null

    val isCustomFileOrUrl = !avatarUri.isNullOrBlank() &&
        matchedPreset == null &&
        (avatarUri.startsWith("/") || avatarUri.startsWith("content://") || avatarUri.startsWith("file://") || avatarUri.startsWith("http"))

    val effectiveProfile = matchedPreset ?: if (!avatarUri.isNullOrBlank()) AvatarProfile.getById(avatarUri) else null
    val isCutoutMascot = effectiveProfile?.isMascot == true

    Box(
        modifier = modifier
            .size(size)
            .then(if (isCutoutMascot) Modifier else Modifier.clip(CircleShape))
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (effectiveProfile != null && !isCustomFileOrUrl) {
            Image(
                painter = painterResource(id = effectiveProfile.drawableRes),
                contentDescription = "Avatar $userName",
                modifier = if (isCutoutMascot) {
                    Modifier.size(size)
                } else {
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .border(borderWidth, borderColor, CircleShape)
                },
                contentScale = if (effectiveProfile.isRealPerson) ContentScale.Crop else ContentScale.Fit
            )
        } else if (isCustomFileOrUrl) {
            val resolvedData: Any = if (avatarUri!!.startsWith("/")) File(avatarUri) else avatarUri
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedData)
                    .crossfade(true)
                    .error(R.drawable.img_avatar_fox)
                    .fallback(R.drawable.img_avatar_fox)
                    .build(),
                contentDescription = "Avatar $userName",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(borderWidth, borderColor, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Clean gradient initials fallback
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF065F46))
                        )
                    )
                    .border(borderWidth, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        if (showEditBadge && onAvatarSelected != null) {
            Box(
                modifier = Modifier
                    .size(size * 0.38f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF0C100E))
                    .border(1.dp, EmeraldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Schimbă Avatarul",
                    tint = EmeraldAccent,
                    modifier = Modifier.size(size * 0.22f)
                )
            }
        }
    }
}
