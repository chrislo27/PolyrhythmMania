package polyrhythmmania.editor.block

import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventRowBlockDespawn
import polyrhythmmania.world.EventRowBlockRetract
import java.util.*


class BlockDespawnPattern(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {

    var rowSetting: RowSetting = RowSetting.ONLY_A
    
    init {
        this.width = 1f
        this.defaultText.bind { Localization.getVar("block.despawnPattern.name").use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return RowSetting.getRows(rowSetting, editor.world).map { row ->
            EventRowBlockDespawn(editor.engine, row, -1, b)
        }
    }

    override fun copy(): BlockDespawnPattern {
        return BlockDespawnPattern(editor).also { 
            this.copyBaseInfoTo(it)
        }
    }
}