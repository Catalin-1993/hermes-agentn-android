package com.mobilefork.hermesagent.ui.hub

import android.content.Context
import androidx.annotation.DrawableRes
import com.mobilefork.hermesagent.R
import com.mobilefork.hermesagent.ui.brain.BrainPage
import com.mobilefork.hermesagent.ui.i18n.HubStrings
import com.mobilefork.hermesagent.ui.shell.AppSection
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** What a library can hold. Notes is the library itself; Ideas and Goals are pages the person can add to it. */
enum class LibraryTemplate(val key: String) {
    Notes("notes"),
    Ideas("ideas"),
    Goals("goals"),
}

/** Whether a template is in the person's library, and whether it is switched on. */
data class TemplateState(val added: Boolean = true, val active: Boolean = true)

/** Which templates the library holds. Everything starts in the library and on, so nothing that existed is lost. */
class LibraryTemplateStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("brain_templates", Context.MODE_PRIVATE)

    fun load(): Map<LibraryTemplate, TemplateState> = LibraryTemplate.entries.associateWith { template ->
        TemplateState(
            added = preferences.getBoolean("added_${template.key}", true),
            active = preferences.getBoolean("active_${template.key}", true),
        )
    }

    fun save(template: LibraryTemplate, state: TemplateState) {
        preferences.edit()
            .putBoolean("added_${template.key}", state.added)
            .putBoolean("active_${template.key}", state.active)
            .apply()
    }
}

/** True where the template is in the library and switched on. */
fun Map<LibraryTemplate, TemplateState>.isOn(template: LibraryTemplate): Boolean =
    this[template]?.let { it.added && it.active } ?: true

/** A button the bottom bar and the shortcuts page can show: one page of the app. */
enum class DockItem(
    val key: String,
    val section: AppSection?,
    val page: BrainPage?,
    @DrawableRes val iconRes: Int,
    val template: LibraryTemplate? = null,
) {
    Chat("chat", AppSection.Hermes, null, R.drawable.ic_tab_chat),
    Conversations("conversations", AppSection.History, null, R.drawable.ic_tab_conversations),
    Shortcuts("shortcuts", AppSection.Hub, null, R.drawable.ic_tab_apps),
    Library("library", null, BrainPage.Library, R.drawable.ic_tab_library),
    Calendar("calendar", null, BrainPage.Calendar, R.drawable.ic_tab_calendar),
    Projects("projects", null, BrainPage.Projects, R.drawable.ic_tab_projects),
    Ideas("ideas", AppSection.Ideas, null, R.drawable.ic_tab_ideas, LibraryTemplate.Ideas),
    Goals("goals", AppSection.Kanban, null, R.drawable.ic_tab_goals, LibraryTemplate.Goals),
    Settings("settings", AppSection.Settings, null, R.drawable.ic_action_cog);

    fun title(words: HubStrings): String = when (this) {
        Chat -> words.dockChat
        Conversations -> words.dockConversations
        Shortcuts -> words.dockShortcuts
        Library -> words.dockLibrary
        Calendar -> words.dockCalendar
        Projects -> words.dockProjects
        Ideas -> words.dockIdeas
        Goals -> words.dockGoals
        Settings -> words.dockSettings
    }

    /** Whether the page can be opened: a page from a template needs that template on. */
    fun isAvailable(templates: Map<LibraryTemplate, TemplateState>): Boolean = template == null || templates.isOn(template)

    companion object {
        fun fromKey(key: String): DockItem? = entries.firstOrNull { it.key == key }
        const val MIN = 2
        const val MAX = 5
        val Default = listOf(Chat, Conversations, Shortcuts, Library)
    }
}

/**
 * One button of the bottom bar. [key] is a [DockItem] key for a page of the app, "project:<id>" for one
 * project, or "page:<id>" for one page or folder of the library; [label] is the name those two carry.
 */
data class DockEntry(val key: String, val label: String = "") {
    val item: DockItem? get() = DockItem.fromKey(key)
    val projectId: String? get() = key.takeIf { it.startsWith(PROJECT_PREFIX) }?.removePrefix(PROJECT_PREFIX)
    val pageId: String? get() = key.takeIf { it.startsWith(PAGE_PREFIX) }?.removePrefix(PAGE_PREFIX)

    fun title(words: HubStrings): String = item?.title(words) ?: label

    @DrawableRes
    fun iconRes(): Int = item?.iconRes ?: if (projectId != null) R.drawable.ic_tab_projects else R.drawable.ic_tab_note

    /** A project or a library page stays available; a page of a template needs that template on. */
    fun isAvailable(templates: Map<LibraryTemplate, TemplateState>): Boolean = item?.isAvailable(templates) ?: true

    companion object {
        const val PROJECT_PREFIX = "project:"
        const val PAGE_PREFIX = "page:"
        fun of(item: DockItem) = DockEntry(item.key)
        fun project(id: String, name: String) = DockEntry(PROJECT_PREFIX + id, name)
        fun page(id: String, title: String) = DockEntry(PAGE_PREFIX + id, title)
        val Default = DockItem.Default.map(::of)
    }
}

/** The buttons of the bottom bar, in order. The person edits them by holding one. */
class DockStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("brain_dock", Context.MODE_PRIVATE)

    fun load(): List<DockEntry> {
        val saved = preferences.getString("entries", null)?.let { raw ->
            runCatching {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    DockEntry(item.optString("key"), item.optString("label")).takeIf { it.isValid() }
                }
            }.getOrNull()
        } ?: preferences.getString("items", null)?.split(",")?.mapNotNull { DockItem.fromKey(it.trim())?.let(DockEntry::of) }
        return saved?.distinctBy { it.key }?.takeIf { it.size >= DockItem.MIN }?.take(DockItem.MAX) ?: DockEntry.Default
    }

    fun save(items: List<DockEntry>) {
        val array = JSONArray()
        items.distinctBy { it.key }.take(DockItem.MAX).forEach { array.put(JSONObject().put("key", it.key).put("label", it.label)) }
        preferences.edit().putString("entries", array.toString()).apply()
    }

    private fun DockEntry.isValid(): Boolean =
        item != null || ((projectId != null || pageId != null) && label.isNotBlank())
}

/** What a shortcut opens. */
enum class ShortcutKind(val key: String) {
    Page("page"),
    Agent("agent"),
    Chat("chat"),
    App("app"),
}

/** One tile of the shortcuts page. [target] is a [DockItem] key, an agent id, a conversation id or a package name. */
data class Shortcut(
    val id: String,
    val kind: ShortcutKind,
    val target: String,
    val label: String,
)

/** The tiles of the shortcuts page, in order. */
class ShortcutStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("brain_shortcuts", Context.MODE_PRIVATE)

    fun load(): List<Shortcut> {
        val raw = preferences.getString("shortcuts", null)
            ?: return DEFAULTS.map { Shortcut(UUID.randomUUID().toString(), ShortcutKind.Page, it.key, "") }.also { save(it) }
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val kind = ShortcutKind.entries.firstOrNull { it.key == item.optString("kind") } ?: return@mapNotNull null
                Shortcut(item.optString("id"), kind, item.optString("target"), item.optString("label"))
                    .takeIf { it.id.isNotBlank() && it.target.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    fun save(shortcuts: List<Shortcut>) {
        val array = JSONArray()
        shortcuts.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("kind", item.kind.key).put("target", item.target).put("label", item.label))
        }
        preferences.edit().putString("shortcuts", array.toString()).apply()
    }

    private companion object {
        /** A first look that shows what the page is for. */
        val DEFAULTS = listOf(DockItem.Calendar, DockItem.Library, DockItem.Projects)
    }
}
