package polyrhythmmania.credits

import java.util.*


object Credits {
    
    val credits: Map<String, List<String>> = linkedMapOf(
            "credits.programming" to listOf("chrislo27", "[font=rodin](◉.◉)☂[]"),
            "credits.graphicDesign" to abcSorted(
                    "garbo",
            ),
            "credits.qa" to abcSorted(
                    "Lvl100Feraligatr", "<...>"
            ),
            "credits.resourcesAndTechnologies" to listOf(
                    "[font=rodin]リズム天国[] assets\nby Nintendo",
                    "libGDX",
                    "LWJGL",
                    "Kotlin",
                    "Java",
                    "Beads",
                    "minimal-json",
                    "JCommander",
                    "SLF4J",
                    "zip4j",
            ),
            "credits.specialThanks" to abcSorted(
                    "Lvl100Feraligatr",
                    "GrueKun",
                    "spoopster"
            ),
    )
    
    private fun abcSorted(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
}