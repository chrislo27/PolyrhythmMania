package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.Pane
import io.github.chrislo27.paintbox.ui.area.Insets
import io.github.chrislo27.paintbox.ui.contextmenu.ContextMenu
import io.github.chrislo27.paintbox.ui.contextmenu.CustomMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.RowSelectorMenuPane
import polyrhythmmania.engine.Engine
import polyrhythmmania.engine.Event
import polyrhythmmania.util.RodinSpecialChars
import polyrhythmmania.world.EventDeployRod
import java.util.*


class BlockDeployRod(editor: Editor) : Block(editor, EnumSet.of(BlockType.INPUT)) {

    val rowSettingBehaviour: RowBasedBehaviour = RowBasedBehaviour()

    init {
        this.width = 1f
        val text = Localization.getVar("block.deployRod.name", Var.bind { 
            rowSettingBehaviour.getSymbol(this)
        })
        this.defaultText.bind { text.use() }
        val block = this.defaultTextBlock.getOrCompute()
        block.computeLayouts()
    }

    override fun compileIntoEvents(): List<Event> {
        val b = this.beat - 4
        return RowSetting.getRows(rowSettingBehaviour.rowSetting.getOrCompute(), editor.world).map { row ->
            EventDeployRod(editor.engine, row, b)
        }
    }

    override fun createContextMenu(): ContextMenu {
        return ContextMenu().also { ctxmenu ->
            ctxmenu.addMenuItem(rowSettingBehaviour.createMenuItem(editor))
        }
    }

    override fun copy(): BlockDeployRod {
        return BlockDeployRod(editor).also {
            this.copyBaseInfoTo(it)
            it.rowSettingBehaviour.rowSetting.set(this.rowSettingBehaviour.rowSetting.getOrCompute())
        }
    }
}