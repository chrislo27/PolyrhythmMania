package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Align
import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.ui.contextmenu.*
import paintbox.ui.control.ComboBox
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.HBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.data.SpotlightActionData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.entity.EntityRodDecor
import polyrhythmmania.world.spotlights.EventSpotlightTransition
import polyrhythmmania.world.spotlights.Spotlight
import polyrhythmmania.world.spotlights.Spotlights
import polyrhythmmania.world.tileset.PaletteTransition
import java.util.*


class BlockSpotlightSwitch(engine: Engine) : AbstractBlockSpotlight(engine, BlockSpotlightSwitch.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
        val ALLOWED_ACTION_TYPES: List<SpotlightActionType> by lazy { SpotlightActionType.VALUES }
        
        fun addTimingModeToContextMenu(ctxmenu: ContextMenu, editor: Editor, timingMode: Var<SpotlightTimingMode>, tooltipLocalizationKey: String) {
            val combobox = ComboBox(SpotlightTimingMode.VALUES, timingMode.getOrCompute()).also { combobox ->
                combobox.markup.set(editor.editorPane.palette.markup)
                combobox.itemStringConverter.set {
                    Localization.getValue(it.localizationNameKey)
                }
                combobox.selectedItem.addListener {
                    timingMode.set(it.getOrCompute())
                }
            }
            val comboboxPane = HBox().also { hbox ->
                hbox.spacing.set(8f)
                hbox.bounds.height.set(32f)
                hbox += TextLabel(Localization.getValue("blockContextMenu.spotlightSwitch.timingMode")).apply {
                    this.markup.set(editor.editorPane.palette.markup)
                    this.renderAlign.set(Align.right)
                    this.bounds.width.set(150f)
                    this.tooltipElement.set(editor.editorPane.createDefaultTooltip(
                            Localization.getValue(tooltipLocalizationKey,
                                    Localization.getValue("blockContextMenu.spotlightSwitch.timingMode.explanation"))
                    ))
                }
                hbox += combobox.apply {
                    this.bindWidthToParent(adjust = -158f)
                }
            }
            ctxmenu.addMenuItem(CustomMenuItem(comboboxPane))
        }

        fun createSpotlightEvents(
                engine: Engine, spotlights: Spotlights,
                rowAActions: List<Pair<Color?, Float?>>, rowDpadActions: List<Pair<Color?, Float?>>,
                blockBeat: Float, timingMode: SpotlightTimingMode, paletteTransition: PaletteTransition
        ): List<Event> {
            val spotlightEvents: MutableList<Event> = mutableListOf()
            val spawnPatternBlocks: List<BlockSpawnPattern> = if (timingMode == SpotlightTimingMode.SPAWN_PATTERN) {
                engine.container?.blocks?.filterIsInstance<BlockSpawnPattern>()?.filter {
                    MathUtils.isEqual(blockBeat, it.beat, 0.01f)
                }?.sortedBy { it.trackIndex } ?: emptyList()
            } else emptyList() // Don't search if we don't need to
            
            /*
            Spawn Pattern mode is handled as such:
            - Block priority is top to bottom in track index order
            - Rows (A and D-pad) are handled independently
            - We can use a row from a SP block if it has at least one PLATFORM or PISTON CubeType in its list
              - Once we find one, we stop searching
            */

            fun compileRow(isA: Boolean, spotlightRow: List<Spotlight>, actionRow: List<Pair<Color?, Float?>>) {
                val timingOffsets = FloatArray(actionRow.size) { 0f }

                if (timingMode == SpotlightTimingMode.IN_ORDER || timingMode == SpotlightTimingMode.SPAWN_PATTERN) {
                    // Note: SPAWN_PATTERN uses IN_ORDER as a fallback
                    for (i in timingOffsets.indices) {
                        timingOffsets[i] = i * (1f / EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT)
                    }
                }
                if (timingMode == SpotlightTimingMode.SPAWN_PATTERN) {
                    if (spawnPatternBlocks.isNotEmpty()) {
                        for (spb in spawnPatternBlocks) {
                            val rowTypes = if (isA) spb.patternData.rowATypes else spb.patternData.rowDpadTypes
                            if (rowTypes.any { it == CubeType.PLATFORM || it == CubeType.PISTON }) {
                                val computed = BlockSpawnPattern.computeTimingOffsetsForRow(rowTypes, spb.getBeatsPerBlock())
                                computed.forEachIndexed { index, fl ->
                                    timingOffsets[index] = fl
                                }
                                break
                            }
                        }
                    }
                }

                actionRow.forEachIndexed { index, (targetColor: Color?, targetStrength: Float?) ->
                    if (targetColor != null || targetStrength != null) {
                        spotlightEvents += EventSpotlightTransition(engine, blockBeat + timingOffsets[index],
                                paletteTransition, spotlightRow[index].lightColor, targetColor, targetStrength)
                    }
                }
            }
            
            compileRow(true, spotlights.spotlightsRowA, rowAActions)
            compileRow(false, spotlights.spotlightsRowDpad, rowDpadActions)
            
            return spotlightEvents
        }
        
        private fun createSpotlightEvents(
                engine: Engine, spotlights: Spotlights,
                patternData: SpotlightActionData,
                blockBeat: Float, timingMode: SpotlightTimingMode, paletteTransition: PaletteTransition
        ): List<Event> {
            fun List<SpotlightActionType>.actionTypesToPairs(): List<Pair<Color?, Float?>> {
                return this.map { Pair(null, when (it) {
                    SpotlightActionType.NO_CHANGE -> null
                    SpotlightActionType.TURN_ON -> 1f
                    SpotlightActionType.TURN_OFF -> 0f
                }) }
            }
            return createSpotlightEvents(engine, spotlights,
                    patternData.rowATypes.actionTypesToPairs(), patternData.rowDpadTypes.actionTypesToPairs(),
                    blockBeat, timingMode, paletteTransition)
        }
    }
    
    private val numSpotlightsPerRow: Int get() = engine.world.spotlights.numPerRow
    
    val ambientLightDarken: BooleanVar = BooleanVar(true)
    var patternData: SpotlightActionData = SpotlightActionData(numSpotlightsPerRow, ALLOWED_ACTION_TYPES, SpotlightActionType.NO_CHANGE)
        private set
    val timingMode: Var<SpotlightTimingMode> = Var(SpotlightTimingMode.SPAWN_PATTERN)

    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightSwitch.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val beat = this.beat
        val spotlights = engine.world.spotlights
        val events = mutableListOf<Event>()

        val timingMode = timingMode.getOrCompute()
        val paletteTransition = PaletteTransition.INSTANT
        events += EventSpotlightTransition(engine, beat, paletteTransition, spotlights.ambientLight, null, if (ambientLightDarken.get()) 0f else 1f)
        events.addAll(createSpotlightEvents(engine, spotlights, patternData, beat, timingMode, paletteTransition))
        
        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spotlightSwitch"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(ambientLightDarken,
                    Localization.getValue("blockContextMenu.spotlightSwitch.ambient"),
                    editor.editorPane.palette.markup).apply {
                this.createTooltip = {
                    it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.spotlightSwitch.ambient.tooltip")))
                }
            })
            ctxmenu.addMenuItem(SeparatorMenuItem())
            patternData.createMenuItems(editor, SpotlightActionType.NO_CHANGE, 0).forEach { ctxmenu.addMenuItem(it) }
            
            addTimingModeToContextMenu(ctxmenu, editor, timingMode, "blockContextMenu.spotlightSwitch.timingMode.tooltip")
        }
    }

    override fun copy(): BlockSpotlightSwitch {
        return BlockSpotlightSwitch(engine).also {
            this.copyBaseInfoTo(it)
            it.ambientLightDarken.set(this.ambientLightDarken.get())
            for (i in 0..<numSpotlightsPerRow) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
            it.timingMode.set(this.timingMode.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("ambientLightDarken", ambientLightDarken.get())
        obj.add("timingMode", timingMode.getOrCompute().jsonId)
        patternData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        this.ambientLightDarken.set(obj.getBoolean("ambientLightDarken", true))
        this.timingMode.set(SpotlightTimingMode.INDEX_MAP[obj.getInt("timingMode", 0)] ?: SpotlightTimingMode.SPAWN_PATTERN)
        this.patternData = SpotlightActionData.readFromJson(obj, ALLOWED_ACTION_TYPES) ?: this.patternData
    }
}
