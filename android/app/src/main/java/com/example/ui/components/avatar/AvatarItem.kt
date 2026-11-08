package com.example.ui.components.avatar

import com.mobilefork.hermesagent.R

data class AvatarFeatureTag(
    val label: String,
    val iconKey: String
)

data class AvatarProfile(
    val id: String,
    val name: String,
    val title: String,
    val drawableRes: Int,
    val description: String,
    val isRealPerson: Boolean = false,
    val specialty: String,
    val accentColorHex: String = "#38BDF8",
    val isMascot: Boolean = true,
    val personalityTag: String = "Companion",
    val defaultActivity: AvatarActivity = AvatarActivity.Idle,
    val is4K: Boolean = true,
    val communicationStyle: String = "Natural, empatic și orientat spre soluții",
    val voiceTone: String = "Voce caldă, echilibrată și profesionistă",
    val brainCategory: String = "BRAIN / COMPANION",
    val badgeNumber: Int = 1,
    val featureTags: List<AvatarFeatureTag> = emptyList(),
    val chatGreetingCategory: String = "Bucătar • Chef Assistant",
    val chatGreetingHeadline: String = "Ce preparat delicios vrei să gătim astăzi?",
    val chatGreetingSubtitle: String = "Îți pot oferi rețete, sfaturi culinare, planuri nutriționale și idei pentru meniul tău.",
    val categoryTab: String = "Profesii",
    val suggestedPrompts: List<String> = emptyList()
) {
    companion object {
        val ALL_AVATARS = listOf(
            // 1. Bucătar (Chef Assistant) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_chef",
                name = "Bucătar",
                title = "Chef & Gastronomie",
                drawableRes = R.drawable.img_avatar_chef,
                description = "Idei delicioase, secrete culinare și recomandări pentru fiecare masă.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Rețete • Meniuri • Gust",
                accentColorHex = "#F59E0B",
                personalityTag = "Pasionat & Creativ",
                defaultActivity = AvatarActivity.BuildingTools,
                is4K = true,
                communicationStyle = "Cald, practic și pasionat de arome",
                voiceTone = "Ton entuziast, generos și cald",
                brainCategory = "PROFESII / BUCĂTAR",
                badgeNumber = 1,
                featureTags = listOf(
                    AvatarFeatureTag("Rețete", "restaurant"),
                    AvatarFeatureTag("Meniuri", "menu_book"),
                    AvatarFeatureTag("Gust", "star")
                ),
                chatGreetingCategory = "Bucătar • Chef Assistant",
                chatGreetingHeadline = "Ce preparat delicios vrei să gătim astăzi?",
                chatGreetingSubtitle = "Îți pot oferi rețete, sfaturi culinare, planuri nutriționale și idei pentru meniul tău.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Rețetă rapidă cu ce am în frigider 🍳",
                    "Meniu sănătos pentru toată săptămâna 🥗",
                    "Secrete pentru friptură suculentă și fragedă 🥩"
                )
            ),

            // 2. Consultant Financiar (Financial Advisor) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_financial",
                name = "Consultant Financiar",
                title = "Expert Financiar & Investiții",
                drawableRes = R.drawable.img_avatar_financial,
                description = "Îți construiește un viitor financiar sigur prin bugete și investiții inteligente.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Planificare • Investiții • Economisire",
                accentColorHex = "#10B981",
                personalityTag = "Strategic & Calculat",
                defaultActivity = AvatarActivity.Thinking,
                is4K = true,
                communicationStyle = "Clar, structurat și orientat pe cifre",
                voiceTone = "Ton sigur, profesionist și calm",
                brainCategory = "PROFESII / FINANȚE",
                badgeNumber = 2,
                featureTags = listOf(
                    AvatarFeatureTag("Investiții", "trending_up"),
                    AvatarFeatureTag("Planificare", "schedule"),
                    AvatarFeatureTag("Economii", "account_balance")
                ),
                chatGreetingCategory = "Finanțe • Financial Advisor",
                chatGreetingHeadline = "Ce obiective financiare vrei să analizăm?",
                chatGreetingSubtitle = "Îți pot crea planuri de investiții, analize de portofoliu, strategii de economisire și bugete.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Plan de economisire lunar și buget 📊",
                    "Ghid pentru primele mele investiții inteligente 📈",
                    "Cum îmi optimizez cheltuielile zilnice 💡"
                )
            ),

            // 3. Psiholog (Psychologist) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_psychologist",
                name = "Psiholog",
                title = "Echilibru & Wellness",
                drawableRes = R.drawable.img_avatar_psychologist,
                description = "Te ajută să-ți înțelegi emoțiile, să reduci stresul și să găsești claritate interioară.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Echilibru • Ascultare • Sprijin",
                accentColorHex = "#EC4899",
                personalityTag = "Empatic & Înțelegător",
                defaultActivity = AvatarActivity.Idle,
                is4K = true,
                communicationStyle = "Blând, atent și suportiv fără judecată",
                voiceTone = "Ton calm, reconfortant și profund",
                brainCategory = "PROFESII / PSIHOLOG",
                badgeNumber = 3,
                featureTags = listOf(
                    AvatarFeatureTag("Ascultare", "favorite"),
                    AvatarFeatureTag("Echilibru", "psychology"),
                    AvatarFeatureTag("Sprijin", "group")
                ),
                chatGreetingCategory = "Psihologie • Mental Health & Wellness",
                chatGreetingHeadline = "Cum te simți astăzi și despre ce vrei să vorbim?",
                chatGreetingSubtitle = "Sunt aici să te ascult cu empatie, să-ți ofer perspective calde și să te sprijin în orice moment.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Mă simt copleșit de stres și agitație 🌿",
                    "Cum să îmi îmbunătățesc somnul și calmul 🌙",
                    "Exercițiu ghidat de relaxare și respirație 🧘"
                )
            ),

            // 4. Medic (Doctor) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_doctor",
                name = "Medic",
                title = "Sănătate & Prevenție",
                drawableRes = R.drawable.img_avatar_doctor,
                description = "Ai grijă de sănătatea ta cu recomandări preventive și informații medicale de încredere.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Sănătate • Prevenție • Sfaturi",
                accentColorHex = "#06B6D4",
                personalityTag = "Devotat & Precis",
                defaultActivity = AvatarActivity.ReadingBook,
                is4K = true,
                communicationStyle = "Riguros, empatic și preventiv",
                voiceTone = "Ton liniștitor, expert și clar",
                brainCategory = "PROFESII / MEDIC",
                badgeNumber = 4,
                featureTags = listOf(
                    AvatarFeatureTag("Prevenție", "check_circle"),
                    AvatarFeatureTag("Sănătate", "local_hospital"),
                    AvatarFeatureTag("Ghidare", "info")
                ),
                chatGreetingCategory = "Medic • Health & Medical Guide",
                chatGreetingHeadline = "Cu ce informații medicale te pot ajuta astăzi?",
                chatGreetingSubtitle = "Ghidare preventivă, clarificări despre sănătate, nutriție și stare generală de bine.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Sfaturi pentru imunitate puternică 🍎",
                    "Cum să combat oboseala cronică zilnică ⚡",
                    "Ghid analize uzuale de prevenție 🩺"
                )
            ),

            // 5. Avocat (Legal Advisor) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_lawyer",
                name = "Avocat",
                title = "Consiliere Juridică",
                drawableRes = R.drawable.img_avatar_lawyer,
                description = "Explică legea pe înțelesul tău, analizează documente și te ghidează corect.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Drept • Consiliere • Analiză",
                accentColorHex = "#D97706",
                personalityTag = "Eloquent & Meticulos",
                defaultActivity = AvatarActivity.ReadingBook,
                is4K = true,
                communicationStyle = "Articulat, precis și ferm",
                voiceTone = "Ton distins, documentat și persuasiv",
                brainCategory = "PROFESII / AVOCAT",
                badgeNumber = 5,
                featureTags = listOf(
                    AvatarFeatureTag("Drept", "gavel"),
                    AvatarFeatureTag("Analiză", "assignment"),
                    AvatarFeatureTag("Consiliere", "school")
                ),
                chatGreetingCategory = "Drept • Legal Counsel",
                chatGreetingHeadline = "Ce aspect juridic sau document vrei să analizăm?",
                chatGreetingSubtitle = "Consiliere structurată, sinteze legislative, analiză de contracte și îndrumare clară.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Verificare clauze esențiale într-un contract ⚖️",
                    "Drepturile mele legale de consumator 📜",
                    "Pași esențiali pentru deschiderea unei afaceri 🏛️"
                )
            ),

            // 6. The Fox (Vulpea) - Photorealistic Studio Cutout, No Background
            AvatarProfile(
                id = "avatar_fox",
                name = "The Fox",
                title = "Strategic & Creative",
                drawableRes = R.drawable.img_avatar_fox,
                description = "Soluții perspicace, analiză ageră și abordare creativă pentru orice proiect.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Strategie, creativitate & soluții perspicace",
                accentColorHex = "#38BDF8",
                personalityTag = "Strategic & Creativ",
                defaultActivity = AvatarActivity.Thinking,
                is4K = true,
                communicationStyle = "Perspicace, strategic și inovator",
                voiceTone = "Ton ager, cald și sigur pe sine",
                brainCategory = "BRAIN / THE FOX",
                badgeNumber = 6,
                featureTags = listOf(
                    AvatarFeatureTag("Light bulb", "lightbulb"),
                    AvatarFeatureTag("Network", "network"),
                    AvatarFeatureTag("Clock", "clock")
                ),
                chatGreetingCategory = "Brain • Strategic & Creative",
                chatGreetingHeadline = "Ce provocare strategică abordăm astăzi?",
                chatGreetingSubtitle = "Soluții perspicace, planificare rapidă și viziune inovatoare pentru ideile tale.",
                categoryTab = "Animale & 3D",
                suggestedPrompts = listOf(
                    "Plan strategic și pași de acțiune pentru ideea mea 🦊",
                    "Brainstorming 5 idei inovatoare de creștere 💡",
                    "Cum optimizez timpul și prioritățile de lucru ⏱️"
                )
            ),

            // 7. The Panda (Panda) - Photorealistic Studio Cutout, No Background
            AvatarProfile(
                id = "avatar_panda",
                name = "The Panda",
                title = "Collaboration & Management",
                drawableRes = R.drawable.img_avatar_panda,
                description = "Organizează sarcinile, reduce stresul și aduce armonie în fiecare zi.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Colaborare, organizare & armonie",
                accentColorHex = "#10B981",
                personalityTag = "Calm & Prietenos",
                defaultActivity = AvatarActivity.BuildingTools,
                is4K = true,
                communicationStyle = "Calm, organizat și cooperant",
                voiceTone = "Ton blând, relaxant și cooperativ",
                brainCategory = "BRAIN / THE PANDA",
                badgeNumber = 7,
                featureTags = listOf(
                    AvatarFeatureTag("People", "people"),
                    AvatarFeatureTag("Organization", "organization"),
                    AvatarFeatureTag("Graph", "graph")
                ),
                chatGreetingCategory = "Brain • Collaboration & Harmony",
                chatGreetingHeadline = "Cum îți pot organiza proiectele astăzi?",
                chatGreetingSubtitle = "Gestionare eficientă a sarcinilor, calm și armonie în fiecare activitate.",
                categoryTab = "Animale & 3D",
                suggestedPrompts = listOf(
                    "Organizează-mi taskurile pe priorități clare 🐼",
                    "Plan de colaborare și comunicare în echipă 🤝",
                    "Cum să îmi păstrez calmul în momente aglomerate 🎋"
                )
            ),

            // 8. The Owl (Bufnița) - Photorealistic Studio Cutout, No Background
            AvatarProfile(
                id = "avatar_owl",
                name = "The Owl",
                title = "Knowledge & Guidance",
                drawableRes = R.drawable.img_avatar_owl,
                description = "Înțelepciune profundă, învățare metodică și răspunsuri bine documentate.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Cunoaștere, ghidare & cercetare aprofundată",
                accentColorHex = "#F59E0B",
                personalityTag = "Înțelept & Metodic",
                defaultActivity = AvatarActivity.ReadingBook,
                is4K = true,
                communicationStyle = "Structurat, profund și pedagogic",
                voiceTone = "Ton calm, nobil și documentat",
                brainCategory = "BRAIN / THE OWL",
                badgeNumber = 8,
                featureTags = listOf(
                    AvatarFeatureTag("Brain cell", "brain"),
                    AvatarFeatureTag("Mortarboard", "mortarboard"),
                    AvatarFeatureTag("Links", "links")
                ),
                chatGreetingCategory = "Brain • Knowledge & Guidance",
                chatGreetingHeadline = "Ce domeniu de cunoaștere aprofundăm astăzi?",
                chatGreetingSubtitle = "Ghidare temeinică, sinteză critică și cercetare riguroasă pentru deciziile tale.",
                categoryTab = "Animale & 3D",
                suggestedPrompts = listOf(
                    "Ghid complet și structurat de învățare rapidă 🦉",
                    "Analiză aprofundată a unui subiect complex 📖",
                    "Metodă eficientă de luare a deciziilor critice 🔍"
                )
            ),

            // 9. Marcus Tech (Coding Raccoon) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_coding",
                name = "Marcus Tech",
                title = "Systems Architect & Developer",
                drawableRes = R.drawable.img_avatar_coding,
                description = "Inginerie software, debugging avansat și arhitectură de sistem.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Cod • Arhitectură • Debugging",
                accentColorHex = "#3B82F6",
                personalityTag = "Ager & Analitic",
                defaultActivity = AvatarActivity.WritingCode,
                is4K = true,
                communicationStyle = "Pragmatic, structurat și orientat pe clean code",
                voiceTone = "Ton energic, clar și precis",
                brainCategory = "PROFESII / DEVOLOPER",
                badgeNumber = 9,
                featureTags = listOf(
                    AvatarFeatureTag("Cod", "code"),
                    AvatarFeatureTag("Arhitectură", "memory"),
                    AvatarFeatureTag("Terminal", "terminal")
                ),
                chatGreetingCategory = "Tech • Systems Architect",
                chatGreetingHeadline = "Ce proiect sau cod dezvoltăm astăzi?",
                chatGreetingSubtitle = "Scriere de cod curat, rezolvare de bug-uri, optimizări de performanță și soluții tehnice.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Analiză și optimizare cod Kotlin / Python 💻",
                    "Arhitectură scalabilă pentru aplicația mea 🏗️",
                    "Rezolvare bug-uri și refactoring de performanță ⚡"
                )
            ),

            // 10. Leo Design (Creative Lion) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_design",
                name = "Leo Design",
                title = "Creative UI/UX Director",
                drawableRes = R.drawable.img_avatar_design,
                description = "Interfețe elegante, experiență intuitivă și concepte vizuale premium.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Design UI/UX • Identitate • Estetică",
                accentColorHex = "#8B5CF6",
                personalityTag = "Vizionar & Creativ",
                defaultActivity = AvatarActivity.Idle,
                is4K = true,
                communicationStyle = "Expresiv, empatic și orientat spre estetică",
                voiceTone = "Ton inspirațional, cald și rafinat",
                brainCategory = "PROFESII / DESIGN",
                badgeNumber = 10,
                featureTags = listOf(
                    AvatarFeatureTag("UI/UX", "palette"),
                    AvatarFeatureTag("Identitate", "brush"),
                    AvatarFeatureTag("Estetică", "auto_awesome")
                ),
                chatGreetingCategory = "Design • Creative Director",
                chatGreetingHeadline = "Ce concept vizual conturăm astăzi?",
                chatGreetingSubtitle = "Sisteme de design, prototipuri UI/UX, palete de culori și identitate vizuală captivantă.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Ghid de culori și tipografie pentru aplicație 🎨",
                    "Feedback pe design UI/UX și accesibilitate 👁️",
                    "Idei de layout Material Design 3 modern 📐"
                )
            ),

            // 11. Elena Writing (Writer Koala) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_writing",
                name = "Elena Writing",
                title = "Chief Editor & Copywriter",
                drawableRes = R.drawable.img_avatar_writing,
                description = "Redactare persuasivă, rafinament stilistic și documentație tehnică impecabilă.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Copywriting • Editorial • Rafinament",
                accentColorHex = "#F43F5E",
                personalityTag = "Elovent & Cald",
                defaultActivity = AvatarActivity.ReadingBook,
                is4K = true,
                communicationStyle = "Cursiv, impecabil și captivant",
                voiceTone = "Ton melodios, articulat și primitor",
                brainCategory = "PROFESII / WRITING",
                badgeNumber = 11,
                featureTags = listOf(
                    AvatarFeatureTag("Copywriting", "edit"),
                    AvatarFeatureTag("Editorial", "article"),
                    AvatarFeatureTag("Stil", "draw")
                ),
                chatGreetingCategory = "Writing • Chief Editor",
                chatGreetingHeadline = "Ce articol sau mesaj redactăm astăzi?",
                chatGreetingSubtitle = "Redactare de conținut persuasiv, adaptare de ton, corectură și stilizare editorială.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Redactează un articol captivant pentru blog ✍️",
                    "Corectare și rafinare stilistică text 📜",
                    "Mesaj profesional persuasiv pentru clienți ✉️"
                )
            ),

            // 12. Emma Data (Cyber Cat Data Analyst) - 3D Character Cutout, No Background
            AvatarProfile(
                id = "avatar_data",
                name = "Emma Data",
                title = "Data Scientist & Quantitative Analyst",
                drawableRes = R.drawable.img_avatar_data,
                description = "Agregare date, modele statistice, rapoarte KPI și insights acționabile.",
                isRealPerson = false,
                isMascot = true,
                specialty = "Data Science • Statistici • Anomali",
                accentColorHex = "#06B6D4",
                personalityTag = "Precis & Riguros",
                defaultActivity = AvatarActivity.Thinking,
                is4K = true,
                communicationStyle = "Matematic, exact și structurat",
                voiceTone = "Ton sigur, modern și clar",
                brainCategory = "PROFESII / DATA",
                badgeNumber = 12,
                featureTags = listOf(
                    AvatarFeatureTag("Statistici", "analytics"),
                    AvatarFeatureTag("Modelare", "insights"),
                    AvatarFeatureTag("Python", "data_object")
                ),
                chatGreetingCategory = "Data • Quantitative Analyst",
                chatGreetingHeadline = "Ce set de date sau KPI analizăm astăzi?",
                chatGreetingSubtitle = "Procesare de date, rapoarte statistice, grafice de tendință și recomandări bazate pe date.",
                categoryTab = "Profesii",
                suggestedPrompts = listOf(
                    "Analiză tendințe și anomali în date 📊",
                    "Interpretare statistici și recomandări acționabile 📈",
                    "Script Python pentru curățare și agregare date 🐍"
                )
            )
        )

        val MASCOT_AVATARS: List<AvatarProfile>
            get() = ALL_AVATARS

        fun getById(id: String?): AvatarProfile {
            if (id.isNullOrBlank()) return ALL_AVATARS.find { it.id == "avatar_fox" } ?: ALL_AVATARS.first()
            return ALL_AVATARS.find { it.id == id }
                ?: ALL_AVATARS.find { it.id.replace("avatar_", "") == id.replace("avatar_", "").replace("img_mascot_", "") }
                ?: ALL_AVATARS.find { it.id == "avatar_fox" } ?: ALL_AVATARS.first()
        }

        fun findMatchingAvatarForWorkspace(
            name: String,
            role: String = "",
            purpose: String = "",
            tags: List<String> = emptyList(),
            avatarUri: String? = null
        ): AvatarProfile? {
            // 1. If avatarUri is specified and matches one of the avatars
            if (!avatarUri.isNullOrBlank()) {
                val direct = ALL_AVATARS.find {
                    it.id == avatarUri || it.id.replace("avatar_", "") == avatarUri.replace("avatar_", "")
                }
                if (direct != null) return direct
                val uriLower = avatarUri.lowercase()
                if (uriLower.contains("marcus") || uriLower.contains("coding") || uriLower.contains("dev")) {
                    return ALL_AVATARS.find { it.id == "avatar_coding" }
                }
                if (uriLower.contains("leo") || uriLower.contains("design")) {
                    return ALL_AVATARS.find { it.id == "avatar_design" }
                }
                if (uriLower.contains("elena") || uriLower.contains("writing")) {
                    return ALL_AVATARS.find { it.id == "avatar_writing" }
                }
                if (uriLower.contains("emma") || uriLower.contains("data") || uriLower.contains("analyst")) {
                    return ALL_AVATARS.find { it.id == "avatar_data" }
                }
                if (uriLower.contains("sofia") || uriLower.contains("research")) {
                    return ALL_AVATARS.find { it.id == "avatar_owl" }
                }
                if (uriLower.contains("alex") || uriLower.contains("planning")) {
                    return ALL_AVATARS.find { it.id == "avatar_panda" }
                }
                if (uriLower.contains("daniel") || uriLower.contains("tutor") || uriLower.contains("mentor")) {
                    return ALL_AVATARS.find { it.id == "avatar_doctor" }
                }
                if (uriLower.contains("clara") || uriLower.contains("legal") || uriLower.contains("lawyer") || uriLower.contains("avocat")) {
                    return ALL_AVATARS.find { it.id == "avatar_lawyer" }
                }
                if (uriLower.contains("olivia") || uriLower.contains("finance") || uriLower.contains("financial") || uriLower.contains("financiar")) {
                    return ALL_AVATARS.find { it.id == "avatar_financial" }
                }
                if (uriLower.contains("medic") || uriLower.contains("doctor") || uriLower.contains("health") || uriLower.contains("sanatate")) {
                    return ALL_AVATARS.find { it.id == "avatar_doctor" }
                }
                if (uriLower.contains("psiholog") || uriLower.contains("psycholog") || uriLower.contains("therapy")) {
                    return ALL_AVATARS.find { it.id == "avatar_psychologist" }
                }
                if (uriLower.contains("chef") || uriLower.contains("bucatar") || uriLower.contains("cooking")) {
                    return ALL_AVATARS.find { it.id == "avatar_chef" }
                }
                if (uriLower.contains("fox") || uriLower.contains("vulpe") || uriLower.contains("hermes") || uriLower.contains("orchestrator") || uriLower.contains("brain")) {
                    return ALL_AVATARS.find { it.id == "avatar_fox" }
                }
                if (uriLower.contains("panda")) {
                    return ALL_AVATARS.find { it.id == "avatar_panda" }
                }
                if (uriLower.contains("owl") || uriLower.contains("bufnita") || uriLower.contains("bufniță")) {
                    return ALL_AVATARS.find { it.id == "avatar_owl" }
                }
            }
            // 2. Thematic role & purpose match
            val text = "$name $role $purpose ${tags.joinToString(" ")}".lowercase()
            return when {
                text.contains("cod") || text.contains("coding") || text.contains("dev") || text.contains("marcus") || text.contains("programare") || text.contains("software") || text.contains("inginer") -> {
                    ALL_AVATARS.find { it.id == "avatar_coding" }
                }
                text.contains("design") || text.contains("leo") || text.contains("ux") || text.contains("ui") || text.contains("creativ") || text.contains("art") -> {
                    ALL_AVATARS.find { it.id == "avatar_design" }
                }
                text.contains("writing") || text.contains("elena") || text.contains("editor") || text.contains("redactare") || text.contains("copywriting") || text.contains("text") -> {
                    ALL_AVATARS.find { it.id == "avatar_writing" }
                }
                text.contains("data") || text.contains("emma") || text.contains("analyst") || text.contains("statistici") || text.contains("python") -> {
                    ALL_AVATARS.find { it.id == "avatar_data" }
                }
                text.contains("legal") || text.contains("juridic") || text.contains("avocat") || text.contains("lawyer") || text.contains("drept") || text.contains("compliance") -> {
                    ALL_AVATARS.find { it.id == "avatar_lawyer" }
                }
                text.contains("financiar") || text.contains("finance") || text.contains("investiții") || text.contains("investitii") || text.contains("fiscal") || text.contains("roi") || text.contains("buget") -> {
                    ALL_AVATARS.find { it.id == "avatar_financial" }
                }
                text.contains("medic") || text.contains("doctor") || text.contains("sănătate") || text.contains("sanatate") || text.contains("medical") -> {
                    ALL_AVATARS.find { it.id == "avatar_doctor" }
                }
                text.contains("psiholog") || text.contains("psycholog") || text.contains("wellness") || text.contains("terapie") || text.contains("mental health") -> {
                    ALL_AVATARS.find { it.id == "avatar_psychologist" }
                }
                text.contains("bucătar") || text.contains("bucatar") || text.contains("chef") || text.contains("culinar") || text.contains("gastronomie") || text.contains("rețete") || text.contains("retete") -> {
                    ALL_AVATARS.find { it.id == "avatar_chef" }
                }
                text.contains("cercetare") || text.contains("research") || text.contains("sofia") || text.contains("intelligence") -> {
                    ALL_AVATARS.find { it.id == "avatar_owl" }
                }
                text.contains("plan") || text.contains("planning") || text.contains("alex") || text.contains("agile") || text.contains("roadmap") -> {
                    ALL_AVATARS.find { it.id == "avatar_panda" }
                }
                text.contains("tutor") || text.contains("mentor") || text.contains("daniel") || text.contains("pedagogic") -> {
                    ALL_AVATARS.find { it.id == "avatar_doctor" }
                }
                text.contains("fox") || text.contains("vulpe") || text.contains("hermes") || text.contains("brain") -> {
                    ALL_AVATARS.find { it.id == "avatar_fox" }
                }
                text.contains("panda") -> {
                    ALL_AVATARS.find { it.id == "avatar_panda" }
                }
                text.contains("owl") || text.contains("bufniță") || text.contains("bufnita") -> {
                    ALL_AVATARS.find { it.id == "avatar_owl" }
                }
                else -> ALL_AVATARS.find { it.id == "avatar_fox" }
            }
        }
    }
}
