package polyrhythmmania.container


import paintbox.ui.Pane
import paintbox.ui.StringConverter
import paintbox.ui.control.ComboBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane
import java.util.*


class TexPackSrcSelectorMenuPane(
        editorPane: EditorPane, currentSource: TexturePackSource,
        val legalValues: List<TexturePackSource> = TexturePackSource.VALUES,
        val changeListener: (TexturePackSource) -> Unit
) : Pane() {
    
    val combobox: ComboBox<TexturePackSource> = ComboBox(legalValues, currentSource).also { combobox ->
        combobox.markup.set(editorPane.palette.markup)
        combobox.onItemSelected = { src ->
            changeListener(src)
        }
        combobox.itemStringConverter.set(StringConverter { src ->
            when (src) {
                TexturePackSource.STOCK_GBA -> Localization.getVar("editor.dialog.texturePack.stock.gba")
                TexturePackSource.STOCK_HD -> Localization.getVar("editor.dialog.texturePack.stock.hd")
                TexturePackSource.STOCK_ARCADE -> Localization.getVar("editor.dialog.texturePack.stock.arcade")
                TexturePackSource.CUSTOM -> Localization.getVar("editor.dialog.texturePack.stock.custom")
            }.getOrCompute()
        })
    }

    init {
        this += combobox

//        this.bounds.width.set(250f)
        this.bounds.height.set(32f)
    }
    
}
