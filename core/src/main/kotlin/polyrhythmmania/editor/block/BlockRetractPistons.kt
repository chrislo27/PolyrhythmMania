package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventRowBlockRetract
import java.util.*


class BlockRetractPistons(engine: Engine) : Block(engine, BlockRetractPistons.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)
    }
    
    val rowData: RowBlockData = RowBlockData(RowSetting.BOTH)
    
    init {
        this.width = 1f
        val text = Localization.getVar("block.retractPistons.name")
        this.defaultText.bind { text.use() }
        this.defaultTextSecondLine.bind { "[font=rodin]${rowData.rowSetting.use().stringRepresentation}[]" }
        this.secondLineTextAlign = TextAlign.RIGHT
    }
    
    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).map { row ->
            EventRowBlockRetract(engine, row, -1, b)
        }
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            rowData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockRetractPistons {
        return BlockRetractPistons(engine).also { 
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        rowData.writeToJson(obj)
    }

    override fun readFromJson(obj: JsonObject) {
        super.readFromJson(obj)
        rowData.readFromJson(obj)
    }
}