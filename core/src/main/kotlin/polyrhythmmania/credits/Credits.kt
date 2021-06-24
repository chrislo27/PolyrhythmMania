package polyrhythmmania.credits

import java.util.*


object Credits {
    
    val credits: Map<String, List<String>> = linkedMapOf(
            "credits.programming" to listOf("chrislo27", "[font=rodin](◉.◉)☂[]"),
            "credits.graphicDesign" to abcSorted(
                    "garbo", "snow krow",
            ),
            "credits.qa" to abcSorted(
                    "Lvl100Feraligatr", "Gosh", "GenericArrangements", "snow krow", "Kievit"
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
                    "earlygrey/shapedrawer"
            ),
            "credits.specialThanks" to abcSorted(
                    "Lvl100Feraligatr",
                    "GrueKun",
                    "spoopster"
            ),
    )
    
    private fun abcSorted(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
}