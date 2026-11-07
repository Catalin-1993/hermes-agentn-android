package com.mobilefork.hermesagent.data

import androidx.annotation.DrawableRes
import com.mobilefork.hermesagent.R

/** The eight drawn characters an agent can wear. Their ids are stored, so they never change. */
enum class AvatarPreset(val id: String, @DrawableRes val drawable: Int, val displayName: String) {
    Bit("bit", R.drawable.avatar_preset_bit, "Bit"),
    Miri("miri", R.drawable.avatar_preset_miri, "Miri"),
    Lumi("lumi", R.drawable.avatar_preset_lumi, "Lumi"),
    Nova("nova", R.drawable.avatar_preset_nova, "Nova"),
    Sage("sage", R.drawable.avatar_preset_sage, "Sage"),
    Pip("pip", R.drawable.avatar_preset_pip, "Pip"),
    Bolt("bolt", R.drawable.avatar_preset_bolt, "Bolt"),
    Teddy("teddy", R.drawable.avatar_preset_teddy, "Teddy"),
    ;

    val storedValue: String get() = AVATAR_PRESET_PREFIX + id

    companion object {
        fun fromId(id: String): AvatarPreset? = entries.firstOrNull { it.id == id }
    }
}

/**
 * What an agent's `avatarPath` holds. Only a photo is a file: every other value is a word, and
 * nothing that deletes pictures may ever treat it as a path.
 */
sealed interface AvatarChoice {
    /** The face drawn from the agent's name, or the app's mark for the main agent. */
    data object Generated : AvatarChoice
    data class Preset(val preset: AvatarPreset) : AvatarChoice
    data object Initials : AvatarChoice
    data class Photo(val path: String) : AvatarChoice
}

const val AVATAR_PRESET_PREFIX = "preset:"
const val AVATAR_INITIALS = "initials"

fun avatarChoiceOf(stored: String): AvatarChoice {
    val value = stored.trim()
    return when {
        value.isEmpty() -> AvatarChoice.Generated
        value == AVATAR_INITIALS -> AvatarChoice.Initials
        value.startsWith(AVATAR_PRESET_PREFIX) ->
            AvatarPreset.fromId(value.removePrefix(AVATAR_PRESET_PREFIX))?.let(AvatarChoice::Preset) ?: AvatarChoice.Generated
        isAvatarFile(value) -> AvatarChoice.Photo(value)
        else -> AvatarChoice.Generated
    }
}

/** Pictures are saved under the app's own storage, always as an absolute path. */
fun isAvatarFile(stored: String): Boolean = stored.startsWith("/")

/** One or two letters for the initials avatar: "Ana Pop" is AP, "Nova" is N. */
fun avatarInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1)
        else -> words[0].take(1) + words[1].take(1)
    }
    return letters.uppercase()
}
