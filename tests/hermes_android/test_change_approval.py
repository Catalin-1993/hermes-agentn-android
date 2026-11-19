"""Inside the Android app, a call that changes something runs only after the person accepts it."""
from hermes_android import change_approval
from hermes_android.change_approval import proposal_for, review_change


def _never_asked(proposal):
    raise AssertionError(f"a read was sent for approval: {proposal}")


def test_reads_pass_without_a_card():
    for tool, args in [
        ("brain_library", {"action": "list"}),
        ("brain_library", {"action": "read", "id": "l-1"}),
        ("brain_calendar", {"action": "list", "date": "2026-11-19"}),
        ("read_file", {"path": "/x"}),
        ("terminal", {"command": "ls -la | grep notes"}),
    ]:
        assert review_change(tool, args, ask=_never_asked) == (None, None)


def test_every_change_waits_for_the_person():
    changes = [
        ("brain_library", {"action": "create", "title": "Grafice", "content": "x"}),
        ("brain_library", {"action": "delete", "id": "l-1"}),
        ("brain_calendar", {"action": "update", "id": "e1", "title": "Dentist"}),
        ("write_file", {"path": "/sdcard/a.html", "content": "<p>"}),
        ("patch", {"path": "/a", "old_string": "a", "new_string": "b"}),
        ("android_shared_folder_write", {"relative_path": "a.txt", "content": "x"}),
        ("memory", {"action": "add", "target": "memory", "content": "rule"}),
        ("skill_manage", {"action": "delete", "name": "x"}),
        ("terminal", {"command": "rm -rf notes"}),
        ("terminal", {"command": "echo hi > notes.txt"}),
        ("terminal", {"command": "ls; rm x"}),
    ]
    for tool, args in changes:
        asked = []
        message, _ = review_change(tool, args, ask=lambda p: asked.append(p) or {"decision": "reject"})
        assert asked and asked[0]["tool"] == tool, tool
        assert message and "rejected" in message, tool


def test_accept_runs_the_call_with_only_the_fields_the_card_lets_the_person_edit():
    args = {"action": "create", "title": "Old", "content": "draft"}
    answer = {"decision": "accept", "arguments": {"title": "New", "content": "final", "action": "delete"}}
    message, edited = review_change("brain_library", args, ask=lambda p: answer)
    assert message is None
    assert edited == {"action": "create", "title": "New", "content": "final"}


def test_no_answer_or_no_app_means_no_change():
    assert review_change("write_file", {"path": "/a", "content": "x"}, ask=lambda p: {})[0]

    def unreachable(proposal):
        raise RuntimeError("bridge unavailable")

    assert "could not be shown" in review_change("write_file", {"path": "/a"}, ask=unreachable)[0]


def test_every_dispatch_path_asks_inside_the_app(monkeypatch):
    from hermes_cli import plugins

    monkeypatch.setenv("HERMES_ANDROID_BOOTSTRAP", "1")
    monkeypatch.setattr(change_approval, "_ask_the_person", lambda p: {"decision": "reject"})
    monkeypatch.setattr(change_approval.review_change, "__defaults__", (change_approval._ask_the_person,))
    message, _ = plugins._dispatch_pre_tool_call_hooks("brain_calendar", {"action": "delete", "id": "e1"})
    assert message and "rejected" in message
    assert plugins._dispatch_pre_tool_call_hooks("brain_calendar", {"action": "list"})[0] is None


def test_outside_the_app_nothing_is_asked(monkeypatch):
    from hermes_cli import plugins

    monkeypatch.delenv("HERMES_ANDROID_BOOTSTRAP", raising=False)
    monkeypatch.setattr(change_approval.review_change, "__defaults__", (_never_asked,))
    assert plugins._dispatch_pre_tool_call_hooks("write_file", {"path": "/a", "content": "x"})[0] is None


def test_a_field_too_long_to_show_whole_cannot_be_edited():
    long_text = "x" * 10_000
    proposal = proposal_for("write_file", {"path": "/a", "content": long_text})
    assert "content" in proposal["truncated"] and "content" not in proposal["editable"]
    answer = {"decision": "accept", "arguments": {"content": "short"}}
    _, edited = review_change("write_file", {"path": "/a", "content": long_text}, ask=lambda p: answer)
    assert edited is None or edited["content"] == long_text


def test_a_card_shows_what_will_change():
    proposal = proposal_for("brain_calendar", {"action": "create", "title": "Dentist", "date": "2026-11-20"})
    assert proposal["title"] and proposal["fields"]["title"] == "Dentist"
    assert "date" in proposal["editable"]
