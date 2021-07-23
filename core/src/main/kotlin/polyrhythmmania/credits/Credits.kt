package polyrhythmmania.credits

import java.util.*


object Credits {
    
    val credits: Map<String, List<String>> = linkedMapOf(
            "credits.programming" to listOf("chrislo27", "[font=rodin](◉.◉)☂[]"),
            "credits.graphicDesign" to abcSorted(
                    "garbo", "snow krow",
            ),
            "credits.music" to abcSorted(
                    "GENERIC"
            ),
            "credits.qa" to abcSorted(
                    "Lvl100Feraligatr", "Gosh", "GENERIC", "snow krow", "Kievit", "Chloe", "GrueKun", "Huebird",
            ) + listOf("Tourneycord Discord\nserver"),
            "credits.resourcesAndTechnologies" to listOf(
                    "[font=rodin]リズム天国[] assets\nby Nintendo",
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
                    "Pexels",
            ),
            "credits.specialThanks" to abcSorted(
                    "Lvl100Feraligatr",
                    "GrueKun",
                    "spoopster",
                    "Kievit",
                    "Turtike",
            ),
    )
    
    private fun abcSorted(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
}