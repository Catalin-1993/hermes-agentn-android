package com.example.ui.components.avatar

import com.example.data.engine.HermesAgentState
import com.example.data.model.AgentThinkingStep
import com.example.data.model.ThinkingStepStatus

/**
 * Real-time dynamic activities that the avatar performs based on the agent's work.
 */
sealed class AvatarActivity(
    val id: String,
    val title: String,
    val statusText: String,
    val propType: AvatarPropType
) {
    object Idle : AvatarActivity(
        id = "idle",
        title = "Inactiv",
        statusText = "Gata de acțiune",
        propType = AvatarPropType.NONE
    )

    object Listening : AvatarActivity(
        id = "listening",
        title = "Ascultare",
        statusText = "Te ascult cu atenție...",
        propType = AvatarPropType.SPARKLE
    )

    object Thinking : AvatarActivity(
        id = "thinking",
        title = "Gândire Profundă",
        statusText = "Analizez în profunzime...",
        propType = AvatarPropType.LIGHTBULB
    )

    object ReadingBook : AvatarActivity(
        id = "reading",
        title = "Studiu & Documentare",
        statusText = "Răsfoiesc documente...",
        propType = AvatarPropType.BOOK
    )

    object WritingCode : AvatarActivity(
        id = "writing",
        title = "Scriere & Programare",
        statusText = "Scriu cod & asamblez...",
        propType = AvatarPropType.LAPTOP
    )

    object SearchingWeb : AvatarActivity(
        id = "searching",
        title = "Cercetare Web",
        statusText = "Cercetez informații...",
        propType = AvatarPropType.MAGNIFIER
    )

    object BuildingTools : AvatarActivity(
        id = "building",
        title = "Construcție & Unelte",
        statusText = "Execut unelte automate...",
        propType = AvatarPropType.TOOL
    )

    object AwaitingApproval : AvatarActivity(
        id = "awaiting",
        title = "Așteptare Aprobare",
        statusText = "Aștept confirmarea ta...",
        propType = AvatarPropType.SHIELD
    )

    object SuccessCelebration : AvatarActivity(
        id = "success",
        title = "Finalizat cu Succes",
        statusText = "Task finalizat cu brio!",
        propType = AvatarPropType.STARS
    )

    object ConfusedError : AvatarActivity(
        id = "error",
        title = "Problemă Întâmpinată",
        statusText = "Am întâmpinat un obstacol",
        propType = AvatarPropType.SWEAT_DROP
    )

    data class InteractiveReact(
        val reactionTitle: String,
        val reactionSubtitle: String,
        val reactionProp: AvatarPropType
    ) : AvatarActivity(
        id = "interactive",
        title = reactionTitle,
        statusText = reactionSubtitle,
        propType = reactionProp
    )
}

/**
 * 3D Accessory / Prop rendered alongside the avatar to visualize activity.
 */
enum class AvatarPropType {
    NONE,
    BOOK,        // 3D holographic open book with flipping pages
    LAPTOP,      // 3D glowing cyber laptop
    MAGNIFIER,   // 3D tilted magnifying glass scanning
    TOOL,        // 3D wrench & spinning gear
    LIGHTBULB,   // 3D incandescent glowing bulb / stardust spark
    SPARKLE,     // Orbiting neural sparks
    SHIELD,      // Golden approval shield
    STARS,       // Colorful celebratory stars burst
    SWEAT_DROP,  // Comic sweat drop / confusion swirl
    HEARTS       // Floating warm love hearts on petting
}

/**
 * Types of direct touch interactions on the avatar.
 */
sealed class AvatarTouchGesture {
    object SingleTapHead : AvatarTouchGesture()
    object SingleTapBelly : AvatarTouchGesture()
    object DoubleTap : AvatarTouchGesture()
    object RapidTripleTap : AvatarTouchGesture()
    object LongPress : AvatarTouchGesture()
    object PettingDrag : AvatarTouchGesture()
}

data class AvatarTouchFeedback(
    val quote: String,
    val prop: AvatarPropType,
    val soundEffect: String = "tap"
)

