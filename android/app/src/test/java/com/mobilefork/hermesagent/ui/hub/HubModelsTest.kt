package com.mobilefork.hermesagent.ui.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HubModelsTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun theBarStartsWithChatConversationsShortcutsAndLibrary() {
        assertEquals(
            listOf(DockItem.Chat, DockItem.Conversations, DockItem.Shortcuts, DockItem.Library),
            DockStore(context).load(),
        )
    }

    @Test
    fun anEditedBarIsKeptInOrderAndStaysBetweenTwoAndFiveButtons() {
        val store = DockStore(context)
        store.save(listOf(DockItem.Calendar, DockItem.Chat, DockItem.Projects))
        assertEquals(listOf(DockItem.Calendar, DockItem.Chat, DockItem.Projects), store.load())
        store.save(DockItem.entries)
        assertEquals(DockItem.MAX, store.load().size)
        store.save(listOf(DockItem.Chat))
        assertEquals("one button is too few, so the usual bar returns", DockItem.Default, store.load())
    }

    @Test
    fun aTemplateThatIsOffOrRemovedKeepsItsPageOutOfTheBar() {
        val store = LibraryTemplateStore(context)
        assertTrue(DockItem.Ideas.isAvailable(store.load()))
        store.save(LibraryTemplate.Ideas, TemplateState(added = true, active = false))
        assertFalse(DockItem.Ideas.isAvailable(store.load()))
        assertTrue(DockItem.Goals.isAvailable(store.load()))
        store.save(LibraryTemplate.Goals, TemplateState(added = false, active = true))
        assertFalse(DockItem.Goals.isAvailable(store.load()))
        assertTrue("pages that belong to no template are always available", DockItem.Calendar.isAvailable(store.load()))
    }

    @Test
    fun shortcutsStartWithTheMainPagesAndKeepTheirOrder() {
        val store = ShortcutStore(context)
        val first = store.load()
        assertEquals(listOf("calendar", "library", "projects"), first.map { it.target })
        assertEquals("the first look is saved, so it stays the same", first.map { it.id }, store.load().map { it.id })
        val extra = Shortcut("x", ShortcutKind.App, "com.whatsapp", "WhatsApp")
        store.save(listOf(extra) + first)
        assertEquals("x", store.load().first().id)
        assertEquals(ShortcutKind.App, store.load().first().kind)
        assertEquals("WhatsApp", store.load().first().label)
    }
}
