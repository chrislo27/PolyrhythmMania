package polyrhythmmania.editor.block

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
        const val ROW_COUNT: Int = Spotlights.NUM_ON_ROW
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
        val ALLOWED_ACTION_TYPES: List<SpotlightActionType> by lazy { SpotlightActionType.VALUES }
    }
    
    val ambientLightDarken: BooleanVar = BooleanVar(true)
    var patternData: SpotlightActionData = SpotlightActionData(ROW_COUNT, ALLOWED_ACTION_TYPES, SpotlightActionType.NO_CHANGE)
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
        
        events += EventSpotlightTransition(engine, beat, PaletteTransition.INSTANT, spotlights.ambientLight, null, if (ambientLightDarken.get()) 0f else 1f)
        
        val timingMode = timingMode.getOrCompute()
        val spotlightEvents: MutableList<Event> = mutableListOf()
        
        val spawnPatternBlocks: List<BlockSpawnPattern> = if (timingMode == SpotlightTimingMode.SPAWN_PATTERN) {
            engine.container?.blocks?.filterIsInstance<BlockSpawnPattern>()?.filter { 
                MathUtils.isEqual(this.beat, it.beat, 0.01f)
            }?.sortedBy { it.trackIndex } ?: emptyList()
        } else emptyList() // Don't search if we don't need to

        
        /*
        Spawn Pattern mode is handled as such:
        - Block priority is top to bottom in track index order
        - Rows (A and D-pad) are handled independently
        - We can use a row from a SP block if it has at least one PLATFORM or PISTON CubeType in its list
          - Once we find one, we stop searching
        */

        fun compileRow(isA: Boolean, spotlightRow: List<Spotlight>, actionRow: List<SpotlightActionType>) {
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
            
            actionRow.forEachIndexed { index, action -> 
                if (action != SpotlightActionType.NO_CHANGE) {
                    spotlightEvents += EventSpotlightTransition(engine, beat + timingOffsets[index],
                            PaletteTransition.INSTANT, spotlightRow[index].lightColor, null,
                            if (action == SpotlightActionType.TURN_OFF) 0f else 1f)
                }
            }
        }
        compileRow(true, spotlights.spotlightsRowA, patternData.rowATypes)
        compileRow(false, spotlights.spotlightsRowDpad, patternData.rowDpadTypes)

        events.addAll(spotlightEvents)
        
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
            
            val combobox = ComboBox(SpotlightTimingMode.VALUES, timingMode.getOrCompute()).also { combobox ->
                combobox.markup.set(editor.editorPane.palette.markup)
                combobox.itemStringConverter.set {
                    Localization.getValue(it.localizationNameKey)
                }
                combobox.selectedItem.addListener {
                    this.timingMode.set(it.getOrCompute())
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
                            Localization.getValue("blockContextMenu.spotlightSwitch.timingMode.tooltip",
                                    Localization.getValue("editor.track.input_0"),
                                    Localization.getValue("editor.track.input_1"))
                    ))
                }
                hbox += combobox.apply {
                    this.bindWidthToParent(adjust = -158f)
                }
            }
            ctxmenu.addMenuItem(CustomMenuItem(comboboxPane))
        }
    }

    override fun copy(): BlockSpotlightSwitch {
        return BlockSpotlightSwitch(engine).also {
            this.copyBaseInfoTo(it)
            it.ambientLightDarken.set(this.ambientLightDarken.get())
            for (i in 0 until ROW_COUNT) {
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

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.ambientLightDarken.set(obj.getBoolean("ambientLightDarken", true))
        this.timingMode.set(SpotlightTimingMode.INDEX_MAP[obj.getInt("timingMode", 0)] ?: SpotlightTimingMode.SPAWN_PATTERN)
        this.patternData = SpotlightActionData.readFromJson(obj, ALLOWED_ACTION_TYPES) ?: this.patternData
    }
}
