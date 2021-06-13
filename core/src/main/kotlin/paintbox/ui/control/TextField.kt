package paintbox.ui.control

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import paintbox.ui.Focusable
import paintbox.ui.skin.DefaultSkins
import paintbox.ui.skin.Skin
import paintbox.ui.skin.SkinFactory

/**
 * A single-line text field.
 */
class TextField : Control<TextField>(), Focusable {
    companion object {
        const val TEXT_FIELD_SKIN_ID: String = "TextField"
        
        const val BACKSPACE: Char = 8.toChar()
        const val ENTER_DESKTOP = '\r'
        const val ENTER_ANDROID = '\n'
        const val TAB = '\t'
        const val DELETE: Char = 127.toChar()
        const val BULLET: Char = 149.toChar()
        const val CARET_BLINK_RATE: Float = 0.5f
        const val CARET_MOVE_TIMER: Float = 0.05f
        const val INITIAL_CARET_TIMER: Float = 0.4f
//        const val NEWLINE_WRAP: Char = '\uE056'

        init {
            DefaultSkins.register(TEXT_FIELD_SKIN_ID, SkinFactory { element: TextField ->
                TextFieldSkin(element)
            })
        }
    }

    override fun onFocusGained() {
        super.onFocusGained()
    }

    override fun onFocusLost() {
        super.onFocusLost()
    }

    override fun getDefaultSkinID(): String = TextField.TEXT_FIELD_SKIN_ID
}

open class TextFieldSkin(element: TextField) : Skin<TextField>(element) {
    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
    }

    override fun renderSelfAfterChildren(originX: Float, originY: Float, batch: SpriteBatch) {
    }
}