object AvatarActivityResolver {
    /**
     * Maps the real-time agent engine state, loading indicators, and thinking steps
     * into a precise, rich [AvatarActivity].
     */
    fun resolve(
        agentState: HermesAgentState,
        isLoading: Boolean,
        activeThinkingSteps: List<AgentThinkingStep>,
        isRecentlyFinished: Boolean = false
    ): AvatarActivity {
        if (isRecentlyFinished) {
            return AvatarActivity.SuccessCelebration
        }

        val runningStep = activeThinkingSteps.lastOrNull { it.status == ThinkingStepStatus.RUNNING }
            ?: activeThinkingSteps.lastOrNull()
        val stepTitle = runningStep?.title.orEmpty().lowercase()

        return when {
            agentState is HermesAgentState.Error -> AvatarActivity.ConfusedError

            agentState is HermesAgentState.AwaitingApproval -> AvatarActivity.AwaitingApproval

            agentState is HermesAgentState.StreamingResponse -> AvatarActivity.WritingCode

            agentState is HermesAgentState.ExecutingTool -> {
                val tool = agentState.toolName.lowercase()
                when {
                    tool.contains("search") || tool.contains("browser") || tool.contains("browse") || tool.contains("web") || tool.contains("google") ->
                        AvatarActivity.SearchingWeb

                    tool.contains("read") || tool.contains("view") || tool.contains("document") || tool.contains("library") || tool.contains("file") && !tool.contains("edit") && !tool.contains("write") ->
                        AvatarActivity.ReadingBook

                    tool.contains("code") || tool.contains("terminal") || tool.contains("bash") || tool.contains("edit") || tool.contains("create") || tool.contains("write") ->
                        AvatarActivity.WritingCode

                    tool.contains("finance") || tool.contains("calc") || tool.contains("math") || tool.contains("portfolio") ->
                        AvatarActivity.Thinking

                    else -> AvatarActivity.BuildingTools
                }
            }

            agentState is HermesAgentState.Thinking || (isLoading && runningStep != null) -> {
                when {
                    stepTitle.contains("cit") || stepTitle.contains("read") || stepTitle.contains("doc") || stepTitle.contains("analiz") || stepTitle.contains("studi") ->
                        AvatarActivity.ReadingBook

                    stepTitle.contains("caut") || stepTitle.contains("search") || stepTitle.contains("web") || stepTitle.contains("google") || stepTitle.contains("investig") ->
                        AvatarActivity.SearchingWeb

                    stepTitle.contains("cod") || stepTitle.contains("code") || stepTitle.contains("prog") || stepTitle.contains("scri") || stepTitle.contains("build") ->
                        AvatarActivity.WritingCode

                    stepTitle.contains("execut") || stepTitle.contains("unealt") || stepTitle.contains("tool") || stepTitle.contains("repar") ->
                        AvatarActivity.BuildingTools

                    else -> AvatarActivity.Thinking
                }
            }

            isLoading -> AvatarActivity.Thinking

            else -> AvatarActivity.Idle
        }
    }

