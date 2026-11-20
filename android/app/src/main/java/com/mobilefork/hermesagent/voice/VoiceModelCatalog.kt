package com.mobilefork.hermesagent.voice

/** The kinds of model the voice manager offers. Each one runs on the phone through sherpa-onnx. */
enum class VoiceFamily { Vits, Kokoro, Supertonic, Pocket, ZipVoice, SileroVad, TenVad }

/**
 * One thing to download for a model: a single file, or a `.tar.bz2` ([extract]) whose one top-level
 * folder is opened into the model's own folder. The size and SHA-256 are those of the official file
 * and are checked before anything is used.
 */
internal class VoiceDownload(
    val fileName: String,
    val url: String,
    val bytes: Long,
    val sha256: String,
    val extract: Boolean,
)

/** A voice or detection model the person can download, use and remove. */
class VoiceModelSpec internal constructor(
    val id: String,
    val name: String,
    val family: VoiceFamily,
    val summary: String,
    /** ISO 639-1 codes of the languages the model speaks. Empty for a detection model. */
    val languages: Set<String>,
    /** Speaks in the voice of a short recording of the person. */
    val canClone: Boolean,
    /** Cloning also needs the words said in that recording. */
    val needsReferenceText: Boolean,
    /** Shown wherever the model is offered, as the licences ask. */
    val credit: String,
    internal val downloads: List<VoiceDownload>,
    /** The names of the voices inside the model, in the order the model numbers them. */
    val voiceNames: List<String> = listOf("Default"),
) {
    val downloadBytes: Long = downloads.sumOf { it.bytes }
    val downloadMegabytes: Int get() = ((downloadBytes + 500_000L) / 1_000_000L).toInt()
    val isVoice: Boolean get() = family != VoiceFamily.SileroVad && family != VoiceFamily.TenVad

    fun speaks(languageTag: String): Boolean {
        val code = languageTag.substringBefore('-').substringBefore('_').lowercase()
        return code.isEmpty() || code in languages
    }
}

/**
 * Five voices and two speech detectors. Every URL is a fixed release asset, never "latest", so the
 * size and hash listed here are exactly what is downloaded. Several voices together stay far below 1 GB
 * each; only one voice is loaded in memory at a time.
 */
object VoiceModelCatalog {
    private const val TTS = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

    /** Kokoro v1.0's voices in the order the model numbers them (speaker 0 to 53). */
    private val KOKORO_VOICES = listOf(
        "af_alloy", "af_aoede", "af_bella", "af_heart", "af_jessica", "af_kore", "af_nicole", "af_nova", "af_river",
        "af_sarah", "af_sky", "am_adam", "am_echo", "am_eric", "am_fenrir", "am_liam", "am_michael", "am_onyx",
        "am_puck", "am_santa", "bf_alice", "bf_emma", "bf_isabella", "bf_lily", "bm_daniel", "bm_fable", "bm_george",
        "bm_lewis", "ef_dora", "em_alex", "ff_siwis", "hf_alpha", "hf_beta", "hm_omega", "hm_psi", "if_sara",
        "im_nicola", "jf_alpha", "jf_gongitsune", "jf_nezumi", "jf_tebukuro", "jm_kumo", "pf_dora", "pm_alex",
        "pm_santa", "zf_xiaobei", "zf_xiaoni", "zf_xiaoxiao", "zf_xiaoyi", "zm_yunjian", "zm_yunxi", "zm_yunxia",
        "zm_yunyang", "em_santa",
    )

    /** "af_heart" reads as "Heart (American, female)". */
    internal fun kokoroVoiceLabel(id: String): String {
        val accent = when (id.first()) {
            'a' -> "American"
            'b' -> "British"
            'e' -> "Spanish"
            'f' -> "French"
            'h' -> "Hindi"
            'i' -> "Italian"
            'j' -> "Japanese"
            'p' -> "Portuguese"
            'z' -> "Chinese"
            else -> ""
        }
        val gender = if (id.getOrNull(1) == 'f') "female" else "male"
        return "${id.substringAfter('_').replaceFirstChar { it.uppercase() }} ($accent, $gender)"
    }

