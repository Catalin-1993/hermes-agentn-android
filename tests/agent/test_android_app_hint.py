"""The Android app draws fenced blocks in its chat, so its runtime must not be told to assume plain text."""

from types import SimpleNamespace

from agent.prompt_builder import ANDROID_APP_HINT, PLATFORM_HINTS
from agent.system_prompt import _platform_hint


def _agent(platform: str) -> SimpleNamespace:
    return SimpleNamespace(platform=platform, pass_session_id=False)


def test_embedded_android_runtime_is_told_the_chat_draws(monkeypatch):
    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    hint = _platform_hint(_agent("api_server"))
    assert ANDROID_APP_HINT in hint
    assert PLATFORM_HINTS["api_server"] not in hint


def test_an_api_server_elsewhere_keeps_its_own_hint(monkeypatch):
    monkeypatch.delenv("HERMES_ANDROID_BOOTSTRAP", raising=False)
    hint = _platform_hint(_agent("api_server"))
    assert PLATFORM_HINTS["api_server"] in hint
    assert ANDROID_APP_HINT not in hint


def test_the_android_hint_points_library_and_calendar_requests_at_their_tools(monkeypatch):
    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    hint = _platform_hint(_agent("api_server"))
    assert "brain_library" in hint and "brain_calendar" in hint
    # A past refusal, in the chat or in a recalled memory, must not outweigh the tools that are there.
    assert "it was wrong" in hint


def test_the_android_hint_asks_for_short_clean_replies(monkeypatch):
    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    hint = _platform_hint(_agent("api_server"))
    assert "short, clean replies" in hint and "no emoji" in hint


def test_the_android_hint_asks_for_a_plan_the_app_can_show(monkeypatch):
    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    hint = _platform_hint(_agent("api_server"))
    assert "todo_list" in hint and "in_progress" in hint


def test_the_android_hint_lets_the_agent_react_with_an_emoji(monkeypatch):
    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    hint = _platform_hint(_agent("api_server"))
    assert "[react:" in hint and "rarely" in hint
