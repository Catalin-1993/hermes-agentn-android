"""What the Hermes agent can do, for the Android capabilities page, with one-tap changes.

Everything here goes through Hermes' own code paths, so the app and ``hermes`` agree:
skills through ``skills.disabled`` and the official skills hub, plugins through
``hermes plugins enable/disable``, and tools through the ``api_server`` platform's
``platform_toolsets`` (the platform the app's chat talks to).
"""
from __future__ import annotations

import contextlib
import io
import json
import os
from pathlib import Path
from typing import Any

from hermes_android.python_path import prefer_hermes_package_root

prefer_hermes_package_root()

APP_PLATFORM = "api_server"


def _ok(**payload: Any) -> str:
    return json.dumps({"ok": True, **payload}, ensure_ascii=False)


def _err(message: str, **payload: Any) -> str:
    return json.dumps({"ok": False, "error": message, **payload}, ensure_ascii=False)


def _use_home(hermes_home: str | None) -> Path:
    from hermes_android.skills_bridge import _hermes_home

    home = _hermes_home(hermes_home)
    os.environ["HERMES_HOME"] = str(home)
    return home


@contextlib.contextmanager
def _quiet():
    """Hermes' commands talk to a terminal; here their output is only kept for errors."""
    sink = io.StringIO()
    with contextlib.redirect_stdout(sink), contextlib.redirect_stderr(sink):
        yield sink


def _last_line(text: str) -> str:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    return lines[-1] if lines else ""


# ---------------------------------------------------------------------------- skills


def _skills(home: Path) -> list[dict[str, Any]]:
    from hermes_android.bundled_assets import bundled_skills_dir, optional_skills_dir
    from hermes_android.skills_bridge import (
        _enabled_for_name, _iter_skill_md, _load_skills_config, _read_skill_md_header,
    )

    skills_cfg, _ = _load_skills_config(home)
    seen: set[str] = set()
    out: list[dict[str, Any]] = []
    for root, source in ((home / "skills", "installed"), (bundled_skills_dir(), "bundled")):
        if not root.is_dir():
            continue
        for skill_md in _iter_skill_md(root):
            name, description = _read_skill_md_header(skill_md)
            if not name or name in seen:
                continue
            seen.add(name)
            out.append({
                "name": name,
                "description": description,
                "category": skill_md.parent.relative_to(root).parts[0] if skill_md.parent != root else "",
                "source": source,
                "state": "active" if _enabled_for_name(name, skills_cfg) else "disabled",
            })
    optional = optional_skills_dir()
    if optional.is_dir():
        for skill_md in _iter_skill_md(optional):
            name, description = _read_skill_md_header(skill_md)
            if not name or name in seen:
                continue
            seen.add(name)
            rel = skill_md.parent.relative_to(optional)
            out.append({
                "name": name,
                "description": description,
                "category": rel.parts[0] if rel.parts else "",
                "source": "official",
                "identifier": "official/" + rel.as_posix(),
                "state": "available",
            })
    out.sort(key=lambda item: (item["state"] == "available", item["name"].lower()))
    return out


def install_skill(identifier: str, hermes_home: str | None = None) -> str:
    """Install one of Hermes' official optional skills, the way ``hermes skills install`` does."""
    try:
        home = _use_home(hermes_home)
        ident = str(identifier or "").strip()
        if not ident.startswith("official/") or ".." in ident:
            return _err("only official Hermes skills can be installed here")
        from rich.console import Console
        from hermes_cli.skills_hub import do_install

        with _quiet() as sink:
            console = Console(file=sink, force_terminal=False, width=120)
            try:
                do_install(ident, console=console, skip_confirm=True)
            except SystemExit:
                pass
        installed = {item["name"] for item in _skills(home) if item["state"] != "available"}
        wanted = ident.rsplit("/", 1)[-1]
        if not any(name == wanted or name.endswith("/" + wanted) for name in installed):
            return _err(_last_line(sink.getvalue()) or "the skill was not installed")
        return list_capabilities(str(home))
    except Exception as exc:  # noqa: BLE001 - surfaced to the Android UI
        return _err(str(exc) or exc.__class__.__name__)


