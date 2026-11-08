package com.example.utils

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import com.example.data.model.WorkspaceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class AppStrings(
    val language: String = "ro",
    val appName: String = "Brain",
    val navChat: String = "Chat",
    val navLibrary: String = "Librării",
    val navPortfolio: String = "Portofoliu",
    val navCalendar: String = "Calendar",
    val calendar: String = "Calendar",
    val navSpaces: String = "Projects",
    val navSpace: String = "Projects",
    val navSettings: String = "Setări",
    val navDiscover: String = "Descoperă",
    val navModels: String = "Modele Hub",
    val navVoice: String = "Voce",
    val searchPlaceholder: String = "Caută...",
    val navSearchPlaceholder: String = "Caută conversații...",
    val navSearchConversations: String = "Caută conversații...",
    val pinned: String = "Fixate",
    val navCollapseLibrary: String = "Restrânge Librăria",
    val navExpandLibrary: String = "Extinde Librăria",
    val navAllDocuments: String = "Toate Documentele",
    val navNotesMarkdown: String = "Notițe & Markdown",
    val navTask: String = "Sarcini",
    val navExpandChat: String = "Extinde Chat",
    val navNewChat: String = "Conversație Nouă",
    val newSession: String = "Sesiune Nouă",
    val inputPlaceholder: String = "Întreabă-l pe Brain orice sau trimite o comandă...",
    val thinking: String = "Brain gândește...",
    val executingTool: String = "Se execută unealta...",
    val cancel: String = "Anulează",
    val save: String = "Salvează",
    val delete: String = "Șterge",
    val edit: String = "Editează",
    val share: String = "Distribuie",
    val done: String = "Gata",
    val retry: String = "Reîncearcă",
    val close: String = "Închide",
    val settingsTitle: String = "Setări",
    val clearChat: String = "Șterge Conversația",
    val exportChat: String = "Exportă Conversația",
    val memoryRecall: String = "Memorie Activă",
    val liveModel: String = "Model Activ",
    val noEventsToday: String = "Niciun eveniment programat astăzi.",
    val addEvent: String = "Adaugă Eveniment",
    val noDocuments: String = "Niciun document salvat încă.",
    val addDocument: String = "Adaugă Document",
    val generalWorkspace: String = "General",
    val defaultWorkspaceDesc: String = "Spațiul de lucru principal pentru sarcini zilnice și asistență.",
    
    // Calendar strings
    val january: String = "Ianuarie",
    val february: String = "Februarie",
    val march: String = "Martie",
    val april: String = "Aprilie",
    val may: String = "Mai",
    val june: String = "Iunie",
    val july: String = "Iulie",
    val august: String = "August",
    val september: String = "Septembrie",
    val october: String = "Octombrie",
    val november: String = "Noiembrie",
    val december: String = "Decembrie",
    val sun: String = "Dum",
    val mon: String = "Lun",
    val tue: String = "Mar",
    val wed: String = "Mie",
    val thu: String = "Joi",
    val fri: String = "Vin",
    val sat: String = "Sâm",
    val agenda: String = "Agendă",
    val month: String = "Lună",
    val week: String = "Săptămână",
    val day: String = "Zi",
    val search: String = "Caută",
    val liveWeather: String = "Vreme Live",
    val selectCityOrVillage: String = "Selectează Localitate",
    val useGps: String = "Folosește GPS",
    val searchCityVillagePlaceholder: String = "Caută oraș sau sat...",
    val weatherDetails: String = "Detalii Meteo",
    val searchingWeb: String = "Se caută pe web...",
    val changeLocation: String = "Schimbă Locația",
    val meeting: String = "Ședință",
    val work: String = "Muncă",
    val personal: String = "Personal",
    val urgent: String = "Urgent",
    val holiday: String = "Sărbătoare",
    val sync: String = "Sincronizare",
    val review: String = "Revizuire",
    val newEvent: String = "Eveniment Nou",
    val quickSuggestions: String = "Sugestii Rapide",
    val eventTitle: String = "Titlu Eveniment",
    val category: String = "Categorie",
    val startHour: String = "Ora de Început",
    val locationOrMeeting: String = "Locație sau Link Meet",
    val additionalNotes: String = "Notițe Adiționale",

    // Discover & Library strings
    val discover: String = "Descoperă",
    val articleSavedToLibrary: String = "Articol salvat în librărie",
    val listenSummary: String = "Ascultă Rezumat",
    val saveToLibrary: String = "Salvează în Librărie",
    val shareNews: String = "Distribuie Știrea",
    val notes: String = "Notițe",
    val documents: String = "Documente",
    val images: String = "Imagini",
    val video: String = "Video",
    val trash: String = "Coș de Gunoi",
    val noImages: String = "Nicio imagine salvată",
    val noVideos: String = "Niciun videoclip salvat",
    val addImage: String = "Adaugă Imagine",
    val addVideo: String = "Adaugă Video",
    val deleteItemConfirm: String = "Sigur dorești să ștergi acest element?",
    val sprintPriorities: String = "Priorități Sprint & Roadmap",
    val brainstormAi: String = "Brainstorming Funcționalități AI",
    val quickDataAnalysis: String = "Analiză Rapidă Set de Date",
    val quickProjectIdeas: String = "Idei Noi de Proiecte Hermes",
    val spacesTitle: String = "Projects",
    val manageSpaces: String = "Gestionează Projects",
    val askAi: String = "Întreabă AI",
    val askAiAboutDoc: String = "Pune o întrebare despre acest document sau cere un rezumat:",
    val typeMessage: String = "Scrie un mesaj...",
    val send: String = "Trimite",

    // Categories in Settings
    val catModelTitle: String = "Modele & Inteligență",
    val catModelDesc: String = "Cloud LLM, GGUF local pe dispozitiv, setări voce și rutare sarcini.",
    val catAbilitiesTitle: String = "Abilități & Instrumente",
    val catAbilitiesDesc: String = "Unelte autonome, căutare web, execuție cod și skill-uri active.",
    val catConnectionsTitle: String = "Conexiuni & Integrări",
    val catConnectionsDesc: String = "Conturi conectate, chei API externe și servere MCP.",
    val catMemoryTitle: String = "Memorie & Date",
    val catMemoryDesc: String = "Amintiri pe termen lung, fapte memorate, cache și baze de date.",
    val catAccessTitle: String = "Autonomie & Securitate",
    val catAccessDesc: String = "Nivel de autonomie, permisiuni hardware și politici de execuție.",
    val catSystemTitle: String = "Sistem & Diagnostic",
    val catSystemDesc: String = "Telemetrie în timp real, joburi cron în fundal și versiune app.",
    val catAppearanceTitle: String = "Aspect & Personalizare",
    val catAppearanceDesc: String = "Teme vizuale, culori de accent, animații, fonturi și limbă.",

    val groupHermes: String = "Configurare Brain Core",
    val groupApp: String = "Aplicație & Mediu",

    // Sub-items
    val subCloud: String = "Modele Cloud",
    val subCloudDesc: String = "Configurare OpenRouter, Claude, GPT-4o, Gemini, DeepSeek.",
    val subLocal: String = "Modele Locale GGUF",
    val subLocalDesc: String = "Descărcare și rulare modele Llama 3 / Hermes 3 direct pe CPU/NPU.",
    val subVoice: String = "Voce & Sinteză Audio",
    val subVoiceDesc: String = "Motoare neurale ASR și Piper TTS pentru limba română.",
    val subOperationModels: String = "Rutare Modele pe Operațiuni",
    val subOperationModelsDesc: String = "Atribuie modele specifice pentru Coding, Web Search, Documente.",

    val subSkills: String = "Catalog Abilități (Skills)",
    val subSkillsDesc: String = "Activează sau dezactivează uneltele automate ale agentului.",
    val subConnectedAccounts: String = "Conturi Conectate",
    val subConnectedAccountsDesc: String = "Servicii terțe, OAuth și conexiuni externe securizate.",
    val subMemoryFacts: String = "Fapte Memorate & Context",
    val subMemoryFactsDesc: String = "Informații reținute despre preferințele și proiectele tale.",
    val subStorageCache: String = "Stocare & Cache Local",
    val subStorageCacheDesc: String = "Gestiune spațiu de stocare, indexare și curățare fișiere.",
    val subAutonomy: String = "Nivel de Autonomie",
    val subAutonomyDesc: String = "Execuție automată sau cu confirmare prealabilă a pașilor.",
    val subToolsPolicy: String = "Politici de Execuție Unelte",
    val subToolsPolicyDesc: String = "Restricții de acces la fișiere, rețea și comenzi terminal.",
    val subDevicePermissions: String = "Permisiuni Dispozitiv",
    val subDevicePermissionsDesc: String = "Microfon, stocare, notificări și senzori Android.",
    val subSecurityAudit: String = "Audit Securitate & Jurnal",
    val subSecurityAuditDesc: String = "Istoric detaliat al tuturor acțiunilor autonome executate.",
    val subLiveTelemetry: String = "Telemetrie Live",
    val subLiveTelemetryDesc: String = "Monitorizare consum RAM, CPU, latență și tokeni utilizați.",
    val subBackgroundCron: String = "Sarcini Cron în Fundal",
    val subBackgroundCronDesc: String = "Planificator de execuție automată periodică a sarcinilor.",
    val subAbout: String = "Despre Hermes Agent",
    val subAboutDesc: String = "Informații versiune, licență și documentație tehnică.",
    val subTheme: String = "Temă Vizuală",
    val subThemeDesc: String = "Comută între Dark Mode, Light Mode sau adaptiv la sistem.",
    val subAccent: String = "Culoare de Accent",
    val subAccentDesc: String = "Personalizează nuanța principală (Emerald, Cyan, Violet etc.).",
    val subChat: String = "Interfață Chat",
    val subChatDesc: String = "Dimensiune bule, redare Markdown, diagrame Mermaid și carduri A2UI.",
    val subText: String = "Text & Tipografie",
    val subTextDesc: String = "Scară font și lizibilitate pentru ecran.",
    val subAnimations: String = "Animații & Feedback",
    val subAnimationsDesc: String = "Vibrații haptice, sunete de sistem și efecte fluide.",
    val subLanguage: String = "Limbă Interfață",
    val subLanguageDesc: String = "Română, Engleză și alte opțiuni lingvistice.",
    val languageRoTitle: String = "Română",
    val languageRoDesc: String = "Interfață completă în limba română",
    val languageEnTitle: String = "English",
    val languageEnDesc: String = "Standard English interface",
    val chooseLanguage: String = "Alege Limba",
)

