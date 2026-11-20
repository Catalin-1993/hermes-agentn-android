from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def _package_text(relative_dir: str) -> str:
    """All Kotlin sources of one UI package: the screens were split into several files over time."""
    return "".join(path.read_text(encoding="utf-8") for path in sorted((REPO_ROOT / relative_dir).glob("*.kt")))


def test_app_shell_uses_drawer_navigation_and_context_sheet():
    app_shell = _package_text("android/app/src/main/java/com/mobilefork/hermesagent/ui/shell")
    shell_models = (REPO_ROOT / "android/app/src/main/java/com/mobilefork/hermesagent/ui/shell/ShellModels.kt").read_text(encoding="utf-8")
    action_sheet = (REPO_ROOT / "android/app/src/main/java/com/mobilefork/hermesagent/ui/shell/ContextActionSheet.kt").read_text(encoding="utf-8")

    assert 'Scaffold(' in app_shell
    assert 'NavigationBar(' not in app_shell
    assert 'FloatingActionButton' not in app_shell
    assert 'currentSection != AppSection.Hermes' in app_shell
    assert 'ContextActionSheet(' in app_shell
    assert 'TabRow' not in app_shell
    assert 'Portal(' in shell_models
    assert 'iconRes = R.drawable.ic_nav_agent' in shell_models
    assert 'ModalBottomSheet' in action_sheet
    assert 'LazyColumn(' in action_sheet
    assert 'navigationBarsPadding()' in action_sheet


def test_shell_branding_and_settings_page_hide_context_actions():
    app_shell = _package_text("android/app/src/main/java/com/mobilefork/hermesagent/ui/shell")

    assert 'setActions(emptyList())' in app_shell
    assert 'onOpenContextActions = { showActionSheet = true }' in app_shell
