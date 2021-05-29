package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.world.EventDeployRod
import java.util.*


class BlockDeployRod(engine: Engine) : Block(engine, EnumSet.of(BlockType.INPUT)) {

    val rowData: RowBlockData = RowBlockData()

    init {
        this.width = 1f
        val text = Localization.getVar("block.deployRod.name", Var.bind { 
            rowData.getSymbol(this)
        })
        this.defaultText.bind { text.use() }
        val block = this.defaultTextBlock.getOrCompute()
        block.computeLayouts()
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat - 4
        return RowSetting.getRows(rowData.rowSetting.getOrCompute(), engine.world).map { row ->
            EventDeployRod(engine, row, b)
        }
    }

    override fun createContextMenu(editor: Editor): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            rowData.createMenuItems(editor).forEach { ctxmenu.addMenuItem(it) }
        }
    }

    override fun copy(): BlockDeployRod {
        return BlockDeployRod(engine).also {
            this.copyBaseInfoTo(it)
            it.rowData.rowSetting.set(this.rowData.rowSetting.getOrCompute())
        }
    }
}