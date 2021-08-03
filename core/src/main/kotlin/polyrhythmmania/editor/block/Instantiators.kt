package polyrhythmmania.editor.block

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization
import polyrhythmmania.engine.Engine
import java.util.*


/**
 * Indicates that this object can appear in the instantiator list, but may not necessarily be an [Instantiator]
 */
interface ObjectListable {
    val name: ReadOnlyVar<String>
    val summary: ReadOnlyVar<String>
    val desc: ReadOnlyVar<String>
    val allowedTracks: EnumSet<BlockType>?
}

data class ListCategory(
        val categoryID: String,
        override val name: ReadOnlyVar<String>,
        override val summary: ReadOnlyVar<String>,
        override val desc: ReadOnlyVar<String>,
): ObjectListable {
    override val allowedTracks: EnumSet<BlockType>? = null
}

/**
 * An [Instantiator] is effectively a [Block] factory with extra metadata for the UI.
 */
data class Instantiator<B : Block>(val id: String,
                                   val blockClass: Class<B>,
                                   override val name: ReadOnlyVar<String>,
                                   override val summary: ReadOnlyVar<String>,
                                   override val desc: ReadOnlyVar<String>,
                                   override val allowedTracks: EnumSet<BlockType>,
                                   val deprecatedIDs: Set<String> = emptySet(),
                                   val onlyOne: Boolean = false,
                                   val factory: Instantiator<B>.(Engine) -> B) : ObjectListable

object Instantiators {
    
    private const val CATEGORY_CORE: String = "core"
    private const val CATEGORY_FX: String = "fx"
    private const val CATEGORY_ADVANCED: String = "advanced"

    val endStateInstantiator: Instantiator<BlockEndState>
    
    val instantiatorMap: Map<String, Instantiator<*>>
    val instantiatorList: List<Instantiator<*>>
    
    val fullList: List<ObjectListable>
    val categoryMap: Map<String, List<Instantiator<*>>>
    val categoryList: List<ListCategory>
    val instantiatorCategories: Map<Instantiator<*>, String>


    val classMapping: Map<Class<*>, Instantiator<*>>

