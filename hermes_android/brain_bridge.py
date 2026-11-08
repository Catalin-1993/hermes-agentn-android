"""The agent's door to the app's library and calendar (Kotlin: ui/brain/BrainAgentBridge)."""
from __future__ import annotations

import json
from typing import Any

from hermes_android.device_bridge import _bridge_json_result, _call_android_bridge

_BRAIN_BRIDGE_CLASS = "com.mobilefork.hermesagent.ui.brain.BrainAgentBridge"


def _call_brain_bridge(method_name: str, *args: Any) -> Any:
    return _call_android_bridge(_BRAIN_BRIDGE_CLASS, method_name, *args)


def library_action(arguments: dict[str, Any]) -> dict[str, Any]:
    return _bridge_json_result(_call_brain_bridge, "libraryJson", json.dumps(arguments, ensure_ascii=False))


def calendar_action(arguments: dict[str, Any]) -> dict[str, Any]:
    return _bridge_json_result(_call_brain_bridge, "calendarJson", json.dumps(arguments, ensure_ascii=False))
