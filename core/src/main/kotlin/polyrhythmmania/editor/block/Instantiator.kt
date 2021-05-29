package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.ReadOnlyVar
import io.github.chrislo27.paintbox.binding.Var
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor


/**
 * An [Instantiator] is effectively a [Block] factory with extra metadata for the UI.
 */
data class Instantiator(val id: String,
                        val name: ReadOnlyVar<String>,
                        val summary: ReadOnlyVar<String>,
                        val desc: ReadOnlyVar<String>,
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

        add(Instantiator("endState", Localization.getVar("instantiator.endState.name"),
                Localization.getVar("instantiator.endState.summary"),
                Localization.getVar("instantiator.endState.desc")) { editor ->
            BlockEndState(editor)
        })
        add(Instantiator("spawnPattern", Localization.getVar("instantiator.spawnPattern.name"),
                Localization.getVar("instantiator.spawnPattern.summary"),
                Localization.getVar("instantiator.spawnPattern.desc")) { editor ->
            BlockSpawnPattern(editor)
        })
        add(Instantiator("deployRod", Localization.getVar("instantiator.deployRod.name"),
                Localization.getVar("instantiator.deployRod.summary"),
                Localization.getVar("instantiator.deployRod.desc")) { editor ->
            BlockDeployRod(editor)
        })
        add(Instantiator("retractPistons", Localization.getVar("instantiator.retractPistons.name"),
                Localization.getVar("instantiator.retractPistons.summary"),
                Localization.getVar("instantiator.retractPistons.desc")) { editor ->
            BlockRetractPistons(editor)
        })
        add(Instantiator("despawnPattern", Localization.getVar("instantiator.despawnPattern.name"),
                Localization.getVar("instantiator.despawnPattern.summary"),
                Localization.getVar("instantiator.despawnPattern.desc")) { editor ->
            BlockDespawnPattern(editor)
        })

//        add(Instantiator("baselineTest", Var("Baseline Test"), Var("Description baseline test."), Var("[font=prmania_icons]RspladAD[]")) { editor ->
//            BlockTest(editor)
//        })
//        (1..19).forEach { i ->
//            add(Instantiator("test$i", Var("Test$i"), Var(""), Var("[font=prmania_icons]RspladAD[]")) { editor ->
//                BlockTest(editor)
//            })
//        }

        map = tempMap
        list = tempList
    }
}