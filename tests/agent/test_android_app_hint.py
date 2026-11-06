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
