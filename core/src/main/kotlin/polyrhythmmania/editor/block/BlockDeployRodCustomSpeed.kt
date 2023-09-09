package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.EditorSpecialFlags
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventDeployRod
import java.util.*


class BlockDeployRodCustomSpeed(
    engine: Engine,
    blockTypes: EnumSet<BlockType> = BLOCK_TYPES,
) : BlockDeployRod(engine, blockTypes), CustomSpeedBlock {

    override var xUnitsPerBeat: Float = CustomSpeedBlock.DEFAULT_X_UNITS_PER_BEAT

    init {
        this.defaultText.bind { Localization.getVar("block.deployRodCustomSpeed.name").use() }
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat - 4
        return RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).map { row ->
            EventDeployRod(engine, row, b) { rod ->
                rod.xUnitsPerBeat = this.xUnitsPerBeat.coerceAtLeast(CustomSpeedBlock.MIN_X_UNITS_PER_BEAT)
            }
        }
    }

    override fun copy(): BlockDeployRodCustomSpeed {
        return BlockDeployRodCustomSpeed(engine).also {
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
            it.xUnitsPerBeat = this.xUnitsPerBeat
        }
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return super.createContextMenu(editor).also { ctxmenu ->
            ctxmenu.defaultWidth.set(300f)
            ctxmenu.addMenuItem(SeparatorMenuItem())
            ctxmenu.addToContextMenu(editor)
        }
    }

    override fun writeToJson(obj: JsonObject) {
        super.writeToJson(obj)
        obj.add("xUnitsPerBeat", this.xUnitsPerBeat)
    }

    override fun readFromJson(obj: JsonObject, editorFlags: EnumSet<EditorSpecialFlags>) {
        super.readFromJson(obj, editorFlags)
        this.xUnitsPerBeat =
            obj.getFloat("xUnitsPerBeat", CustomSpeedBlock.DEFAULT_X_UNITS_PER_BEAT).coerceAtLeast(CustomSpeedBlock.MIN_X_UNITS_PER_BEAT)
    }
}