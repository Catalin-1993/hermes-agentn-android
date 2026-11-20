package com.mobilefork.hermesagent.approval

import android.content.Context
import com.mobilefork.hermesagent.HermesApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How much the agent may change on its own, as the person chose it in the "+" sheet. */
enum class PermissionMode(val key: String) {
    /** Every change is applied without asking. */
    Full("full"),

    /** Notes, calendar events and memory are saved without asking; deletions, files and commands still ask. */
    Auto("auto"),

    /** Every change waits for Accept or Reject. The safe default. */
    Ask("ask");

    companion object {
        fun fromKey(key: String?): PermissionMode = values().firstOrNull { it.key == key } ?: Ask
    }
}

/**
 * Where the chosen [PermissionMode] lives, and what it means for one proposed change. It is read on
 * every request, so a change in the sheet applies to the very next thing the agent tries.
 */
object ChangePermissions {
    private const val PREFERENCES = "brain_change_permissions"
    private const val KEY = "mode"

    /** Tools whose changes only touch the app's own notes, calendar and memory. */
    private val APP_DATA_TOOLS = setOf("brain_library", "brain_calendar", "memory")

    private val _mode = MutableStateFlow(PermissionMode.Ask)
    val mode: StateFlow<PermissionMode> = _mode.asStateFlow()

    /** Reads the saved choice; call once when the sheet or the app starts. */
    fun load(context: Context): PermissionMode {
        val saved = runCatching {
            PermissionMode.fromKey(
                context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, null),
            )
        }.getOrDefault(PermissionMode.Ask)
        _mode.value = saved
        return saved
    }

    fun set(context: Context, mode: PermissionMode) {
        _mode.value = mode
        runCatching {
            context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putString(KEY, mode.key).apply()
        }
        loaded = true
        ChangeApprovalCenter.releaseAllowed()
    }

    /** The mode in force right now, loading it from storage the first time the agent asks. */
    internal fun current(): PermissionMode {
        if (!loaded) {
            loaded = true
            runCatching { load(HermesApplication.instance.applicationContext) }
        }
        return _mode.value
    }

    @Volatile
    private var loaded = false

    /** True when [mode] lets this change through without showing the card. */
    internal fun allowsWithoutAsking(mode: PermissionMode, tool: String, kind: String): Boolean = when (mode) {
        PermissionMode.Full -> true
        PermissionMode.Auto -> tool in APP_DATA_TOOLS && kind != "delete"
        PermissionMode.Ask -> false
    }

    /** For tests: forget what was loaded and start from [mode]. */
    internal fun resetForTest(mode: PermissionMode = PermissionMode.Ask) {
        loaded = true
        _mode.value = mode
    }
}
