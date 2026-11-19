"""Nothing changes on the phone until the person accepts it (Kotlin: approval/ChangeApprovalBridge).

Every tool call passes ``hermes_cli.plugins._dispatch_pre_tool_call_hooks``; inside the Android app
that asks :func:`review_change` first. A call that would save, change or delete something becomes a
proposal the app shows as a card with Accept, Edit and Reject. The call waits for the answer: Accept
runs it (with the person's edits), Reject or no answer blocks it and tells the model so. A prompt
cannot talk its way past this, because the model never decides whether a call needs approval.
"""
from __future__ import annotations

import json
import re
import shlex
from typing import Any, Callable

from hermes_android.device_bridge import _call_android_bridge

_BRIDGE_CLASS = "com.mobilefork.hermesagent.approval.ChangeApprovalBridge"
_PREVIEW_CHARS = 4_000

# Programs that only look. Anything else, any redirection or command chaining, needs approval.
_READ_ONLY_PROGRAMS = frozenset({
    "ls", "cat", "head", "tail", "grep", "egrep", "fgrep", "rg", "wc", "pwd", "echo", "printf",
    "stat", "file", "du", "df", "date", "whoami", "id", "uname", "which", "type", "env", "printenv",
    "ps", "top", "free", "uptime", "tree", "less", "more", "sort", "uniq", "cut", "tr", "diff",
    "md5sum", "sha256sum", "basename", "dirname", "realpath", "readlink", "jq", "cal", "true",
})
_SHELL_WRITES = re.compile(r">|`|\$\(|;|&&|\|\||\btee\b|-delete\b|-exec\b|--output\b|\bsed\s+-i")


def _is_read_only_command(command: str) -> bool:
    if not command.strip() or _SHELL_WRITES.search(command):
        return False
    for segment in command.split("|"):
        try:
            words = shlex.split(segment)
        except ValueError:
            return False
        if not words or words[0].rsplit("/", 1)[-1] not in _READ_ONLY_PROGRAMS:
            return False
    return True


def _action(args: dict[str, Any]) -> str:
    return str(args.get("action") or "").strip().lower()


def _library(args: dict[str, Any]) -> dict[str, Any] | None:
    action = _action(args)
    titles = {"create": "Add to Library", "update": "Change a Library note", "delete": "Delete from Library"}
    if action not in titles:
        return None
    target = args.get("title") or args.get("id") or ""
    editable = [] if action == "delete" else ["title", "content", "tags"]
    note = "Pages inside this note are deleted with it." if action == "delete" else ""
    return {"kind": action, "title": titles[action], "target": str(target), "editable": editable, "note": note}


def _calendar(args: dict[str, Any]) -> dict[str, Any] | None:
    action = _action(args)
    titles = {"create": "Add a calendar event", "update": "Change a calendar event", "delete": "Delete a calendar event"}
    if action not in titles:
        return None
    editable = [] if action == "delete" else ["title", "date", "start_time", "end_time", "location", "description"]
    return {"kind": action, "title": titles[action], "target": str(args.get("title") or args.get("id") or ""),
            "editable": editable}


def _write_file(args: dict[str, Any]) -> dict[str, Any]:
    return {"kind": "write", "title": "Write a file", "target": str(args.get("path") or ""), "editable": ["content"]}


def _patch(args: dict[str, Any]) -> dict[str, Any]:
    editable = ["patch"] if str(args.get("mode") or "") == "patch" else ["new_string"]
    return {"kind": "update", "title": "Change a file", "target": str(args.get("path") or ""), "editable": editable}


def _shared_folder_write(args: dict[str, Any]) -> dict[str, Any]:
    return {"kind": "write", "title": "Write to a shared folder", "target": str(args.get("relative_path") or args.get("path") or ""),
            "editable": ["content"]}


def _memory(args: dict[str, Any]) -> dict[str, Any]:
    action = _action(args) or "update"
    titles = {"add": "Remember something", "replace": "Change something remembered", "remove": "Forget something"}
    editable = ["content"] if action in ("add", "replace") and "content" in args else []
    return {"kind": "delete" if action == "remove" else action, "title": titles.get(action, "Change memory"),
            "target": str(args.get("target") or "memory"), "editable": editable}


