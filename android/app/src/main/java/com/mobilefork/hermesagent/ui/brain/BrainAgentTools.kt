package com.mobilefork.hermesagent.ui.brain

import android.content.Context
import com.example.data.model.CalendarEventItem
import com.example.data.model.LibraryItem
import com.example.data.model.NotionBlock
import com.example.data.model.NotionBlockType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * What the agent can do with the person's library (notes and documents) and calendar: list, read,
 * create, update and delete. Both tools take one JSON object with an `action`, and always answer
 * with a JSON object: the result, or `{"error": …}` saying what to fix.
 */
class BrainAgentTools(private val context: Context) {

    fun library(args: JSONObject): JSONObject = guarded {
        when (val action = args.optString("action").trim().lowercase()) {
            "list", "search" -> libraryList(args)
            "read" -> libraryRead(args)
            "create" -> libraryCreate(args)
            "update" -> libraryUpdate(args)
            "delete" -> libraryDelete(args)
            else -> error("Unknown action '$action'. Use list, read, create, update or delete.")
        }
    }

    fun calendar(args: JSONObject): JSONObject = guarded {
        when (val action = args.optString("action").trim().lowercase()) {
            "list", "search" -> calendarList(args)
            "create" -> calendarCreate(args)
            "update" -> calendarUpdate(args)
            "delete" -> calendarDelete(args)
            else -> error("Unknown action '$action'. Use list, create, update or delete.")
        }
    }

    private fun guarded(block: () -> JSONObject): JSONObject = try {
        block()
    } catch (e: ToolError) {
        JSONObject().put("error", e.message)
    } catch (e: Exception) {
        JSONObject().put("error", e.message ?: e::class.java.simpleName)
    }

    private class ToolError(message: String) : Exception(message)

    private fun error(message: String): Nothing = throw ToolError(message)

    // ------------------------------------------------------------------ library

