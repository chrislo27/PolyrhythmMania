package polyrhythmmania.editor.block


import com.eclipsesource.json.JsonObject
import paintbox.font.TextAlign
import paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import java.util.*


class BlockCondApplause(engine: Engine) : Block(engine, BlockCondApplause.BLOCK_TYPES) {
    
    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT, BlockType.FX)
    }

    val rowData: RowBlockData = RowBlockData(RowSetting.BOTH)

    init {
        this.width = 0.5f
        val text = Localization.getVar("block.condApplause.name")
        this.defaultText.bind { text.use() }
        this.defaultTextSecondLine.bind { "[font=rodin]${rowData.rowSetting.use().stringRepresentation}[]" }
        this.secondLineTextAlign = TextAlign.RIGHT
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return listOf()
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            rowData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockCondApplause {
        return BlockCondApplause(engine).also {
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