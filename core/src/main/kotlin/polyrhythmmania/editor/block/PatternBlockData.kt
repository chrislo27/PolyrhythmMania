package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import io.github.chrislo27.paintbox.ui.contextmenu.CustomMenuItem
import io.github.chrislo27.paintbox.ui.contextmenu.LabelMenuItem
import io.github.chrislo27.paintbox.ui.contextmenu.MenuItem
import io.github.chrislo27.paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.PatternMenuPane
import polyrhythmmania.editor.block.contextmenu.RowSelectorMenuPane


class PatternBlockData(val rowCount: Int) {
    
    val rowATypes: Array<CubeType> = Array(rowCount) { CubeType.NONE }
    val rowDpadTypes: Array<CubeType> = Array(rowCount) { CubeType.NONE }

    fun createMenuItems(editor: Editor): List<MenuItem> {
        return listOf(
                LabelMenuItem.create(Localization.getValue("blockContextMenu.spawnPattern"), editor.editorPane.palette.markup),
                SeparatorMenuItem(),
                CustomMenuItem(PatternMenuPane(editor.editorPane, this)),
        )
    }
}