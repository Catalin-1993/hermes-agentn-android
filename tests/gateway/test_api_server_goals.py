"""The goal loop reaches API clients: set a goal, send each reply to the judge, follow its decision."""

from types import SimpleNamespace

from gateway.platforms.api_server_goals import MAX_TURNS_LIMIT, run_goal_action


class FakeManager:
    """Stands in for hermes_cli.goals.GoalManager with a scripted judge."""

    def __init__(self, verdicts):
        self.state = None
        self._verdicts = list(verdicts)
        self.max_turns = 20

    def set(self, goal, max_turns=None):
        self.max_turns = max_turns or self.max_turns
        self.state = SimpleNamespace(status="active", goal=goal, turns_used=0, max_turns=self.max_turns,
                                     last_verdict="", last_reason="")
        return self.state

    def is_active(self):
        return self.state is not None and self.state.status == "active"

    def clear(self):
        self.state = None

    def evaluate_after_turn(self, reply):
        self.state.turns_used += 1
        verdict = self._verdicts.pop(0)
        done = verdict == "done"
        if done:
            self.state.status = "done"
        return {"status": self.state.status, "should_continue": not done,
                "continuation_prompt": None if done else "Keep going.", "verdict": verdict,
                "reason": "because", "message": ""}


def _factory(manager):
    return lambda session_id, max_turns: manager


def test_a_goal_is_set_judged_after_each_turn_and_ends_when_done():
    manager = FakeManager(["continue", "done"])
    status, state = run_goal_action("s1", {"action": "set", "goal": "Write the report", "max_turns": 8}, _factory(manager))
    assert status == 200 and state["status"] == "active" and state["max_turns"] == 8

    status, first = run_goal_action("s1", {"action": "evaluate", "reply": "Drafted."}, _factory(manager))
    assert first["should_continue"] is True and first["continuation_prompt"] == "Keep going."
    assert first["turns_used"] == 1

    status, last = run_goal_action("s1", {"action": "evaluate", "reply": "Finished."}, _factory(manager))
    assert last["should_continue"] is False and last["status"] == "done"


def test_evaluating_without_a_goal_never_asks_the_judge():
    manager = FakeManager(["continue"])
    status, answer = run_goal_action("s1", {"action": "evaluate", "reply": "hi"}, _factory(manager))
    assert status == 200 and answer["should_continue"] is False and answer["verdict"] == "inactive"
    assert manager._verdicts == ["continue"]


def test_clear_and_status():
    manager = FakeManager([])
    run_goal_action("s1", {"action": "set", "goal": "x"}, _factory(manager))
    assert run_goal_action("s1", {"action": "status"}, _factory(manager))[1]["goal"] == "x"
    assert run_goal_action("s1", {"action": "clear"}, _factory(manager))[1] == {"status": "cleared"}
    assert run_goal_action("s1", {"action": "status"}, _factory(manager))[1] == {"status": None}


def test_bad_requests_are_refused_before_anything_runs():
    manager = FakeManager([])
    assert run_goal_action("bad id!", {"action": "status"}, _factory(manager))[0] == 400
    assert run_goal_action("s1", {"action": "set"}, _factory(manager))[0] == 400
    assert run_goal_action("s1", {"action": "dance"}, _factory(manager))[0] == 400
    assert run_goal_action("s1", {"action": "set", "goal": "x", "max_turns": "many"}, _factory(manager))[0] == 400


def test_the_turn_budget_is_capped():
    manager = FakeManager([])
    _, state = run_goal_action("s1", {"action": "set", "goal": "x", "max_turns": 9999}, _factory(manager))
    assert state["max_turns"] == MAX_TURNS_LIMIT
