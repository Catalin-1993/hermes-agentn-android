package com.mobilefork.hermesagent.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/** Words of the empty chat's greeting and of the "+" sheet's newer rows. */
data class ChatStrings(
    // greeting by the hour: a short, casual line, then the name, then a short question
    val greetingNight: List<String> = listOf("Up late?", "Still up?"),
    val greetingMorning: List<String> = listOf("Good morning", "Morning"),
    val greetingDay: List<String> = listOf("Good day", "Hey", "What's up"),
    val greetingEvening: List<String> = listOf("Good evening", "Evening"),
    val questions: List<String> = listOf("What are we working on?", "What's on your mind?", "How can I help today?"),
    val groupQuestion: String = "Say something to the group.",
    // "+" sheet
    val tileFiles: String = "Files",
    val tileLibrary: String = "Library",
    val researchTitle: String = "Research",
    val researchBody: String = "A fuller search across several sources",
    val thinkingTitle: String = "Thinking",
    val thinkingLevels: List<String> = listOf("Auto", "Low", "Medium", "High"),
    val agentsTitle: String = "Agents",
    val agentsBody: String = "Start a group chat",
    val agentsPick: String = "Pick the agents to invite",
    val agentsStartGroup: String = "Start group chat",
    val agentsAddToGroup: String = "Add to the group",
    val agentsNone: String = "Create another agent first, then invite it here.",
    val libraryPickTitle: String = "Attach from the library",
    val libraryEmpty: String = "The library is empty.",
    val libraryKindNote: String = "Note",
    val libraryKindDocument: String = "Document",
    val libraryKindImage: String = "Image",
    // problems
    val cameraPermission: String = "Allow the camera to take a photo.",
    val cameraUnavailable: String = "No camera app could be opened.",
    val fileUnsupported: String = "This kind of file cannot be read yet. Text files and images work.",
    val fileTooBig: String = "That file is too large to attach.",
    val fileAttached: String = "File attached",
    val researchOnStatus: String = "Research is on",
)

private val englishChatStrings = ChatStrings()

private val romanianChatStrings = ChatStrings(
    greetingNight = listOf("Încă treaz?", "Noapte lungă?"),
    greetingMorning = listOf("Bună dimineața", "Bună"),
    greetingDay = listOf("Bună ziua", "Salut", "Ce faci"),
    greetingEvening = listOf("Bună seara", "Salut"),
    questions = listOf("La ce lucrăm azi?", "Ce ai în minte?", "Cu ce te pot ajuta?"),
    groupQuestion = "Scrie-le ceva celor din grup.",
    tileFiles = "Fișiere",
    tileLibrary = "Librării",
    researchTitle = "Research",
    researchBody = "O căutare mai completă, din mai multe surse",
    thinkingTitle = "Thinking",
    thinkingLevels = listOf("Auto", "Redus", "Mediu", "Intens"),
    agentsTitle = "Agenți",
    agentsBody = "Pornește o conversație de grup",
    agentsPick = "Alege agenții pe care îi inviți",
    agentsStartGroup = "Pornește conversația de grup",
    agentsAddToGroup = "Adaugă în grup",
    agentsNone = "Creează mai întâi un alt agent, apoi invită-l de aici.",
    libraryPickTitle = "Atașează din librărie",
    libraryEmpty = "Librăria este goală.",
    libraryKindNote = "Notiță",
    libraryKindDocument = "Document",
    libraryKindImage = "Imagine",
    cameraPermission = "Permite camera ca să faci o poză.",
    cameraUnavailable = "Nicio aplicație de cameră nu a putut fi deschisă.",
    fileUnsupported = "Acest tip de fișier nu poate fi citit încă. Merg fișierele text și imaginile.",
    fileTooBig = "Fișierul este prea mare ca să fie atașat.",
    fileAttached = "Fișier atașat",
    researchOnStatus = "Research este activ",
)

private val germanChatStrings = ChatStrings(
    greetingNight = listOf("Noch wach?", "Lange Nacht?"),
    greetingMorning = listOf("Guten Morgen", "Morgen"),
    greetingDay = listOf("Guten Tag", "Hallo", "Was geht"),
    greetingEvening = listOf("Guten Abend", "Abend"),
    questions = listOf("Woran arbeiten wir?", "Was beschäftigt dich?", "Wobei kann ich helfen?"),
    groupQuestion = "Schreib der Gruppe etwas.",
    tileFiles = "Dateien",
    tileLibrary = "Bibliothek",
    researchTitle = "Research",
    researchBody = "Eine gründlichere Suche in mehreren Quellen",
    thinkingTitle = "Thinking",
    thinkingLevels = listOf("Auto", "Niedrig", "Mittel", "Hoch"),
    agentsTitle = "Agenten",
    agentsBody = "Gruppenchat starten",
    agentsPick = "Wähle die Agenten zum Einladen",
    agentsStartGroup = "Gruppenchat starten",
    agentsAddToGroup = "Zur Gruppe hinzufügen",
    agentsNone = "Erstelle zuerst einen weiteren Agenten und lade ihn dann hier ein.",
    libraryPickTitle = "Aus der Bibliothek anhängen",
    libraryEmpty = "Die Bibliothek ist leer.",
    libraryKindNote = "Notiz",
    libraryKindDocument = "Dokument",
    libraryKindImage = "Bild",
    cameraPermission = "Erlaube die Kamera, um ein Foto aufzunehmen.",
    cameraUnavailable = "Es konnte keine Kamera-App geöffnet werden.",
    fileUnsupported = "Dieser Dateityp kann noch nicht gelesen werden. Textdateien und Bilder funktionieren.",
    fileTooBig = "Die Datei ist zu groß zum Anhängen.",
    fileAttached = "Datei angehängt",
    researchOnStatus = "Research ist an",
)

fun chatStringsFor(languageTag: String?): ChatStrings = when (normalizeUiLanguageTag(languageTag)) {
    UiLanguage.ROMANIAN.tag -> romanianChatStrings
    UiLanguage.GERMAN.tag -> germanChatStrings
    else -> englishChatStrings
}

val LocalChatStrings = staticCompositionLocalOf { englishChatStrings }

/** The greeting for an hour of the day; [seed] picks one of the variants, so it holds still while the page is open. */
fun ChatStrings.greetingFor(hour: Int, seed: Int): String {
    val options = when (hour) {
        in 5..11 -> greetingMorning
        in 12..17 -> greetingDay
        in 18..22 -> greetingEvening
        else -> greetingNight
    }
    return options[Math.floorMod(seed, options.size)]
}
