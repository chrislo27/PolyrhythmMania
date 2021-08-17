package polyrhythmmania.credits

import java.util.*


object Credits {
    
    val credits: Map<String, List<String>> = linkedMapOf(
            "credits.programming" to listOf("chrislo27", "[font=rodin](◉.◉)☂[]"),
            "credits.graphicDesign" to abcSorted(
                    "garbo", "snow krow", "GENERIC",
            ),
            "credits.music" to listOf(
                    "GENERIC", "Rhythm Heaven\nsoundtrack"
            ),
            "credits.qa" to abcSorted(
                    "Lvl100Feraligatr", "Gosh", "GENERIC", "snow krow", "Kievit", "Chloe", "GrueKun", "Huebird",
                    "RedCrowNose", "J-D Thunder",
            ) + listOf("Tourneycord Discord\nserver"),
            "credits.specialThanks" to abcSorted(
                    "Lvl100Feraligatr",
                    "GrueKun",
                    "spoopster",
                    "Kievit",
                    "Turtike",
            ),
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
                    "java-discord-rpc",
                    "twelvemonkeys",
                    "Roboto",
                    "Rodin",
                    "Kurokane",
                    "Pexels",
            ),
    )
    
    private fun abcSorted(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
}