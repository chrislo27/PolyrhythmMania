package polyrhythmmania.editor.block

import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventDeployRod
import polyrhythmmania.world.EventRowBlockRetract
import java.util.*


class BlockRetractPistons(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {

    var rowSetting: RowSetting = RowSetting.ONLY_A
    
    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.retractPistons.name").use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return RowSetting.getRows(rowSetting, editor.world).map { row ->
            EventRowBlockRetract(editor.engine, row, -1, b)
        }
    }

    override fun copy(): BlockRetractPistons {
        return BlockRetractPistons(editor).also { 
            this.copyBaseInfoTo(it)
            it.rowSetting = this.rowSetting
        }
    }
}