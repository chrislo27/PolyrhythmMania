package polyrhythmmania.editor.block

import paintbox.binding.ReadOnlyVar
import polyrhythmmania.Localization
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.storymode.*
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
    val editorFlags: EnumSet<EditorSpecialFlags>
    
    fun canBeShown(allowedFlags: EnumSet<EditorSpecialFlags>): Boolean {
        return this.editorFlags.none { f -> f !in allowedFlags }
    }
}

data class ListCategory(
    val categoryID: String,
    override val name: ReadOnlyVar<String>,
    override val summary: ReadOnlyVar<String>,
    override val desc: ReadOnlyVar<String>,
    override val editorFlags: EnumSet<EditorSpecialFlags>,
) : ObjectListable {
    override val allowedTracks: EnumSet<BlockType>? = null
}

/**
 * An [Instantiator] is effectively a [Block] factory with extra metadata for the UI.
 */
data class Instantiator<B : Block>(
    val id: String,
    val blockClass: Class<B>,
    override val name: ReadOnlyVar<String>,
    override val summary: ReadOnlyVar<String>,
    override val desc: ReadOnlyVar<String>,
    override val allowedTracks: EnumSet<BlockType>,
    val deprecatedIDs: Set<String> = emptySet(),
    val onlyOne: Boolean = false,
    override val editorFlags: EnumSet<EditorSpecialFlags> = EnumSet.noneOf(EditorSpecialFlags::class.java),
    val factory: Instantiator<B>.(Engine) -> B
) : ObjectListable

object Instantiators {
    
