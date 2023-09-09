package polyrhythmmania.editor.block

import com.badlogic.gdx.graphics.Color
import paintbox.ui.Anchor
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.ContextMenu
import paintbox.ui.contextmenu.CustomMenuItem
import paintbox.ui.contextmenu.LabelMenuItem
import paintbox.ui.control.Button
import paintbox.ui.control.DecimalTextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.DecimalFormats
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.editor.block.contextmenu.AbstractPatternMenuPane
import polyrhythmmania.world.entity.EntityRodDecor
import java.text.DecimalFormat


interface CustomSpeedBlock {

    companion object {

        const val DEFAULT_X_UNITS_PER_BEAT: Float = EntityRodDecor.DEFAULT_X_UNITS_PER_BEAT
        const val MIN_X_UNITS_PER_BEAT: Float = 0.25f
        
        val INPUT_DECIMAL_FORMAT: DecimalFormat = DecimalFormats["0.0##"]
        
        private fun invertButtonSkin(button: Button) = AbstractPatternMenuPane.invertButtonSkin(button)
    }

    var xUnitsPerBeat: Float


    fun ContextMenu.addToContextMenu(editor: Editor): ContextMenu {
        return this.also { ctxmenu ->
            ctxmenu.addMenuItem(
                LabelMenuItem.create(
                    Localization.getValue(
                        "blockContextMenu.customSpeedBlock.beatsPerBlock",
                        1f / DEFAULT_X_UNITS_PER_BEAT
                    ),
                    editor.editorPane.palette.markup
                ).apply {
                    this.createTooltip = { v ->
                        v.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.customSpeedBlock.beatsPerBlock.tooltip")))
                    }
                }
            )
            ctxmenu.addMenuItem(CustomMenuItem(
                HBox().apply {
                    this.bounds.height.set(32f)
                    this.spacing.set(4f)
                    
                    val textField: DecimalTextField
                    
                    this += RectElement(Color(0f, 0f, 0f, 1f)).apply {
                        this.bounds.width.set(175f)
                        
                        this.border.set(Insets(1f))
                        this.borderStyle.set(SolidBorder(Color.WHITE))
                        this.padding.set(Insets(2f))
                        textField = DecimalTextField(
                            startingValue = 1f / xUnitsPerBeat,
                            decimalFormat = INPUT_DECIMAL_FORMAT,
                            font = editor.editorPane.palette.musicDialogFont
                        ).apply {
                            this.minimumValue.set(0.25f)
                            this.textColor.set(Color(1f, 1f, 1f, 1f))

                            this.value.addListener {
                                xUnitsPerBeat = 1f / it.getOrCompute().coerceAtLeast(MIN_X_UNITS_PER_BEAT)
                            }
                        }
                        this += textField
                    }

                    this += Button(
                        Localization.getVar("common.reset"),
                        font = editor.editorPane.main.fontEditor
                    ).apply {
                        Anchor.CentreRight.configure(this)
                        this.bounds.width.set(64f)
                        invertButtonSkin(this)
                        this.setOnAction {
                            textField.setValue(1f / DEFAULT_X_UNITS_PER_BEAT)
                        }
                    }
                }
            ))
        }
    }

}