def set_skill_enabled(name: str, enabled: bool, hermes_home: str | None = None) -> str:
    from hermes_android.skills_bridge import set_skill_enabled as toggle

    result = json.loads(toggle(name, enabled, hermes_home))
    if not result.get("ok"):
        return json.dumps(result)
    return list_capabilities(hermes_home)


# ---------------------------------------------------------------------------- plugins


def _plugins() -> list[dict[str, Any]]:
    from hermes_cli.plugins_discovery import (
        _get_disabled_plugins, _get_enabled_plugins, collect_directory_manifests, gate_manifest,
    )
    from hermes_cli.plugins_manifest import manifest_key

    disabled = _get_disabled_plugins()
    enabled = _get_enabled_plugins()
    out: list[dict[str, Any]] = []
    for manifest in collect_directory_manifests():
        # Messaging-platform adapters and provider backends are chosen elsewhere; a phone switches
        # on the standalone plugins that add abilities to the agent itself.
        if manifest.kind != "standalone":
            continue
        gate = gate_manifest(manifest, disabled, enabled)
        out.append({
            "key": manifest_key(manifest),
            "name": manifest.name,
            "description": manifest.description,
            "kind": manifest.kind,
            "source": manifest.source,
            "enabled": getattr(gate, "action", "") == "load",
        })
    out.sort(key=lambda item: (not item["enabled"], item["name"].lower()))
    return out


def set_plugin_enabled(key: str, enabled: bool, hermes_home: str | None = None) -> str:
    try:
        _use_home(hermes_home)
        from hermes_cli import plugins_cmd

        with _quiet() as sink:
            try:
                if enabled:
                    plugins_cmd.cmd_enable(str(key), allow_tool_override=False)
                else:
                    plugins_cmd.cmd_disable(str(key))
            except SystemExit:
                return _err(_last_line(sink.getvalue()) or "the plugin could not be changed")
        return list_capabilities(hermes_home)
    except Exception as exc:  # noqa: BLE001
        return _err(str(exc) or exc.__class__.__name__)


# ---------------------------------------------------------------------------- tools


def _toolsets() -> list[dict[str, Any]]:
    from hermes_cli.config import load_config
    from hermes_cli.tools_config import _get_effective_configurable_toolsets, _get_platform_tools

    active = _get_platform_tools(load_config(), APP_PLATFORM)
    return [
        {"key": key, "label": label, "description": description, "enabled": key in active}
        for key, label, description in _get_effective_configurable_toolsets()
    ]


def set_toolset_enabled(key: str, enabled: bool, hermes_home: str | None = None) -> str:
    try:
        _use_home(hermes_home)
        from hermes_cli.config import load_config, save_config
        from hermes_cli.tools_config import _get_platform_tools, _save_platform_tools

        config = load_config()
        active = set(_get_platform_tools(config, APP_PLATFORM))
        if enabled:
            active.add(str(key))
        else:
            active.discard(str(key))
        _save_platform_tools(config, APP_PLATFORM, active)
        save_config(config)
        return list_capabilities(hermes_home)
    except Exception as exc:  # noqa: BLE001
        return _err(str(exc) or exc.__class__.__name__)


# ---------------------------------------------------------------------------- all together


def list_capabilities(hermes_home: str | None = None) -> str:
    """Skills, plugins and tools in one answer. A part that fails says why; the rest still loads."""
    try:
        home = _use_home(hermes_home)
    except Exception as exc:  # noqa: BLE001
        return _err(str(exc) or exc.__class__.__name__)
    payload: dict[str, Any] = {}
    errors: dict[str, str] = {}
    for part, loader in (("skills", lambda: _skills(home)), ("plugins", _plugins), ("toolsets", _toolsets)):
        try:
            with _quiet():
                payload[part] = loader()
        except Exception as exc:  # noqa: BLE001
            payload[part] = []
            errors[part] = str(exc) or exc.__class__.__name__
    if errors:
        payload["errors"] = errors
    return _ok(**payload)