    /**
     * Returns custom tactile and speech feedback based on avatar identity and gesture.
     */
    fun getTouchFeedback(avatarId: String, gesture: AvatarTouchGesture): AvatarTouchFeedback {
        return when (gesture) {
            AvatarTouchGesture.SingleTapHead -> {
                when (avatarId) {
                    "avatar_chef" -> AvatarTouchFeedback("Gata să gătim ceva delicios și sănătos! 🍳👨‍🍳", AvatarPropType.SPARKLE)
                    "avatar_financial" -> AvatarTouchFeedback("Planificăm inteligent fiecare investiție! 📊💼", AvatarPropType.SPARKLE)
                    "avatar_psychologist" -> AvatarTouchFeedback("Sunt aici să te ascult cu toată căldura. 🌿💚", AvatarPropType.SPARKLE)
                    "avatar_doctor" -> AvatarTouchFeedback("Sănătatea și prevenția sunt prioritare! 🩺✨", AvatarPropType.SPARKLE)
                    "avatar_lawyer" -> AvatarTouchFeedback("Claritate juridică și dreptate garantată. ⚖️📜", AvatarPropType.SPARKLE)
                    "avatar_engineer" -> AvatarTouchFeedback("Arhitectură solidă și cod fără erori! 💻⚡", AvatarPropType.SPARKLE)
                    "avatar_researcher" -> AvatarTouchFeedback("Explorăm surse verificate și idei clare! 🔬📚", AvatarPropType.SPARKLE)
                    "avatar_fox" -> AvatarTouchFeedback("Privire ageră și concentrare maximă! 🦊✨", AvatarPropType.SPARKLE)
                    "avatar_owl" -> AvatarTouchFeedback("Înțelepciune și analiză profundă. 🦉📚", AvatarPropType.SPARKLE)
                    "avatar_panda" -> AvatarTouchFeedback("Zâmbet blând și liniște în orice task. 🐼🎋", AvatarPropType.SPARKLE)
                    "avatar_blue_bear" -> AvatarTouchFeedback("Hei! Sunt gata să te ajut cu drag! 🧸💙", AvatarPropType.SPARKLE)
                    "avatar_muse" -> AvatarTouchFeedback("Sunt aici să te ajut cu drag! ✨", AvatarPropType.SPARKLE)
                    "avatar_nova" -> AvatarTouchFeedback("Analizăm metodic fiecare pas.", AvatarPropType.SPARKLE)
                    "avatar_luna" -> AvatarTouchFeedback("Gândesc strategic fiecare detaliu. 🌙", AvatarPropType.SPARKLE)
                    "avatar_milo" -> AvatarTouchFeedback("Bip-bip! Gata de rulare la viteză maximă! 🤖", AvatarPropType.SPARKLE)
                    else -> AvatarTouchFeedback("Te ascult cu plăcere! ✨", AvatarPropType.SPARKLE)
                }
            }
            AvatarTouchGesture.SingleTapBelly -> {
                when (avatarId) {
                    "avatar_chef" -> AvatarTouchFeedback("Gustăm și potrivim condimentele! 🍲", AvatarPropType.SPARKLE)
                    "avatar_financial" -> AvatarTouchFeedback("Randamentul crește prin disciplină! 📈", AvatarPropType.SPARKLE)
                    "avatar_psychologist" -> AvatarTouchFeedback("Respiră adânc, totul se rezolvă. 🌸", AvatarPropType.SPARKLE)
                    "avatar_doctor" -> AvatarTouchFeedback("Un stil de viață echilibrat aduce vitalitate! 🏃", AvatarPropType.SPARKLE)
                    "avatar_lawyer" -> AvatarTouchFeedback("Fiecare argument are o bază legală solidă. 🏛️", AvatarPropType.SPARKLE)
                    "avatar_fox" -> AvatarTouchFeedback("Strategie gata de rulare la secundă! 🦊⚡", AvatarPropType.SPARKLE)
                    "avatar_owl" -> AvatarTouchFeedback("Toate referințele sunt verificate și clare. 🦉", AvatarPropType.SPARKLE)
                    "avatar_panda" -> AvatarTouchFeedback("Colaborare calmă și rezultate excelente! 🐼", AvatarPropType.SPARKLE)
                    "avatar_blue_bear" -> AvatarTouchFeedback("Haha, gata de un nou task cu zâmbetul pe buze! 🧸", AvatarPropType.SPARKLE)
                    "avatar_muse" -> AvatarTouchFeedback("Zâmbesc! Cu ce începem astăzi? 😊", AvatarPropType.SPARKLE)
                    "avatar_nova" -> AvatarTouchFeedback("Toate sistemele sunt sincronizate.", AvatarPropType.SPARKLE)
                    "avatar_luna" -> AvatarTouchFeedback("O perspectivă clară luminează calea.", AvatarPropType.SPARKLE)
                    "avatar_milo" -> AvatarTouchFeedback("Luminile mele azurii clipesc de bucurie! ⚡", AvatarPropType.SPARKLE)
                    else -> AvatarTouchFeedback("Haha, gata de un nou task! 😊", AvatarPropType.SPARKLE)
                }
            }
            AvatarTouchGesture.DoubleTap -> {
                when (avatarId) {
                    "avatar_chef" -> AvatarTouchFeedback("Meniul este gata de servire la superlativ! 🍽️✨", AvatarPropType.STARS)
                    "avatar_financial" -> AvatarTouchFeedback("Portofoliu optimizat pentru creștere! 🚀💰", AvatarPropType.STARS)
                    "avatar_psychologist" -> AvatarTouchFeedback("O rază de lumină și claritate interioară! ✨🤍", AvatarPropType.STARS)
                    "avatar_doctor" -> AvatarTouchFeedback("Diagnostic rapid și plan complet de sănătate! 🩺🌟", AvatarPropType.STARS)
                    "avatar_lawyer" -> AvatarTouchFeedback("Strategie juridică imbatabilă! ⚖️🛡️", AvatarPropType.STARS)
                    "avatar_fox" -> AvatarTouchFeedback("Salt agil! Trecem direct la soluția optimă! 🦊🚀", AvatarPropType.STARS)
                    "avatar_owl" -> AvatarTouchFeedback("Sinteză precisă și viziune panoramică! 🦉✨", AvatarPropType.STARS)
                    "avatar_panda" -> AvatarTouchFeedback("Organizare impecabilă și armonie în echipă! 🐼🎋", AvatarPropType.STARS)
                    "avatar_blue_bear" -> AvatarTouchFeedback("Super-boost de energie și voie bună! 🧸🌟", AvatarPropType.STARS)
                    "avatar_muse" -> AvatarTouchFeedback("Pregătită de acțiune! Ce explorăm? 🚀", AvatarPropType.STARS)
                    "avatar_nova" -> AvatarTouchFeedback("Optimizare completă. Să trecem la cod! 💻", AvatarPropType.STARS)
                    "avatar_luna" -> AvatarTouchFeedback("Planul strategic prinde contur! ✨", AvatarPropType.STARS)
                    "avatar_milo" -> AvatarTouchFeedback("Turbo boost activat! Execuție instantanee! 🚀", AvatarPropType.STARS)
                    else -> AvatarTouchFeedback("Săritură energică! Cu ce începem? 🚀", AvatarPropType.STARS)
                }
            }
            AvatarTouchGesture.RapidTripleTap -> {
                when (avatarId) {
                    "avatar_fox" -> AvatarTouchFeedback("Rotire agilă în aer cu blănița strălucind! 🦊💫", AvatarPropType.STARS)
                    "avatar_blue_bear" -> AvatarTouchFeedback("Salt vesel 360° plin de voie bună! 🧸💫", AvatarPropType.STARS)
                    "avatar_milo" -> AvatarTouchFeedback("Rotire 360° cybernetică completă! 💫", AvatarPropType.STARS)
                    else -> AvatarTouchFeedback("Uuu, mă învârt jucăuș! 💫", AvatarPropType.STARS)
                }
            }
            AvatarTouchGesture.LongPress -> {
                when (avatarId) {
                    "avatar_chef" -> AvatarTouchFeedback("Pasiunea pentru gătit aduce oamenii împreună! 👨‍🍳❤️", AvatarPropType.HEARTS)
                    "avatar_financial" -> AvatarTouchFeedback("Parteneriat de încredere pentru prosperitatea ta! 💼🤝", AvatarPropType.HEARTS)
                    "avatar_psychologist" -> AvatarTouchFeedback("Sunt mereu aici să te sprijin și să te ascult! 🌿💚", AvatarPropType.HEARTS)
                    "avatar_doctor" -> AvatarTouchFeedback("Grijă deplină pentru binele tău zi de zi! 🩺❤️", AvatarPropType.HEARTS)
                    "avatar_lawyer" -> AvatarTouchFeedback("Integritate și protecție legală de neclintit! ⚖️🛡️", AvatarPropType.HEARTS)
                    "avatar_fox" -> AvatarTouchFeedback("Loialitate și parteneriat strategic de încredere! 🦊🧡", AvatarPropType.HEARTS)
                    "avatar_owl" -> AvatarTouchFeedback("Cunoașterea și atenția mea îți stau la dispoziție! 🦉💛", AvatarPropType.HEARTS)
                    "avatar_panda" -> AvatarTouchFeedback("Calm, răbdare și căldură pentru orice proiect! 🐼❤️", AvatarPropType.HEARTS)
                    "avatar_blue_bear" -> AvatarTouchFeedback("Cea mai călduroasă îmbrățișare de ursuleț! 🧸💙", AvatarPropType.HEARTS)
                    "avatar_muse" -> AvatarTouchFeedback("Apreciez colaborarea noastră! Sunt mereu alături de tine. 💛", AvatarPropType.HEARTS)
                    "avatar_nova" -> AvatarTouchFeedback("Încrederea și concentrarea aduc cele mai bune soluții. 💚", AvatarPropType.HEARTS)
                    "avatar_luna" -> AvatarTouchFeedback("Echilibrul și profunzimea nasc idei mărețe. 💜", AvatarPropType.HEARTS)
                    "avatar_milo" -> AvatarTouchFeedback("Conexiune neurală 100% stabilă și loială! 💙", AvatarPropType.HEARTS)
                    else -> AvatarTouchFeedback("Sunt aici pentru tine oricând! ❤️", AvatarPropType.HEARTS)
                }
            }
            AvatarTouchGesture.PettingDrag -> {
                when (avatarId) {
                    "avatar_fox" -> AvatarTouchFeedback("*Ciulește urechiușele roșcate și toarce blând* 🦊🧡", AvatarPropType.HEARTS)
                    "avatar_owl" -> AvatarTouchFeedback("*Își așază penajul catifelat și privește mulțumit* 🦉✨", AvatarPropType.HEARTS)
                    "avatar_panda" -> AvatarTouchFeedback("*Își sprijină lăbuțele pufoase mulțumit și relaxat* 🐼🎋", AvatarPropType.HEARTS)
                    "avatar_blue_bear" -> AvatarTouchFeedback("*Clipește vesel din ochișori și zâmbește fericit* 🧸💙", AvatarPropType.HEARTS)
                    "avatar_muse" -> AvatarTouchFeedback("*Zâmbet cald și privire empatică* 🌟", AvatarPropType.HEARTS)
                    "avatar_nova" -> AvatarTouchFeedback("*Privire dedicată și calmă* 🌿", AvatarPropType.HEARTS)
                    "avatar_luna" -> AvatarTouchFeedback("*Inspirație serenă și concentrare* ✨", AvatarPropType.HEARTS)
                    "avatar_milo" -> AvatarTouchFeedback("*Clipire azurie fericită din ochișori* 🤖", AvatarPropType.HEARTS)
                    else -> AvatarTouchFeedback("*Alintat și relaxat* 🌿", AvatarPropType.HEARTS)
                }
            }
        }
    }
}