    private const val CATEGORY_CORE: String = "core"
    private const val CATEGORY_FX: String = "fx"
    private const val CATEGORY_ADVANCED: String = "advanced"
    private const val CATEGORY_SPOTLIGHTS: String = "spotlights"
    private const val CATEGORY_STORYMODE: String = "storymode"

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
            Localization.getVar("instantiatorCategory.core.desc"),
            EnumSet.noneOf(EditorSpecialFlags::class.java)
        ))
        add(ListCategory(CATEGORY_FX,
            Localization.getVar("instantiatorCategory.fx.name"),
            Localization.getVar("instantiatorCategory.fx.summary"),
            Localization.getVar("instantiatorCategory.fx.desc"),
            EnumSet.noneOf(EditorSpecialFlags::class.java)))
        add(ListCategory(CATEGORY_SPOTLIGHTS,
                Localization.getVar("instantiatorCategory.spotlights.name"),
                Localization.getVar("instantiatorCategory.spotlights.summary"),
                Localization.getVar("instantiatorCategory.spotlights.desc"),
                EnumSet.noneOf(EditorSpecialFlags::class.java)))
        add(ListCategory(CATEGORY_ADVANCED,
            Localization.getVar("instantiatorCategory.advanced.name"),
            Localization.getVar("instantiatorCategory.advanced.summary"),
            Localization.getVar("instantiatorCategory.advanced.desc"),
            EnumSet.noneOf(EditorSpecialFlags::class.java)))
        add(ListCategory(CATEGORY_STORYMODE,
            ReadOnlyVar.const("Story Mode Only"),
            ReadOnlyVar.const("Special Category: Story Mode"),
            ReadOnlyVar.const("Blocks used for Story Mode.\n\nWill not function in regular levels."),
            EnumSet.of(EditorSpecialFlags.STORY_MODE)))
        
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
        add(CATEGORY_FX, Instantiator("condApplause", BlockCondApplause::class.java,
                Localization.getVar("instantiator.condApplause.name"),
                Localization.getVar("instantiator.condApplause.summary"),
                Localization.getVar("instantiator.condApplause.desc"),
                BlockCondApplause.BLOCK_TYPES) { engine ->
            BlockCondApplause(engine)
        })
        add(CATEGORY_FX, Instantiator("paletteChange", BlockPaletteChange::class.java,
                Localization.getVar("instantiator.paletteChange.name"),
                Localization.getVar("instantiator.paletteChange.summary"),
                Localization.getVar("instantiator.paletteChange.desc"),
                BlockPaletteChange.BLOCK_TYPES, deprecatedIDs = setOf("tilesetChange")) { engine ->
            BlockPaletteChange(engine)
        })
        add(CATEGORY_FX, Instantiator("texPackChange", BlockTexPackChange::class.java,
                Localization.getVar("instantiator.texPackChange.name"),
                Localization.getVar("instantiator.texPackChange.summary"),
                Localization.getVar("instantiator.texPackChange.desc"),
                BlockTexPackChange.BLOCK_TYPES) { engine ->
            BlockTexPackChange(engine)
        })
        add(CATEGORY_FX, Instantiator("textbox", BlockTextbox::class.java,
                Localization.getVar("instantiator.textbox.name"),
                Localization.getVar("instantiator.textbox.summary"),
                Localization.getVar("instantiator.textbox.desc"),
                BlockTextbox.BLOCK_TYPES) { engine ->
            BlockTextbox(engine)
        })
        add(CATEGORY_FX, Instantiator("subtitle", BlockSubtitle::class.java,
                Localization.getVar("instantiator.subtitle.name"),
                Localization.getVar("instantiator.subtitle.summary"),
                Localization.getVar("instantiator.subtitle.desc"),
                BlockSubtitle.BLOCK_TYPES) { engine ->
            BlockSubtitle(engine)
        })
        add(CATEGORY_FX, Instantiator("songInfoCard", BlockSongInfoCard::class.java,
                Localization.getVar("instantiator.songInfoCard.name"),
                Localization.getVar("instantiator.songInfoCard.summary"),
                Localization.getVar("instantiator.songInfoCard.desc"),
                BlockSongInfoCard.BLOCK_TYPES) { engine ->
            BlockSongInfoCard(engine)
        })
        add(CATEGORY_FX, Instantiator("zoom", BlockZoom::class.java,
                Localization.getVar("instantiator.zoom.name"),
                Localization.getVar("instantiator.zoom.summary"),
                Localization.getVar("instantiator.zoom.desc"),
                BlockZoom.BLOCK_TYPES) { engine ->
            BlockZoom(engine)
        })
        
        // Spotlight instantiators
        add(CATEGORY_SPOTLIGHTS, Instantiator("spotlightSwitch", BlockSpotlightSwitch::class.java,
                Localization.getVar("instantiator.spotlightSwitch.name"),
                Localization.getVar("instantiator.spotlightSwitch.summary"),
                Localization.getVar("instantiator.spotlightSwitch.desc"),
                BlockSpotlightSwitch.BLOCK_TYPES) { engine ->
            BlockSpotlightSwitch(engine)
        })
        add(CATEGORY_SPOTLIGHTS, Instantiator("spotlightAdvanced", BlockSpotlightAdvanced::class.java,
                Localization.getVar("instantiator.spotlightAdvanced.name"),
                Localization.getVar("instantiator.spotlightAdvanced.summary"),
                Localization.getVar("instantiator.spotlightAdvanced.desc"),
                BlockSpotlightAdvanced.BLOCK_TYPES) { engine ->
            BlockSpotlightAdvanced(engine)
        })
        
        // Advanced instantiators
        add(CATEGORY_ADVANCED, Instantiator("selectiveSpawn", BlockSelectiveSpawnPattern::class.java,
                Localization.getVar("instantiator.selectiveSpawn.name"),
                Localization.getVar("instantiator.selectiveSpawn.summary"),
                Localization.getVar("instantiator.selectiveSpawn.desc"),
                BlockSelectiveSpawnPattern.BLOCK_TYPES) { engine ->
            BlockSelectiveSpawnPattern(engine)
        })

        // Story Mode instantiators
        add(CATEGORY_STORYMODE, Instantiator("storyMode_memo", BlockMemoStoryMode::class.java,
                ReadOnlyVar.const("Memo (SM)"),
                ReadOnlyVar.const("A block for storing a comment string."),
                ReadOnlyVar.const("This block does not affect the game.\n\nIt only serves as a comment block.\n\nYou can put up to two separate strings."),
                BlockMemoStoryMode.BLOCK_TYPES, editorFlags = EnumSet.of(EditorSpecialFlags.STORY_MODE)) { engine ->
            BlockMemoStoryMode(engine)
        })
        add(CATEGORY_STORYMODE, Instantiator("storyMode_deployRod", BlockDeployRodStoryMode::class.java,
                ReadOnlyVar.const("Deploy Rod (SM)"),
                ReadOnlyVar.const("Deploys a rod, with special settings."),
                ReadOnlyVar.const("Deploys a rod. You can change how fast it moves.\n\nYou can also mark this rod as defective. This should be\nused with the appropriate setting in\nGlobal World Settings."),
                BlockDeployRod.BLOCK_TYPES, editorFlags = EnumSet.of(EditorSpecialFlags.STORY_MODE)) { engine ->
            BlockDeployRodStoryMode(engine)
        })
        add(CATEGORY_STORYMODE, Instantiator("storyMode_spawnPattern", BlockSpawnPatternStoryMode::class.java,
                ReadOnlyVar.const("Spawn Pattern (SM)"),
                ReadOnlyVar.const("Spawns a pattern, with special settings."),
                ReadOnlyVar.const("Same as the normal Spawn Pattern block,\nbut lets you change the speed."),
                BlockSpawnPattern.BLOCK_TYPES, editorFlags = EnumSet.of(EditorSpecialFlags.STORY_MODE)) { engine ->
            BlockSpawnPatternStoryMode(engine)
        })
        add(CATEGORY_STORYMODE, Instantiator("storyMode_panCamera", Block8BallCamera::class.java,
                ReadOnlyVar.const("Cts. Camera Pan"),
                ReadOnlyVar.const("Pans the camera for continuous (cts.) mode."),
                ReadOnlyVar.const("This pans the camera forward for [i]continuous mode[].\n\nYou should place this to be aligned at the same beat\nas every [b]Despawn Pattern[] block.\nEvery single [b]Despawn Pattern[] block needs to have\nits tail end disabled."),
                Block8BallCamera.BLOCK_TYPES, editorFlags = EnumSet.of(EditorSpecialFlags.STORY_MODE)) { engine ->
            Block8BallCamera(engine)
        })
        add(CATEGORY_STORYMODE, Instantiator("storyMode_monsterGoalPoints_start", BlockMonsterGoalPointStart::class.java,
                ReadOnlyVar.const("Monster Goal Start"),
                ReadOnlyVar.const("Sets the start point for Monster Goal."),
                ReadOnlyVar.const("This sets the start point for Monster Goal.\nThis should be placed [i]2 beats[] before the FIRST INPUT.\n\nNeeds to be used in tandem with [b]Monster Goal End[]."),
                BlockMonsterGoalPoints.BLOCK_TYPES, editorFlags = EnumSet.of(EditorSpecialFlags.STORY_MODE), onlyOne = true) { engine ->
            BlockMonsterGoalPointStart(engine)
        })
        add(CATEGORY_STORYMODE, Instantiator("storyMode_monsterGoalPoints_end", BlockMonsterGoalPointEnd::class.java,
                ReadOnlyVar.const("Monster Goal End"),
                ReadOnlyVar.const("Sets the end point for Monster Goal."),
                ReadOnlyVar.const("This sets the end point for Monster Goal.\nThis should be placed where the last input is.\nThe monster goal will stop counting down at this point.\n\nNeeds to be used in tandem with [b]Monster Goal Start[]."),
                BlockMonsterGoalPoints.BLOCK_TYPES, editorFlags = EnumSet.of(EditorSpecialFlags.STORY_MODE), onlyOne = true) { engine ->
            BlockMonsterGoalPointEnd(engine)
        })

        instantiatorMap = tempMap
        instantiatorList = tempList
        fullList = tempListAll
        categoryList = tempCategoryList
        categoryMap = tempCategoryMap
        instantiatorCategories = tempInstCats
        classMapping = instantiatorList.associateBy { it.blockClass }
    }
}