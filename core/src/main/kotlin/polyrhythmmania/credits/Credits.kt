package polyrhythmmania.credits

import java.util.*


object Credits {
    
    val credits: Map<String, List<String>> = linkedMapOf(
            "credits.programming" to listOf("chrislo27", "[font=rodin](◉.◉)☂[]"),
            "credits.externalAssets" to ordered("Nintendo",),
            "credits.graphicDesign" to ordered("theonetruegarbo",),
            "credits.qa" to ordered("<...>",),
            "credits.specialThanks" to ordered("Lvl100Feraligatr", "GrueKun", "spoopster"),
    )
    
    private fun ordered(vararg things: String): List<String> = things.sortedBy { it.lowercase(Locale.ROOT) }
}