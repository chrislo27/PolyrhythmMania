package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import paintbox.binding.BooleanVar
import paintbox.binding.ContextBinding
import paintbox.binding.Var
import paintbox.font.PaintboxFont
import paintbox.ui.UIElement
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.Toggle
import paintbox.ui.control.ToggleGroup
import paintbox.ui.element.RectElement


class IndentedButton : Button, Toggle {

    override val selectedState: BooleanVar = BooleanVar(false)
    override val toggleGroup: Var<ToggleGroup?> = Var(null)
    
    val indentedButtonBorder: Var<Insets> = Var(Insets(2f))
    val indentedButtonBorderColor: Var<Color> = Var(Color.BLACK)
    
    val borderIndentElement: RectElement
    
    constructor(text: String, font: PaintboxFont = UIElement.defaultFont)
            : super(text, font)
    constructor(binding: ContextBinding<String>, font: PaintboxFont = UIElement.defaultFont)
            : super(binding, font)
    
    init {
        // Faux pas: users may add more buttons not into this element.
        borderIndentElement = RectElement(Color(1f, 1f, 1f, 0f)).apply {
            this.borderStyle.set(SolidBorder().apply {
                this.color.bind {
                    indentedButtonBorderColor.use()
                }
            })
            this.border.bind {
                if (selectedState.use()) indentedButtonBorder.use() else Insets.ZERO
            }
        }
        this += borderIndentElement
        
        this.setOnAction {
            selectedState.invert()
        }
    }
    
}