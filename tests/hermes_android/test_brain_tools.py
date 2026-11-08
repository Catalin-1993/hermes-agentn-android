"""The agent's library and calendar tools reach the app's bridge, and only inside the Android app."""
import json
from unittest.mock import patch

from tools import android_brain_tool as tool


def test_both_tools_are_registered_for_the_android_app_toolset():
    from toolsets import resolve_toolset
    from tools.registry import registry

    resolved = resolve_toolset("hermes-android-app", include_registry=False)
    assert {"brain_library", "brain_calendar"} <= set(resolved)
    for name in ("brain_library", "brain_calendar"):
        entry = registry.get_entry(name) if hasattr(registry, "get_entry") else registry._tools[name]
        schema = entry.schema
        assert schema["parameters"]["required"] == ["action"]
        assert "list" in schema["parameters"]["properties"]["action"]["enum"]


def test_actions_the_agent_must_have_exist_for_both_tools():
    from tools.registry import registry

    entry = lambda name: registry.get_entry(name) if hasattr(registry, "get_entry") else registry._tools[name]
    library = entry("brain_library").schema["parameters"]["properties"]["action"]["enum"]
    calendar = entry("brain_calendar").schema["parameters"]["properties"]["action"]["enum"]
    assert {"list", "read", "create", "update", "delete"} <= set(library)
    assert {"list", "create", "update", "delete"} <= set(calendar)


def test_a_missing_action_is_refused_before_the_app_is_called():
    with patch.object(tool, "library_action") as library, patch.object(tool, "calendar_action") as calendar:
        assert "action is required" in json.loads(tool.brain_library_tool({}))["error"]
        assert "action is required" in json.loads(tool.brain_calendar_tool({"title": "x"}))["error"]
        library.assert_not_called()
        calendar.assert_not_called()


def test_arguments_reach_the_app_without_empty_fields():
    with patch.object(tool, "calendar_action", return_value={"count": 0, "events": []}) as calendar:
        result = json.loads(tool.brain_calendar_tool({"action": "list", "date": "2026-11-08", "query": None}))
    calendar.assert_called_once_with({"action": "list", "date": "2026-11-08"})
    assert result == {"count": 0, "events": []}


def test_outside_the_android_app_the_tools_are_not_offered(monkeypatch):
    monkeypatch.delenv("HERMES_ANDROID_BOOTSTRAP", raising=False)
    monkeypatch.setattr("sys.platform", "linux")
    assert tool.check_requirements() is False
    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    assert tool.check_requirements() is True


def test_a_bridge_that_is_not_there_answers_with_an_error_not_a_crash():
    from hermes_android import brain_bridge

    answer = brain_bridge.library_action({"action": "list"})
    assert "error" in answer