    val voices: List<VoiceModelSpec> = listOf(
        VoiceModelSpec(
            id = "piper-ro-mihai",
            name = "Piper, Romanian (Mihai)",
            family = VoiceFamily.Vits,
            summary = "Small and fast. The sure way to hear Romanian, with one clear male voice.",
            languages = setOf("ro"),
            canClone = false,
            needsReferenceText = false,
            credit = "Voice: Piper ro_RO Mihai, trained on CC0 data by the Piper project (rhasspy / Open Home Foundation).",
            downloads = listOf(
                VoiceDownload(
                    "vits-piper-ro_RO-mihai-medium.tar.bz2", "$TTS/vits-piper-ro_RO-mihai-medium.tar.bz2",
                    67_182_137L, "cfbee3cdc25cf063339f69e189f4d93cc3be6610d267567537323ced3cf87f4d", extract = true,
                ),
            ),
            voiceNames = listOf("Mihai"),
        ),
        VoiceModelSpec(
            id = "supertonic-3",
            name = "Supertonic 3",
            family = VoiceFamily.Supertonic,
            summary = "Fast and natural, 10 voices, 31 languages including Romanian and English.",
            languages = setOf(
                "ar", "bg", "hr", "cs", "da", "nl", "en", "et", "fi", "fr", "de", "el", "hi", "hu", "id", "it", "ja",
                "ko", "lv", "lt", "pl", "pt", "ro", "ru", "sk", "sl", "es", "sv", "tr", "uk", "vi",
            ),
            canClone = false,
            needsReferenceText = false,
            credit = "Voice: Supertonic 3 by Supertone Inc. (code MIT, model OpenRAIL), converted by the sherpa-onnx project.",
            downloads = listOf(
                VoiceDownload(
                    "sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2", "$TTS/sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2",
                    128_774_318L, "82fa96f91c4ef8abaae3a14a3f4153facf88bed821d1f7331cec2700f432c427", extract = true,
                ),
            ),
            voiceNames = (1..10).map { "Voice $it" },
        ),
        VoiceModelSpec(
            id = "kokoro-82m",
            name = "Kokoro 82M",
            family = VoiceFamily.Kokoro,
            summary = "The most natural English voices of the small models, 54 voices. No Romanian.",
            languages = setOf("en", "fr", "es", "it", "pt", "zh", "ja", "hi"),
            canClone = false,
            needsReferenceText = false,
            credit = "Voice: Kokoro 82M v1.0 (Apache 2.0), converted by the sherpa-onnx project.",
            downloads = listOf(
                VoiceDownload(
                    "kokoro-int8-multi-lang-v1_0.tar.bz2", "$TTS/kokoro-int8-multi-lang-v1_0.tar.bz2",
                    132_353_410L, "72c347230107ef8a31098ec4d468a605244beef5f1ed00db97efa13518800653", extract = true,
                ),
            ),
            voiceNames = KOKORO_VOICES.map { kokoroVoiceLabel(it) },
        ),
        VoiceModelSpec(
            id = "pocket-tts",
            name = "Pocket TTS (your voice)",
            family = VoiceFamily.Pocket,
            summary = "Speaks in your voice from a 10 second recording. English only.",
            languages = setOf("en"),
            canClone = true,
            needsReferenceText = false,
            credit = "Voice: Pocket TTS by Kyutai (CC BY 4.0). The ONNX export it runs from is for non-commercial use only.",
            downloads = listOf(
                VoiceDownload(
                    "sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2", "$TTS/sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2",
                    98_336_520L, "2f3b88823cbbb9bf0b2477ec8ae7b3fec417b3a87b6bb5f256dba66f2ad967cb", extract = true,
                ),
            ),
        ),
        VoiceModelSpec(
            id = "zipvoice",
            name = "ZipVoice (largest, experimental)",
            family = VoiceFamily.ZipVoice,
            summary = "The biggest model here, about 480 MB. Copies the voice in your recording, and needs the words you said. Best in Chinese; English works but can cut a sentence short.",
            languages = setOf("en", "zh"),
            canClone = true,
            needsReferenceText = true,
            credit = "Voice: ZipVoice distilled (k2-fsa, Apache 2.0) with the Vocos vocoder, converted by the sherpa-onnx project.",
            downloads = listOf(
                VoiceDownload(
                    "sherpa-onnx-zipvoice-distill-fp32-zh-en-emilia.tar.bz2", "$TTS/sherpa-onnx-zipvoice-distill-fp32-zh-en-emilia.tar.bz2",
                    477_800_463L, "3b6729d03bf4ba64deeec113048bdbbe55dbc30580b609be76342e0099fd23a8", extract = true,
                ),
                VoiceDownload(
                    "vocos_24khz.onnx", "https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos_24khz.onnx",
                    54_157_409L, "bcb3b970e384161c4d634f0bb9e999ff1c471b34c9bc0b1049a5014065ed3cc0", extract = false,
                ),
            ),
        ),
    )

    /** Notices when the person starts and stops speaking. A few hundred kilobytes each. */
    val detectors: List<VoiceModelSpec> = listOf(
        VoiceModelSpec(
            id = "silero-vad",
            name = "Silero VAD",
            family = VoiceFamily.SileroVad,
            summary = "The proven choice for hearing when you start and stop speaking.",
            languages = emptySet(),
            canClone = false,
            needsReferenceText = false,
            credit = "Speech detection: Silero VAD (MIT).",
            downloads = listOf(
                VoiceDownload(
                    "silero_vad.onnx", "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx",
                    643_854L, "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6", extract = false,
                ),
            ),
        ),
        VoiceModelSpec(
            id = "ten-vad",
            name = "TEN VAD",
            family = VoiceFamily.TenVad,
            summary = "Even smaller, and quicker to notice the first word.",
            languages = emptySet(),
            canClone = false,
            needsReferenceText = false,
            credit = "Speech detection: TEN VAD (TEN framework).",
            downloads = listOf(
                VoiceDownload(
                    "ten-vad.onnx", "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/ten-vad.onnx",
                    332_211L, "718cb7eef47e3cf5ddbe7e967a7503f46b8b469c0706872f494dfa921b486206", extract = false,
                ),
            ),
        ),
    )

    val all: List<VoiceModelSpec> = voices + detectors

    fun find(id: String): VoiceModelSpec? = all.firstOrNull { it.id == id }
}
