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


def test_the_app_tools_stay_in_the_model_tool_list_when_tool_search_is_on():
    """Behind tool_search the model never saw them and told the person it had no access."""
    from tools.tool_search import ToolSearchConfig, assemble_tool_defs, is_deferrable_tool_name

    with patch.object(tool, "check_requirements", return_value=True):
        assert not is_deferrable_tool_name("brain_library")
        assert not is_deferrable_tool_name("brain_calendar")
        from tools.registry import registry

        entry = lambda name: registry.get_entry(name) if hasattr(registry, "get_entry") else registry._tools[name]
        defs = [{"type": "function", "function": entry(n).schema} for n in ("brain_library", "brain_calendar")]
        result = assemble_tool_defs(defs, context_length=128_000, config=ToolSearchConfig.from_raw({"enabled": "on"}))
        names = {d["function"]["name"] for d in result.tool_defs}
        assert {"brain_library", "brain_calendar"} <= names


def test_the_plan_tool_is_not_hidden_behind_tool_search_in_the_android_app(monkeypatch):
    """The app draws the agent's plan from todo_list, so the model has to see it."""
    from tools.tool_search import is_deferrable_tool_name

    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    assert not is_deferrable_tool_name("todo_list", frozenset({"todo_list"}))
    monkeypatch.delenv("HERMES_ANDROID_BOOTSTRAP", raising=False)
    monkeypatch.setattr("sys.platform", "linux")
    assert is_deferrable_tool_name("todo_list", frozenset({"todo_list"}))


def test_a_bridge_failure_is_explained_as_an_app_fault_not_a_missing_toolset():
    with patch("hermes_android.brain_bridge._call_brain_bridge", side_effect=RuntimeError("Chaquopy Java bridge is unavailable")):
        answer = json.loads(tool.brain_library_tool({"action": "create", "title": "Cătălin", "content": "x"}))
    assert "bridge failed" in answer["error"] and "Chaquopy" in answer["error"]
    assert "not a missing toolset" in answer["note"]


def test_the_apps_own_answers_pass_through_unchanged():
    reply = json.dumps({"error": "No library item is titled 'x'. List the library to find its id."})
    with patch("hermes_android.brain_bridge._call_brain_bridge", return_value=reply):
        answer = json.loads(tool.brain_library_tool({"action": "read", "title": "x"}))
    assert answer == {"error": "No library item is titled 'x'. List the library to find its id."}


def test_status_asks_the_app_whether_it_answers():
    reply = json.dumps({"ok": True, "library_items": 3, "calendar_events": 1})
    with patch("hermes_android.brain_bridge._call_brain_bridge", return_value=reply) as call:
        answer = json.loads(tool.brain_library_tool({"action": "status"}))
    call.assert_called_once_with("statusJson")
    assert answer == {"ok": True, "library_items": 3, "calendar_events": 1}


def test_create_update_and_delete_reach_the_app_with_what_the_person_asked():
    seen = []

    def fake(method, payload=None):
        seen.append((method, json.loads(payload)))
        return json.dumps({"created": True, "id": "l-1"})

    with patch("hermes_android.brain_bridge._call_brain_bridge", side_effect=fake):
        tool.brain_library_tool({"action": "create", "title": "Cătălin", "content": "# Cătălin"})
        tool.brain_library_tool({"action": "update", "id": "l-1", "content": "new"})
        tool.brain_library_tool({"action": "delete", "id": "l-1"})
    assert [(m, p["action"]) for m, p in seen] == [("libraryJson", "create"), ("libraryJson", "update"), ("libraryJson", "delete")]
    assert seen[0][1]["title"] == "Cătălin"
