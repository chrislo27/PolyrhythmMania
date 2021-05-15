package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventRowBlockDespawn
import polyrhythmmania.world.EventRowBlockRetract
import java.util.*


class BlockDespawnPattern(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {

    val rowSettingBehaviour: RowBasedBehaviour = RowBasedBehaviour()
    
    init {
        this.width = 1f
        val text = Localization.getVar("block.despawnPattern.name", Var.bind {
            rowSettingBehaviour.getSymbol(this)
        })
        this.defaultText.bind { text.use() }
    }
    
    override fun compileIntoEvents(): List<Event> {
        val b = this.beat
        return RowSetting.getRows(rowSettingBehaviour.rowSetting.getOrCompute(), editor.world).map { row ->
            EventRowBlockDespawn(editor.engine, row, -1, b)
        }
    }

    override fun createContextMenu(): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(rowSettingBehaviour.createMenuItem(editor))
        }
    }

    override fun copy(): BlockDespawnPattern {
        return BlockDespawnPattern(editor).also { 
            this.copyBaseInfoTo(it)
            it.rowSettingBehaviour.rowSetting.set(this.rowSettingBehaviour.rowSetting.getOrCompute())
        }
    }
}