data class WorkspaceDisplayInfo(
    val profession: String,
    val customName: String? = null
)

object WorkspaceDisplayUtils {
    fun getDisplayInfo(ws: WorkspaceProfile): WorkspaceDisplayInfo {
        val prof = if (ws.agentRole.isNotBlank()) ws.agentRole else ws.name
        val custom = if (ws.name != ws.agentRole && ws.name.isNotBlank()) ws.name else null
        return WorkspaceDisplayInfo(profession = prof, customName = custom)
    }
}

val LocalAppStrings = compositionLocalOf { AppStrings() }

fun getAppStrings(language: String = "ro"): AppStrings {
    return when (language.lowercase()) {
        "en" -> AppStrings(
            language = "en",
            appName = "Brain",
            navChat = "Chat",
            navLibrary = "Libraries",
            navPortfolio = "Portfolio",
            navCalendar = "Calendar",
            calendar = "Calendar",
            navSpaces = "Projects",
            navSpace = "Projects",
            navSettings = "Settings",
            navDiscover = "Discover",
            navModels = "Models Hub",
            navVoice = "Voice",
            searchPlaceholder = "Search...",
            navSearchPlaceholder = "Search conversations...",
            navSearchConversations = "Search conversations...",
            pinned = "Pinned",
            navCollapseLibrary = "Collapse Library",
            navExpandLibrary = "Expand Library",
            navAllDocuments = "All Documents",
            navNotesMarkdown = "Notes & Markdown",
            navTask = "Tasks",
            navExpandChat = "Expand Chat",
            navNewChat = "New Chat",
            newSession = "New Session",
            inputPlaceholder = "Ask Brain anything or give a task...",
            thinking = "Brain is thinking...",
            executingTool = "Executing tool...",
            cancel = "Cancel",
            save = "Save",
            delete = "Delete",
            edit = "Edit",
            share = "Share",
            done = "Done",
            retry = "Retry",
            close = "Close",
            generalWorkspace = "General",
            defaultWorkspaceDesc = "Main workspace for general tasks and autonomous assistance.",
            spacesTitle = "Projects",
            manageSpaces = "Manage Projects",
        )
        else -> AppStrings()
    }
}

class VoiceRecognitionManager(private val context: Context) {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceIntensity = MutableStateFlow(0f)
    val voiceIntensity: StateFlow<Float> = _voiceIntensity.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startListening(
        language: String = "ro-RO",
        provider: String = "system",
        apiKey: String = "",
        customEndpoint: String = "",
        customModel: String = "",
        endpoint: String = "",
        model: String = "",
        openAiKey: String = "",
        onResult: (String) -> Unit = {}
    ) {
        _isListening.value = true
        _errorMessage.value = null
        _partialText.value = "Listening..."
    }

    fun stopListening() {
        _isListening.value = false
    }

    fun cancelListening() {
        stopListening()
    }
}
