package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.data.SpotlightActionData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class BlockSpotlightPalette(engine: Engine) : AbstBlockSpotlight(engine, BlockSpotlightPalette.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
    }


    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightPalette.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        val events = mutableListOf<Event>()

        val world = engine.world
        // TODO
//        val silent = false
//        events += compileRow(b, patternData.rowATypes, world.rowA, EntityPiston.Type.PISTON_A, 0, false, silent)
//        events += compileRow(b, patternData.rowDpadTypes, world.rowDpad, EntityPiston.Type.PISTON_DPAD, 0, false, silent)

        return events
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(300f)
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spotlightPalette"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
        }
    }

    override fun copy(): BlockSpotlightPalette {
        return BlockSpotlightPalette(engine).also {
            this.copyBaseInfoTo(it)
            
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        
    }
}
