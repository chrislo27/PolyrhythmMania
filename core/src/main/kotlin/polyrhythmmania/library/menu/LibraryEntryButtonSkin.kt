package polyrhythmmania.library.menu

import com.badlogic.gdx.graphics.Color
import paintbox.ui.control.Button
import paintbox.ui.control.ButtonSkin
import paintbox.ui.skin.Skin
import paintbox.util.gdxutils.grey


class LibraryEntryButtonSkin(element: LibraryEntryButton) : ButtonSkin(element) {
    
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
    }

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
    }
}