"""The capabilities page changes the agent through Hermes' own code, and reads back what it changed."""
import json

from hermes_android import capabilities_bridge as bridge


def _load(home):
    result = json.loads(bridge.list_capabilities(str(home)))
    assert result["ok"], result
    return result


def test_lists_skills_plugins_and_tools_from_hermes(tmp_path, monkeypatch):
    monkeypatch.setenv("HERMES_HOME", str(tmp_path))
    result = _load(tmp_path)
    assert not result.get("errors")
    states = {skill["state"] for skill in result["skills"]}
    assert {"active", "available"} <= states
    assert all(skill["identifier"].startswith("official/") for skill in result["skills"] if skill["state"] == "available")
    assert result["toolsets"] and result["plugins"]


def test_an_official_skill_installs_and_becomes_active(tmp_path, monkeypatch):
    monkeypatch.setenv("HERMES_HOME", str(tmp_path))
    available = next(s for s in _load(tmp_path)["skills"] if s["state"] == "available")
    after = json.loads(bridge.install_skill(available["identifier"], str(tmp_path)))
    assert after["ok"], after
    assert [s["state"] for s in after["skills"] if s["name"] == available["name"]] == ["active"]


def test_only_official_skills_can_be_installed_here(tmp_path, monkeypatch):
    monkeypatch.setenv("HERMES_HOME", str(tmp_path))
    assert not json.loads(bridge.install_skill("github/someone/skill", str(tmp_path)))["ok"]
    assert not json.loads(bridge.install_skill("official/../../etc", str(tmp_path)))["ok"]


def test_switches_read_back_what_they_changed(tmp_path, monkeypatch):
    monkeypatch.setenv("HERMES_HOME", str(tmp_path))
    first = _load(tmp_path)
    tool = first["toolsets"][0]
    after = json.loads(bridge.set_toolset_enabled(tool["key"], not tool["enabled"], str(tmp_path)))
    assert [t["enabled"] for t in after["toolsets"] if t["key"] == tool["key"]] == [not tool["enabled"]]
    plugin = first["plugins"][0]
    on = json.loads(bridge.set_plugin_enabled(plugin["key"], True, str(tmp_path)))
    assert [p["enabled"] for p in on["plugins"] if p["key"] == plugin["key"]] == [True]
    off = json.loads(bridge.set_plugin_enabled(plugin["key"], False, str(tmp_path)))
    assert [p["enabled"] for p in off["plugins"] if p["key"] == plugin["key"]] == [False]
    skill = next(s for s in first["skills"] if s["state"] == "active")
    disabled = json.loads(bridge.set_skill_enabled(skill["name"], False, str(tmp_path)))
    assert [s["state"] for s in disabled["skills"] if s["name"] == skill["name"]] == ["disabled"]
