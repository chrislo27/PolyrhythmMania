package polyrhythmmania.editor.pane

import com.badlogic.gdx.graphics.Color
import paintbox.PaintboxGame
import paintbox.binding.Var
import paintbox.binding.invert
import paintbox.font.PaintboxFont
import paintbox.ui.area.Insets
import paintbox.ui.border.SolidBorder
import paintbox.ui.control.Button
import paintbox.ui.control.Toggle
import paintbox.ui.control.ToggleGroup


class IndentedButton : Button, Toggle {

    override val selectedState: Var<Boolean> = Var(false)
    override val toggleGroup: Var<ToggleGroup?> = Var(null)
    
    val indentedButtonBorder: Var<Insets> = Var(Insets(2f))
    val indentedButtonBorderColor: Var<Color> = Var(Color.BLACK)
    
    constructor(text: String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : super(text, font)
    constructor(binding: Var.Context.() -> String, font: PaintboxFont = PaintboxGame.gameInstance.debugFont)
            : super(binding, font)
    
    init {
        this.borderStyle.set(SolidBorder().apply {
            this.color.bind {
                indentedButtonBorderColor.use()
            }
        })
        this.border.bind {
            if (selectedState.use()) indentedButtonBorder.use() else Insets.ZERO
        }
        
        this.setOnAction {
            selectedState.invert()
        }
    }
    
}