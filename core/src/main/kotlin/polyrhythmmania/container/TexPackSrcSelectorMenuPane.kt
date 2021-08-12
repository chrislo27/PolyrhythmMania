package polyrhythmmania.container


import paintbox.PaintboxGame
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.ui.Pane
import paintbox.ui.control.RadioButton
import paintbox.ui.control.ToggleGroup
import polyrhythmmania.Localization
import polyrhythmmania.editor.pane.EditorPane


class TexPackSrcSelectorMenuPane(editorPane: EditorPane, currentSource: TexturePackSource, val changeListener: (TexturePackSource) -> Unit)
    : Pane() {

    val toggleGroup: ToggleGroup = ToggleGroup()
    val gbaRadio: TPSSettingRadioButton = TPSSettingRadioButton(TexturePackSource.STOCK_GBA, binding = { Localization.getVar("editor.dialog.texturePack.stock.gba").use() })
    val hdRadio: TPSSettingRadioButton = TPSSettingRadioButton(TexturePackSource.STOCK_HD, binding = { Localization.getVar("editor.dialog.texturePack.stock.hd").use() })
    val customRadio: TPSSettingRadioButton = TPSSettingRadioButton(TexturePackSource.CUSTOM, binding = { Localization.getVar("editor.dialog.texturePack.stock.custom").use() })
    val radios: List<RadioButton> = listOf(gbaRadio, hdRadio, customRadio)

    init {
        val radioHeight = 32f
        var posY = 0f
        for (radio in radios) {
            radio.bounds.y.set(posY)
            radio.bounds.height.set(radioHeight)
            radio.textLabel.markup.set(editorPane.palette.markup)
            toggleGroup.addToggle(radio)

            posY += radio.bounds.height.get()

            this.addChild(radio)
        }

        when (currentSource) {
            TexturePackSource.STOCK_GBA -> gbaRadio
            TexturePackSource.STOCK_HD -> hdRadio
            TexturePackSource.CUSTOM -> customRadio
        }.checkedState.set(true)

        this.bounds.width.set(250f)
        this.bounds.height.set(posY)
    }

    inner class TPSSettingRadioButton(val src: TexturePackSource,
                                      binding: Var.Context.() -> String,
                                      font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
        : RadioButton(binding, font) {

        init {
            this.onSelected = {
                changeListener.invoke(src)
            }
        }
    }

}