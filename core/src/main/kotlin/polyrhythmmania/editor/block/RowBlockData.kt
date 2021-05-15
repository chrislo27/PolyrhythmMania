package polyrhythmmania.editor.block

import io.github.chrislo27.paintbox.binding.Var
import io.github.chrislo27.paintbox.ui.contextmenu.CustomMenuItem
import io.github.chrislo27.paintbox.ui.contextmenu.MenuItem
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.RowSelectorMenuPane
import polyrhythmmania.util.RodinSpecialChars


class RowBlockData(defaultSetting: RowSetting = RowSetting.ONLY_A) {

    var rowSetting: Var<RowSetting> = Var(defaultSetting)

    fun getSymbol(ctx: Var.Context): List<Any?> {
        val setting = ctx.use(this.rowSetting)
        val symbol: String = when (setting) {
            RowSetting.ONLY_A -> RodinSpecialChars.BORDERED_A
            RowSetting.ONLY_DPAD -> RodinSpecialChars.BORDERED_DPAD
            RowSetting.BOTH -> "${RodinSpecialChars.BORDERED_A}${RodinSpecialChars.BORDERED_DPAD}"
        }
        return listOf(symbol)
    }

    fun createMenuItem(editor: Editor): MenuItem {
        return CustomMenuItem(RowSelectorMenuPane(editor.editorPane, rowSetting.getOrCompute()) { newSetting ->
            this.rowSetting.set(newSetting)
        })
    }

}