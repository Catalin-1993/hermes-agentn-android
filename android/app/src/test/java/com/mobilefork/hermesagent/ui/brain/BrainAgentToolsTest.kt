package com.mobilefork.hermesagent.ui.brain

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BrainAgentToolsTest {
    private val context = RuntimeEnvironment.getApplication()
    private val tools = BrainAgentTools(context)

    @org.junit.Before
    fun startEmpty() {
        // The test application's storage outlives a test, so each starts from an empty library and calendar.
        BrainDataStore.withLibrary(context) { it.clear() }
        BrainDataStore.withCalendar(context) { it.clear() }
    }

    private fun library(vararg pairs: Pair<String, Any>) = tools.library(JSONObject(mapOf(*pairs)))
    private fun calendar(vararg pairs: Pair<String, Any>) = tools.calendar(JSONObject(mapOf(*pairs)))

    @Test
    fun aNoteCanBeCreatedFoundReadChangedAndDeleted() {
        val created = library("action" to "create", "title" to "Idei lansare", "content" to "# Plan\n- primul pas\nText liber", "tags" to listOf("lansare", "#plan"))
        assertTrue(created.toString(), created.optBoolean("created"))
        val id = created.getString("id")

        val found = library("action" to "list", "query" to "lansare")
        assertEquals(1, found.getInt("count"))
        assertEquals(id, found.getJSONArray("items").getJSONObject(0).getString("id"))

        val read = library("action" to "read", "id" to id)
        assertEquals("# Plan\n- primul pas\nText liber", read.getString("content"))
        assertEquals(listOf("lansare", "plan"), (0 until 2).map { read.getJSONArray("tags").getString(it) })

        val updated = library("action" to "update", "title" to "Idei lansare v2", "content" to "Doar atât", "favorite" to true)
        assertTrue(updated.toString(), updated.has("error"))   // an exact title that no longer exists is refused
        val changed = library("action" to "update", "id" to id, "title" to "Idei lansare v2", "content" to "Doar atât", "favorite" to true)
        assertEquals("Idei lansare v2", changed.getString("title"))
        assertEquals("Favorite", changed.getString("section"))
        assertEquals("Doar atât", library("action" to "read", "title" to "Idei lansare v2").getString("content"))

        val deleted = library("action" to "delete", "id" to id)
        assertEquals(1, deleted.getInt("deleted"))
        assertEquals(0, library("action" to "list", "query" to "lansare").getInt("count"))
    }

    @Test
    fun theLibraryRefusesWhatItCannotDoInsteadOfGuessing() {
        assertTrue(library("action" to "create", "content" to "fără titlu").has("error"))
        assertTrue(library("action" to "read", "id" to "nu-exista").has("error"))
        assertTrue(library("action" to "frobnicate").getString("error").contains("Unknown action"))
        assertTrue(library("action" to "delete").has("error"))
    }

    @Test
    fun twoNotesWithTheSameTitleAreNotConfusedByTitle() {
        library("action" to "create", "title" to "Dublura", "content" to "unu")
        library("action" to "create", "title" to "Dublura", "content" to "doi")
        val answer = library("action" to "read", "title" to "Dublura")
        assertTrue(answer.getString("error").contains("2 items"))
        // Each keeps its own text: the second does not overwrite the first's file.
        val ids = library("action" to "list", "query" to "Dublura").getJSONArray("items").let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }
        assertEquals(setOf("unu", "doi"), ids.map { library("action" to "read", "id" to it).getString("content") }.toSet())
        val files = com.example.data.storage.LibraryStorageManager.loadLibraryIndex(context).map { it.filePath }
        assertEquals(2, files.toSet().size)
    }

    @Test
    fun deletingANoteRemovesItsFileButNeverAFileOutsideTheApp() {
        val created = library("action" to "create", "title" to "Cu fișier", "content" to "text")
        val file = java.io.File(
            com.example.data.storage.LibraryStorageManager.loadLibraryIndex(context).first { it.id == created.getString("id") }.filePath!!,
        )
        assertTrue(file.isFile)
        library("action" to "delete", "id" to created.getString("id"))
        assertFalse(file.exists())

        val outside = java.io.File.createTempFile("not-the-apps", ".txt").apply { writeText("keep") }
        val item = com.example.data.model.LibraryItem(id = "l-out", title = "Extern", filePath = outside.absolutePath)
        BrainDataStore.withLibrary(context) { it.add(item) }
        library("action" to "delete", "id" to "l-out")
        assertTrue("a file outside the app's storage must stay", outside.exists())
        outside.delete()
    }

    @Test
    fun anEventCanBeCreatedListedChangedAndDeleted() {
        val created = calendar("action" to "create", "title" to "Doctor", "date" to "2026-11-20", "start_time" to "09:30", "location" to "Clinica")
        assertTrue(created.toString(), created.optBoolean("created"))
        assertEquals("09:30 - 10:30", created.getString("time"))
        val id = created.getString("id")

        val listed = calendar("action" to "list", "from" to "2026-11-01", "to" to "2026-11-30")
        assertEquals(1, listed.getInt("count"))
        assertEquals(0, calendar("action" to "list", "date" to "2026-11-21").getInt("count"))

        val moved = calendar("action" to "update", "id" to id, "date" to "2026-11-21", "start_time" to "14:00", "end_time" to "15:15")
        assertEquals("2026-11-21", moved.getString("date"))
        assertEquals("14:00 - 15:15", moved.getString("time"))
        assertEquals("Clinica", moved.getString("location"))

        assertEquals(1, calendar("action" to "delete", "id" to id).getInt("deleted"))
        assertEquals(0, calendar("action" to "list").getInt("count"))
    }

    @Test
    fun anEventWithoutATimeIsAllDayAndBadInputIsRefused() {
        assertEquals("Toată ziua", calendar("action" to "create", "title" to "Concediu", "date" to "2026-12-24").getString("time"))
        assertTrue(calendar("action" to "create", "title" to "x", "date" to "24.12.2026").has("error"))
        assertTrue(calendar("action" to "create", "title" to "x", "date" to "2026-02-30").has("error"))
        assertTrue(calendar("action" to "create", "title" to "x", "date" to "2026-12-24", "start_time" to "25:00").has("error"))
        assertTrue(calendar("action" to "create", "date" to "2026-12-24").has("error"))
        assertTrue(calendar("action" to "update", "id" to "nu-exista", "title" to "y").has("error"))
        assertTrue(calendar("action" to "delete", "id" to "nu-exista").has("error"))
    }

    @Test
    fun thePageSeesWhatTheAgentChanged() {
        val before = BrainDataStore.revision.value
        calendar("action" to "create", "title" to "Vizibil", "date" to "2026-11-25")
        assertTrue(BrainDataStore.revision.value > before)
        assertEquals("Vizibil", BrainDataStore.calendar(context).single().title)
    }

    @Test
    fun theBridgeAnswersInJsonEvenForBrokenInput() {
        assertTrue(JSONObject(BrainAgentBridgeForTest.run("{nu e json", false)).has("error"))
        val ok = JSONObject(BrainAgentBridgeForTest.run("""{"action":"list"}""", true))
        assertEquals(0, ok.getInt("count"))
    }
}

/** The bridge uses the application's own context, which is the test application here. */
private object BrainAgentBridgeForTest {
    fun run(json: String, calendar: Boolean): String =
        if (calendar) BrainAgentBridge.calendarJson(json) else BrainAgentBridge.libraryJson(json)
}