def _skill(args: dict[str, Any]) -> dict[str, Any]:
    action = _action(args) or "change"
    editable = [key for key in ("content", "file_content", "new_string") if key in args]
    return {"kind": "delete" if action in ("delete", "remove_file") else "update",
            "title": f"Skill: {action.replace('_', ' ')}", "target": str(args.get("name") or ""), "editable": editable}


def _terminal(args: dict[str, Any]) -> dict[str, Any] | None:
    command = str(args.get("command") or "")
    if _is_read_only_command(command):
        return None
    return {"kind": "run", "title": "Run a command that can change the phone", "target": "", "editable": ["command"]}


def _process(args: dict[str, Any]) -> dict[str, Any] | None:
    if _action(args) not in ("kill", "write", "submit"):
        return None
    return {"kind": "run", "title": f"Process: {_action(args)}", "target": str(args.get("session_id") or ""), "editable": []}


_CHANGE_RULES: dict[str, Callable[[dict[str, Any]], dict[str, Any] | None]] = {
    "brain_library": _library,
    "brain_calendar": _calendar,
    "write_file": _write_file,
    "patch": _patch,
    "android_shared_folder_write": _shared_folder_write,
    "memory": _memory,
    "skill_manage": _skill,
    "terminal": _terminal,
    "process_manage": _process,
}


def proposal_for(tool_name: str, args: dict[str, Any] | None) -> dict[str, Any] | None:
    """What the card shows for a call that changes something, or ``None`` for one that only reads."""
    rule = _CHANGE_RULES.get(tool_name)
    arguments = dict(args or {})
    proposal = rule(arguments) if rule else None
    if proposal is None:
        return None
    shown = {key: value for key, value in arguments.items() if value not in (None, "", [], {})}
    text = {key: _as_text(value) for key, value in shown.items()}
    preview = {key: value[:_PREVIEW_CHARS] for key, value in text.items()}
    # A shortened field stays read-only: accepting an edit of it would save the shortened text.
    truncated = [key for key, value in text.items() if len(value) > _PREVIEW_CHARS]
    proposal["editable"] = [key for key in proposal["editable"] if key in preview and key not in truncated]
    return {"tool": tool_name, "fields": preview, "truncated": truncated, **proposal}


def _as_text(value: Any) -> str:
    if isinstance(value, str):
        return value
    if isinstance(value, list) and all(isinstance(item, str) for item in value):
        return ", ".join(value)
    return json.dumps(value, ensure_ascii=False)


def _ask_the_person(proposal: dict[str, Any]) -> dict[str, Any]:
    raw = _call_android_bridge(_BRIDGE_CLASS, "requestJson", json.dumps(proposal, ensure_ascii=False))
    answer = json.loads(str(raw))
    return answer if isinstance(answer, dict) else {}


def review_change(
    tool_name: str,
    args: dict[str, Any] | None,
    ask: Callable[[dict[str, Any]], dict[str, Any]] = _ask_the_person,
) -> tuple[str | None, dict[str, Any] | None]:
    """``(block_message, edited_args)``: ``(None, None)`` lets a read through untouched."""
    proposal = proposal_for(tool_name, args)
    if proposal is None:
        return None, None
    try:
        answer = ask(proposal)
    except Exception as exc:  # no app to ask means no change
        return (f"Not done: the change could not be shown to the person for approval ({exc}).", None)
    if str(answer.get("decision") or "").lower() != "accept":
        reason = str(answer.get("reason") or "").strip()
        detail = f" ({reason})" if reason else ""
        return (
            "The person rejected this change, so nothing was saved, changed or deleted"
            f"{detail}. Do not retry it unless they ask; say briefly what you would have done.",
            None,
        )
    edited = answer.get("arguments")
    if isinstance(edited, dict):
        merged = dict(args or {})
        merged.update({key: edited[key] for key in proposal["editable"] if key in edited})
        return None, merged
    return None, None
