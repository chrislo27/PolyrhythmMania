package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.MenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.RowSelectorMenuPane
import polyrhythmmania.util.RodinSpecialChars


class RowBlockData(defaultSetting: RowSetting = RowSetting.ONLY_A) {

    var rowSetting: Var<RowSetting> = Var(defaultSetting)

    fun getSymbolAsListArg(ctx: Var.Context): List<Any?> {
        val setting = ctx.use(this.rowSetting)
        val symbol: String = setting.stringRepresentation
        return listOf(symbol)
    }

    fun createMenuItems(editor: Editor): List<MenuItem> {
        return listOf(
                LabelMenuItem.create(Localization.getValue("blockContextMenu.rowPicker"), editor.editorPane.palette.markup),
                SeparatorMenuItem(),
                CustomMenuItem(RowSelectorMenuPane(editor.editorPane, rowSetting.getOrCompute()) { newSetting ->
                    this.rowSetting.set(newSetting)
                }),
        )
    }
    
    fun writeToJson(obj: JsonObject) {
        obj.add("rowSetting", rowSetting.getOrCompute().jsonId)
    }

    fun readFromJson(obj: JsonObject) {
        rowSetting.set(RowSetting.INDEX_MAP[obj.getInt("rowSetting", 0)] ?: RowSetting.ONLY_A)
    }

}