    private fun libraryList(args: JSONObject): JSONObject {
        val query = args.optString("query").trim()
        val section = args.optString("section").trim()
        val limit = args.optInt("limit", DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        val matches = BrainDataStore.library(context).filter { item ->
            (section.isEmpty() || item.section.equals(section, ignoreCase = true)) &&
                (query.isEmpty() || item.matches(query))
        }
        return JSONObject()
            .put("count", matches.size)
            .put("items", JSONArray(matches.take(limit).map(::summaryOf)))
    }

    private fun libraryRead(args: JSONObject): JSONObject {
        val item = findItem(BrainDataStore.library(context), args)
        val maxChars = args.optInt("max_chars", DEFAULT_READ_CHARS).coerceIn(1, MAX_READ_CHARS)
        val content = contentOf(item).ifBlank { fileText(item) }
        return summaryOf(item)
            .put("content", content.take(maxChars))
            .put("truncated", content.length > maxChars)
    }

    private fun libraryCreate(args: JSONObject): JSONObject {
        val title = args.optString("title").trim()
        if (title.isEmpty()) error("A title is required to create a library note.")
        val content = args.optString("content")
        val tags = tagsOf(args)
        val now = System.currentTimeMillis()
        val favorite = args.optBoolean("favorite", false)
        val base = LibraryItem(
            id = "l-" + UUID.randomUUID().toString().take(6),
            title = title,
            category = NOTE_CATEGORY,
            snippet = snippetOf(content),
            fullContent = content,
            dateAdded = now,
            iconEmoji = "icon:Description",
            tags = tags,
            blocks = blocksOf(content),
            isRecent = true,
            isFavorite = favorite,
            section = if (favorite) "Favorite" else "Privat",
            lastModified = now,
        )
        val stored = BrainDataStore.addNote(context, base)
        return summaryOf(stored).put("created", true)
    }

    private fun libraryUpdate(args: JSONObject): JSONObject {
        val updated = BrainDataStore.withLibrary(context) { list ->
            val current = findItem(list, args)
            val hasContent = args.has("content")
            val content = if (hasContent) args.optString("content") else contentOf(current)
            var next = current.copy(lastModified = System.currentTimeMillis())
            if (args.has("title")) {
                val title = args.optString("title").trim()
                if (title.isEmpty()) error("The title cannot be empty.")
                next = next.copy(title = title)
            }
            if (hasContent) {
                next = next.copy(fullContent = content, snippet = snippetOf(content), blocks = blocksOf(content))
                writeFile(next, content)
            }
            if (args.has("tags")) next = next.copy(tags = tagsOf(args))
            if (args.has("favorite")) {
                val favorite = args.optBoolean("favorite")
                next = next.copy(isFavorite = favorite, section = if (favorite) "Favorite" else "Privat")
            }
            list[list.indexOfFirst { it.id == current.id }] = next
            next
        }
        return summaryOf(updated).put("updated", true)
    }

    private fun libraryDelete(args: JSONObject): JSONObject {
        val removed = BrainDataStore.withLibrary(context) { list ->
            val item = findItem(list, args)
            // A note's children go with it, so no page is left pointing at one that is gone.
            val doomed = descendantsOf(list, item.id) + item.id
            val gone = list.filter { it.id in doomed }
            list.removeAll { it.id in doomed }
            gone
        }
        removed.forEach(::deleteOwnFile)
        return JSONObject().put("deleted", removed.size).put("ids", JSONArray(removed.map { it.id }))
    }

    private fun findItem(list: List<LibraryItem>, args: JSONObject): LibraryItem {
        val id = args.optString("id").trim()
        if (id.isNotEmpty()) return list.firstOrNull { it.id == id } ?: error("No library item has the id '$id'.")
        val title = args.optString("title").trim()
        if (title.isEmpty()) error("Give the item's id (from a list) or its exact title.")
        val matches = list.filter { it.title.equals(title, ignoreCase = true) }
        return when (matches.size) {
            0 -> error("No library item is titled '$title'. List the library to find its id.")
            1 -> matches.single()
            else -> error("${matches.size} items are titled '$title'. Use the id from a list.")
        }
    }

    private fun descendantsOf(list: List<LibraryItem>, id: String): Set<String> {
        val found = mutableSetOf<String>()
        var frontier = setOf(id)
        while (frontier.isNotEmpty()) {
            frontier = list.filter { it.parentId in frontier && it.id !in found }.map { it.id }.toSet()
            found += frontier
        }
        return found
    }

    private fun LibraryItem.matches(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
            snippet.contains(query, ignoreCase = true) ||
            fullContent.contains(query, ignoreCase = true) ||
            tags.any { it.contains(query, ignoreCase = true) }

    private fun summaryOf(item: LibraryItem): JSONObject = JSONObject()
        .put("id", item.id)
        .put("title", item.title)
        .put("category", item.category)
        .put("section", item.section)
        .put("tags", JSONArray(item.tags))
        .put("favorite", item.isFavorite)
        .put("snippet", item.snippet.take(SNIPPET_CHARS))
        .put("is_file", !item.filePath.isNullOrBlank() && item.category != NOTE_CATEGORY)
        .put("updated", isoTime(item.lastModified.takeIf { it > 0 } ?: item.dateAdded))

    private fun contentOf(item: LibraryItem): String =
        item.fullContent.ifBlank { item.blocks.joinToString("\n") { it.content } }

    private fun fileText(item: LibraryItem): String {
        val path = item.filePath ?: return ""
        return runCatching { File(path).takeIf { it.isFile && it.length() <= MAX_FILE_BYTES }?.readText() }.getOrNull().orEmpty()
    }

    /** Only files this app keeps in its own storage are ever written or deleted from here. */
    private fun ownFile(item: LibraryItem): File? {
        val path = item.filePath?.takeIf { it.isNotBlank() } ?: return null
        val file = File(path)
        val root = context.filesDir.canonicalPath + File.separator
        return file.takeIf { it.canonicalPath.startsWith(root) }
    }

    private fun writeFile(item: LibraryItem, content: String) {
        ownFile(item)?.let { runCatching { it.writeText(content) } }
    }

    private fun deleteOwnFile(item: LibraryItem) {
        ownFile(item)?.let { runCatching { it.delete() } }
    }

    private fun tagsOf(args: JSONObject): List<String> {
        val raw = args.opt("tags")
        val list = when (raw) {
            is JSONArray -> (0 until raw.length()).map { raw.optString(it) }
            is String -> raw.split(',')
            else -> emptyList()
        }
        return list.map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.distinct().take(MAX_TAGS)
    }

    private fun snippetOf(content: String): String {
        val flat = content.replace(Regex("\\s+"), " ").trim()
        return if (flat.length > 70) flat.take(70) + "..." else flat
    }

    /** Markdown becomes the note's blocks: headings, bullets, quotes, and paragraphs for the rest. */
    private fun blocksOf(content: String): List<NotionBlock> {
        val lines = content.replace("\r\n", "\n").split('\n')
        val blocks = lines.map { line ->
            val text = line.trimEnd()
            when {
                text.startsWith("### ") -> NotionBlock(type = NotionBlockType.HEADING_3, content = text.drop(4))
                text.startsWith("## ") -> NotionBlock(type = NotionBlockType.HEADING_2, content = text.drop(3))
                text.startsWith("# ") -> NotionBlock(type = NotionBlockType.HEADING_1, content = text.drop(2))
                text.startsWith("- ") || text.startsWith("* ") -> NotionBlock(type = NotionBlockType.BULLET_LIST_ITEM, content = text.drop(2))
                text.startsWith("> ") -> NotionBlock(type = NotionBlockType.QUOTE, content = text.drop(2))
                else -> NotionBlock(type = NotionBlockType.PARAGRAPH, content = text)
            }
        }
        return blocks.ifEmpty { listOf(NotionBlock(type = NotionBlockType.PARAGRAPH, content = "")) }
    }

    // ------------------------------------------------------------------ calendar

    private fun calendarList(args: JSONObject): JSONObject {
        val date = args.optString("date").trim()
        val from = args.optString("from").trim().ifEmpty { date }
        val to = args.optString("to").trim().ifEmpty { date }
        listOf(from, to).filter { it.isNotEmpty() }.forEach(::requireDate)
        val query = args.optString("query").trim()
        val limit = args.optInt("limit", DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        val matches = BrainDataStore.calendar(context)
            .filter { (from.isEmpty() || it.dateString >= from) && (to.isEmpty() || it.dateString <= to) }
            .filter { query.isEmpty() || it.title.contains(query, true) || it.description.contains(query, true) || it.location.contains(query, true) }
            .sortedWith(compareBy({ it.dateString }, { it.startHour }))
        return JSONObject().put("count", matches.size).put("events", JSONArray(matches.take(limit).map(::eventJson)))
    }

    private fun calendarCreate(args: JSONObject): JSONObject {
        val title = args.optString("title").trim()
        if (title.isEmpty()) error("A title is required to create an event.")
        val date = requireDate(args.optString("date").trim().ifEmpty { error("A date (YYYY-MM-DD) is required to create an event.") })
        val (time, hour) = timeOf(args.optString("start_time").trim(), args.optString("end_time").trim())
        val event = CalendarEventItem(
            id = UUID.randomUUID().toString(),
            title = title,
            dateString = date,
            time = time,
            startHour = hour,
            category = args.optString("category").trim().ifEmpty { "Work" },
            location = args.optString("location").trim(),
            description = args.optString("description").trim(),
        )
        BrainDataStore.withCalendar(context) { it.add(event) }
        return eventJson(event).put("created", true)
    }

    private fun calendarUpdate(args: JSONObject): JSONObject {
        val updated = BrainDataStore.withCalendar(context) { list ->
            val id = args.optString("id").trim().ifEmpty { error("The event's id (from a list) is required.") }
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) error("No event has the id '$id'.")
            var next = list[index]
            if (args.has("title")) {
                val title = args.optString("title").trim()
                if (title.isEmpty()) error("The title cannot be empty.")
                next = next.copy(title = title)
            }
            if (args.has("date")) next = next.copy(dateString = requireDate(args.optString("date").trim()))
            if (args.has("start_time") || args.has("end_time")) {
                val (time, hour) = timeOf(args.optString("start_time").trim(), args.optString("end_time").trim())
                next = next.copy(time = time, startHour = hour)
            }
            if (args.has("location")) next = next.copy(location = args.optString("location").trim())
            if (args.has("description")) next = next.copy(description = args.optString("description").trim())
            if (args.has("category")) next = next.copy(category = args.optString("category").trim().ifEmpty { "Work" })
            list[index] = next
            next
        }
        return eventJson(updated).put("updated", true)
    }

    private fun calendarDelete(args: JSONObject): JSONObject {
        val id = args.optString("id").trim().ifEmpty { error("The event's id (from a list) is required.") }
        val removed = BrainDataStore.withCalendar(context) { list ->
            val before = list.size
            list.removeAll { it.id == id }
            before - list.size
        }
        if (removed == 0) error("No event has the id '$id'.")
        return JSONObject().put("deleted", removed).put("id", id)
    }

    private fun eventJson(event: CalendarEventItem): JSONObject = JSONObject()
        .put("id", event.id)
        .put("title", event.title)
        .put("date", event.dateString)
        .put("time", event.time)
        .put("category", event.category)
        .put("location", event.location)
        .put("description", event.description)
        .put("holiday", event.isHoliday)

    private fun requireDate(value: String): String {
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(value)
        }.getOrNull()
        if (parsed == null || !Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) error("'$value' is not a date. Use YYYY-MM-DD.")
        return value
    }

    /** "09:30" and "11:00" become the text the calendar shows and the hour it files the event under. */
    private fun timeOf(start: String, end: String): Pair<String, Int> {
        if (start.isEmpty()) return ALL_DAY to 0
        val begin = parseClock(start)
        val finish = if (end.isEmpty()) {
            "%02d:%02d".format(Locale.ROOT, (begin.first + 1).coerceAtMost(23), begin.second)
        } else {
            parseClock(end).let { "%02d:%02d".format(Locale.ROOT, it.first, it.second) }
        }
        return "%02d:%02d - %s".format(Locale.ROOT, begin.first, begin.second, finish) to begin.first
    }

    private fun parseClock(value: String): Pair<Int, Int> {
        val match = Regex("^(\\d{1,2}):(\\d{2})$").find(value) ?: error("'$value' is not a time. Use HH:MM, 24-hour.")
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) error("'$value' is not a valid time.")
        return hour to minute
    }

    private fun isoTime(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ROOT).format(epochMs)

    private companion object {
        const val NOTE_CATEGORY = "Notițe"
        const val ALL_DAY = "Toată ziua"
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 200
        const val DEFAULT_READ_CHARS = 20_000
        const val MAX_READ_CHARS = 100_000
        const val SNIPPET_CHARS = 160
        const val MAX_TAGS = 12
        const val MAX_FILE_BYTES = 2_000_000L
    }
}
