from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def _package_text(relative_dir: str) -> str:
    """All Kotlin sources of one UI package: the screens were split into several files over time."""
    return "".join(path.read_text(encoding="utf-8") for path in sorted((REPO_ROOT / relative_dir).glob("*.kt")))


def test_manifest_and_chat_use_android_voice_input():
    manifest = (REPO_ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    chat_screen = _package_text("android/app/src/main/java/com/mobilefork/hermesagent/ui/chat")
    speech_controller = (REPO_ROOT / "android/app/src/main/java/com/mobilefork/hermesagent/ui/chat/SpeechInputController.kt").read_text(encoding="utf-8")

    assert 'android.permission.RECORD_AUDIO' in manifest
    assert 'ActivityResultContracts.RequestPermission()' in chat_screen
    assert 'SpeechInputController.buildIntent()' in chat_screen
    assert 'strings.voiceRecognitionUnavailable()' in chat_screen
    assert 'RecognizerIntent.ACTION_RECOGNIZE_SPEECH' in speech_controller


def test_chat_supports_tts_playback_for_assistant_replies():
    chat_screen = _package_text("android/app/src/main/java/com/mobilefork/hermesagent/ui/chat")
    tts_controller = (REPO_ROOT / "android/app/src/main/java/com/mobilefork/hermesagent/ui/chat/HermesTtsController.kt").read_text(encoding="utf-8")
    strings = (REPO_ROOT / "android/app/src/main/java/com/mobilefork/hermesagent/ui/i18n/HermesStrings.kt").read_text(encoding="utf-8")

    assert 'HermesTtsController' in chat_screen
    assert 'AppLanguage.ENGLISH -> "Speak reply"' in strings
    assert 'TextToSpeech' in tts_controller
    assert 'fun speak(text: String): Boolean' in tts_controller
    assert 'textToSpeech.speak' in tts_controller
