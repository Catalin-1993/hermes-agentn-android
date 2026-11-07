"""A checkout cloned from a former source moves to the update source; any other origin stays."""

import subprocess

import pytest

from hermes_cli import update_cmd_git
from hermes_cli.update_source import UPDATE_BRANCH, UPDATE_REPO_HTTPS_URL


def _git(cwd, *args):
    return subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True, text=True).stdout.strip()


@pytest.fixture
def checkout(tmp_path):
    _git(tmp_path, "init", "-q")
    return tmp_path


@pytest.mark.parametrize("former", [
    "https://github.com/NousResearch/hermes-agent.git",
    "git@github.com:NousResearch/hermes-agent.git",
    "https://github.com/adybag14-cyber/hermes-agent",
])
def test_former_source_is_repointed_and_single_branch_refspec_follows(checkout, former):
    _git(checkout, "remote", "add", "origin", former)
    _git(checkout, "config", "remote.origin.fetch", "+refs/heads/main:refs/remotes/origin/main")

    assert update_cmd_git._repoint_origin_to_update_source(["git"], checkout) is True

    assert _git(checkout, "remote", "get-url", "origin") == UPDATE_REPO_HTTPS_URL
    assert _git(checkout, "config", "--get-all", "remote.origin.fetch") == (
        f"+refs/heads/{UPDATE_BRANCH}:refs/remotes/origin/{UPDATE_BRANCH}")


def test_wildcard_refspec_is_left_alone(checkout):
    _git(checkout, "remote", "add", "origin", "https://github.com/NousResearch/hermes-agent.git")

    assert update_cmd_git._repoint_origin_to_update_source(["git"], checkout) is True

    assert _git(checkout, "config", "--get-all", "remote.origin.fetch") == "+refs/heads/*:refs/remotes/origin/*"


@pytest.mark.parametrize("other", [UPDATE_REPO_HTTPS_URL, "https://github.com/someone/else.git"])
def test_other_origins_are_not_touched(checkout, other):
    _git(checkout, "remote", "add", "origin", other)

    assert update_cmd_git._repoint_origin_to_update_source(["git"], checkout) is False

    assert _git(checkout, "remote", "get-url", "origin") == other
