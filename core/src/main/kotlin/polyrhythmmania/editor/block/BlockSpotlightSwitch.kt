package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.data.SpotlightActionData
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class BlockSpotlightSwitch(engine: Engine) : Block(engine, BlockSpotlightSwitch.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.FX)
        val ROW_COUNT: Int = 10
        val ALLOWED_ACTION_TYPES: List<SpotlightActionType> by lazy { SpotlightActionType.VALUES }
    }

    var patternData: SpotlightActionData = SpotlightActionData(ROW_COUNT, ALLOWED_ACTION_TYPES, SpotlightActionType.NO_CHANGE)
        private set

    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.spotlightSwitch.name").use() }
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
            ctxmenu.addMenuItem(LabelMenuItem.create(Localization.getValue("blockContextMenu.spotlightSwitch"), editor.editorPane.palette.markup))
            ctxmenu.addMenuItem(SeparatorMenuItem())
            patternData.createMenuItems(editor, SpotlightActionType.NO_CHANGE, 0).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockSpotlightSwitch {
        return BlockSpotlightSwitch(engine).also {
            this.copyBaseInfoTo(it)
            for (i in 0 until ROW_COUNT) {
                it.patternData.rowATypes[i] = this.patternData.rowATypes[i]
                it.patternData.rowDpadTypes[i] = this.patternData.rowDpadTypes[i]
            }
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        patternData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        this.patternData = SpotlightActionData.readFromJson(obj, ALLOWED_ACTION_TYPES) ?: this.patternData
    }
}
