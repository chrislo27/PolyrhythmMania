package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.font.TextAlign
import paintbox.ui.contextmenu.CheckBoxMenuItem
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventRowBlockDespawn
import java.util.*


class BlockDespawnPattern(engine: Engine) : Block(engine, BlockDespawnPattern.BLOCK_TYPES) {

    companion object {
        val BLOCK_TYPES: EnumSet<BlockType> = EnumSet.of(BlockType.INPUT)
    }
    
    val rowData: RowBlockData = RowBlockData(RowSetting.BOTH)
    val disableTailEnd: BooleanVar = BooleanVar(false)
    
    init {
        this.width = 1f
//        this.textScale = 0.9f
        val text = Localization.getVar("block.despawnPattern.name", Var.bind {
//            rowData.getSymbolAsListArg(this)
            listOf("")
        })
        this.defaultText.bind { text.use() }
        this.defaultTextSecondLine.bind { "[font=rodin]${rowData.rowSetting.use().stringRepresentation}[]" }
        this.secondLineTextAlign = TextAlign.RIGHT
    }
    
    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return if (disableTailEnd.get()) {
            RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).flatMap { row ->
                (0..<BlockSpawnPattern.ROW_COUNT).map { idx ->
                    EventRowBlockDespawn(engine, row, idx, b)
                }
            }
        } else {
            RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).map { row ->
                EventRowBlockDespawn(engine, row, -1, b)
            }
        }
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.defaultWidth.set(350f)
            rowData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addMenuItem(CheckBoxMenuItem.create(disableTailEnd,
                    Localization.getValue("blockContextMenu.despawnPattern.disableTailEnd"),
                    editor.editorPane.palette.markup))
        }
    }

    override fun copy(): BlockDespawnPattern {
        return BlockDespawnPattern(engine).also { 
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
            it.disableTailEnd.set(this.disableTailEnd.get())
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        rowData.writeToJson(obj)
        if (disableTailEnd.get()) {
            obj.add("disableTailEnd", true)
        }
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        rowData.readFromJson(obj)
        val disableTailEndValue = obj.get("disableTailEnd")
        if (disableTailEndValue != null && disableTailEndValue.isBoolean) {
            disableTailEnd.set(disableTailEndValue.asBoolean())
        }
    }
}