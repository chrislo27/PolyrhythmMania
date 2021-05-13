package polyrhythmmania.editor.block

import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventDeployRod
import java.util.*


class BlockDeployRod(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {
    
    var rowSetting: RowSetting = RowSetting.ONLY_A
    
    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.deployRod.name").use() }
        val block = this.defaultTextBlock.getOrCompute()
        block.computeLayouts()
        println("$block  ${block.firstCapHeight}  ${block.height}  ${block.lastDescent}")
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat - 4
        return RowSetting.getRows(rowSetting, editor.world).map { row ->
            EventDeployRod(editor.engine, row, b)
        }
    }

    override fun copy(): BlockDeployRod {
        return BlockDeployRod(editor).also { 
            this.copyBaseInfoTo(it)
            it.rowSetting = this.rowSetting
        }
    }
}