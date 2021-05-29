package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventRowBlockDespawn
import java.util.*


class BlockDespawnPattern(engine: Engine) : Block(engine, EnumSet.of(BlockType.INPUT)) {

    val rowData: RowBlockData = RowBlockData()
    
    init {
        this.width = 1f
        val text = Localization.getVar("block.despawnPattern.name", Var.bind {
            rowData.getSymbol(this)
        })
        this.defaultText.bind { text.use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).map { row ->
            EventRowBlockDespawn(engine, row, -1, b)
        }
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            rowData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockDespawnPattern {
        return BlockDespawnPattern(engine).also { 
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
        }
    }
}