package polyrhythmmania.editor.block

import com.eclipsesource.json.JsonObject
import paintbox.binding.Var
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.contextmenu.MenuItem
import paintbox.ui.contextmenu.SeparatorMenuItem
import polyrhythmmania.Localization
import polyrhythmmania.container.TexPackSrcSelectorMenuPane
import polyrhythmmania.container.TexturePackSource
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.RowSelectorMenuPane


class TexPackSourceData(defaultSetting: TexturePackSource = TexturePackSource.STOCK_GBA) {

    var texPackSrc: Var<TexturePackSource> = Var(defaultSetting)


    fun createMenuItems(editor: Editor): List<MenuItem> {
        return listOf(
                LabelMenuItem.create(Localization.getValue("blockContextMenu.texturePackSource"), editor.editorPane.palette.markup),
                SeparatorMenuItem(),
                CustomMenuItem(TexPackSrcSelectorMenuPane(editor.editorPane, texPackSrc.getOrCompute()) { newSetting ->
                    this.texPackSrc.set(newSetting)
                }),
        )
    }
    
    fun writeToJson(obj: JsonObject) {
        obj.add("texPackSrc", texPackSrc.getOrCompute().jsonId)
    }

    fun readFromJson(obj: JsonObject) {
        texPackSrc.set(TexturePackSource.INDEX_MAP[obj.getInt("texPackSrc", 0)] ?: TexturePackSource.STOCK_GBA)
    }

}