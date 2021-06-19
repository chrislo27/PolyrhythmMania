package polyrhythmmania.editor.block.contextmenu

import paintbox.PaintboxGame
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.ui.Pane
import paintbox.ui.control.RadioButton
import paintbox.ui.control.ToggleGroup
import polyrhythmmania.Localization
import polyrhythmmania.editor.block.RowSetting
import polyrhythmmania.editor.pane.EditorPane


class RowSelectorMenuPane(editorPane: EditorPane, currentRowSetting: RowSetting, val changeListener: (RowSetting) -> Unit)
    : Pane() {

    val toggleGroup: ToggleGroup = ToggleGroup()
    val aRadio: RowSettingRadioButton = RowSettingRadioButton(RowSetting.ONLY_A, binding = { Localization.getVar("blockContextMenu.rowPicker.onlyA").use() })
    val dpadRadio: RowSettingRadioButton = RowSettingRadioButton(RowSetting.ONLY_DPAD, binding = { Localization.getVar("blockContextMenu.rowPicker.onlyDpad").use() })
    val bothRadio: RowSettingRadioButton = RowSettingRadioButton(RowSetting.BOTH, binding = { Localization.getVar("blockContextMenu.rowPicker.both").use() })
    val radios: List<RadioButton> = listOf(aRadio, dpadRadio, bothRadio)

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

        when (currentRowSetting) {
            RowSetting.ONLY_A -> aRadio
            RowSetting.ONLY_DPAD -> dpadRadio
            RowSetting.BOTH -> bothRadio
        }.checkedState.set(true)

        this.bounds.width.set(250f)
        this.bounds.height.set(posY)
    }

    inner class RowSettingRadioButton(val rowSetting: RowSetting,
                                      binding: Var.Context.() -> String,
                                      font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
        : RadioButton(binding, font) {

        init {
            this.onSelected = {
                changeListener.invoke(rowSetting)
            }
        }
    }

}