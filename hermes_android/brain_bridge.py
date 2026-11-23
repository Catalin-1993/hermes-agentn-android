"""The agent's door to the app's library and calendar (Kotlin: ui/brain/BrainAgentBridge)."""
from __future__ import annotations

import json
from typing import Any

from hermes_android.device_bridge import _bridge_json_result, _call_android_bridge

_BRAIN_BRIDGE_CLASS = "com.mobilefork.hermesagent.ui.brain.BrainAgentBridge"


def _call_brain_bridge(method_name: str, *args: Any) -> Any:
    return _call_android_bridge(_BRAIN_BRIDGE_CLASS, method_name, *args)


def _explained(result: dict[str, Any]) -> dict[str, Any]:
    """An error that says what failed, so the model reports it instead of guessing a cause."""
    error = result.get("error")
    if not error or result.get("bridge") is not None:
        return result
    # The app's own answers (a missing id, a refused title) are already plain; only a call that never
    # reached the app needs the explanation.
    app_answers = ("No library item", "No calendar event", "Give the item", "A title is required",
                   "Unknown action", "The title cannot", "items are titled")
    if any(str(error).startswith(prefix) or prefix in str(error) for prefix in app_answers):
        return result
    return {
        "error": f"The app's Library and Calendar bridge failed: {error}",
        "bridge": "unreachable",
        "note": "This is a fault inside the app, not a missing toolset or an unconnected gateway. "
                "Tell the person this exact error; do not search files for the cause.",
    }


def library_action(arguments: dict[str, Any]) -> dict[str, Any]:
    return _explained(_bridge_json_result(_call_brain_bridge, "libraryJson", json.dumps(arguments, ensure_ascii=False)))


def calendar_action(arguments: dict[str, Any]) -> dict[str, Any]:
    return _explained(_bridge_json_result(_call_brain_bridge, "calendarJson", json.dumps(arguments, ensure_ascii=False)))


def bridge_status() -> dict[str, Any]:
    """Whether the app answers, and how many notes and events it holds: the quickest proof the tools work."""
    return _explained(_bridge_json_result(_call_brain_bridge, "statusJson"))
