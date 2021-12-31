package polyrhythmmania.credits

import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.Localization
import java.util.*


object Credits {
    
    val credits: Map<ReadOnlyVar<String>, List<ReadOnlyVar<String>>> = linkedMapOf(
            Localization.getVar("credits.programming") to listOf("chrislo27", "[font=rodin](◉.◉)☂[]").toVars(),
            Localization.getVar("credits.graphicDesign") to abcSorted(
                    "garbo", "snow krow", "GENERIC", "Merch_Andise",
            ).toVars(),
            Localization.getVar("credits.music") to listOf(
                    Var("GENERIC"), Localization.getVar("credits.rhSoundtrack")
            ),
            Localization.getVar("credits.qa") to abcSorted(
                    "Lvl100Feraligatr", "Gosh", "GENERIC", "snow krow", "Kievit", "Chloe", "GrueKun", "Huebird",
                    "RedCrowNose", "J-D Thunder", "garbo", "Doggo",
                    "The Eggo55",
            ).toVars() + listOf(
                    Localization.getVar("credits.tourneycord"),
            ),
            Localization.getVar("credits.githubBugReporters") to abcSorted(
                    "thomasynthesis", "Dummatt", "user670", "ZaptorZap", "notpolyrhythmic", "Unnamedspaceuser",
            ).toVars(),
            Localization.getVar("credits.specialThanks") to abcSorted(
                    "Lvl100Feraligatr",
                    "GrueKun",
                    "spoopster",
                    "Kievit",
                    "Turtike",
                    "J-D Thunder",
                    "RedCrowNose",
            ).toVars() + listOf(Localization.getVar("credits.projectDonators")),
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
                    "earlygrey/shapedrawer",
                    "discord-game-sdk4j",
                    "twelvemonkeys",
                    "Pexels",
                    "world-flags-sprite",
            ).toVars() + listOf(
                    "Kurokane",
                    "Leland",
                    "Roboto",
                    "Rodin",
            ).map { Localization.getVar("credits.fontName", Var { listOf(it) }) },
    )
    
    private fun abcSorted(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
    private fun List<String>.toVars(): List<ReadOnlyVar<String>> = this.map { Var(it) }
}