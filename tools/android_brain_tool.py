#!/usr/bin/env python3
"""Tools that let the agent read and change the Brain app's library and calendar."""

from __future__ import annotations

import json

from hermes_android.brain_bridge import calendar_action, library_action
from hermes_android.runtime_identity import is_embedded_android_runtime
from tools.registry import registry


def check_requirements() -> bool:
    return is_embedded_android_runtime()


def _arguments(args: dict | None) -> dict:
    return {key: value for key, value in (args or {}).items() if value is not None}


def brain_library_tool(args: dict | None = None, task_id: str | None = None) -> str:
    del task_id
    arguments = _arguments(args)
    if not str(arguments.get("action", "")).strip():
        return json.dumps({"error": "action is required: list, read, create, update or delete"}, ensure_ascii=False)
    return json.dumps(library_action(arguments), ensure_ascii=False)


def brain_calendar_tool(args: dict | None = None, task_id: str | None = None) -> str:
    del task_id
    arguments = _arguments(args)
    if not str(arguments.get("action", "")).strip():
        return json.dumps({"error": "action is required: list, create, update or delete"}, ensure_ascii=False)
    return json.dumps(calendar_action(arguments), ensure_ascii=False)


registry.register(
    name="brain_library",
    toolset="hermes-android-app",
    schema={
        "name": "brain_library",
        "description": (
            "Read and change the person's Library in the app: their notes and documents. "
            "Actions: list (optionally query, section, limit), read (id or exact title), "
            "create (title, content in Markdown, optional tags and favorite), "
            "update (id or title, then any of title, content, tags, favorite), delete (id or title). "
            "Notes the person sees on screen are these. List first to find an item's id before changing it. "
            "Deleting a note also removes the pages inside it, so confirm with the person before deleting."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "action": {"type": "string", "enum": ["list", "read", "create", "update", "delete"]},
                "id": {"type": "string", "description": "The item's id, from a list."},
                "title": {"type": "string", "description": "A new note's title, or the exact title to find an item."},
                "content": {"type": "string", "description": "The note's text in Markdown (headings with #, bullets with -)."},
                "tags": {"type": "array", "items": {"type": "string"}},
                "favorite": {"type": "boolean"},
                "query": {"type": "string", "description": "Text to look for in titles, tags and content."},
                "section": {"type": "string", "description": "Filter a list by section, for example Privat or Favorite."},
                "limit": {"type": "integer"},
                "max_chars": {"type": "integer", "description": "Longest content to return when reading."},
            },
            "required": ["action"],
            "additionalProperties": False,
        },
    },
    handler=lambda args, **kwargs: brain_library_tool(args, task_id=kwargs.get("task_id")),
    check_fn=check_requirements,
    description="Read and change the person's library of notes and documents",
    emoji="📚",
)

registry.register(
    name="brain_calendar",
    toolset="hermes-android-app",
    schema={
        "name": "brain_calendar",
        "description": (
            "Read and change the person's Calendar in the app. "
            "Actions: list (a date, or from and to, optionally query and limit), "
            "create (title and date YYYY-MM-DD; optional start_time and end_time as HH:MM, location, description, category), "
            "update (id, then any field), delete (id). "
            "List first to find an event's id before changing it. Without a start_time the event is all day."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "action": {"type": "string", "enum": ["list", "create", "update", "delete"]},
                "id": {"type": "string", "description": "The event's id, from a list."},
                "title": {"type": "string"},
                "date": {"type": "string", "description": "YYYY-MM-DD"},
                "from": {"type": "string", "description": "First day of a list, YYYY-MM-DD"},
                "to": {"type": "string", "description": "Last day of a list, YYYY-MM-DD"},
                "start_time": {"type": "string", "description": "HH:MM, 24-hour"},
                "end_time": {"type": "string", "description": "HH:MM, 24-hour"},
                "location": {"type": "string"},
                "description": {"type": "string"},
                "category": {"type": "string"},
                "query": {"type": "string"},
                "limit": {"type": "integer"},
            },
            "required": ["action"],
            "additionalProperties": False,
        },
    },
    handler=lambda args, **kwargs: brain_calendar_tool(args, task_id=kwargs.get("task_id")),
    check_fn=check_requirements,
    description="Read and change the person's calendar events",
    emoji="📅",
)
