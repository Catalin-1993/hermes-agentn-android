package com.example.ui.screens.workspace

import androidx.compose.foundation.BorderStroke
import com.example.ui.components.dock.dockBottomGradientFade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.mobilefork.hermesagent.R
import com.example.ui.components.avatar.AvatarProfile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import com.example.ui.components.UserAvatar

private val CardBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color.White else Color(0xFF101613)
private val ItemBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFF1F5F9) else Color(0xFF141B18)
private val InputBackground: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFF8FAFC) else Color(0xFF0C110E)
private val SubtleBorderColor: Color
    @Composable get() = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0x1A000000) else Color(0x18FFFFFF)
private val EmeraldBorderColor = Color(0x3310B981)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceInstructionsScreen(
    formEmoji: String,
    onEmojiChange: (String) -> Unit,
    formAvatarUri: String?,
    onAvatarUriChange: (String?) -> Unit,
    formName: String,
    onNameChange: (String) -> Unit,
    formRole: String,
    onRoleChange: (String) -> Unit,
    formPurpose: String,
    onPurposeChange: (String) -> Unit,
    formInstructions: String,
    onInstructionsChange: (String) -> Unit,
    formPreferredModel: String,
    onPreferredModelChange: (String) -> Unit,
    formReasoning: String,
    onReasoningChange: (String) -> Unit,
    formAutonomy: String,
    onAutonomyChange: (String) -> Unit,
    formTags: List<String> = listOf("Agent", "Workspace"),
    onTagsChange: (List<String>) -> Unit = {},
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }
    var isAddingTag by remember { mutableStateOf(false) }

    val quickEmojis = listOf("⚡", "🤖", "💻", "🧠", "🔬", "📈", "🎨", "🛠️", "🛡️", "🚀", "📚", "💡")
    val roleSuggestions = listOf(
        "Senior Full-Stack Architect",
        "AI Research & Document Analyst",
        "Strategic Business Advisor",
        "SecOps & Vulnerability Analyst"
    )

    val models = listOf(
        Triple("Hermes 3 (Nous Research)", "Nous Research • Open-Weights Agentic", "Recomandat"),
        Triple("Gemini 2.5 Flash", "Google DeepMind • Viteză & Multimodal", "Ultra Rapid"),
        Triple("Gemini 1.5 Pro", "Google DeepMind • Context Masiv & Analiză", "Deep Analysis"),
        Triple("Claude 3.5 Sonnet", "Anthropic • Excelență în Cod & Logică", "Cod & Logică"),
        Triple("GPT-4o", "OpenAI • Model Multimodal Echilibrat", "Generalist"),
        Triple("DeepSeek Coder", "DeepSeek • Specializat Algoritmi & Refactoring", "Programare")
    )

    val quickDirectives = listOf(
        "⚡ Răspunsuri concise, directe și fără divagații.",
        "🛡️ Validează erorile și folosește Defensive Coding.",
        "🧹 Scrie cod modular, curat și fără cod mort.",
        "🔍 Explică pas cu pas raționamentul arhitectural.",
        "🇷🇴 Răspunde întotdeauna în limba română tehnică."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .dockBottomGradientFade(fadeHeight = 185.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            // Header Hero Preview
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, EmeraldBorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (formAvatarUri != null) {
                                UserAvatar(
                                    userName = formName.ifBlank { "Agent" },
                                    avatarUri = formAvatarUri,
                                    size = 52.dp,
                                    borderWidth = 1.5.dp,
                                    borderColor = EmeraldPrimary,
                                    isClickable = true,
                                    showEditBadge = true,
                                    onAvatarSelected = onAvatarUriChange
                                )
                            } else {
                                UserAvatar(
                                    userName = formName.ifBlank { "Agent" },
                                    avatarUri = "img_easy_avatar",
                                    size = 52.dp,
                                    borderWidth = 1.5.dp,
                                    borderColor = EmeraldPrimary,
                                    isClickable = true,
                                    showEditBadge = true,
                                    onAvatarSelected = onAvatarUriChange
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formName.ifBlank { "Agent Fără Nume" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formRole.ifBlank { "Asistent Specializat" },
                                    fontSize = 12.sp,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${formPreferredModel.split(" ").first()} • Mod $formReasoning • Autonomie $formAutonomy",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // --- PREMIUM PORTRAIT AVATAR SELECTOR (FEȚE REALE) ---
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Portret Profesional Agent (Față Reală):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val avatars = listOf(
                                    Triple("avatar_lawyer", "Avocat", "Legal Counsel"),
                                    Triple("avatar_financial", "Consultant Financiar", "Financial Advisor"),
                                    Triple("avatar_doctor", "Medic", "Sănătate & Prevenție"),
                                    Triple("avatar_psychologist", "Psiholog", "Echilibru & Wellness"),
                                    Triple("avatar_chef", "Bucătar", "Chef & Gastronomie"),
                                    Triple("avatar_fox", "The Fox", "Strategic & Creative"),
                                    Triple("avatar_panda", "The Panda", "Collaboration & Management"),
                                    Triple("avatar_owl", "The Owl", "Knowledge & Guidance"),
                                    Triple("avatar_daniel_kim", "Daniel Kim", "Sentiment Analyst"),
                                    Triple("avatar_sofia_reyes", "Sofia Reyes", "Macro Analyst"),
                                    Triple("avatar_marcus_liu", "Marcus Liu", "Technical Analyst"),
                                    Triple("avatar_emma_hart", "Emma Hart", "Data Scientist"),
                                    Triple("avatar_alex_chen", "Alex Chen", "Web Developer"),
                                    Triple("avatar_leo_vance", "Leo Vance", "Creative Designer"),
                                    Triple("avatar_elena_pop", "Elena Pop", "Content Editor"),
                                    Triple("img_easy_avatar", "Default", "Universal")
                                )
                                items(avatars) { (id, name, desc) ->
                                    val isSelected = formAvatarUri == id || (id == "img_easy_avatar" && formAvatarUri == null)
                                    val matched = AvatarProfile.ALL_AVATARS.find { it.id == id || it.id.replace("avatar_", "") == id.replace("avatar_", "") }
                                    val drawableId = matched?.drawableRes ?: when (id) {
                                        "avatar_daniel_kim" -> R.drawable.avatar_daniel_kim
                                        "avatar_sofia_reyes" -> R.drawable.avatar_sofia_reyes
                                        "avatar_marcus_liu" -> R.drawable.avatar_marcus_liu
                                        "avatar_emma_hart" -> R.drawable.avatar_emma_hart
                                        "avatar_alex_chen" -> R.drawable.avatar_alex_chen
                                        "avatar_olivia_smith" -> R.drawable.img_avatar_financial
                                        "avatar_leo_vance" -> R.drawable.avatar_leo_vance
                                        "avatar_clara_vance" -> R.drawable.img_avatar_lawyer
                                        "avatar_elena_pop" -> R.drawable.avatar_elena_pop
                                        else -> R.drawable.img_easy_avatar
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) (if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF132B20)) else ItemBackground
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) EmeraldPrimary else SubtleBorderColor
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .width(72.dp)
                                            .clickable {
                                                if (id == "img_easy_avatar") {
                                                    onAvatarUriChange(null)
                                                } else {
                                                    onAvatarUriChange(id)
                                                    // Auto-populate blank/default fields
                                                    if (formName.isBlank() || formName == "New Agent" || formName == "Workspace Nou") {
                                                        onNameChange(name)
                                                    }
                                                    if (formRole.isBlank()) {
                                                        onRoleChange(desc)
                                                    }
                                                }
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = drawableId),
                                                contentDescription = name,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.dp,
                                                        color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = name.split(" ").first(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) TextPrimary else TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = desc.split(" ").first(),
                                                fontSize = 8.sp,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Curated Emoji Picker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Alege Pictogramă Rapidă (Emoji):", fontSize = 11.sp, color = TextMuted)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickEmojis) { emo ->
                                    val isSelected = formEmoji == emo
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF1E3A2E) else ItemBackground)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) EmeraldPrimary else SubtleBorderColor,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onEmojiChange(emo) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emo, fontSize = 17.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 1: Identitate & Misiune
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            Text("Identitate & Misiune", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = formEmoji,
                                onValueChange = { if (it.length <= 2) onEmojiChange(it) },
                                label = { Text("Emoji", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.width(76.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = SubtleBorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground
                                )
                            )

                            OutlinedTextField(
                                value = formName,
                                onValueChange = onNameChange,
                                label = { Text("Nume Workspace / Agent", fontSize = 11.sp) },
                                placeholder = { Text("ex. Arhitectură Kotlin & Android", color = TextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = SubtleBorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = InputBackground,
                                    unfocusedContainerColor = InputBackground
                                )
                            )
                        }

                        OutlinedTextField(
                            value = formRole,
                            onValueChange = onRoleChange,
                            label = { Text("Rol Profesional / Persona", fontSize = 11.sp) },
                            placeholder = { Text("ex. Senior Full-Stack Architect", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )

                        // Role Suggestions
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(roleSuggestions) { sugg ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ItemBackground,
                                    border = BorderStroke(0.8.dp, SubtleBorderColor),
                                    modifier = Modifier.clickable { onRoleChange(sugg) }
                                ) {
                                    Text(
                                        text = sugg,
                                        fontSize = 10.5.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = formPurpose,
                            onValueChange = onPurposeChange,
                            label = { Text("Scop & Obiective Specializate", fontSize = 11.sp) },
                            placeholder = { Text("Descrie pe scurt ce realizează și pe ce se axează agentul...", color = TextMuted, fontSize = 12.sp) },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )

                        // Tags Management
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Etichete & Domenii:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                if (!isAddingTag) {
                                    Text(
                                        "+ Adaugă Tag",
                                        fontSize = 11.sp,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { isAddingTag = true }
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                formTags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (com.example.ui.theme.LocalHermesTheme.current.isLight) Color(0xFFECFDF5) else Color(0xFF16251E),
                                        border = BorderStroke(0.8.dp, Color(0x3310B981))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("#$tag", fontSize = 11.sp, color = EmeraldAccent)
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Șterge tag",
                                                tint = TextMuted,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable {
                                                        onTagsChange(formTags.filter { it != tag })
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAddingTag) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newTagInput,
                                        onValueChange = { newTagInput = it },
                                        placeholder = { Text("ex. Android, Securitate...", fontSize = 11.sp, color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = SubtleBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = InputBackground,
                                            unfocusedContainerColor = InputBackground
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            if (newTagInput.isNotBlank()) {
                                                val clean = newTagInput.trim().replace("#", "")
                                                if (!formTags.contains(clean)) {
                                                    onTagsChange(formTags + clean)
                                                }
                                                newTagInput = ""
                                                isAddingTag = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Adaugă", fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { isAddingTag = false; newTagInput = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Anulează", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 2: Creier AI & Model de Bază
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            Text("Model Cognitiv Principal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }
                        Text("Selectează arhitectura neuronală cu care acest agent procesează instrucțiunile:", fontSize = 11.5.sp, color = TextMuted)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            models.forEach { (modelName, desc, badge) ->
                                val isSelected = formPreferredModel.contains(modelName.split(" ").first())
                                val isLightTheme = com.example.ui.theme.LocalHermesTheme.current.isLight
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) (if (isLightTheme) Color(0xFFECFDF5) else Color(0xFF13281E)) else ItemBackground,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) EmeraldPrimary else SubtleBorderColor
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPreferredModelChange(modelName) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(modelName, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = if (isSelected) TextPrimary else TextSecondary)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.25f) else (if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF222B26)))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(badge, fontSize = 9.sp, color = if (isSelected) EmeraldAccent else TextMuted, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(desc, fontSize = 10.5.sp, color = TextMuted)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, if (isSelected) EmeraldPrimary else SubtleBorderColor, CircleShape)
                                                .background(if (isSelected) EmeraldPrimary else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Secțiunea 3: Directivă de Sistem (System Prompt)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                Text("Instrucțiuni de Sistem (System Prompt)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Text("${formInstructions.length} caractere", fontSize = 10.5.sp, color = TextMuted)
                        }

                        Text("Aceste directive guvernează gândirea, prioritățile și constrângerile agentului.", fontSize = 11.5.sp, color = TextMuted)

                        // Quick Directive Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Adaugă Directivă Rapidă:", fontSize = 10.5.sp, color = TextMuted)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(quickDirectives) { directive ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ItemBackground,
                                        border = BorderStroke(0.8.dp, SubtleBorderColor),
                                        modifier = Modifier.clickable {
                                            val cleanDir = directive.substringAfter(" ").trim()
                                            if (!formInstructions.contains(cleanDir)) {
                                                val updated = if (formInstructions.isBlank()) cleanDir else "$formInstructions\n- $cleanDir"
                                                onInstructionsChange(updated)
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "+ ${directive.take(28)}...",
                                            fontSize = 10.5.sp,
                                            color = EmeraldAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = formInstructions,
                            onValueChange = onInstructionsChange,
                            placeholder = {
                                Text(
                                    "Definește comportamentul strict al agentului:\n- Răspunde concis și structurat\n- Nu introduce cod mort\n- Validează întotdeauna intrările utilizatorului...",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            },
                            minLines = 6,
                            maxLines = 14,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = SubtleBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground
                            )
                        )
                    }
                }
            }

            // Secțiunea 4: Raționament & Nivel Autonomie
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, SubtleBorderColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                            Text("Mod Raționament & Politică Autonomie", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }

                        // Raționament (Thinking Mode)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Efort Cognitiv (Thinking Budget):", fontSize = 11.5.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("Rapid", "Latență minimă, răspuns direct", Icons.Default.Speed),
                                    Triple("Echilibrat", "Verificare analitică optimă", Icons.Default.Tune),
                                    Triple("Profund", "Thinking extins pas-cu-pas", Icons.Default.Psychology)
                                ).forEach { (mode, explanation, icon) ->
                                    val isSel = formReasoning.equals(mode, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF162B21) else ItemBackground,
                                        border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) EmeraldPrimary else SubtleBorderColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onReasoningChange(mode) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(icon, contentDescription = null, tint = if (isSel) EmeraldAccent else TextMuted, modifier = Modifier.size(16.dp))
                                            Text(mode, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isSel) TextPrimary else TextSecondary)
                                            Text(explanation, fontSize = 9.sp, color = TextMuted, maxLines = 2, lineHeight = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = SubtleBorderColor, thickness = 0.8.dp)

                        // Nivel de Autonomie
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Nivel Permisiune & Autonomie:", fontSize = 11.5.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("Cautious", "Precaut", "Cere confirmare la orice acțiune de fișiere/sistem"),
                                    Triple("Balanced", "Echilibrat", "Execută unelte sigure; cere aprobare pentru comenzi critice"),
                                    Triple("Autonomous", "Autonom", "Execuție completă și auto-corectare fără întreruperi")
                                ).forEach { (key, label, desc) ->
                                    val isSel = formAutonomy.equals(key, ignoreCase = true) || formAutonomy.equals(label, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFF162B21) else ItemBackground,
                                        border = BorderStroke(if (isSel) 1.5.dp else 1.dp, if (isSel) EmeraldPrimary else SubtleBorderColor),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onAutonomyChange(key) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isSel) TextPrimary else TextSecondary)
                                            Text(desc, fontSize = 9.sp, color = TextMuted, maxLines = 3, lineHeight = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 86.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondary)
            ) {
                Text("Înapoi", fontSize = 13.5.sp)
            }

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salvează Modificările", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}
