package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.editor.block.data.PaletteTransitionData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.tileset.PaletteTransition
import java.util.*


class BlockSpotlightAdvanced(engine: Engine) : AbstractBlockSpotlight(engine, BlockSpotlightAdvanced.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }

    val timingMode: Var<SpotlightTimingMode> = Var(SpotlightTimingMode.SPAWN_PATTERN)
    val transitionData: PaletteTransitionData = PaletteTransitionData()

    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightAdvanced.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(500f)
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spotlightAdvanced"), editor.editorPane.palette.markup))
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
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        
        obj.add("timingMode", timingMode.getOrCompute().jsonId)
        transitionData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)

        this.timingMode.set(SpotlightTimingMode.INDEX_MAP[obj.getInt("timingMode", 0)] ?: SpotlightTimingMode.SPAWN_PATTERN)
        transitionData.readFromJson(obj)
    }
}
