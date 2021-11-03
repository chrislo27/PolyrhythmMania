package polyrhythmmania.library.menu

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.ui.control.ButtonSkin
import paintbox.ui.control.PressedState
import paintbox.util.MathHelper
import paintbox.util.gdxutils.grey
import paintbox.util.gdxutils.set


class LibraryEntryButtonSkin(override val element: LibraryEntryButton) : ButtonSkin(element) {
    
    companion object {
        val TEXT_COLOR: Color = Color().grey(90f / 255f, 1f)
        val DISABLED_TEXT: Color = Color().grey(150f / 255f)
        val HOVERED_TEXT: Color = Color().grey(1f)
        val PRESSED_TEXT: Color = Color(0.4f, 1f, 1f, 1f)
        val PRESSED_AND_HOVERED_TEXT: Color = Color(0.5f, 1f, 1f, 1f)

        val BG_COLOR: Color = Color(1f, 1f, 1f, 0f)
        val HOVERED_BG: Color = Color().grey(90f / 255f, 0.8f)
        val DISABLED_BG: Color = BG_COLOR.cpy()
        val PRESSED_BG: Color = HOVERED_BG.cpy()
        val PRESSED_AND_HOVERED_BG: Color = HOVERED_BG.cpy()
        
        val SELECTED_TEXT: Color = TEXT_COLOR.cpy()
        val SELECTED_BG_FROM: Color = Color().set(78, 219, 81)
        val SELECTED_BG_TO: Color = Color().set(87, 221, 242)
    }
    
    
    private val selectedBgColor: Color = SELECTED_BG_FROM.cpy() // This gets mutated on render
    private val selectedTextColor: Var<Color> = Var(SELECTED_TEXT)
    
    private val tmpTextColor: Color = Color().set(selectedTextColor.getOrCompute())

    init {
        val grey = TEXT_COLOR
        this.defaultTextColor.set(grey)
        this.disabledTextColor.set(DISABLED_TEXT)
        this.hoveredTextColor.set(HOVERED_TEXT)
        this.pressedTextColor.set(PRESSED_TEXT)
        this.pressedAndHoveredTextColor.set(PRESSED_AND_HOVERED_TEXT)
        this.defaultBgColor.set(BG_COLOR)
        this.hoveredBgColor.set(HOVERED_BG)
        this.disabledBgColor.set(DISABLED_BG)
        this.pressedBgColor.set(PRESSED_BG)
        this.pressedAndHoveredBgColor.set(PRESSED_AND_HOVERED_BG)
        this.roundedRadius.set(0)
        
        (textColorToUse as Var).bind {
            val selectedState = element.selectedState.useB()
            val pressedState = element.pressedState.use()
            if (selectedState) {
                tmpTextColor.set(selectedTextColor.use())
                if (pressedState.hovered) {
                    tmpTextColor.mul(1.25f, 1.25f, 1.25f, 1f)
                }
                if (pressedState.pressed) {
                    tmpTextColor.mul(0.5f, 0.5f, 0.5f, 1f)
                }
                tmpTextColor
            } else {
                if (element.apparentDisabledState.useB()) {
                    disabledTextColor.use()
                } else {
                    when (pressedState) {
                        PressedState.NONE -> defaultTextColor.use()
                        PressedState.HOVERED -> hoveredTextColor.use()
                        PressedState.PRESSED -> pressedTextColor.use()
                        PressedState.PRESSED_AND_HOVERED -> pressedAndHoveredTextColor.use()
                    }
                }
            }
        }
        (bgColorToUse as Var).bind {
            val selectedState = element.selectedState.useB()
            if (selectedState) {
                selectedBgColor
            } else {
                val pressedState = element.pressedState.use()
                if (element.apparentDisabledState.useB()) {
                    disabledBgColor.use()
                } else {
                    when (pressedState) {
                        PressedState.NONE -> defaultBgColor.use()
                        PressedState.HOVERED -> hoveredBgColor.use()
                        PressedState.PRESSED -> pressedBgColor.use()
                        PressedState.PRESSED_AND_HOVERED -> pressedAndHoveredBgColor.use()
                    }
                }
            }
        }
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val alpha = MathHelper.getCosineWave(System.currentTimeMillis() - element.selectedTimeMs, 60f / 112.0f)
        selectedBgColor.set(SELECTED_BG_FROM).lerp(SELECTED_BG_TO, alpha)
        
        super.renderSelf(originX, originY, batch)
    }
}