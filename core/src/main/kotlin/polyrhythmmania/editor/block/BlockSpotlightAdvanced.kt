package polyrhythmmania.editor.block

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import paintbox.ui.contextmenu.SimpleMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.data.PaletteTransitionData
import polyrhythmmania.editor.block.data.SpotlightsColorData
import polyrhythmmania.editor.block.data.SwitchedLightColor
import polyrhythmmania.editor.pane.dialog.SpotlightEditDialog
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.spotlights.EventSpotlightTransition
import polyrhythmmania.world.tileset.PaletteTransition
import java.util.*


class BlockSpotlightAdvanced(engine: Engine) : AbstractBlockSpotlight(engine, BlockSpotlightAdvanced.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }

    val timingMode: Var<SpotlightTimingMode> = Var(SpotlightTimingMode.SPAWN_PATTERN)
    val transitionData: PaletteTransitionData = PaletteTransitionData(PaletteTransition.INSTANT)
    val colorData: SpotlightsColorData = SpotlightsColorData()

    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightAdvanced.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val beat = this.beat
        val spotlights = engine.world.spotlights
        val events = mutableListOf<Event>()

        val timingMode = timingMode.getOrCompute()
        val paletteTransition = transitionData.paletteTransition.getOrCompute()
        val ambientLight = colorData.ambientLight
        if (ambientLight.enabled) {
            events += EventSpotlightTransition(engine, beat, paletteTransition, spotlights.ambientLight, ambientLight.color, ambientLight.strength)
        }
        events.addAll(BlockSpotlightSwitch.createSpotlightEvents(engine, spotlights,
                colorData.rowA.map(SwitchedLightColor::toPair), colorData.rowDpad.map(SwitchedLightColor::toPair),
                beat, timingMode, paletteTransition))

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(500f)
            ctxmenu.addMenuItem(SimpleMenuItem.create(Localization.getValue("blockContextMenu.spotlightAdvanced"),
                    editor.editorPane.palette.markup).apply {
                this.onAction = {
                    val editorPane = editor.editorPane
                    editorPane.openDialog(SpotlightEditDialog(editorPane, colorData))
                }
            })
            ctxmenu.addMenuItem(SeparatorMenuItem())
            BlockSpotlightSwitch.addTimingModeToContextMenu(ctxmenu, editor, timingMode, "blockContextMenu.spotlightAdvanced.timingMode.tooltip")
            ctxmenu.addMenuItem(SeparatorMenuItem())
            transitionData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockSpotlightAdvanced {
        return BlockSpotlightAdvanced(engine).also {
            this.copyBaseInfoTo(it)

            it.timingMode.set(this.timingMode.getOrCompute())
            it.transitionData.paletteTransition.set(this.transitionData.paletteTransition.getOrCompute().copy())
            this.colorData.copyTo(it.colorData)
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        
        obj.add("timingMode", this.timingMode.getOrCompute().jsonId)
        this.transitionData.writeToJson(obj)
        obj.add("colorData", Json.`object`().also { o ->
            this.colorData.writeToJson(o)
        })
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)

        this.timingMode.set(SpotlightTimingMode.INDEX_MAP[obj.getInt("timingMode", 0)] ?: SpotlightTimingMode.SPAWN_PATTERN)
        this.transitionData.readFromJson(obj)
        val colorDataObj = obj.get("colorData")?.asObject()
        if (colorDataObj != null) {
            this.colorData.readFromJson(colorDataObj)
        }
    }
}
