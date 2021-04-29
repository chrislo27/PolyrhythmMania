package polyrhythmmania.editor.track.block

import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor


/**
 * An [Instantiator] is effectively a [Block] factory with extra metadata for the UI.
 */
data class Instantiator(val id: String, 
                        val name: ReadOnlyVar<String>,
                        val deprecatedIDs: Set<String> = emptySet(),
                        val factory: Instantiator.(Editor) -> Block)

object Instantiators {
    
    val map: Map<String, Instantiator>
    val list: List<Instantiator>
    
    init {
        val tempMap = mutableMapOf<String, Instantiator>()
        val tempList = mutableListOf<Instantiator>()

        fun add(instantiator: Instantiator) {
            tempMap[instantiator.id] = instantiator
            instantiator.deprecatedIDs.forEach { tempMap[it] = instantiator }
            tempList += instantiator
        }

        add(Instantiator("pattern", Localization.getVar("instantiator.pattern.name")) { editor ->
            BlockPattern(editor)
        })

        map = tempMap
        list = tempList
    }
}