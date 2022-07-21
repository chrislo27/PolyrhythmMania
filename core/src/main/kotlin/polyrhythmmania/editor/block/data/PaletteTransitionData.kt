package polyrhythmmania.editor.block.data

import com.badlogic.gdx.graphics.Color
import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.contextmenu.*
import paintbox.ui.control.DecimalTextField
import paintbox.ui.control.TextField
import paintbox.ui.element.RectElement
import paintbox.ui.layout.HBox
import paintbox.util.DecimalFormats
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.world.tileset.PaletteTransition


class PaletteTransitionData {

    val paletteTransition: Var<PaletteTransition> = Var(PaletteTransition.DEFAULT)


    fun createMenuItems(editor: Editor): List<MenuItem> {
        return listOf(
                LabelMenuItem.create(Localization.getValue("blockContextMenu.paletteChange.transitionDuration"), editor.editorPane.palette.markup),
                CustomMenuItem(
                        HBox().apply {
                            this.bounds.height.set(32f)
                            this.spacing.set(4f)

                            fun createTextField(): Pair<UIElement, TextField> {
                                val textField = DecimalTextField(startingValue = paletteTransition.getOrCompute().duration, decimalFormat = DecimalFormats["0.0##"],
                                        font = editor.editorPane.palette.musicDialogFont).apply {
                                    this.minimumValue.set(0f)
                                    this.textColor.set(Color(1f, 1f, 1f, 1f))

                                    this.value.addListener {
                                        val dur = it.getOrCompute()
                                        paletteTransition.set(paletteTransition.getOrCompute().copy(duration = dur.coerceAtLeast(0f)))
                                    }
                                }

                                return RectElement(Color(0f, 0f, 0f, 1f)).apply {
                                    this.bindWidthToParent(adjust = -4f, multiplier = 0.333f)
                                    this.border.set(Insets(1f))
                                    this.borderStyle.set(SolidBorder(Color.WHITE))
                                    this.padding.set(Insets(2f))
                                    this += textField
                                } to textField
                            }

                            this += HBox().apply {
                                this.spacing.set(4f)
                                this += createTextField().first
                            }
                        }
                ),

                SeparatorMenuItem(),
                CheckBoxMenuItem.create(BooleanVar(paletteTransition.getOrCompute().pulseMode),
                        Localization.getValue("blockContextMenu.paletteChange.pulseMode"), editor.editorPane.palette.markup).apply {
                    this.createTooltip = {
                        it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.paletteChange.pulseMode.tooltip")))
                    }
                    this.checkState.addListener {
                        paletteTransition.set(paletteTransition.getOrCompute().copy(pulseMode = it.getOrCompute()))
                    }
                },
                CheckBoxMenuItem.create(BooleanVar(paletteTransition.getOrCompute().reverse),
                        Localization.getValue("blockContextMenu.paletteChange.reverse"), editor.editorPane.palette.markup).apply {
                    this.createTooltip = {
                        it.set(editor.editorPane.createDefaultTooltip(Localization.getValue("blockContextMenu.paletteChange.reverse.tooltip")))
                    }
                    this.checkState.addListener {
                        paletteTransition.set(paletteTransition.getOrCompute().copy(reverse = it.getOrCompute()))
                    }
                },
        )
    }
    
    fun writeToJson(rootObj: JsonObject) {
        val pt = paletteTransition.getOrCompute()
        rootObj.add("duration", pt.duration)
        rootObj.add("pulseMode", pt.pulseMode)
        rootObj.add("reverse", pt.reverse)
    }

    fun readFromJson(rootObj: JsonObject) {
        var pt = PaletteTransition.DEFAULT
        
        val durationVal = rootObj.get("duration")
        if (durationVal != null && durationVal.isNumber) {
            pt = pt.copy(duration = durationVal.asFloat().coerceAtLeast(0f))
        }
        val pulseVal = rootObj.get("pulse")
        if (pulseVal != null && pulseVal.isBoolean) {
            pt = pt.copy(pulseMode = pulseVal.asBoolean())
        }
        val reverseVal = rootObj.get("reverse")
        if (reverseVal != null && reverseVal.isBoolean) {
            pt = pt.copy(reverse = pulseVal.asBoolean())
        }
        
        this.paletteTransition.set(pt)
    }

}