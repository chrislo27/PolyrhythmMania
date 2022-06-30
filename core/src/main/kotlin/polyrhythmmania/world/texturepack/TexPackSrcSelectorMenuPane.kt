package polyrhythmmania.world.texturepack


import paintbox.ui.Pane
import paintbox.ui.StringConverter
import paintbox.ui.control.ComboBox
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane


class TexPackSrcSelectorMenuPane(
        editorPane: EditorPane, currentSource: TexturePackSource,
        val legalValues: List<TexturePackSource> = TexturePackSource.VALUES_WITH_CUSTOM,
        val changeListener: (TexturePackSource) -> Unit
) : Pane() {
    
    val combobox: ComboBox<TexturePackSource> = ComboBox(legalValues, currentSource).also { combobox ->
        combobox.markup.set(editorPane.palette.markup)
        combobox.onItemSelected = { src ->
            changeListener(src)
        }
        combobox.itemStringConverter.set(StringConverter { src ->
            when (src) {
                TexturePackSource.StockGBA -> Localization.getValue("editor.dialog.texturePack.stock.gba")
                TexturePackSource.StockHD -> Localization.getValue("editor.dialog.texturePack.stock.hd")
                TexturePackSource.StockArcade -> Localization.getValue("editor.dialog.texturePack.stock.arcade")
                is TexturePackSource.Custom -> Localization.getValue("editor.dialog.texturePack.stock.custom", src.id)
            }
        })
    }

    init {
        this += combobox

//        this.bounds.width.set(250f)
        this.bounds.height.set(32f)
    }
    
}
