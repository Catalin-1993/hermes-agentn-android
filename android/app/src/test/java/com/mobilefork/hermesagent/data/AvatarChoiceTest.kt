package com.mobilefork.hermesagent.data

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AvatarChoiceTest {
    @Test
    fun everyStoredValueReadsBackAsWhatWasChosen() {
        AvatarPreset.entries.forEach { preset ->
            assertEquals(AvatarChoice.Preset(preset), avatarChoiceOf(preset.storedValue))
        }
        assertEquals(AvatarChoice.Initials, avatarChoiceOf(AVATAR_INITIALS))
        assertEquals(AvatarChoice.Generated, avatarChoiceOf(""))
        assertEquals(AvatarChoice.Photo("/data/x/a.png"), avatarChoiceOf("/data/x/a.png"))
        // A character that no longer exists falls back to the automatic look instead of breaking.
        assertEquals(AvatarChoice.Generated, avatarChoiceOf("preset:gone"))
    }

    @Test
    fun onlyPhotosAreFilesSoNothingElseIsEverDeleted() {
        assertTrue(isAvatarFile("/data/user/0/app/files/agent-avatars/a.png"))
        AvatarPreset.entries.forEach { assertFalse(isAvatarFile(it.storedValue)) }
        assertFalse(isAvatarFile(AVATAR_INITIALS))
        assertFalse(isAvatarFile(""))
    }

    @Test
    fun initialsTakeTheFirstLetterOfUpToTwoWords() {
        assertEquals("AP", avatarInitials("ana pop"))
        assertEquals("N", avatarInitials("Nova"))
        assertEquals("ȘT", avatarInitials("Ștefan Tudor Ionescu"))
        assertEquals("?", avatarInitials("  "))
    }

    @Test
    fun everyCharacterDrawsAtChatSize() {
        val context = RuntimeEnvironment.getApplication()
        AvatarPreset.entries.forEach { preset ->
            val drawable = assertNotNull(preset.id, ContextCompat.getDrawable(context, preset.drawable)).let {
                ContextCompat.getDrawable(context, preset.drawable)!!
            }
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 96, 96)
            drawable.draw(Canvas(bitmap))
        }
    }

    @Test
    fun theMainAgentKeepsItsChosenLook() {
        val store = AgentStore(RuntimeEnvironment.getApplication())
        store.saveMainAvatar(AvatarPreset.Nova.storedValue)
        assertEquals(AvatarPreset.Nova.storedValue, store.main().avatarPath)
        assertEquals(AvatarPreset.Nova.storedValue, store.get(AgentProfile.MAIN_ID).avatarPath)
        assertTrue(store.list().first().isMain)
    }
}