    init {
        val tempMap = mutableMapOf<String, Instantiator<*>>()
        val tempList = mutableListOf<Instantiator<*>>()
        val tempListAll = mutableListOf<ObjectListable>()
        val tempCategoryList = mutableListOf<ListCategory>()
        val tempCategoryMap = mutableMapOf<String, MutableList<Instantiator<*>>>()
        val tempInstCats = mutableMapOf<Instantiator<*>, String>()

        fun addObj(obj: ObjectListable) {
            tempListAll += obj
        }
        fun add(category: ListCategory) {
            addObj(category)
            tempCategoryList.add(category)
        }
        fun add(category: String, instantiator: Instantiator<*>) {
            addObj(instantiator as ObjectListable)
            tempMap[instantiator.id] = instantiator
            instantiator.deprecatedIDs.forEach { tempMap[it] = instantiator }
            tempList += instantiator
            tempCategoryMap.getOrPut(category) { mutableListOf() }.add(instantiator)
            tempInstCats[instantiator] = category
        }

        // Categories
        add(ListCategory(CATEGORY_CORE,
                Localization.getVar("instantiatorCategory.core.name"),
                Localization.getVar("instantiatorCategory.core.summary"),
                Localization.getVar("instantiatorCategory.core.desc")))
        add(ListCategory(CATEGORY_FX,
                Localization.getVar("instantiatorCategory.fx.name"),
                Localization.getVar("instantiatorCategory.fx.summary"),
                Localization.getVar("instantiatorCategory.fx.desc")))
        add(ListCategory(CATEGORY_ADVANCED,
                Localization.getVar("instantiatorCategory.advanced.name"),
                Localization.getVar("instantiatorCategory.advanced.summary"),
                Localization.getVar("instantiatorCategory.advanced.desc")))
        
        // Basic instantiators
        endStateInstantiator = Instantiator("endState", BlockEndState::class.java,
                Localization.getVar("instantiator.endState.name"),
                Localization.getVar("instantiator.endState.summary"),
                Localization.getVar("instantiator.endState.desc"),
                BlockEndState.BLOCK_TYPES,
                onlyOne = true) { engine ->
            BlockEndState(engine)
        }
        add(CATEGORY_CORE, endStateInstantiator)
        add(CATEGORY_CORE, Instantiator("spawnPattern", BlockSpawnPattern::class.java,
                Localization.getVar("instantiator.spawnPattern.name"),
                Localization.getVar("instantiator.spawnPattern.summary"),
                Localization.getVar("instantiator.spawnPattern.desc"),
                BlockSpawnPattern.BLOCK_TYPES) { engine ->
            BlockSpawnPattern(engine)
        })
        add(CATEGORY_CORE, Instantiator("deployRod", BlockDeployRod::class.java,
                Localization.getVar("instantiator.deployRod.name"),
                Localization.getVar("instantiator.deployRod.summary"),
                Localization.getVar("instantiator.deployRod.desc"),
                BlockDeployRod.BLOCK_TYPES) { engine ->
            BlockDeployRod(engine)
        })
        add(CATEGORY_CORE, Instantiator("retractPistons", BlockRetractPistons::class.java,
                Localization.getVar("instantiator.retractPistons.name"),
                Localization.getVar("instantiator.retractPistons.summary"),
                Localization.getVar("instantiator.retractPistons.desc"),
                BlockRetractPistons.BLOCK_TYPES) { engine ->
            BlockRetractPistons(engine)
        })
        add(CATEGORY_CORE, Instantiator("despawnPattern", BlockDespawnPattern::class.java,
                Localization.getVar("instantiator.despawnPattern.name"),
                Localization.getVar("instantiator.despawnPattern.summary"),
                Localization.getVar("instantiator.despawnPattern.desc"),
                BlockDespawnPattern.BLOCK_TYPES) { engine ->
            BlockDespawnPattern(engine)
        })
        add(CATEGORY_CORE, Instantiator("skillStar", BlockSkillStar::class.java,
                Localization.getVar("instantiator.skillStar.name"),
                Localization.getVar("instantiator.skillStar.summary"),
                Localization.getVar("instantiator.skillStar.desc"),
                BlockSkillStar.BLOCK_TYPES,
                onlyOne = true) { engine ->
            BlockSkillStar(engine)
        })
        
        // FX instantiators
        add(CATEGORY_FX, Instantiator("paletteChange", BlockPaletteChange::class.java,
                Localization.getVar("instantiator.paletteChange.name"),
                Localization.getVar("instantiator.paletteChange.summary"),
                Localization.getVar("instantiator.paletteChange.desc"),
                BlockPaletteChange.BLOCK_TYPES, deprecatedIDs = setOf("tilesetChange")) { engine ->
            BlockPaletteChange(engine)
        })
        add(CATEGORY_FX, Instantiator("textbox", BlockTextbox::class.java,
                Localization.getVar("instantiator.textbox.name"),
                Localization.getVar("instantiator.textbox.summary"),
                Localization.getVar("instantiator.textbox.desc"),
                BlockTextbox.BLOCK_TYPES) { engine ->
            BlockTextbox(engine)
        })
        add(CATEGORY_FX, Instantiator("condApplause", BlockCondApplause::class.java,
                Localization.getVar("instantiator.condApplause.name"),
                Localization.getVar("instantiator.condApplause.summary"),
                Localization.getVar("instantiator.condApplause.desc"),
                BlockCondApplause.BLOCK_TYPES) { engine ->
            BlockCondApplause(engine)
        })
        
        // Advanced instantiators
        add(CATEGORY_ADVANCED, Instantiator("selectiveSpawn", BlockSelectiveSpawnPattern::class.java,
                Localization.getVar("instantiator.selectiveSpawn.name"),
                Localization.getVar("instantiator.selectiveSpawn.summary"),
                Localization.getVar("instantiator.selectiveSpawn.desc"),
                BlockSelectiveSpawnPattern.BLOCK_TYPES) { engine ->
            BlockSelectiveSpawnPattern(engine)
        })

//        add(Instantiator("baselineTest", Var("Baseline Test"), Var("Description baseline test."), Var("[font=prmania_icons]RspladAD[]")) { engine ->
//            BlockTest(engine)
//        })
//        (1..19).forEach { i ->
//            add(Instantiator("test$i", Var("Test$i"), Var(""), Var("[font=prmania_icons]RspladAD[]")) { engine ->
//                BlockTest(engine)
//            })
//        }

        instantiatorMap = tempMap
        instantiatorList = tempList
        fullList = tempListAll
        categoryList = tempCategoryList
        categoryMap = tempCategoryMap
        instantiatorCategories = tempInstCats
        classMapping = instantiatorList.associateBy { it.blockClass }
    }
}