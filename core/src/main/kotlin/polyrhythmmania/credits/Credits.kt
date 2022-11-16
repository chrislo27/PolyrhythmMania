package polyrhythmmania.credits

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import java.util.*


object Credits : CreditsBase() {
    
    override val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>> = linkedMapOf(
            Localization.getVar("credits.projectLead") to listOf("chrislo27").toVars(),
            Localization.getVar("credits.programming") to listOf("chrislo27", "[font=rodin](◉.◉)☂[]").toVars(),
            Localization.getVar("credits.graphicDesign") to abcSorted(
                    "garbo", "snow krow", "GENERIC", "Merch_Andise", "Kievit", "Luxury",
            ).toVars(),
            Localization.getVar("credits.music") to listOf(
                    Var("GENERIC"), Localization.getVar("credits.rhSoundtrack")
            ),
            Localization.getVar("credits.qa") to abcSorted(
                    "Lvl100Feraligatr", "Gosh", "GENERIC", "snow krow", "Kievit", "Chloe", "GrueKun", "Huebird",
                    "RedCrowNose", "J-D Thunder", "garbo", "Conn", "Merch_Andise", "Turtike",
                    "The Eggo55",
            ).toVars() + listOf(
                    Localization.getVar("credits.tourneycord"),
            ),
            Localization.getVar("credits.specialThanks") to abcSorted(
                    "Lvl100Feraligatr",
                    "GrueKun",
                    "spoopster",
                    "Kievit",
                    "Turtike",
                    "J-D Thunder",
                    "RedCrowNose",
            ).toVars() + listOf(Localization.getVar("credits.githubBugReporters"), Localization.getVar("credits.projectDonators")),
            Localization.getVar("credits.resourcesAndTechnologies") to listOf(
                    Localization.getVar("credits.rhAssets"),
            ) + listOf(
                    "libGDX",
                    "LWJGL",
                    "Kotlin",
                    "Java",
                    "Paintbox",
                    "Beads",
                    "minimal-json",
                    "JCommander",
                    "SLF4J",
                    "zip4j",
                    "shapedrawer",
                    "discord-game-sdk4j",
                    "twelvemonkeys",
                    "Pexels",
                    "world-flags-sprite",
                    "glsl-fast-gaussian-blur",
            ).toVars() + abcSorted(
                    "Roboto",
                    "Rodin",
                    "Kurokane",
                    "Leland",
                    "Roboto Mono",
                    "Roboto Condensed",
                    "Roboto Slab",
                    "Lexend",
                    "Open Sans",
                    "Flow Circular",
                    "Share Tech Mono",
            ).map { Localization.getVar("credits.fontName", Var { listOf(it) }) },
    )
    
    val languageCredits: Map<Locale, LanguageCredit> = listOf(
            LanguageCredit(Locale("es", "", ""), listOf("J-D Thunder"), listOf("Yumiko!")),
    ).associateBy { it.locale }
    
}
