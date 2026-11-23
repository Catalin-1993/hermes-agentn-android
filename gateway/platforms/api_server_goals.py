"""``POST /api/goals/{session_id}``: the standing-goal loop of ``hermes_cli.goals`` for API clients.

A client that drives its own turns (the Android app) sets a goal, sends the agent's reply to ``evaluate``
after each turn, and gets back whether to continue, with the continuation prompt, or whether the goal is
done, blocked or out of turns. The judge, the turn budget and the quality gates are the core's own.
"""

from __future__ import annotations

import re
from typing import Any, Callable, Dict, Optional, Tuple

MAX_TURNS_LIMIT = 40
_SESSION_ID = re.compile(r"^[\w.:@-]{1,200}$")
_REPLY_LIMIT = 20_000


def _state_dict(state: Any) -> Dict[str, Any]:
    if state is None:
        return {"status": None}
    return {
        "status": state.status,
        "goal": state.goal,
        "turns_used": state.turns_used,
        "max_turns": state.max_turns,
        "last_verdict": state.last_verdict,
        "last_reason": state.last_reason,
    }


def _default_manager(session_id: str, max_turns: int):
    from hermes_cli.goals import GoalManager

    return GoalManager(session_id=session_id, default_max_turns=max_turns)


def run_goal_action(
    session_id: str,
    body: Dict[str, Any],
    manager_factory: Optional[Callable[[str, int], Any]] = None,
) -> Tuple[int, Dict[str, Any]]:
    """Runs one goal action and returns ``(http status, JSON body)``. Blocking: the judge calls a model."""
    if not _SESSION_ID.match(session_id or ""):
        return 400, {"error": "Invalid session id"}
    action = str(body.get("action") or "").strip().lower()
    try:
        max_turns = int(body.get("max_turns") or 0)
    except (TypeError, ValueError):
        return 400, {"error": "max_turns must be a number"}
    max_turns = max(0, min(max_turns, MAX_TURNS_LIMIT))
    manager = (manager_factory or _default_manager)(session_id, max_turns or 20)

    if action == "set":
        goal = str(body.get("goal") or "").strip()
        if not goal:
            return 400, {"error": "goal is required"}
        state = manager.set(goal, max_turns=max_turns or None)
        return 200, _state_dict(state)
    if action == "status":
        return 200, _state_dict(manager.state)
    if action == "clear":
        manager.clear()
        return 200, {"status": "cleared"}
    if action == "evaluate":
        reply = str(body.get("reply") or "")[:_REPLY_LIMIT]
        if not manager.is_active():
            return 200, {**_state_dict(manager.state), "should_continue": False, "verdict": "inactive",
                         "continuation_prompt": None, "reason": "no active goal", "message": ""}
        decision = manager.evaluate_after_turn(reply)
        return 200, {**_state_dict(manager.state), **decision}
    return 400, {"error": "action must be set, evaluate, status or clear"}


def _http_routes(adapter) -> list:
    return [("POST", "/api/goals/{session_id}", adapter._handle_goal)]
